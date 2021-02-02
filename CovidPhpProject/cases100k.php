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

function calculate_median($arr) {
    $count = count($arr); //total numbers in array
    $middleval = floor(($count-1)/2); // find the middle value, or the lowest middle value
    if($count % 2) { // odd number, middle is the median
        $median = $arr[$middleval];
    } else { // even number, calculate avg of 2 medians
        $low = $arr[$middleval];
        $high = $arr[$middleval+1];
        $median = (($low+$high)/2);
    }
    return $median;
}

function calculate_avg_distance($arr) {
    $total_distance = 0;
    $num_distances = 0;
    for ($i = 0; $i < count($arr) - 2; $i++) {
        $total_distance += $arr[$i + 1] - $arr[$i];
        $num_distances++;
    }
    if ($num_distances === 0) {
        return 0;
    }
    return $total_distance / $num_distances;
}

function standard_deviation($aValues, $bSample = false)
{
    $fMean = array_sum($aValues) / count($aValues);
    $fVariance = 0.0;
    foreach ($aValues as $i)
    {
        $fVariance += pow($i - $fMean, 2);
    }
    $fVariance /= ( $bSample ? count($aValues) - 1 : count($aValues) );
    return (float) sqrt($fVariance);
}

function distribute_values($median, $rowArray, $isHigh) {
    $highest = max($rowArray) + 0.1;
    $medianSum = floatval($median);
    if (!$isHigh) {
        $medianValues[0] = 0;
        $medians["0"] = 0;
        $count = 1;
    } else {
        $count = 0;
    }
    while ($medianSum < $rowArray[count($rowArray) - 1] - $median) {
        $medianValues[$count] = $medianSum;
        $medians["$medianSum"] = 0;
        $medianSum += $median;
        $count++;
    }
    $medians["$highest"] = 0;
    
    return casesLoop($rowArray, $medians, $medianValues, $highest);
}

function casesLoop ($rowArray, $medians, $medianValues, $highest) {
    // begin cases loop
    for ($i = 0; $i < count($rowArray); $i++) {
        $isPlaced = false;
        if (floatval($rowArray[$i]) === 0.0) {
            $medians["0"]++;
            continue;
        }
        for ($j = 0; $j < count($medianValues); $j++) {
           if (floatval($rowArray[$i]) <= $medianValues[$j]) {
               $medians["$medianValues[$j]"]++;
               $isPlaced = true;
               break;
           }
        }
        if (!$isPlaced) {
           $medians["$highest"]++;
        }
    }
    return $medians;
}

function splitLargest($values, $counts, $original) {
    $highest = max($original) + 0.1;
    $largest = 0;
    $current = 0;
    $low = 0;
    $high = 0;
    for ($i = 1; $i < count($counts); $i++) {
        if ($counts[$i] > $largest) {
            $largest = $counts[$i];
            $low = $current;
            $high = $values[$i];
        }
        $current = $values[$i];
    }
    
    $temp["0"] = 0;
    $medianValues = Array();
    split($values, $medianValues, $temp, $low, $high);
    $temp["$highest"] = 0;
    
    return casesLoop($original, $temp, $medianValues, $highest);
}

function split($values, &$medianValues, &$temp, $low, $high) {
    $current = 0;
    $count = 0;
    for ($i = 1; $i < count($values); $i++) {
        if ($current == $low) {
            $new = ($low + $high) / 2;
            $temp["$new"] = 0;
            $medianValues[$count] = $new;
            $count++;
            $temp["$high"] = 0;
            $medianValues[$count] = $high;
            $count++;
            $current = $high;
        } else if ($values[$i] != 0) {
            $temp["$values[$i]"] = 0;
            $medianValues[$count] = $values[$i];
            $count++;
            $current = $values[$i];
        }
    }
}

function combineSmallest($values, $counts, $original) {
    $highest = max($original) + 0.1;
    $smallest = 219;
    $current = 0;
    $low = 0;
    $high = 0;
    for ($i = 1; $i < count($counts) - 2; $i++) {
        if ($counts[$i] + $counts[$i + 1] < $smallest) {
            $smallest = $counts[$i] + $counts[$i + 1];
            $low = $current;
            $high = $values[$i + 1];
        }
        $current = $values[$i];
    }
    $temp["0"] = 0;
    $medianValues = Array();
    combine($values, $medianValues, $temp, $low, $high);
    $temp["$highest"] = 0;
    return casesLoop($original, $temp, $medianValues, $highest);
}

