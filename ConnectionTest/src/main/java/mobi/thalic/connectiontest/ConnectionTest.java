/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobi.thalic.connectiontest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author Gary Larson gary@thalic.mobi
 */
public class ConnectionTest {
    
    public static void main(String[] args)  {
        // Declare constants
        final String CONNECTION_STRING = "jdbc:mysql://mysql.stackcp.com:54691/";
        final String USER_NAME = "java_program";
        final String USER_PASSWORD = "3peb3NnzY_2Md@*yGb";
        final String USER_DATABASE = "havran-3135393c01";
        // Declare variables
        Connection conn = null;
        try {
            // Attempt to connect to database
            conn = DriverManager.getConnection(CONNECTION_STRING + USER_DATABASE, 
                USER_NAME, USER_PASSWORD);
            System.out.println("Connection Successful!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    // close connection
                    conn.close();
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }
}
    