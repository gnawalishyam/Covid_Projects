/*
 * The MIT License
 *
 * Copyright 2020 Gary Larson <gary@thalic.mobi>.
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
package mobi.thalic.getcoviddata;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Class that deals with Covid data
 * @author Gary Larson gary@thalic.mobi
 */
public class CovidData {
    // Declare constants
    private final String YESTERDAY = getYesterdaysDate();
    private final String PATH = "C:\\covid\\";
    // Declare configuration options
    private final HashMap<String, String> configMap = new HashMap<>();
    
    public CovidData () {
        String results = getConfigParams();
        if (!results.equals("")) {
            System.out.println(results);
            System.exit(1);
        }
    }
    
    /**
     * Method to get the configuration items
     * @return 
     */
    private String getConfigParams() {
        //Declare variables
        List<List<String>> listStringLists = new ArrayList<>();
        BufferedReader fileReader;
        String inputString, results = "";
        
        
        try {
            fileReader = new BufferedReader(new FileReader("get_covid_data.ini"));
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
            results = "IOException: " + e.getMessage();
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
            writeUSToDatabase(unitedStatesStrings);
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
            writeWorldToDatabase(worldStrings);
        }
        // return result string
        return result;
    }
    
    /**
     * Method to remove unwanted columns from United States lists
     * @param lists to modify
     * @return modified lists
     */
    private List<List<String>> createUnitedStatesStrings(List<List<String>> lists) {
        // Declare variables
        List<List<String>> newLists = new ArrayList<>();
        // initialize counter for total population
        long totalPopulation = 0L;
        String temp;
        long population;
        DatabaseUtilities database = new DatabaseUtilities();
        try (
                Connection conn = 
                    database.connect(configMap.get("DB_CONNECTION"), 
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));) {
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
                    if (i == 0) {
                        strings.add("Population");
                    } else {
                        if (strings.get(0).equals("Total:")) {
                            population = database.selectStatePopulation(conn, 
                                    "USA Total");
                        } else {
                            population = database.selectStatePopulation(conn, 
                                    strings.get(0));
                        }

                        NumberFormat numberFormat = 
                                NumberFormat.getNumberInstance(Locale.US);
                        temp = numberFormat.format(population);
                        strings.add(temp);
                    }

                // add new list to new lists
                    newLists.add(strings);
                }
            }
             database.closeConnection(conn);
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return newLists;
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
     * Method to write US totals to the database
     * @param lists of data to process
     */
    private void writeUSToDatabase(List<List<String>> lists) {
        // initialize database variable
        DatabaseUtilities databaseUtilities = new DatabaseUtilities();
        try {
            // open connection to database
            Connection conn = 
                    databaseUtilities.connect(configMap.get("DB_CONNECTION"), 
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
            // insert data in total table in database
            databaseUtilities.insertUSTotals(conn, lists);
            // close database connection
            databaseUtilities.closeConnection(conn);
        } catch (SQLException | ParseException e) {
            // output SQL exception messages
            System.out.println(e.getMessage());
        } 
    }
    
    /**
     * Method to get yesterday's date
     * @return a string representation of yesterday's date
     */
    private String getYesterdaysDate () {
        // create date formatter
        DateTimeFormatter dateTimeFormatter = 
                DateTimeFormatter.ofPattern("yyyy_MM_dd");
        // get date
        LocalDateTime today = LocalDateTime.now();
        // remove one day
        LocalDateTime yesterday = today.minusDays(1);
        return dateTimeFormatter.format(yesterday);
    }
    
    /**
     * Method to remove unwanted columns from World lists
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
        return newLists;
    }
    
    /**
     * Method to write World totals to the database
     * @param lists of data to process
     */
    private void writeWorldToDatabase(List<List<String>> lists) {
        // initialize database variable
        DatabaseUtilities databaseUtilities = new DatabaseUtilities();
        try {
            // open connection to database
            Connection conn = 
                    databaseUtilities.connect(configMap.get("DB_CONNECTION"), 
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
            // insert data in total table in database
            databaseUtilities.insertWorldTotals(conn, lists);   
            // close database connection
            databaseUtilities.closeConnection(conn);
        } catch (SQLException | ParseException e) {
            // output SQL exception messages
            System.out.println(e.getMessage());
        }
    }
}
