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

    // open connection to database
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
        define ("TABLE_QUERY", "SELECT country, alpha_2, alpha_3, `numeric`,
                    region, display, population
                    FROM country_codes 
                    WHERE country_codes.alpha_2 NOT IN ('R', 'S')
                    ORDER BY display");
        // create statement
        $stmt = $db_conn->prepare(TABLE_QUERY);
        // get results from table query
        $stmt->execute();
        // set the resulting array to associative
        $stmt->setFetchMode(PDO::FETCH_ASSOC);
        if (!$stmt) {
            die("No Table Results");
        }
        // Build table body
        while ($row = $stmt->fetch()) {
          echo "<tr>";
          echo "<td>" . $row["country"] . "</td>";
          echo "<td>" . $row["alpha_2"] . "</td>";
          echo "<td>" . $row["alpha_3"] . "</td>";
          echo "<td>" . number_format(intval($row["numeric"])) . "</td>"; 
          echo "<td>" . number_format(intval($row["region"])) . "</td>";
          echo "<td>" . $row["display"] . "</td>";
          echo "<td>" . number_format(intval($row["population"])) . "</td>";
          echo "</tr>";
        }
        // close database connection
        $db_conn = null;
    } catch(PDOException $e) {
      echo "Connection failed: " . $e->getMessage();
    }
