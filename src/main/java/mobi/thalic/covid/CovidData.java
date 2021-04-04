/*
 * The MIT License
 *
 * Copyright 2020 Gary Larson gary@thalic.mobi.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package mobi.thalic.covid;

import static java.lang.Math.round;
import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 *
 * @author GaryL
 */
public class CovidData {
    // Declare constants
    private final SimpleDateFormat simpleDateFormat = 
            new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat simpleDateFormatAlt = 
            new SimpleDateFormat("yyyy_MM_dd");
    private final java.sql.Date TODAY = getTodaysDate();
    private final String YESTERDAY = getYesterdaysDate();
    private final java.sql.Date YESTERDAY_DATE = getYesterday();
    private final String PATH = "C:\\covid\\";
    
    // Declare database variables
    
    private final DatabaseUtilities databaseUtilities;
    private final JSONUtilities jsonUtilities;
    private final CSVUtilities csvUtilities;
    private final Results mResults;
    
    /**
     * Default constructor
     * @param results of all activities
     */
    public CovidData (Results results) {
        mResults = results;
        databaseUtilities = new DatabaseUtilities(mResults);
        jsonUtilities = new JSONUtilities(mResults);
        csvUtilities = new CSVUtilities(mResults);
    }
    
    public void getOwidData() {
        List<Owid> owidList = jsonUtilities.processOwidFullJson();
        if (owidList.size() > 0) {
            owidList.stream().map(owid -> {
                int isoResults;
                do {
                    isoResults = databaseUtilities.isIsoCode(owid.getIsoCode());
                    if (isoResults == databaseUtilities.RETURN_FALSE) {
                        int countryResults;
                        do {
                            countryResults = databaseUtilities.insertOwidCountry(
                                    owid.getIsoCode(), owid.getContinent(),
                                    owid.getLocation(), owid.getPopulation(),
                                    owid.getPopulation100k());
                            if (countryResults == databaseUtilities.RETURN_FALSE) {
                                mResults.addResults("getOwidData Insert Country Failed" +
                                        " " + owid.getIsoCode());
                            }
                        } while (countryResults == databaseUtilities.RETURN_ERROR);
                    }
                } while (isoResults == databaseUtilities.RETURN_ERROR);
                return owid;
            }).forEachOrdered(owid -> {
                owid.getOwidDaily().forEach(daily -> {
                    int isoDateResults;
                    do {
                        isoDateResults = databaseUtilities.isDaily(owid.getIsoCode(),
                                daily.getDate());
                        if (isoDateResults == databaseUtilities.RETURN_FALSE) {
                            int dailyResults;
                            do {
                                dailyResults = databaseUtilities.insertOwidDaily(
                                        owid.getIsoCode(), daily, owid.getPopulation100k());
                                if (dailyResults == databaseUtilities.RETURN_FALSE) {
                                    mResults.addResults("getOwidData Insert Daily "
                                            + "Failed Code:  Code: " +
                                            owid.getIsoCode() + " Date: " +
                                            daily.getDate());
                                }
                            } while (dailyResults == databaseUtilities.RETURN_ERROR);
                        }
                    } while(isoDateResults == databaseUtilities.RETURN_ERROR);
                });
            });
        }
    }
    
    /**\
     * Method to create a csv file of the latest totals
     */
    public void createCSVFile() {
        Connection conn = getDatabaseConnection();
        List<List<String>> lists = databaseUtilities.getLatestCountryTotals(
                conn, YESTERDAY_DATE);
        csvUtilities.writeCSVFile(lists, PATH + "world_covid_" + YESTERDAY + 
                ".csv");
    }
    
    /**
     * Method to write World totals to the database
     * @param conn to the database
     * @param lists of data to process
     */
    private void writeWorldToDatabase(Connection conn, 
            List<List<String>> lists) {
        // insert data in total table in database
        for(int i = 1; i < lists.size(); i++) {
            databaseUtilities.insertWorldTotal(conn, lists.get(i));
        }
    }
    
