/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobi.thalic.covid;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Gary Larson gary@thalic.mobi
 */
public class JSONUtilities {
    public List<List<String>> processJsonArrayFile (String fileName) throws Exception {
        // parsing file "JSONExample.json" 
        
        Object obj = new JSONParser().parse(new InputStreamReader(new FileInputStream(fileName), "UTF-8")); 
          
        // typecasting obj to JSONObject 
        JSONArray jsonArray = (JSONArray) obj; 
   
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
