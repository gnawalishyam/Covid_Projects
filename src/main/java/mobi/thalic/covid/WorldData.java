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

/**
 * Class to deal with world data for calculating front end data
 * @author Gary Larson gary@thalic.mobi
 */
public class WorldData {
    private long cases;
    private long deaths;
    private long active;
    private long population;
    
    /**
     * Default constructor
     */
    public WorldData() {}
    
    /**
     * Constructor for all variables
     * @param cases to set
     * @param deaths to set
     * @param active to set
     * @param population to set
     */
    public WorldData(long cases, long deaths, long active, long population) {
        this.cases = cases;
        this.deaths = deaths;
        this.active = active;
        this.population = population;
    }
 
    /**
     * Getter for total cases
     * @return total cases
     */
    public long getCases() {
        return cases;
    }
 
    /**
     * Setter for total cases
     * @param cases to set
     */
    public void setCases(long cases) {
        this.cases = cases;
    }
    
    /**
     * Getter for deaths
     * @return deaths
     */
    public long getDeaths() {
        return deaths;
    }
   
    /**
     * Setter for deaths
     * @param deaths to set
     */
    public void setDeaths(long deaths) {
        this.deaths = deaths;
    }
    
    /**
     * Getter for active cases
     * @return active cases
     */
    public long getActive() {
        return active;
    }
    
    /**
     * Setter for active cases
     * @param active cases to set
     */
    public void setActive(long active) {
        this.active = active;
    }
    
    /**
     * Getter for population
     * @return population
     */
    public long getPopulation() {
        return population;
    }
    
    /**
     * Setter for population
     * @param population to set
     */
    public void setPopulation(long population) {
        this.population = population;
    }
   
    /**
     * Getter to calculate recovered cases
     * @return recovered cases
     */
    public long getRecovered() {
        return cases - deaths - active;
    }
   
    /**
     * Getter to calculate mortality
     * @return mortality
     */
    public double getMortality() {
        if ((deaths + getRecovered()) > 0) {
            return ((double) deaths / (deaths + getRecovered())) * 
                    100;
        }
        return 0;
    }
    
    /**
     * Getter to calculate cases per 10,000 population
     * @return cases per 10,000 population
     */
    public double getCases10k() {
        if (population > 0) {
            return ((double) cases / population) * 10000;
        }
        return 0;
    }
    
    /**
     * Getter to calculate deaths per 10,000 population
     * @return deaths per 10,000 population
     */
    public double getDeaths10k() {
        if (population > 0) {
            return ((double) deaths / population) * 10000;
        }
        return 0;
    }
    
    /**
     * Getter to calculate active cases per 10,000 population
     * @return active cases per 10,000 population
     */
    public double getActive10k() {
        if (population > 0) {
            return ((double) active / population) * 10000;
        }
        return 0;
    }
    
    /**
     * Getter to calculate recovered cases per 10,000 population
     * @return recovered cases per 10,000 population
     */
    public double getRecovered10k() {
        if (population > 0) {
            return ((double) getRecovered() / population) * 10000;
        }
        return 0;
    }
}
