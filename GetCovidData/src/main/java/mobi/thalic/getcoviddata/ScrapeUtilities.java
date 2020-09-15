/*
 * The MIT License
 *
 * Copyright 2020 Gary Larson <gary@thalic.mobi>.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Class to deal with scraping websites
 * @author Gary Larson gary@thalic.mobi
 */
public class ScrapeUtilities {
    /**
     * Method to retrieve table data from websites
     * @param url of the website to get table from
     * @return a list of string lists
     */
    public List<List<String>> getTableData(String url) {
        // Declare variables
        String htmlString = null;
        // attempt to connect to the website and get the html
        try {
            Document doc = Jsoup.connect(url).get();
            // convert html to a string
            htmlString = doc.toString();
        } catch (IOException e) {
            System.out.println("An error occurred. jsoup: " + e);
            System.exit(1);
        }

        // if no html return nothing
        if (htmlString == null) {
            return null;
        }
        // declare and initialize variables
        String tableString = null;
        int position = 0;
        // loop through html looking for tables
        do {
            int rowStart = htmlString.indexOf("<table", position);
            int rowEnd = htmlString.indexOf("</table>", rowStart);
            String htmlTableString = htmlString.substring(rowStart, rowEnd);
            // only add table if it is what we are looking for
            if (htmlTableString.contains("yesterday")) {
                if (!htmlTableString.contains("yesterday2")) {
                    tableString = htmlTableString;
                }
            }
            // test for state population table 
            // and break after locating the first one
            if (htmlTableString
                    .contains("United States House of Representatives")) {
                tableString = htmlTableString;
                break;
            }
            position = rowEnd;
            // test if there are more tables
        } while (htmlString.indexOf("<table", position) > -1);
        // if no table found return nothing
        if (tableString == null) {
            return null;
        } else {
            // call extract table from html and return list of list of strings
            return extractTable(tableString);
        }
    }

    /**
     * Method to convert an HTML string of a table 
     * into a list of list of strings
     * @param tableString to convert
     * @return converted list of list of strings
     */
    private List<List<String>> extractTable(String tableString) {
        // Declare variables
        List<List<String>> rowList;
        // look for heading of table
        int headingStart = tableString.indexOf("<thead>");
        int headingEnd = tableString.indexOf("</thead>");
        // if no standard heading take the first row
        if (headingEnd == -1 || headingStart == -1) {
            headingStart = tableString.indexOf("<tr>");
            headingEnd = tableString.indexOf("</tr>", headingStart);
        }
        // convert heading into a list of strings
        List<String> headingList = extractTableHeading(tableString
                .substring(headingStart, headingEnd));
        // initialize the list of list of strings
        rowList = new ArrayList<>();
        // add the heading as the first list of strings
        rowList.add(headingList);
        // set current position in html
        int position = headingEnd;
        // loop through the remaining rows in table (the data)
        do {
            // extract the next row
            int rowStart = tableString.indexOf("<tr", position);
            int rowEnd = tableString.indexOf("</tr>", rowStart);
            // convert html row into a list of strings
            rowList.add(extractTableRow(tableString
                    .substring(rowStart, rowEnd), headingList.size()));
            // update position in html string
            position = rowEnd;
            // test for another row
        } while (tableString.indexOf("<tr", position) > -1);
        return rowList;
    }

    /**
     * Method to convert HTML string to a list of strings
     * @param headingString to convert
     * @return list of strings
     */
    private List<String> extractTableHeading(String headingString) {
        // Declare variables
        List<String> headingList = new ArrayList<>();
        int position = 1;
        // loop through html and extract each heading into a string
        do {
            // get positions of headings
            int headingStart = headingString.indexOf("<th", position);
            headingStart = headingString.indexOf('>', headingStart + 1);
            headingStart++;
            int headingEnd = headingString.indexOf("</th>", headingStart);
            // extract string of heading with html
            String heading = headingString.substring(headingStart, headingEnd);
            // remove html from string
            heading = htmlToText(heading);
            // ignore unwanted columns
            if (!(heading.equals("Source") || heading.equals("Projections"))) {
                headingList.add(heading);
            }
            // update position in html string
            position = headingEnd;
            // test for another heading
        } while (headingString.indexOf("<th", position) > -1);
        return headingList;
    }

    /**
     * Method to convert table row to list of strings
     * @param row to convert
     * @param size number of entries to get
     * @return list of stings
     */
    private List<String> extractTableRow(String row, int size) {
        // Declare variables
        List<String> rowData = new ArrayList<>();
        int position = 1;
        // loop through the html code up to the number of headings passed in
        for (int i = 0; i < size; i++) {
            // get element start and end positions
            int rowStart = row.indexOf("<td", position);
            rowStart = row.indexOf('>', rowStart + 1);
            rowStart++;
            int rowEnd = row.indexOf("</td>", rowStart);
            // get html string of entry
            String rowString = row.substring(rowStart, rowEnd);
            // remove html from string
            rowString = htmlToText(rowString);
            // add string to list of strings
            rowData.add(rowString);
            // update position
            position = rowEnd;
        }
        return rowData;
    }

    /**
     * Method to remove HTML from a string
     * @param html string to remove HTML from
     * @return clean string of data
     */
    public String htmlToText(String html) {
        // return cleaned string
        return Jsoup.parse(html).text();
    }
}
