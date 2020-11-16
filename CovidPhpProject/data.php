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
    
    /**
     * function to Create Ranks of index
     * @param type $rows an array of data to be ranked
     * @return array with the ranks
     */
    function createRanks($rows) : array {
        for ($i = 0; $i < count($rows); $i++) {
            $ranks[$rows[$i]["country"]] = $i + 1;
        }
        return $ranks;
    }
    
    /**
     * function to Create Ranks Ascending
     * @param type $rows an array of data to be ranked
     * @param type $stringIndex to access elements in the array
     * @return array with the ranks
     */
    function createRanksAsc($rows, $stringIndex) : array {
        $rank = 0;
        for ($i = 0; $i < count($rows); $i++) {
            if ($rows[$i][$stringIndex] > 0) {
                if ($rank == 0) {
                    $rank = 1;
                    $ranks[$rank] = $rows[$i][$stringIndex];
                } else if ($rows[$i][$stringIndex] > $ranks[$rank]) {
                    $rank++;
                    $ranks[$rank] = $rows[$i][$stringIndex];
                }
            }
        }
        return $ranks;
    }
    
    /**
     * function to Create Ranks Descending
     * @param type $rows an array of data to be ranked
     * @param type $stringIndex to access elements in the array
     * @return array with the ranks
     */
    function createRanksDesc($rows, $stringIndex) : array {
        $rank = 0;
        for ($i = 0; $i < count($rows); $i++) {
            if ($rows[$i][$stringIndex] > 0) {
                if ($rank == 0) {
                    $rank = 1;
                    $ranks[$rank] = $rows[$i][$stringIndex];
                } else if ($rows[$i][$stringIndex] < $ranks[$rank]) {
                    $rank++;
                    $ranks[$rank] = $rows[$i][$stringIndex];
                }
            }
        }
        return $ranks;
    }
    
    /**
     * function to Calculates the grade values from the ranks
     * @param type $caseRank rank to calculate
     * @return array of medians
     */
    function calculateMedians($caseRank) : array {
        // Get given ranks A+ and F
        $median["AP"] = 1;
        $median["F"] = count($caseRank);
        // calculate C+
        $median["CP"] = ($median["AP"] + $median["F"]) / 2;
        // calculate B+
        $median["BP"] = ($median["AP"] + $median["CP"]) / 2;
        // calculate D+
        $median["DP"] = ($median["CP"] + $median["F"]) / 2;
        // calculate temporary medians TA, TB, TC, TD
        $median["TA"] = ($median["AP"] + $median["BP"]) / 2;
        $median["TB"] = ($median["BP"] + $median["CP"]) / 2;
        $median["TC"] = ($median["CP"] + $median["DP"]) / 2;
        $median["TD"] = ($median["DP"] + $median["F"]) / 2;
        // calculate A
        $median["A"] = ($median["AP"] + $median["TA"]) / 2;
        // calculate AM
        $median["AM"] = ($median["TA"] + $median["BP"]) / 2;
        // calculate B
        $median["B"] = ($median["BP"] + $median["TB"]) / 2;
        // calculate BM
        $median["BM"] = ($median["TB"] + $median["CP"]) / 2;
        // calculate C
        $median["C"] = ($median["CP"] + $median["TC"]) / 2;
        // calculate CM
        $median["CM"] = ($median["TC"] + $median["DP"]) / 2;
        // calculate D
        $median["D"] = ($median["DP"] + $median["TD"]) / 2;
        // calculate DM
        $median["DM"] = ($median["TD"] + $median["F"]) / 2;
        return $median;
    }
    
    /**
     * function to assign rank designation
     * @param type $caseRank rank to assign
     * @return array of final ranks
     */
    function assignRanks ($caseRank) : array {
        // calculate medians
        $median = calculateMedians($caseRank);
        // assign AP
        $item["AP"] = $caseRank[$median["AP"]];
        // assign F
        $item["F"] = $caseRank[$median["F"]];
        // assign C+
        $item["CP"] = $caseRank[$median["CP"]];
        // assign B+
        $item["BP"] = $caseRank[$median["BP"]];
        // assign D+
        $item["DP"] = $caseRank[$median["DP"]];
        // assign A
        $item["A"] = $caseRank[$median["A"]];
        // assign AM
        $item["AM"] = $caseRank[$median["AM"]];
        // assign B
        $item["B"] = $caseRank[$median["B"]];
        // assign BM
        $item["BM"] = $caseRank[$median["BM"]];
        // assign C
        $item["C"] = $caseRank[$median["C"]];
        // assign CM
        $item["CM"] = $caseRank[$median["CM"]];
        // assign D
        $item["D"] = $caseRank[$median["D"]];
        // assign DM
        $item["DM"] = $caseRank[$median["DM"]];
        return $item;
    }
    
    /**
     * function to convert rows from number based to associative
     * @param type $rows to convert
     * @param type $stringIndex to access entries
     * @return array of converted data
     */
    function convertRows($rows, $stringIndex) : array {
        $array = [];
        foreach($rows as $row) {
            $array[$row["country"]] = $row[$stringIndex];
        }
        return $array;
    }
    
    /**
     * function to convert cases, deaths, active_cases and recovered to 
     * population 10k
     * @param type $worldRow array of world data
     * @return array of population 10k world data
     */
    function createWorld10k($worldRow) : array {
        $world10k["cases"] = $worldRow["cases"] / $worldRow["population"] * 10000;
        $world10k["deaths"] = $worldRow["deaths"] / $worldRow["population"] * 10000;
        $world10k["active"] = $worldRow["cases"] / $worldRow["population"] * 10000;
        $world10k["recovered"] = $worldRow["recovered"] / $worldRow["population"] * 10000;
        return $world10k;
    }
    
    /**
     * function to get the score from the ranks and amount
     * @param type $ranks
     * @param type $amount
     * @return string
     */
    function get10kRankScore($ranks, $amount) : string {
        if ($ranks["AP"] < $ranks["F"]) {
           return get10kRankScoreAsc($ranks, $amount);
        } else {
           return get10kRankScoreDesc($ranks, $amount);
        }
    }
    
    /**
     * function to get the score from the ranks and amount
     * @param type $ranks
     * @param type $amount
     * @return string
     */
    function get10kRankScoreDesc($ranks, $amount) : string {
        if ($amount > $ranks["A"]) {return "A+";}
        if ($amount > $ranks["AM"]) {return "A";}
        if ($amount > $ranks["BP"]) {return "A-";}
        if ($amount > $ranks["B"]) {return "B+";}
        if ($amount > $ranks["BM"]) {return "B";}
        if ($amount > $ranks["CP"]) {return "B-";}
        if ($amount > $ranks["C"]) {return "C+";}
        if ($amount > $ranks["CM"]) {return "C";}
        if ($amount > $ranks["DP"]) {return "C-";}
        if ($amount > $ranks["D"]) {return "D+";}
        if ($amount > $ranks["DM"]) {return "D";}
        if ($amount > $ranks["F"]) {return "D-";}
        return "F";
    }
    
    /**
     * function t0 get the score from the ranks and amount
     * @param type $ranks
     * @param type $amount
     * @return string
     */
    function get10kRankScoreAsc($ranks, $amount) : string {
        if ($amount < $ranks["A"]) {return "A+";}
        if ($amount < $ranks["AM"]) {return "A";}
        if ($amount < $ranks["BP"]) {return "A-";}
        if ($amount < $ranks["B"]) {return "B+";}
        if ($amount < $ranks["BM"]) {return "B";}
        if ($amount < $ranks["CP"]) {return "B-";}
        if ($amount < $ranks["C"]) {return "C+";}
        if ($amount < $ranks["CM"]) {return "C";}
        if ($amount < $ranks["DP"]) {return "C-";}
        if ($amount < $ranks["D"]) {return "D+";}
        if ($amount < $ranks["DM"]) {return "D";}
        if ($amount < $ranks["F"]) {return "D-";}
        return "F"; 
    }
    
    /**
     * function to convert a letter score to a number rank
     * @param type $score to convert
     * @return float rank
     */
    function getScoreToRank($score) : float {
        if ($score == "A+") {return 4.33;}
        if ($score == "A") {return 4.00;}
        if ($score == "A-") {return 3.67;}
        if ($score == "B+") {return 3.33;}
        if ($score == "B") {return 3.00;}
        if ($score == "B-") {return 2.67;}
        if ($score == "C+") {return 2.33;}
        if ($score == "C") {return 2.00;}
        if ($score == "C-") {return 1.67;}
        if ($score == "D+") {return 1.33;}
        if ($score == "D") {return 1.00;}
        if ($score == "D-") {return 0.67;}
        return 0;
    }
    
    /**
     * function to convert a number rank to a letter score
     * @param type $rank to convert
     * @return string score
     */
    function getRankToScore($rank) : string {
        if ($rank > 4.00) {return "A+";}
        if ($rank > 3.67) {return "A";}
        if ($rank > 3.33) {return "A-";}
        if ($rank > 3.00) {return "B+";}
        if ($rank > 2.67) {return "B";}
        if ($rank > 2.33) {return "B-";}
        if ($rank > 2.00) {return "C+";}
        if ($rank > 1.67) {return "C";}
        if ($rank > 1.33) {return "C-";}
        if ($rank > 1.00) {return "D+";}
        if ($rank > 0.67) {return "D";}
        if ($rank > 0.00) {return "D-";}
        return "F";
    }
    
    function  makeColumnIntegers ($rows, $stringIndex) : array {
        $array = [];
        for($i = 0; $i < count($rows); $i++) {
            $array[$i]["country"] = $rows[$i]["country"];
            $array[$i][$stringIndex] =  intval($rows[$i][$stringIndex]);
        }
        return $array;
    }
    
    function  makeColumnReals ($rows, $stringIndex) : array {
        $array = [];
        for($i = 0; $i < count($rows); $i++) {
            $array[$i]["country"] = $rows[$i]["country"];
            $array[$i][$stringIndex] =  floatval($rows[$i][$stringIndex]);
        }
        return $array;
    }

    // create connection string
    $servername = "550dae4.online-server.cloud";
    $username = "web_php";
    $password = 'nz3Rp"3XZL=2v4.Q';
    $database = "covid";
    

    try {
        // open connection to database
        $db_conn = new PDO("mysql:host=$servername;dbname=$database;charset=utf8", $username, $password);
        // set the PDO error mode to exception
        $db_conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
   
        /**
         * Query to retrieve the world data from the database for computations
         */
        define ("WORLD_QUERY" , "SELECT cases, deaths, 
                    active, recovered, population, date, mortality
                    FROM latest_world_totals");
        /**
         * Query to retrieve case data from the database
         */
        define ("CASE_QUERY", "SELECT country, cases FROM latest_country_cases");
        /**
         * Query to retrieve death data from the database
         */
        define ("DEATH_QUERY", "SELECT country, deaths FROM latest_country_deaths");
        /**
         * Query to retrieve active cases data from the database
         */
        define ("ACTIVE_QUERY", 
                "SELECT country, active FROM latest_country_active");
        /**
         * Query to retrieve recovered data from the database
         */
        define ("RECOVERED_QUERY", 
                "SELECT country, recovered FROM latest_country_recovered");
        /**
         * Query to retrieve case data for population 10k from the database
         */
        define ("CASE10K_QUERY", 
                "SELECT country, cases FROM latest_country_cases10k");
        /**
         * Query to retrieve death data for population 10k from the database
         */
        define ("DEATH10K_QUERY", 
                "SELECT country, deaths FROM latest_country_deaths10k");
        /**
         * Query to retrieve active cases data for population 10k from the database
         */
        define ("ACTIVE10K_QUERY", 
                "SELECT country, active FROM latest_country_active10k");
        /**
         * Query to retrieve recovered data for population 10k from the database
         */
        define ("RECOVERED10K_QUERY", 
                "SELECT country, recovered FROM latest_country_recovered10k");
        /**
         * Query to retrieve population data from the database
         */
        define ("POPULATION_QUERY", 
                "SELECT country, population FROM country_populations");
        /**
         * Query to retrieve mortality data from the database
         */
        define("MORTALITY_QUERY", 
                "SELECT country, mortality FROM latest_country_mortality");



        // get results from world query
        $stmt = $db_conn->prepare(WORLD_QUERY);
        $stmt->execute();
        // set the resulting array to associative
        $stmt->setFetchMode(PDO::FETCH_ASSOC);
        $worldRow = $stmt->fetch();
        if (!$worldRow) {
            die("No World Results");
        }
        $worldRow["cases"] = intval($worldRow["cases"]);
        $worldRow["deaths"] = intval($worldRow["deaths"]);
        $worldRow["active"] = intval($worldRow["active"]);
        $worldRow["recovered"] = intval($worldRow["recovered"]);
        $worldRow["population"] = intval($worldRow["population"]);
        $worldRow["mortality"] = floatval($worldRow["mortality"]);
        // get results from the case query
        $stmt = $db_conn->prepare(CASE_QUERY);
        $stmt->execute();
        // set the resulting array to associative
        $stmt->setFetchMode(PDO::FETCH_ASSOC);
        $caseResults = $stmt->fetchAll();
        $caseRows = makeColumnIntegers($caseResults, "cases");
        if (!$caseRows) {
           die("No Case Results");
        }
        // get results from the death query
        $stmt = $db_conn->prepare(DEATH_QUERY);
        $stmt->execute();
        // set the resulting array to associative
        $stmt->setFetchMode(PDO::FETCH_ASSOC);
        $deathResults = $stmt->fetchAll();
        $deathRows = makeColumnIntegers($deathResults, "deaths");
        if (!$deathRows) {
            die("No Death Results");
        }
        // get results from the active query
        $stmt = $db_conn->prepare(ACTIVE_QUERY);
        $stmt->execute();
        // set the resulting array to associative
        $stmt->setFetchMode(PDO::FETCH_ASSOC);
        $activeResults = $stmt->fetchAll();
        $activeRows = makeColumnIntegers($activeResults, "active");
        if (!$activeRows) {
            die("No Active Case Results");
        }
        // get results from the recovered query
        $stmt = $db_conn->prepare(RECOVERED_QUERY);
        $stmt->execute();
        // set the resulting array to associative
        $stmt->setFetchMode(PDO::FETCH_ASSOC);
        $recoveredResults = $stmt->fetchAll();
        $recoveredRows = makeColumnIntegers($recoveredResults, "recovered");
        if (!$recoveredRows) {
            die("No Recovered Results");
        }
        // get results from the case10k query
        $stmt = $db_conn->prepare(CASE10K_QUERY);
        $stmt->execute();
        // set the resulting array to associative
        $stmt->setFetchMode(PDO::FETCH_ASSOC);
        $case10kResults = $stmt->fetchAll();
        $case10kRows = makeColumnReals($case10kResults, "cases");
        if (!$case10kRows) {
            die("No Case10k Results");
        }
        // get results from the death10k query
        $stmt = $db_conn->prepare(DEATH10K_QUERY);
        $stmt->execute();
        // set the resulting array to associative
        $stmt->setFetchMode(PDO::FETCH_ASSOC);
        $death10kResults = $stmt->fetchAll();
        $death10kRows = makeColumnReals($death10kResults, "deaths");
        if (!$death10kRows) {
            die("No Death10k Results");
        }
        // get results from the active10k query
        $stmt = $db_conn->prepare(ACTIVE10K_QUERY);
        $stmt->execute();
        // set the resulting array to associative
        $stmt->setFetchMode(PDO::FETCH_ASSOC);
        $active10kResults = $stmt->fetchAll();
        $active10kRows = makeColumnReals($active10kResults, "active");
        if (!$active10kRows) {
            die("No Active10k Case Results");
        }
        // get results from the recovered10k query
        $stmt = $db_conn->prepare(RECOVERED10K_QUERY);
        $stmt->execute();
        // set the resulting array to associative
        $stmt->setFetchMode(PDO::FETCH_ASSOC);
        $recovered10kResults = $stmt->fetchAll();
        $recovered10kRows = makeColumnReals($recovered10kResults, "recovered");
        if (!$recovered10kRows) {
            die("No Recovered10k Results");
        }
        // get results from the mortality query
        $stmt = $db_conn->prepare(MORTALITY_QUERY);
        $stmt->execute();
        // set the resulting array to associative
        $stmt->setFetchMode(PDO::FETCH_ASSOC);
        $mortalityRequests = $stmt->fetchAll();
        $mortalityRows = makeColumnReals($mortalityRequests, "mortality");
        if (!$mortalityRows) {
            die("No Mortality Results");
        }
        // get results from the population query
        $stmt = $db_conn->prepare(POPULATION_QUERY);
        $stmt->execute();
        // set the resulting array to associative
        $stmt->setFetchMode(PDO::FETCH_ASSOC);
        $populationRequests = $stmt->fetchAll();
        $populationRows = makeColumnIntegers($populationRequests, "population");
        if (!$populationRows) {
            die("No Population Results");
        }
        // set JSON header
        header('Content-Type: application/json');
        // close database connection
        $db_conn = null;
        // create population associative array
        $populations = convertRows($populationRows, "population");
        // create ranks for populations
        $populationRanks = createRanks($populationRows);
        // create motality associative array
        $mortalities = convertRows($mortalityRows, "mortality");
        // create death associative array
        $deaths = convertRows($deathRows, "deaths");
        // create case associative array
        $cases = convertRows($caseRows, "cases");
        // create active case associative array
        $actives = convertRows($activeRows, "active");
        // create recovered associative array
        $recoveries = convertRows($recoveredRows, "recovered");
        // convert world values to 10k population
        $world10K = createWorld10k($worldRow);
        // create case ranks
        $caseRank = createRanksAsc($case10kRows, "cases");
        $caseRanks = assignRanks($caseRank);
        // create case1 0k associative array
        $case10k = convertRows($case10kRows, "cases");
        // create case 10k ranks
        $case10kRank = createRanks($case10kRows);
        // create death ranks
        $deathRank = createRanksAsc($death10kRows, "deaths");
        $deathRanks = assignRanks($deathRank);
        // create death 10k associative array
        $death10k = convertRows($death10kRows, "deaths");
        // create death 10k ranks
        $death10kRank = createRanks($death10kRows);
        // create active case ranks
        $activeRank = createRanksAsc($active10kRows, "active");
        $activeRanks = assignRanks($activeRank);
        // create active 10k associative array
        $active10k = convertRows($active10kRows, "active");
        // create active 10k ranks
        $active10kRank = createRanks($active10kRows);
        // create recovered case ranks
        $recoveredRank = createRanksDesc($recovered10kRows, "recovered");
        $recoveredRanks = assignRanks($recoveredRank);
        // create recovered 10k associative array
        $recovered10k = convertRows($recovered10kRows, "recovered");
        // create recovered 10k ranks
        $recovered10kRank = createRanks($recovered10kRows);
        // begin country loop
        for ($i = 0; $i < count($populationRows); $i++) {
            // set country
            $country = $populationRows[$i]["country"];
            $jsonEntry["country"] = $country;
            // set percent of world population
            $jsonEntry["pcOfWorldPopulation"] = $populationRows[$i]["population"] / 
                    $worldRow["population"] *100;
            // set percent of world mortality
            $jsonEntry["pcOfWorldMortality"] = ($mortalities[$country] * 
                    ($cases[$country] / $populationRows[$i]["population"])) / 
                    ($worldRow["mortality"]) 
                    * 100;
            // set percent of world deaths
            $jsonEntry["pcOfWorldDeaths"] = $deaths[$country] / 
                    $worldRow["deaths"] * 100;
            // set percent of world active cases
            $jsonEntry["pcOfActiveCases"] = $actives[$country] / 
                    $worldRow["active"] * 100;
            // set persent of world recovered
            $jsonEntry["pcOfRecovered"] = $recoveries[$country] / 
                    $worldRow["recovered"] * 100;
            // set percent of world cases
            $jsonEntry["pcOfTotalCases"] = $cases[$country] / 
                    $worldRow["cases"] * 100;
            // set population
            $jsonEntry["population"] = $populations[$country];
            // set population world rank
            $jsonEntry["populationWorldRank"] = $populationRanks[$country];
            // set deaths per 10k population
            $jsonEntry["deaths10k"] = $death10k[$country];
            $deathsRank = $death10kRank[$country];
            // set deaths per 10k population rank
            $jsonEntry["deaths10kRank"] = $deathsRank;
            $deathsScore = get10kRankScore($deathRanks, $death10k[$country]);
            // set deaths per 10k population score
            $jsonEntry["deaths10kScore"] = $deathsScore;
            // set active cases per 10k population
            $jsonEntry["activeCases10k"] = $active10k[$country];
            $activesRank = $active10kRank[$country];
            // set active cases per 10k population rank
            $jsonEntry["activeCases10kRank"] = $activesRank;
            $activesScore = get10kRankScore($activeRanks, $active10k[$country]);
            // set active cases per 10k population score
            $jsonEntry["activeCases10kScore"] = $activesScore;
            // set recovered per 10k population
            $jsonEntry["recovered10k"] = $recovered10k[$country];
            $recoveriesRank = $recovered10kRank[$country];
            // set recovered per 10k population rank
            $jsonEntry["recovered10kRank"] = $recoveriesRank;
            $recoveriesScore = get10kRankScore($recoveredRanks, $recovered10k[$country]);
            // set recovered per 10k population score
            $jsonEntry["recovered10kScore"] = $recoveriesScore;
            // set cases per 10k population
            $jsonEntry["totalCases10k"] = $case10k[$country];
            $casesRank = $case10kRank[$country];
            // set cases per 10k population rank
            $jsonEntry["totalCases10kRank"] = $casesRank;
            $casesScore = get10kRankScore($caseRanks, $case10k[$country]);
            // set cases per 10k population score
            $jsonEntry["totalCases10kScore"] = $casesScore;
            $jsonEntry["rank"] = ($deathsRank + $activesRank + $casesRank + 
                    $recoveriesRank) / 4;
            $jsonEntry["score"] = getRankToScore((getScoreToRank($deathsScore) +
                    getScoreToRank($activesScore) + getScoreToRank($casesScore) +
                    getScoreToRank($recoveriesScore)) / 4);
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