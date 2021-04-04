<?php

/*
 * The MIT License
 *
 * Copyright 2021 Gary Larson <gary@thalic.mobi>.
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

    define ("DATE_QUERY", "SELECT MAX(`date`) AS mdate FROM country_json");

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

    /**
    * Query to retrieve the calculation data from the database for countries
    */
   define ("COUNTRY_QUERY" , "SELECT country, `date`, `population`, population_world_rank, 
           pc_of_world_population, mortality_rate, pc_of_world_deaths, 
           pc_of_world_active_cases, pc_of_world_recovered, 
           pc_of_world_total_cases, total_cases, new_cases, total_deaths, 
           new_deaths, total_active_cases, total_deaths100k, 
           total_deaths100k_rank, total_deaths100k_score, 
           total_deaths100k_grade, total_active100k, total_active100k_rank, 
           total_active100k_score, total_active100k_grade, 
           total_cases100k, total_cases100k_rank, total_cases100k_score, 
           total_cases100k_grade, new_cases100k_15days, 
           new_cases100k_15days_rank, new_cases100k_15days_score, 
           new_cases100k_15days_grade, new_deaths100k_15days, 
           new_deaths100k_15days_rank, new_deaths100k_15days_score,
           new_deaths100k_15days_grade, new_cases100k_30days, 
           new_cases100k_30days_rank, new_cases100k_30days_score, 
           new_cases100k_30days_grade, new_deaths100k_30days, 
           new_deaths100k_30days_rank, new_deaths100k_30days_score, 
           new_deaths100k_30days_grade, overall_rank, overall_score, 
           overall_grade
       FROM country_json 
       WHERE `date` = (SELECT MAX(`date`) FROM country_json);");

    // get results from the table query
    $stmt = $db_conn->prepare(COUNTRY_QUERY);
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
        echo "<td>" . $date . "</td>";
        echo "<td>" . number_format(intval($row["population"])) . "</td>";
        echo "<td>" . number_format(intval($row["population_world_rank"])) . "</td>";
        echo "<td>" . number_format(floatval($row["pc_of_world_population"]), 5) . "</td>"; 
        echo "<td>" . number_format(floatval($row["mortality_rate"]), 5) . "</td>";
        echo "<td>" . number_format(floatval($row["pc_of_world_deaths"]), 5) . "</td>"; 
        echo "<td>" . number_format(floatval($row["pc_of_world_active_cases"]), 5) . "</td>";
        echo "<td>" . number_format(floatval($row["pc_of_world_recovered"]), 5) . "</td>";
        echo "<td>" . number_format(floatval($row["pc_of_world_total_cases"]), 5) . "</td>";
        echo "<td>" . number_format(intval($row["total_cases"])) . "</td>";
        echo "<td>" . number_format(intval($row["new_cases"])) . "</td>";
        echo "<td>" . number_format(intval($row["total_deaths"])) . "</td>";
        echo "<td>" . number_format(intval($row["new_deaths"])) . "</td>";
        echo "<td>" . number_format(intval($row["total_active_cases"])) . "</td>";
        echo "<td>" . number_format(floatval($row["total_deaths100k"]), 5) . "</td>";
        echo "<td>" . number_format(intval($row["total_deaths100k_rank"])) . "</td>";
        echo "<td>" . number_format(intval($row["total_deaths100k_score"])) . "</td>";
        echo "<td>" . $row["total_deaths100k_grade"] . "</td>";
        echo "<td>" . number_format(floatval($row["total_active100k"]), 5) . "</td>";
        echo "<td>" . number_format(intval($row["total_active100k_rank"])) . "</td>";
        echo "<td>" . number_format(intval($row["total_active100k_score"])) . "</td>";
        echo "<td>" . $row["total_active100k_grade"] . "</td>";
        echo "<td>" . number_format(floatval($row["total_cases100k"]), 5) . "</td>";
        echo "<td>" . number_format(intval($row["total_cases100k_rank"])) . "</td>";
        echo "<td>" . number_format(intval($row["total_cases100k_score"])) . "</td>";
        echo "<td>" . $row["total_cases100k_grade"] . "</td>";
        echo "<td>" . number_format(floatval($row["new_cases100k_15days"]), 5) . "</td>";
        echo "<td>" . number_format(intval($row["new_cases100k_15days_rank"])) . "</td>";
        echo "<td>" . number_format(intval($row["new_cases100k_15days_score"])) . "</td>";
        echo "<td>" . $row["new_cases100k_15days_grade"] . "</td>";
        echo "<td>" . number_format(floatval($row["new_deaths100k_15days"]), 5) . "</td>";
        echo "<td>" . number_format(intval($row["new_deaths100k_15days_rank"])) . "</td>";
        echo "<td>" . number_format(intval($row["new_deaths100k_15days_score"])) . "</td>";
        echo "<td>" . $row["new_deaths100k_15days_grade"] . "</td>";
        echo "<td>" . number_format(floatval($row["new_cases100k_30days"]), 5) . "</td>";
        echo "<td>" . number_format(intval($row["new_cases100k_30days_rank"])) . "</td>";
        echo "<td>" . number_format(intval($row["new_cases100k_30days_score"])) . "</td>";
        echo "<td>" . $row["new_cases100k_30days_grade"] . "</td>";
        echo "<td>" . number_format(floatval($row["new_deaths100k_30days"]), 5) . "</td>";
        echo "<td>" . number_format(intval($row["new_deaths100k_30days_rank"])) . "</td>";
        echo "<td>" . number_format(intval($row["new_deaths100k_30days_score"])) . "</td>";
        echo "<td>" . $row["new_deaths100k_30days_grade"] . "</td>";
        echo "<td>" . number_format(intval($row["overall_rank"])) . "</td>";
        echo "<td>" . number_format(intval($row["overall_score"])) . "</td>";
        echo "<td>" . $row["overall_grade"] . "</td>";
        echo "</tr>";
    }
    // close database connection
    $db_conn = null;
} catch(PDOException $e) {
  echo "Connection failed: " . $e->getMessage();
}