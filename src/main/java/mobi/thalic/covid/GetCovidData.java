/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobi.thalic.covid;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

/**
 *
 * @author GaryL
 */
public class GetCovidData {
    // Declare constants
//    private static final String US_STATES_POPULATION = "https://en.wikipedia.org/wiki/List_of_states_and_territories_of_the_United_States_by_population";
//    private static final String COUNTRY_CODE_CSV_FILE = "C:\\covid\\Files\\country_codes.csv";
    

    
    
    
    /**
     * Main entry Method
     * @param args command line arguments
     */
    public static void main(String[] args)  {
        final String DATABASE_CONNECTION_STRING = 
            "jdbc:postgresql://52d9225.online-server.cloud:5432/covid";
        final String DATABASE_USER_NAME = "java_program";
        final String DATABASE_USER_PASSWORD = "3peb3NnzY_2Md@*yGb";
        final String WORLDOMETER_US = 
                "https://www.worldometers.info/coronavirus/country/us/";
        final String WORLDOMETER_ALL = 
                    "https://www.worldometers.info/coronavirus/";
        final String US_DAILY_DATA_SQL = 
            "with a AS " +
            "(SELECT state, total_cases, total_deaths, active_cases, " + 
                "population, total_date " +
            "FROM states INNER JOIN totals ON states.id = totals.state_id " +
            "LEFT OUTER JOIN populations ON states.id = populations.state_id " +
            "WHERE total_date = '2020-08-27' ORDER BY totals.id)\n" +
            "SELECT * FROM a UNION ALL \n" +
            "SELECT 'Totals', SUM(total_cases), SUM(total_deaths), " + 
                "SUM(active_cases), SUM(population), total_date " +
            "FROM a GROUP BY total_date";
        final String WORLD_DAILY_DATA_SQL = 
            "with a AS " +
            "(SELECT country, total_cases, total_deaths, active_cases, " + 
            "population, total_date " +
            "FROM countries INNER JOIN totals ON countries.id = " + 
                "totals.country_id " +
            "LEFT OUTER JOIN populations ON countries.id = " + 
                "populations.country_id " +
            "WHERE total_date = '2020-08-27' " +
            "ORDER BY totals.id) \n" +
            "SELECT * FROM a UNION ALL \n" +
            "SELECT 'Totals', SUM(total_cases), SUM(total_deaths), " + 
                "SUM(active_cases), SUM(population), total_date " +
            "FROM a GROUP BY total_date";

        
        
        
        CovidData covidData = new CovidData();
        String results = covidData.processWorldometerScrape();
        System.out.println(results);
        //covidData.loadOurWorldInData("files\\owid-covid-data.csv");

    }
}