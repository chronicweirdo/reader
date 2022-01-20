package com.cacoveanu;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBVerify {

    public static void main(String[] args) {
            Connection con = null;
            Statement stmt = null;
            ResultSet result = null;

            try {
                //Registering the HSQLDB JDBC driver
                Class.forName("org.hsqldb.jdbc.JDBCDriver");
                //Creating the connection with HSQLDB
                String connectionString = "jdbc:hsqldb:file:c:/chronicreader-config/reader_db;DB_CLOSE_DELAY=-1";
                con = DriverManager.getConnection(connectionString, "sa", "");
                if (con!= null){
                    System.out.println("Connection created successfully");

                    Statement stmt1 = con.createStatement();
                    ResultSet result1 = stmt1.executeQuery("select count(*) as cnt from book b where b.added is not null");
                    while(result1.next()) {
                        System.out.println(result1.getInt("cnt"));
                    }


                    stmt = con.createStatement();
                    result = stmt.executeQuery("select * from book b where b.added is not null order by b.added desc limit 100");

                    while(result.next()){
                        System.out.println(result.getString("title"));
                        System.out.println(result.getString("collection"));
                        System.out.println(result.getDate("added"));
                    }

                } else {
                    System.out.println("Problem with creating connection");
                }

            }  catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
}

