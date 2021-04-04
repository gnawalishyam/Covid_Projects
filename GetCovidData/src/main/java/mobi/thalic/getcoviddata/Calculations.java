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
package mobi.thalic.getcoviddata;

/**
 * class to manage the calculations for the front end
 * @author Gary Larson gary@thalic.mobi
 */
public class Calculations {
    // Declare member variables
    private String country;
    private java.sql.Date date;
    
    private long population;
    private int populationRank;
    
    private double percentPopulation;
    private double mortalityRate;
    private double percentDeaths;
    private double percentActive;
    private double percentRecovered;
    private double percentCases;
    
    private long totalCases;
    private long newCases;
    private long totalDeaths;
    private long newDeaths;
    private long totalActiveCases;
    
    private double deaths100k;
    private int deaths100kRank;
    private int deaths100kScore;
    private String deaths100kGrade;
    private double active100k;
    private int active100kRank;
    private int active100kScore;
    private String active100kGrade;
    private double cases100k;
    private int cases100kRank;
    private int cases100kScore;
    private String cases100kGrade;
    
    private double cases100k15;
    private int cases100k15Rank;
    private int cases100k15Score;
    private String cases100k15Grade;
    private double deaths100k15;
    private int deaths100k15Rank;
    private int deaths100k15Score;
    private String deaths100k15Grade;
    private double cases100k30;
    private int cases100k30Rank;
    private int cases100k30Score;
    private String cases100k30Grade;
    private double deaths100k30;
    private int deaths100k30Rank;
    private int deaths100k30Score;
    private String deaths100k30Grade;
    
    private int rank;
    private int score;
    private String grade;
    
    /**
     * Default Constructor
     */
    public Calculations() {
        country = "";

        population = 0l;
        populationRank = 0;

        percentPopulation = 0;
        mortalityRate = 0;
        percentDeaths = 0;
        percentActive = 0;
        percentRecovered = 0;
        percentCases = 0;

        totalCases = 0l;
        newCases = 0l;
        totalDeaths = 0l;
        newDeaths = 0l;
        totalActiveCases = 0l;

        deaths100k = 0;
        deaths100kRank = 0;
        deaths100kScore = 0;
        deaths100kGrade = "";
        active100k = 0;
        active100kRank = 0;
        active100kScore = 0;
        active100kGrade = "";
        cases100k = 0;
        cases100kRank = 0;
        cases100kScore = 0;
        cases100kGrade = "";

        cases100k15 = 0;
        cases100k15Rank = 0;
        cases100k15Score = 0;
        cases100k15Grade = "";
        deaths100k15 = 0;
        deaths100k15Rank = 0;
        deaths100k15Score = 0;
        deaths100k15Grade = "";
        cases100k30 = 0;
        cases100k30Rank = 0;
        cases100k30Score = 0;
        cases100k30Grade = "";
        deaths100k30 = 0;
        deaths100k30Rank = 0;
        deaths100k30Score = 0;
        deaths100k30Grade = "";

        rank = 0;
        score = 0;
        grade = "";
    }
    
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
     * Getter for mortality rate
     * @return mortality rate
     */
    public double getMortalityRate() {
        return mortalityRate;
    }
    
