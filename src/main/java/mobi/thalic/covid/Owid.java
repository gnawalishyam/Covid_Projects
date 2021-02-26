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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Gary Larson gary@thalic.mobi
 */
public class Owid {
    private String isoCode;
    private String continent;
    private String location;
    private long population;
    private final List<OwidDaily> dailies;
    
    public Owid() {
        this.dailies = new ArrayList<>();
    }
    
    public Owid(String isoCode, String continent, String location, 
            long population) {
        this.isoCode = isoCode;
        this.continent = continent;
        this.location = location;
        this.population = population;
        this.dailies = new ArrayList<>();
    }
    
    public String getIsoCode() {
        return this.isoCode;
    }
    
    public void setIsoCode(String isoCode) {
        this.isoCode = isoCode;
    }
    
    public String getContinent() {
        return this.continent;
    }
    
    public void setContinent(String continent) {
        this.continent = continent;
    }
    
    public String getLocation() {
        return this.location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public long getPopulation() {
        return this.population;
    }
    
    public void setPopulation(long population) {
        this.population = population;
    }
    
    public double getPopulation100k() {
        return this.population / 100000.0;
    }
    
    public List<OwidDaily> getOwidDaily () {
        return this.dailies;
    }
    
    public void addDaily(OwidDaily daily) {
        dailies.add(daily);
    }
    
}
