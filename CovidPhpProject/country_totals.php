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

    // get date entered
    $date = filter_input(INPUT_GET, 'date', FILTER_SANITIZE_URL);
    if ($date == null) {
        die("No date parameter");
    }
    
    // create connection string
    $servername = "52d9225.online-server.cloud";
    $username = "web_php";
    $password = 'nz3Rp"3XZL=2v4.Q';
    $database = "covid";

    try {
        // open connection to database
        $db_conn = new PDO("mysql:host=$servername;dbname=$database", $username, $password);
        // set the PDO error mode to exception
        $db_conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
   
        if (!$db_conn) {
            die("Could not connect");
        }
        define ("TABLE_QUERY", "SELECT display, cases, active, deaths,
                    cases - active - deaths AS recovered
                    FROM country_totals 
                    INNER JOIN country_codes 
                    ON country_totals.country_id = country_codes.id
                    WHERE date = :dateParam AND country_codes.alpha_2 NOT IN ('R', 'S')
                    ORDER BY display");
        // set params
        
        $params = [$date];
        echo $date;
        // get results from the table query
        $stmt = $db_conn->prepare(TABLE_QUERY);
        // add parameter
        $stmt->bindParam(':dateParam', $date);
        $stmt->execute();
        // set the resulting array to associative
        $stmt->setFetchMode(PDO::FETCH_ASSOC);
        if (!$stmt) {
            die("No Table Results");
        }
      // Build table body
      while ($row = $stmt->fetch()) {
        echo "<tr>";
        echo "<td>" . $row["display"] . "</td>";
        echo "<td>" . number_format(intval($row["cases"])) . "</td>";
        echo "<td>" . number_format(intval($row["active"])) . "</td>";
        echo "<td>" . number_format(intval($row["deaths"])) . "</td>"; 
        echo "<td>" . number_format(intval($row["recovered"])) . "</td>";
        echo "<td>" . $date . "</td>";
        echo "</tr>";
      }
      // close database connection
      $db_conn = null;
    } catch(PDOException $e) {
      echo "Connection failed: " . $e->getMessage();
    }