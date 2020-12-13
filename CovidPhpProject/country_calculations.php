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

    $userDate = filter_input(INPUT_POST, 'date');
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
        
        define ("DATE_QUERY", "SELECT MAX(`date`) AS mdate FROM country_calculations");
        
        if ($userDate == null) {
            // get results from the date query
            $stmt = $db_conn->prepare(DATE_QUERY);
            $stmt->execute();
            // set the results to associative
            $stmt->setFetchMode(PDO::FETCH_ASSOC);
            if(!$stmt) {
                die("No Date Results");
            }
            // get result
            $row = $stmt->fetch();
            // get date
            $date = $row["mdate"];
        } else {
            $date = $userDate;
        }
        
        define ("TABLE_QUERY", "SELECT country, pc_population, pc_mortality, 
            pc_deaths, pc_active_cases, pc_recovered, pc_total_cases, 
            survival_rate, population,
            population_rank, deaths10k, deaths10k_rank, deaths10k_score, 
            active10k, active10k_rank, active10k_score, recovered10k, 
            recovered10k_rank, recovered10k_score, cases10k, cases10k_rank,
            cases10k_score, `rank`, score 
            FROM country_calculations 
            WHERE `date` = :dateParam
            ORDER BY country;");
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
            echo "<td>" . $row["country"] . "</td>";
            echo "<td>" . number_format(floatval($row["pc_population"]), 2) . "</td>";
            echo "<td>" . number_format(floatval($row["pc_mortality"]), 2) . "</td>";
            echo "<td>" . number_format(floatval($row["pc_deaths"]), 2) . "</td>"; 
            echo "<td>" . number_format(floatval($row["pc_active_cases"]), 2) . "</td>";
            echo "<td>" . number_format(floatval($row["pc_recovered"]), 2) . "</td>";
            echo "<td>" . number_format(floatval($row["pc_total_cases"]), 2) . "</td>";
            echo "<td>" . number_format(floatval($row["survival_rate"]), 2) . "</td>";
            echo "<td>" . number_format(intval($row["population"])) . "</td>";
            echo "<td>" . number_format(intval($row["population_rank"])) . "</td>";
            echo "<td>" . number_format(floatval($row["deaths10k"]), 2) . "</td>";
            echo "<td>" . number_format(intval($row["deaths10k_rank"])) . "</td>";
            echo "<td>" . $row["deaths10k_score"] . "</td>";
            echo "<td>" . number_format(floatval($row["active10k"]), 2) . "</td>";
            echo "<td>" . number_format(intval($row["active10k_rank"])) . "</td>";
            echo "<td>" . $row["active10k_score"] . "</td>";
            echo "<td>" . number_format(floatval($row["recovered10k"]), 2) . "</td>";
            echo "<td>" . number_format(intval($row["recovered10k_rank"])) . "</td>";
            echo "<td>" . $row["recovered10k_score"] . "</td>";
            echo "<td>" . number_format(floatval($row["cases10k"]), 2) . "</td>";
            echo "<td>" . number_format(intval($row["cases10k_rank"])) . "</td>";
            echo "<td>" . $row["cases10k_score"] . "</td>";
            echo "<td>" . number_format(intval($row["rank"])) . "</td>";
            echo "<td>" . $row["score"] . "</td>";
            echo "<td>" . $date . "</td>";
            echo "</tr>";
        }
        // close database connection
        $db_conn = null;
    } catch(PDOException $e) {
      echo "Connection failed: " . $e->getMessage();
    }