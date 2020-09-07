<?php
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
    // Build Queries
    define ("WORLD_QUERY" , "SELECT total_cases, total_deaths, 
                active_cases, total_date
                FROM countries INNER JOIN totals ON countries.id = 
                    totals.country_id
                WHERE countries.country = 'World' AND total_date = 
                    (SELECT MAX(total_date) FROM totals)");
    define ("TABLE_QUERY", "with b as (SELECT totals.country_id, 
                country, total_cases, total_deaths, active_cases, 
                population, total_date 
            FROM countries INNER JOIN totals ON countries.id = 
                totals.country_id 
            LEFT OUTER JOIN populations ON countries.id = 
                populations.country_id 
            WHERE total_date = (SELECT MAX(total_date) FROM totals) 
            ORDER BY total_cases DESC)
            SELECT country, total_cases, total_deaths, active_cases, 
                population, total_date, b.total_cases - 
                (SELECT total_cases FROM totals  
            WHERE totals.country_id = b.country_id AND 
                totals.total_date = b.total_date -1) AS new_cases 
            FROM b");
    // get results from world query
    $worldResult = pg_query($db_conn, WORLD_QUERY);
    if (!$worldResult) {
        die("No World Results");
    }
    // get results from table query
    $tableResult = pg_query($db_conn, TABLE_QUERY);
    if (!$tableResult) {
        die("No Table Results");
    }
    // get world row
    $worldRow = pg_fetch_row($worldResult);
  // Build table body
  while ($row = pg_fetch_row($tableResult)) {
    echo "<tr>";
    echo "<td>" . $row[0] . "</td>";
    echo "<td>" . number_format(intval($row[4])) . "</td>";
    echo "<td>" . number_format(intval($row[1])) . "</td>";
    echo "<td>" . number_format(intval(($row[1])/ 
            intval($worldRow[0])) * 100) . "%" . "</td>";
    echo "<td>" . number_format(intval($row[6])) . "</td>";
    echo "<td>" . number_format(intval([3])) . "</td>";
    echo "<td>" . number_format((intval($row[3]) / 
            intval($worldRow[2])) * 100) . "%" . "</td>";
    echo "<td>" . number_format(intval($row[2])) . "</td>";
    echo "<td>" . number_format((intval($row[2]) / 
            intval($worldRow[1])) * 100) . "%" . "</td>";
    echo "<td>" . number_format(($row[1]) - intval($row[3])) . "</td>";
    echo "<td>" . number_format(((intval($row[1]) - intval($row[3])) / 
            (intval($worldRow[0]) - 
            intval($worldRow[2]))) * 100) . "%" . "</td>";
    echo "<td>" . number_format((intval($row[3]) / intval($row[1])) * 100) . 
            "%" . "</td>";
    echo "</tr>";
  }

?>