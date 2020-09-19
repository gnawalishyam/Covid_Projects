<?php
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

    // load information from configuration file
    function loadConfig( $vars = array() ) {
        foreach( $vars as $v ) {
            define( $v, get_cfg_var( "covid.cfg.$v" ) );
        }
    }

    // Then call :
    $constantNames = array( 'DB_HOST', 'DB_NAME', 'DB_USER', 'DB_PASS');
    loadConfig( $constantNames );
    $connString = sprintf("host=%s dbname=%s user=%s password=%s", 
            DB_HOST, DB_NAME, DB_USER, DB_PASS);
    // open connection to database
    $db_conn = pg_connect($connString);
    if (!$db_conn) {
        die("Could not connect");
    }
    // Build Queries
    define ("WORLD_QUERY" , "SELECT total_cases, total_deaths, 
                active_cases, total_date
                FROM latest_world_totals");
    define ("TABLE_QUERY", "SELECT country, total_cases, total_deaths, 
                active_cases, population, total_date, new_cases 
                FROM main_table");
    // get results from world query
    $worldResult = pg_query($db_conn, WORLD_QUERY);
    if (!$worldResult) {
        die("No World Results");
    }
    // get results from table query
    $tableResult = pg_query($db_conn, TABLE_QUERY);
    if (!$tableResult) {
        die("No Table Results");
    }
    // get world row
    $worldRow = pg_fetch_row($worldResult);
  // Build table body
  while ($row = pg_fetch_row($tableResult)) {
    echo "<tr>";
    echo "<td>" . $row[0] . "</td>";
    echo "<td>" . number_format(intval($row[4])) . "</td>";
    echo "<td>" . number_format(intval($row[1])) . "</td>";
    echo "<td>" . number_format((intval($row[1])/ 
            intval($worldRow[0])) * 100) . "%" . "</td>";
    echo "<td>" . number_format(intval($row[6])) . "</td>";
    echo "<td>" . number_format(intval($row[3])) . "</td>";
    echo "<td>" . number_format((intval($row[3]) / 
            intval($worldRow[2])) * 100) . "%" . "</td>";
    echo "<td>" . number_format(intval($row[2])) . "</td>";
    echo "<td>" . number_format((intval($row[2]) / 
            intval($worldRow[1])) * 100) . "%" . "</td>";
    echo "<td>" . number_format(($row[1]) - intval($row[3])) . "</td>";
    echo "<td>" . number_format(((intval($row[1]) - intval($row[3])) / 
            (intval($worldRow[0]) - 
            intval($worldRow[2]))) * 100) . "%" . "</td>";
    echo "<td>" . number_format((intval($row[3]) / intval($row[1])) * 100) . 
            "%" . "</td>";
    echo "</tr>";
  }

?>