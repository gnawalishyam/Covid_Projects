/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobi.thalic.covid;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author GaryL
 */
public class DatabaseUtilities {
    
//    /**
//     * Method to backup database
//     * @param conn to database
//     * @param fileName
//     * @throws SQLException 
//     */
//    public void backupDatabase(Connection conn, String fileName) 
//            throws SQLException {
//        // Declare constant
//        final String BACKUP_DATABASE_SQL = "BACKUP DATABASE 'covid' TO DISK = ?";
//        try ( 
//            // statenent to use
//            PreparedStatement statement = 
//                    conn.prepareStatement(BACKUP_DATABASE_SQL)) {
//            // add country parameter to statement
//            statement.setString(1, fileName);
//            // run query
//            statement.execute();
//            // close statement
//            statement.close();
//        } 
//    }
    
    public void insertOurWorldInData(Connection conn, List<List<String>> lists) 
            throws SQLException, ParseException {
        // Declare constants
        final String INSERT_HISTORY_SQL = "INSERT INTO history (country_id,"
                + "history_date, total_cases, total_deaths, new_cases, "
                + "new_deaths) VALUES (?, ?, ?, ?, ?, ?);";
        // Declare variables
        // loop through list
        for (int i = 1; i < lists.size(); i++) {
            try ( 
                // statenent to use
                PreparedStatement statement = 
                        conn.prepareStatement(INSERT_HISTORY_SQL)) {
                // add country id parameter to 
                int countryId = 0;
                if (lists.get(i).get(0).equals("OWID_KOS")) {
                    lists.get(i).set(0, "XKX");
                } 
                countryId = selectCountryIdByAlpha3(conn, 
                    lists.get(i).get(0));
                if (lists.get(i).get(0).equals("OWID_WRL")) {
                    countryId = 220;
                }
                if (countryId == 0) {
                    String countryCode = selectCountryCodeByAlpha3(conn, 
                            lists.get(i).get(0));
                    if (countryCode.equals("")) {
                        System.out.println("Country code not listed: " + 
                                lists.get(i).get(0));
                    } else {
                        insertCountry(conn, lists.get(i).get(2), countryCode);
                        countryId = selectCountryIdByAlpha3(conn, 
                                lists.get(i).get(0));
                    }
                }
                if (countryId == 0) {
                    continue;
                }
                statement.setInt(1, countryId);
                // add date parameter to statement
                String tempString = lists.get(i).get(3);
                SimpleDateFormat format = new 
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                java.util.Date parsed = format.parse(tempString);
                java.sql.Date tempDate = new java.sql.Date(parsed.getTime());
                statement.setDate(2, tempDate);
                // add total cases parameter to statement
                long tempLong = 0;
                double tempDouble = 0.0;
                tempString = lists.get(i).get(4);
                if (!(tempString == null || tempString.equals(""))) {
                    tempDouble = Double.parseDouble(tempString);
                    tempLong = (long) tempDouble;
                }
                statement.setLong(3, tempLong);
                // add total deaths parameter to statement
                tempLong = 0;
                tempDouble = 0.0;
                tempString = lists.get(i).get(7);
                if (!(tempString == null || tempString.equals(""))) {
                    tempDouble = Double.parseDouble(tempString);
                    tempLong = (long) tempDouble;
                }
                statement.setLong(4, tempLong);
                // add new cases parameter to statement
                tempLong = 0;
                tempDouble = 0.0;
                tempString = lists.get(i).get(5);
                if (!(tempString == null || tempString.equals(""))) {
                    tempDouble = Double.parseDouble(tempString);
                    tempLong = (long) tempDouble;
                }
                statement.setLong(5, tempLong);
                // add new deathes parameter to statement
                tempLong = 0;
                tempDouble = 0.0;
                tempString = lists.get(i).get(8);
                if (!(tempString == null || tempString.equals(""))) {
                    tempDouble = Double.parseDouble(tempString);
                    tempLong = (long) tempDouble;
                }
                statement.setLong(6, tempLong);
                // run query
                statement.execute();
                // close statement
                statement.close();
            }
        }
    }
    
