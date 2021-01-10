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

    // create connection string
    $servername = "52d9225.online-server.cloud";
    $username = "web_php";
    $password = 'nz3Rp"3XZL=2v4.Q';
    $database = "covid";

    try {
        // open connection to database
        $db_conn = new PDO("mysql:host=$servername;dbname=$database;charset=utf8", $username, $password);
        // set the PDO error mode to exception
        $db_conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
   
        if (!$db_conn) {
            die("Could not connect");
        }
        
        define ("COUNTRY_QUERY", "SELECT DISTINCT display, country_id "
                . "FROM country_dailies "
                . "INNER JOIN country_codes ON country_dailies.country_id = "
                . "country_codes.id WHERE country_codes.alpha_2 NOT IN ('R', 'S')"
                . "ORDER BY display;");
        
        // get results from the date query
        $stmt = $db_conn->prepare(COUNTRY_QUERY);
        $stmt->execute();
        // set the results to associative
        $stmt->setFetchMode(PDO::FETCH_ASSOC);
        if(!$stmt) {
            die("No Country Results");
        }
        
        // Build ui list
        while ($row = $stmt->fetch()) {
            echo '<li><a href="#" onclick=loadTable("' . $row["country_id"] . '");>' . 
                    $row["display"] . '</a>';
        }
      // close database connection
      $db_conn = null;
    } catch(PDOException $e) {
      echo "Connection failed: " . $e->getMessage();
    }