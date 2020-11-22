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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    private final long DAY = 82800000l;
    private final int UNKNOWN_COUNTRY_ID = 261;
    private final SimpleDateFormat simpleDateFormat = 
            new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat simpleDateFormatAlt = 
            new SimpleDateFormat("yyyy_MM_dd");
    private final Results mResults;
    
    /**
     * Default constructor
     * @param results of all activities
     */
    public DatabaseUtilities(Results results) {
        mResults = results;
    }
    
    /**
     * Method to establish connection to database
     * @param connectionString database to connect to
     * @param userName of database user
     * @param userPassword for the user
     * @return usable connection
     */
    public Connection connect(String connectionString, String userName, 
            String userPassword) {
        // Declare variables
        Connection conn = null;
        try {
        // Attempt to connect to database
        conn = DriverManager.getConnection(connectionString, 
                userName, userPassword);
        } catch (SQLException e) {
            mResults.addResults("connect " + e.getMessage());
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
        } catch(SQLException e) {
            mResults.addResults("selectStatePopulation" + state + " " + 
                    e.getMessage());
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
        // run query and get results
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
            statement.close();
        } catch (SQLException e) {
            mResults.addResults("selectStateId" + state + " " + e.getMessage());
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
        // loop through list
        
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
                tempString = list.get(5);
                try {
                    mDate = new java.sql.Date(simpleDateFormatAlt
                            .parse(tempString).getTime());
                } catch (ParseException e) {
                    mResults.addResults("insertWorldTotal parse exception " + 
                            e.getMessage());
                }
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
            if (mCountryId >0 && mDate != null) {
                insertCountryDaily(conn, mCountryId, mDate, list);
            }
        } catch (SQLException e) {
            mResults.addResults("insertWorldTotal " + list.get(0) + " " + 
                    e.getMessage());
        }
    }
    
    /**
     * Method to insert country daily
     * @param conn to the database
     * @param stateId of the state
     * @param date used
     * @throws SQLException if an error
     */
    private void insertCountryDaily(Connection conn, int countryId, 
            java.sql.Date date, List<String> list) {
        // Declare constant
        final String INSERT_COUNTRY_DAILY_SQL = "INSERT INTO country_dailies" + 
            " (country_id, cases, deaths, recovered, date)" + 
                " VALUES (?, ?, ?, ?, ?);";
        if (countryId == 0 || date == null) {
            mResults.addResults("insertCountryDaily No countryId or date");
            return;
        }
        java.sql.Date date1 = new java.sql.Date(date.getTime() - DAY);
        long mCases, mCases1 = 0l, mDeaths, mDeaths1 = 0l, 
                mActive, mActive1 = 0l, mNewCases, mNewDeaths, mRecovered;
        List<Long> totalYesterday = getCountryTotal(conn, countryId, date1);
        if (totalYesterday != null && totalYesterday.size() > 0) {
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
        try ( 
            // statement to use
            PreparedStatement statement2 = 
                    conn.prepareStatement(INSERT_COUNTRY_DAILY_SQL)) { 
            // add state id parameter
            statement2.setInt(1, countryId);
            // add new cases parameter
            mNewCases = mCases - mCases1;
            if (mNewCases < 0) {
                mNewCases = 0;
            }
            statement2.setLong(2, mNewCases);
            // add new deaths parameter
            mNewDeaths = mDeaths - mDeaths1;
            if (mNewDeaths < 0) {
                mNewDeaths = 0;
            }
            statement2.setLong(3, mNewDeaths);
            // add recovered parameter
            mRecovered = mCases - mDeaths - mActive - 
                    (mCases1 - mDeaths1 - mActive1);
            if (mRecovered < 0) {
                mRecovered = 0;
            }
            statement2.setLong(4, mRecovered);
            // add date parameter
            java.sql.Date tempDate = new java.sql.Date(date.getTime());
            statement2.setDate(5, tempDate);
            // execute statement and get result
            statement2.execute();
            // close statement
            statement2.close();
        } catch (SQLException e) {
            mResults.addResults("insertCountryDaily " + list.get(0) + " " + 
                    e.getMessage());
        }
    }
    
    private List<Long> getCountryTotal(Connection conn, int countryId, 
            java.sql.Date date) {
        // Declare constant
        final String GET_COUNTRY_TOTAL_SQL = "SELECT cases, deaths, "
                + "active FROM country_totals WHERE date = ? "
                + "AND country_id = ?";
        // declare variable
        List<Long> total = new ArrayList<>();
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
        // Declare variables
        int countryId = 0;
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
        // add state parameter
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
            statement.close();
        } catch (SQLException e) {
            mResults.addResults("insertCountry " + country + " " + 
                    e.getMessage());
        }
    }
    
    /*
     * Methof to get the country code by country name
     * @param conn to the database
     * @param country name to finad
     * @return country code
     */
    public String selectCountryCode(Connection conn, String country) {
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
        } catch (SQLException e) {
            mResults.addResults("selectCountryCode " + country + " " + 
                    e.getMessage());
        }
        // return state id
        return countryCode;
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
        // Declare variable
        int mStateId;
        java.sql.Date mDate = null;
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
                try {
                    mDate = new java.sql.Date(simpleDateFormatAlt
                            .parse(list.get(5)).getTime());
                } catch (ParseException e) {
                    mResults.addResults("insertUSTotal Parse Excaption " + 
                            list.get(0) + " " + e.getMessage());
                }
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
            tempString = list.get(5);
            java.sql.Date parsed = null;
            try {
                parsed = new java.sql.Date(simpleDateFormat.parse(tempString).getTime());
            } catch (ParseException e) {
                System.out.println(e.getMessage());
            }
            statement.setDate(5, parsed);
            // run query
            statement.execute();
            statement.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    
    /**
     * Method to insert state daily
     * @param conn to the database
     * @param stateId of the state
     * @param date used
     * @throws SQLException if an error
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
        java.sql.Date date1 = new java.sql.Date(date.getTime() - DAY);
        long mCases, mCases1 = 0, mDeaths, mDeaths1 = 0, mActive, mActive1 = 0, 
                mNewCases, mNewDeaths, mRecovered;
        // get stat totals one day ago
        List<Long> totalYesterday = getStateTotal(conn, stateId, date1);
        // if not exist set all numbers to 0 
        if (totalYesterday != null && totalYesterday.size() > 0) {
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
            // close statement
            statement.close();
        } catch (SQLException e) {
            mResults.addResults("insertStateDaily " + list.get(0) + " " + 
                    e.getMessage());
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
        // declare variable
        List<Long> total = new ArrayList<>();
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
        } catch (SQLException e) {
            mResults.addResults("updateStatePopulation " + state + " " + 
                    e.getMessage());
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
        } catch (SQLException e) {
            mResults.addResults("updateCountryPopulation " + country + " " + 
                    e.getMessage());
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
        } catch (SQLException e) {
            mResults.addResults("selectWorldPopulation " + country + " " + 
                    e.getMessage());
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
        // add state parameter
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
            //clase statement
            statement.close();
        }
        catch(SQLException e) {
            mResults.addResults("insertStatePopulation " + state + " " + 
                    e.getMessage());
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
            java.sql.Date tempDate = null;
            try {
                tempDate = new java.sql.Date(simpleDateFormat.parse(list.get(5)).getTime());
            } catch (ParseException e) {
                mResults.addResults("insertUnknownCountry Parse Exception " + 
                        list.get(0) + " " + e.getMessage());
            }
            statement.setDate(5, tempDate);
            // run query
            statement.execute();
            statement.close();
        } catch(SQLException e) {
            mResults.addResults("insertUnknownCountry " + list.get(0) + " " + 
                    e.getMessage());
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
        // declare variable
        int countryId = 0;
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
                // close results
                resultSet.close();
                
            }
            // close statement
            statement1.close();
        } catch (SQLException e) {
            mResults.addResults("selectStatCountryId " + country + " " + 
                    e.getMessage());
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
        // declare variable
        java.sql.Date maxDate = null;
        try (
            // statement to use to get stat country id
            PreparedStatement statement2 = 
                     conn.prepareStatement(SELECT_COUNTRY_STAT_MAX_DATE_SQL)) {
            // add stat id parameter
            statement2.setInt(1, countryId);
            try (
                // run query and get results
                ResultSet resultSet = statement2.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    maxDate = resultSet.getDate(1);
                }
                // close results
                resultSet.close();
            }
            statement2.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
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
        // declare variables
        long recovered = 0, active;
        try {
            mDate = new java.sql.Date(simpleDateFormat.parse(list.get(0)).getTime());
        } catch (ParseException e) {
            mResults.addResults("inertStatTotal Parse Exception" + list.get(2) 
                    + " " + e.getMessage());
        }
        try (
            // statement to use to get stat country id
            PreparedStatement statement3 = 
                     conn.prepareStatement(INSERT_STAT_TOTALS_SQL)) {
            // add country id parameter
            statement3.setInt(1, countryId);
            // add date parameter
            statement3.setDate(2, mDate);
            // add cases parameter
            long cases = 0;
            if (list.get(3) != null && !list.get(3).isEmpty()) {
                cases = Long.parseLong(list.get(3));
            }
            statement3.setLong(3, cases);
            // add deaths parameter
            long deaths = 0;
            if (list.get(4) != null && !list.get(4).isEmpty()) {
                deaths = Long.parseLong(list.get(4));
            }
            statement3.setLong(4, deaths);
            // add recovered parameter 
            if (list.get(5) != null && !list.get(5).isEmpty()) {
                recovered = Long.parseLong(list.get(5));
            }
            statement3.setLong(6, recovered);
            // add active parameter
            active = cases - deaths - recovered;
            if (active < 0) {
                active = 0;
            }
            statement3.setLong(5, active);
            // run statement
            statement3.execute();
            // close statement
            statement3.close();
        }
        catch(SQLException e) {
            mResults.addResults("insertStatTotal " + list.get(2) + " " + 
                    e.getMessage());
            return;
        }
        if (countryId > 0 && active > 0) {
            updateCountryTotal(conn, countryId, active, mDate);
        }
    }
    
    /**
     * Method to update the active country data from stat data
     * @param conn connection to the database
     * @param countryId of the country
     * @param active data
     * @param date of the data
     */
    private void updateCountryTotal(Connection conn, int countryId, long active,
            java.sql.Date date) {
        // Declare constants 
        final String UPDATE_COUNTRY_TOTAL_ACTIVE_SQL = "UPDATE country_totals "
                + "SET active = ? WHERE country_id = ? AND `date` = ?;";
        
        if (checkCountryTotalActive(conn, countryId, date)) {
            try (
                // statement to use to update country total active
                PreparedStatement statement = 
                         conn.prepareStatement(UPDATE_COUNTRY_TOTAL_ACTIVE_SQL)) {
                // add active parameter
                statement.setLong(1, active);
                // add country id parameter
                statement.setInt(2, countryId);
                // add date parameter
                statement.setDate(3, date);
                // execute statement
                statement.execute();
                // close statement
                statement.close();
            } catch (SQLException e) {
                mResults.addResults("updateCountryTotal " + countryId + " " + 
                        e.getMessage());
            }
        }
    }
    
    /**
     * Method to check if country total active data exists
     * @param conn connection to the database
     * @param countryId of the country
     * @param date of the data
     * @return true if needs update false otherwise
     */
    private boolean checkCountryTotalActive(Connection conn, int countryId, 
            java.sql.Date date) {
        // Declare constant 
        final String CHECK_COUNTRY_TOTAL_ACTIVE_SQL = "SELECT EXISTS (SELECT id"
                + " FROM country_totals WHERE country_id = ? AND `date` = ?"
                + " AND active = 0);";
        // declare variable
        boolean doesExist = false;
        try (
            // statement to use to get stat country id
            PreparedStatement statement = 
                     conn.prepareStatement(CHECK_COUNTRY_TOTAL_ACTIVE_SQL)) {
            // add stat id parameter
            statement.setInt(1, countryId);
            // add date parameter
            statement.setDate(2, date);
            try (
                // run query and get results
                ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    doesExist = resultSet.getBoolean(1);
                }
                // close results
                resultSet.close();
            }
            statement.close();
        } catch (SQLException e) {
            mResults.addResults("checkCountryTotalActive " + countryId + " " + 
                    e.getMessage());
            return false;
        }
        return doesExist;
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
        // declare variable
        WorldData world = new WorldData();
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
                // close results
                resultSet.close();
            }
            statement.close();
        } catch (SQLException e) {
            mResults.addResults("getWorldData " + date.toString() + " " + 
                    e.getMessage());
            return null;
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
                + "AND country_codes.alpha_2 NOT IN ('R', 'S', 'W')"
                + "ORDER BY cases;";
        // declare variable
        Map<String, Long> cases = new HashMap<>();
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
                // close results
                resultSet.close();
            }
            statement.close();
        } catch (SQLException e) {
            mResults.addResults("getCasesData " + date.toString() + " " + 
                    e.getMessage());
            return null;
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
                + "AND country_codes.alpha_2 NOT IN ('R', 'S', 'W')"
                + "ORDER BY deaths;";
        // declare variable
        Map<String, Long> deaths = new HashMap<>();
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
                // close results
                resultSet.close();
            }
            statement.close();
        } catch (SQLException e) {
            mResults.addResults("getCasesData " + date.toString() + " " + 
                    e.getMessage());
            return null;
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
                + "AND country_codes.alpha_2 NOT IN ('R', 'S', 'W')"
                + "ORDER BY active;";
        // declare variable
        Map<String, Long> active = new HashMap<>();
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
                // close results
                resultSet.close();
            }
            statement.close();
        } catch (SQLException e) {
            mResults.addResults("getActiveData " + date.toString() + " " + 
                    e.getMessage());
            return null;
        }
        return active;
    }
    
    /**
     * Method to get recovered data from the database
     * @param conn to the database
     * @param date of the data
     * @return recovered data
     */
    public Map<String, Long> getRecoveredData(Connection conn, 
            java.sql.Date date) {
        // Declare constant
        final String SELECT_RECOVERED_COUNTRY_TOTALS_SQL = 
                "SELECT display, cases - active - deaths AS recovered "
                + "FROM country_totals INNER JOIN country_codes "
                + "ON country_totals.country_id = country_codes.id "
                + "WHERE `date` = ? "
                + "AND country_codes.alpha_2 NOT IN ('R', 'S', 'W')"
                + "ORDER BY recovered DESC;";
        // declare variable
        Map<String, Long> recovered = new HashMap<>();
        try (
            // statement to use to get stat country id
            PreparedStatement statement = 
                   conn.prepareStatement(SELECT_RECOVERED_COUNTRY_TOTALS_SQL)) {
            // add date parameter
            statement.setDate(1, date);
            try (
                // run query and get results
                ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    recovered.put(resultSet.getString("display"), 
                            resultSet.getLong("recovered"));
                }
                // close results
                resultSet.close();
            }
            statement.close();
        } catch (SQLException e) {
            mResults.addResults("getRecoveredData " + date.toString() + " " + 
                    e.getMessage());
            return null;
        }
        return recovered;
    }
   
    /**
     * Method to get cases10k data from the database
     * @param conn to the database
     * @param date of the data
     * @return cases10k data
     */
    public List<CountryDouble> getCases10kData(Connection conn, 
            java.sql.Date date) {
        // Declare constant
        final String SELECT_CASES10K_COUNTRY_TOTALS_SQL = 
                "SELECT display, (cases / population) * 10000 AS cases10k "
                + "FROM country_totals INNER JOIN country_codes "
                + "ON country_totals.country_id = country_codes.id "
                + "WHERE `date` = ? "
                + "AND country_codes.alpha_2 NOT IN ('R', 'S', 'W') "
                + "ORDER BY cases10k;";
        // declare variable
        List<CountryDouble> cases10k = new ArrayList<>();
        try (
            // statement to use to get stat country id
            PreparedStatement statement = 
                   conn.prepareStatement(SELECT_CASES10K_COUNTRY_TOTALS_SQL)) {
            // add date parameter
            statement.setDate(1, date);
            try (
                // run query and get results
                ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    CountryDouble countryDouble = new CountryDouble(
                            resultSet.getString("display"), 
                            resultSet.getDouble("cases10k"));
                    cases10k.add(countryDouble);
                }
                // close results
                resultSet.close();
            }
            statement.close();
        } catch (SQLException e) {
            mResults.addResults("getCases10kData " + date.toString() + " " + 
                    e.getMessage());
            return null;
        }
        return cases10k;
    }
    
    /**
     * Method to get deaths10k data from the database
     * @param conn to the database
     * @param date of the data
     * @return deaths10k data
     */
    public List<CountryDouble> getDeaths10kData(Connection conn, 
            java.sql.Date date) {
        // Declare constant
        final String SELECT_DEATHS10K_COUNTRY_TOTALS_SQL = 
                "SELECT display, (deaths / population) * 10000 AS deaths10k "
                + "FROM country_totals INNER JOIN country_codes "
                + "ON country_totals.country_id = country_codes.id "
                + "WHERE `date` = ? "
                + "AND country_codes.alpha_2 NOT IN ('R', 'S', 'W') "
                + "ORDER BY deaths10k;";
        // declare variable
        List<CountryDouble> deaths10k = new ArrayList<>();
        try (
            // statement to use to get stat country id
            PreparedStatement statement = 
                   conn.prepareStatement(SELECT_DEATHS10K_COUNTRY_TOTALS_SQL)) {
            // add date parameter
            statement.setDate(1, date);
            try (
                // run query and get results
                ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    CountryDouble countryDouble = new CountryDouble(
                            resultSet.getString("display"), 
                            resultSet.getDouble("deaths10k"));
                    deaths10k.add(countryDouble);
                }
                // close results
                resultSet.close();
            }
            statement.close();
        } catch (SQLException e) {
            mResults.addResults("getDeaths10kData " + date.toString() + " " + 
                    e.getMessage());
            return null;
        }
        return deaths10k;
    }
    
    /**
     * Method to get active10k data from the database
     * @param conn to the database
     * @param date of the data
     * @return active10k data
     */
    public List<CountryDouble> getActive10kData(Connection conn, 
            java.sql.Date date) {
        // Declare constant
        final String SELECT_ACTIVE10K_COUNTRY_TOTALS_SQL = 
                "SELECT display, (active / population) * 10000 AS active10k "
                + "FROM country_totals INNER JOIN country_codes "
                + "ON country_totals.country_id = country_codes.id "
                + "WHERE `date` = ? "
                + "AND country_codes.alpha_2 NOT IN ('R', 'S', 'W') "
                + "ORDER BY active10k;";
        // declare variable
        List<CountryDouble> active10k = new ArrayList<>();
        try (
            // statement to use to get stat country id
            PreparedStatement statement = 
                     conn.prepareStatement(SELECT_ACTIVE10K_COUNTRY_TOTALS_SQL)) {
            // add date parameter
            statement.setDate(1, date);
            try (
                // run query and get results
                ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    CountryDouble countryDouble = new CountryDouble(
                            resultSet.getString("display"), 
                            resultSet.getDouble("active10k"));
                    active10k.add(countryDouble);
                }
                // close results
                resultSet.close();
            }
            statement.close();
        } catch (SQLException e) {
            mResults.addResults("getActive10kData " + date.toString() + " " + 
                    e.getMessage());
            return null;
        }
        return active10k;
    }
    
    /**
     * Method to get recovered10k data from the database
     * @param conn to the database
     * @param date of the data
     * @return recovered10k data
     */
    public List<CountryDouble> getRecovered10kData(Connection conn, 
            java.sql.Date date) {
        // Declare constant
        final String SELECT_RECOVERED10K_COUNTRY_TOTALS_SQL = 
                "SELECT display, ((cases - deaths - active) / population) "
                + "* 10000 AS recovered10k "
                + "FROM country_totals INNER JOIN country_codes "
                + "ON country_totals.country_id = country_codes.id "
                + "WHERE `date` = ? "
                + "AND country_codes.alpha_2 NOT IN ('R', 'S', 'W') "
                + "ORDER BY recovered10k DESC;";
        // declare variable
        List<CountryDouble> recovered10k = new ArrayList<>();
        try (
            // statement to use to get stat country id
            PreparedStatement statement = 
                     conn.prepareStatement(SELECT_RECOVERED10K_COUNTRY_TOTALS_SQL)) {
            // add date parameter
            statement.setDate(1, date);
            try (
                // run query and get results
                ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    CountryDouble countryDouble = new CountryDouble(
                            resultSet.getString("display"), 
                            resultSet.getDouble("recovered10k"));
                    recovered10k.add(countryDouble);
                }
                // close results
                resultSet.close();
            }
            statement.close();
        } catch (SQLException e) {
            mResults.addResults("getRecovered10kData " + date.toString() + " " + 
                    e.getMessage());
            return null;
        }
        return recovered10k;
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
                "SELECT display, (deaths / cases) * 100 AS mortality "
                + "FROM country_totals INNER JOIN country_codes "
                + "ON country_totals.country_id = country_codes.id "
                + "WHERE `date` = ? "
                + "AND country_codes.alpha_2 NOT IN ('R', 'S', 'W')"
                + "ORDER BY mortality;";
        // declare variable
        Map<String, Double> mortality = new HashMap<>();
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
                            resultSet.getDouble("mortality"));
                }
                // close results
                resultSet.close();
            }
            statement.close();
        } catch (SQLException e) {
            mResults.addResults("getMortalityData " + date.toString() + " " + 
                    e.getMessage());
            return null;
        }
        return mortality;
    }
    
    /**
     * Method to get population data from the database
     * @param conn to the database
     * @param date of the data
     * @return population data
     */
    public List<CountryLong> getPopulationData(Connection conn, 
            java.sql.Date date) {
        // Declare constant
        final String SELECT_POPULATION_COUNTRY_TOTALS_SQL = 
                "SELECT display, population "
                + "FROM country_totals INNER JOIN country_codes "
                + "ON country_totals.country_id = country_codes.id "
                + "WHERE `date` = ? "
                + "AND country_codes.alpha_2 NOT IN ('R', 'S', 'W')"
                + "ORDER BY population DESC;";
        // declare variable
        List<CountryLong> population = new ArrayList<>();
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
                    CountryLong countryLong = new CountryLong(
                            resultSet.getString("display"), 
                            resultSet.getLong("population"));
                    population.add(countryLong);
                }
                // close results
                resultSet.close();
            }
            statement.close();
        } catch (SQLException e) {
            mResults.addResults("getPopulationData " + date.toString() + " " + 
                    e.getMessage());
            return null;
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
                + "country_calculations (country, `date`, pc_population, "
                + "pc_mortality, pc_deaths, pc_active_cases, pc_recovered, "
                + "pc_total_cases, population, population_rank, deaths10k, "
                + "deaths10k_rank, deaths10k_score, active10k, active10k_rank, "
                + "active10k_score, recovered10k, recovered10k_rank, "
                + "recovered10k_score, cases10k, cases10k_rank, cases10k_score, "
                + "rank, score) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, ?, ?, ?);";
        try ( 
            // statement to use
            PreparedStatement statement = 
                    conn.prepareStatement(INSERT_CALCULATIONS_SQL)) {
            // add country parameter
            statement.setString(1, calc.getCountry());
            // add date parameter
            statement.setDate(2, calc.getDate());
            // add percent of population parameter
            statement.setDouble(3, calc.getPercentPopulation());
            // add percent of mortality parameter
            statement.setDouble(4, calc.getPercentMortality());
            // add percent of deaths parameter
            statement.setDouble(5, calc.getPercentDeaths());
            // add percent of active cases parameter
            statement.setDouble(6, calc.getPercentActive());
            // add percent of recovered cases parameter
            statement.setDouble(7, calc.getPercentRecovered());
            // add percent of total cases parameter
            statement.setDouble(8, calc.getPercentCases());
            // add population parameter
            statement.setLong(9, calc.getPopulation());
            // add population rank parameter
            statement.setInt(10, calc.getPopulationRank());
            // add deaths10k parameter
            statement.setDouble(11, calc.getDeaths10k());
            // add deaths10k rank parameter
            statement.setInt(12, calc.getDeaths10kRank());
            // add deaths10k score parameter
            statement.setString(13, calc.getDeaths10kScore());
            // add active10k parameter
            statement.setDouble(14, calc.getActive10k());
            // add active10k rank parameter
            statement.setInt(15, calc.getActive10kRank());
            // add active10k score parameter
            statement.setString(16, calc.getActive10kScore());
            // add recovered10k parameter
            statement.setDouble(17, calc.getRecovered10k());
            // add recovered10k rank parameter
            statement.setInt(18, calc.getRecovered10kRank());
            // add recovered10k score parameter
            statement.setString(19, calc.getRecovered10kScore());
            // add cases10k parameter
            statement.setDouble(20, calc.getCases10k());
            // add cases10k rank parameter
            statement.setInt(21, calc.getCases10kRank());
            // add cases10k score parameter
            statement.setString(22, calc.getCases10kScore());
            // add overall rank parameter
            statement.setInt(23, calc.getRank());
            // add overall score parameter
            statement.setString(24, calc.getScore());
            // run statement
            statement.execute();
            // close statement
            statement.close();
        } catch (SQLException e) {
            mResults.addResults("insertCalculations " + e.getMessage());
        }
    }
}
