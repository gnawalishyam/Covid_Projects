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
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 *
 * @author GaryL
 */
public class CovidData {
    // Declare constants
    private final String YESTERDAY = getYesterdaysDate();
    private final String PATH = "C:\\covid\\";
    // Declare database variables
    private final HashMap<String, String> configMap = new HashMap<>();
    
    /**
     * Default constructor
     */
    public CovidData () {
        getConfigParams();
    }
    
    /**
     * Method to get the configuration items
     * @return 
     */
    private void getConfigParams() {
        //Declare variables
        List<List<String>> listStringLists = new ArrayList<>();
        BufferedReader fileReader;
        String inputString, results = "";
        
        
        try {
            fileReader = new BufferedReader(new FileReader("/etc/get_covid_data.ini"));
            while ((inputString = fileReader.readLine()) != null) {
                if (inputString.contains(",")) {
                    String[] data = inputString.split(",");
                    if (data.length == 2) {
                        configMap.put(data[0], data[1]);
                    } else {
                        results = "Error in configuration file";
                        break;
                    }
                } else {
                    results = "Seperation error in configuration file";
                    break;
                }
            }
            fileReader.close();
        } catch (IOException e) {
            System.out.println(results);
            System.out.println("IOException: " + e.getMessage());
            System.exit(5);
        }
    }
    
