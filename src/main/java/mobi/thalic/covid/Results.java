/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobi.thalic.covid;

/**
 * Class to accumulate errors
 * @author Gary Larson gary@thalic.mobi
 */
public class Results {
    // declare member variables
    private String mResults;
    
    /**
     * Default constructor
     */
    public Results() {
        mResults = "";
    }
    
    /**
     * Getter for string
     * @return string
     */
    public String getResults () {
        return mResults;
    }
    
    /**
     * Method to add an error to the errors
     * @param result to add
     */
    public void addResults(String result) {
        mResults += result + "\n";
    }
}
