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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to deal with database
 * @author Gary Larson gary@thalic.mobi
 */
public class DatabaseUtilities {
    // declare constants
    private final long DAY = 82800000L;
    private final int UNKNOWN_COUNTRY_ID = 261;
    private final SimpleDateFormat simpleDateFormat = 
            new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat simpleDateFormatAlt = 
            new SimpleDateFormat("yyyy_MM_dd");
    private final Results mResults;
    private final HashMap<String, String> configMap = new HashMap<>();
    
    /**
     * Default constructor
     * @param results of all activities
     */
    public DatabaseUtilities(Results results) {
        getConfigParams();
        mResults = results;
    }
    
    /**
     * Method to get the configuration items
     */
    private void getConfigParams() {
        //Declare variables
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
    
    /**
     * Method to establish connection to database
     * @return usable connection
     */
    public Connection connect() {
        // Declare variables
        Connection conn = null;
        int i = 0;
        do {
            try {
                // Attempt to connect to database
                conn = DriverManager.getConnection(configMap.get("DB_CONNECT"), 
                        configMap.get("DB_USER_NAME"), 
                        configMap.get("DB_USER_PASSWORD"));
            } catch (SQLException e) {
                mResults.addResults("connect " + e.getMessage());
            }
            i++;
        } while(conn == null && i < 10);
        // test connection
        if (conn == null) {
            mResults.addResults("No connection in 10 attempts!");
            System.exit(1);
        }
        // return usable connection or null
        return conn;
    }
    
    /**
     * Method to close connection if one exists
     * @param conn to close
     */
    public void closeConnection(Connection conn) {
        // check if there is a connection
        if (conn != null) {
            try {
            // close connection
            conn.close();
            } catch (SQLException e) {
                mResults.addResults("closeConnection " + e.getMessage());
            }
        }
    }
    
    /**
     * Method to get state population
     * @param conn to database
     * @param state to get population for
     * @return population
     */
    public long selectStatePopulation(Connection conn, String state) {
        // Declare constant
        final String SELECT_POPULATION = 
            "SELECT population FROM states WHERE id = ?;";
        // Declare variables
        long population = 0;
        // test connection
        if (conn == null) {
            mResults.addResults("selectStatePopulation no connection");
            connect();
        }
        if (conn != null) {
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
            } catch (SQLException e) {
                mResults.addResults("selectStatePopulation" + state + " " +
                        e.getMessage());
            }
        }
        // return population
        return population;
    }
    