    /**
     * Method to populate country codes table
     * @param conn to database
     * @param lists of country codes to process 
     *      (country, alpha_2, alpha_3, numeric)
     * @throws SQLException 
     */
    public void insertCountryCodes(Connection conn, List<List<String>> lists) 
            throws SQLException {
        // Declare constant
        final String INSERT_COUNTRY_CODES_SQL = "INSERT INTO country_codes" + 
            " (country, alpha_2, alpha_3, numeric) VALUES (?, ?, ?, ?);";
        // loop through list
        for (int i = 27; i < lists.size(); i++) {
            // Declare and initialize temporary list
            List<String> temp = new ArrayList<>();
            // Check if extra element in list
            if(lists.get(i).size() > 4) {
                // add elements to list
                temp.add(lists.get(i).get(0) + "," + lists.get(i).get(1));
                temp.add(lists.get(i).get(2));
                temp.add(lists.get(i).get(3));
                temp.add(lists.get(i).get(4));
            } else {
                // add normal list
                temp = lists.get(i);
            }
            // add country parameter to statement
            try ( 
                // statenent to use
                PreparedStatement statement = 
                        conn.prepareStatement(INSERT_COUNTRY_CODES_SQL)) {
                // add country parameter to statement
                statement.setString(1, temp.get(0));
                // add alphs-2 parameter to statement
                statement.setString(2, temp.get(1));
                // add alpha-3 parameter to statement
                statement.setString(3, temp.get(2));
                // convert numeric to integer
                int numeric = Integer.parseInt(temp.get(3));
                // add numeric parameter to statement
                statement.setInt(4, numeric);
                // run query
                statement.execute();
                // close statement
                statement.close();
            }
        }
    }
  
    /**
     * Method to INSERT US totals into the database
     * @param conn to the database
     * @param lists to insert
     * @return results
     * @throws SQLException on SQL error
     * @throws ParseException on parse error
     */
    public String insertUSTotals(Connection conn, List<List<String>> lists) 
            throws SQLException, ParseException {
        // Declare constant
        final String INSERT_US_TOTALS_SQL = "INSERT INTO totals" + 
            " (state_id, total_cases, total_deaths, active_cases, total_date)" + 
                " VALUES (?, ?, ?, ?, ?);";
        // Declare variable
        String result = "";
        // loop through list
        for (int i = 1; i < lists.size(); i++) {
            if (!lists.get(i).get(0).equals("Total:")) {
                try ( 
                    // statenent to use
                    PreparedStatement statement = 
                            conn.prepareStatement(INSERT_US_TOTALS_SQL)) {
                    // add state id parameter to statement
                    int stateId = selectStateId(conn, lists.get(i).get(0));
                    if (stateId > 0) {
                        statement.setInt(1, stateId);
                        // add total cases parameter to statement
                        String tempString = lists.get(i).get(1);
                        long tempLong = 0L;
                        if (!(tempString.equals("") || tempString.equals("N/A"))) {
                            tempString = tempString.replace(",", "");
                            tempLong = Long.parseLong(tempString);
                        }
                        statement.setLong(2, tempLong);
                        // add total deaths parameter to statement
                        tempString = lists.get(i).get(2);
                        tempLong = 0L;
                        if (!(tempString.equals("") || tempString.equals("N/A"))) {
                            tempString = tempString.replace(",", "");
                            tempLong = Long.parseLong(tempString);
                        }
                        statement.setLong(3, tempLong);
                        // add active cases parameter to statement
                        tempString = lists.get(i).get(3);
                        tempLong = 0L;
                        if (!(tempString.equals("") || tempString.equals("N/A"))) {
                            tempString = tempString.replace(",", "");
                            tempLong = Long.parseLong(tempString);
                        }
                        statement.setLong(4, tempLong);
                        // add total date parameter to statement
                        tempString = lists.get(i).get(5);
                        SimpleDateFormat format = new 
                            SimpleDateFormat("yyyy_MM_dd", Locale.getDefault());
                        java.util.Date parsed = format.parse(tempString);
                        java.sql.Date tempDate = new java.sql.Date(parsed.getTime());
                        statement.setDate(5, tempDate);
                        // run query
                        statement.execute();
                    } else {
                        result += lists.get(i).get(0) + 
                                " does not exist in database.\n";
                    }
                    // close statement
                    statement.close();
                }
            }
        }
        return result;
    }

