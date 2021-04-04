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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class to process comma delimited files
 * @author GaryL
 */
public class CSVUtilities {
    // Declare member varaible
    Results mResults;
    
    /**
     * Default constructor
     * @param results to keep track of errors
     */
    public CSVUtilities (Results results) {
        mResults = results;
    }
    
    /**
     * Method to read a csv file and return a list of list of strings
     * @param fileName to process a complete path (directory must exist)
     * @return a list of list of strings
     */
    public List<List<String>> getCsvFile(String fileName) {
        //Declare variables
        
        List<List<String>> listStringLists = new ArrayList<>();
        BufferedReader csvReader;
        String row;
        
        
        try {
            csvReader = new BufferedReader(new FileReader(fileName));
            while ((row = csvReader.readLine()) != null) {
                List<String> stringList = new ArrayList<>();
                if (!row.contains("\"")) {
                    String[] data = row.split(",");
                    stringList.addAll(Arrays.asList(data));
                } else {
                    int position = 0;
                    do {
                        int beginQuote = row.indexOf('"', position);
                        int endQuote = row.indexOf('"', beginQuote + 1);
                        if (!(beginQuote < 0 || endQuote < 0)) {
                            if (beginQuote > position) {
                                String[] data = row.split(",");
                                stringList.addAll(Arrays.asList(data));
                            }
                            String temp = row.substring(beginQuote, endQuote);
                            temp = temp.replace("\"", "");
                            stringList.add(temp);
                            position = endQuote + 2;
                        } else {
                            if (position < row.length()) {
                                String temp = row.substring(position);
                                String[] data = temp.split(",");
                                stringList.addAll(Arrays.asList(data));
                                position = row.length();
                            }
                        }
                    } while(position < row.length());
                }
                
                if (stringList.size() > 0) {
                    listStringLists.add(stringList);
                }
            }
            csvReader.close();
        } catch (IOException e) {
            mResults.addResults("getCSVFile " + fileName + " " + 
                    e.getMessage());
        }
        return listStringLists;
    }
    
    /**
     * Method to write a csv data to a file
     * @param lists the data in the form of a list of lists of strings
     * @param fileName to save data to
     */
    public void writeCSVFile(List<List<String>> lists, String fileName) {
        // convert list of list of string to one string
        String contents = createCSVString(lists);
        
        // write the contents to the file
        try ( // open the file
                FileWriter myWriter = new FileWriter(fileName)) {
            // write the contents to the file
            myWriter.write(contents);
            // close the file
        }catch(IOException e) {
            mResults.addResults("writeCSVFile " + fileName + " " + 
                    e.getMessage());
        }
    }

    /**
     * Method to create the comma-separated values file and add yesterday's date
     * @param lists the data to convert
     * @return a complete comma-separated values string
     */
    private String createCSVString(List<List<String>> lists) {
        // Declare variables
        String[] stringArray = new String[lists.size()];
        StringBuilder finalString = new StringBuilder();
        // loop through the lists
        for (int i = 0; i < lists.size(); i++) {
            for (int j = 0; j < lists.get(i).size(); j++) {
                // if first entry in list initialize and put first entry in
                if (j == 0) {
                    stringArray[i] = '"' + lists.get(i).get(j) + '"' + ',';
                    // if last entry in list new line
                } else if (j == lists.get(i).size() - 1) {
                    if (i != lists.size() - 1) {
                        // add entry and new line
                        stringArray[i] += '"' + lists.get(i).get(j) + '"' + '\n';
                    } else {
                        // add entry
                        stringArray[i] += '"' + lists.get(i).get(j) + '"';
                    }
                } else {
                    // add entry and comma
                    stringArray[i] += '"' + lists.get(i).get(j) + '"' + ',';
                }
            }
        }
        // put all rows into one string
        for (String s : stringArray) {
            finalString.append(s);
        }
        return finalString.toString();
    }
}