    /**
     * Method to get state id from database
     * @param conn to database
     * @param state to get id for
     * @return state id
     */
    public int selectStateId(Connection conn, String state) {
        final String SELECT_STATE_ID_SQL = 
                "SELECT id FROM states WHERE state = ?;";
        int stateId = 0;
        // test connection
        if (conn == null) {
            mResults.addResults("selectStateId no connection");
            connect();
        }
        if (conn != null) {
            try ( // statement to use
                  PreparedStatement statement =
                          conn.prepareStatement(SELECT_STATE_ID_SQL)) {
                // add state parameter to statement
                statement.setString(1, state);

                try ( // run query and get results
                      ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        stateId = resultSet.getInt("id");
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("selectStateId" + state + " " + e.getMessage());
            }
        }
       return stateId; 
    }
    
    /**
     * Method to INSERT World totals into database
     * @param conn to the database
     * @param list to insert
     */
    public void insertWorldTotal(Connection conn, List<String> list) {
        // Declare constant
        final String INSERT_COUNTRY_TOTALS_SQL = "INSERT INTO country_totals" + 
            " (country_id, cases, deaths, active, date)" + 
                " VALUES (?, ?, ?, ?, ?);";
        // Declare variable
        int mCountryId = 0;
        java.sql.Date mDate = null;
        try {
            mDate = new java.sql.Date(simpleDateFormatAlt
                    .parse(list.get(5)).getTime());
        } catch (ParseException e) {
            mResults.addResults("insertWorldTotal parse exception " +
                    e.getMessage());
        }
        // test connection
        if (conn == null) {
            mResults.addResults("insertWorldTotal no connection");
            connect();
        }
        if (conn != null) {
            try (
                    // statenent to use
                    PreparedStatement statement =
                            conn.prepareStatement(INSERT_COUNTRY_TOTALS_SQL)) {
                // add country id parameter to statement
                int countryId = selectCountryId(conn, list.get(0));
                if (countryId > 0 && countryId != UNKNOWN_COUNTRY_ID) {
                    mCountryId = countryId;
                    statement.setInt(1, countryId);
                    // add total cases parameter to statement
                    String tempString = list.get(1);
                    long tempLong = 0L;
                    if (!(tempString.equals("") || tempString.equals("N/A"))) {
                        tempString = tempString.replace(",", "");
                        tempLong = Long.parseLong(tempString);
                    }
                    statement.setLong(2, tempLong);
                    // add total deaths parameter to statement
                    tempString = list.get(2);
                    tempLong = 0L;
                    if (!(tempString.equals("") || tempString.equals("N/A"))) {
                        tempString = tempString.replace(",", "");
                        tempLong = Long.parseLong(tempString);
                    }
                    statement.setLong(3, tempLong);
                    // add active cases parameter to statement
                    tempString = list.get(3);
                    tempLong = 0L;
                    if (!(tempString.equals("") || tempString.equals("N/A"))) {
                        tempString = tempString.replace(",", "");
                        tempLong = Long.parseLong(tempString);
                    }
                    statement.setLong(4, tempLong);
                    // add total date parameter to statement
                    statement.setDate(5, mDate);
                    // run query
                    statement.execute();
                } else {
                    mResults.addResults("insertWorldTotal" + list.get(0) +
                            " does not exist in database");
                    insertUnknownCountryTotal(conn, mDate, list);
                }
                // close statement
                statement.close();
                if (mCountryId > 0 && mDate != null) {
                    insertCountryDaily(conn, mCountryId, mDate, list);
                }
            } catch (SQLException e) {
                mResults.addResults("insertWorldTotal " + list.get(0) + " " +
                        e.getMessage());
            }
        }
    }
    
    /**
     * Method to insert country daily
     * @param conn to the database
     * @param date used
     */
    private void insertCountryDaily(Connection conn, int countryId, 
            java.sql.Date date, List<String> list) {
        // Declare constant
        final String INSERT_COUNTRY_DAILY_SQL = "INSERT INTO country_dailies" + 
            " (country_id, cases, deaths, recovered, date)" + 
                " VALUES (?, ?, ?, ?, ?);";
        // test connection
        if (conn == null) {
            mResults.addResults("insertCountryDaily no connection");
            connect();
        }
        if (countryId == 0 || date == null) {
            mResults.addResults("insertCountryDaily No countryId or date");
            return;
        }
        java.sql.Date date1 = new java.sql.Date(date.getTime() - DAY);
        long mCases, mCases1 = 0L, mDeaths, mDeaths1 = 0L,
                mActive, mActive1 = 0L, mNewCases, mNewDeaths, mRecovered;
        List<Long> totalYesterday = getCountryTotal(conn, countryId, date1);
        if (totalYesterday.size() > 0) {
            mCases1 = totalYesterday.get(0);
            mDeaths1 = totalYesterday.get(1);
            mActive1 = totalYesterday.get(2);
        }
        String tempString = list.get(1);
        long tempLong = 0L;
        if (!(tempString.equals("") || tempString.equals("N/A"))) {
            tempString = tempString.replace(",", "");
            tempLong = Long.parseLong(tempString);
        }
        mCases = tempLong;
        // add total deaths parameter to statement
        tempString = list.get(2);
        tempLong = 0L;
        if (!(tempString.equals("") || tempString.equals("N/A"))) {
            tempString = tempString.replace(",", "");
            tempLong = Long.parseLong(tempString);
        }
        mDeaths = tempLong;
        // add active cases parameter to statement
        tempString = list.get(3);
        tempLong = 0L;
        if (!(tempString.equals("") || tempString.equals("N/A"))) {
            tempString = tempString.replace(",", "");
            tempLong = Long.parseLong(tempString);
        }
        mActive = tempLong;
        // test current day results
        if (mCases == 0 && mDeaths == 0 && mActive == 0) {
            mResults.addResults("insertCountryDaily " + 
                    String.format("No record found for country id %d on %s", 
                    countryId, simpleDateFormat.format(date)));
            return;
        }
        // calculate and insert daily data
        try { 
            if (conn != null) {
                // add state id parameter
                try ( // statement to use
                        PreparedStatement statement = 
                            conn.prepareStatement(INSERT_COUNTRY_DAILY_SQL)) {
                    // add state id parameter
                    statement.setInt(1, countryId);
                    // add new cases parameter
                    mNewCases = mCases - mCases1;
                    if (mNewCases < 0) {
                        mNewCases = 0;
                    }   statement.setLong(2, mNewCases);
                    // add new deaths parameter
                    mNewDeaths = mDeaths - mDeaths1;
                    if (mNewDeaths < 0) {
                        mNewDeaths = 0;
                    }   statement.setLong(3, mNewDeaths);
                    // add recovered parameter
                    mRecovered = mCases - mDeaths - mActive -
                            (mCases1 - mDeaths1 - mActive1);
                    if (mRecovered < 0) {
                        mRecovered = 0;
                    }   statement.setLong(4, mRecovered);
                    // add date parameter
                    java.sql.Date tempDate = new java.sql.Date(date.getTime());
                    statement.setDate(5, tempDate);
                    // execute statement and get result
                    statement.execute();
                    // close statement
                }
            }
        } catch (SQLException e) {
            mResults.addResults("insertCountryDaily " + list.get(0) + " " + 
                    e.getMessage());
        }
    }
    
    /**
     * Method to get country total
     * @param conn o database
     * @param countryId of country
     * @param date of data
     * @return country total
     */
    private List<Long> getCountryTotal(Connection conn, int countryId, 
            java.sql.Date date) {
        // Declare constant
        final String GET_COUNTRY_TOTAL_SQL = "SELECT cases, deaths, "
                + "active FROM country_totals WHERE date = ? "
                + "AND country_id = ?";
        // test connection
        if (conn == null) {
            mResults.addResults("getCountryTotal no connection");
            connect();
        }
        // declare variable
        List<Long> total = new ArrayList<>();
        if (conn != null) {
            try (
                // statement to use
                PreparedStatement statement = 
                        conn.prepareStatement(GET_COUNTRY_TOTAL_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                // add state id parameter
                statement.setInt(2, countryId);
                try (
                    // run query with results
                    ResultSet resultSet = statement.executeQuery()) {
                    // check for result(s)
                    while (resultSet.next()) {
                        // get cases from results
                        total.add(resultSet.getLong(1));
                        // get deaths from results
                        total.add(resultSet.getLong(2));
                        // get active
                        total.add(resultSet.getLong(3));
                    } 
                    // close results
                    resultSet.close();
                    // close statement
                    statement.close();
                }
            } catch(SQLException e) {
                mResults.addResults("getCountryTotal " + countryId + " " + date + 
                        " " + e.getMessage());
            }
        }
        return total;
    }
    
    /**
     * Method to get the country id
     * @param conn of database
     * @param country to get id for
     * @return country id
     */
    public int selectCountryId(Connection conn, String country)  {
        // Declare constant
        final String SELECT_COUNTRY_ID = 
            "SELECT country_id FROM country_labels WHERE label = ?;";
        // test connection
        if (conn == null) {
            mResults.addResults("selectCountryId no connection");
            connect();
        }
        // Declare variables
        int countryId = 0;
        if (conn != null) {
            try (     
                // statement to use
                PreparedStatement statement = 
                        conn.prepareStatement(SELECT_COUNTRY_ID)) {
                // add country parameter
                statement.setString(1, country);
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
            } catch (SQLException e) {
                mResults.addResults("selectCountryId " + country + " " + 
                        e.getMessage());
            }
        }
        // check id
        if(countryId < 1) {
            // insert into country labels with unknown country id
            insertCountry(conn, country);
            countryId = UNKNOWN_COUNTRY_ID;
        }
        // return country id
        return countryId;
    }
    
    /**
     * Method to add a country to the database
     * @param conn to database
     * @param country to add
     */
    public void insertCountry(Connection conn, String country) {
        // declare constant
        final String INSERT_COUNTRY_LABEL_SQL = 
            "INSERT INTO country_label (label, country_id) VALUES (?, ?);";
        // test connection
        if (conn == null) {
            mResults.addResults("insertCountry no connection");
            connect();
        }
        if (conn != null) {
            try ( 
                // statement to use
                PreparedStatement statement = 
                        conn.prepareStatement(INSERT_COUNTRY_LABEL_SQL)) {
                // add state parameter
                statement.setString(1, country);
                //add population parameter
                statement.setInt(2, UNKNOWN_COUNTRY_ID);
                // run statement
                statement.execute();
                // close statement
            } catch (SQLException e) {
                mResults.addResults("insertCountry " + country + " " + 
                        e.getMessage());
            }
        }
    }

    /**
     * Method to INSERT US totals into the database
     * @param conn to the database
     * @param list to insert
     */
    public void insertUSTotal(Connection conn, List<String> list) {
        // Declare constant
        final String INSERT_US_TOTAL_SQL = "INSERT INTO state_totals" + 
            " (state_id, cases, deaths, active, date)" + 
                " VALUES (?, ?, ?, ?, ?);";
        // test connection
        if (conn == null) {
            mResults.addResults("insertUSTotal no connection");
            connect();
        }
        // Declare variable
        int mStateId;
        java.sql.Date mDate = null;
        try {
            mDate = new java.sql.Date(simpleDateFormatAlt
                    .parse(list.get(5)).getTime());
        } catch (ParseException e) {
            mResults.addResults("insertUSTotal Parse Exception " +
                    list.get(0) + " " + e.getMessage());
        }
        if (conn != null) {
            // insert us total
            try ( 
                // statenent to use
                PreparedStatement statement = 
                        conn.prepareStatement(INSERT_US_TOTAL_SQL)) {
                // add state id parameter to statement
                int stateId = selectStateId(conn, list.get(0));
                mStateId = stateId;
                if (stateId > 0) {
                    statement.setInt(1, stateId);
                    // add total cases parameter to statement
                    String tempString = list.get(1);
                    long tempLong = 0L;
                    if (!(tempString.equals("") || tempString.equals("N/A"))) {
                        tempString = tempString.replace(",", "");
                        tempLong = Long.parseLong(tempString);
                    }
                    statement.setLong(2, tempLong);
                    // add total deaths parameter to statement
                    tempString = list.get(2);
                    tempLong = 0L;
                    if (!(tempString.equals("") || tempString.equals("N/A"))) {
                        tempString = tempString.replace(",", "");
                        tempLong = Long.parseLong(tempString);
                    }
                    statement.setLong(3, tempLong);
                    // add active cases parameter to statement
                    tempString = list.get(3);
                    tempLong = 0L;
                    if (!(tempString.equals("") || tempString.equals("N/A"))) {
                        tempString = tempString.replace(",", "");
                        tempLong = Long.parseLong(tempString);
                    }
                    statement.setLong(4, tempLong);
                    // add total date parameter to statement
                    statement.setDate(5, mDate);
                    // run query
                    statement.execute();
                } else {
                    mResults.addResults(list.get(0) + 
                            " does not exist in database.\n");
                    insertUnknownStateTotal(conn, mDate, list);
                }
                // close statement
                statement.close();
                if (mStateId > 0 && mDate != null) {
                    insertStateDaily(conn, mStateId, mDate, list);
                }
            } catch (SQLException e) {
                mResults.addResults("insertUsTotal " + list.get(0) + " " + 
                        e.getMessage());
            }
        }
    }
    
    /**
     * Method to insert unknown state total
     * @param conn to the database
     * @param date of the data
     * @param list of data to use
     */
    public void insertUnknownStateTotal(Connection conn, java.sql.Date date,
            List<String> list) {
        // Declare constant
        final String INSERT_UNKNOWN_STATE_TOTALS_SQL = "INSERT INTO "
                + "unknown_state_totals (state, cases, deaths, active, "
                + "date) VALUES (?, ?, ?, ?, ?);";
        // test connection
        if (conn == null) {
            mResults.addResults("insertUnknownStateTotal no connection");
            connect();
        }
        if (conn != null) {
            try ( 
                // statenent to use
                PreparedStatement statement = 
                        conn.prepareStatement(INSERT_UNKNOWN_STATE_TOTALS_SQL)) {
                // add state parameter to statement
                statement.setString(1, list.get(0));
                // add total cases parameter to statement
                String tempString = list.get(1);
                long tempLong = 0L;
                if (!(tempString.equals("") || tempString.equals("N/A"))) {
                    tempString = tempString.replace(",", "");
                    tempLong = Long.parseLong(tempString);
                }
                statement.setLong(2, tempLong);
                // add total deaths parameter to statement
                tempString = list.get(2);
                tempLong = 0L;
                if (!(tempString.equals("") || tempString.equals("N/A"))) {
                    tempString = tempString.replace(",", "");
                    tempLong = Long.parseLong(tempString);
                }
                statement.setLong(3, tempLong);
                // add active cases parameter to statement
                tempString = list.get(3);
                tempLong = 0L;
                if (!(tempString.equals("") || tempString.equals("N/A"))) {
                    tempString = tempString.replace(",", "");
                    tempLong = Long.parseLong(tempString);
                }
                statement.setLong(4, tempLong);
                // add total date parameter to statement
                statement.setDate(5, date);
                // run query
                statement.execute();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }
    
    /**
     * Method to insert state daily
     * @param conn to the database
     * @param stateId of the state
     * @param date used
     */
    private void insertStateDaily(Connection conn, int stateId, 
            java.sql.Date date, List<String> list) {
        // Declare constant
        final String INSERT_US_STATE_DAILY_SQL = "INSERT INTO state_dailies" + 
            " (state_id, cases, deaths, recovered, date)" + 
                " VALUES (?, ?, ?, ?, ?);";
        if (stateId == 0 || date == null) {
            mResults.addResults("insertStateDaily " + list.get(0) + 
                    " no state id or date");
            return;
        }
        // test connection
        if (conn == null) {
            mResults.addResults("insertStateDaily no connection");
            connect();
        }
        java.sql.Date date1 = new java.sql.Date(date.getTime() - DAY);
        long mCases, mCases1 = 0, mDeaths, mDeaths1 = 0, mActive, mActive1 = 0, 
                mNewCases, mNewDeaths, mRecovered;
        // get stat totals one day ago
        List<Long> totalYesterday = getStateTotal(conn, stateId, date1);
        // if not exist set all numbers to 0 
        if (totalYesterday.size() > 0) {
            // get data from yesterday
            mCases1 = totalYesterday.get(0);
            mDeaths1 = totalYesterday.get(1);
            mActive1 = totalYesterday.get(2);
        }
        String tempString = list.get(1);
        long tempLong = 0L;
        if (!(tempString.equals("") || tempString.equals("N/A"))) {
            tempString = tempString.replace(",", "");
            tempLong = Long.parseLong(tempString);
        }
        tempString = list.get(2);
        mCases = tempLong;
        if (!(tempString.equals("") || tempString.equals("N/A"))) {
            tempString = tempString.replace(",", "");
            tempLong = Long.parseLong(tempString);
        }
        mDeaths = tempLong;
        tempString = list.get(3);
        if (!(tempString.equals("") || tempString.equals("N/A"))) {
            tempString = tempString.replace(",", "");
            tempLong = Long.parseLong(tempString);
        }
        mActive = tempLong;
        // test current day results
        if (mCases == 0 && mDeaths == 0 && mActive == 0) {
            mResults.addResults("insertStateDaily " + 
                    String.format("No record found for state id %d on %s", 
                    stateId, simpleDateFormat.format(date)));
        }
        if (conn != null) {
            // calculate and insert dialy data
            try ( 
                // statement to use
                PreparedStatement statement = 
                        conn.prepareStatement(INSERT_US_STATE_DAILY_SQL)) { 
                // add state id parameter
                statement.setInt(1, stateId);
                // add new cases parameter
                mNewCases = mCases - mCases1;
                if (mNewCases < 0) {
                    mNewCases = 0;
                }
                statement.setLong(2, mNewCases);
                // add new deaths parameter
                mNewDeaths = mDeaths - mDeaths1;
                if (mNewDeaths < 0) {
                    mNewDeaths = 0;
                }
                statement.setLong(3, mNewDeaths);
                // add recovered parameter
                mRecovered = mCases - mDeaths - mActive - 
                        (mCases1 - mDeaths1 - mActive1);
                if (mRecovered < 0) {
                    mRecovered = 0;
                }
                statement.setLong(4, mRecovered);
                // add date parameter
                statement.setDate(5, date);
                // execute statement and get result
                statement.execute();
            } catch (SQLException e) {
                mResults.addResults("insertStateDaily " + list.get(0) + " " + 
                        e.getMessage());
            }
        }
    }
    
    /**
     * Method to get state total from the database
     * @param conn to the database
     * @param stateId to get state total for
     * @param date of the data
     * @return state total list
     */
    private List<Long> getStateTotal(Connection conn, int stateId, 
            java.sql.Date date) {
        // Declare constant
        final String GET_US_STATE_TOTAL_SQL = "SELECT cases, deaths, "
                + "`active` FROM state_totals WHERE `date` = ? "
                + "AND state_id = ?";
        // test connection
        if (conn == null) {
            mResults.addResults("getStateTotal no connection");
            connect();
        }
        // declare variable
        List<Long> total = new ArrayList<>();
        if (conn != null) {
            try (
                // statement to use
                PreparedStatement statement = 
                        conn.prepareStatement(GET_US_STATE_TOTAL_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                // add state id parameter
                statement.setInt(2, stateId);
                try (
                    // run query with results
                    ResultSet resultSet = statement.executeQuery()) {
                    // check for result(s)
                    while (resultSet.next()) {
                        // get cases from results
                        total.add(resultSet.getLong(1));
                        // get deaths from results
                        total.add(resultSet.getLong(2));
                        // get active
                        total.add(resultSet.getLong(3));
                    } 
                    // close results
                    resultSet.close();
                    // close statement
                    statement.close();
                }
            } catch(SQLException e) {
                mResults.addResults("getStateTotal " + stateId + " " + date + " " +
                        e.getMessage());
            }
        }
        return total;
    }
    
    /**
     * Method to update the state populations
     * @param conn to database
     * @param state of the population to update
     * @param population to update
     */
    public void updateStatePopulation(Connection conn, String state, 
            long population) {
        // declare constant
        final String UPDATE_POPULATION_SQL = 
            "UPDATE states SET population = ? WHERE id = ?;";
        // test connection
        if (conn == null) {
            mResults.addResults("updateStatePopulation no connection");
            connect();
        }
        if (conn != null) {
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
            } catch (SQLException e) {
                mResults.addResults("updateStatePopulation " + state + " " + 
                        e.getMessage());
            }
        }
    }
    
    /**
     * Method to update world populations
     * @param conn to database
     * @param country of population to update
     * @param population to update
     */
    public void updateWorldPopulation(Connection conn, String country, 
            long population) {
        // declare constant
        final String UPDATE_POPULATION_SQL = 
            "UPDATE country_codes SET population = ? WHERE id = ?;";
        // test connection
        if (conn == null) {
            mResults.addResults("updateWorldPopulation no connection");
            connect();
        }
        if (conn != null) {
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
            } catch (SQLException e) {
                mResults.addResults("updateCountryPopulation " + country + " " + 
                        e.getMessage());
            }
        }
    }
    
    /**
     * Method to get world population
     * @param conn to database
     * @param country to get population for
     * @return population
     */
    public long selectWorldPopulation(Connection conn, String country) {
        // Declare constant
        final String SELECT_POPULATION = 
            "SELECT population FROM country_codes WHERE id = ?;";
        // test connection
        if (conn == null) {
            mResults.addResults("selectWorldPopulation no connection");
            connect();
        }
        // Declare variables
        long population = 0;
        if (conn != null) {
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
            } catch (SQLException e) {
                mResults.addResults("selectWorldPopulation " + country + " " + 
                        e.getMessage());
            }
        }
        // return population
        return population;
    }
    
    /**
     * Method to insert population into database
     * @param conn to the database
     * @param state to enter
     * @param population to enter
     */
    public void insertStatePopulation(Connection conn, String state, 
            long population) {
        // declare constant
        final String INSERT_POPULATION_SQL = 
            "INSERT INTO states (state, population) VALUES (?, ?);";
        if (selectStateId(conn, state) > 0) {
            updateStatePopulation(conn, state, population);
            return;
        }
        // test connection
        if (conn == null) {
            mResults.addResults("insertStatePopulation no connection");
            connect();
        }
        if (conn != null) {
            try ( 
                // statement to use
                PreparedStatement statement = 
                        conn.prepareStatement(INSERT_POPULATION_SQL)) {
                // add state parameter
                statement.setString(1, state);
                //add population parameter
                statement.setLong(2, population);
                // run statement
                statement.execute();
            }
            catch(SQLException e) {
                mResults.addResults("insertStatePopulation " + state + " " + 
                        e.getMessage());
            }
        }
    }
    
    /**
     * Method to insert world population
     * @param conn to database
     * @param country to insert population for
     * @param population to insert
     */
    public void insertWorldPopulation(Connection conn, String country, 
            long population) {
        updateWorldPopulation(conn, country, population);
    }
    
    /**
     * Method to insert unknown country total
     * @param conn to the database
     * @param date of the data
     * @param list of data to use
     */
    public void insertUnknownCountryTotal(Connection conn, java.sql.Date date, 
            List<String> list) {
        // Declare constant
        final String INSERT_UNKNOWN_COUNTRY_TOTALS_SQL = "INSERT INTO "
                + "unknown_country_totals (country, cases, deaths, active, "
                + "date) VALUES (?, ?, ?, ?, ?);";
        // test connection
        if (conn == null) {
            mResults.addResults("insertUnknownCountryTotal no connection");
            connect();
        }
        if (conn != null) {
            try ( 
                // statenent to use
                PreparedStatement statement = 
                        conn.prepareStatement(INSERT_UNKNOWN_COUNTRY_TOTALS_SQL)) {
                // add country parameter to statement 
                statement.setString(1, list.get(0));
                // add cases parameter to statement
                String tempString = list.get(1);
                long tempLong = 0L;
                if (!(tempString.equals("") || tempString.equals("N/A"))) {
                    tempString = tempString.replace(",", "");
                    tempLong = Long.parseLong(tempString);
                }
                statement.setLong(2, tempLong);
                // add deaths parameter to statement
                tempString = list.get(2);
                tempLong = 0L;
                if (!(tempString.equals("") || tempString.equals("N/A"))) {
                    tempString = tempString.replace(",", "");
                    tempLong = Long.parseLong(tempString);
                }
                statement.setLong(3, tempLong);
                // add active cases parameter to statement
                tempString = list.get(3);
                tempLong = 0L;
                if (!(tempString.equals("") || tempString.equals("N/A"))) {
                    tempString = tempString.replace(",", "");
                    tempLong = Long.parseLong(tempString);
                }
                statement.setLong(4, tempLong);
                // add date parameter to statement
                statement.setDate(5, date);
                // run query
                statement.execute();
            } catch(SQLException e) {
                mResults.addResults("insertUnknownCountry " + list.get(0) + " " + 
                        e.getMessage());
            }
        }
    }
    
    /**
     * Method to get the stat country id
     * @param conn connection to the database
     * @param country to get the country id for
     * @return country id
     */
    public int selectStatCountryId(Connection conn, String country) {
        // declare constants
        final String SELECT_STAT_COUNTRIES_SQL = "SELECT country_id FROM "
                + "stat_countries WHERE country = ?;";
        // test connection
        if (conn == null) {
            mResults.addResults("selectStatCountryId no connection");
            connect();
        }
        // declare variable
        int countryId = 0;
        if (conn != null) {
            try (
                // statement to use to get stat country id
                PreparedStatement statement1 = 
                         conn.prepareStatement(SELECT_STAT_COUNTRIES_SQL)) {
                // add country parameter
                statement1.setString(1, country);
                try (
                    // run query and get results
                    ResultSet resultSet = statement1.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        // get country id from results
                        countryId = resultSet.getInt(1);
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("selectStatCountryId " + country + " " + 
                        e.getMessage());
            }
        }
        return countryId;
    }
    
    /**
     * Method to get the maximum date for the country through country id
     * @param conn of the database
     * @param countryId of the country
     * @return the maximum date
     */
    public java.sql.Date getStatCountryMaxDate(Connection conn, int countryId) {
        // Declare constant
        final String SELECT_COUNTRY_STAT_MAX_DATE_SQL = "SELECT MAX(`date`)"
                + " FROM stat_totals WHERE country_id = ?;";
        // test connection
        if (conn == null) {
            mResults.addResults("getStatCountryMaxDate no connection");
            connect();
        }
        // declare variable
        java.sql.Date maxDate = null;
        if (conn != null) {
            try (
                // statement to use to get stat country id
                PreparedStatement statement =
                         conn.prepareStatement(SELECT_COUNTRY_STAT_MAX_DATE_SQL)) {
                // add stat id parameter
                statement.setInt(1, countryId);
                try (
                    // run query and get results
                    ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        maxDate = resultSet.getDate(1);
                    }
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
        return maxDate;
    }
    
    /**
     * Method to insert the stat total in the database
     * @param conn connection to the data base
     * @param countryId of the country
     * @param mDate of the data
     * @param list of data
     */
    public void insertStatTotal (Connection conn, int countryId, 
            java.sql.Date mDate, List<String> list) {
        // Declare constant
        final String INSERT_STAT_TOTALS_SQL = 
            "INSERT INTO stat_totals (country_id, `date`, cases, deaths, "
                + "`active`, recovered) VALUES (?, ?, ?, ?, ?, ?);";
        // test connection
        if (conn == null) {
            mResults.addResults("insertStatTotal no connection");
            connect();
        }
        // declare variables
        long recovered = 0, active, cases, deaths;
        if (conn != null) {
            try {
                mDate = new java.sql.Date(simpleDateFormat.parse(list.get(0)).getTime());
            } catch (ParseException e) {
                mResults.addResults("inertStatTotal Parse Exception" + list.get(2) 
                        + " " + e.getMessage());
            }
            try (
                // statement to use to get stat country id
                PreparedStatement statement =
                         conn.prepareStatement(INSERT_STAT_TOTALS_SQL)) {
                // add country id parameter
                statement.setInt(1, countryId);
                // add date parameter
                statement.setDate(2, mDate);
                // add cases parameter
                cases = 0;
                if (list.get(3) != null && !list.get(3).isEmpty()) {
                    cases = Long.parseLong(list.get(3));
                }
                statement.setLong(3, cases);
                // add deaths parameter
                deaths = 0;
                if (list.get(4) != null && !list.get(4).isEmpty()) {
                    deaths = Long.parseLong(list.get(4));
                }
                statement.setLong(4, deaths);
                // add recovered parameter 
                if (list.get(5) != null && !list.get(5).isEmpty()) {
                    recovered = Long.parseLong(list.get(5));
                }
                statement.setLong(6, recovered);
                // add active parameter
                active = cases - deaths - recovered;
                if (active < 0) {
                    active = 0;
                }
                statement.setLong(5, active);
                // run statement
                statement.execute();
            }
            catch(SQLException e) {
                mResults.addResults("insertStatTotal " + list.get(2) + " " + 
                        e.getMessage());
                return;
            }
            if (countryId > 0 && active > 0) {
                updateCountryTotal(conn, countryId, active, cases, deaths, mDate);
            }
        }
    }
    
    /**
     * Method to update the active country data from stat data
     * @param conn connection to the database
     * @param countryId of the country
     * @param active data
     * @param cases data
     * @param deaths data
     * @param date of the data
     */
    private void updateCountryTotal(Connection conn, int countryId, long active,
            long cases, long deaths, java.sql.Date date) {
        // Declare constants 
        final String UPDATE_COUNTRY_TOTAL_ACTIVE_SQL = "UPDATE country_totals "
                + "SET active = ?, cases = ?, deaths = ? "
                + "WHERE country_id = ? AND `date` = ?;";
        // test connection
        if (conn == null) {
            mResults.addResults("updateCountryTotal no connection");
            connect();
        }
        if (checkCountryUpdate(conn, countryId) && conn != null) {
            try (
                // statement to use to update country total active
                PreparedStatement statement = 
                         conn.prepareStatement(UPDATE_COUNTRY_TOTAL_ACTIVE_SQL)) {
                // add active parameter
                statement.setLong(1, active);
                // add cases parameter
                statement.setLong(2, cases);
                // add deaths parameter
                statement.setLong(3, deaths);
                // add country id parameter
                statement.setInt(4, countryId);
                // add date parameter
                statement.setDate(5, date);
                // execute statement
                statement.execute();
            } catch (SQLException e) {
                mResults.addResults("updateCountryTotal " + countryId + " " + 
                        e.getMessage());
            }
        }
    }
    
    /**
     * Method to check if country should be updated
     * @param conn connection to the database
     * @param countryId of the country
     * @return true if needs update false otherwise
     */
    private boolean checkCountryUpdate(Connection conn, int countryId) {
        // Declare constant 
        final String CHECK_COUNTRY_UPDATE_SQL = "SELECT `update`"
                + " FROM stat_countries WHERE country_id = ?;";
        // test connection
        if (conn == null) {
            mResults.addResults("checkCountryUpdate no connection");
            connect();
        }
        // declare variable
        boolean canUpdate = false;
        if (conn != null) {
            try (
                // statement to use to get stat country id
                PreparedStatement statement = 
                         conn.prepareStatement(CHECK_COUNTRY_UPDATE_SQL)) {
                // add stat id parameter
                statement.setInt(1, countryId);
                try (
                    // run query and get results
                    ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        canUpdate = resultSet.getBoolean(1);
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("checkCountryTotalActive " + countryId + " " + 
                        e.getMessage());
                return false;
            }
        }
        return canUpdate;
    }
    
    /**
     * Method to get world data from the database
     * @param conn to the database
     * @param date of the data
     * @return world data
     */
    public WorldData getWorldData(Connection conn, java.sql.Date date) {
        // Declare constant
        final String SELECT_WORLD_TOTAL_SQL = 
                "SELECT cases, deaths, active, population "
                + "FROM country_totals INNER JOIN country_codes "
                + "ON country_totals.country_id = country_codes.id "
                + "WHERE `date` = ? AND alpha_2 = 'W';";
        // test connection
        if (conn == null) {
            mResults.addResults("getWorldData no connection");
            connect();
        }
        // declare variable
        WorldData world = new WorldData();
        if (conn != null) {
            try (
                // statement to use to get stat country id
                PreparedStatement statement = 
                       conn.prepareStatement(SELECT_WORLD_TOTAL_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                    // run query and get results
                    ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        world.setCases(resultSet.getLong("cases"));
                        world.setDeaths(resultSet.getLong("deaths"));
                        world.setActive(resultSet.getLong("active"));
                        world.setPopulation(resultSet.getLong("population"));
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getWorldData " + date.toString() + " " + 
                        e.getMessage());
                return null;
            }
        }
        return world;
    }
    
    /**
     * Method to get case data from the database
     * @param conn to the database
     * @param date of the data
     * @return case data
     */
    public Map<String, Long> getCasesData(Connection conn, java.sql.Date date) {
        // Declare constant
        final String SELECT_CASES_COUNTRY_TOTALS_SQL =
                "SELECT display, cases "
                        + "FROM country_totals INNER JOIN country_codes "
                        + "ON country_totals.country_id = country_codes.id "
                        + "WHERE `date` = ? "
                        + "AND country_codes.alpha_2 NOT IN ('R', 'S')"
                        + "ORDER BY cases;";
        // test connection
        if (conn == null) {
            mResults.addResults("getCaseData no connection");
            connect();
        }
        // declare variable
        Map<String, Long> cases = new HashMap<>();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_CASES_COUNTRY_TOTALS_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        cases.put(resultSet.getString("display"),
                                resultSet.getLong("cases"));
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getCasesData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return cases;
    }
    
    /**
     * Method to get new case data from the database
     * @param conn to the database
     * @param date of the data
     * @return case data
     */
    public Map<String, Long> getNewCasesData(Connection conn, java.sql.Date date) {
        // Declare constant
        final String SELECT_CASES_COUNTRY_DAILIES_SQL =
                "SELECT display, cases "
                        + "FROM country_dailies INNER JOIN country_codes "
                        + "ON country_dailies.country_id = country_codes.id "
                        + "WHERE `date` = ? "
                        + "AND country_codes.alpha_2 NOT IN ('R', 'S')"
                        + "ORDER BY cases;";
        // test connection
        if (conn == null) {
            mResults.addResults("getNewCaseData no connection");
            connect();
        }
        // declare variable
        Map<String, Long> cases = new HashMap<>();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_CASES_COUNTRY_DAILIES_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        cases.put(resultSet.getString("display"),
                                resultSet.getLong("cases"));
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getNewCasesData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return cases;
    }
    
    /**
     * Method to get death data from the database
     * @param conn to the database
     * @param date of the data
     * @return death data
     */
    public Map<String, Long> getDeathsData(Connection conn,
                                           java.sql.Date date) {
        // Declare constant
        final String SELECT_DEATHS_COUNTRY_TOTALS_SQL =
                "SELECT display, deaths "
                        + "FROM country_totals INNER JOIN country_codes "
                        + "ON country_totals.country_id = country_codes.id "
                        + "WHERE `date` = ? "
                        + "AND country_codes.alpha_2 NOT IN ('R', 'S')"
                        + "ORDER BY deaths;";
        // test connection
        if (conn == null) {
            mResults.addResults("getDeathsData no connection");
            connect();
        }
        // declare variable
        Map<String, Long> deaths = new HashMap<>();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_DEATHS_COUNTRY_TOTALS_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        deaths.put(resultSet.getString("display"),
                                resultSet.getLong("deaths"));
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getDeathsData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return deaths;
    }
    
    /**
     * Method to get death data from the database
     * @param conn to the database
     * @param date of the data
     * @return death data
     */
    public Map<String, Long> getNewDeathsData(Connection conn,
                                           java.sql.Date date) {
        // Declare constant
        final String SELECT_DEATHS_COUNTRY_DAILIES_SQL =
                "SELECT display, deaths "
                        + "FROM country_dailies INNER JOIN country_codes "
                        + "ON country_dailies.country_id = country_codes.id "
                        + "WHERE `date` = ? "
                        + "AND country_codes.alpha_2 NOT IN ('R', 'S')"
                        + "ORDER BY deaths;";
        // test connection
        if (conn == null) {
            mResults.addResults("getNewDeathsData no connection");
            connect();
        }
        // declare variable
        Map<String, Long> deaths = new HashMap<>();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_DEATHS_COUNTRY_DAILIES_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        deaths.put(resultSet.getString("display"),
                                resultSet.getLong("deaths"));
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getNewDeathsData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return deaths;
    }
    
    /**
     * Method to get active data from the database
     * @param conn to the database
     * @param date of the data
     * @return active data
     */
    public Map<String, Long> getActiveData(Connection conn,
                                           java.sql.Date date) {
        // Declare constant
        final String SELECT_ACTIVE_COUNTRY_TOTALS_SQL =
                "SELECT display, active "
                        + "FROM country_totals INNER JOIN country_codes "
                        + "ON country_totals.country_id = country_codes.id "
                        + "WHERE `date` = ? "
                        + "AND country_codes.alpha_2 NOT IN ('R', 'S')"
                        + "ORDER BY active;";
        // test connection
        if (conn == null) {
            mResults.addResults("getActiveData no connection");
            connect();
        }
        // declare variable
        Map<String, Long> active = new HashMap<>();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_ACTIVE_COUNTRY_TOTALS_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        active.put(resultSet.getString("display"),
                                resultSet.getLong("active"));
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getActiveData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return active;
    }
    
    /**
     * Method to get active100k data from the database
     * @param conn to the database
     * @param date of the data
     * @return active100k data
     */
    public List<StringDouble> getActive100kData(Connection conn,
                java.sql.Date date) {
        // Declare constant
        final String SELECT_ACTIVE100K_COUNTRY_TOTALS_SQL =
                "SELECT display, FORMAT((active / population) * 100000, 5, false) AS active100k "
                        + "FROM country_totals INNER JOIN country_codes "
                        + "ON country_totals.country_id = country_codes.id "
                        + "WHERE `date` = ? "
                        + "AND country_codes.alpha_2 NOT IN ('R', 'S') "
                        + "ORDER BY (active / population) * 100000;";
        // test connection
        if (conn == null) {
            mResults.addResults("getActive100kData no connection");
            connect();
        }
        // declare variable
        List<StringDouble> active100k = new ArrayList<>();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_ACTIVE100K_COUNTRY_TOTALS_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        StringDouble countryDouble = new StringDouble(
                                resultSet.getString("display"),
                                cleanDouble(resultSet.getString("active100k")));
                        active100k.add(countryDouble);
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getActive100kData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return active100k;
    }
    
    /**
     * Method to remove commas from string
     * @param string to clean
     * @return number in double format
     */
    private double cleanDouble (String string) {
        if (string == null || string.isBlank()) {
            return 0.0;
        }
        string = string.replace(",", "");
        double tempDouble = Double.parseDouble(string);
        return tempDouble;
    }
  
    /**
     * Method to get mortality data from the database
     * @param conn to the database
     * @param date of the data
     * @return mortality data
     */
    public Map<String, Double> getMortalityData(Connection conn,
                                                java.sql.Date date) {
        // Declare constant
        final String SELECT_MORTALITY_COUNTRY_TOTALS_SQL =
                "SELECT display, FORMAT((deaths / (deaths + "
                        + "(cases - `active` - deaths))) * 100, 5, false) AS mortality "
                        + "FROM country_totals INNER JOIN country_codes "
                        + "ON country_totals.country_id = country_codes.id "
                        + "WHERE `date` = ? "
                        + "AND country_codes.alpha_2 NOT IN ('R', 'S')"
                        + "ORDER BY (deaths / (deaths + (cases - `active` - deaths))) * "
                        + "100;";
        // test connection
        if (conn == null) {
            mResults.addResults("getMortalityData no connection");
            connect();
        }
        // declare variable
        Map<String, Double> mortality = new HashMap<>();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_MORTALITY_COUNTRY_TOTALS_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        mortality.put(resultSet.getString("display"),
                                cleanDouble(resultSet.getString("mortality")));
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getMortalityData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return mortality;
    }
    
    /**
     * Method to get population data from the database
     * @param conn to the database
     * @param date of the data
     * @return population data
     */
    public List<StringLong> getPopulationData(Connection conn,
                java.sql.Date date) {
        // Declare constant
        final String SELECT_POPULATION_COUNTRY_TOTALS_SQL =
                "SELECT display, population "
                        + "FROM country_totals INNER JOIN country_codes "
                        + "ON country_totals.country_id = country_codes.id "
                        + "WHERE `date` = ? "
                        + "AND country_codes.alpha_2 NOT IN ('R', 'S')"
                        + "ORDER BY population DESC;";
        // test connection
        if (conn == null) {
            mResults.addResults("getPopulationData no connection");
            connect();
        }
        // declare variable
        List<StringLong> population = new ArrayList<>();
        if (conn != null) {
            try (
                // statement to use to get stat country id
                PreparedStatement statement =
                    conn.prepareStatement(SELECT_POPULATION_COUNTRY_TOTALS_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        StringLong countryLong = new StringLong(
                                resultSet.getString("display"),
                                resultSet.getLong("population"));
                        population.add(countryLong);
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getPopulationData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return population;
    }
    
    /**
     * Method to get population data from the database
     * @param conn to the database
     * @param date of the data
     * @return population data
     */
    public List<StringLong> getStatePopulationData(Connection conn,
                java.sql.Date date) {
        // Declare constant
        final String SELECT_POPULATION_STATE_TOTALS_SQL =
                "SELECT state, population "
                        + "FROM state_totals INNER JOIN states "
                        + "ON state_totals.state_id = states.id "
                        + "WHERE `date` = ? "
                        + "ORDER BY population DESC;";
        // test connection
        if (conn == null) {
            mResults.addResults("getStatePopulationData no connection");
            connect();
        }
        // declare variable
        List<StringLong> population = new ArrayList<>();
        if (conn != null) {
            try (
                // statement to use to get stat country id
                PreparedStatement statement =
                    conn.prepareStatement(SELECT_POPULATION_STATE_TOTALS_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        StringLong countryLong = new StringLong(
                                resultSet.getString("state"),
                                resultSet.getLong("population"));
                        population.add(countryLong);
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getStatePopulationData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return population;
    }
    
    /**
     * Method to insert calculation data to the database
     * @param conn connection to the database
     * @param calc calculation data
     */
    public void insertCalculation(Connection conn, Calculations calc) {
        final String INSERT_CALCULATIONS_SQL = "INSERT INTO "
                + "country_json (country, `date`, population, population_world_rank, "
                + "pc_of_world_population, mortality_rate, pc_of_world_deaths, "
                + "pc_of_world_active_cases, pc_of_world_recovered, "
                + "pc_of_world_total_cases, total_cases, new_cases, total_deaths, "
                + "new_deaths, total_active_cases, total_deaths100k, "
                + "total_deaths100k_rank, total_deaths100k_score, "
                + "total_deaths100k_grade, total_active100k, total_active100k_rank, "
                + "total_active100k_score, total_active100k_grade, "
                + "total_cases100k, total_cases100k_rank, total_cases100k_score, "
                + "total_cases100k_grade, new_cases100k_15days, "
                + "new_cases100k_15days_rank, new_cases100k_15days_score, "
                + "new_cases100k_15days_grade, new_deaths100k_15days, "
                + "new_deaths100k_15days_rank, new_deaths100k_15days_score,"
                + "new_deaths100k_15days_grade, new_cases100k_30days, "
                + "new_cases100k_30days_rank, new_cases100k_30days_score, "
                + "new_cases100k_30days_grade, new_deaths100k_30days, "
                + "new_deaths100k_30days_rank, new_deaths100k_30days_score, "
                + "new_deaths100k_30days_grade, overall_rank, overall_score, "
                + "overall_grade) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, ?, ?, ?, ?, ?);";
        // test connection
        if (conn == null) {
            mResults.addResults("insertCalculations no connection");
            connect();
        }
        if (conn != null) {
            try (
                    // statement to use
                    PreparedStatement statement =
                            conn.prepareStatement(INSERT_CALCULATIONS_SQL)) {
                // add country parameter
                statement.setString(1, calc.getCountry());
                // add date parameter
                statement.setDate(2, calc.getDate());
                // add population parameter
                statement.setLong(3, calc.getPopulation());
                // add population rank parameter
                statement.setInt(4, calc.getPopulationRank());
                // add percent of population parameter
                statement.setDouble(5, calc.getPercentPopulation());
                // add percent of mortality parameter
                statement.setDouble(6, calc.getMortalityRate());
                // add percent of deaths parameter
                statement.setDouble(7, calc.getPercentDeaths());
                // add percent of active cases parameter
                statement.setDouble(8, calc.getPercentActive());
                // add percent of recovered cases parameter
                statement.setDouble(9, calc.getPercentRecovered());
                // add percent of total cases parameter
                statement.setDouble(10, calc.getPercentCases());
                // add total cases parameter
                statement.setLong(11, calc.getTotalCases());
                // add new cases parameter
                statement.setLong(12, calc.getNewCases());
                // add total deaths parameter
                statement.setLong(13, calc.getTotalDeaths());
                // add new deaths parameter
                statement.setLong(14, calc.getNewDeaths());
                // add total active cases parameter
                statement.setLong(15, calc.getTotalActiveCases());
                // add deaths100k parameter
                statement.setDouble(16, calc.getDeaths100k());
                // add deaths100k rank parameter
                statement.setInt(17, calc.getDeaths100kRank());
                // add deaths100k score parameter
                statement.setInt(18, calc.getDeaths100kScore());
                // add deaths100k grade parameter
                statement.setString(19, calc.getDeaths100kGrade());
                // add active100k parameter
                statement.setDouble(20, calc.getActive100k());
                // add active100k rank parameter
                statement.setInt(21, calc.getActive100kRank());
                // add active100k score parameter
                statement.setInt(22, calc.getActive100kScore());
                // add active100k grade parameter
                statement.setString(23, calc.getActive100kGrade());
                // add cases100k parameter
                statement.setDouble(24, calc.getCases100k());
                // add cases100k rank parameter
                statement.setInt(25, calc.getCases100kRank());
                // add cases100k score parameter
                statement.setInt(26, calc.getCases100kScore());
                // add cases100k grade parameter
                statement.setString(27, calc.getCases100kGrade());
                // add deaths100k 15 days parameter
                statement.setDouble(28, calc.getDeaths100k15());
                // add deaths100k 15 days rank parameter
                statement.setInt(29, calc.getDeaths100k15Rank());
                // add deaths100k 15 days score parameter
                statement.setInt(30, calc.getDeaths100k15Score());
                // add deaths100k 15 days grade parameter
                statement.setString(31, calc.getDeaths100k15Grade());
                // add cases100k 15 days parameter
                statement.setDouble(32, calc.getCases100k15());
                // add cases100k 15 days rank parameter
                statement.setInt(33, calc.getCases100k15Rank());
                // add cases100k 15 days score parameter
                statement.setInt(34, calc.getCases100k15Score());
                // add cases100k 15 days grade parameter
                statement.setString(35, calc.getCases100k15Grade());
                // add deaths100k 30 days parameter
                statement.setDouble(36, calc.getDeaths100k30());
                // add deaths100k 30 days rank parameter
                statement.setInt(37, calc.getDeaths100k30Rank());
                // add deaths100k 30 days score parameter
                statement.setInt(38, calc.getDeaths100k30Score());
                // add deaths100k 30 days grade parameter
                statement.setString(39, calc.getDeaths100k30Grade());
                // add cases100k 30 days parameter
                statement.setDouble(40, calc.getCases100k30());
                // add cases100k 30 days rank parameter
                statement.setInt(41, calc.getCases100k30Rank());
                // add cases100k 30 days score parameter
                statement.setInt(42, calc.getCases100k30Score());
                // add cases100k 30 days grade parameter
                statement.setString(43, calc.getCases100k30Grade());
                // add overall rank parameter
                statement.setInt(44, calc.getRank());
                // add overall score parameter
                statement.setInt(45, calc.getScore());
                // add overall grade parameter
                statement.setString(46, calc.getGrade());
                // run statement
                statement.execute();
            } catch (SQLException e) {
                mResults.addResults("insertCalculations " + calc.getCountry() + 
                        " " + calc.getDate().toString() + " " + e.getMessage());
            }
        }
    }
    
    /**
     * Method to insert calculation data to the database
     * @param conn connection to the database
     * @param calc calculation data
     */
    public void insertStateCalculation(Connection conn, Calculations calc) {
        final String INSERT_CALCULATIONS_SQL = "INSERT INTO "
                + "state_json (state, `date`, population, population_usa_rank, "
                + "pc_of_usa_population, mortality_rate, pc_of_usa_deaths, "
                + "pc_of_usa_active_cases, pc_of_usa_recovered, "
                + "pc_of_usa_total_cases, total_cases, new_cases, total_deaths, "
                + "new_deaths, total_active_cases, total_deaths100k, "
                + "total_deaths100k_rank, total_deaths100k_score, "
                + "total_deaths100k_grade, total_active100k, total_active100k_rank, "
                + "total_active100k_score, total_active100k_grade, "
                + "total_cases100k, total_cases100k_rank, total_cases100k_score, "
                + "total_cases100k_grade, new_cases100k_15days, "
                + "new_cases100k_15days_rank, new_cases100k_15days_score, "
                + "new_cases100k_15days_grade, new_deaths100k_15days, "
                + "new_deaths100k_15days_rank, new_deaths100k_15days_score,"
                + "new_deaths100k_15days_grade, new_cases100k_30days, "
                + "new_cases100k_30days_rank, new_cases100k_30days_score, "
                + "new_cases100k_30days_grade, new_deaths100k_30days, "
                + "new_deaths100k_30days_rank, new_deaths100k_30days_score, "
                + "new_deaths100k_30days_grade, overall_rank, overall_score, "
                + "overall_grade) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, ?, ?, ?, ?, ?);";
        // test connection
        if (conn == null) {
            mResults.addResults("insertStateCalculations no connection");
            connect();
        }
        if (conn != null) {
            try (
                    // statement to use
                    PreparedStatement statement =
                            conn.prepareStatement(INSERT_CALCULATIONS_SQL)) {
                // add state parameter
                statement.setString(1, calc.getCountry());
                // add date parameter
                statement.setDate(2, calc.getDate());
                // add population parameter
                statement.setLong(3, calc.getPopulation());
                // add population rank parameter
                statement.setInt(4, calc.getPopulationRank());
                // add percent of population parameter
                statement.setDouble(5, calc.getPercentPopulation());
                // add percent of mortality parameter
                statement.setDouble(6, calc.getMortalityRate());
                // add percent of deaths parameter
                statement.setDouble(7, calc.getPercentDeaths());
                // add percent of active cases parameter
                statement.setDouble(8, calc.getPercentActive());
                // add percent of recovered cases parameter
                statement.setDouble(9, calc.getPercentRecovered());
                // add percent of total cases parameter
                statement.setDouble(10, calc.getPercentCases());
                // add total cases parameter
                statement.setLong(11, calc.getTotalCases());
                // add new cases parameter
                statement.setLong(12, calc.getNewCases());
                // add total deaths parameter
                statement.setLong(13, calc.getTotalDeaths());
                // add new deaths parameter
                statement.setLong(14, calc.getNewDeaths());
                // add total active cases parameter
                statement.setLong(15, calc.getTotalActiveCases());
                // add deaths100k parameter
                statement.setDouble(16, calc.getDeaths100k());
                // add deaths100k rank parameter
                statement.setInt(17, calc.getDeaths100kRank());
                // add deaths100k score parameter
                statement.setInt(18, calc.getDeaths100kScore());
                // add deaths100k grade parameter
                statement.setString(19, calc.getDeaths100kGrade());
                // add active100k parameter
                statement.setDouble(20, calc.getActive100k());
                // add active100k rank parameter
                statement.setInt(21, calc.getActive100kRank());
                // add active100k score parameter
                statement.setInt(22, calc.getActive100kScore());
                // add active100k grade parameter
                statement.setString(23, calc.getActive100kGrade());
                // add cases100k parameter
                statement.setDouble(24, calc.getCases100k());
                // add cases100k rank parameter
                statement.setInt(25, calc.getCases100kRank());
                // add cases100k score parameter
                statement.setInt(26, calc.getCases100kScore());
                // add cases100k grade parameter
                statement.setString(27, calc.getCases100kGrade());
                // add deaths100k 15 days parameter
                statement.setDouble(28, calc.getDeaths100k15());
                // add deaths100k 15 days rank parameter
                statement.setInt(29, calc.getDeaths100k15Rank());
                // add deaths100k 15 days score parameter
                statement.setInt(30, calc.getDeaths100k15Score());
                // add deaths100k 15 days grade parameter
                statement.setString(31, calc.getDeaths100k15Grade());
                // add cases100k 15 days parameter
                statement.setDouble(32, calc.getCases100k15());
                // add cases100k 15 days rank parameter
                statement.setInt(33, calc.getCases100k15Rank());
                // add cases100k 15 days score parameter
                statement.setInt(34, calc.getCases100k15Score());
                // add cases100k 15 days grade parameter
                statement.setString(35, calc.getCases100k15Grade());
                // add deaths100k 30 days parameter
                statement.setDouble(36, calc.getDeaths100k30());
                // add deaths100k 30 days rank parameter
                statement.setInt(37, calc.getDeaths100k30Rank());
                // add deaths100k 30 days score parameter
                statement.setInt(38, calc.getDeaths100k30Score());
                // add deaths100k 30 days grade parameter
                statement.setString(39, calc.getDeaths100k30Grade());
                // add cases100k 30 days parameter
                statement.setDouble(40, calc.getCases100k30());
                // add cases100k 30 days rank parameter
                statement.setInt(41, calc.getCases100k30Rank());
                // add cases100k 30 days score parameter
                statement.setInt(42, calc.getCases100k30Score());
                // add cases100k 30 days grade parameter
                statement.setString(43, calc.getCases100k30Grade());
                // add overall rank parameter
                statement.setInt(44, calc.getRank());
                // add overall score parameter
                statement.setInt(45, calc.getScore());
                // add overall grade parameter
                statement.setString(46, calc.getGrade());
                // run statement
                statement.execute();
            } catch (SQLException e) {
                mResults.addResults("insertStateCalculations " + calc.getCountry() + 
                        " " + calc.getDate().toString() + " " + e.getMessage());
            }
        }
    }
    
    /**
     * Method to get active data from the database
     * @param conn to the database
     * @param date of the data
     * @return active data
     */
    public Map<String, Long> getStateActiveData(Connection conn,
                                           java.sql.Date date) {
        // Declare constant
        final String SELECT_ACTIVE_STATE_TOTALS_SQL =
                "SELECT state, active "
                        + "FROM state_totals INNER JOIN states "
                        + "ON state_totals.state_id = states.id "
                        + "WHERE `date` = ? "
                        + "ORDER BY active;";
        // test connection
        if (conn == null) {
            mResults.addResults("getStateActiveData no connection");
            connect();
        }
        // declare variable
        Map<String, Long> active = new HashMap<>();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_ACTIVE_STATE_TOTALS_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        active.put(resultSet.getString("state"),
                                resultSet.getLong("active"));
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getStateActiveData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return active;
    }
    
    /**
     * Method to get active100k data from the database
     * @param conn to the database
     * @param date of the data
     * @return active100k data
     */
    public List<StringDouble> getStateActive100kData(Connection conn,
                java.sql.Date date) {
        // Declare constant
        final String SELECT_ACTIVE100K_STATE_TOTALS_SQL =
                "SELECT state, FORMAT((active / population) * 100000, 5, false) AS active100k "
                        + "FROM state_totals INNER JOIN states "
                        + "ON state_totals.state_id = states.id "
                        + "WHERE `date` = ? "
                        + "ORDER BY (active / population) * 100000;";
        // test connection
        if (conn == null) {
            mResults.addResults("getStateActive100kData no connection");
            connect();
        }
        // declare variable
        List<StringDouble> active100k = new ArrayList<>();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_ACTIVE100K_STATE_TOTALS_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        StringDouble countryDouble = new StringDouble(
                                resultSet.getString("state"),
                                cleanDouble(resultSet.getString("active100k")));
                        active100k.add(countryDouble);
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getStateActive100kData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return active100k;
    }
    
    /**
     * Method to get cases100k data from the database
     * @param conn to the database
     * @param date of the data
     * @return cases100k data
     */
    public List<StringDouble> getCases100kData(Connection conn, 
            java.sql.Date date) {
        // Declare constant
        final String SELECT_CASES100K_COUNTRY_TOTALS_SQL =
                "SELECT display, FORMAT((cases / population) * 100000, 5, false) AS cases100k "
                        + "FROM country_totals INNER JOIN country_codes "
                        + "ON country_totals.country_id = country_codes.id "
                        + "WHERE `date` = ? "
                        + "AND country_codes.alpha_2 NOT IN ('R', 'S') "
                        + "ORDER BY (cases / population) * 100000;";
        // test connection
        if (conn == null) {
            mResults.addResults("getCases100kData no connection");
            connect();
        }
        // declare variable
        List<StringDouble> cases100k = new ArrayList<>();
        if (conn != null) {
            try (
                // statement to use to get stat country id
                PreparedStatement statement =
                    conn.prepareStatement(SELECT_CASES100K_COUNTRY_TOTALS_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        StringDouble countryDouble = new StringDouble(
                                resultSet.getString("display"),
                                cleanDouble(resultSet.getString("cases100k")));
                        cases100k.add(countryDouble);
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getCases100kData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return cases100k;
    }
    
    /**
     * Method to get case data from the database
     * @param conn to the database
     * @param date of the data
     * @return case data
     */
    public Map<String, Long> getStateCasesData(Connection conn, java.sql.Date date) {
        // Declare constant
        final String SELECT_CASES_STATE_TOTALS_SQL =
                "SELECT state, cases "
                        + "FROM state_totals INNER JOIN states "
                        + "ON state_totals.state_id = states.id "
                        + "WHERE `date` = ? "
                        + "ORDER BY cases;";
        // test connection
        if (conn == null) {
            mResults.addResults("getStateCaseData no connection");
            connect();
        }
        // declare variable
        Map<String, Long> cases = new HashMap<>();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_CASES_STATE_TOTALS_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        cases.put(resultSet.getString("state"),
                                resultSet.getLong("cases"));
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getStateCasesData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return cases;
    }
    
    /**
     * Method to get new case data from the database
     * @param conn to the database
     * @param date of the data
     * @return case data
     */
    public Map<String, Long> getNewStateCasesData(Connection conn, java.sql.Date date) {
        // Declare constant
        final String SELECT_CASES_STATE_DAILIES_SQL =
                "SELECT state, cases "
                        + "FROM state_dailies INNER JOIN states "
                        + "ON state_dailies.state_id = states.id "
                        + "WHERE `date` = ? "
                        + "ORDER BY cases;";
        // test connection
        if (conn == null) {
            mResults.addResults("getNewStateCaseData no connection");
            connect();
        }
        // declare variable
        Map<String, Long> cases = new HashMap<>();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_CASES_STATE_DAILIES_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        cases.put(resultSet.getString("state"),
                                resultSet.getLong("cases"));
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getNewStateCasesData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return cases;
    }
    
    /**
     * Method to get cases100k data from the database
     * @param conn to the database
     * @param date of the data
     * @return cases100k data
     */
    public List<StringDouble> getStateCases100kData(Connection conn, 
            java.sql.Date date) {
        // Declare constant
        final String SELECT_CASES100K_STATE_TOTALS_SQL =
                "SELECT state, FORMAT((cases / population) * 100000, 5, false) AS cases100k "
                        + "FROM state_totals INNER JOIN states "
                        + "ON state_totals.state_id = states.id "
                        + "WHERE `date` = ? "
                        + "ORDER BY (cases / population) * 100000;";
        // test connection
        if (conn == null) {
            mResults.addResults("getStateCases100kData no connection");
            connect();
        }
        // declare variable
        List<StringDouble> cases100k = new ArrayList<>();
        if (conn != null) {
            try (
                // statement to use to get stat country id
                PreparedStatement statement =
                    conn.prepareStatement(SELECT_CASES100K_STATE_TOTALS_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        StringDouble countryDouble = new StringDouble(
                                resultSet.getString("state"),
                                cleanDouble(resultSet.getString("cases100k")));
                        cases100k.add(countryDouble);
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getStateCases100kData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return cases100k;
    }
    
    /**
     * Method to get new cases100k data from 16 days prior from the database
     * @param conn to the database
     * @param date of the data
     * @return cases100k data
     */
    public List<StringDouble> getCases100kData16(Connection conn, 
            java.sql.Date date) {
        // Declare constant
        final String SELECT_CASES100K16_COUNTRY_TOTALS_SQL =
                "SELECT display, FORMAT((cases / population) * 100000, 5, false) AS cases100k16 "
                        + "FROM country_totals INNER JOIN country_codes "
                        + "ON country_totals.country_id = country_codes.id "
                        + "WHERE `date` = ? "
                        + "AND country_codes.alpha_2 NOT IN ('R', 'S') "
                        + "ORDER BY (cases / population) * 100000;";
        // test connection
        if (conn == null) {
            mResults.addResults("getCases100kData16 no connection");
            connect();
        }
        // declare variable
        List<StringDouble> cases100k16 = new ArrayList<>();
        if (conn != null) {
            try (
                // statement to use to get stat country id
                PreparedStatement statement =
                    conn.prepareStatement(SELECT_CASES100K16_COUNTRY_TOTALS_SQL)) {
                // add date - 16 days parameter
                LocalDate pastDate = date.toLocalDate().minusDays(16);
                java.sql.Date date1 = java.sql.Date.valueOf(pastDate);
                statement.setDate(1, date1);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        StringDouble countryDouble = new StringDouble(
                                resultSet.getString("display"),
                                cleanDouble(resultSet.getString("cases100k16")));
                        cases100k16.add(countryDouble);
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getCases100kData16 " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return cases100k16;
    }
    
    /**
     * Method to get new cases100k data from 16 days prior from the database
     * @param conn to the database
     * @param date of the data
     * @return cases100k data
     */
    public List<StringDouble> getStateCases100kData16(Connection conn, 
            java.sql.Date date) {
        // Declare constant
        final String SELECT_CASES100K16_STATE_TOTALS_SQL =
                "SELECT state, FORMAT((cases / population) * 100000, 5, false) AS cases100k16 "
                        + "FROM state_totals INNER JOIN states "
                        + "ON state_totals.state_id = states.id "
                        + "WHERE `date` = ? "
                        + "ORDER BY (cases / population) * 100000;";
        // test connection
        if (conn == null) {
            mResults.addResults("getStateCases100kData16 no connection");
            connect();
        }
        // declare variable
        List<StringDouble> cases100k16 = new ArrayList<>();
        if (conn != null) {
            try (
                // statement to use to get stat country id
                PreparedStatement statement =
                    conn.prepareStatement(SELECT_CASES100K16_STATE_TOTALS_SQL)) {
                // add date - 16 days parameter
                LocalDate pastDate = date.toLocalDate().minusDays(16);
                java.sql.Date date1 = java.sql.Date.valueOf(pastDate);
                statement.setDate(1, date1);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        StringDouble countryDouble = new StringDouble(
                                resultSet.getString("state"),
                                cleanDouble(resultSet.getString("cases100k16")));
                        cases100k16.add(countryDouble);
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getStateCases100kData16 " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return cases100k16;
    }
    
    /**
     * Method to get new cases100k data from 31 days prior from the database
     * @param conn to the database
     * @param date of the data
     * @return cases100k data
     */
    public List<StringDouble> getCases100kData31(Connection conn, 
            java.sql.Date date) {
        // Declare constant
        final String SELECT_CASES100K31_COUNTRY_TOTALS_SQL =
                "SELECT display, FORMAT((cases / population) * 100000, 5, false) AS cases100k31 "
                        + "FROM country_totals INNER JOIN country_codes "
                        + "ON country_totals.country_id = country_codes.id "
                        + "WHERE `date` = ? "
                        + "AND country_codes.alpha_2 NOT IN ('R', 'S') "
                        + "ORDER BY (cases / population) * 100000;";
        // test connection
        if (conn == null) {
            mResults.addResults("getCases100kData31 no connection");
            connect();
        }
        // declare variable
        List<StringDouble> cases100k31 = new ArrayList<>();
        if (conn != null) {
            try (
                // statement to use to get stat country id
                PreparedStatement statement =
                    conn.prepareStatement(SELECT_CASES100K31_COUNTRY_TOTALS_SQL)) {
                // add date - 16 days parameter
                LocalDate pastDate = date.toLocalDate().minusDays(31);
                java.sql.Date date1 = java.sql.Date.valueOf(pastDate);
                statement.setDate(1, date1);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        StringDouble countryDouble = new StringDouble(
                                resultSet.getString("display"),
                                cleanDouble(resultSet.getString("cases100k31")));
                        cases100k31.add(countryDouble);
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getCases100kData31 " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return cases100k31;
    }
    
    /**
     * Method to get new cases100k data from 31 days prior from the database
     * @param conn to the database
     * @param date of the data
     * @return cases100k data
     */
    public List<StringDouble> getStateCases100kData31(Connection conn, 
            java.sql.Date date) {
        // Declare constant
        final String SELECT_CASES100K31_STATE_TOTALS_SQL =
                "SELECT state, FORMAT((cases / population) * 100000, 5, false) AS cases100k31 "
                        + "FROM state_totals INNER JOIN states "
                        + "ON state_totals.state_id = states.id "
                        + "WHERE `date` = ? "
                        + "ORDER BY (cases / population) * 100000;";
        // test connection
        if (conn == null) {
            mResults.addResults("getStateCases100kData31 no connection");
            connect();
        }
        // declare variable
        List<StringDouble> cases100k31 = new ArrayList<>();
        if (conn != null) {
            try (
                // statement to use to get stat country id
                PreparedStatement statement =
                    conn.prepareStatement(SELECT_CASES100K31_STATE_TOTALS_SQL)) {
                // add date - 16 days parameter
                LocalDate pastDate = date.toLocalDate().minusDays(31);
                java.sql.Date date1 = java.sql.Date.valueOf(pastDate);
                statement.setDate(1, date1);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        StringDouble countryDouble = new StringDouble(
                                resultSet.getString("state"),
                                cleanDouble(resultSet.getString("cases100k31")));
                        cases100k31.add(countryDouble);
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getStateCases100kData31 " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return cases100k31;
    }
    
    /**
     * Method to get death data from the database
     * @param conn to the database
     * @param date of the data
     * @return death data
     */
    public Map<String, Long> getStateDeathsData(Connection conn,
                                           java.sql.Date date) {
        // Declare constant
        final String SELECT_DEATHS_STATE_TOTALS_SQL =
                "SELECT state, deaths "
                        + "FROM state_totals INNER JOIN states "
                        + "ON state_totals.state_id = states.id "
                        + "WHERE `date` = ? "
                        + "ORDER BY deaths;";
        // test connection
        if (conn == null) {
            mResults.addResults("getStateDeathsData no connection");
            connect();
        }
        // declare variable
        Map<String, Long> deaths = new HashMap<>();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_DEATHS_STATE_TOTALS_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        deaths.put(resultSet.getString("state"),
                                resultSet.getLong("deaths"));
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getStateDeathsData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return deaths;
    }
    
    /**
     * Method to get death data from the database
     * @param conn to the database
     * @param date of the data
     * @return death data
     */
    public Map<String, Long> getNewStateDeathsData(Connection conn,
                                           java.sql.Date date) {
        // Declare constant
        final String SELECT_DEATHS_STATE_DAILIES_SQL =
                "SELECT state, deaths "
                        + "FROM state_dailies INNER JOIN states "
                        + "ON state_dailies.state_id = states.id "
                        + "WHERE `date` = ? "
                        + "ORDER BY deaths;";
        // test connection
        if (conn == null) {
            mResults.addResults("getNewStateDeathsData no connection");
            connect();
        }
        // declare variable
        Map<String, Long> deaths = new HashMap<>();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_DEATHS_STATE_DAILIES_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        deaths.put(resultSet.getString("state"),
                                resultSet.getLong("deaths"));
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getNewStateDeathsData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return deaths;
    }
    
    /**
     * Method to get deaths100k data from the database
     * @param conn to the database
     * @param date of the data
     * @return deaths100k data
     */
    public List<StringDouble> getDeaths100kData(Connection conn,
                java.sql.Date date) {
        // Declare constant
        final String SELECT_DEATHS100K_COUNTRY_TOTALS_SQL =
                "SELECT display, FORMAT((deaths / population) * 100000, 5, false) AS deaths100k "
                        + "FROM country_totals INNER JOIN country_codes "
                        + "ON country_totals.country_id = country_codes.id "
                        + "WHERE `date` = ? "
                        + "AND country_codes.alpha_2 NOT IN ('R', 'S') "
                        + "ORDER BY (deaths / population) * 100000;";
        // test connection
        if (conn == null) {
            mResults.addResults("getDeaths100kData no connection");
            connect();
        }
        // declare variable
        List<StringDouble> deaths100k = new ArrayList<>();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_DEATHS100K_COUNTRY_TOTALS_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        StringDouble countryDouble = new StringDouble(
                                resultSet.getString("display"),
                                cleanDouble(resultSet.getString("deaths100k")));
                        deaths100k.add(countryDouble);
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getDeaths10kData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return deaths100k;
    }
    
    /**
     * Method to get deaths100k data from the database
     * @param conn to the database
     * @param date of the data
     * @return deaths100k data
     */
    public List<StringDouble> getStateDeaths100kData(Connection conn,
                               java.sql.Date date) {
        // Declare constant
        final String SELECT_DEATHS100K_STATE_TOTALS_SQL =
                "SELECT state, FORMAT((deaths / population) * 100000, 5, false) AS deaths100k "
                        + "FROM state_totals INNER JOIN states "
                        + "ON state_totals.state_id = states.id "
                        + "WHERE `date` = ? "
                        + "ORDER BY (deaths / population) * 100000;";
        // test connection
        if (conn == null) {
            mResults.addResults("getStateDeaths100kData no connection");
            connect();
        }
        // declare variable
        List<StringDouble> deaths100k = new ArrayList<>();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_DEATHS100K_STATE_TOTALS_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        StringDouble countryDouble = new StringDouble(
                                resultSet.getString("state"),
                                cleanDouble(resultSet.getString("deaths100k")));
                        deaths100k.add(countryDouble);
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getStateDeaths10kData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return deaths100k;
    }
    
    /**
     * Method to get new deaths100k data from 16 days prior from the database
     * @param conn to the database
     * @param date of the data
     * @return deaths100k data
     */
    public List<StringDouble> getDeaths100kData16(Connection conn,
                    java.sql.Date date) {
        // Declare constant
        final String SELECT_DEATHS100K16_COUNTRY_TOTALS_SQL =
                "SELECT display, FORMAT((deaths / population) * 100000, 5, false) AS deaths100k16 "
                        + "FROM country_totals INNER JOIN country_codes "
                        + "ON country_totals.country_id = country_codes.id "
                        + "WHERE `date` = ? "
                        + "AND country_codes.alpha_2 NOT IN ('R', 'S') "
                        + "ORDER BY (deaths / population) * 100000;";
        // test connection
        if (conn == null) {
            mResults.addResults("getDeaths10kData16 no connection");
            connect();
        }
        // declare variable
        List<StringDouble> deaths100k16 = new ArrayList<>();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_DEATHS100K16_COUNTRY_TOTALS_SQL)) {
                // add date - 16 days parameter
                LocalDate pastDate = date.toLocalDate().minusDays(16);
                java.sql.Date date1 = java.sql.Date.valueOf(pastDate);
                statement.setDate(1, date1);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        StringDouble countryDouble = new StringDouble(
                                resultSet.getString("display"),
                                cleanDouble(resultSet.getString("deaths100k16")));
                        deaths100k16.add(countryDouble);
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getDeaths100kData16 " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return deaths100k16;
    }
    
    /**
     * Method to get new deaths100k data from 16 days prior from the database
     * @param conn to the database
     * @param date of the data
     * @return deaths100k data
     */
    public List<StringDouble> getStateDeaths100kData16(Connection conn,
                    java.sql.Date date) {
        // Declare constant
        final String SELECT_DEATHS100K16_STATE_TOTALS_SQL =
                "SELECT state, FORMAT((deaths / population) * 100000, 5, false) AS deaths100k16 "
                        + "FROM state_totals INNER JOIN states "
                        + "ON state_totals.state_id = states.id "
                        + "WHERE `date` = ? "
                        + "ORDER BY (deaths / population) * 100000;";
        // test connection
        if (conn == null) {
            mResults.addResults("getStateDeaths10kData16 no connection");
            connect();
        }
        // declare variable
        List<StringDouble> deaths100k16 = new ArrayList<>();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_DEATHS100K16_STATE_TOTALS_SQL)) {
                // add date - 16 days parameter
                LocalDate pastDate = date.toLocalDate().minusDays(16);
                java.sql.Date date1 = java.sql.Date.valueOf(pastDate);
                statement.setDate(1, date1);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        StringDouble countryDouble = new StringDouble(
                                resultSet.getString("state"),
                                cleanDouble(resultSet.getString("deaths100k16")));
                        deaths100k16.add(countryDouble);
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getStateDeaths100kData16 " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return deaths100k16;
    }
    
    /**
     * Method to get new deaths100k data from 31 days prior from the database
     * @param conn to the database
     * @param date of the data
     * @return deaths100k data
     */
    public List<StringDouble> getDeaths100kData31(Connection conn,
                    java.sql.Date date) {
        // Declare constant
        final String SELECT_DEATHS100K31_COUNTRY_TOTALS_SQL =
                "SELECT display, FORMAT((deaths / population) * 100000, 5, false) AS deaths100k31 "
                        + "FROM country_totals INNER JOIN country_codes "
                        + "ON country_totals.country_id = country_codes.id "
                        + "WHERE `date` = ? "
                        + "AND country_codes.alpha_2 NOT IN ('R', 'S') "
                        + "ORDER BY (deaths / population) * 100000;";
        // test connection
        if (conn == null) {
            mResults.addResults("getDeaths10kData31 no connection");
            connect();
        }
        // declare variable
        List<StringDouble> deaths100k31 = new ArrayList<>();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_DEATHS100K31_COUNTRY_TOTALS_SQL)) {
                // add date - 16 days parameter
                LocalDate pastDate = date.toLocalDate().minusDays(31);
                java.sql.Date date1 = java.sql.Date.valueOf(pastDate);
                statement.setDate(1, date1);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        StringDouble countryDouble = new StringDouble(
                                resultSet.getString("display"),
                                cleanDouble(resultSet.getString("deaths100k31")));
                        deaths100k31.add(countryDouble);
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getDeaths100kData31 " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return deaths100k31;
    }
    
    /**
     * Method to get new deaths100k data from 31 days prior from the database
     * @param conn to the database
     * @param date of the data
     * @return deaths100k data
     */
    public List<StringDouble> getStateDeaths100kData31(Connection conn,
                    java.sql.Date date) {
        // Declare constant
        final String SELECT_DEATHS100K31_STATE_TOTALS_SQL =
                "SELECT state, FORMAT((deaths / population) * 100000, 5, false) AS deaths100k31 "
                        + "FROM state_totals INNER JOIN states "
                        + "ON state_totals.state_id = states.id "
                        + "WHERE `date` = ? "
                        + "ORDER BY (deaths / population) * 100000;";
        // test connection
        if (conn == null) {
            mResults.addResults("getStateDeaths10kData31 no connection");
            connect();
        }
        // declare variable
        List<StringDouble> deaths100k31 = new ArrayList<>();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_DEATHS100K31_STATE_TOTALS_SQL)) {
                // add date - 16 days parameter
                LocalDate pastDate = date.toLocalDate().minusDays(31);
                java.sql.Date date1 = java.sql.Date.valueOf(pastDate);
                statement.setDate(1, date1);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        StringDouble countryDouble = new StringDouble(
                                resultSet.getString("state"),
                                cleanDouble(resultSet.getString("deaths100k31")));
                        deaths100k31.add(countryDouble);
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getStateDeaths100kData31 " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return deaths100k31;
    }
    
    /**
     * Method to get world data from the database
     * @param conn to the database
     * @param date of the data
     * @return world data
     */
    public USAData getUSAData(Connection conn, java.sql.Date date) {
        // Declare constant
        final String SELECT_USA_TOTAL_SQL =
                "SELECT cases, deaths, active, population "
                        + "FROM country_totals INNER JOIN country_codes "
                        + "ON country_totals.country_id = country_codes.id "
                        + "WHERE `date` = ? AND display = 'USA';";
        // test connection
        if (conn == null) {
            mResults.addResults("getUSAData no connection");
            connect();
        }
        // declare variable
        USAData usa = new USAData();
        if (conn != null) {
            try (
                    // statement to use to get stat country id
                    PreparedStatement statement =
                            conn.prepareStatement(SELECT_USA_TOTAL_SQL)) {
                // add date parameter
                statement.setDate(1, date);
                try (
                        // run query and get results
                        ResultSet resultSet = statement.executeQuery()) {
                    // check if results
                    while (resultSet.next()) {
                        usa.setCases(resultSet.getLong("cases"));
                        usa.setDeaths(resultSet.getLong("deaths"));
                        usa.setActive(resultSet.getLong("active"));
                        usa.setPopulation(resultSet.getLong("population"));
                    }
                }
            } catch (SQLException e) {
                mResults.addResults("getUSAData " + date.toString() + " " +
                        e.getMessage());
                return null;
            }
        }
        return usa;
    }
}