function combine($values, &$medianValues, &$temp, $low, $high) {
    $current = 0;
    $count = 0;
    for ($i = 1; $i < count($values); $i++) {
        if ($current == $low) {
            $temp["$high"] = 0;
            $medianValues[$count] = $high;
            $count++;
            $current = $high;
            $i++;
        } else if ($values[$i] != 0) {
            $temp["$values[$i]"] = 0;
            $medianValues[$count] = $values[$i];
            $count++;
            $current = $values[$i];
        }
    }
}

    //$userDate = filter_input(INPUT_POST, 'date');
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
        
        define ("DATE_QUERY", "SELECT MAX(`date`) AS mdate FROM country_totals");
        
        //if ($userDate == null) {
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
        //} else {
        //    $date = $userDate;
        //}
         
        define ("SELECT_QUERY", "SELECT  
                (cases / population) * 100000 AS cases100k 
                FROM country_totals INNER JOIN country_codes 
                ON country_totals.country_id = country_codes.id 
                WHERE `date` = :dateParam 
                  AND country_codes.alpha_2 NOT IN ('R', 'S', 'W') 
                  ORDER BY (cases / population) * 100000;");
        
        // get results from the table query
        $stmt2 = $db_conn->prepare(SELECT_QUERY);
        // add parameter
        $stmt2->bindParam(':dateParam', $date);
        $stmt2->execute();
        // set the resulting array to associative
        $stmt->setFetchMode(PDO::FETCH_ASSOC);
        if (!$stmt2) {
            die("No Table Results");
        }
        $rows = $stmt2->fetchAll();
        // make standard array
        for ($i = 0; $i < count($rows); $i++) {
            $rowArray[$i] = $rows[$i]["cases100k"];
            
        }
        // set CORS headers
        header("Access-Control-Allow-Origin: *");
        // set JSON header
        header('Content-Type: application/json');
        // close database connection
        $db_conn = null;
        // calculate standard deviation 
        $standardDeviation = standard_deviation($rowArray);
        // calculate variance
        $variance = $standardDeviation ** 2;
        // calculate mean
        $mean = array_sum($rowArray) / count($rowArray);
        // calculate median
        $median = calculate_median($rowArray);
        // create medians
        $medians = distribute_values($median, $rowArray, false);
        // split arrays at mean
        $rowLowCount = 0;
        $rowHighCount = 0;
        for ($i = 0; $i < count($rowArray); $i++) {
            if ($rowArray[$i] < $mean) {
                $rowLow[$rowLowCount] = $rowArray[$i];
                $rowLowCount++;
            } else {
                $rowHigh[$rowHighCount] = $rowArray[$i];
                $rowHighCount++;
            }
        }
        // calculate median low
        $medianLow = calculate_median($rowLow);
        // create medians low
        $mediansLow = distribute_values($medianLow, $rowLow, false);
        // calulate standard deviation low
        $standardDeviationLow = standard_deviation($rowLow);
        // calculate variance low
        $varianceLow = $standardDeviationLow ** 2;
        // calculate mean low
        $meanLow = array_sum($rowLow) / count($rowLow);
        // calculate median High
        $medianHigh = calculate_median($rowHigh);
        // create medians High
        $mediansHigh = distribute_values($medianHigh, $rowHigh, true);
        // calulate standard deviation High
        $standardDeviationHigh = standard_deviation($rowHigh);
        // calculate variance High
        $varianceHigh = $standardDeviationHigh ** 2;
        // calculate mean High
        $meanHigh = array_sum($rowHigh) / count($rowHigh);
        // combine medians low and medians high
        $arrayCount = 0;
        $highestValue = 0;
        if ($mediansLow["0"] == 0) {
            $targetCount = 13;
            $scoreLabels = ['A+', 'A', 'A-', 'B+', 'B', 'B-', 'C+', 'C', 'C-', 
                'D+', 'D', 'D-', 'F'];
            unset($mediansLow["0"]);
        } else {
            $targetCount = 14;
            $scoreLabels = ['0','A+', 'A', 'A-', 'B+', 'B', 'B-', 'C+', 'C', 
                'C-', 'D+', 'D', 'D-', 'F'];
        }
        while ($value = current($mediansLow)) {
            if (key($mediansLow) === ">") {
                $medianOut[$arrayCount] = $value;
                $highestValue += $medianLow;
                $medianValues[$arrayCount] = $highestValue;
            } else {
                $medianOut[$arrayCount] = $value;
                $medianValues[$arrayCount] = floatval(key($mediansLow));
                $highestValue = floatval(key($mediansLow));
            }
            $arrayCount++;
            next($mediansLow);
        }
        while ($value = current($mediansHigh)) {
            $medianOut[$arrayCount] = $value;
            $medianValues[$arrayCount] = floatval(key($mediansHigh));
            $arrayCount++;
            next($mediansHigh);
        }
        while (count($medianOut) < $targetCount) {
            $temp = splitLargest($medianValues, $medianOut, $rowArray);
            $arrayCount = 0;
            $medianValues = array();
            $medianOut = array();
            while ($value = current($temp)) {
                $medianValues[$arrayCount] = floatval(key($temp));
                $medianOut[$arrayCount] = $value;
                $arrayCount++;
                next($temp);
            }
        }
        while (count($medianOut) > $targetCount) {
            $temp = combineSmallest($medianValues, $medianOut, $rowArray);
            $arrayCount = 0;
            $medianValues = array();
            $medianOut = array();
            while ($value = current($temp)) {
                $medianValues[$arrayCount] = floatval(key($temp));
                $medianOut[$arrayCount] = $value;
                $arrayCount++;
                next($temp);
            }
        }     
        // create Json Object
        $jsonObj["date"] = $date;
        $jsonObj["mean"] = $mean;
        $jsonObj["median"] = $median;
        $jsonObj["StandardDeviation"] = $standardDeviation;
        $jsonObj["variance"] = $variance;
        $jsonObj["counts"] = $medianOut;
        $jsonObj["labels"] = $scoreLabels;
	$jsonObj["values"] = $medianValues;
        // encode json array
        $json = json_encode($jsonObj, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
        /* Return the JSON string. */
        echo $json;
    } catch(PDOException $e) {
      echo "Connection failed: " . $e->getMessage();
    }