    /**
     * Method to create state dailies
     * @return results
     */
    public String createStateDailies() {
        String results;
        // initialize database variable
        DatabaseUtilities databaseUtilities = new DatabaseUtilities();
        try {
            // open connection to database
            Connection conn = 
                    databaseUtilities.connect(configMap.get("DB_CONNECTION"), 
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
            // insert data in total table in database
            results = databaseUtilities.createStateDailies(conn);
        } catch(SQLException e) {
            results = e.getMessage();
        }
        return results;
    }
    
    /**
     * Method to create country dailies
     * @return results
     */
    public String createCountryDailies() {
        String results;
        // initialize database variable
        DatabaseUtilities databaseUtilities = new DatabaseUtilities();
        try {
            // open connection to database
            Connection conn = 
                    databaseUtilities.connect(configMap.get("DB_CONNECTION"), 
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
            // insert data in total table in database
            results = databaseUtilities.createCountryDailies(conn);
        } catch(SQLException e) {
            results = e.getMessage();
        }
        return results;
    }
    
    /**
     * Method to process world-o-meter scrape to enter data into a database
     * @return results of WorldOMeter scrapes
     */
    public String processWorldometerScrape(){
        // declare and initialize variable
        String result = "United States Results";
        result += "\n" + processUnitedStatesScrape();
        result += "\n World Results";
        result += "\n" + processWorldScrape();
        //result += "\n" + backupDatabase();
        return result;
    }
    
    /**
     * Method to test the availability of the database
     * @return 
     */
    public String testDatabase(){
        // Declare variables
        String results;
        DatabaseUtilities database = new DatabaseUtilities();
        try (
            Connection conn = 
                database.connect(configMap.get("DB_CONNECTION"), 
                configMap.get("DB_USER_NAME"), 
                configMap.get("DB_USER_PASSWORD"));) {
            long population = database.selectStatePopulation(conn, 
                                    "USA Total");
            results = String.format(Locale.getDefault(), 
                    "USA Total population = %,d", population);
            database.closeConnection(conn);
        } catch (SQLException ex) {
            results = ex.getMessage();
        }
        return results;
    }
    
    /**
     * Method to add statistiques countries to the database
     * @param lists to be added
     * @return 
     */
    public String addStatCountries(List<List<String>> lists) {
        // Declare variables
        String results = "Done";
        Set<String> set = new HashSet<>();
        lists.forEach(strings -> {
            set.add(strings.get(2));
        });
        DatabaseUtilities databaseUtilities = new DatabaseUtilities();
        try (
            Connection conn = 
                databaseUtilities.connect(configMap.get("DB_CONNECTION"), 
                configMap.get("DB_USER_NAME"), 
                configMap.get("DB_USER_PASSWORD"));) {
            for(String string : set) {
                databaseUtilities.insertStatCountry(conn, string);
            }
            databaseUtilities.closeConnection(conn);
        } catch (SQLException ex) {
            results = ex.getMessage();
        }
        return results;
    }
    
    /**
     * Method to add our world in data to the database
     * @param fileName of the file
     */
    public void loadOurWorldInData(String fileName) {
        CSVUtilities csvUtilities = new CSVUtilities();
        List<List<String>> lists = csvUtilities.getCsvFile(PATH + fileName);
        DatabaseUtilities databaseUtilities = new DatabaseUtilities();
        try {
            Connection conn = 
                databaseUtilities.connect(configMap.get("DB_CONNECTION"), 
                configMap.get("DB_USER_NAME"), 
                configMap.get("DB_USER_PASSWORD"));
            databaseUtilities.insertOurWorldInData(conn, lists);
            databaseUtilities.closeConnection(conn);
        } catch (SQLException | ParseException e) {
            System.out.println("Error occured: " + e.getMessage());
        }
    }
    
   
    /**
     * Method to scrape and process United States data
     * @return results of WorldOMeter scrapes
     */
    private String processUnitedStatesScrape () {
        // Declare constants
        final String WORLDOMETER_US = 
                "https://www.worldometers.info/coronavirus/country/us/";
        final String US_BASE_NAME = "us_covid_";
        // Declare variable
        String result = "Scrape Failed";
        
        // scrape WorldOMeter for United States Table
        ScrapeUtilities scrapeUtilities = new ScrapeUtilities();
        List<List<String>> unitedStatesStrings = 
                scrapeUtilities.getTableData(WORLDOMETER_US);
        
        // process united states covid data and put in csv file
        if (unitedStatesStrings != null) {
            // modify raw US strings and add population
            unitedStatesStrings = createUnitedStatesStrings(unitedStatesStrings);
            // add date
            unitedStatesStrings = addYesterday(unitedStatesStrings);
            // write to database
            String tempResult = writeUSToDatabase(unitedStatesStrings);
            result = tempResult + "\n" + 
                    "Successfully acquired United States covid data";
        }
        // return result string
        return result;
    }
 
    /**
     * Method to scrape and process World data
     * @return results
     */
    private String processWorldScrape() {
        // Declare constants
        final String WORLDOMETER_ALL = 
                "https://www.worldometers.info/coronavirus/";
        final String WORLD_BASE_NAME = "world_covid_";
        // Declare variables
        String result = "Scrape Failed";
        
        // Scrape world table
        ScrapeUtilities scrapeUtilities = new ScrapeUtilities();
        List<List<String>> worldStrings = 
                scrapeUtilities.getTableData(WORLDOMETER_ALL);
        // process world covid data and put in csv file
        if (worldStrings != null) {
            // modify raw world covid data
            worldStrings = createWorldStrings(worldStrings);
            // add date
            worldStrings = addYesterday(worldStrings);
            // write to database
            String tempResult = writeWorldToDatabase(worldStrings);
            result = tempResult + "\n" + 
                    "Successfully acquired world covid data";
        }
        // return result string
        return result;
    }

    /**
     * Method to write US totals to the database
     * @param lists of data to process
     * @return results
     */
    private String writeUSToDatabase(List<List<String>> lists) {
        String result;
        Connection conn = null;
        // initialize database variable
        DatabaseUtilities databaseUtilities = new DatabaseUtilities();
        try {
            // open connection to database
            conn = 
                    databaseUtilities.connect(configMap.get("DB_CONNECTION"), 
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
            // insert data in total table in database
            result = databaseUtilities.insertUSTotals(conn, lists);
        } catch (SQLException | ParseException e) {
            // output SQL exception messages
            result = e.getMessage() + "\n";
        } finally {
            // close database connection
            if (conn != null) {
                try {
                databaseUtilities.closeConnection(conn);
                } catch (SQLException e) {
                    // output SQL exception messages
                    result = e.getMessage() + "\n";
                }
            }
        }
        return result;
    }
    
    /**
     * Method to write World totals to the database
     * @param lists of data to process
     */
    private String writeWorldToDatabase(List<List<String>> lists) {
        // initialize variable
        String result;
        // initialize database variable
        DatabaseUtilities databaseUtilities = new DatabaseUtilities();
        try {
            // open connection to database
            Connection conn = 
                    databaseUtilities.connect(configMap.get("DB_CONNECTION"), 
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
            // insert data in total table in database
            result = databaseUtilities.insertWorldTotals(conn, lists);   
            // close database connection
            databaseUtilities.closeConnection(conn);
        } catch (SQLException | ParseException e) {
            // output SQL exception messages 
            result = e.getMessage();
        }
        return result;
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
        // create date formatter
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");
        // get date
        LocalDateTime today = LocalDateTime.now();
        // remove one day
        LocalDateTime yesterday = today.minusDays(1);
        return dateTimeFormatter.format(yesterday);
    }
    
    /**
     * Method to get today's date
     * @return a string representation of yesterday's date
     */
    private String getTodaysDate () {
        // create date formatter
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");
        // get date
        LocalDateTime today = LocalDateTime.now();
        return dateTimeFormatter.format(today);
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
     * @param lists to modify
     * @return modified lists
     */
    private List<List<String>> createWorldStrings(List<List<String>> lists) {
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
        updatePopulation("World", newLists);
        return newLists;
    }

    /**
     * Method to remove unwanted columns
     * @param lists to modify
     * @param map with state populations
     * @return modified lists
     */
    private List<List<String>> createUnitedStatesStrings(List<List<String>> lists) {
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
        updatePopulation("UnitedStates", newLists);
        return newLists;
    }
    
    /**
     * Method to update population
     * @param country either World or UnitedStates
     * @param lists to update
     */
    private void updatePopulation(String country, List<List<String>> lists) {
        // initialize database variable
        DatabaseUtilities databaseUtilities = new DatabaseUtilities();
        long adjustment;
        try {
            // open connection to database
            Connection conn = 
                databaseUtilities.connect(configMap.get("DB_CONNECTION"), 
                configMap.get("DB_USER_NAME"), 
                configMap.get("DB_USER_PASSWORD"));
            for (int i = 1; i < lists.size(); i++) {
                long population = convertPopulation(lists.get(i).get(4));
                String place = lists.get(i).get(0);
                if (country.equals("UnitedStates")) {
                    long statePopulation = databaseUtilities
                            .selectStatePopulation(conn, place);
                    if (statePopulation == 0 && population != 0) {
                        if (databaseUtilities.selectStateId(conn, place) != 0) {
                            databaseUtilities.insertStatePopulation(conn, 
                                    place, population);
                        }
                    } else {
                    adjustment = statePopulation / 10;
                        if (statePopulation > population - adjustment && 
                                statePopulation < population + adjustment && 
                                population != statePopulation) {
                            databaseUtilities.updateStatePopulation(conn, 
                                place, population);
                        }
                    }
                } else {
                    long countryPopulation = databaseUtilities
                            .selectWorldPopulation(conn, place);
                    if (countryPopulation == 0 && population != 0) {
                        if (databaseUtilities.selectCountryId(conn, place) != 
                                0) {
                            databaseUtilities.insertWorldPopulation(conn, 
                                    place, population);
                        }
                    } else {
                        adjustment = countryPopulation / 10;
                        if (countryPopulation > population - adjustment && 
                                countryPopulation < population + adjustment && 
                                countryPopulation != population) {
                            databaseUtilities.updateWorldPopulation(conn, 
                                    place, population);
                            if (place.equals("USA")) {
                                databaseUtilities.updateStatePopulation(conn, 
                                    "USA Total", population);
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            // output SQL exception messages
            System.out.println(e.getMessage());
        } 
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
    private HashMap<String, String> createStatePopulations(List<List<String>> lists) {
        // Declare variables
        HashMap<String, String> hashMap = new HashMap<>();
        // get population and state name and put in hash map
        for (int i = 1; i < lists.size(); i++) {
            hashMap.put(lists.get(i).get(2), lists.get(i).get(3));
        }
        return hashMap;
    }
}