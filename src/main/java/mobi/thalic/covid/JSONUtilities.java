/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobi.thalic.covid;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
}
