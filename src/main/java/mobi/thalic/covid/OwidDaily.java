/*
 * The MIT License
 *
 * Copyright 2021 Gary Larson gary@thalic.mobi.
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
 *
 * @author Gary Larson gary@thalic.mobi
 */
public class OwidDaily {
    private String date;
    private long totalCases;
    private long newCases;
    private long totalDeaths;
    private long newDeaths;
    private long totalTests;
    private long newTests;
    
    public OwidDaily() {}
    
    public OwidDaily(String date, long totalCases, long newCases, 
            long totalDeaths, long newDeaths, long totalTests, long newTests) {
        this.date = date;
        this.totalCases = totalCases;
        this.newCases = newCases;
        this.totalDeaths = totalDeaths;
        this.newDeaths = newDeaths;
        this.totalTests = totalTests;
        this.newTests = newTests;
    }
    
    public String getDate() {
        return this.date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public long getTotalCases() {
        return this.totalCases;
    }
    
    public void setTotalCases(long totalCases) {
        this.totalCases = totalCases;
    }
    
    public long getNewCases() {
        return this.newCases;
    }
    
    public void setNewCases(long newCases) {
        this.newCases = newCases;
    }
    
    public long getTotalDeaths() {
        return this.totalDeaths;
    }
    
    public void setTotalDeaths(long totalDeaths) {
        this.totalDeaths = totalDeaths;
    }
    
    public long getNewDeaths() {
        return this.newDeaths;
    }
    
    public void setNewDeaths(long newDeaths) {
        this.newDeaths = newDeaths;
    }
    
    public long getTotalTests() {
        return this.totalDeaths;
    }
    
    public void setTotalTests(long totalTests) {
        this.totalTests = totalTests;
    }
    
    public long getNewTests() {
        return this.newTests;
    }
    
    public void setNewTests(long newTests) {
        this.newTests = newTests;
    }
    
    public double getTotalCases100k(double population100k) {
        if (population100k <= 0.0) {
            return 0.0;
        }
        return this.totalCases / population100k;
    }
    
    public double getNewCases100k(double population100k) {
        if (population100k <= 0.0) {
            return 0.0;
        }
        return this.newCases / population100k;
    }
    
    public double getTotalDeaths100k(double population100k) {
        if (population100k <= 0.0) {
            return 0.0;
        }
        return this.totalDeaths / population100k;
    }
    
    public double getNewDeaths100k(double population100k) {
        if (population100k <= 0.0) {
            return 0.0;
        }
        return this.newDeaths / population100k;
    }
    
    public double getTotalTests100k(double population100k) {
        if (population100k <= 0.0) {
            return 0.0;
        }
        return this.totalDeaths / population100k;
    }
    
    public double getNewTests100k(double population100k) {
        if (population100k <= 0.0) {
            return 0.0;
        }
        return this.newTests / population100k;
    }
}

