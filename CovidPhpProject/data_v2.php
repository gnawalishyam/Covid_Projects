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

        /**
         * Query to retrieve the calculation data from the database for states
         */
        define ("STATE_QUERY" , "SELECT `state`, `date`, `population`, population_usa_rank, 
                pc_of_usa_population, mortality_rate, pc_of_usa_deaths, 
                pc_of_usa_active_cases, pc_of_usa_recovered, 
                pc_of_usa_total_cases, total_cases, new_cases, total_deaths, 
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
            FROM state_json 
            WHERE `date` = (SELECT MAX(`date`) FROM state_json);");
        
        // get results from world query
        $stmt1 = $db_conn->prepare(COUNTRY_QUERY);
        $stmt1->execute();
        // set the resulting array to associative
        $stmt1->setFetchMode(PDO::FETCH_ASSOC);
        $c_rows = $stmt1->fetchAll();
        // get results from state query
        $stmt2 = $db_conn->prepare(STATE_QUERY);
        $stmt2->execute();
        // set the resulting array to associative
        $stmt2->setFetchMode(PDO::FETCH_ASSOC);
        $s_rows = $stmt2->fetchAll();
        // set CORS headers
        header("Access-Control-Allow-Origin: *");
        // set JSON header
        header('Content-Type: application/json');
        // close database connection
        $db_conn = null;
        // begin country loop
        for ($i = 0; $i < count($c_rows); $i++) {
            // set country
            $jsonEntry["country"] = $c_rows[$i]["country"];
            
            // set population
            $jsonEntry["population"] = intval($c_rows[$i]["population"]);
            // set population world rank
            $jsonEntry["populationWorldRank"] = 
                    intval($c_rows[$i]["population_world_rank"]);
            
            // set percent of world population
            $jsonEntry["pcOfWorldPopulation"] = 
                    floatval($c_rows[$i]["pc_of_world_population"]);
            // set percent of world mortality
            $jsonEntry["mortalityRate"] = 
                    floatval($c_rows[$i]["mortality_rate"]);
            // set percent of world deaths
            $jsonEntry["pcOfWorldDeaths"] = floatval($c_rows[$i]["pc_of_world_deaths"]); 
            // set percent of world active cases
            $jsonEntry["pcOfWorldActiveCases"] = 
                    floatval($c_rows[$i]["pc_of_world_active_cases"]);
            // set persent of world recovered
            $jsonEntry["pcOfWorldRecovered"] = floatval($c_rows[$i]["pc_of_world_recovered"]);
            // set percent of world cases
            $jsonEntry["pcOfWorldTotalCases"] = 
                    floatval($c_rows[$i]["pc_of_world_total_cases"]);
            
            // set total cases
            $jsonEntry["totalCases"] = intval($c_rows[$i]["total_cases"]);
            // set new cases
            $jsonEntry["newCases"] = intval($c_rows[$i]["new_cases"]);
            // set total deaths
            $jsonEntry["totalDeaths"] = intval($c_rows[$i]["total_deaths"]);
            // set new deaths
            $jsonEntry["newDeaths"] = intval($c_rows[$i]["new_deaths"]);
            // set total active cases
            $jsonEntry["totalActiveCases"] = intval($c_rows[$i]["total_active_cases"]);
            
            // set deaths per 100k population
            $jsonEntry["totalDeaths100k"] = floatval($c_rows[$i]["total_deaths100k"]);
            // set deaths per 100k population rank
            $jsonEntry["totalDeaths100kRank"] = intval($c_rows[$i]["total_deaths100k_rank"]);
            // set deaths per 100k population score
            $jsonEntry["totalDeaths100kScore"] = intval($c_rows[$i]["total_deaths100k_score"]);
            // set deaths per 100k population score
            $jsonEntry["totalDeaths100kGrade"] = $c_rows[$i]["total_deaths100k_grade"];
            // set active cases per 100k population
            $jsonEntry["totalActiveCases100k"] = floatval($c_rows[$i]["total_active100k"]);
            // set active cases per 100k population rank
            $jsonEntry["totalActiveCases100kRank"] = 
                    intval($c_rows[$i]["total_active100k_rank"]);
            // set active cases per 100k population score
            $jsonEntry["totalActiveCases100kScore"] = intval($c_rows[$i]["total_active100k_score"]);
            // set active cases per 100k population grade
            $jsonEntry["totalActiveCases100kGrade"] = $c_rows[$i]["total_active100k_grade"];
            // set cases per 100k population
            $jsonEntry["totalCases100k"] = 
                    floatval($c_rows[$i]["total_cases100k"]);
            // set cases per 100k population rank
            $jsonEntry["totalCases100kRank"] = 
                    intval($c_rows[$i]["total_cases100k_rank"]);
            // set cases per 100k population score
            $jsonEntry["totalCases100kScore"] = 
                    intval($c_rows[$i]["total_cases100k_score"]);
            // set cases per 100k population grade
            $jsonEntry["totalCases100kGrade"] = 
                $c_rows[$i]["total_cases100k_grade"];
            
            // set new cases per 100k population over last 15 days
            $jsonEntry["newCases100k15Days"] = floatval($c_rows[$i]["new_cases100k_15days"]);
            // set new cases per 100k population over last 15 days rank
            $jsonEntry["newCases100k15DaysRank"] = intval($c_rows[$i]["new_cases100k_15days_rank"]);
            // set new cases per 100k population over last 15 days score
            $jsonEntry["newCases100k15DaysScore"] = intval($c_rows[$i]["new_cases100k_15days_score"]);
            // set new cases per 100k population over last 15 days grade
            $jsonEntry["newCases100k15DaysGrade"] = $c_rows[$i]["new_cases100k_15days_grade"];
            // set new deaths per 100k population over last 15 days
            $jsonEntry["newDeaths100k15Days"] = floatval($c_rows[$i]["new_deaths100k_15days"]);
            // set new deaths per 100k population over last 15 days rank
            $jsonEntry["newDeaths100k15DaysRank"] = intval($c_rows[$i]["new_deaths100k_15days_rank"]);
            // set new deaths per 100k population over last 15 days score
            $jsonEntry["newDeaths100k15DaysScore"] = intval($c_rows[$i]["new_deaths100k_15days_score"]);
            // set new deaths per 100k population over last 15 days grade
            $jsonEntry["newDeaths100k15DaysGrade"] = $c_rows[$i]["new_deaths100k_15days_grade"];
            // set new cases per 100k population over last 30 days
            $jsonEntry["newCases100k305Days"] = floatval($c_rows[$i]["new_cases100k_30days"]);
            // set new cases per 100k population over last 30 days rank
            $jsonEntry["newCases100k30DaysRank"] = intval($c_rows[$i]["new_cases100k_30days_rank"]);
            // set new cases per 100k population over last 30 days score
            $jsonEntry["newCases100k30DaysScore"] = intval($c_rows[$i]["new_cases100k_30days_score"]);
            // set new cases per 100k population over last 30 days grade
            $jsonEntry["newCases100k30DaysGrade"] = $c_rows[$i]["new_cases100k_30days_grade"];
            // set new deaths per 100k population over last 30 days
            $jsonEntry["newDeaths100k30Days"] = floatval($c_rows[$i]["new_deaths100k_30days"]);
            // set new deaths per 100k population over last 30 days rank
            $jsonEntry["newDeaths100k30DaysRank"] = intval($c_rows[$i]["new_deaths100k_30days_rank"]);
            // set new deaths per 100k population over last 30 days score
            $jsonEntry["newDeaths100k30DaysScore"] = intval($c_rows[$i]["new_deaths100k_30days_score"]);
            // set new deaths per 100k population over last 30 days grade
            $jsonEntry["newDeaths100k30DaysGrade"] = $c_rows[$i]["new_deaths100k_30days_grade"];
            
            // set overall rank
            $jsonEntry["overallRank"] = intval($c_rows[$i]["overall_rank"]);
            // set overall score
            $jsonEntry["overallScore"] = intval($c_rows[$i]["overall_score"]);
            // set overall grade
            $jsonEntry["overallGrade"] = $c_rows[$i]["overall_grade"];
            
            // add entry to array
            $jsonArray[$i] = $jsonEntry;
        }
        // add array to countries
        $jsonMaster["countries"] = $jsonArray;
        // begin state loop
        for ($i = 0; $i < count($s_rows); $i++) {
                // set country
                $jsonEntry1["state"] = $s_rows[$i]["state"];
                
                // set population
                $jsonEntry1["population"] = intval($s_rows[$i]["population"]);
                // set population USA rank
                $jsonEntry1["populationUSARank"] = 
                        intval($s_rows[$i]["population_usa_rank"]);
                
                // set percent of USA population
                $jsonEntry1["pcOfUSAPopulation"] = 
                        floatval($s_rows[$i]["pc_of_usa_population"]);
                // set percent of USA mortality
                $jsonEntry1["mortalityRate"] = 
                        floatval($s_rows[$i]["mortality_rate"]);
                // set percent of USA deaths
                $jsonEntry1["pcOfUSADeaths"] = floatval($s_rows[$i]["pc_of_usa_deaths"]); 
                // set percent of USA active cases
                $jsonEntry1["pcOfUSAActiveCases"] = 
                        floatval($s_rows[$i]["pc_of_usa_active_cases"]);
                // set persent of USA recovered
                $jsonEntry1["pcOfUSARecovered"] = floatval($s_rows[$i]["pc_of_usa_recovered"]);
                // set percent of USA cases
                $jsonEntry1["pcOfUSATotalCases"] = 
                        floatval($s_rows[$i]["pc_of_usa_total_cases"]);
                
                // set total cases
                $jsonEntry1["totalCases"] = intval($s_rows[$i]["total_cases"]);
                // set new cases
                $jsonEntry1["newCases"] = intval($s_rows[$i]["new_cases"]);
                // set total deaths
                $jsonEntry1["totalDeaths"] = intval($s_rows[$i]["total_deaths"]);
                // set new deaths
                $jsonEntry1["newDeaths"] = intval($s_rows[$i]["new_deaths"]);
                // set total active cases
                $jsonEntry1["totalActiveCases"] = intval($s_rows[$i]["total_active_cases"]);
                
                // set deaths per 100k population
                $jsonEntry1["totalDeaths100k"] = floatval($s_rows[$i]["total_deaths100k"]);
                // set deaths per 100k population rank
                $jsonEntry1["totalDeaths100kRank"] = intval($s_rows[$i]["total_deaths100k_rank"]);
                // set deaths per 100k population score
                $jsonEntry1["totalDeaths10kScore"] = intval($s_rows[$i]["total_deaths100k_score"]);
                // set deaths per 100k population score
                $jsonEntry1["totalDeaths10kGrade"] = $s_rows[$i]["total_deaths100k_grade"];
                // set active cases per 100k population
                $jsonEntry1["totalActiveCases100k"] = floatval($s_rows[$i]["total_active100k"]);
                // set active cases per 100k population rank
                $jsonEntry1["totalActiveCases100kRank"] = 
                        intval($s_rows[$i]["total_active100k_rank"]);
                // set active cases per 100k population score
                $jsonEntry1["totalActiveCases100kScore"] = intval($s_rows[$i]["total_active100k_score"]);
                // set active cases per 100k population grade
                $jsonEntry1["totalActiveCases100kGrade"] = $s_rows[$i]["total_active100k_grade"];
                // set cases per 100k population
                $jsonEntry1["totalCases100k"] = 
                        floatval($s_rows[$i]["total_cases100k"]);
                // set cases per 100k population rank
                $jsonEntry1["totalCases100kRank"] = 
                        intval($s_rows[$i]["total_cases100k_rank"]);
                // set cases per 100k population score
                $jsonEntry1["totalCases100kScore"] = 
                        intval($s_rows[$i]["total_cases100k_score"]);
                // set cases per 100k population grade
                $jsonEntry1["totalCases100kGrade"] = 
                    $s_rows[$i]["total_cases100k_grade"];
                
                // set new cases per 100k population over last 15 days
                $jsonEntry1["newCases100k15Days"] = floatval($s_rows[$i]["new_cases100k_15days"]);
                // set new cases per 100k population over last 15 days rank
                $jsonEntry1["newCases100k15DaysRank"] = intval($s_rows[$i]["new_cases100k_15days_rank"]);
                // set new cases per 100k population over last 15 days score
                $jsonEntry1["newCases100k15DaysScore"] = intval($s_rows[$i]["new_cases100k_15days_score"]);
                // set new cases per 100k population over last 15 days grade
                $jsonEntry1["newCases100k15DaysGrade"] = $s_rows[$i]["new_cases100k_15days_grade"];
                // set new deaths per 100k population over last 15 days
                $jsonEntry1["newDeaths100k15Days"] = floatval($s_rows[$i]["new_deaths100k_15days"]);
                // set new deaths per 100k population over last 15 days rank
                $jsonEntry1["newDeaths100k15DaysRank"] = intval($s_rows[$i]["new_deaths100k_15days_rank"]);
                // set new deaths per 100k population over last 15 days score
                $jsonEntry1["newDeaths100k15DaysScore"] = intval($s_rows[$i]["new_deaths100k_15days_score"]);
                // set new deaths per 100k population over last 15 days grade
                $jsonEntry1["newDeaths100k15DaysGrade"] = $s_rows[$i]["new_deaths100k_15days_grade"];
                // set new cases per 100k population over last 30 days
                $jsonEntry1["newCases100k305Days"] = floatval($s_rows[$i]["new_cases100k_30days"]);
                // set new cases per 100k population over last 30 days rank
                $jsonEntry1["newCases100k30DaysRank"] = intval($s_rows[$i]["new_cases100k_30days_rank"]);
                // set new cases per 100k population over last 30 days score
                $jsonEntry1["newCases100k30DaysScore"] = intval($s_rows[$i]["new_cases100k_30days_score"]);
                // set new cases per 100k population over last 30 days grade
                $jsonEntry1["newCases100k30DaysGrade"] = $s_rows[$i]["new_cases100k_30days_grade"];
                // set new deaths per 100k population over last 30 days
                $jsonEntry1["newDeaths100k30Days"] = floatval($s_rows[$i]["new_deaths100k_30days"]);
                // set new deaths per 100k population over last 30 days rank
                $jsonEntry1["newDeaths100k30DaysRank"] = intval($s_rows[$i]["new_deaths100k_30days_rank"]);
                // set new deaths per 100k population over last 30 days score
                $jsonEntry1["newDeaths100k30DaysScore"] = intval($s_rows[$i]["new_deaths100k_30days_score"]);
                // set new deaths per 100k population over last 30 days grade
                $jsonEntry1["newDeaths100k30DaysGrade"] = $s_rows[$i]["new_deaths100k_30days_grade"];
                
                // set overall rank
                $jsonEntry1["overallRank"] = intval($s_rows[$i]["overall_rank"]);
                // set overall score
                $jsonEntry1["overallScore"] = intval($s_rows[$i]["overall_score"]);
                // set overall grade
                $jsonEntry1["overallGrade"] = $s_rows[$i]["overall_grade"];
                
                // add entry to array
                $jsonStateArray[$i] = $jsonEntry1;
            }
            // add array to countries
            $jsonMaster["states"] = $jsonStateArray;
        // encode json array
        $json = json_encode($jsonMaster, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
        //$json = json_encode($worldRow, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
        /* Return the JSON string. */
        echo $json;
    } catch(PDOException $e) {
      echo "Connection failed: " . $e->getMessage();
    }