    /**
     * Method to INSERT World totals into database
     * @param conn to the database
     * @param lists to insert
     * @return results
     * @throws SQLException on SQL error
     * @throws ParseException on parse error
     */
    public String insertWorldTotals(Connection conn, List<List<String>> lists) 
            throws SQLException, ParseException {
        // Declare constant
        final String INSERT_COUNTRY_TOTALS_SQL = "INSERT INTO totals" + 
            " (country_id, total_cases, total_deaths, active_cases, total_date)" + 
                " VALUES (?, ?, ?, ?, ?);";
        // Declare variable
        String result = "";
        // loop through list
        for (int i = 1; i < lists.size(); i++) {
            try ( 
                // statenent to use
                PreparedStatement statement = 
                        conn.prepareStatement(INSERT_COUNTRY_TOTALS_SQL)) {
                // add country id parameter to statement
                int countryId = selectCountryId(conn, lists.get(i).get(0));
                if (countryId > 0) {
                    statement.setInt(1, countryId);
                    // add total cases parameter to statement
                    String tempString = lists.get(i).get(1);
                    long tempLong = 0L;
                    if (!(tempString.equals("") || tempString.equals("N/A"))) {
                        tempString = tempString.replace(",", "");
                        tempLong = Long.parseLong(tempString);
                    }
                    statement.setLong(2, tempLong);
                    // add total deaths parameter to statement
                    tempString = lists.get(i).get(2);
                    tempLong = 0L;
                    if (!(tempString.equals("") || tempString.equals("N/A"))) {
                        tempString = tempString.replace(",", "");
                        tempLong = Long.parseLong(tempString);
                    }
                    statement.setLong(3, tempLong);
                    // add active cases parameter to statement
                    tempString = lists.get(i).get(3);
                    tempLong = 0L;
                    if (!(tempString.equals("") || tempString.equals("N/A"))) {
                        tempString = tempString.replace(",", "");
                        tempLong = Long.parseLong(tempString);
                    }
                    statement.setLong(4, tempLong);
                    // add total date parameter to statement
                    tempString = lists.get(i).get(5);
                    SimpleDateFormat format = new 
                        SimpleDateFormat("yyyy_MM_dd", Locale.getDefault());
                    java.util.Date parsed = format.parse(tempString);
                    java.sql.Date tempDate = new java.sql.Date(parsed.getTime());
                    statement.setDate(5, tempDate);
                    // run query
                    statement.execute();
                } else {
                    result += lists.get(i).get(0) + 
                            " does not exist in database";
                }
                // close statement
                statement.close();
            }
        }
        return result;
    }
 
    /**
     * Method to insert population into database
     * @param conn to the database
     * @param state to enter
     * @param population to enter
     * @throws SQLException 
     */
    public void insertPopulation(Connection conn, String state, 
            long population) throws SQLException {
        // declare constant
        final String INSERT_POPULATION_SQL = 
            "INSERT INTO populations (state_id, population) VALUES (?, ?);";
        // add state parameter
        try ( 
            // statement to use
            PreparedStatement statement = 
                    conn.prepareStatement(INSERT_POPULATION_SQL)) {
            // add state parameter
            statement.setInt(1, selectStateId(conn, state));
            //add population parameter
            statement.setLong(2, population);
            // run statement
            statement.execute();
            //clase statement
            statement.close();
        }
    }
    
