/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobi.thalic.covid;

/**
 * class to manage the calculations for the front end
 * @author Gary Larson gary@thalic.mobi
 */
public class Calculations {
    // Declare member variables
    private String country;
    private java.sql.Date date;
    private double percentPopulation;
    private double percentMortality;
    private double percentDeaths;
    private double percentActive;
    private double percentRecovered;
    private double percentCases;
    private long population;
    private int populationRank;
    private double deaths10k;
    private int deaths10kRank;
    private String deaths10kScore;
    private double active10k;
    private int active10kRank;
    private String active10kScore;
    private double recovered10k;
    private int recovered10kRank;
    private String recovered10kScore;
    private double cases10k;
    private int cases10kRank;
    private String cases10kScore;
    private int rank;
    private String score;
    private double survivalRate;
    
    /**
     * Default Constructor
     */
    public Calculations() {}
    
    /**
     * Getter for country 
     * @return country 
     */
    public String getCountry () {
        return country;
    }
    
    /**
     * Setter for country 
     * @param country to set
     */
    public void setCountry (String country) {
        this.country = country;
    }
    
    /**
     * Getter for date
     * @return date
     */
    public java.sql.Date getDate () {
        return date;
    }
    
    /**
     * Setter for date
     * @param date to set
     */
    public void setDate(java.sql.Date date) {
        this.date = date;
    }
    
    /**
     * Getter for percent of population
     * @return percent of population
     */
    public double getPercentPopulation() {
        return percentPopulation;
    }
    
    /**
     * Setter for percent of population
     * @param percent to set
     */
    public void setPercentPopulation(double percent) {
        this.percentPopulation = percent;
    }
    
    /**
     * Getter for percent of mortality
     * @return percent of mortality
     */
    public double getPercentMortality() {
        return percentMortality;
    }
    
    /**
     * Setter for percent of mortality
     * @param percent to set
     */
    public void setPercentMortality(double percent) {
        this.percentMortality = percent;
    }
   
    /**
     * Getter for percent of deaths
     * @return percent of deaths
     */
    public double getPercentDeaths() {
        return percentDeaths;
    }
    
    /**
     * Setter for percent of deaths
     * @param percent to set
     */
    public void setPercentDeaths(double percent) {
        this.percentDeaths = percent;
    }
    
    /**
     * Getter for percent of active cases
     * @return percent of active cases
     */
    public double getPercentActive() {
        return percentActive;
    }
    
    /**
     * Setter for percent of active cases
     * @param percent to set
     */
    public void setPercentActive(double percent) {
        this.percentActive = percent;
    }
    
    /**
     * Getter for percent of recovered cases
     * @return percent of recovered cases
     */
    public double getPercentRecovered() {
        return percentRecovered;
    }
   
    /**
     * Setter for percent of recovered cases
     * @param percent to set 
     */
    public void setPercentRecovered(double percent) {
        this.percentRecovered = percent;
    }
    
    /**
     * Getter for percent of total cases
     * @return percent of total cases
     */
    public double getPercentCases() {
        return percentCases;
    }
   
