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
public class StringInteger {
    // Declare member variables
    private String string;
    private int value;
    
    /**
     * Default constructor
     */
    public StringInteger () {};
    
    /**
     * Constructor for all variables
     * @param string to set
     * @param value to set
     */
    public StringInteger (String string, int value) {
        this.string = string;
        this.value = value;
    }
    
    /**
     * Getter for string
     * @return string
     */
    public String getString() {
        return string;
    }
    
    /**
     * Setter for string
     * @param string to set
     */
    public void setString(String string) {
        this.string = string;
    }
    
    /**
     * Getter for value
     * @return value
     */
    public int getValue() {
        return value;
    }
    
    /**
     * Setter for value
     * @param value to set
     */
    public void setValue(int value) {
        this.value = value;
    }
}
