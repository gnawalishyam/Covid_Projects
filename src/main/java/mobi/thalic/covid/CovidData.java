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
        // get deaths from database
        Map<String, Long> deathsData = 
                databaseUtilities.getDeathsData(conn, date);
        // get active from database
        Map<String, Long> activeData = 
                databaseUtilities.getActiveData(conn, date);
        // get population list from database
        List<CountryLong> populationList = 
                databaseUtilities.getPopulationData(conn, date);
        // get cases10k list from database
        List<CountryDouble> cases10kList = 
                databaseUtilities.getCases10kData(conn, date);
        // get cases10k16 list from database
        List<CountryDouble> cases10k16List = 
                databaseUtilities.getCases10kData16(conn, date);
        // get deaths10k from database
        List<CountryDouble> deaths10kList = 
                databaseUtilities.getDeaths10kData(conn, date);
        // get deaths10k16 from database
        List<CountryDouble> deaths10k16List = 
                databaseUtilities.getDeaths10kData16(conn, date);
        // get active10k from database
        List<CountryDouble> active10kList = 
                databaseUtilities.getActive10kData(conn, date);
        // get recovered10k from database
        List<CountryDouble> recovered10kList = 
                databaseUtilities.getRecovered10kData(conn, date);
        // get recovered percent data
        List<CountryDouble> recoveredPercentList = 
                databaseUtilities.getRecoveredPercentData(conn, date);
        // create population ranks
        Map<String, Integer> populationRanks = 
                assignRanksLong(populationList);
        // create cases10k ranks
        Map<String, Integer> cases10kRanks = 
                assignRanksDouble(cases10kList);
        // create deaths10k ranks
        Map<String, Integer> deaths10kRanks = 
                assignRanksDouble(deaths10kList);
        // create active10k ranks
        Map<String, Integer> active10kRanks = 
                assignRanksDouble(active10kList);
        // create recovered10k ranks
        Map<String, Integer> recovered10kRanks = 
                assignRanksDouble(recovered10kList);
        // create recovered percent ranks
        Map<String, Integer> recoveredPercentRanks = 
                assignRanksDouble(recoveredPercentList);
        // create case10k medians
        Map<String, Double> cases10kMedians = createMediansAsc(cases10kList);
        // create cases10k scores
        Map<String, String> cases10kScores = 
                createScoresAsc(cases10kMedians, cases10kList);
        // create death10k medians
        Map<String, Double> deaths10kMedians = createMediansAsc(deaths10kList);
        // create deaths10k scores
        Map<String, String> deaths10kScores = 
                createScoresAsc(deaths10kMedians, deaths10kList);
        // create active10k medians
        Map<String, Double> active10kMedians = createMediansAsc(active10kList);
        // create active10k scores
        Map<String, String> active10kScores = 
                createScoresAsc(active10kMedians, active10kList);
        // create recovered10k medians
        Map<String, Double> recovered10kMedians = 
                createMediansDesc(recovered10kList);
        // create recovered10k scores
        Map<String, String> recovered10kScores = 
                createScoresDesc(recovered10kMedians, recovered10kList);
        // create recovered percent scores
        Map<String, String> recoveredPercentScores = 
                createRecoveredPercentScores(recoveredPercentList);
        // create overall scores
        List<CountryDouble> overallScoresList = 
                createOverallScores(populationList, deaths10kScores, 
                        active10kScores);
        // create overall score ranks
        Map<String, Integer> overallScoresRanks = 
                assignRanksDouble(overallScoresList);
        // create overall scores medians
        Map<String, Double> overallScoresMedians = 
                createMediansDesc(overallScoresList);
        // create overall scores
        Map<String, String> overallScores = 
                createScoresDesc(overallScoresMedians, overallScoresList);
        // create populations data
        Map<String, Long> populationData = createDataLong(populationList);
        // create cases10k data
        Map<String, Double> cases10kData = createDataDouble(cases10kList);
        // create cases10k16 data
        Map<String, Double> cases10k16Data = new HashMap<>();
        if (cases10k16List.size() > 0) {
            cases10k16Data = createDataDouble(cases10k16List);
        }
        // create deaths10k data
        Map<String, Double> deaths10kData = createDataDouble(deaths10kList);
        // create deaths10k data
        Map<String, Double> deaths10k16Data = new HashMap<>();
        if (deaths10k16List.size() > 0) {
            deaths10k16Data = createDataDouble(deaths10k16List);
        }
        // create active10k data
        Map<String, Double> active10kData = createDataDouble(active10kList);
        // create recovered percent data
        Map<String, Double> recoveredPercentData = 
                createDataDouble(recoveredPercentList);
        // create recovered10k
        Map<String, Double> recovered10kData = 
                createDataDouble(recovered10kList);
        // loop through populations and populate the data into calculations
        List<Calculations> calcList = new ArrayList<>();
        for (int i = 0; i < populationList.size(); i++) {
            // create a calculations class to hold data
            Calculations calc = new Calculations();
            // get country
            String country = populationList.get(i).getCountry();
            // get population
            long population = populationList.get(i).getValue();
            // set country
            calc.setCountry(country);
            // set date of data
            calc.setDate(date);
            // set percent of world population
            calc.setPercentPopulation(calculatePercent(population, 
                    worldData.getPopulation()));
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
            // set percent of mortality
            calc.setPercentMortality(calculatePercent(deathsData.get(country), 
                    casesData.get(country) - activeData.get(country)));
            // set population
            calc.setPopulation(populationData.get(country));
            // set population rank
            calc.setPopulationRank(populationRanks.get(country));
            // set deaths per 10,000 population
            calc.setDeaths10k(deaths10kData.get(country));
            // set deaths per 10,000 population rank
            calc.setDeaths10kRank(deaths10kRanks.get(country));
            // set deaths per 10,000 population score
            calc.setDeaths10kScore(deaths10kScores.get(country));
            // set active cases per 10,000 population
            calc.setActive10k(active10kData.get(country));
            // set active cases per 10,ooo population rank
            calc.setActive10kRank(active10kRanks.get(country));
            // set active cases per 10,000 population score
            calc.setActive10kScore(active10kScores.get(country));
            // set recovered cases per 10,000 population
            calc.setRecovered10k(recovered10kData.get(country));
            // set recovered cases per 10,000 population rank
            calc.setRecovered10kRank(recovered10kRanks.get(country));
            // set recovered cases per 10,000 population score
            calc.setRecovered10kScore(recovered10kScores.get(country));
            // set total cases per 10,000 population
            calc.setCases10k(cases10kData.get(country));
            // set total ceses per 10,000 population rank
            calc.setCases10kRank(cases10kRanks.get(country));
            // set total casres per 10,000 population score
            calc.setCases10kScore(cases10kScores.get(country));
            // set overall rank
            calc.setRank(overallScoresRanks.get(country));
            // set overall score
            calc.setScore(overallScores.get(country));
            // calculate and set survival rate
            calc.setSurvivalRate(calculatePercent(casesData.get(country) - 
                    activeData.get(country) - deathsData.get(country), 
                    casesData.get(country) - activeData.get(country)));
            // calculate and set active percent
            calc.setActivePercent(calculatePercent(activeData.get(country), 
                    casesData.get(country)));
            // set recovered percent
            calc.setRecoveredPercent(recoveredPercentData.get(country));
            // set recovered percent rank
            calc.setRecoveredPercentRank(recoveredPercentRanks.get(country));
            // set recovered percent score
            calc.setRecoveredPercentScore(recoveredPercentScores.get(country));
            // add cases10k 15 day average data
            if (cases10k16Data != null && cases10k16Data.size() > 0 && 
                    cases10k16Data.containsKey(country) && 
                    cases10kData.get(country) - 
                    cases10k16Data.get(country) > 0) {
                calc.setCases10k15(calculateAverage(cases10kData.get(country) - 
                        cases10k16Data.get(country), 15));
            } else {
                calc.setCases10k15(0.0);
            }
            calc.setCases10k15Rank(0);
            calc.setCases10k15Score("");
            // add deaths10k 15 day average data
            if (deaths10k16Data != null && deaths10k16Data.size() > 0 && 
                    deaths10k16Data.containsKey(country) && 
                    deaths10kData.get(country) - 
                    deaths10k16Data.get(country) > 0) {
                calc.setDeaths10k15(
                        calculateAverage(deaths10kData.get(country) - 
                        deaths10k16Data.get(country), 15));
            } else {
                calc.setDeaths10k15(0.0);
            }
            calc.setDeaths10k15Rank(0);
            calc.setDeaths10k15Score("");
            // add calculations to calculations list
            databaseUtilities.insertCalculation(conn, calc);
        }
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
        String tempString = String.format(Locale.getDefault(), "%.2f", tempDouble);
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
        String tempString = String.format(Locale.getDefault(), "%.2f", tempDouble);
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
        String tempString = String.format(Locale.getDefault(), "%.2f", number);
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
    private Map<String, Integer> createOverallRanks (List<CountryLong> list, 
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
            String country = list.get(i).getCountry();
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
            countryRanks.put(list.get(i).getCountry(), 
                    ranks.get(rankData.get(list.get(i).getCountry())));
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
     * @return sorted country scores list
     */
    private List<CountryDouble> createOverallScores(List<CountryLong> list, 
            Map<String, String> score1, Map<String, String> score2) {
        // declare variables
        List<CountryDouble> countryScoresList = new ArrayList<>();
        Map<String, Double> scoresMap = new HashMap<>();
        List<Double> scoresList = new ArrayList<>();
        // loop through countries
        for (int i = 0; i < list.size(); i++) {
            // get country
              String country = list.get(i).getCountry();
              // calculate overall score
              double score = calculatePercent(
                      (getScoreValue(score1.get(country)) + 
                      getScoreValue(score2.get(country))) / 2.0);
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
                for (Map.Entry<String, Double> entry : scoresMap.entrySet()) {
                    if (entry.getValue() == score) {
                        // add entry to country scores list
                        countryScoresList.add(new CountryDouble(entry.getKey(), 
                                entry.getValue()));
                    }
                }
            }
        }
        // return sorted country scores list
        return countryScoresList;
    }
     
    /**
     * Method to get the value of a score
     * @param score to get value of
     * @return value of the score
     */
    private double getScoreValue(String score) {
        switch (score) {
            case "A+" :
                return 4.33;
            case "A" :
                return 4.0;
            case "A-" :
                return 3.67;
            case "B+" :
                return 3.33;
            case "B" :
                return 3.0;
            case "B-" :
                return 2.67;
            case "C+" :
                return 2.33;
            case "C" :
                return 2.0;
            case "C-" :
                return 1.67;
            case "D+" :
                return 1.33;
            case "D" :
                return 1.0;
            case "D-" :
                return 0.67;
            default:
                return 0.0;
        }
    }
    
    /**
     * Method to get score from a value
     * @param value to get score for
     * @return score of the value
     */
    private String getScore(double value) {
        if (value > 4.0) {
            return "A+";
        } else if (value > 3.67) {
            return "A";
        } else if (value > 3.33) {
            return "A-";
        } else if (value > 3.0) {
            return "B+";
        } else if (value > 2.67) {
            return "B";
        } else if (value > 2.33) {
            return "B-";
        } else if (value > 2.0) {
            return "C+";
        } else if (value > 1.67) {
            return "C";
        } else if (value > 1.33) {
            return "C-";
        } else if (value > 1.0) {
            return "D+";
        } else if (value > 0.67) {
            return "D";
        } else if (value > 0.33) {
            return "D-";
        } 
        return "F";
    }
    
    /**
     * Method to assign ranks of long values
     * @param list of values
     * @return assigned ranks
     */
    private Map<String, Integer> assignRanksLong(List<CountryLong> list) {
        // Declare values;
        int rank = 0;
        long value = -1;
        Map<String, Integer> ranks = new HashMap<>();
        for(int i = 0; i < list.size(); i++) {
            if (list.get(i).getValue() != value) {
                rank = i + 1;
                value = list.get(i).getValue(); 
            }
            ranks.put(list.get(i).getCountry(), rank);
        }
        return ranks;
    }
    
    /**
     * Method to assign ranks of double values ascending
     * @param list of values
     * @return ranks
     */
    private Map<String, Integer> assignRanksDouble(List<CountryDouble> list) {
        // declare variables
        int rank = 0;
        double value = -1.0;
        Map<String, Integer> ranks = new HashMap<>();
        for(int i = 0; i < list.size(); i++) {
            if (list.get(i).getValue() != value) {
                rank = i + 1;
                value = list.get(i).getValue(); 
            }
            ranks.put(list.get(i).getCountry(), rank);
        }
        return ranks;
    }
    
    /**
     * Method to create median for the scores
     * @param list to generate medians from ascending
     * @return Map of medians
     */
    private Map<String, Double> createMediansAsc(List<CountryDouble> list) {
        Map<String, Double> medians = new HashMap<>();
        // Get given ranks A+ and F
        double step = (list.get(list.size() - 1).getValue() - 
                list.get(0).getValue()) / 13;
        double current = calculatePercent(list.get(0).getValue());
        medians.put("A+", current);
        // calculate A
        current = calculatePercent(current + step);
        medians.put("A", current);
        // calculate A-
        current = calculatePercent(current + step);
        medians.put("A-", current);
        // calculate B+
        current = calculatePercent(current + step);
        medians.put("B+", current);
        // calculate B
        current = calculatePercent(current + step);
        medians.put("B", current);
        // calculate B-
        current = calculatePercent(current + step);
        medians.put("B-", current);
        // calculate C+
        current = calculatePercent(current + step);
        medians.put("C+", current);
        // calculate C
        current = calculatePercent(current + step);
        medians.put("C", current);
        // calculate C-
        current = calculatePercent(current + step);
        medians.put("C-", current);
        // calculate D+
        current = calculatePercent(current + step);
        medians.put("D+", current);
        // calculate D
        current = calculatePercent(current + step);
        medians.put("D", current);
        // calculate D-
        current = calculatePercent(current + step);
        medians.put("D-", current);
        // calculate F
        current = calculatePercent(current + step);
        medians.put("F", current);
        return medians;
    }
    
    /**
     * Method to create median for the scores
     * @param list to generate medians from descending
     * @return Map of medians
     */
    private Map<String, Double> createMediansDesc(List<CountryDouble> list) {
        Map<String, Double> medians = new HashMap<>();
        // Get given ranks A+ and F
        double step = (list.get(0).getValue() - 
                list.get(list.size() - 1).getValue()) / 13;
        double current = calculatePercent(list.get(0).getValue());
        medians.put("A+", current);
        // calculate A
        current = calculatePercent(current - step);
        medians.put("A", current);
        // calculate A-
        current = calculatePercent(current - step);
        medians.put("A-", current);
        // calculate B+
        current = calculatePercent(current - step);
        medians.put("B+", current);
        // calculate B
        current = calculatePercent(current - step);
        medians.put("B", current);
        // calculate B-
        current = calculatePercent(current - step);
        medians.put("B-", current);
        // calculate C+
        current = calculatePercent(current - step);
        medians.put("C+", current);
        // calculate C
        current = calculatePercent(current - step);
        medians.put("C", current);
        // calculate C-
        current = calculatePercent(current - step);
        medians.put("C-", current);
        // calculate D+
        current = calculatePercent(current - step);
        medians.put("D+", current);
        // calculate D
        current = calculatePercent(current - step);
        medians.put("D", current);
        // calculate D-
        current = calculatePercent(current - step);
        medians.put("D-", current);
        // calculate F
        current = calculatePercent(current - step);
        medians.put("F", current);
        return medians;
    }
    
    /**
     * Method to create scores
     * @param medians to use
     * @param list of values
     * @return scores
     */
    private Map<String, String> createScoresAsc(Map<String, Double> medians, 
            List<CountryDouble> list) {
        Map<String, String> scores = new HashMap<>();
        
        for (int i = 0; i < list.size(); i++) {
            scores.put(list.get(i).getCountry(), 
                    getScoreAsc(medians, list.get(i).getValue()));
        }
        return scores;
    }
    
    /**
     * Method to get a score
     * @param medians to use to find score
     * @param value to get score for
     * @return score
     */
    private String getScoreAsc(Map<String, Double> medians, double value) {
        String score;
        
        if (medians.get("A") > value) {
            score = "A+";
        } else if(medians.get("A-") > value) {
            score = "A";
        } else if(medians.get("B+") > value) {
            score = "A-";
        } else if(medians.get("B") > value) {
            score = "B+";
        } else if(medians.get("B-") > value) {
            score = "B";
        } else if(medians.get("C+") > value) {
            score = "B-";
        } else if(medians.get("C") > value) {
            score = "C+";
        } else if(medians.get("C-") > value) {
            score = "C";
        } else if(medians.get("D+") > value) {
            score = "C-";
        } else if(medians.get("D") > value) {
            score = "D+";
        } else if(medians.get("D-") > value) {
            score = "D";
        } else if(medians.get("F") > value) {
            score = "D-";
        } else {
            score = "F";
        }
        return score;
    }
    
    /**
     * Method to create scores
     * @param medians to use
     * @param list of values
     * @return scores
     */
    private Map<String, String> createScoresDesc(Map<String, Double> medians, 
            List<CountryDouble> list) {
        Map<String, String> scores = new HashMap<>();
        
        for (int i = 0; i < list.size(); i++) {
            scores.put(list.get(i).getCountry(), 
                    getScoreDesc(medians, list.get(i).getValue()));
        }
        return scores;
    }
    
    /**
     * Method to create scores
     * @param list of values
     * @return scores
     */
    private Map<String, String> createRecoveredPercentScores(
            List<CountryDouble> list) {
        Map<String, String> scores = new HashMap<>();
        String score;
        
        for (int i = 0; i < list.size(); i++) {
            double value = list.get(i).getValue();
            if (value > 96.0) {
                score = "A+";
            } else if (value > 92.0) {
                score = "A";
            } else if (value > 86.0) {
                score = "A-";
            } else if (value > 82.0) {
                score = "B+";
            } else if (value > 78.0) {
                score = "B";
            } else if (value > 74.0) {
                score = "B-";
            } else if (value > 70.0) {
                score = "C+";
            } else if (value > 66.0) {
                score = "C";
            } else if (value > 62.0) {
                score = "C-";
            } else if (value > 58.0) {
                score = "D+";
            } else if (value > 54.0) {
                score = "D";
            } else if (value > 50.0) {
                score = "D-";
            } else {
                score = "F";
            }
            scores.put(list.get(i).getCountry(), score);
        }
        return scores;
    }
    
    /**
     * Method to get a score
     * @param medians to use to find score
     * @param value to get score for
     * @return score
     */
    private String getScoreDesc(Map<String, Double> medians, double value) {
        String score;
        
        if (medians.get("A") < value) {
            score = "A+";
        } else if(medians.get("A-") < value) {
            score = "A";
        } else if(medians.get("B+") < value) {
            score = "A-";
        } else if(medians.get("B") < value) {
            score = "B+";
        } else if(medians.get("B-") < value) {
            score = "B";
        } else if(medians.get("C+") < value) {
            score = "B-";
        } else if(medians.get("C") < value) {
            score = "C+";
        } else if(medians.get("C-") < value) {
            score = "C";
        } else if(medians.get("D+") < value) {
            score = "C-";
        } else if(medians.get("D") < value) {
            score = "D+";
        } else if(medians.get("D-") < value) {
            score = "D";
        } else if(medians.get("F") < value) {
            score = "D-";
        } else {
            score = "F";
        }
        return score;
    }
    
    /**
     * Method to create a long data Map
     * @param list of data
     * @return map of data
     */
    private Map<String, Long> createDataLong (List<CountryLong> list) {
        Map<String, Long> data = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            data.put(list.get(i).getCountry(), list.get(i).getValue());
        }
        return data;
    }
    
    /**
     * Method to create a double data map
     * @param list of data
     * @return Map of data
     */
    private Map<String, Double> createDataDouble (List<CountryDouble> list) {
        Map<String, Double> data = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            data.put(list.get(i).getCountry(), list.get(i).getValue());
        }
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