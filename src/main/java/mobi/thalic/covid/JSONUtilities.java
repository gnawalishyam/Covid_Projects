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


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Class to get and process json
 * @author Gary Larson gary@thalic.mobi
 */
public class JSONUtilities {
    // Declare constante
    private final String STAT_URL_JSON = 
            "https://www.coronavirus-statistiques.com/corostats/openstats/open"
            + "_stats_coronavirus.json";
    private final String OWID_URL_JSON = 
            "https://covid.ourworldindata.org/data/owid-covid-data.json";
    // declare variable
    private final Results mResults;
    
    public JSONUtilities(Results results) {
        mResults = results;
    }
    
    /**
     * Method to process a statistiques json url
     * @param yesterday date to process
     * @return a list of lists representation of the json
     */
    public List<List<String>> processJsonArray (String yesterday) {
        //declare variables
        JSONParser jsonParser = new JSONParser();
        List<List<String>> dataList = new ArrayList<>();
        String dataString = "";
        
        try {
            URL url = new URL(STAT_URL_JSON);
            Object obj = new JSONParser().parse(
                    new InputStreamReader(url.openStream(), "UTF-8")); 
            
            JSONArray jsonArray = (JSONArray) obj;
            // create list of lists
            for (int i = 0; i < jsonArray.size(); i++) {
                List<String> data = new ArrayList<>();
                JSONObject jsonData = (JSONObject) jsonArray.get(i);
                String temp = (String) jsonData.get("date");
                if (temp.equals(yesterday)) {
                    data.add(temp);
                    temp = (String) jsonData.get("code");
                    data.add(temp);
                    temp = (String) jsonData.get("nom");
                    data.add(temp);
                    temp = (String) jsonData.get("cas");
                    data.add(temp);
                    temp = (String) jsonData.get("deces");
                    data.add(temp);
                    temp = (String) jsonData.get("guerisons");
                    data.add(temp);
                    temp = (String) jsonData.get("source");
                    data.add(temp);
                    dataList.add(data);
                }
            }
            return dataList;
        } catch (FileNotFoundException e) {
            mResults.addResults("processJsonArray FileNotFound Exception " + 
                    e.getMessage());
        } catch (IOException | ParseException e) {
            mResults.addResults("processJsonArray IO or parse Exception " + 
                    e.getMessage());
        }  
        return null;
    }
    
    /**
     * Method to process a statistiques json url
     * @return a list of lists representation of the json
     */
    public List<List<String>> processJsonArray () {
        //declare variables
        JSONParser jsonParser = new JSONParser();
        List<List<String>> dataList = new ArrayList<>();
        String dataString = "";
        
        try {
            URL url = new URL(STAT_URL_JSON);
            Object obj = new JSONParser().parse(
                    new InputStreamReader(url.openStream(), "UTF-8")); 
            
            JSONArray jsonArray = (JSONArray) obj;
            // create list of lists
            for (int i = 0; i < jsonArray.size(); i++) {
                List<String> data = new ArrayList<>();
                JSONObject jsonData = (JSONObject) jsonArray.get(i);
                String temp = (String) jsonData.get("date");
                data.add(temp);
                temp = (String) jsonData.get("code");
                data.add(temp);
                temp = (String) jsonData.get("nom");
                data.add(temp);
                temp = (String) jsonData.get("cas");
                data.add(temp);
                temp = (String) jsonData.get("deces");
                data.add(temp);
                temp = (String) jsonData.get("guerisons");
                data.add(temp);
                temp = (String) jsonData.get("source");
                data.add(temp);
                dataList.add(data);
            }
            return dataList;
        } catch (FileNotFoundException e) {
            mResults.addResults("processJsonArray FileNotFound Exception " + 
                    e.getMessage());
        } catch (IOException | ParseException e) {
            mResults.addResults("processJsonArray IO or parse Exception " + 
                    e.getMessage());
        }  
        return null;
    }
    
