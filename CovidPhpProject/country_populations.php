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
    define ("TABLE_QUERY", "SELECT DISTINCT country_display, population
                FROM populations INNER JOIN country_codes 
                ON populations.country_id = country_codes.id
		INNER JOIN country_totals 
                ON country_codes.id = country_totals.country_id
                WHERE country_codes.alpha_2 NOT IN ('R', 'S')
                ORDER BY country_display");
    // get results from table query
    $tableResult = pg_query($db_conn, TABLE_QUERY);
    if (!$tableResult) {
        die("No Table Results");
    }
  // Build table body
  while ($row = pg_fetch_row($tableResult)) {
    echo "<tr>";
    echo "<td>" . $row[0] . "</td>";
    echo "<td>" . number_format(intval($row[1])) . "</td>";
    echo "</tr>";
  }
  // close database connection
  pg_close($db_conn);

