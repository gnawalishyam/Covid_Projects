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
package mobi.thalic.getcoviddata;

/**
 *
 * @author Gary Larson gary@thalic.mobi
 */
public class DoubleInteger {
    // Declare member variables
    private double doubleValue;
    private int intValue;
    
    /**
     * Default constructor
     */
    public DoubleInteger () {};
    
    /**
     * Constructor for all variables
     * @param doubleValue to set
     * @param intValue to set
     */
    public DoubleInteger (double doubleValue, int intValue) {
        this.doubleValue = doubleValue;
        this.intValue = intValue;
    }
    
    /**
     * Getter for double value
     * @return double value
     */
    public double getDoubleValue() {
        return doubleValue;
    }
    
    /**
     * Setter for double value
     * @param doubleValue to set
     */
    public void setString(double doubleValue) {
        this.doubleValue = doubleValue;
    }
    
    /**
     * Getter for int value
     * @return int value
     */
    public int getIntValue() {
        return intValue;
    }
    
    /**
     * Setter for int value
     * @param intValue to set
     */
    public void setIntValue(int intValue) {
        this.intValue = intValue;
    }
}
