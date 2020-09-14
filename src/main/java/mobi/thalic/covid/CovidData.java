/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobi.thalic.covid;

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
 *
 * @author GaryL
 */
public class CovidData {
    // Declare constants
    private final String YESTERDAY = getYesterdaysDate();
    private final String PATH = "C:\\covid\\";
    // Declare database variables
    private final String DATABASE_CONNECTION_STRING = 
            "jdbc:postgresql://52d9225.online-server.cloud:5432/covid";
    private final String DATABASE_USER_NAME = "java_program";
    private final String DATABASE_USER_PASSWORD = "3peb3NnzY_2Md@*yGb";
    /**
     * Method to process world-o-meter scrape to comma-separated values files
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
    
    public void loadOurWorldInData(String fileName) {
        CSVUtilities csvUtilities = new CSVUtilities();
        List<List<String>> lists = csvUtilities.getCsvFile(PATH + fileName);
        DataBaseUtilities databaseUtilities = new DataBaseUtilities();
        try {
            Connection conn = databaseUtilities.connect(
                    DATABASE_CONNECTION_STRING, DATABASE_USER_NAME, 
                    DATABASE_USER_PASSWORD);
            databaseUtilities.insertOurWorldInData(conn, lists);
            databaseUtilities.closeConnection(conn);
        } catch (SQLException | ParseException e) {
            System.out.println("Error occured: " + e.getMessage());
        }
    }
    
//    /**
//     * Method to backup a database
//     * @return 
//     */
//    public String backupDatabase() {
//        // Declare and initialize variables
//        String result = "";
//        String fileName = PATH + "backups\\covid_" + getTodaysDate() + ".sql";
//        // initialize database variable
//        DataBaseUtilities databaseUtilities = new DataBaseUtilities();
//        
//        try {
//            // open connection to database
//            Connection conn = databaseUtilities.connect(
//                    DATABASE_CONNECTION_STRING, DATABASE_USER_NAME, 
//                    DATABASE_USER_PASSWORD);
//            // backup database
//            databaseUtilities.backupDatabase(conn, fileName);
//            // close database connection
//            databaseUtilities.closeConnection(conn);
//            result = "Sucessfully backed up database";
//        } catch (SQLException e) {
//            // output SQL exception messages
//            result = e.getMessage();
//        }
//        return result;
//    }
   
    /**
     * Method to scrape and process United States data
     * @return results
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
            // write to file
            CSVUtilities csvUtilities = new CSVUtilities();
            try {
                csvUtilities.writeCSVFile(unitedStatesStrings, 
                        createFileName(US_BASE_NAME));
                // Send success message to console
                result = "Successfully acquired United States covid data";
            } catch(IOException e) {
                result = "IO error: " + e.getMessage();
            }
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
            // write to file
            CSVUtilities csvUtilities = new CSVUtilities();
            try {
                csvUtilities.writeCSVFile(worldStrings, createFileName(WORLD_BASE_NAME));
                // Send success message to console
                result = "Successfully acquired world covid data";
            } catch(IOException e) {
                result = "IO error: " + e.getMessage();
            }
        }
        // return result string
        return result;
    }

    /**
     * Method to write US totals to the database
     * @param lists of data to process
     */    
    private void writeUSToDatabase(List<List<String>> lists) {
        // initialize database variable
        DataBaseUtilities databaseUtilities = new DataBaseUtilities();
        try {
            // open connection to database
            Connection conn = databaseUtilities.connect(
                    DATABASE_CONNECTION_STRING, DATABASE_USER_NAME, 
                    DATABASE_USER_PASSWORD);
            // insert data in total table in database
            databaseUtilities.insertUSTotals(conn, lists);
            // close database connection
            databaseUtilities.closeConnection(conn);
        } catch (SQLException e) {
            // output SQL exception messages
            System.out.println(e.getMessage());
        } catch (ParseException e) {
            // output parse exception messages
            System.out.println(e.getMessage());
        }
    }
    
