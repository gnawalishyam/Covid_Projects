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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Gary Larson gary@thalic.mobi
 */
public class MySqlToMysqlDowngrade {
    private final HashMap<String, String> configMap = new HashMap<>();
    
    /**
     * Default constructor
     */
    public MySqlToMysqlDowngrade () {
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
    
    public String transferCountryCodes() {
        // initialize variables
        String result = "Done";
        List<String> countryList = new ArrayList();
        List<String> alpha_2List = new ArrayList();
        List<String> alpha_3List = new ArrayList();
        List<Integer> numericList = new ArrayList();
        List<Integer> regionList = new ArrayList();
        List<String> displayList = new ArrayList();
        List<Long> populationList = new ArrayList();
        Connection connMysql57 = null, connMysql = null;
        // declare constant
        final String SELECT_COUNTRY_CODES_SQL = 
                "SELECT country, alpha_2, alpha_3, `numeric`, region, "
                + "display, population "
                + "FROM country_codes";
        final String INSERT_COUNTRY_CODES_SQL = 
            "INSERT INTO country_codes (country, alpha_2, alpha_3, `numeric`, "
                + "region, display, population) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try {
            // open connection to database
            connMysql = 
                    connect(configMap.get("MYSQL_CONNECTION"), 
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
         
            // run query and get results
            try ( // statement to use
                    PreparedStatement statement = 
                            connMysql.prepareStatement(SELECT_COUNTRY_CODES_SQL); // run query and get results
                    ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    // create lists for later insertion
                    countryList.add(resultSet.getString("country"));
                    alpha_2List.add(resultSet.getString("alpha_2"));
                    alpha_3List.add(resultSet.getString("alpha_3"));
                    numericList.add(resultSet.getInt("numeric"));
                    regionList.add(resultSet.getInt("region"));
                    displayList.add(resultSet.getString("display"));
                    populationList.add(resultSet.getLong("population"));
                } 
                resultSet.close();
                statement.close();
            }
        } catch (SQLException e) {
            // output SQL exception messages
            result = e.getMessage() + "\n";
        } finally {
            // close database connection
            if (connMysql != null) {
                try {
                    closeConnection(connMysql);
                } catch (SQLException e) {
                    // output SQL exception messages
                    result = e.getMessage() + "\n";
                }
            }
        }
        try {
            // open database connection
            connMysql57 = connect(configMap.get("MYSQL57_CONNECTION"),
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
            for (int i = 0; i < countryList.size(); i++) {
                // set parameters for statement 2 from statement 1
                // add country parameter to statement
                try (PreparedStatement statement2 = 
                        connMysql57.prepareStatement(INSERT_COUNTRY_CODES_SQL)) {
                    // set parameters for statement 2 from statement 1
                    // add country parameter to statement
                    statement2.setString(1, countryList.get(i));
                    // add alphs-2 parameter to statement
                    statement2.setString(2, alpha_2List.get(i));
                    // add alpha-3 parameter to statement
                    statement2.setString(3, alpha_3List.get(i));
                    // add numeric parameter to statement
                    statement2.setInt(4, numericList.get(i));
                    // add region parameter to statement
                    statement2.setInt(5, regionList.get(i));
                    // add display parameter to statement
                    statement2.setString(6, displayList.get(i));
                    // add population parameter to statement
                    statement2.setLong(7, populationList.get(i));
                    // run query
                    statement2.execute();
                    statement2.close();
                }
            }
        } catch (SQLException e) {
            // output SQL exception messages
            result = e.getMessage() + "\n";
        } finally {
            if (connMysql57 != null) {
                try {
                    closeConnection(connMysql57);
                } catch (SQLException e) {
                    // output SQL exception messages
                    result = e.getMessage() + "\n";
                }
            }
        }
        return result;
    }
    
    public String transferCountryLabels() {
        // initialize variables
        String result = "Done";
        List<String> countryList = new ArrayList();
        List<String> labelList = new ArrayList();
        Connection connMysql57 = null, connMysql = null;
        // declare constant
        final String SELECT_COUNTRY_LABELS_SQL = 
                "SELECT country, label "
                + "FROM country_labels INNER JOIN country_codes "
                + "ON country_labels.country_id = country_codes.id;";
        final String INSERT_COUNTRY_LABELS_SQL = 
            "INSERT INTO country_labels (country_id, label) VALUES (?, ?);";
        try {
            // open connection to database
            connMysql = 
                    connect(configMap.get("MYSQL_CONNECTION"), 
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
         
            // run query and get results
            try ( // statement to use
                    PreparedStatement statement = 
                            connMysql.prepareStatement(SELECT_COUNTRY_LABELS_SQL); 
                    // run query and get results
                    ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    // create lists for later insertion
                    countryList.add(resultSet.getString("country"));
                    labelList.add(resultSet.getString("label"));
                } 
                resultSet.close();
                statement.close();
            }
        } catch (SQLException e) {
            // output SQL exception messages
            result = e.getMessage() + "\n";
        } finally {
            // close database connection
            if (connMysql != null) {
                try {
                    closeConnection(connMysql);
                } catch (SQLException e) {
                    // output SQL exception messages
                    result = e.getMessage() + "\n";
                }
            }
        }
        try {
            // open database connection
            connMysql57 = connect(configMap.get("MYSQL57_CONNECTION"),
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
            for (int i = 0; i < countryList.size(); i++) {
                // set parameters for statement 2 from statement 1
                // add country parameter to statement
                try (PreparedStatement statement2 = 
                        connMysql57.prepareStatement(INSERT_COUNTRY_LABELS_SQL)) {
                    // set parameters for statement 2 from statement 1
                    // add country id parameter to statement
                    int tempInt = getCountryId(connMysql57, countryList.get(i));
                    statement2.setInt(1, tempInt);
                    // add label parameter to statement
                    statement2.setString(2, labelList.get(i));
                    // run query
                    statement2.execute();
                    statement2.close();
                }
            }
        } catch (SQLException e) {
            // output SQL exception messages
            result = e.getMessage() + "\n";
        } finally {
            if (connMysql57 != null) {
                try {
                    closeConnection(connMysql57);
                } catch (SQLException e) {
                    // output SQL exception messages
                    result = e.getMessage() + "\n";
                }
            }
        }
        return result;
    }
    
    private int getCountryId(Connection conn, String country) throws SQLException {
        final String SELECT_COUNTRY_ID_SQL = 
                "SELECT country_id FROM country_labels WHERE label = ?;";
        int countryId = 0;
        // run query and get results
        try ( // statement to use
            PreparedStatement statement = 
                    conn.prepareStatement(SELECT_COUNTRY_ID_SQL)) {
            // add country parameter to statement
            statement.setString(1, country);

            // run query and get results
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                countryId = resultSet.getInt("country_id");
            }
        }
       return countryId;     
    }
    
    public String transferCountryTotals() {
        // initialize variables
        String result = "Done";
        List<String> countryList = new ArrayList();
        List<Long> casesList = new ArrayList();
        List<Long> deathsList = new ArrayList();
        List<Long> activeList = new ArrayList();
        List<Date> dateList = new ArrayList();
        Connection connMysql57 = null, connMysql = null;
        final String SELECT_COUNTRY_TOTALS_SQL = 
                "SELECT country, cases, deaths, `active`, `date` "
                + "FROM country_totals INNER JOIN country_codes "
                + "ON country_totals.country_id = country_codes.id;";
        final String INSERT_COUNTRY_TOTALS_SQL = 
            "INSERT INTO country_totals (country_id, cases, deaths, `active`, "
                + "`date`) VALUES (?, ?, ?, ?, ?);";
        try {
            // open connection to database
            connMysql = 
                    connect(configMap.get("MYSQL_CONNECTION"), 
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
         
            // run query and get results
            try ( // statement to use
                    PreparedStatement statement = 
                            connMysql.prepareStatement(SELECT_COUNTRY_TOTALS_SQL); 
                    // run query and get results
                    ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    // create lists for later insertion
                    countryList.add(resultSet.getString("country"));
                    casesList.add(resultSet.getLong("cases"));
                    deathsList.add(resultSet.getLong("deaths"));
                    activeList.add(resultSet.getLong("active"));
                    dateList.add(resultSet.getDate("date"));
                } 
                resultSet.close();
                statement.close();
            }
        } catch (SQLException e) {
            // output SQL exception messages
            result = e.getMessage() + "\n";
        } finally {
            // close database connection
            if (connMysql != null) {
                try {
                    closeConnection(connMysql);
                } catch (SQLException e) {
                    // output SQL exception messages
                    result = e.getMessage() + "\n";
                }
            }
        }
        try {
            // open database connection
            connMysql57 = connect(configMap.get("MYSQL57_CONNECTION"),
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
            for (int i = 0; i < countryList.size(); i++) {
                // set parameters for statement 2 from statement 1
                // add country parameter to statement
                try (PreparedStatement statement2 = 
                        connMysql57.prepareStatement(INSERT_COUNTRY_TOTALS_SQL)) {
                    // set parameters for statement 2 from statement 1
                    // add country id parameter to statement
                    int tempInt = getCountryId(connMysql57, countryList.get(i));
                    statement2.setInt(1, tempInt);
                    // add cases parameter to statement
                    statement2.setLong(2, casesList.get(i));
                    // add deaths parameter to statement
                    statement2.setLong(3, deathsList.get(i));
                    // add active parameter to statement
                    statement2.setLong(4, activeList.get(i));
                    // add cases parameter to statement
                    statement2.setDate(5, dateList.get(i));
                    // run query
                    statement2.execute();
                    statement2.close();
                }
            }
        } catch (SQLException e) {
            // output SQL exception messages
            result = e.getMessage() + "\n";
        } finally {
            if (connMysql57 != null) {
                try {
                    closeConnection(connMysql57);
                } catch (SQLException e) {
                    // output SQL exception messages
                    result = e.getMessage() + "\n";
                }
            }
        }
        return result;
    }
    
    public String transferCountryDailies() {
        // initialize variables
        String result = "Done";
        List<String> countryList = new ArrayList();
        List<Long> casesList = new ArrayList();
        List<Long> deathsList = new ArrayList();
        List<Long> recoveredList = new ArrayList();
        List<Date> dateList = new ArrayList();
        Connection connMysql57 = null, connMysql = null;
        final String SELECT_COUNTRY_DAILIES_SQL = 
                "SELECT country, cases, deaths, recovered, `date` "
                + "FROM country_dailies INNER JOIN country_codes "
                + "ON country_dailies.country_id = country_codes.id;";
        final String INSERT_COUNTRY_DAILIES_SQL = 
            "INSERT INTO country_dailies (country_id, cases, deaths, recovered, "
                + "`date`) VALUES (?, ?, ?, ?, ?);";
        try {
            // open connection to database
            connMysql = 
                    connect(configMap.get("MYSQL_CONNECTION"), 
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
         
            // run query and get results
            try ( // statement to use
                    PreparedStatement statement = 
                            connMysql.prepareStatement(SELECT_COUNTRY_DAILIES_SQL); 
                    // run query and get results
                    ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    // create lists for later insertion
                    countryList.add(resultSet.getString("country"));
                    casesList.add(resultSet.getLong("cases"));
                    deathsList.add(resultSet.getLong("deaths"));
                    recoveredList.add(resultSet.getLong("recovered"));
                    dateList.add(resultSet.getDate("date"));
                } 
                resultSet.close();
                statement.close();
            }
        } catch (SQLException e) {
            // output SQL exception messages
            result = e.getMessage() + "\n";
        } finally {
            // close database connection
            if (connMysql != null) {
                try {
                    closeConnection(connMysql);
                } catch (SQLException e) {
                    // output SQL exception messages
                    result = e.getMessage() + "\n";
                }
            }
        }
        try {
            // open database connection
            connMysql57 = connect(configMap.get("MYSQL57_CONNECTION"),
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
            for (int i = 0; i < countryList.size(); i++) {
                // set parameters for statement 2 from statement 1
                // add country parameter to statement
                try (PreparedStatement statement2 = 
                        connMysql57.prepareStatement(INSERT_COUNTRY_DAILIES_SQL)) {
                    // set parameters for statement 2 from statement 1
                    // add country id parameter to statement
                    int tempInt = getCountryId(connMysql57, countryList.get(i));
                    statement2.setInt(1, tempInt);
                    // add cases parameter to statement
                    statement2.setLong(2, casesList.get(i));
                    // add deaths parameter to statement
                    statement2.setLong(3, deathsList.get(i));
                    // add active parameter to statement
                    statement2.setLong(4, recoveredList.get(i));
                    // add cases parameter to statement
                    statement2.setDate(5, dateList.get(i));
                    // run query
                    statement2.execute();
                    statement2.close();
                }
            }
        } catch (SQLException e) {
            // output SQL exception messages
            result = e.getMessage() + "\n";
        } finally {
            if (connMysql57 != null) {
                try {
                    closeConnection(connMysql57);
                } catch (SQLException e) {
                    // output SQL exception messages
                    result = e.getMessage() + "\n";
                }
            }
        }
        return result;
    }
    
    public String transferStates() {
    // initialize variables
        String result = "Done";
        List<String> stateList = new ArrayList();
        List<Long> populationList = new ArrayList();
        Connection connMysql57 = null, connMysql = null;
        // declare constant
        final String SELECT_STATES_SQL = 
                "SELECT state, population "
                + "FROM states";
        final String INSERT_STATES_SQL = 
            "INSERT INTO states (state, population) VALUES (?, ?);";
        try {
            // open connection to database
            connMysql = 
                    connect(configMap.get("MYSQL_CONNECTION"), 
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
         
            // run query and get results
            try ( // statement to use
                    PreparedStatement statement = 
                            connMysql.prepareStatement(SELECT_STATES_SQL); 
                    // run query and get results
                    ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    // create lists for later insertion
                    stateList.add(resultSet.getString("state"));
                    populationList.add(resultSet.getLong("population"));
                } 
                resultSet.close();
                statement.close();
            }
        } catch (SQLException e) {
            // output SQL exception messages
            result = e.getMessage() + "\n";
        } finally {
            // close database connection
            if (connMysql != null) {
                try {
                    closeConnection(connMysql);
                } catch (SQLException e) {
                    // output SQL exception messages
                    result = e.getMessage() + "\n";
                }
            }
        }
        try {
            // open database connection
            connMysql57 = connect(configMap.get("MYSQL57_CONNECTION"),
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
            for (int i = 0; i < stateList.size(); i++) {
                // set parameters for statement 2 from statement 1
                // add country parameter to statement
                try (PreparedStatement statement2 = 
                        connMysql57.prepareStatement(INSERT_STATES_SQL)) {
                    // set parameters for statement 2 from statement 1
                    // add state parameter to statement
                    statement2.setString(1, stateList.get(i));
                    // add population parameter to statement
                    statement2.setLong(2, populationList.get(i));
                    // run query
                    statement2.execute();
                    statement2.close();
                }
            }
        } catch (SQLException e) {
            // output SQL exception messages
            result = e.getMessage() + "\n";
        } finally {
            if (connMysql57 != null) {
                try {
                    closeConnection(connMysql57);
                } catch (SQLException e) {
                    // output SQL exception messages
                    result = e.getMessage() + "\n";
                }
            }
        }
        return result;
    }
    
    private int getStateId(Connection conn, String state) throws SQLException {
        final String SELECT_STATE_ID_SQL = 
                "SELECT id FROM states WHERE state = ?;";
        int stateId = 0;
        // run query and get results
        try ( // statement to use
            PreparedStatement statement = conn.prepareStatement(SELECT_STATE_ID_SQL)) {
            // add state parameter to statement
            statement.setString(1, state);

            // run query and get results
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                stateId = resultSet.getInt("id");
            }
        }
       return stateId;     
    }
    
    public String transferStateDailies() {
        // initialize variables
        String result = "Done";
        List<String> stateList = new ArrayList();
        List<Long> casesList = new ArrayList();
        List<Long> deathsList = new ArrayList();
        List<Long> recoveredList = new ArrayList();
        List<Date> dateList = new ArrayList();
        Connection connMysql57 = null, connMysql = null;
        final String SELECT_STATE_DAILIES_SQL = 
                "SELECT state, cases, deaths, recovered, `date` "
                + "FROM state_dailies INNER JOIN states "
                + "ON state_dailies.state_id = states.id;";
        final String INSERT_STATE_DAILIES_SQL = 
            "INSERT INTO state_dailies (state_id, cases, deaths, recovered, "
                + "`date`) VALUES (?, ?, ?, ?, ?);";
        try {
            // open connection to database
            connMysql = 
                    connect(configMap.get("MYSQL_CONNECTION"), 
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
         
            // run query and get results
            try ( // statement to use
                    PreparedStatement statement = 
                            connMysql.prepareStatement(SELECT_STATE_DAILIES_SQL); 
                    // run query and get results
                    ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    // create lists for later insertion
                    stateList.add(resultSet.getString("state"));
                    casesList.add(resultSet.getLong("cases"));
                    deathsList.add(resultSet.getLong("deaths"));
                    recoveredList.add(resultSet.getLong("recovered"));
                    dateList.add(resultSet.getDate("date"));
                } 
                resultSet.close();
                statement.close();
            }
        } catch (SQLException e) {
            // output SQL exception messages
            result = e.getMessage() + "\n";
        } finally {
            // close database connection
            if (connMysql != null) {
                try {
                    closeConnection(connMysql);
                } catch (SQLException e) {
                    // output SQL exception messages
                    result = e.getMessage() + "\n";
                }
            }
        }
        try {
            // open database connection
            connMysql57 = connect(configMap.get("MYSQL57_CONNECTION"),
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
            for (int i = 0; i < stateList.size(); i++) {
                // set parameters for statement 2 from statement 1
                // add state parameter to statement
                try (PreparedStatement statement2 = 
                        connMysql57.prepareStatement(INSERT_STATE_DAILIES_SQL)) {
                    // set parameters for statement 2 from statement 1
                    // add state id parameter to statement
                    int tempInt = getStateId(connMysql57, stateList.get(i));
                    statement2.setInt(1, tempInt);
                    // add cases parameter to statement
                    statement2.setLong(2, casesList.get(i));
                    // add deaths parameter to statement
                    statement2.setLong(3, deathsList.get(i));
                    // add active parameter to statement
                    statement2.setLong(4, recoveredList.get(i));
                    // add cases parameter to statement
                    statement2.setDate(5, dateList.get(i));
                    // run query
                    statement2.execute();
                    statement2.close();
                }
            }
        } catch (SQLException e) {
            // output SQL exception messages
            result = e.getMessage() + "\n";
        } finally {
            if (connMysql57 != null) {
                try {
                    closeConnection(connMysql57);
                } catch (SQLException e) {
                    // output SQL exception messages
                    result = e.getMessage() + "\n";
                }
            }
        }
        return result;
    }
    
    
    
    public String transferStateTotals() {
        // initialize variables
        String result = "Done";
        List<String> stateList = new ArrayList();
        List<Long> casesList = new ArrayList();
        List<Long> deathsList = new ArrayList();
        List<Long> activeList = new ArrayList();
        List<Date> dateList = new ArrayList();
        Connection connMysql57 = null, connMysql = null;
        final String SELECT_STATE_TOTALS_SQL = 
                "SELECT state, cases, deaths, `active`, `date` "
                + "FROM state_totals INNER JOIN states "
                + "ON state_totals.state_id = states.id;";
        final String INSERT_STATE_TOTALS_SQL = 
            "INSERT INTO state_totals (state_id, cases, deaths, `active`, `date`) VALUES (?, ?, ?, ?, ?);";
        try {
            // open connection to database
            connMysql = 
                    connect(configMap.get("MYSQL_CONNECTION"), 
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
         
            // run query and get results
            try ( // statement to use
                    PreparedStatement statement = connMysql.prepareStatement(SELECT_STATE_TOTALS_SQL); 
                    // run query and get results
                    ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    // create lists for later insertion
                    stateList.add(resultSet.getString("state"));
                    casesList.add(resultSet.getLong("cases"));
                    deathsList.add(resultSet.getLong("deaths"));
                    activeList.add(resultSet.getLong("active"));
                    dateList.add(resultSet.getDate("date"));
                } 
                resultSet.close();
                statement.close();
            }
        } catch (SQLException e) {
            // output SQL exception messages
            result = e.getMessage() + "\n";
        } finally {
            // close database connection
            if (connMysql != null) {
                try {
                    closeConnection(connMysql);
                } catch (SQLException e) {
                    // output SQL exception messages
                    result = e.getMessage() + "\n";
                }
            }
        }
        try {
            // open database connection
            connMysql57 = connect(configMap.get("MYSQL57_CONNECTION"),
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
            for (int i = 0; i < stateList.size(); i++) {
                // set parameters for statement 2 from statement 1
                // add state parameter to statement
                try (PreparedStatement statement2 = connMysql57.prepareStatement(INSERT_STATE_TOTALS_SQL)) {
                    // set parameters for statement 2 from statement 1
                    // add state id parameter to statement
                    int tempInt = getStateId(connMysql57, stateList.get(i));
                    statement2.setInt(1, tempInt);
                    // add cases parameter to statement
                    statement2.setLong(2, casesList.get(i));
                    // add deaths parameter to statement
                    statement2.setLong(3, deathsList.get(i));
                    // add active parameter to statement
                    statement2.setLong(4, activeList.get(i));
                    // add cases parameter to statement
                    statement2.setDate(5, dateList.get(i));
                    // run query
                    statement2.execute();
                    statement2.close();
                }
            }
        } catch (SQLException e) {
            // output SQL exception messages
            result = e.getMessage() + "\n";
        } finally {
            if (connMysql57 != null) {
                try {
                    closeConnection(connMysql57);
                } catch (SQLException e) {
                    // output SQL exception messages
                    result = e.getMessage() + "\n";
                }
            }
        }
        return result;
    }
    
    public String transferCountryHistory() {
        // initialize variables
        String result = "Done";
        List<String> countryList = new ArrayList();
        List<Long> casesList = new ArrayList();
        List<Long> deathsList = new ArrayList();
        List<Long> activeList = new ArrayList();
        List<Date> dateList = new ArrayList();
        List<Long> newCasesList = new ArrayList();
        List<Long> newDeathsList = new ArrayList();
        Connection connMysql57 = null, connMysql = null;
        final String SELECT_HISTORY_SQL = 
                "SELECT country, `date`, cases, deaths, "
                + "`active`, new_cases, new_deaths "
                + "FROM country_codes INNER JOIN country_history "
                + "ON country_codes.id = country_history.country_id;";
        final String INSERT_HISTORY_SQL = 
            "INSERT INTO country_history (country_id, `date`, cases, deaths, "
                + "`active`, new_cases, new_deaths) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try {
            // open connection to database
            connMysql = 
                    connect(configMap.get("MYSQL_CONNECTION"), 
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
         
            // run query and get results
            try ( // statement to use
                    PreparedStatement statement = connMysql.prepareStatement(SELECT_HISTORY_SQL); 
                    // run query and get results
                    ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    // create lists for later insertion
                    countryList.add(resultSet.getString("country"));
                    casesList.add(resultSet.getLong("cases"));
                    deathsList.add(resultSet.getLong("deaths"));
                    activeList.add(resultSet.getLong("active"));
                    dateList.add(resultSet.getDate("date"));
                    newCasesList.add(resultSet.getLong("new_cases"));
                    newDeathsList.add(resultSet.getLong("new_deaths"));
                } 
                resultSet.close();
                statement.close();
            }
        } catch (SQLException e) {
            // output SQL exception messages
            result = e.getMessage() + "\n";
        } finally {
            // close database connection
            if (connMysql != null) {
                try {
                    closeConnection(connMysql);
                } catch (SQLException e) {
                    // output SQL exception messages
                    result = e.getMessage() + "\n";
                }
            }
        }
        try {
            // open database connection
            connMysql57 = connect(configMap.get("MYSQL57_CONNECTION"),
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
            for (int i = 0; i < countryList.size(); i++) {
                // set parameters for statement 2 from statement 1
                // add country parameter to statement
                try (PreparedStatement statement2 = connMysql57.prepareStatement(INSERT_HISTORY_SQL)) {
                    // set parameters for statement 2 from statement 1
                    // add country id parameter to statement
                    int tempInt = getCountryId(connMysql57, countryList.get(i));
                    statement2.setInt(1, tempInt);
                    // add date parameter to statement
                    statement2.setDate(2, dateList.get(i));
                    // add cases parameter to statement
                    statement2.setLong(3, casesList.get(i));
                    // add deaths parameter to statement
                    statement2.setLong(4, deathsList.get(i));
                    // add active parameter to statement
                    statement2.setLong(5, activeList.get(i));
                    // add new cases parameter to statement
                    statement2.setLong(6, newCasesList.get(i));
                    // add new deaths parameter to statement
                    statement2.setLong(7, newDeathsList.get(i));
                    // run query
                    statement2.execute();
                    statement2.close();
                }
            }
        } catch (SQLException e) {
            // output SQL exception messages
            result = e.getMessage() + "\n";
        } finally {
            if (connMysql57 != null) {
                try {
                    closeConnection(connMysql57);
                } catch (SQLException e) {
                    // output SQL exception messages
                    result = e.getMessage() + "\n";
                }
            }
        }
        return result;
    }
    
    public String transferStatCountries() {
        // initialize variables
        String result = "Done";
        List<String> countryList = new ArrayList();
        List<String> countryCodeList = new ArrayList();
        List<Long> populationList = new ArrayList();
        Connection connMysql57 = null, connMysql = null;
        final String SELECT_STAT_COUNTRIES_SQL = 
                "SELECT country, country_code, population "
                + "FROM stat_countries;";
        final String INSERT_STAT_COUNTRIES_SQL = 
            "INSERT INTO stat_countries (country, country_id, country_code, population)"
                + " VALUES (?, ?, ?, ?);";
        try {
            // open connection to database
            connMysql = 
                    connect(configMap.get("MYSQL_CONNECTION"), 
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
         
            // run query and get results
            try ( // statement to use
                    PreparedStatement statement = connMysql.prepareStatement(SELECT_STAT_COUNTRIES_SQL); 
                    // run query and get results
                    ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    // create lists for later insertion
                    countryList.add(resultSet.getString("country"));
                    countryCodeList.add(resultSet.getString("country_code"));
                    populationList.add(resultSet.getLong("population"));
                } 
                resultSet.close();
                statement.close();
            }
        } catch (SQLException e) {
            // output SQL exception messages
            result = e.getMessage() + "\n";
        } finally {
            // close database connection
            if (connMysql != null) {
                try {
                    closeConnection(connMysql);
                } catch (SQLException e) {
                    // output SQL exception messages
                    result = e.getMessage() + "\n";
                }
            }
        }
        try {
            // open database connection
            connMysql57 = connect(configMap.get("MYSQL57_CONNECTION"),
                    configMap.get("DB_USER_NAME"), 
                    configMap.get("DB_USER_PASSWORD"));
            for (int i = 0; i < countryList.size(); i++) {
                // set parameters for statement 2 from statement 1
                // add country parameter to statement
                try (PreparedStatement statement2 = connMysql57.prepareStatement(INSERT_STAT_COUNTRIES_SQL)) {
                    // set parameters for statement 2 from statement 1
                    // add country parameter to statement
                    statement2.setString(1, countryList.get(i));
                    // add country id parameter to statement
                    int tempInt = getCountryId(connMysql57, countryList.get(i));
                    statement2.setInt(2, tempInt);
                    // add country code parameter to statement
                    statement2.setString(3, countryCodeList.get(i));
                    // add population parameter to statement
                    statement2.setLong(4, populationList.get(i));
                    // run query
                    statement2.execute();
                    statement2.close();
                }
            }
        } catch (SQLException e) {
            // output SQL exception messages
            result = e.getMessage() + "\n";
        } finally {
            if (connMysql57 != null) {
                try {
                    closeConnection(connMysql57);
                } catch (SQLException e) {
                    // output SQL exception messages
                    result = e.getMessage() + "\n";
                }
            }
        }
        return result;
    }
}