    /**
     * Method to update population
     * @param conn to the database
     * @param country either World or UnitedStates
     * @param lists to update
     */
    private void updatePopulation(Connection conn, String country, 
            List<List<String>> lists) {
        long adjustment;
       
        for (int i = 1; i < lists.size(); i++) {
            long population = convertPopulation(lists.get(i).get(4));
            String place = lists.get(i).get(0);
            if (country.equals("UnitedStates")) {
                long statePopulation = databaseUtilities
                        .selectStatePopulation(conn, place);
                if (statePopulation == 0) {
                    databaseUtilities.insertStatePopulation(conn, place, 
                            population);
                } else {
                adjustment = statePopulation / 10;
                    if (statePopulation > population - adjustment && 
                            statePopulation < population + adjustment && 
                            population != statePopulation) {
                        databaseUtilities.updateStatePopulation(conn, place, 
                                population);
                    }
                }
            } else {
                long countryPopulation = databaseUtilities
                        .selectWorldPopulation(conn, place);
                if (countryPopulation == 0 && population != 0) {
                    if (databaseUtilities.selectCountryId(conn, place) != 0) {
                        databaseUtilities.insertWorldPopulation(conn, place, 
                                population);
                    }
                } else {
                    adjustment = countryPopulation / 10;
                    if (countryPopulation > population - adjustment && 
                            countryPopulation < population + adjustment && 
                            countryPopulation != population) {
                        databaseUtilities.updateWorldPopulation(conn, place, 
                                population);
                        if (place.equals("USA")) {
                            databaseUtilities.updateStatePopulation(conn, 
                                    "USA Total", population);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Method to test the database
     */
    public void testDatabase(){
        Connection conn = getDatabaseConnection();
        long population = databaseUtilities.selectStatePopulation(conn, 
                                "USA Total");
        mResults.addResults(String.format(Locale.getDefault(), 
                "USA Total population = %,d", population));
        // close connection
        databaseUtilities.closeConnection(conn);        
    }
    
    /**
     * Method to write US totals to the database
     * @param conn to the database
     * @param lists of data to process
     * @return results
     */
    private void writeUSToDatabase(Connection conn, 
            List<List<String>> lists) {
        for (int i = 1; i < lists.size(); i++) {
            if (!lists.get(i).get(0).equals("Total:")) {
                // insert data in total table in database
                databaseUtilities.insertUSTotal(conn, lists.get(i));
            }
        } 
    }
    
    /**
     * Method to create and add calculations to the database
     */
    public void createCalculations() {
        mResults.addResults("Starting create calculations!");
        // get connection to the database
        Connection conn = getDatabaseConnection();
        // get dates of all totals
        List<java.sql.Date> dates = 
                databaseUtilities.getCountryTotalsDates(conn);
        // loop through dates
        dates.forEach(date -> {
            // calculate totals for this date
            calculateTotal(conn, date);
            calculateStateTotal(conn, date);
        });
        // close database connection
        databaseUtilities.closeConnection(conn);
        mResults.addResults("Completed create calculations!");
    }
    
    /**
     * Method to create and add calculations to the database
     */
    public void runCalculationsYesterday() {
        mResults.addResults("Starting run calculations yesterday!");
        // get connection to the database
        Connection conn = getDatabaseConnection();
        
        // calculate totals for yesterday
        calculateTotal(conn, YESTERDAY_DATE);
        // close database connection
        databaseUtilities.closeConnection(conn);
        mResults.addResults("Completed run calculations yesterday!");
    }
    
    /**
     * Method to create and add calculations to the database
     * @param date of calculations
     */
    public void runCalculations(String date) {
        mResults.addResults("Starting run calculations!");
        // get connection to the database
        Connection conn = getDatabaseConnection();
        // convert date string to date
        try {
            java.util.Date parsedate = simpleDateFormat.parse(date);
            java.sql.Date sql = new java.sql.Date(parsedate.getTime());
            // calculate totals for yesterday
            calculateTotal(conn, sql);
        } catch(ParseException e) {
            System.out.println(e);
        }
        // close database connection
        databaseUtilities.closeConnection(conn);
        mResults.addResults("Completed run calculations!");
    }
    
    /**
     * Method to calculate the median from a list of doubles
     * @param list of doubles to calculate from
     * @return the median
     */
    private double calculateMedian(List<Double> list) {
        // Create variable to return
        double median;
        // Determin if odd or even array entrie
        if (list.size() % 2 == 0) {
            // get two midde entries
            double value1 = list.get((list.size() / 2) - 1);
            double value2 = list.get(list.size() / 2);
            // sum and devide in half
            median = (value1 + value2) / 2.0;
        } else {
            // get middle entry
            median = list.get((list.size() / 2));
        }
        // return median
        return median;
    }
    
    /**
     * Method to create 13 medians
     * @param locationList with values to create medians from
     * @return map of country and score
     */
    private List<Double> createMedians(List<StringDouble> locationList) {
        // Create variable for the sum;
        double sum = 0.0;
        // create a list of doubles from the list of country doubles and get sum
        List<Double> list = new ArrayList<>();
        sum = locationList.stream().map(locationDouble -> {
            list.add(locationDouble.getValue());
            return locationDouble;
        }).map(locationDouble -> locationDouble.getValue()).reduce(sum, (accumulator, _item) -> accumulator + _item);
        // calculate mean
        double mean = sum / list.size();
        // split list
        List<Double> lowList = new ArrayList<>();
        List<Double> highList = new ArrayList<>();
        list.forEach(value -> {
            if (value < mean) {
                lowList.add(value);
            } else {
                highList.add(value);
            }
        });
        if (lowList.isEmpty() || highList.isEmpty()) {
            return new ArrayList<>();
        }
        // get list for the low medians
        List<Double> lowMedians = getMedians(lowList);
        List<DoubleInteger> lowMedianCounts = getCounts(lowList, lowMedians);
        while (lowMedianCounts.size() > 7) {
            // adjust medians if too many
            lowMedians = combineSmallest(lowMedianCounts);
            lowMedianCounts = getCounts(lowList, lowMedians);
        }
        while (lowMedianCounts.size() < 7) {
            // adjust medians if too few
            lowMedians = splitLargest(lowMedianCounts, lowList);
            lowMedianCounts = getCounts(lowList, lowMedians);
        }
        // get list for the high medians
        List<Double> highMedians = getMedians(highList);
        List<DoubleInteger> highMedianCounts = getCounts(highList, highMedians);
        while (highMedianCounts.size() > 6) {
            // adjust medians if too many
            highMedians = combineSmallest(highMedianCounts);
            highMedianCounts = getCounts(highList, highMedians);
        }
        while (highMedianCounts.size() < 6) {
            // adjust medians if too few
            highMedians = splitLargest(highMedianCounts, highList);
            highMedianCounts = getCounts(highList, highMedians);
        }
        // combine medians
        lowMedians.addAll(highMedians);
        
        return lowMedians;
    }
    
    /**
     * Method to get the median from a list and the median
     * @param list of values
     * @param median of the list
     * @return a list of medians
     */
    private List<Double> getMedians(List<Double> list) {
        if (list.isEmpty()) {
            return new ArrayList<>();
        }
        // get the median from the list of values
        double median = calculateMedian(list);
        // Create a variable for the medians
        List<Double> medians = new ArrayList<>();
        // get the highest value and add 0.1
        double highest = list.get(list.size() -1) + 0.1;
        // Create the medium sum value and set it to the median
        double medianSum = median;
        // Loop through and create the remaining medians
        while(medianSum < list.get(list.size() - 1) - median) {
            medians.add(medianSum);
            medianSum += median;
        }
        // add the highest as a median
        medians.add(highest);
        // return the medians
        return medians;
    }
    
    /**
     * Method to get number of values per median
     * @param list of values
     * @param medians list of medians
     * @return array of integers
     */
    private List<DoubleInteger> getCounts(List<Double> list, List<Double> medians) {
        
        // Create the array for the counts
        List<DoubleInteger> medianCounts = new ArrayList<>();
        for (int i = 0; i < medians.size(); i++) {
            medianCounts.add(new DoubleInteger(medians.get(i), 0));
        }
        for (double value : list) {
            // Declare variables and initialize
            boolean isAssigned = false;
            // find median value belongs to and increment the count
            for (int i = 0; i < medians.size(); i++) {
                if(value < medians.get(i)) {
                    medianCounts.get(i).setIntValue(medianCounts.get(i)
                            .getIntValue() + 1);
                    isAssigned = true;
                    break;
                }
            }
            // if not assigned add to highest
            if (!isAssigned) {
                medianCounts.get(medians.size() - 1)
                    .setIntValue(medianCounts.get(medians.size() - 1)
                            .getIntValue() + 1);
            }
        }
        // return array
        return medianCounts;
    }
    
    /**
     * Method to adjust for too many medians
     * @param medians medians to combine
     * @param counts of the medians
     * @return adjusted medians
     */
    private List<Double> combineSmallest(List<DoubleInteger> medians){
        // get largest count
        int count = 0;
        for (int i = 0; i < medians.size(); i++) {
            if (medians.get(i).getIntValue() > count) {
                count = medians.get(i).getIntValue();
            }
        }
        // Declare and initialize variables
        int smallest = count * 2;
        int target = 0;
        for (int i = 0; i < medians.size() - 1; i++) {
            // find the smallest consecutive counts
            if (medians.get(i).getIntValue() + medians.get(i + 1).getIntValue() 
                    < smallest) {
               smallest =  medians.get(i).getIntValue() + 
                       medians.get(i + 1).getIntValue();
               // get target median
               target = i;
            }
        }
        // remove target median
        medians.remove(target);
        // create new list
        List<Double> finalMedians = new ArrayList<>();
        for (int i = 0; i < medians.size(); i++) {
            finalMedians.add(medians.get(i).getDoubleValue());
        }
        return finalMedians;
    }
    
    /**
     * Method to adjust for too few medians
     * @param medians to adjust
     * @param counts of values for medians
     * @return adjusted medians
     */
    private List<Double> splitLargest(List<DoubleInteger> medians, 
            List<Double> list) {
        // Declare and initialize variables
        int largest = 0;
        double current = 0;
        double low = 0.0;
        double high = 0.0;
        // loop through and find median with the most values
        for (int i = 0; i < medians.size(); i++) {
            if(medians.get(i).getIntValue() > largest) {
                largest = medians.get(i).getIntValue();
                low = current;
                high = medians.get(i).getDoubleValue();
            }
            current = medians.get(i).getDoubleValue();
        }
        // Create a list for the new medians
        List<Double> newMedians = new ArrayList<>();
        List<Double> tempList = new ArrayList<>();
        for (int i = 0; i < medians.size(); i++) {
            if(medians.get(i).getDoubleValue() == low || low == 0) {
                if (low > 0) {
                    // add original low median
                    newMedians.add(low);
                    // create a temp list of median to be split
                    for (double value : list) {
                        if (value > low && value < high) {
                            tempList.add(value);
                        }
                    }
                    // calculate middle value of median to be split
                    double newMedian = (Collections.min(tempList) + 
                            Collections.max(tempList)) / 2;
                    // add new median
                    newMedians.add(newMedian); 
                } else {
                    // create a temp list of median to be split
                    for (double value : list) {
                        if (value >= low && value < high) {
                            tempList.add(value);
                        }
                    }
                    // calculate middle value of median to be split
                    double newMedian = (Collections.min(tempList) + 
                            Collections.max(tempList)) / 2;
                    // add new median
                    newMedians.add(newMedian); 
                    // add lowest median
                    newMedians.add(high);
                    // prevent from repeating this option
                    low = -1;
                }
            }
            else {
                // add original median
                newMedians.add(medians.get(i).getDoubleValue());
            }
        }
        // return new medians
        return newMedians;
    }
    
    /**
     * Method to create the data points for the front end
     * @param conn connection to the database
     * @param date of the data
     */
    public void calculateTotal(Connection conn, java.sql.Date date) {
        // get data from database
        WorldData worldData = databaseUtilities.getWorldData(conn, date);
        // get cases from database
        Map<String, Long> casesData = 
                databaseUtilities.getCasesData(conn, date);
        // get new cases from database
        Map<String, Long> newCasesData = 
                databaseUtilities.getNewCasesData(conn, date);
        // get deaths from database
        Map<String, Long> deathsData = 
                databaseUtilities.getDeathsData(conn, date);
        // get new deaths from database
        Map<String, Long> newDeathsData = 
                databaseUtilities.getNewDeathsData(conn, date);
        // get active from database
        Map<String, Long> activeData = 
                databaseUtilities.getActiveData(conn, date);
        // get population list from database
        List<StringLong> populationList = 
                databaseUtilities.getPopulationData(conn, date);
        // get cases100k list from database
        List<StringDouble> cases100kList = 
                databaseUtilities.getCases100kData(conn, date);
        // get cases100k16 list from database
        List<StringDouble> cases100k16List = 
                databaseUtilities.getCases100kData16(conn, date);
        List<StringDouble> cases100k31List = 
                databaseUtilities.getCases100kData31(conn, date);
        // get deaths100k from database
        List<StringDouble> deaths100kList = 
                databaseUtilities.getDeaths100kData(conn, date);
        // get deaths100k16 from database
        List<StringDouble> deaths100k16List = 
                databaseUtilities.getDeaths100kData16(conn, date);
        // get deaths100k16 from database
        List<StringDouble> deaths100k31List = 
                databaseUtilities.getDeaths100kData31(conn, date);
        // get active100k from database
        List<StringDouble> active100kList = 
                databaseUtilities.getActive100kData(conn, date);
        // create population ranks
        Map<String, Integer> populationRanks = 
                assignRanksLong(populationList);
        // create cases100k ranks
        Map<String, Integer> cases100kRanks = 
                assignRanksDouble(cases100kList);
        // create deaths100k ranks
        Map<String, Integer> deaths100kRanks = 
                assignRanksDouble(deaths100kList);
        // create active100k ranks
        Map<String, Integer> active100kRanks = 
                assignRanksDouble(active100kList);
        // create case100k medians
        List<Double> cases100kMedians = createMedians(cases100kList);
        // create cases100k scores
        Map<String, Integer> cases100kScores = 
                createScores(cases100kMedians, cases100kList);
        // create cases100k grades
        Map<String, String> cases100kGrades = createGrades(cases100kScores);
        // create death100k medians
        List<Double> deaths100kMedians = createMedians(deaths100kList);
        // create deaths100k scores
        Map<String, Integer> deaths100kScores = 
                createScores(deaths100kMedians, deaths100kList);
        Map<String, String> deaths100kGrades = createGrades(deaths100kScores);
        // create active100k medians
        List<Double> active100kMedians = createMedians(active100kList);
        // create active100k scores
        Map<String, Integer> active100kScores = 
                createScores(active100kMedians, active100kList);
        // create active100k grades
        Map<String, String> active100kGrades = createGrades(active100kScores);
        // create populations data
        Map<String, Long> populationData = createDataLong(populationList);
        // create cases100k data
        Map<String, Double> cases100kData = createDataDouble(cases100kList);
        // create cases100k 15 days data
        // Declare and initialize variables
        Map<String, Double> cases100k15Data = new HashMap<>();
        Map<String, Integer> cases100k15Ranks = new HashMap<>();
        Map<String, Integer> cases100k15Scores = new HashMap<>();
        Map<String, String> cases100k15Grades = new HashMap<>();
        if (cases100k16List.size() > 0) {
            // convert cases100k 16th day data
            Map<String, Double> cases100k16Data = 
                    createDataDouble(cases100k16List);
            // create cases100k 15 days list
            List<StringDouble> cases100k15List = 
                createDayAveragesList(cases100kData, cases100k16Data, 15);
            // create cases100k 15 days ranks
            cases100k15Ranks = assignRanksDouble(cases100k15List);
            // create cases100k 15 days data
            cases100k15Data = createDataDouble(cases100k15List);
            // create case100k 15 days medians
            List<Double> cases100k15Medians = createMedians(cases100k15List);
            // create cases100k 15 days scores
            cases100k15Scores = createScores(cases100k15Medians, cases100k15List);
            // create cases100k 15 days grades
            cases100k15Grades = createGrades(cases100k15Scores);
        }
        // create deaths100k data
        Map<String, Double> deaths100kData = createDataDouble(deaths100kList);
        // create deaths100k 15 days data
        // Declare and initialize variables
        Map<String, Integer> deaths100k15Ranks = new HashMap<>();
        Map<String, Double> deaths100k15Data = new HashMap<>();
        Map<String, Integer> deaths100k15Scores = new HashMap<>();
        Map<String, String> deaths100k15Grades = new HashMap<>();
        if (deaths100k16List.size() > 0) {
            // convert deaths100k 16th day data
            Map<String, Double> deaths100k16Data = 
                    createDataDouble(deaths100k16List);
            // create deaths100k 15 days list
            List<StringDouble> deaths100k15List = createDayAveragesList(
                    deaths100kData, deaths100k16Data, 15);
            // create deaths100k 15 days ranks
            deaths100k15Ranks = assignRanksDouble(deaths100k15List);
            // create deaths100k 15 days data
            deaths100k15Data = createDataDouble(deaths100k15List);
            // create deaths100k 15 days medians
            List<Double> deaths100k15Medians = createMedians(deaths100k15List);
            // create deaths100k 15 days scores
            deaths100k15Scores = createScores(deaths100k15Medians, deaths100k15List);
            // create deaths100k 15 days grades
            deaths100k15Grades = createGrades(cases100k15Scores);
        }
        // create cases100k 30 days data
        // Declare and initialize variables
        Map<String, Double> cases100k30Data = new HashMap<>();
        Map<String, Integer> cases100k30Ranks = new HashMap<>();
        Map<String, Integer> cases100k30Scores = new HashMap<>();
        Map<String, String> cases100k30Grades = new HashMap<>();
        if (cases100k31List.size() > 0) {
            // convert cases100k 31st day data
            Map<String, Double> cases100k31Data = 
                    createDataDouble(cases100k31List);
            // create cases100k 30 days list
            List<StringDouble> cases100k30List = 
                createDayAveragesList(cases100kData, cases100k31Data, 30);
            // create cases100k 30 days ranks
            cases100k30Ranks = assignRanksDouble(cases100k30List);
            // create cases100k 30 days data
            cases100k30Data = createDataDouble(cases100k30List);
            // create case100k 30 days medians
            List<Double> cases100k30Medians = createMedians(cases100k30List);
            // create cases100k 30 days scores
            cases100k30Scores = createScores(cases100k30Medians, cases100k30List);
            // create cases100k 30 days grades
            cases100k30Grades = createGrades(cases100k30Scores);
        }
        // create deaths100k 30 days data
        // Declare and initialize variables
        Map<String, Integer> deaths100k30Ranks = new HashMap<>();
        Map<String, Double> deaths100k30Data = new HashMap<>();
        Map<String, Integer> deaths100k30Scores = new HashMap<>();
        Map<String, String> deaths100k30Grades = new HashMap<>();
        if (deaths100k31List.size() > 0) {
            // convert deaths100k 31st day data
            Map<String, Double> deaths100k31Data = 
                    createDataDouble(deaths100k31List);
            // create deaths100k 30 days list
            List<StringDouble> deaths100k30List = createDayAveragesList(
                    deaths100kData, deaths100k31Data, 30);
            // create deaths100k 30 days ranks
            deaths100k30Ranks = assignRanksDouble(deaths100k30List);
            // create deaths100k 30 days data
            deaths100k30Data = createDataDouble(deaths100k30List);
            // create deaths100k 30 days medians
            List<Double> deaths100k30Medians = createMedians(deaths100k30List);
            // create deaths100k 30 days scores
            deaths100k30Scores = createScores(deaths100k30Medians, deaths100k30List);
            // create deaths100k 30 days grades
            deaths100k30Grades = createGrades(cases100k30Scores);
        }
        // create overall scores
        List<StringInteger> overallScoresList = 
                createOverallScores(populationList, deaths100kScores, 
                    active100kScores, cases100k15Scores, deaths100k15Scores);
        // create overall score ranks
        Map<String, Integer> overallScoresRanks = 
                assignRanksInteger(overallScoresList);
        // create overall scores
        Map<String, Integer> overallScores = 
                createDataInteger(overallScoresList);
        // create overall Grades
        Map<String, String> overallGrades = createOverallGrades(overallScores);
        // create active100k data
        Map<String, Double> active100kData = createDataDouble(active100kList);
        // loop through populations and populate the data into calculations
        for (int i = 0; i < populationList.size(); i++) {
            // create a calculations class to hold data
            Calculations calc = new Calculations();
            // get country
            String country = populationList.get(i).getString();
            // get population
            long population = populationList.get(i).getValue();
            // set country
            calc.setCountry(country);
            // set date of data
            calc.setDate(date);
            // set population
            calc.setPopulation(populationData.get(country));
            // set population rank
            calc.setPopulationRank(populationRanks.get(country));
            // set percent of world population
            calc.setPercentPopulation(calculatePercent(population, 
                    worldData.getPopulation()));
            // set percent of mortality
            calc.setMortalityRate(calculatePercent(deathsData.get(country), 
                    casesData.get(country) - activeData.get(country)));
            // set percent of world deaths
            calc.setPercentDeaths(calculatePercent(deathsData.get(country), 
                    worldData.getDeaths()));
            // set percent of world active cases
            calc.setPercentActive(calculatePercent(activeData.get(country), 
                    worldData.getActive()));
            // set percent of recovered cases
            calc.setPercentRecovered(calculatePercent(casesData.get(country) - 
                    activeData.get(country) - deathsData.get(country), 
                    worldData.getRecovered()));
            // set percent of world total cases
            calc.setPercentCases(calculatePercent(casesData.get(country),  
                    worldData.getCases()));
            // set total cases
            calc.setTotalCases(casesData.get(country));
            // set new cases
            if (newCasesData.size() > 0 && newCasesData.containsKey(country)) {
                calc.setNewCases(newCasesData.get(country));
            }
            // set total deaths
            calc.setTotalDeaths(deathsData.get(country));
            // set new deaths
            if (newDeathsData.size() > 0 && newDeathsData.containsKey(country)) {
                calc.setNewDeaths(newDeathsData.get(country));
            }
            // set total active cases
            calc.setTotalActiveCases(activeData.get(country));
            // set deaths per 100,000 population
            calc.setDeaths100k(deaths100kData.get(country));
            // set deaths per 100,000 population rank
            calc.setDeaths100kRank(deaths100kRanks.get(country));
            // set deaths per 100,000 population score
            calc.setDeaths100kScore(deaths100kScores.get(country));
            // set deaths per 100,000 population grade
            calc.setDeaths100kGrade(deaths100kGrades.get(country));
            // set active cases per 100,000 population
            calc.setActive100k(active100kData.get(country));
            // set active cases per 100,ooo population rank
            calc.setActive100kRank(active100kRanks.get(country));
            // set active cases per 100,000 population score
            calc.setActive100kScore(active100kScores.get(country));
            // set active cases per 100,000 population grade
            calc.setActive100kGrade(active100kGrades.get(country));
            // set total cases per 100,000 population
            calc.setCases100k(cases100kData.get(country));
            // set total ceses per 100,000 population rank
            calc.setCases100kRank(cases100kRanks.get(country));
            // set total casres per 100,000 population score
            calc.setCases100kScore(cases100kScores.get(country));
            // set total casres per 100,000 population grade
            calc.setCases100kGrade(cases100kGrades.get(country));
            // add cases100k 15 day average data
            if (cases100k15Data.size() > 0  && 
                    cases100k15Data.containsKey(country)) {
                calc.setCases100k15(cases100k15Data.get(country));
            } 
            // add cases100k 15 day average rank data
            if (cases100k15Ranks.size() > 0  && 
                    cases100k15Ranks.containsKey(country)) {
                calc.setCases100k15Rank(cases100k15Ranks.get(country));
            } 
            // add cases100k 15 day average score data
            if (cases100k15Scores.size() > 0  && 
                    cases100k15Scores.containsKey(country)) {
                calc.setCases100k15Score(cases100k15Scores.get(country));
            }
            // add cases100k 15 day average grade data
            if (cases100k15Grades.size() > 0  && 
                    cases100k15Grades.containsKey(country)) {
                calc.setCases100k15Grade(cases100k15Grades.get(country));
            }
            // add deaths100k 15 day average data
            if (deaths100k15Data.size() > 0  && 
                    deaths100k15Data.containsKey(country)) {
                calc.setDeaths100k15(deaths100k15Data.get(country));
            }
            // add deaths100k 15 day average rank data
            if (deaths100k15Ranks.size() > 0  && 
                    deaths100k15Ranks.containsKey(country)) {
                calc.setDeaths100k15Rank(deaths100k15Ranks.get(country));
            }
            // add deaths100k 15 day average score data
            if (deaths100k15Scores.size() > 0  && 
                    deaths100k15Scores.containsKey(country)) {
                calc.setDeaths100k15Score(deaths100k15Scores.get(country));
            }
            // add deaths100k 15 day average grade data
            if (deaths100k15Grades.size() > 0  && 
                    deaths100k15Grades.containsKey(country)) {
                calc.setDeaths100k15Grade(deaths100k15Grades.get(country));
            }
            // add cases100k 1530 day average data
            if (cases100k30Data.size() > 0 && 
                    cases100k30Data.containsKey(country)) {
                calc.setCases100k30(cases100k30Data.get(country));
            }
            // add cases100k 30 day average rank data
            if (cases100k30Ranks.size() > 0 && 
                    cases100k30Ranks.containsKey(country)) {
                calc.setCases100k30Rank(cases100k30Ranks.get(country));
            }
            // add cases100k 30 day average score data
            if (cases100k30Scores.size() > 0 && 
                    cases100k30Scores.containsKey(country)) {
                calc.setCases100k30Score(cases100k30Scores.get(country));
            }
            // add cases100k 30 day average grade data
            if (cases100k30Grades.size() > 0 && 
                    cases100k30Grades.containsKey(country)) {
                calc.setCases100k30Grade(cases100k30Grades.get(country));
            }
            // add deaths100k 30 day average data
            if (deaths100k30Data.size() > 0 && 
                    deaths100k30Data.containsKey(country)) {
                calc.setDeaths100k30(deaths100k30Data.get(country));
            }
            // add deaths100k 30 day average rank data
            if (deaths100k30Ranks.size() > 0 && 
                    deaths100k30Ranks.containsKey(country)) {
                calc.setDeaths100k30Rank(deaths100k30Ranks.get(country));
            }
            // add deaths100k 30 day average score data
            if (deaths100k30Scores.size() > 0 && 
                    deaths100k30Scores.containsKey(country)) {
                calc.setDeaths100k30Score(deaths100k30Scores.get(country));
            }
            // add deaths100k 30 day average grade data
            if (deaths100k30Grades.size() > 0 && 
                    deaths100k30Grades.containsKey(country)) {
                calc.setDeaths100k30Grade(deaths100k30Grades.get(country));
            }
            // set overall rank
            calc.setRank(overallScoresRanks.get(country));
            // set overall score
            calc.setScore(overallScores.get(country));
            // set overall grade
            calc.setGrade(overallGrades.get(country));
            // add calculations to calculations list
            databaseUtilities.insertCalculation(conn, calc);
        }
    }
    
    /**
     * Method to create the data points for the front end
     * @param conn connection to the database
     * @param date of the data
     */
    public void calculateStateTotal(Connection conn, java.sql.Date date) {
        // get data from database
        USAData usaData = databaseUtilities.getUSAData(conn, date);
        // get cases from database
        Map<String, Long> casesData = 
                databaseUtilities.getStateCasesData(conn, date);
        // get new cases from database
        Map<String, Long> newCasesData = 
                databaseUtilities.getNewStateCasesData(conn, date);
        // get deaths from database
        Map<String, Long> deathsData = 
                databaseUtilities.getStateDeathsData(conn, date);
        // get new deaths from database
        Map<String, Long> newDeathsData = 
                databaseUtilities.getNewStateDeathsData(conn, date);
        // get active from database
        Map<String, Long> activeData = 
                databaseUtilities.getStateActiveData(conn, date);
        // get population list from database
        List<StringLong> populationList = 
                databaseUtilities.getStatePopulationData(conn, date);
        // get cases100k list from database
        List<StringDouble> cases100kList = 
                databaseUtilities.getStateCases100kData(conn, date);
        // get cases100k16 list from database
        List<StringDouble> cases100k16List = 
                databaseUtilities.getStateCases100kData16(conn, date);
        List<StringDouble> cases100k31List = 
                databaseUtilities.getStateCases100kData31(conn, date);
        // get deaths100k from database
        List<StringDouble> deaths100kList = 
                databaseUtilities.getStateDeaths100kData(conn, date);
        // get deaths100k16 from database
        List<StringDouble> deaths100k16List = 
                databaseUtilities.getStateDeaths100kData16(conn, date);
        // get deaths100k16 from database
        List<StringDouble> deaths100k31List = 
                databaseUtilities.getStateDeaths100kData31(conn, date);
        // get active100k from database
        List<StringDouble> active100kList = 
                databaseUtilities.getStateActive100kData(conn, date);
        // create population ranks
        Map<String, Integer> populationRanks = 
                assignRanksLong(populationList);
        // create cases100k ranks
        Map<String, Integer> cases100kRanks = 
                assignRanksDouble(cases100kList);
        // create deaths100k ranks
        Map<String, Integer> deaths100kRanks = 
                assignRanksDouble(deaths100kList);
        // create active100k ranks
        Map<String, Integer> active100kRanks = 
                assignRanksDouble(active100kList);
        // create case100k medians
        List<Double> cases100kMedians = createMedians(cases100kList);
        // create cases100k scores
        Map<String, Integer> cases100kScores = 
                createScores(cases100kMedians, cases100kList);
        // create cases100k grades
        Map<String, String> cases100kGrades = createGrades(cases100kScores);
        // create death100k medians
        List<Double> deaths100kMedians = createMedians(deaths100kList);
        // create deaths100k scores
        Map<String, Integer> deaths100kScores = 
                createScores(deaths100kMedians, deaths100kList);
        Map<String, String> deaths100kGrades = createGrades(deaths100kScores);
        // create active100k medians
        List<Double> active100kMedians = createMedians(active100kList);
        // create active100k scores
        Map<String, Integer> active100kScores = 
                createScores(active100kMedians, active100kList);
        // create active100k grades
        Map<String, String> active100kGrades = createGrades(active100kScores);
        // create populations data
        Map<String, Long> populationData = createDataLong(populationList);
        // create cases100k data
        Map<String, Double> cases100kData = createDataDouble(cases100kList);
        // create cases100k 15 days data
        // Declare and initialize variables
        Map<String, Double> cases100k15Data = new HashMap<>();
        Map<String, Integer> cases100k15Ranks = new HashMap<>();
        Map<String, Integer> cases100k15Scores = new HashMap<>();
        Map<String, String> cases100k15Grades = new HashMap<>();
        if (cases100k16List.size() > 0) {
            // convert cases100k 16th day data
            Map<String, Double> cases100k16Data = 
                    createDataDouble(cases100k16List);
            // create cases100k 15 days list
            List<StringDouble> cases100k15List = 
                createDayAveragesList(cases100kData, cases100k16Data, 15);
            // create cases100k 15 days ranks
            cases100k15Ranks = assignRanksDouble(cases100k15List);
            // create cases100k 15 days data
            cases100k15Data = createDataDouble(cases100k15List);
            // create case100k 15 days medians
            List<Double> cases100k15Medians = createMedians(cases100k15List);
            // create cases100k 15 days scores
            cases100k15Scores = createScores(cases100k15Medians, cases100k15List);
            // create cases100k 15 days grades
            cases100k15Grades = createGrades(cases100k15Scores);
        }
        // create deaths100k data
        Map<String, Double> deaths100kData = createDataDouble(deaths100kList);
        // create deaths100k 15 days data
        // Declare and initialize variables
        Map<String, Integer> deaths100k15Ranks = new HashMap<>();
        Map<String, Double> deaths100k15Data = new HashMap<>();
        Map<String, Integer> deaths100k15Scores = new HashMap<>();
        Map<String, String> deaths100k15Grades = new HashMap<>();
        if (deaths100k16List.size() > 0) {
            // convert deaths100k 16th day data
            Map<String, Double> deaths100k16Data = 
                    createDataDouble(deaths100k16List);
            // create deaths100k 15 days list
            List<StringDouble> deaths100k15List = createDayAveragesList(
                    deaths100kData, deaths100k16Data, 15);
            // create deaths100k 15 days ranks
            deaths100k15Ranks = assignRanksDouble(deaths100k15List);
            // create deaths100k 15 days data
            deaths100k15Data = createDataDouble(deaths100k15List);
            // create deaths100k 15 days medians
            List<Double> deaths100k15Medians = createMedians(deaths100k15List);
            // create deaths100k 15 days scores
            deaths100k15Scores = createScores(deaths100k15Medians, deaths100k15List);
            // create deaths100k 15 days grades
            deaths100k15Grades = createGrades(cases100k15Scores);
        }
        // create cases100k 30 days data
        // Declare and initialize variables
        Map<String, Double> cases100k30Data = new HashMap<>();
        Map<String, Integer> cases100k30Ranks = new HashMap<>();
        Map<String, Integer> cases100k30Scores = new HashMap<>();
        Map<String, String> cases100k30Grades = new HashMap<>();
        if (cases100k31List.size() > 0) {
            // convert cases100k 31st day data
            Map<String, Double> cases100k31Data = 
                    createDataDouble(cases100k31List);
            // create cases100k 30 days list
            List<StringDouble> cases100k30List = 
                createDayAveragesList(cases100kData, cases100k31Data, 30);
            // create cases100k 30 days ranks
            cases100k30Ranks = assignRanksDouble(cases100k30List);
            // create cases100k 30 days data
            cases100k30Data = createDataDouble(cases100k30List);
            // create case100k 30 days medians
            List<Double> cases100k30Medians = createMedians(cases100k30List);
            // create cases100k 30 days scores
            cases100k30Scores = createScores(cases100k30Medians, cases100k30List);
            // create cases100k 30 days grades
            cases100k30Grades = createGrades(cases100k30Scores);
        }
        // create deaths100k 30 days data
        // Declare and initialize variables
        Map<String, Integer> deaths100k30Ranks = new HashMap<>();
        Map<String, Double> deaths100k30Data = new HashMap<>();
        Map<String, Integer> deaths100k30Scores = new HashMap<>();
        Map<String, String> deaths100k30Grades = new HashMap<>();
        if (deaths100k31List.size() > 0) {
            // convert deaths100k 31st day data
            Map<String, Double> deaths100k31Data = 
                    createDataDouble(deaths100k31List);
            // create deaths100k 30 days list
            List<StringDouble> deaths100k30List = createDayAveragesList(
                    deaths100kData, deaths100k31Data, 30);
            // create deaths100k 30 days ranks
            deaths100k30Ranks = assignRanksDouble(deaths100k30List);
            // create deaths100k 30 days data
            deaths100k30Data = createDataDouble(deaths100k30List);
            // create deaths100k 30 days medians
            List<Double> deaths100k30Medians = createMedians(deaths100k30List);
            // create deaths100k 30 days scores
            deaths100k30Scores = createScores(deaths100k30Medians, deaths100k30List);
            // create deaths100k 30 days grades
            deaths100k30Grades = createGrades(cases100k30Scores);
        }
        // create overall scores
        List<StringInteger> overallScoresList = 
                createOverallScores(populationList, deaths100kScores, 
                    active100kScores, cases100k15Scores, deaths100k15Scores);
        // create overall score ranks
        Map<String, Integer> overallScoresRanks = 
                assignRanksInteger(overallScoresList);
        // create overall scores
        Map<String, Integer> overallScores = 
                createDataInteger(overallScoresList);
        // create overall Grades
        Map<String, String> overallGrades = createOverallGrades(overallScores);
        // create active100k data
        Map<String, Double> active100kData = createDataDouble(active100kList);
        // loop through populations and populate the data into calculations
        for (int i = 0; i < populationList.size(); i++) {
            // create a calculations class to hold data
            Calculations calc = new Calculations();
            // get country
            String state = populationList.get(i).getString();
            // get population
            long population = populationList.get(i).getValue();
            // set country
            calc.setCountry(state);
            // set date of data
            calc.setDate(date);
            // set population
            calc.setPopulation(populationData.get(state));
            // set population rank
            calc.setPopulationRank(populationRanks.get(state));
            // set percent of world population
            calc.setPercentPopulation(calculatePercent(population, 
                    usaData.getPopulation()));
            // set percent of mortality
            calc.setMortalityRate(calculatePercent(deathsData.get(state), 
                    casesData.get(state) - activeData.get(state)));
            // set percent of world deaths
            calc.setPercentDeaths(calculatePercent(deathsData.get(state), 
                    usaData.getDeaths()));
            // set percent of world active cases
            calc.setPercentActive(calculatePercent(activeData.get(state), 
                    usaData.getActive()));
            // set percent of recovered cases
            calc.setPercentRecovered(calculatePercent(casesData.get(state) - 
                    activeData.get(state) - deathsData.get(state), 
                    usaData.getRecovered()));
            // set percent of world total cases
            calc.setPercentCases(calculatePercent(casesData.get(state),  
                    usaData.getCases()));
            // set total cases
            calc.setTotalCases(casesData.get(state));
            // set new cases
            if (newCasesData.size() > 0 && newCasesData.containsKey(state)) {
                calc.setNewCases(newCasesData.get(state));
            }
            // set total deaths
            calc.setTotalDeaths(deathsData.get(state));
            // set new deaths
            if (newDeathsData.size() > 0 && newDeathsData.containsKey(state)) {
                calc.setNewDeaths(newDeathsData.get(state));
            }
            // set total active cases
            calc.setTotalActiveCases(activeData.get(state));
            // set deaths per 100,000 population
            calc.setDeaths100k(deaths100kData.get(state));
            // set deaths per 100,000 population rank
            calc.setDeaths100kRank(deaths100kRanks.get(state));
            // set deaths per 100,000 population score
            calc.setDeaths100kScore(deaths100kScores.get(state));
            // set deaths per 100,000 population grade
            calc.setDeaths100kGrade(deaths100kGrades.get(state));
            // set active cases per 100,000 population
            calc.setActive100k(active100kData.get(state));
            // set active cases per 100,ooo population rank
            calc.setActive100kRank(active100kRanks.get(state));
            // set active cases per 100,000 population score
            calc.setActive100kScore(active100kScores.get(state));
            // set active cases per 100,000 population grade
            calc.setActive100kGrade(active100kGrades.get(state));
            // set total cases per 100,000 population
            calc.setCases100k(cases100kData.get(state));
            // set total ceses per 100,000 population rank
            calc.setCases100kRank(cases100kRanks.get(state));
            // set total casres per 100,000 population score
            calc.setCases100kScore(cases100kScores.get(state));
            // set total casres per 100,000 population grade
            calc.setCases100kGrade(cases100kGrades.get(state));
            // add cases100k 15 day average data
            if (cases100k15Data.size() > 0  && 
                    cases100k15Data.containsKey(state)) {
                calc.setCases100k15(cases100k15Data.get(state));
            } 
            // add cases100k 15 day average rank data
            if (cases100k15Ranks.size() > 0  && 
                    cases100k15Ranks.containsKey(state)) {
                calc.setCases100k15Rank(cases100k15Ranks.get(state));
            } 
            // add cases100k 15 day average score data
            if (cases100k15Scores.size() > 0  && 
                    cases100k15Scores.containsKey(state)) {
                calc.setCases100k15Score(cases100k15Scores.get(state));
            }
            // add cases100k 15 day average grade data
            if (cases100k15Grades.size() > 0  && 
                    cases100k15Grades.containsKey(state)) {
                calc.setCases100k15Grade(cases100k15Grades.get(state));
            }
            // add deaths100k 15 day average data
            if (deaths100k15Data.size() > 0  && 
                    deaths100k15Data.containsKey(state)) {
                calc.setDeaths100k15(deaths100k15Data.get(state));
            }
            // add deaths100k 15 day average rank data
            if (deaths100k15Ranks.size() > 0  && 
                    deaths100k15Ranks.containsKey(state)) {
                calc.setDeaths100k15Rank(deaths100k15Ranks.get(state));
            }
            // add deaths100k 15 day average score data
            if (deaths100k15Scores.size() > 0  && 
                    deaths100k15Scores.containsKey(state)) {
                calc.setDeaths100k15Score(deaths100k15Scores.get(state));
            }
            // add deaths100k 15 day average grade data
            if (deaths100k15Grades.size() > 0  && 
                    deaths100k15Grades.containsKey(state)) {
                calc.setDeaths100k15Grade(deaths100k15Grades.get(state));
            }
            // add cases100k 1530 day average data
            if (cases100k30Data.size() > 0 && 
                    cases100k30Data.containsKey(state)) {
                calc.setCases100k30(cases100k30Data.get(state));
            }
            // add cases100k 30 day average rank data
            if (cases100k30Ranks.size() > 0 && 
                    cases100k30Ranks.containsKey(state)) {
                calc.setCases100k30Rank(cases100k30Ranks.get(state));
            }
            // add cases100k 30 day average score data
            if (cases100k30Scores.size() > 0 && 
                    cases100k30Scores.containsKey(state)) {
                calc.setCases100k30Score(cases100k30Scores.get(state));
            }
            // add cases100k 30 day average grade data
            if (cases100k30Grades.size() > 0 && 
                    cases100k30Grades.containsKey(state)) {
                calc.setCases100k30Grade(cases100k30Grades.get(state));
            }
            // add deaths100k 30 day average data
            if (deaths100k30Data.size() > 0 && 
                    deaths100k30Data.containsKey(state)) {
                calc.setDeaths100k30(deaths100k30Data.get(state));
            }
            // add deaths100k 30 day average rank data
            if (deaths100k30Ranks.size() > 0 && 
                    deaths100k30Ranks.containsKey(state)) {
                calc.setDeaths100k30Rank(deaths100k30Ranks.get(state));
            }
            // add deaths100k 30 day average score data
            if (deaths100k30Scores.size() > 0 && 
                    deaths100k30Scores.containsKey(state)) {
                calc.setDeaths100k30Score(deaths100k30Scores.get(state));
            }
            // add deaths100k 30 day average grade data
            if (deaths100k30Grades.size() > 0 && 
                    deaths100k30Grades.containsKey(state)) {
                calc.setDeaths100k30Grade(deaths100k30Grades.get(state));
            }
            // set overall rank
            calc.setRank(overallScoresRanks.get(state));
            // set overall score
            calc.setScore(overallScores.get(state));
            // set overall grade
            calc.setGrade(overallGrades.get(state));
            // add calculations to calculations list
            databaseUtilities.insertStateCalculation(conn, calc);
        }
    }
    
    /**
     * Method to create a list of an average of ? days of data
     * @param currentList of data
     * @param oldMap of data number of days plus 1
     * @param days to average
     * @return sorted list of of country and double values
     */
    private List<StringDouble> createDayAveragesList(
            Map<String, Double> currentList, Map<String, Double> oldMap, 
            int days) {
        List<StringDouble> resultList = new ArrayList<>();
        currentList.entrySet().stream().map(entry -> {
            double temp = 0.0;
            if (oldMap.containsKey(entry.getKey()))
                temp = calculateAverage(entry.getValue() - 
                        oldMap.get(entry.getKey()), days);
            if (temp < 0.0) {
                temp = 0.0;
            }
            StringDouble listEntry = new StringDouble(entry.getKey(), temp);
            return listEntry;
        }).forEachOrdered(listEntry -> {
            resultList.add(listEntry);
        }); 
        Collections.sort(resultList);
        return resultList;
    }
    
    /**
     * Method to calculate a percentage with 2 decimal places
     * @param number1 dividend
     * @param number2 divisor
     * @return 
     */
    private double calculatePercent(long number1, long number2) {
        // calculate percentage
        if (number2 == 0) {
            return 0.0;
        }
        double tempDouble = ((double) number1 / number2) * 100;
        // convert to string
        String tempString = String.format(Locale.getDefault(), "%.5f", tempDouble);
        // convert string to double and return
        Double tempDouble1 = Double.valueOf(tempString);
        return tempDouble1;
    }
    
    /**
     * Method to calculate a percentage with 2 decimal places
     * @param number1 dividend
     * @param number2 divisor
     * @return 
     */
    private double calculateAverage(double number, int quantity) {
        // calculate average
        if (number == 0 || quantity == 0) {
            return 0.0;
        }
        double tempDouble = number / quantity;
        // convert to string
        String tempString = String.format(Locale.getDefault(), "%.5f", tempDouble);
        // convert string to double and return
        Double tempDouble1 = Double.valueOf(tempString);
        return tempDouble1;
    }
    
    /**
     * Method to calculate a percentage with 2 decimal places
     * @param number1 dividend
     * @param number2 divisor
     * @return 
     */
    private double calculatePercent(double number) {
        // convert to string
        String tempString = String.format(Locale.getDefault(), "%.5f", number);
        // convert string to double and return
        Double tempDouble1 = Double.valueOf(tempString);
        return tempDouble1;
    }
    
    /**
     * Method to create overall ranks
     * @param list to use
     * @param rank1 to add
     * @param rank2 to add
     * @param rank3 to add
     * @return overall ranks
     */
    private Map<String, Integer> createOverallRanks (List<StringLong> list, 
            Map<String, Integer> rank1, Map<String, Integer> rank2, 
            Map<String, Integer> rank3) {
        // Declare country ranks map
        Map<String, Integer> countryRanks = new HashMap<>();
        // declare rank list
        List<Integer> rankList = new ArrayList<>();
        // declare rank data map
        Map<String, Integer> rankData = new HashMap<>();
        // loop through all countries
        for (int i = 0; i < list.size(); i++) {
            // get the country
            String country = list.get(i).getString();
            // initialize and get all ranks
            int allRanks = rank1.get(country) + rank2.get(country) + 
                    rank3.get(country);
            rankData.put(country, allRanks);
            rankList.add(allRanks);
        }
        Collections.sort(rankList);
        Map<Integer, Integer> ranks = new HashMap<>();
        for (int i = 0; i < rankList.size(); i++) {
            ranks.put(rankList.get(i), i + 1);
        }
        
        for (int i = 0; i < list.size(); i++) {
            countryRanks.put(list.get(i).getString(), 
                    ranks.get(rankData.get(list.get(i).getString())));
        }
        return countryRanks;
    }
    
    /**
     * Method to create overall score and return in descending sorted country 
     * double list
     * @param list with countries to iterate through
     * @param score1 to use to create overall score
     * @param score2 to use to create overall score
     * @param score3 to use to create overall score
     * @param score4 to use to create overall score
     * @return sorted country scores list
     */
    private List<StringInteger> createOverallScores(List<StringLong> list, 
            Map<String, Integer> score1, Map<String, Integer> score2, 
            Map<String, Integer> score3, Map<String, Integer> score4)  {
        // declare variables
        List<StringInteger> countryScoresList = new ArrayList<>();
        Map<String, Integer> scoresMap = new HashMap<>();
        List<Integer> scoresList = new ArrayList<>();
        // loop through countries
        for (int i = 0; i < list.size(); i++) {
            // get country
              String country = list.get(i).getString();
              // calculate overall score
              double count = 4.0;
              int scores = score1.get(country) + score2.get(country);
              if (score3.isEmpty()) {
                  count--;
              } else {
                  scores += score3.get(country);
              }
              if (score4.isEmpty()) {
                  count--;
              } else {
                  scores += score4.get(country);
              }
              int score = (int) round(scores / count);
              // check score
              if (score < 1 || score > 13) {
                  score = 13;
              }
              // put country and overall score in map
              scoresMap.put(country, score);
              // put score in list to be reordered
              scoresList.add(score);
        }
        Collections.sort(scoresList);
        // get  value higher than highest score
        double score = scoresList.get(scoresList.size() - 1) + 1;
        // loop through sorted score list in reverse order
        for(int i = scoresList.size() - 1; i >= 0; i--) {
            if (score != scoresList.get(i)) {
                score = scoresList.get(i);
                // find all matching scores
                for (Map.Entry<String, Integer> entry : scoresMap.entrySet()) {
                    if (entry.getValue() == score) {
                        // add entry to country scores list
                        countryScoresList.add(new StringInteger(entry.getKey(), 
                                entry.getValue()));
                    }
                }
            }
        }
        // return sorted country scores list
        return countryScoresList;
    }
    
    /**
     * Method to create overall grades
     * @param scoresMap to use
     * @return map of grades
     */
    private Map<String, String> createOverallGrades(
            Map<String, Integer> scoresMap) {
        // Declare and initialize variable
        Map<String, String> grades = new HashMap<>();
        scoresMap.entrySet().forEach(entry -> {
            // add entry to grades
            grades.put(entry.getKey(), getGrade(entry.getValue()));
        });
        // return data map
        return grades;
    }
     
//    /**
//     * Method to get the value of a score
//     * @param score to get value of
//     * @return value of the score
//     */
//    private double getScoreValue(String score) {
//        switch (score) {
//            case "A+" :
//                return 4.33;
//            case "A" :
//                return 4.0;
//            case "A-" :
//                return 3.67;
//            case "B+" :
//                return 3.33;
//            case "B" :
//                return 3.0;
//            case "B-" :
//                return 2.67;
//            case "C+" :
//                return 2.33;
//            case "C" :
//                return 2.0;
//            case "C-" :
//                return 1.67;
//            case "D+" :
//                return 1.33;
//            case "D" :
//                return 1.0;
//            case "D-" :
//                return 0.67;
//            default:
//                return 0.0;
//        }
//    }
    
//    /**
//     * Method to get score from a value
//     * @param value to get score for
//     * @return score of the value
//     */
//    private String getScore(double value) {
//        if (value > 4.0) {
//            return "A+";
//        } else if (value > 3.67) {
//            return "A";
//        } else if (value > 3.33) {
//            return "A-";
//        } else if (value > 3.0) {
//            return "B+";
//        } else if (value > 2.67) {
//            return "B";
//        } else if (value > 2.33) {
//            return "B-";
//        } else if (value > 2.0) {
//            return "C+";
//        } else if (value > 1.67) {
//            return "C";
//        } else if (value > 1.33) {
//            return "C-";
//        } else if (value > 1.0) {
//            return "D+";
//        } else if (value > 0.67) {
//            return "D";
//        } else if (value > 0.33) {
//            return "D-";
//        } 
//        return "F";
//    }
    
    /**
     * Method to assign ranks of long values
     * @param list of values
     * @return assigned ranks
     */
    private Map<String, Integer> assignRanksLong(List<StringLong> list) {
        // Declare values;
        int rank = 0;
        long value = -1;
        Map<String, Integer> ranks = new HashMap<>();
        for(int i = 0; i < list.size(); i++) {
            if (list.get(i).getValue() != value) {
                rank = i + 1;
                value = list.get(i).getValue(); 
            }
            ranks.put(list.get(i).getString(), rank);
        }
        return ranks;
    }
    
    /**
     * Method to assign ranks of long values
     * @param list of values
     * @return assigned ranks
     */
    private Map<String, Integer> assignRanksInteger(List<StringInteger> list) {
        // Declare values;
        int rank = 0;
        int value = -1;
        Map<String, Integer> ranks = new HashMap<>();
        for(int i = 0; i < list.size(); i++) {
            if (list.get(i).getValue() != value) {
                rank = i + 1;
                value = list.get(i).getValue(); 
            }
            ranks.put(list.get(i).getString(), rank);
        }
        return ranks;
    }
    
    /**
     * Method to assign ranks of double values ascending
     * @param list of values
     * @return ranks
     */
    private Map<String, Integer> assignRanksDouble(List<StringDouble> list) {
        // declare variables
        int rank = 0;
        double value = -1.0;
        Map<String, Integer> ranks = new HashMap<>();
        for(int i = 0; i < list.size(); i++) {
            if (list.get(i).getValue() != value) {
                rank = i + 1;
                value = list.get(i).getValue(); 
            }
            ranks.put(list.get(i).getString(), rank);
        }
        return ranks;
    }
    
//    /**
//     * Method to create median for the scores
//     * @param list to generate medians from ascending
//     * @return Map of medians
//     */
//    private Map<String, Double> createMediansAsc(List<CountryDouble> list) {
//        Map<String, Double> medians = new HashMap<>();
//        // Get given ranks A+ and F
//        double step = (list.get(list.size() - 1).getValue() - 
//                list.get(0).getValue()) / 13;
//        double current = calculatePercent(list.get(0).getValue());
//        medians.put("A+", current);
//        // calculate A
//        current = calculatePercent(current + step);
//        medians.put("A", current);
//        // calculate A-
//        current = calculatePercent(current + step);
//        medians.put("A-", current);
//        // calculate B+
//        current = calculatePercent(current + step);
//        medians.put("B+", current);
//        // calculate B
//        current = calculatePercent(current + step);
//        medians.put("B", current);
//        // calculate B-
//        current = calculatePercent(current + step);
//        medians.put("B-", current);
//        // calculate C+
//        current = calculatePercent(current + step);
//        medians.put("C+", current);
//        // calculate C
//        current = calculatePercent(current + step);
//        medians.put("C", current);
//        // calculate C-
//        current = calculatePercent(current + step);
//        medians.put("C-", current);
//        // calculate D+
//        current = calculatePercent(current + step);
//        medians.put("D+", current);
//        // calculate D
//        current = calculatePercent(current + step);
//        medians.put("D", current);
//        // calculate D-
//        current = calculatePercent(current + step);
//        medians.put("D-", current);
//        // calculate F
//        current = calculatePercent(current + step);
//        medians.put("F", current);
//        return medians;
//    }
//    
//    /**
//     * Method to create median for the scores
//     * @param list to generate medians from descending
//     * @return Map of medians
//     */
//    private Map<String, Double> createMediansDesc(List<CountryDouble> list) {
//        Map<String, Double> medians = new HashMap<>();
//        // Get given ranks A+ and F
//        double step = (list.get(0).getValue() - 
//                list.get(list.size() - 1).getValue()) / 13;
//        double current = calculatePercent(list.get(0).getValue());
//        medians.put("A+", current);
//        // calculate A
//        current = calculatePercent(current - step);
//        medians.put("A", current);
//        // calculate A-
//        current = calculatePercent(current - step);
//        medians.put("A-", current);
//        // calculate B+
//        current = calculatePercent(current - step);
//        medians.put("B+", current);
//        // calculate B
//        current = calculatePercent(current - step);
//        medians.put("B", current);
//        // calculate B-
//        current = calculatePercent(current - step);
//        medians.put("B-", current);
//        // calculate C+
//        current = calculatePercent(current - step);
//        medians.put("C+", current);
//        // calculate C
//        current = calculatePercent(current - step);
//        medians.put("C", current);
//        // calculate C-
//        current = calculatePercent(current - step);
//        medians.put("C-", current);
//        // calculate D+
//        current = calculatePercent(current - step);
//        medians.put("D+", current);
//        // calculate D
//        current = calculatePercent(current - step);
//        medians.put("D", current);
//        // calculate D-
//        current = calculatePercent(current - step);
//        medians.put("D-", current);
//        // calculate F
//        current = calculatePercent(current - step);
//        medians.put("F", current);
//        return medians;
//    }
    
    /**
     * Method to create scores
     * @param medians to use
     * @param list of values
     * @return scores
     */
    private Map<String, Integer> createScores(List<Double> medians, 
            List<StringDouble> list) {
        // Declare and initialize variable
        Map<String, Integer> scores = new HashMap<>();
        
        for (int i = 0; i < list.size(); i++) {
            // get country and score to put in Map
            scores.put(list.get(i).getString(), 
                    getScore(medians, list.get(i).getValue()));
        }
        // return map of scores
        return scores;
    }
    
    /**
     * Method to create grades for a list
     * @param scores to convert to grades
     * @return grades
     */
    private Map<String, String> createGrades(Map<String, Integer> scores) {
        // Declare and initialize variable
        Map<String, String> grades = new HashMap<>();
        // Put country and grade into Map
        scores.entrySet().forEach(entry -> {
            // get country and grade to put in Map
            grades.put(entry.getKey(), getGrade(entry.getValue()));
        });
        // return map of grades
        return grades;
    }
    
//    /**
//     * Method to create scores
//     * @param medians to use
//     * @param list of values
//     * @return scores
//     */
//    private Map<String, String> createScoresAsc(Map<String, Double> medians, 
//            List<CountryDouble> list) {
//        Map<String, String> scores = new HashMap<>();
//        
//        for (int i = 0; i < list.size(); i++) {
//            scores.put(list.get(i).getCountry(), 
//                    getScoreAsc(medians, list.get(i).getValue()));
//        }
//        return scores;
//    }
    
    /**
     * Method to get a score
     * @param medians to use to find score
     * @param value to get score for
     * @return score
     */
    private int getScore(List<Double> medians, double value) {
        int score = 13;
        
        for (int i = 0; i < medians.size(); i++) {
            if (value < medians.get(i)) {
                score = i + 1;
                return score;
            }
        }
        return score;
    }
    
    /**
     * Method to get a grade from a value
     * @param value to get grade for
     * @return grade
     */
    private String getGrade(int value) {
        switch (value) {
            case 1:
                return "A+";
            case 2:
                return "A";
            case 3:
                return "A-";
            case 4:
                return "B+";
            case 5:
                return "B";
            case 6:
                return "B-";
            case 7:
                return "C+";
            case 8:
                return "C";
            case 9:
                return "C-";
            case 10:
                return "D+";
            case 11:
                return "D";
            case 12:
                return "D-";
            case 13:
                return "F";
            default:
                return "ER";
        }
    }
    
//    /**
//     * Method to get a score
//     * @param medians to use to find score
//     * @param value to get score for
//     * @return score
//     */
//    private String getScoreAsc(Map<String, Double> medians, double value) {
//        String score;
//        
//        if (medians.get("A") > value) {
//            score = "A+";
//        } else if(medians.get("A-") > value) {
//            score = "A";
//        } else if(medians.get("B+") > value) {
//            score = "A-";
//        } else if(medians.get("B") > value) {
//            score = "B+";
//        } else if(medians.get("B-") > value) {
//            score = "B";
//        } else if(medians.get("C+") > value) {
//            score = "B-";
//        } else if(medians.get("C") > value) {
//            score = "C+";
//        } else if(medians.get("C-") > value) {
//            score = "C";
//        } else if(medians.get("D+") > value) {
//            score = "C-";
//        } else if(medians.get("D") > value) {
//            score = "D+";
//        } else if(medians.get("D-") > value) {
//            score = "D";
//        } else if(medians.get("F") > value) {
//            score = "D-";
//        } else {
//            score = "F";
//        }
//        return score;
//    }
//    
//    /**
//     * Method to create scores
//     * @param medians to use
//     * @param list of values
//     * @return scores
//     */
//    private Map<String, String> createScoresDesc(Map<String, Double> medians, 
//            List<CountryDouble> list) {
//        Map<String, String> scores = new HashMap<>();
//        
//        for (int i = 0; i < list.size(); i++) {
//            scores.put(list.get(i).getCountry(), 
//                    getScoreDesc(medians, list.get(i).getValue()));
//        }
//        return scores;
//    }
    
//    /**
//     * Method to create scores
//     * @param list of values
//     * @return scores
//     */
//    private Map<String, String> createRecoveredPercentScores(
//            List<CountryDouble> list) {
//        Map<String, String> scores = new HashMap<>();
//        String score;
//        
//        for (int i = 0; i < list.size(); i++) {
//            double value = list.get(i).getValue();
//            if (value > 96.0) {
//                score = "A+";
//            } else if (value > 92.0) {
//                score = "A";
//            } else if (value > 86.0) {
//                score = "A-";
//            } else if (value > 82.0) {
//                score = "B+";
//            } else if (value > 78.0) {
//                score = "B";
//            } else if (value > 74.0) {
//                score = "B-";
//            } else if (value > 70.0) {
//                score = "C+";
//            } else if (value > 66.0) {
//                score = "C";
//            } else if (value > 62.0) {
//                score = "C-";
//            } else if (value > 58.0) {
//                score = "D+";
//            } else if (value > 54.0) {
//                score = "D";
//            } else if (value > 50.0) {
//                score = "D-";
//            } else {
//                score = "F";
//            }
//            scores.put(list.get(i).getCountry(), score);
//        }
//        return scores;
//    }
//    
//    /**
//     * Method to get a score
//     * @param medians to use to find score
//     * @param value to get score for
//     * @return score
//     */
//    private String getScoreDesc(Map<String, Double> medians, double value) {
//        String score;
//        
//        if (medians.get("A") < value) {
//            score = "A+";
//        } else if(medians.get("A-") < value) {
//            score = "A";
//        } else if(medians.get("B+") < value) {
//            score = "A-";
//        } else if(medians.get("B") < value) {
//            score = "B+";
//        } else if(medians.get("B-") < value) {
//            score = "B";
//        } else if(medians.get("C+") < value) {
//            score = "B-";
//        } else if(medians.get("C") < value) {
//            score = "C+";
//        } else if(medians.get("C-") < value) {
//            score = "C";
//        } else if(medians.get("D+") < value) {
//            score = "C-";
//        } else if(medians.get("D") < value) {
//            score = "D+";
//        } else if(medians.get("D-") < value) {
//            score = "D";
//        } else if(medians.get("F") < value) {
//            score = "D-";
//        } else {
//            score = "F";
//        }
//        return score;
//    }
    
    /**
     * Method to create a long data Map
     * @param list of data
     * @return map of data
     */
    private Map<String, Long> createDataLong (List<StringLong> list) {
        // Declare and initialize variable
        Map<String, Long> data = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            // add data to map
            data.put(list.get(i).getString(), list.get(i).getValue());
        }
        // return data map
        return data;
    }
    
    /**
     * Method to create a integer data Map
     * @param list of data
     * @return map of data
     */
    private Map<String, Integer> createDataInteger (List<StringInteger> list) {
        // Declare and initialize variable
        Map<String, Integer> data = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            // add data to map
            data.put(list.get(i).getString(), list.get(i).getValue());
        }
        // return data map
        return data;
    }
    
    /**
     * Method to create a double data map
     * @param list of data
     * @return Map of data
     */
    private Map<String, Double> createDataDouble (List<StringDouble> list) {
        // Declare and initialize variable
        Map<String, Double> data = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            // add data to map
            data.put(list.get(i).getString(), list.get(i).getValue());
        }
        // return data map
        return data;
    }
    
    /**
     * Method to create state dailies
     */
    public void createStateDailies() {
        // declare and initialize variable
        Connection conn = getDatabaseConnection();
        // insert data in total table in database
        databaseUtilities.createStateDailies(conn);
        // close connection
        databaseUtilities.closeConnection(conn);                 
    }
    
    /**
     * Method to create country dailies
     */
    public void createCountryDailies() {
        Connection conn = getDatabaseConnection();
        // insert data in total table in database
        databaseUtilities.createCountryDailies(conn);
        // close database
        databaseUtilities.closeConnection(conn);
    }
    
    /**
     * Method to process world-o-meter scrape to enter data into a database
     */
    public void processWorldometerScrape(){
        mResults.addResults("Starting World o meter scrape");
        // declare and initialize variable
        Connection conn = getDatabaseConnection();
        
        mResults.addResults(" United States Results");
        processUnitedStatesScrape(conn);
        mResults.addResults("\n\n World Results");
        processWorldScrape(conn);
        mResults.addResults("Completed World o meter scrape");
        // close connection
        databaseUtilities.closeConnection(conn);
        mResults.addResults("\n\n Get Stat Totals");
        getStatData();
        mResults.addResults("Completed Stat Totals");
        mResults.addResults("\n\n Calculate Totals");
        // declare and initialize variable
        conn = getDatabaseConnection();
        calculateTotal(conn, YESTERDAY_DATE);
        // close connection
        databaseUtilities.closeConnection(conn);
        mResults.addResults("Completed Calaulate Totals");
    }
    
    /**
     * Method to get database connection
     * @return database connection
     */
    private Connection getDatabaseConnection() {
        // open connection to database
        Connection conn = databaseUtilities.connect();
            
        return conn;
    }
    
//    /**
//     * Method to add statistiques countries to the database
//     * @param lists to be added
//     * @return 
//     */
//    public String addStatCountries(List<List<String>> lists) {
//        // Declare variables
//        Connection conn = null;
//        String results = "Done";
//        Set<String> set = new HashSet<>();
//        lists.forEach(strings -> {
//            set.add(strings.get(2));
//        });
//        try {
//            conn = getDatabaseConnection();
//            for(String string : set) {
//                databaseUtilities.insertStatCountry(conn, string);
//            }
//        } catch (SQLException ex) {
//            results = ex.getMessage();
//        } finally {
//            if (conn != null) {
//                try {
//                    databaseUtilities.closeConnection(conn);
//                } catch (SQLException e) {
//                    System.out.println(e.getMessage());
//                }
//            }
//        }
//        return results;
//    }
    
    /**
     * Method to add statistiques countries data to the database
     */
    public void getStatData() {
        // Declare variables
        Connection conn;
        List<List<String>> lists;
        String country = "";
        java.sql.Date maxDate = null, mDate = null;
        int countryId = 0, id;
        //lists = jsonUtilities.processJsonArray();
        lists = jsonUtilities.processJsonArray(
                simpleDateFormat.format(YESTERDAY_DATE)); 
        if (lists != null) {
            conn = getDatabaseConnection();
            for(List<String> list : lists) {
                if (conn == null) {
                    conn = getDatabaseConnection();
                    mResults.addResults("getStatData " + list.get(2) + 
                            " reconnected");
                }
                // get country id
                if (list.get(2) != null) {
                    id = databaseUtilities.selectStatCountryId(conn, 
                            list.get(2));
                } else {
                    id = 0;
                }
                if (id > 0) {
                    try {
                        if (list.get(0) != null) {
                            mDate = new java.sql.Date(simpleDateFormat
                                    .parse(list.get(0)).getTime());
                        }
                    } catch (ParseException e) {
                        mResults.addResults("getStatData Parse Exception" + 
                                list.get(2) + " " + e.getMessage());
                    }
                    if (mDate == null || mDate.compareTo(TODAY) == 0) {
                        continue;
                    }
                    if (id != countryId) {
                        countryId = id;
                        maxDate = databaseUtilities.getStatCountryMaxDate(conn, 
                                countryId);
                    }
                    if (maxDate == null) {
                        maxDate = mDate;
                        databaseUtilities.insertStatTotal(conn, countryId, 
                                mDate, list);
                    } else if (mDate.compareTo(maxDate) > 0) {
                        maxDate = mDate;
                        databaseUtilities.insertStatTotal(conn, countryId, 
                                mDate, list);
                    }
                }
            }

            if (conn != null) {
                // close connection
                databaseUtilities.closeConnection(conn);
            }
        } else {
            mResults.addResults("No stat data");
        }
    }
    
    /**
     * Method to add our world in data to the database
     * @param fileName of the file
     */
    public void loadOurWorldInData(String fileName) {
        Connection conn;
        List<List<String>> lists = csvUtilities.getCsvFile(PATH + fileName);
        conn = getDatabaseConnection();
        databaseUtilities.insertOurWorldInData(conn, lists);
        // close connection
        databaseUtilities.closeConnection(conn);
    }
    
   
    /**
     * Method to scrape and process United States data
     * @param conn to database
     * @return results of WorldOMeter scrapes
     */
    private void processUnitedStatesScrape (Connection conn) {
        // Declare constants
        final String WORLDOMETER_US = 
                "https://www.worldometers.info/coronavirus/country/us/";
        final String US_BASE_NAME = "us_covid_";
        
        // scrape WorldOMeter for United States Table
        ScrapeUtilities scrapeUtilities = new ScrapeUtilities();
        List<List<String>> unitedStatesStrings = 
                scrapeUtilities.getTableData(WORLDOMETER_US);
        
        // process united states covid data and put in csv file
        if (unitedStatesStrings != null) {
            // modify raw US strings and add population
            unitedStatesStrings = createUnitedStatesStrings(conn, 
                    unitedStatesStrings);
            // add date
            unitedStatesStrings = addYesterday(unitedStatesStrings);
            // write to database
            writeUSToDatabase(conn, unitedStatesStrings);
            mResults.addResults(
                    "Successfully acquired United States covid data");
        } else {
            mResults.addResults("No US data");
        }
    }
 
    /**
     * Method to scrape and process World data
     * @param conn to database
     * @return results
     */
    private void processWorldScrape(Connection conn) {
        // Declare constants
        final String WORLDOMETER_ALL = 
                "https://www.worldometers.info/coronavirus/";
        final String WORLD_BASE_NAME = "world_covid_";
        
        // Scrape world table
        ScrapeUtilities scrapeUtilities = new ScrapeUtilities();
        List<List<String>> worldStrings = 
                scrapeUtilities.getTableData(WORLDOMETER_ALL);
        // process world covid data and put in csv file
        if (worldStrings != null) {
            // modify raw world covid data
            worldStrings = createWorldStrings(conn, worldStrings);
            // add date
            worldStrings = addYesterday(worldStrings);
            // write to database
            writeWorldToDatabase(conn, worldStrings);
            mResults.addResults( 
                    "Successfully acquired world covid data");
        }
    }

    /**
     * Method to generate a file name
     * @param baseName to use
     * @return full file name
     */
    private String createFileName(String baseName) {
        // put file name together and return
        return PATH + baseName + YESTERDAY + ".csv";
    }

    /**
     * Method to get yesterday's date
     * @return a string representation of yesterday's date
     */
    private String getYesterdaysDate () {
        
        // get date
        LocalDate today = LocalDate.now().minusDays(1);
        
        java.sql.Date yesterday = java.sql.Date.valueOf(today);
        return simpleDateFormatAlt.format(yesterday);
    }
    
    /**
     * Method to get yesterday's date
     * @return yesterday's date
     */
    private java.sql.Date getYesterday() {
        // get date and remove 1 day
        LocalDate date = LocalDate.now().minusDays(1);
        // convert to sql date
        java.sql.Date yesterday = java.sql.Date.valueOf(date);
        // convert to sql
        return yesterday;
    }
    
    /**
     * Method to get today's date
     * @return a string representation of yesterday's date
     */
    private java.sql.Date getTodaysDate () {
        // get date
        java.util.Date date = new java.util.Date();
        java.sql.Date today = new java.sql.Date(date.getTime());
        return today;
    }

    /**
     * Method to add yesterday's date to each list of strings
     * @param lists to add yesterday's date to
     * @return converted lists
     */
    private List<List<String>> addYesterday (List<List<String>> lists) {
        // start loops
        for (int i = 0; i < lists.size(); i++) {
            if (i == 0) {
                // add heading
                 lists.get(i).add("Date");
            } else {
                // add date
                lists.get(i).add(YESTERDAY);
            }
        }
        // return converted lists
        return lists;
    }

    /**
     * Method to remove unwanted columns
     * @param conn to database
     * @param lists to modify
     * @return modified lists
     */
     private List<List<String>> createWorldStrings(Connection conn, 
            List<List<String>> lists) {
        // Declare variables
        List<List<String>> newLists = new ArrayList<>();
        // loop through lists putting only what is required in new lists
        lists.forEach(list -> {
            List<String> strings = new ArrayList<>();
            // eliminate untitled or unnecessary columns
            if (!list.get(1).isEmpty() && !list.get(1).equals("Total:")) {
                // get country name
                strings.add(list.get(1));
                // get total cases
                strings.add(list.get(2));
                // get total deaths
                strings.add(list.get(4));
                // get active cases
                strings.add(list.get(8));
                // get population
                strings.add(list.get(14));
                // add new list to new lists
                newLists.add(strings);
            }
        });
        updatePopulation(conn, "World", newLists);
        return newLists;
    }

    /**
     * Method to remove unwanted columns
     * @param conn to database
     * @param lists to modify
     * @return modified lists
     */
    private List<List<String>> createUnitedStatesStrings(Connection conn, 
            List<List<String>> lists) {
        // Declare variables
        List<List<String>> newLists = new ArrayList<>();
        // initialize counter for total population
        long totalPopulation = 0L;
        String temp;
        long population;
        // loop through lists
        for (int i = 0; i < lists.size(); i++) {
            // eliminate unnecessary columns
            if (!lists.get(i).get(0).equals("Total:")) {
                List<String> strings = new ArrayList<>();
                // get state name
                strings.add(lists.get(i).get(1));
                // get total cases
                strings.add(lists.get(i).get(2));
                // get total deaths
                strings.add(lists.get(i).get(4));
                // get active cases
                strings.add(lists.get(i).get(7));
                // get population
                strings.add(lists.get(i).get(12));
                // add new list to new lists
                newLists.add(strings);
            }
        } 
        updatePopulation(conn, "UnitedStates", newLists);
        return newLists;
    }

    /**
     * Method to convert population and add to total population
     * @param totalPopulation current total
     * @param strings string with value to add
     * @return new total population
     */
    private long getTotalPopulation(long totalPopulation, List<String> strings) {
        return totalPopulation += convertPopulation(strings.get(4));
    }
    
    /**
     * Method to convert population from string to long
     * @param temp string population to convert
     * @return population as a long
     */
    private long convertPopulation(String temp) {
        long population = 0;
        // remove commas
        temp = temp.replace(",", "");
        // remove bracketed value to avoid exception
        if (temp.contains("[")) {
            temp = temp.substring(0, temp.indexOf('['));
        }
        // if empty ignore
        if (!temp.isEmpty()) {
            // add to total
            population = Long.parseLong(temp);
        }
        return population;
    }

    /**
     * Method to remove state populations from raw strings
     * @param lists to extract information from
     * @return map with populations
     */
    private HashMap<String, String> createStatePopulations(
            List<List<String>> lists) {
        // Declare variables
        HashMap<String, String> hashMap = new HashMap<>();
        // get population and state name and put in hash map
        for (int i = 1; i < lists.size(); i++) {
            hashMap.put(lists.get(i).get(2), lists.get(i).get(3));
        }
        return hashMap;
    }
}