    /**
     * Method to write World totals to the database
     * @param lists of data to process
     */
    private void writeWorldToDatabase(List<List<String>> lists) {
        // initialize database variable
        DataBaseUtilities databaseUtilities = new DataBaseUtilities();
        try {
            // open connection to database
            Connection conn = databaseUtilities.connect(
                    DATABASE_CONNECTION_STRING, DATABASE_USER_NAME, 
                    DATABASE_USER_PASSWORD);
            // insert data in total table in database
            databaseUtilities.insertWorldTotals(conn, lists);   
            // close database connection
            databaseUtilities.closeConnection(conn);
        } catch (SQLException e) {
            // output SQL exception messages
            System.out.println(e.getMessage());
        } catch (ParseException e) {
            // output parse exception messages
            System.out.println(e.getMessage());
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
        DataBaseUtilities database = new DataBaseUtilities();
        try (
                Connection conn = database.connect(DATABASE_CONNECTION_STRING, 
                        DATABASE_USER_NAME, DATABASE_USER_PASSWORD);) {
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
             conn.close();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return newLists;
    }

    /**
     * Method to convert population and add to total population
     * @param totalPopulation current total
     * @param strings string with value to add
     * @return new total population
     */
    private long getTotalPopulation(long totalPopulation, List<String> strings) {
        // Declare variables
        String temp;
        // get population
        temp = strings.get(4);
        // remove commas
        temp = temp.replace(",", "");
        // remove bracketed value to avoid exception
        if (temp.contains("[")) {
            temp = temp.substring(0, temp.indexOf('['));
        }
        // if empty ignore
        if (!temp.isEmpty()) {
            // add to total
            totalPopulation += Long.parseLong(temp);
        }
        return totalPopulation;
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

//        put all data from files in delivered into database
//        String Path = "C:\\covid\\delivered";
//        String fileName = "";
//        //Creating a File object for directory
//        File directoryPath = new File(Path);
//        //List of all files and directories
//        String contents[] = directoryPath.list();
//        CSVUtilities csvUtilities = new CSVUtilities();
//        
//        DataBaseUtilities databaseUtilities = new DataBaseUtilities();
//        try {
//            Connection conn = databaseUtilities.connect(
//                    DATABASE_CONNECTION_STRING, DATABASE_USER_NAME, 
//                    DATABASE_USER_PASSWORD);
//            for (String content : contents) {
//                fileName = Path + "\\" + content;
//                System.out.println(fileName);
//                List<List<String>> csvList = csvUtilities.getCsvFile(fileName);
//                if (content.contains("world")) {
//                    databaseUtilities.insertWorldTotals(conn, csvList);
//                } else {
//                    databaseUtilities.insertUSTotals(conn, csvList);
//                } 
//            }
//            databaseUtilities.closeConnection(conn);
//        } catch (SQLException e) {
//            System.out.println(e.getMessage());
//        } catch (ParseException e) {
//            System.out.println(e.getMessage());
//        }


//        // add region to countries
//        ScrapeUtilities scrapeUtilities = new ScrapeUtilities();
//        List<List<String>> worldStrings = 
//                scrapeUtilities.getTableData(WORLDOMETER_ALL);
//        DataBaseUtilities databaseUtilities = new DataBaseUtilities();
//        try {
//            Connection conn = databaseUtilities.connect(DATABASE_CONNECTION_STRING,
//                    DATABASE_USER_NAME, DATABASE_USER_PASSWORD);
//            for (List<String> countryList : worldStrings) {
//                // 1= country 15 = region
//                if (!countryList.get(14).equals("")) {
//                    databaseUtilities.updateWorldRegion(conn, 
//                            countryList.get(1), countryList.get(15));
//                }
//            }
//            conn.close();
//        } catch(SQLException e) {
//            System.out.println(e.getMessage());
//        }


//        // Add countries and populations had to update populations after all were 0
//        String Path = "C:\\covid\\delivered";
//        String fileName = "";
//        //Creating a File object for directory
//        File directoryPath = new File(Path);
//        //List of all files and directories
//        String contents[] = directoryPath.list();
//        System.out.println("List of files and directories in the specified directory:");
//        for (String content : contents) {
//            if (content.contains("world")) {
//                fileName = Path + "\\" + content;
//                break;
//            }
//        }
//        CSVUtilities csvUtilities = new CSVUtilities();
//        List<List<String>> worldList = csvUtilities.getCsvFile(fileName);
//        DataBaseUtilities databaseUtilities = new DataBaseUtilities();
//        try {
//            Connection conn = databaseUtilities.connect(DATABASE_CONNECTION_STRING,
//                    DATABASE_USER_NAME, DATABASE_USER_PASSWORD);
//            for (List<String> worldList1 : worldList) {
//                if (!(worldList1.get(4).equals("") || 
//                        worldList1.get(4).equals("Population"))) {
//                    String countryCode = databaseUtilities.selectCountryCode(
//                            conn, worldList1.get(0).trim());
//                    //databaseUtilities.insertCountry(conn, worldList1.get(0),
//                    //        countryCode);
//                    String temp = worldList1.get(4);
//                    long population = 0;
//                    if (temp != null && !temp.equals("")) {
//                        temp = temp.replace(",", "");
//                        if (temp.contains("[")) {
//                            temp = temp.substring(0, temp.indexOf("["));
//                        }
//                        population = Long.valueOf(temp);
//                    }
//                    databaseUtilities.updateWorldPopulation(conn, 
//                            worldList1.get(0), population);
//                }
//            }
//            conn.close();
//        } catch(SQLException e) {
//            System.out.println(e.getMessage());
//        }