    /**
     * Setter for percent of total cases
     * @param percent to set
     */
    public void setPercentCases(double percent) {
        this.percentCases = percent;
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
     * Getter for population rank
     * @return population rank
     */
    public int getPopulationRank() {
        return populationRank;
    }
    
    /**
     * Setter for population rank
     * @param rank to set
     */
    public void setPopulationRank(int rank) {
        this.populationRank = rank;
    }
    
    /**
     * Getter for deaths per 10,000 population
     * @return deaths per 10,000 population
     */
    public double getDeaths10k() {
        return deaths10k;
    }
    
    /**
     * Setter for deaths per 10,000 population
     * @param deaths10k to set
     */
    public void setDeaths10k(double deaths10k) {
        this.deaths10k = deaths10k;
    }
    
    /**
     * Getter for deaths per 10,000 population rank
     * @return deaths per 10,000 population rank
     */
    public int getDeaths10kRank() {
        return deaths10kRank;
    }
    
    /**
     * Setter for deaths per 10,000 population rank
     * @param rank to set
     */
    public void setDeaths10kRank(int rank) {
        this.deaths10kRank = rank;
    }
    
    /**
     * Getter for deaths per 10,000 population score
     * @return deaths per 10,000 population score
     */
    public String getDeaths10kScore() {
        return deaths10kScore;
    }
    
    /**
     * Setter for deaths per 10,000 population score
     * @param score to set
     */
    public void setDeaths10kScore(String score) {
        this.deaths10kScore = score;
    }
    
    /**
     * Getter for active cases per 10,000 population
     * @return active cases per 10,000 population
     */
    public double getActive10k() {
        return active10k;
    }
    
    /**
     * Setter for active cases per 10,000 population
     * @param active10k to set
     */
    public void setActive10k(double active10k) {
        this.active10k = active10k;
    }
    
    /**
     * Getter for active cases per 10,000 population rank
     * @return active cases per 10,000 population rank
     */
    public int getActive10kRank() {
        return active10kRank;
    }
    
    /**
     * Setter for active cases per 10,000 population rank
     * @param rank to set
     */
    public void setActive10kRank(int rank) {
        this.active10kRank = rank;
    }
    
    /**
     * Getter for active cases per 10,000 population score
     * @return active cases per 10,000 population score
     */
    public String getActive10kScore() {
        return active10kScore;
    }
    
    /**
     * Setter for active cases per 10,000 population score
     * @param score to set
     */
    public void setActive10kScore(String score) {
        this.active10kScore = score;
    }
    
    /**
     * Getter for recovered cases per 10,000 population
     * @return recovered cases per 10,000 population
     */
    public double getRecovered10k() {
        return recovered10k;
    }
    
    /**
     * Setter for recovered cases per 10,000 population
     * @param recovered10k to set
     */
    public void setRecovered10k(double recovered10k) {
        this.recovered10k = recovered10k;
    }
    
    /**
     * Getter for recovered cases per 10,000 population rank
     * @return recovered cases per 10,000 population rank
     */
    public int getRecovered10kRank() {
        return recovered10kRank;
    }
    
    /**
     * Setter for recovered cases per 10,000 population rank
     * @param rank to set
     */
    public void setRecovered10kRank(int rank) {
        this.recovered10kRank = rank;
    }
    
    /**
     * Getter for recovered cases per 10,000 population score
     * @return recovered cases per 10,000 population score
     */
    public String getRecovered10kScore() {
        return recovered10kScore;
    }
    
    /**
     * Setter for recovered cases per 10,000 population score
     * @param score to set
     */
    public void setRecovered10kScore(String score) {
        this.recovered10kScore = score;
    }
    
    /**
     * Getter for total cases per 10,000 population
     * @return total cases per 10,000 population
     */
    public double getCases10k() {
        return cases10k;
    }
    
    /**
     * Setter for total cases per 10,000 population score
     * @param cases10k to set
     */
    public void setCases10k(double cases10k) {
        this.cases10k = cases10k;
    }
    
    /**
     * Getter for total cases per 10,000 population rank
     * @return total cases per 10,000 population rank
     */
    public int getCases10kRank() {
        return cases10kRank;
    }
    
    /**
     * Setter for total cases per 10,000 population rank
     * @param rank to set
     */
    public void setCases10kRank(int rank) {
        this.cases10kRank = rank;
    }
    
    /**
     * Getter for total cases per 10,000 population score
     * @return total cases per 10,000 population score
     */
    public String getCases10kScore() {
        return cases10kScore;
    }
    
    /**
     * Setter for total cases per 10,000 population score
     * @param score to set
     */
    public void setCases10kScore(String score) {
        this.cases10kScore = score;
    }
    
    /**
     * Getter for overall rank
     * @return overall rank
     */
    public int getRank() {
        return rank;
    }
    
    /**
     * Setter for overall rank
     * @param rank to set
     */
    public void setRank(int rank) {
        this.rank = rank;
    }
    
    /**
     * Getter for overall score
     * @return overall score
     */
    public String getScore() {
        return score;
    }
    
    /**
     * Setter for overall score
     * @param score to set
     */
    public void setScore(String score) {
        this.score = score;
    }
    
    /**
     * Getter for survival rate
     * @return survival rate
     */
    public double getSurvivalRate() {
        return survivalRate;
    }
    
    /**
     * Setter for survival rate
     * @param survivalRate to set
     */
    public void setSurvivalRate(double survivalRate) {
        this.survivalRate = survivalRate;
    }
}
