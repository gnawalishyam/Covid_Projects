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
import java.util.List;
import java.util.Locale;

/**
 * Class to deal with database
 * @author Gary Larson gary@thalic.mobi
 */
public class DatabaseUtilities {
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
     * @throws SQLException on SQL error
     * @throws ParseException on parse error
     */
    public void insertWorldTotals(Connection conn, List<List<String>> lists) 
            throws SQLException, ParseException {
        // Declare constant
        final String INSERT_COUNTRY_TOTALS_SQL = "INSERT INTO totals" + 
            " (country_id, total_cases, total_deaths, active_cases, total_date)" + 
                " VALUES (?, ?, ?, ?, ?);";
        // loop through list
        for (int i = 1; i < lists.size(); i++) {
            try ( 
                // statenent to use
                PreparedStatement statement = 
                        conn.prepareStatement(INSERT_COUNTRY_TOTALS_SQL)) {
                // add country id parameter to statement
                String tempString = lists.get(i).get(0);
                if (tempString.equals("RÃ©union")) {
                    tempString = "Réunion";
                }
                if (tempString.equals("CuraÃ§ao")) {
                    tempString = "Curaçao";
                }
                statement.setInt(1, selectCountryId(conn, tempString));
                // add total cases parameter to statement
                tempString = lists.get(i).get(1);
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
                // close statement
                statement.close();
            }
        }
    }
    
    /**
     * Method to get country id from country name
     * @param conn to use
     * @param country to find
     * @return country id
     * @throws SQLException on SQL error
     */
    public int selectCountryId(Connection conn, String country) 
            throws SQLException {
        // Declare constant
        final String SELECT_COUNTRY_ID = 
            "SELECT id FROM countries WHERE country = ?;";
        // Declare variables
        int countryId = 0;
        // add country parameter
        try ( 
            // statement to use
            PreparedStatement statement = 
                    conn.prepareStatement(SELECT_COUNTRY_ID)) {
            // add state parameter
            statement.setString(1, country);
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
    
    /**
     * Method to INSERT US totals into the database
     * @param conn to the database
     * @param lists to insert
     * @throws SQLException on SQL error
     * @throws ParseException on parse error
     */
    public void insertUSTotals(Connection conn, List<List<String>> lists) 
            throws SQLException, ParseException {
        // Declare constant
        final String INSERT_US_TOTALS_SQL = "INSERT INTO totals" + 
            " (state_id, total_cases, total_deaths, active_cases, total_date)" + 
                " VALUES (?, ?, ?, ?, ?);";
        // loop through list
        for (int i = 1; i < lists.size(); i++) {
            if (!lists.get(i).get(0).equals("Total:")) {
                try ( 
                    // statenent to use
                    PreparedStatement statement = 
                            conn.prepareStatement(INSERT_US_TOTALS_SQL)) {
                    // add state id parameter to statement
                    statement.setInt(1, selectStateId(conn, lists.get(i).get(0)));
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
                    // close statement
                    statement.close();
                }
            }
        }
    }
}