    /**
     * Method to process a statistiques json file that is an array
     * @param fileName to process
     * @return a list of lists representation of the json file
     */
    public List<List<String>> processJsonArrayFile (String fileName) {
        // parsing file "JSONExample.json" 
        Object object = null;
        try {
            object = new JSONParser().parse(new InputStreamReader(
                    new FileInputStream(fileName), "UTF-8")); 
        } catch (IOException e) {
            mResults.addResults("processJsonArrayFile " + e.getMessage());
        } catch (ParseException e) {
            mResults.addResults("processJsonArrayFile Parse Exception" + 
                    e.getMessage());
        }
        if (object == null) {
            return null;
        }                 
        // typecasting obj to JSONObject 
        JSONArray jsonArray = (JSONArray) object; 
   
        List<List<String>> dataList = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            List<String> data = new ArrayList<>();
            JSONObject jsonData = (JSONObject) jsonArray.get(i);
            String temp = (String) jsonData.get("date");
            data.add(temp);
            temp = (String) jsonData.get("code");
            data.add(temp);
            temp = (String) jsonData.get("nom");
            data.add(temp);
            temp = (String) jsonData.get("cas");
            data.add(temp);
            temp = (String) jsonData.get("deces");
            data.add(temp);
            temp = (String) jsonData.get("gueaisons");
            data.add(temp);
            temp = (String) jsonData.get("source");
            data.add(temp);
            dataList.add(data);
        }
        return dataList;
    }
    
    /**
     * Method to process Our World In Data json url
     * @return a list of Owid objects representation of the json
     */
    public List<Owid> processOwidFullJson () {
        //declare variables
        List<Owid> dataList = new ArrayList<>();
        
        try {
            URL url = new URL(OWID_URL_JSON);
            Object obj = new JSONParser().parse(
                    new InputStreamReader(url.openStream(), "UTF-8")); 
            
            JSONObject jsonObj = (JSONObject) obj;
            Set<String> keys = jsonObj.keySet();
           
            // create list of lists
            keys.stream().map(isoCode -> { 
                JSONObject owidJson = (JSONObject) jsonObj.get(isoCode);
                Owid owid = new Owid();
                owid.setIsoCode(isoCode);
                if (owidJson.containsKey("continent")) {
                    owid.setContinent((String) owidJson.get("continent"));
                }
                owid.setLocation((String) owidJson.get("location"));
                if (owidJson.containsKey("population")) {
                    double tempDouble = (double) owidJson.get("population");
                    owid.setPopulation((long) tempDouble);
                }
                JSONArray jsonArray = (JSONArray) owidJson.get("data");
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonData = (JSONObject) jsonArray.get(i);
                    OwidDaily daily = new OwidDaily();
                    daily.setDate((String) jsonData.get("date"));
                    if (jsonData.containsKey("total_cases")) {
                        double tempDouble = (double) jsonData.get("total_cases");
                        daily.setTotalCases((long) tempDouble);
                    }
                    if (jsonData.containsKey("new_cases")) {
                        double tempDouble = (double) jsonData.get("new_cases");
                        daily.setNewCases((long) tempDouble);
                    }
                    if (jsonData.containsKey("total_deaths")) {
                        double tempDouble = (double) jsonData.get("total_deaths");
                        daily.setTotalDeaths((long) tempDouble);
                    }
                    if (jsonData.containsKey("new_deaths")) {
                        double tempDouble = (double) jsonData.get("new_deaths");
                        daily.setNewDeaths((long) tempDouble);
                    }
                    if (jsonData.containsKey("total_tests")) {
                        double tempDouble = (double) jsonData.get("total_tests");
                        daily.setTotalTests((long) tempDouble);
                    }
                    if (jsonData.containsKey("new_tests")) {
                        double tempDouble = (double) jsonData.get("new_tests");
                        daily.setNewTests((long) tempDouble);
                    }
                    owid.addDaily(daily);
                }
                return owid;
            }).forEachOrdered(owid -> {
                dataList.add(owid);
            });
            return dataList;
        } catch (FileNotFoundException e) {
            mResults.addResults("processJsonArray FileNotFound Exception " + 
                    e.getMessage());
        } catch (IOException | ParseException e) {
            mResults.addResults("processJsonArray IO or parse Exception " + 
                    e.getMessage());
        }  
        return null;
    }
}
