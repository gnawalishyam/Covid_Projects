/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobi.thalic.covid;

/**
 *
 * @author Gary Larson gary@thalic.mobi
 */
public class CountryLong {
    // Declare member variables
    private String country;
    private long value;
    
    /**
     * Default constructor
     */
    public CountryLong () {};
    
    /**
     * Constructor for all variables
     * @param country to set
     * @param value to set
     */
    public CountryLong (String country, long value) {
        this.country = country;
        this.value = value;
    }
    
    /**
     * Getter for country
     * @return country
     */
    public String getCountry() {
        return country;
    }
    
    /**
     * Setter for country
     * @param country to set
     */
    public void setCountry(String country) {
        this.country = country;
    }
    
    /**
     * Getter for value
     * @return value
     */
    public long getValue() {
        return value;
    }
    
    /**
     * Setter for value
     * @param value to set
     */
    public void setValue(long value) {
        this.value = value;
    }
}
