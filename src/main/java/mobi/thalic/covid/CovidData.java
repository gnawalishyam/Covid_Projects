/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobi.thalic.covid;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
    private final HashMap<String, String> configMap = new HashMap<>();
    private final DatabaseUtilities databaseUtilities;
    private final JSONUtilities jsonUtilities;
    private final CSVUtilities csvUtilities;
    private final Results mResults;
    
    /**
     * Default constructor
     * @param results of all activities
     */
    public CovidData (Results results) {
        getConfigParams();
        mResults = results;
        databaseUtilities = new DatabaseUtilities(mResults);
        jsonUtilities = new JSONUtilities(mResults);
        csvUtilities = new CSVUtilities(mResults);
    }
    
    /**
     * Method to get the configuration items
     * @return 
     */
    private void getConfigParams() {
        //Declare variables
        List<List<String>> listStringLists = new ArrayList<>();
        BufferedReader fileReader;
        String inputString;
        
        
        try {
            fileReader = new BufferedReader(new FileReader("/etc/get_covid_data.ini")); // Development
            //fileReader = new BufferedReader(new FileReader("/etc/get_prod_data.ini")); // Production
            while ((inputString = fileReader.readLine()) != null) {
                if (inputString.contains(",")) {
                    String[] data = inputString.split(",");
                    if (data.length == 2) {
                        configMap.put(data[0], data[1]);
                    } else {
                        mResults.addResults("Error in configuration file");
                        break;
                    }
                } else {
                    mResults.addResults("Seperation error in configuration file");
                    break;
                }
            }
            fileReader.close();
        } catch (IOException e) {
            mResults.addResults("IOException: " + e.getMessage());
            System.exit(1);
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
        });
        // close database connection
        databaseUtilities.closeConnection(conn);
        mResults.addResults("Completed create calculations!");
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
        // get recovered from database
        Map<String, Long> recoveredData = 
                databaseUtilities.getRecoveredData(conn, date);
        // get population list from database
        List<CountryLong> populationList = 
                databaseUtilities.getPopulationData(conn, date);
        // get cases10k list from database
        List<CountryDouble> cases10kList = 
                databaseUtilities.getCases10kData(conn, date);
        // get deaths10k from database
        List<CountryDouble> deaths10kList = 
                databaseUtilities.getDeaths10kData(conn, date);
        // get active10k from database
        List<CountryDouble> active10kList = 
                databaseUtilities.getActive10kData(conn, date);
        // get recovered10k from database
        List<CountryDouble> recovered10kList = 
                databaseUtilities.getRecovered10kData(conn, date);
        // get mortality from database
        Map<String, Double> mortalityData = 
                databaseUtilities.getMortalityData(conn, date);
        // create medians
        Map<String, Integer> medians = createMedians(populationList);
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
        // create overall ranks
        Map<String, Integer> ranks = createOverallRanks(populationList, 
                populationRanks, cases10kRanks, deaths10kRanks, active10kRanks, 
                recovered10kRanks);
        // create cases10k scores
        Map<String, String> cases10kScores = 
                createScores(medians, cases10kList, cases10kRanks);
        // create deaths10k scores
        Map<String, String> deaths10kScores = 
                createScores(medians, deaths10kList, deaths10kRanks);
        // create active10k scores
        Map<String, String> active10kScores = 
                createScores(medians, active10kList, active10kRanks);
        // create recovered10k scores
        Map<String, String> recovered10kScores = 
                createScores(medians, recovered10kList, recovered10kRanks);
        // create overall scores
        Map<String, String> scores = getOverallScore(populationList, 
                cases10kScores, deaths10kScores, active10kScores, 
                recovered10kScores);
        // create populations
        Map<String, Long> populationData = createDataLong(populationList);
        // create cases10k
        Map<String, Double> cases10kData = createDataDouble(cases10kList);
        // create deaths10k
        Map<String, Double> deaths10kData = createDataDouble(deaths10kList);
        // create active10k
        Map<String, Double> active10kData = createDataDouble(active10kList);
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
            calc.setPercentPopulation(((double) population /
                    worldData.getPopulation()) * 100);
            // set percent of world deaths
            calc.setPercentDeaths(((double) deathsData.get(country) / 
                    worldData.getDeaths()) * 100);
            // set percent of world active cases
            calc.setPercentActive(((double) activeData.get(country) / 
                    worldData.getActive()) * 100);
            // set percent of recovered cases
            calc.setPercentRecovered(((double) recoveredData.get(country) / 
                    worldData.getRecovered()) * 100);
            // set percent of world total cases
            calc.setPercentCases(((double) casesData.get(country) / 
                    worldData.getCases()) * 100);
            // set percent of mortality
            calc.setPercentMortality(mortalityData.get(country));
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
            calc.setRank(ranks.get(country));
            // set overall score
            calc.setScore(scores.get(country));
            // add calculations to calculations list
            databaseUtilities.insertCalculation(conn, calc);
        }
    }
    
    /**
     * Method to create overall ranks
     * @param list to use
     * @param rank1 to add
     * @param rank2 to add
     * @param rank3 to add
     * @param rank4 to add
     * @param rank5 to add
     * @return overall ranks
     */
    private Map<String, Integer> createOverallRanks (List<CountryLong> list, 
            Map<String, Integer> rank1, Map<String, Integer> rank2, 
            Map<String, Integer> rank3, Map<String, Integer> rank4, 
            Map<String, Integer> rank5) {
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
                    rank3.get(country) + rank4.get(country) + 
                    rank5.get(country);
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
     * Method to get overall score
     * @param list to use
     * @param score1 to add
     * @param score2 to add
     * @param score3 to add
     * @param score4 to add
     * @return overall score
     */
    private Map<String, String> getOverallScore(List<CountryLong> list, 
            Map<String, String> score1, Map<String, String> score2, 
            Map<String, String> score3, Map<String, String> score4) {
        Map<String, String> scores = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
              String country = list.get(i).getCountry();
              double score = (getScoreValue(score1.get(country)) + 
                      getScoreValue(score2.get(country)) + 
                      getScoreValue(score3.get(country)) + 
                      getScoreValue(score4.get(country))) / 4.0;
              scores.put(country, getScore(score));
        }
        return scores;
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
     * @param list to generate medians from
     * @return Map of medians
     */
    private Map<String, Integer> createMedians(List<CountryLong> list) {
        Map<String, Integer> medians = new HashMap<>();
        // Get given ranks A+ and F
        medians.put("A+", 1);
        medians.put("F", list.size());
        // calculate C+
        medians.put("C+", (medians.get("A+") + medians.get("F")) / 2);
        // calculate B+
        medians.put("B+", (medians.get("A+") + medians.get("C+")) / 2);
        // calculate D+
        medians.put("D+", (medians.get("C+") + medians.get("F")) / 2);
        // calculate temporary medians TA, TB, TC, TD
        medians.put("TA", (medians.get("A+") + medians.get("B+")) / 2);
        medians.put("TB", (medians.get("B+") + medians.get("C+")) / 2);
        medians.put("TC", (medians.get("C+") + medians.get("D+")) / 2);
        medians.put("TD", (medians.get("D+") + medians.get("F")) / 2);
        // calculate A
        medians.put("A", (medians.get("A+") + medians.get("TA")) / 2);
        // calculate AM
        medians.put("A-", (medians.get("TA") + medians.get("B+")) / 2);
        // calculate B
        medians.put("B", (medians.get("B+") + medians.get("TB")) / 2);
        // calculate BM
        medians.put("B-", (medians.get("TB") + medians.get("C+")) / 2);
        // calculate C
        medians.put("C", (medians.get("C+") + medians.get("TC")) / 2);
        // calculate CM
        medians.put("C-", (medians.get("TC") + medians.get("D+")) / 2);
        // calculate D
        medians.put("D", (medians.get("D+") + medians.get("TD")) / 2);
        // calculate DM
        medians.put("D-", (medians.get("TD") + medians.get("F")) / 2);
        return medians;
    }
    
    /**
     * Method to create scores
     * @param rankList to use
     * @param list of values
     * @param rankMap to get the  rank
     * @return scores
     */
    private Map<String, String> createScores(Map<String, Integer> medians, 
            List<CountryDouble> list, Map<String, Integer> rankMap) {
        Map<String, String> scores = new HashMap<>();
        
        for (int i = 0; i < list.size(); i++) {
            scores.put(list.get(i).getCountry(), 
                    getScore(medians, list.get(i).getValue(), 
                            rankMap.get(list.get(i).getCountry())));
        }
        return scores;
    }
    
    /**
     * Method to get a score
     * @param medians to use to find score
     * @param value to get score for
     * @param rankList to use to find score
     * @return score
     */
    private String getScore(Map<String, Integer> medians, double value, 
            int rank) {
        String score;
        
        if (medians.get("A") > rank) {
            score = "A+";
        } else if(medians.get("A-") > rank) {
            score = "A";
        } else if(medians.get("B+") > rank) {
            score = "A-";
        } else if(medians.get("B") > rank) {
            score = "B+";
        } else if(medians.get("B-") > rank) {
            score = "B";
        } else if(medians.get("C+") > rank) {
            score = "B-";
        } else if(medians.get("C") > rank) {
            score = "C+";
        } else if(medians.get("C-") > rank) {
            score = "C";
        } else if(medians.get("D+") > rank) {
            score = "C-";
        } else if(medians.get("D") > rank) {
            score = "D+";
        } else if(medians.get("D-") > rank) {
            score = "D";
        } else if(medians.get("F") > rank) {
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
        // declare and initialize variable
        Connection conn = null;
        int i = 0;
        do {
            // open connection to database
            conn = 
                databaseUtilities.connect(configMap.get("DB_CONNECT"),
                configMap.get("DB_USER_NAME"), 
                configMap.get("DB_USER_PASSWORD"));
            i++;
        } while(conn == null && i < 10);
        if (conn == null) {
            mResults.addResults("No connection in 10 attempts!");
        }
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