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
    
        /**
         * Query to retrieve the calculation data from the database
         */
        define ("CALCULATION_QUERY" , "SELECT country, pc_population, 
            pc_mortality, pc_deaths, pc_active_cases, pc_recovered, 
            pc_total_cases, population_rank, deaths10k, deaths10k_rank,
            deaths10k_score, active10k, active10k_rank, active10k_score,
            recovered10k, recovered10k_rank, recovered10k_score, cases10k,
            cases10k_rank, cases10k_score, rank, score, population 
            FROM country_calculations 
            WHERE `date` = (SELECT MAX(`date`) FROM country_calculations);");
        
        // get results from world query
        $stmt = $db_conn->prepare(CALCULATION_QUERY);
        $stmt->execute();
        // set the resulting array to associative
        $stmt->setFetchMode(PDO::FETCH_ASSOC);
        $rows = $stmt->fetchAll();
        // set JSON header
        header('Content-Type: application/json');
        // close database connection
        $db_conn = null;
        // begin country loop
        for ($i = 0; $i < count($rows); $i++) {
            // set country
            $jsonEntry["country"] = $rows[$i]["country"];
            // set percent of world population
            $jsonEntry["pcOfWorldPopulation"] = 
                    floatval($rows[$i]["pc_population"]);
            // set percent of world mortality
            $jsonEntry["pcOfWorldMortality"] = 
                    floatval($rows[$i]["pc_mortality"]);
            // set percent of world deaths
            $jsonEntry["pcOfWorldDeaths"] = floatval($rows[$i]["pc_deaths"]); 
            // set percent of world active cases
            $jsonEntry["pcOfActiveCases"] = 
                    floatval($rows[$i]["pc_active_cases"]);
            // set persent of world recovered
            $jsonEntry["pcOfRecovered"] = floatval($rows[$i]["pc_recovered"]);
            // set percent of world cases
            $jsonEntry["pcOfTotalCases"] = 
                    floatval($rows[$i]["pc_total_cases"]);
            // set population
            $jsonEntry["population"] = intval($rows[$i]["population"]);
            // set population world rank
            $jsonEntry["populationWorldRank"] = 
                    intval($rows[$i]["population_rank"]);
            // set deaths per 10k population
            $jsonEntry["deaths10k"] = floatval($rows[$i]["deaths10k"]);
            // set deaths per 10k population rank
            $jsonEntry["deaths10kRank"] = intval($rows[$i]["deaths10k_rank"]);
            // set deaths per 10k population score
            $jsonEntry["deaths10kScore"] = $rows[$i]["deaths10k_score"];
            // set active cases per 10k population
            $jsonEntry["activeCases10k"] = floatval($rows[$i]["active10k"]);
            // set active cases per 10k population rank
            $jsonEntry["activeCases10kRank"] = 
                    intval($rows[$i]["active10k_rank"]);
            // set active cases per 10k population score
            $jsonEntry["activeCases10kScore"] = $rows[$i]["active10k_score"];
            // set recovered per 10k population
            $jsonEntry["recovered10k"] = floatval($rows[$i]["recovered10k"]);
            // set recovered per 10k population rank
            $jsonEntry["recovered10kRank"] = 
                    intval($rows[$i]["recovered10k_rank"]);
            // set recovered per 10k population score
            $jsonEntry["recovered10kScore"] = $rows[$i]["recovered10k_score"];
            // set cases per 10k population
            $jsonEntry["totalCases10k"] = 
                    floatval($rows[$i]["cases10k"]);
            // set cases per 10k population rank
            $jsonEntry["totalCases10kRank"] = 
                    intval($rows[$i]["cases10k_rank"]);
            // set cases per 10k population score
            $jsonEntry["totalCases10kScore"] = 
                    $rows[$i]["cases10k_score"];
            $jsonEntry["rank"] = intval($rows[$i]["rank"]);
            $jsonEntry["score"] = $rows[$i]["score"];
            // add entry to array
            $jsonArray[$i] = $jsonEntry;
        }
        // encode json array
        $json = json_encode($jsonArray, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
        //$json = json_encode($worldRow, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
        /* Return the JSON string. */
        echo $json;
    } catch(PDOException $e) {
      echo "Connection failed: " . $e->getMessage();
    }