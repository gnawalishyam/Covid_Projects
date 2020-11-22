/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
        if (cases > 0) {
            return ((double) deaths / cases) * 100;
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
