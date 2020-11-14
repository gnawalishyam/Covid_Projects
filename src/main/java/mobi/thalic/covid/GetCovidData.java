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
        

        CovidData covidData = new CovidData();   
        //String result = covidData.createStateDailies();
        //String result = covidData.createCountryDailies();
        String result = covidData.processWorldometerScrape();
        System.out.println(result);
    }
}