/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobi.thalic.covid;


/**
 *
 * @author GaryL
 */
public class GetCovidData {
    
    /**
     * Main entry Method
     * @param args command line arguments
     */
    public static void main(String[] args)  {
        Results results = new Results();

        CovidData covidData = new CovidData(results);   
        //covidData.createStateDailies();
        //String result = covidData.createCountryDailies();
        //covidData.processWorldometerScrape();
        //covidData.createCalculations();
        covidData.createCSVFile();
        //covidData.getStatData();
        //JSONUtilities.processJsonArray();
        System.out.println(results.getResults());
    }
}