    /**
     * Setter for mortality rate
     * @param rate to set
     */
    public void setMortalityRate(double rate) {
        this.mortalityRate = rate;
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
     * Getter for total cases
     * @return total cases
     */
    public long getTotalCases() {
        return totalCases;
    }
    
    /**
     * Setter for total cases
     * @param totalCases to set
     */
    public void setTotalCases(long totalCases) {
        this.totalCases = totalCases;
    }
    
    /**
     * Getter for new cases
     * @return new cases
     */
    public long getNewCases() {
        return newCases;
    }
    
    /**
     * Setter for new cases
     * @param newCases to set
     */
    public void setNewCases(long newCases) {
        this.newCases = newCases;
    }
    
    /**
     * Getter for total deaths
     * @return total deaths
     */
    public long getTotalDeaths() {
        return totalDeaths;
    }
    
    /**
     * Setter for total deaths
     * @param totalDeaths to set
     */
    public void setTotalDeaths(long totalDeaths) {
        this.totalDeaths = totalDeaths;
    }
    
    /**
     * Getter for new deaths
     * @return new deaths
     */
    public long getNewDeaths() {
        return newDeaths;
    }
    
    /**
     * Setter for new deaths
     * @param newDeaths to set
     */
    public void setNewDeaths(long newDeaths) {
        this.newDeaths = newDeaths;
    }
    
    /**
     * Getter for total active cases
     * @return total active cases
     */
    public long getTotalActiveCases() {
        return totalActiveCases;
    }
    
    /**
     * Setter for total Active cases
     * @param totalActiveCases to set
     */
    public void setTotalActiveCases(long totalActiveCases) {
        this.totalActiveCases = totalActiveCases;
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
     * Getter for deaths per 100,000 population
     * @return deaths per 100,000 population
     */
    public double getDeaths100k() {
        return deaths100k;
    }
    
    /**
     * Setter for deaths per 100,000 population
     * @param deaths100k to set
     */
    public void setDeaths100k(double deaths100k) {
        this.deaths100k = deaths100k;
    }
    
    /**
     * Getter for deaths per 100,000 population rank
     * @return deaths per 100,000 population rank
     */
    public int getDeaths100kRank() {
        return deaths100kRank;
    }
    
    /**
     * Setter for deaths per 100,000 population rank
     * @param rank to set
     */
    public void setDeaths100kRank(int rank) {
        this.deaths100kRank = rank;
    }
    
    /**
     * Getter for deaths per 100,000 population score
     * @return deaths per 100,000 population score
     */
    public int getDeaths100kScore() {
        return deaths100kScore;
    }
    
    /**
     * Setter for deaths per 10,000 population score
     * @param score to set
     */
    public void setDeaths100kScore(int score) {
        this.deaths100kScore = score;
    }
    
    /**
     * Getter for deaths per 100,000 population grade
     * @return deaths per 100,000 population grade
     */
    public String getDeaths100kGrade() {
        return deaths100kGrade;
    }
    
    /**
     * Setter for deaths per 100,000 population grade
     * @param grade to set
     */
    public void setDeaths100kGrade(String grade) {
        this.deaths100kGrade = grade;
    }
    
    /**
     * Getter for active cases per 100,000 population
     * @return active cases per 100,000 population
     */
    public double getActive100k() {
        return active100k;
    }
    
    /**
     * Setter for active cases per 10,000 population
     * @param active100k to set
     */
    public void setActive100k(double active100k) {
        this.active100k = active100k;
    }
    
    /**
     * Getter for active cases per 100,000 population rank
     * @return active cases per 100,000 population rank
     */
    public int getActive100kRank() {
        return active100kRank;
    }
    
    /**
     * Setter for active cases per 100,000 population rank
     * @param rank to set
     */
    public void setActive100kRank(int rank) {
        this.active100kRank = rank;
    }
    
    /**
     * Getter for active cases per 100,000 population score
     * @return active cases per 100,000 population score
     */
    public int getActive100kScore() {
        return active100kScore;
    }
    
    /**
     * Setter for active cases per 100,000 population score
     * @param score to set
     */
    public void setActive100kScore(int score) {
        this.active100kScore = score;
    }
    
    /**
     * Getter for active cases per 100,000 population grade
     * @return active cases per 100,000 population grade
     */
    public String getActive100kGrade() {
        return active100kGrade;
    }
    
    /**
     * Setter for active cases per 100,000 population grade
     * @param grade to set
     */
    public void setActive100kGrade(String grade) {
        this.active100kGrade = grade;
    }
    
    /**
     * Getter for total cases per 100,000 population
     * @return total cases per 100,000 population
     */
    public double getCases100k() {
        return cases100k;
    }
    
    /**
     * Setter for total cases per 100,000 population
     * @param cases100k to set
     */
    public void setCases100k(double cases100k) {
        this.cases100k = cases100k;
    }
    
    /**
     * Getter for total cases per 100,000 population rank
     * @return total cases per 100,000 population rank
     */
    public int getCases100kRank() {
        return cases100kRank;
    }
    
    /**
     * Setter for total cases per 100,000 population rank
     * @param rank to set
     */
    public void setCases100kRank(int rank) {
        this.cases100kRank = rank;
    }
    
    /**
     * Getter for total cases per 100,000 population score
     * @return total cases per 100,000 population score
     */
    public int getCases100kScore() {
        return cases100kScore;
    }
    
    /**
     * Setter for total cases per 100,000 population score
     * @param score to set
     */
    public void setCases100kScore(int score) {
        this.cases100kScore = score;
    }
    
    /**
     * Getter for total cases per 100,000 population grade
     * @return total cases per 100,000 population grade
     */
    public String getCases100kGrade() {
        return cases100kGrade;
    }
    
    /**
     * Setter for total cases per 100,000 population grade
     * @param grade to set
     */
    public void setCases100kGrade(String grade) {
        this.cases100kGrade = grade;
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
    public int getScore() {
        return score;
    }
    
    /**
     * Setter for overall score
     * @param score to set
     */
    public void setScore(int score) {
        this.score = score;
    }
    
    /**
     * Getter for overall grade
     * @return overall grade
     */
    public String getGrade() {
        return grade;
    }
    
    /**
     * Setter for overall grade
     * @param grade to set
     */
    public void setGrade(String grade) {
        this.grade = grade;
    }
    
    /**
     * Getter for total cases per 100,000 population average over last 15 days
     * @return total cases per 100,000 population average over last 15 days
     */
    public double getCases100k15() {
        return cases100k15;
    }
    
    /**
     * Setter for total cases per 100,000 population average over last 15 days 
     * @param cases100k to set
     */
    public void setCases100k15(double cases100k) {
        this.cases100k15 = cases100k;
    }
    
    /**
     * Getter for total cases per 100,000 population rank average over last 15 days
     * @return total cases per 100,000 population rank average over last 15 days
     */
    public int getCases100k15Rank() {
        return cases100k15Rank;
    }
    
    /**
     * Setter for total cases per 100,000 population rank average over last 15 days
     * @param rank to set
     */
    public void setCases100k15Rank(int rank) {
        this.cases100k15Rank = rank;
    }
    
    /**
     * Getter for total cases per 100,000 population score average over last 15 days
     * @return total cases per 100,000 population score  average over last 30 days
     */
    public int getCases100k15Score() {
        return cases100k15Score;
    }
    
    /**
     * Setter for total cases per 10,000 population score average over last 15 days
     * @param score to set
     */
    public void setCases100k15Score(int score) {
        this.cases100k15Score = score;
    }
    
    /**
     * Getter for total cases per 100,000 population grade average over last 15 days
     * @return total cases per 100,000 population grade  average over last 30 days
     */
    public String getCases100k15Grade() {
        return cases100k15Grade;
    }
    
    /**
     * Setter for total cases per 10,000 population grade average over last 15 days
     * @param grade to set
     */
    public void setCases100k15Grade(String grade) {
        this.cases100k15Grade = grade;
    }
    
    /**
     * Getter for deaths per 100,000 population average over last 15 days
     * @return deaths per 100,000 population average over last 15 days
     */
    public double getDeaths100k15() {
        return deaths100k15;
    }
    
    /**
     * Setter for deaths per 100,000 population average over last 15 days
     * @param deaths100k to set
     */
    public void setDeaths100k15(double deaths100k) {
        this.deaths100k15 = deaths100k;
    }
    
    /**
     * Getter for deaths per 100,000 population rank average over last 15 days
     * @return deaths per 100,000 population rank average over last 15 days
     */
    public int getDeaths100k15Rank() {
        return deaths100k15Rank;
    }
    
    /**
     * Setter for deaths per 100,000 population rank average over last 15 days
     * @param rank to set
     */
    public void setDeaths100k15Rank(int rank) {
        this.deaths100k15Rank = rank;
    }
    
    /**
     * Getter for deaths per 100,000 population score average over last 15 days
     * @return deaths per 100,000 population score average over last 15 days
     */
    public int getDeaths100k15Score() {
        return deaths100k15Score;
    }
    
    /**
     * Setter for deaths per 100,000 population score average over last 15 days
     * @param score to set
     */
    public void setDeaths100k15Score(int score) {
        this.deaths100k15Score = score;
    }
    
    /**
     * Getter for deaths per 100,000 population grade average over last 15 days
     * @return deaths per 100,000 population grade average over last 15 days
     */
    public String getDeaths100k15Grade() {
        return deaths100k15Grade;
    }
    
    /**
     * Setter for deaths per 100,000 population grade average over last 15 days
     * @param grade to set
     */
    public void setDeaths100k15Grade(String grade) {
        this.deaths100k15Grade = grade;
    }
    
    /**
     * Getter for total cases per 100,000 population average over last 30 days
     * @return total cases per 100,000 population average over last 30 days
     */
    public double getCases100k30() {
        return cases100k30;
    }
    
    /**
     * Setter for total cases per 100,000 population average over last 30 days 
     * @param cases100k to set
     */
    public void setCases100k30(double cases100k) {
        this.cases100k30 = cases100k;
    }
    
    /**
     * Getter for total cases per 100,000 population rank average over last 30 days
     * @return total cases per 100,000 population rank average over last 30 days
     */
    public int getCases100k30Rank() {
        return cases100k30Rank;
    }
    
    /**
     * Setter for total cases per 100,000 population rank average over last 30 days
     * @param rank to set
     */
    public void setCases100k30Rank(int rank) {
        this.cases100k30Rank = rank;
    }
    
    /**
     * Getter for total cases per 100,000 population score average over last 30 days
     * @return total cases per 100,000 population score  average over last 30 days
     */
    public int getCases100k30Score() {
        return cases100k30Score;
    }
    
    /**
     * Setter for total cases per 10,000 population score average over last 30 days
     * @param score to set
     */
    public void setCases100k30Score(int score) {
        this.cases100k30Score = score;
    }
    
    /**
     * Getter for total cases per 100,000 population grade average over last 30 days
     * @return total cases per 100,000 population grade average over last 30 days
     */
    public String getCases100k30Grade() {
        return cases100k30Grade;
    }
    
    /**
     * Setter for total cases per 10,000 population grade average over last 30 days
     * @param grade to set
     */
    public void setCases100k30Grade(String grade) {
        this.cases100k30Grade = grade;
    }
    
    /**
     * Getter for deaths per 100,000 population average over last 30 days
     * @return deaths per 100,000 population average over last 30 days
     */
    public double getDeaths100k30() {
        return deaths100k30;
    }
    
    /**
     * Setter for deaths per 100,000 population average over last 30 days
     * @param deaths100k to set
     */
    public void setDeaths100k30(double deaths100k) {
        this.deaths100k30 = deaths100k;
    }
    
    /**
     * Getter for deaths per 100,000 population rank average over last 30 days
     * @return deaths per 100,000 population rank average over last 30 days
     */
    public int getDeaths100k30Rank() {
        return deaths100k30Rank;
    }
    
    /**
     * Setter for deaths per 100,000 population rank average over last 30 days
     * @param rank to set
     */
    public void setDeaths100k30Rank(int rank) {
        this.deaths100k30Rank = rank;
    }
    
    /**
     * Getter for deaths per 100,000 population score average over last 30 days
     * @return deaths per 100,000 population score average over last 30 days
     */
    public int getDeaths100k30Score() {
        return deaths100k30Score;
    }
    
    /**
     * Setter for deaths per 100,000 population score average over last 30 days
     * @param score to set
     */
    public void setDeaths100k30Score(int score) {
        this.deaths100k30Score = score;
    }
    
    /**
     * Getter for deaths per 100,000 population grade average over last 30 days
     * @return deaths per 100,000 population grade average over last 30 days
     */
    public String getDeaths100k30Grade() {
        return deaths100k30Grade;
    }
    
    /**
     * Setter for deaths per 100,000 population grade average over last 30 days
     * @param grade to set
     */
    public void setDeaths100k30Grade(String grade) {
        this.deaths100k30Grade = grade;
    }
}