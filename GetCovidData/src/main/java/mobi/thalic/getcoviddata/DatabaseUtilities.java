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
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Class to deal with database
 * @author Gary Larson gary@thalic.mobi
 */
public class DatabaseUtilities {
    // declare constants
    private final long DAY = 82800000l;
    private final int UNKNOWN_COUNTRY_ID = 260;
    private final SimpleDateFormat simpleDateFormat = 
            new SimpleDateFormat("yyyy-MM-dd");
    
    /**
     * Method to establish connection to database
     * @param connectionString database to connect to
     * @param userName of database user
     * @param userPassword for the user
     * @return usable connection 
     * @throws SQLException on SQL error
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
     * @throws SQLException on SQL error
     */
    public void closeConnection(Connection conn) throws SQLException {
        // check if there is a connection
        if (conn != null) {
            // close connection
            conn.close();
        }
    }
    
    /**
     * Method to get state population
     * @param conn to database
     * @param state to get population for
     * @return population
     * @throws SQLException on SQL error
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
     * Method to get state id from database
     * @param conn to database
     * @param state to get id for
     * @return state id
     * @throws SQLException on SQL error
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
        final String INSERT_COUNTRY_TOTALS_SQL = "INSERT INTO country_totals" + 
            " (country_id, cases, deaths, active_cases, date)" + 
                " VALUES (?, ?, ?, ?, ?);";
        // Declare variable
        String result = "";
        int mCountryId = 0;
        Date mDate = null;
        // loop through list
        for (int i = 1; i < lists.size(); i++) {
            try ( 
                // statenent to use
                PreparedStatement statement = 
                        conn.prepareStatement(INSERT_COUNTRY_TOTALS_SQL)) {
                // add country id parameter to statement
                int countryId = selectCountryId(conn, lists.get(i).get(0));
                if (countryId > 0) {
                    mCountryId = countryId;
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
                    mDate = parsed;
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
                if (mCountryId >0 && mDate != null) {
                    insertCountryDaily(conn, mCountryId, mDate);
                }
            }
        }
        return result;
    }
    
    /**
     * Method to insert state daily
     * @param conn to the database
     * @param stateId of the state
     * @param date used
     * @throws SQLException if an error
     */
    private void insertCountryDaily(Connection conn, int countryId, Date date) 
            throws SQLException {
        // Declare constant
        final String GET_COUNTRY_TOTAL_SQL = "SELECT cases, deaths, "
                + "active_cases FROM country_totals WHERE date = ? "
                + "AND country_id = ?";
        final String INSERT_COUNTRY_DAILY_SQL = "INSERT INTO country_dailies" + 
            " (country_id, cases, deaths, recovered, date)" + 
                " VALUES (?, ?, ?, ?, ?);";
        if (countryId == 0 || date == null) {
            return;
        }
        Date date1 = new Date(date.getTime() - DAY);
        long mCases = 0l, mCases1 = 0l, mDeaths = 0l, mDeaths1 = 0l, 
                mActive = 0l, mActive1 = 0l, mNewCases, mNewDeaths, mRecovered;
        try ( 
            // statement to use to get previous days data
            PreparedStatement statement1 = 
                    conn.prepareStatement(GET_COUNTRY_TOTAL_SQL)) { 
            // add date parameter
            java.sql.Date tempDate = new java.sql.Date(date1.getTime());
            statement1.setDate(1, tempDate);
            statement1.setInt(2, countryId);
            // check if results
            try ( 
                // run query and get results
                ResultSet resultSet1 = statement1.executeQuery()) {
                // check if results
                while (resultSet1.next()) {
                    // get cases from results
                    mCases1 = resultSet1.getLong("cases");
                    // get deaths from results
                    mDeaths1 = resultSet1.getLong("deaths");
                    // get active cases from results
                    mActive1 = resultSet1.getLong("active_cases");
                } 
                // close results
                resultSet1.close();
                // close statement
                statement1.close();
            }
        }
        try ( 
            // statement to use to get current days data
            PreparedStatement statement = 
                    conn.prepareStatement(GET_COUNTRY_TOTAL_SQL)) { 
            // add date parameter
            java.sql.Date tempDate = new java.sql.Date(date.getTime());
            statement.setDate(1, tempDate);
            statement.setInt(2, countryId);
            // check if results
            try ( 
                // run query and get results
                ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    // get cases from results
                    mCases = resultSet.getLong("cases");
                    // get deaths from results
                    mDeaths = resultSet.getLong("deaths");
                    // get active cases from results
                    mActive = resultSet.getLong("active_cases");
                } 
                // close results
                resultSet.close();
                // close statement
                statement.close();
            }
        }
        // test current day results
        if (mCases == 0 && mDeaths == 0 && mActive == 0) {
            String s = String.format("No record found for country id %d on %s", 
                    countryId, simpleDateFormat.format(date));
            throw new SQLException(s);
        }
        // calculate and insert dialy data
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
     * @throws SQLException if an error
     */
    public void insertCountry(Connection conn, String country) 
            throws SQLException {
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
        }
    }
    
    /**
     * Method to select a country code
     * @param conn to the database
     * @param country to get the code for
     * @return country code
     * @throws SQLException if an error
     */
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
        final String INSERT_US_TOTALS_SQL = "INSERT INTO state_totals" + 
            " (state_id, cases, deaths, active_cases, date)" + 
                " VALUES (?, ?, ?, ?, ?);";
        // Declare variable
        String result = "";
        int mStateId = 0;
        Date mDate = null;
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
                        mStateId = stateId;
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
                        mDate = parsed;
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
                    if (mStateId > 0 && mDate != null) {
                        insertStateDaily(conn, mStateId, mDate);
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Method to insert state daily
     * @param conn to the database
     * @param stateId of the state
     * @param date used
     * @throws SQLException if an error
     */
    private void insertStateDaily(Connection conn, int stateId, Date date) 
            throws SQLException {
        // Declare constant
        final String GET_US_STATE_TOTAL_SQL = "SELECT cases, deaths, "
                + "active_cases FROM state_totals WHERE date = ? "
                + "AND state_id = ?";
        final String INSERT_US_STATE_DAILY_SQL = "INSERT INTO state_dailies" + 
            " (state_id, cases, deaths, recovered, date)" + 
                " VALUES (?, ?, ?, ?, ?);";
        if (stateId == 0 || date == null) {
            return;
        }
        Date date1 = new Date(date.getTime() - DAY);
        long mCases = 0l, mCases1 = 0l, mDeaths = 0l, mDeaths1 = 0l, 
                mActive = 0l, mActive1 = 0l, mNewCases, mNewDeaths, mRecovered;
        try ( 
            // statement to use to get previous days data
            PreparedStatement statement1 = 
                    conn.prepareStatement(GET_US_STATE_TOTAL_SQL)) { 
            // add date parameter
            java.sql.Date tempDate = new java.sql.Date(date1.getTime());
            statement1.setDate(1, tempDate);
            statement1.setInt(2, stateId);
            // check if results
            try ( 
                // run query and get results
                ResultSet resultSet1 = statement1.executeQuery()) {
                // check if results
                while (resultSet1.next()) {
                    // get cases from results
                    mCases1 = resultSet1.getLong("cases");
                    // get deaths from results
                    mDeaths1 = resultSet1.getLong("deaths");
                    // get active cases from results
                    mActive1 = resultSet1.getLong("active_cases");
                } 
                // close results
                resultSet1.close();
                // close statement
                statement1.close();
            }
        }
        try ( 
            // statement to use to get current days data
            PreparedStatement statement = 
                    conn.prepareStatement(GET_US_STATE_TOTAL_SQL)) { 
            // add date parameter
            java.sql.Date tempDate = new java.sql.Date(date.getTime());
            statement.setDate(1, tempDate);
            statement.setInt(2, stateId);
            // check if results
            try ( 
                // run query and get results
                ResultSet resultSet = statement.executeQuery()) {
                // check if results
                while (resultSet.next()) {
                    // get cases from results
                    mCases = resultSet.getLong("cases");
                    // get deaths from results
                    mDeaths = resultSet.getLong("deaths");
                    // get active cases from results
                    mActive = resultSet.getLong("active_cases");
                } 
                // close results
                resultSet.close();
                // close statement
                statement.close();
            }
        }
        // test current day results
        if (mCases == 0 && mDeaths == 0 && mActive == 0) {
            String s = String.format("No record found for state id %d on %s", 
                    stateId, simpleDateFormat.format(date));
            throw new SQLException(s);
        }
        // calculate and insert dialy data
        try ( 
            // statement to use
            PreparedStatement statement2 = 
                    conn.prepareStatement(INSERT_US_STATE_DAILY_SQL)) { 
            // add state id parameter
            statement2.setInt(1, stateId);
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
     * Method to insert population into database
     * @param conn to the database
     * @param state to enter
     * @param population to enter
     * @throws SQLException 
     */
    public void insertStatePopulation(Connection conn, String state, 
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
     * Method to insert world population
     * @param conn to database
     * @param country to insert population for
     * @param population to insert
     * @throws SQLException if an error
     */
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
}