    /**
     * Method to update the state populations
     * @param conn to database
     * @param state of the population to update
     * @param population to update
     * @throws SQLException that could be thrown
     */
    public void updateStatePopulation(Connection conn, String state, 
            long population) throws SQLException {
        // declare constant
        final String UPDATE_POPULATION_SQL = 
            "UPDATE populations SET population = ? WHERE state_id = ?;";
        // add state parameter
        try ( 
            // statement to use
            PreparedStatement statement = 
                    conn.prepareStatement(UPDATE_POPULATION_SQL)) {
            //add population parameter
            statement.setLong(1, population);
            // add state parameter
            statement.setInt(2, selectStateId(conn, state));
            // run statement
            statement.execute();
            //clase statement
            statement.close();
        }
    }
    
    /**
     * Method to get state id from database
     * @param conn to database
     * @param state to get id for
     * @return state id
     * @throws SQLException 
     */
    public int selectStateId(Connection conn, String state) 
            throws SQLException {
        // Declare constant
        final String SELECT_STATE_ID = 
            "SELECT id FROM states WHERE state = ?;";
        // Declare variables
        int stateId = 0;
        // add state parameter
        try ( 
            // statement to use
            PreparedStatement statement = 
                    conn.prepareStatement(SELECT_STATE_ID)) {
            // add state parameter
            statement.setString(1, state);
            // check if results
            try ( 
                // run query and get results
                ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    // get state id from results
                    stateId = resultSet.getInt(1);
                } 
                // close results
                resultSet.close();
                // close statement
                statement.close();
            }
        }
        // return state id
        return stateId;
    }
    
    public void insertCountry(Connection conn, String country, 
            String countryCode) throws SQLException {
        // declare constant
        final String INSERT_COUNTRY_SQL = 
            "INSERT INTO countries (country, country_code) VALUES (?, ?);";
        // add state parameter
        try ( 
            // statement to use
            PreparedStatement statement = 
                    conn.prepareStatement(INSERT_COUNTRY_SQL)) {
            // add state parameter
            statement.setString(1, country);
            //add population parameter
            statement.setString(2, countryCode);
            // run statement
            statement.execute();
            // close statement
            statement.close();
        }
    }
    
    public void insertWorldPopulation(Connection conn, String country, 
            long population) throws SQLException {
        // declare constant
        final String INSERT_POPULATION_SQL = 
            "INSERT INTO populations (country_id, population) VALUES (?, ?);";
        // add state parameter
        try ( 
            // statement to use
            PreparedStatement statement = 
                    conn.prepareStatement(INSERT_POPULATION_SQL)) {
            // add state parameter
            statement.setInt(1, selectCountryId(conn, country));
            //add population parameter
            statement.setLong(2, population);
            // run statement
            statement.execute();
            //clase statement
            statement.close();
        }
    }
    
    /**
     * Method to update world populations
     * @param conn to database
     * @param country of population to update
     * @param population to update
     * @throws SQLException that could be thrown 
     */
    public void updateWorldPopulation(Connection conn, String country, 
            long population) throws SQLException {
        // declare constant
        final String UPDATE_POPULATION_SQL = 
            "UPDATE populations SET population = ? WHERE country_id = ?;";
        // add country parameter
        try ( 
            // statement to use
            PreparedStatement statement = 
                    conn.prepareStatement(UPDATE_POPULATION_SQL)) {
            //add population parameter
            statement.setLong(1, population);
            // add state parameter
            statement.setInt(2, selectCountryId(conn, country));
            // run statement
            statement.execute();
            //clase statement
            statement.close();
        }
    }
    
    public void updateWorldRegion(Connection conn, String country, 
            String region) throws SQLException {
        // declare constant
        final String UPDATE_REGION_SQL = 
            "UPDATE countries SET region = ? WHERE country = ?;";
        // add state parameter
        try ( 
            // statement to use
            PreparedStatement statement = 
                    conn.prepareStatement(UPDATE_REGION_SQL)) {
            //add population parameter
            statement.setString(1, region);
            // add state parameter
            statement.setString(2, country);
            // run statement
            statement.execute();
            //clase statement
            statement.close();
        }
    }
    
    /**
     * Method to get the country id
     * @param conn of database
     * @param country to get id for
     * @return country id
     * @throws SQLException if an error
     */
    public int selectCountryId(Connection conn, String country) 
            throws SQLException {
        // Declare constant
        final String SELECT_COUNTRY_ID = 
            "SELECT id FROM countries WHERE country = ?;";
        // Declare variables
        int countryId = 0;
        String tempString = country;
        // add country parameter
        if (tempString.equals("RÃ©union")) {
            tempString = "Réunion";
        }
        if (tempString.equals("CuraÃ§ao")) {
            tempString = "Curaçao";
        }
        if (tempString.equals("Oceania")) {
            tempString = "Australia/Oceania";
        }
        try (     
            // statement to use
            PreparedStatement statement = 
                    conn.prepareStatement(SELECT_COUNTRY_ID)) {
            // add country parameter
            statement.setString(1, tempString);
            // check if results
            try (
                // run query and get results
                ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    // get country id from results
                    countryId = resultSet.getInt(1);
                } 
                // close results
                resultSet.close();
                // close statement
                statement.close();
            }
        }
        // check id
        if(countryId < 1) {
            insertCountry(conn, tempString, 
                    selectCountryCode(conn, tempString));
            try (     
                // statement to use
                PreparedStatement statement1 = 
                    conn.prepareStatement(SELECT_COUNTRY_ID)) {
                // add country parameter
                statement1.setString(1, tempString);
                // run query and get results
                try (
                    ResultSet resultSet1 = statement1.executeQuery()) {
                    // check if results
                    while (resultSet1.next()) {
                        // get country id from results
                        countryId = resultSet1.getInt(1);
                    } 
                    // close results
                    resultSet1.close();
                    // close statement
                    statement1.close();
                }
            }
        }
        // return country id
        return countryId;
    }
    
    public int selectCountryIdByAlpha3(Connection conn, String alpha3) 
            throws SQLException {
        // Declare constant
        final String SELECT_COUNTRY_ID = 
            "SELECT countries.id FROM countries INNER JOIN country_codes ON "
                + "country_code = alpha_2 WHERE alpha_3 = ?;";
        // Declare variables
        int countryId = 0;
        // add country parameter
        try ( 
            // statement to use
            PreparedStatement statement = 
                    conn.prepareStatement(SELECT_COUNTRY_ID)) {
            // add state parameter
            statement.setString(1, alpha3);
            // check if results
            try ( 
                // run query and get results
                ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    // get state id from results
                    countryId = resultSet.getInt(1);
                } 
                // close results
                resultSet.close();
                // close statement
                statement.close();
            }
        }
        // return state id
        return countryId;
    }
    
    public String selectCountryCodeByAlpha3(Connection conn, String alpha3) 
            throws SQLException {
        // Declare constant
        final String SELECT_COUNTRY_CODE = 
            "SELECT alpha_2 FROM country_codes WHERE alpha_3 = ?;";
        // Declare variables
        String countryCode = "";
        // add alpha3 parameter
        try ( 
            // statement to use
            PreparedStatement statement = 
                    conn.prepareStatement(SELECT_COUNTRY_CODE)) {
            // add state parameter
            statement.setString(1, alpha3);
            // check if results
            try ( 
                // run query and get results
                ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    // get country code from results
                    countryCode = resultSet.getString(1);
                } 
                // close results
                resultSet.close();
                // close statement
                statement.close();
            }
        }
        return countryCode;
    }
    
    public String selectCountryCode(Connection conn, String country) 
            throws SQLException {
        // Declare constant
        final String SELECT_COUNTRY_CODE = 
            "SELECT alpha_2 FROM country_codes WHERE country = ?;";
        // Declare variables
        String countryCode = "";
        // add state parameter
        try ( 
            // statement to use
            PreparedStatement statement = 
                    conn.prepareStatement(SELECT_COUNTRY_CODE)) {
            // add state parameter
            statement.setString(1, country);
            // check if results
            try ( 
                // run query and get results
                ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    // get state id from results
                    countryCode = resultSet.getString(1);
                } 
                // close results
                resultSet.close();
                // close statement
                statement.close();
            }
        }
        // return state id
        return countryCode;
    }

    /**
     * Method to get state population
     * @param conn to database
     * @param state to get population for
     * @return population
     * @throws SQLException 
     */
    public long selectStatePopulation(Connection conn, String state) 
            throws SQLException {
        // Declare constant
        final String SELECT_POPULATION = 
            "SELECT population FROM populations WHERE state_id = ?;";
        // Declare variables
        long population = 0;
        //add state paramenter to statement
        try ( 
            // statement to use
            PreparedStatement statement = 
                    conn.prepareStatement(SELECT_POPULATION)) {
            //add state paramenter to statement
            statement.setInt(1, selectStateId(conn, state));
            // check for result(s)
            try ( 
                // run query with results
                ResultSet resultSet = statement.executeQuery()) {
                // check for result(s)
                while (resultSet.next()) {
                    // get population from results
                    population = resultSet.getLong(1);
                } 
                // close results
                resultSet.close();
                // close statement
                statement.close();
            }
        }
        // return population
        return population;
    }
    
    /**
     * Method to get world population
     * @param conn to database
     * @param country to get population for
     * @return population
     * @throws SQLException that could be thrown
     */
    public long selectWorldPopulation(Connection conn, String country) 
            throws SQLException {
        // Declare constant
        final String SELECT_POPULATION = 
            "SELECT population FROM populations WHERE country_id = ?;";
        // Declare variables
        long population = 0;
        //add state paramenter to statement
        try ( 
            // statement to use
            PreparedStatement statement = 
                    conn.prepareStatement(SELECT_POPULATION)) {
            //add state paramenter to statement
            statement.setInt(1, selectCountryId(conn, country));
            // check for result(s)
            try ( 
                // run query with results
                ResultSet resultSet = statement.executeQuery()) {
                // check for result(s)
                while (resultSet.next()) {
                    // get population from results
                    population = resultSet.getLong(1);
                } 
                // close results
                resultSet.close();
                // close statement
                statement.close();
            }
        }
        // return population
        return population;
    }

    /**
     * insert multiple states
     * @param list of states
     * @param conn to the database
     * @throws SQLException 
     */
    public void insertStates(List<String> list, Connection conn) 
            throws SQLException {
        // Declare constant
        final String INSERT_STATES_SQL = 
                "INSERT INTO states (state) VALUES (?);";
        // declare and initialize count
        try ( 
            // Statement to use
            PreparedStatement statement = 
                    conn.prepareStatement(INSERT_STATES_SQL)) {
            // declare and initialize count
            int count = 0;
            // create group of statements
            for (String state : list) {
                // add state as parameter to statement
                statement.setString(1, state);
                // add to other statement
                statement.addBatch();
                // increment count
                count++;
                // execute every 100 rows or less
                if (count % 100 == 0 || count == list.size()) {
                    // run group query
                    statement.executeBatch();
                }
            }
            // close statement
            statement.close();
        }
    }

    /**
     * Method to establish connection to database
     * @param connectionString database to connect to
     * @param userName of database user
     * @param userPassword for the user
     * @return usable connection
     * @throws SQLException
     */
    public Connection connect(String connectionString, String userName, 
            String userPassword) throws SQLException {
        // Declare variables
        Connection conn;
        // Attempt to connect to database
        conn = DriverManager.getConnection(connectionString, 
                userName, userPassword);
        // return usable connection or null
        return conn;
    }
    
    /**
     * Method to close connection if one exists
     * @param conn to close
     * @throws SQLException 
     */
    public void closeConnection(Connection conn) throws SQLException {
        // check if there is a connection
        if (conn != null) {
            // close connection
            conn.close();
        }
    }
}
