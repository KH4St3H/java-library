package com.example.library;

import com.example.library.types.User;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Database {
    private static final String url = "jdbc:postgresql://localhost:5432/library";
    private static final String username = "library-test";
    private static final String password = "library-test";
    public static Connection connect(){
        try {
            return DriverManager.getConnection(url, username, password);
        }
        catch (SQLException e){
            System.out.println(e.getMessage());
            return null;
        }
    }
    public static void main(String[] args){
        createTables();
        seedDB();
        
    }
    private static String hashPassword(String password){
        String salt = "you-suck-at-hacking+";
        password = salt.concat(password);
        MessageDigest m;
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e){
            return "";
        }
        m.update(password.getBytes());
        byte[] digest = m.digest();
        BigInteger bigInt = new BigInteger(1,digest);
        String hashtext = bigInt.toString(16);
        while(hashtext.length() < 32 ){
            hashtext = "0" + hashtext;
        }
        return hashtext;
    }

    private static void seedDB(){
        // create admin
        execQuery("INSERT INTO users(username, first_name, last_name, password, admin) VALUES(" +
                "'admin', 'Mehrshad', 'Firouzian', ?, TRUE)", hashPassword("admin"));
    }

    public static boolean execStatement(String sql){

        try (var conn =  Database.connect()) {
            assert conn != null;
            try (var stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                conn.close();
                return true;
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return false;
    }

    private static ResultSet execQuery(String sql, String... args){

        try (var conn =  Database.connect()) {
            assert conn != null;
            try (var stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i <args.length; i++) {
                    stmt.setString(i+1, args[i]);
                }
                return stmt.executeQuery();
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    public static void createTables(){
        // create books table
        String sql = "CREATE TABLE IF NOT EXISTS books (" +
                "    id SERIAL PRIMARY KEY," +
                "    title VARCHAR(255) NOT NULL," +
                "    category VARCHAR(30) NOT NULL DEFAULT 'other'," +
                "    total_count INTEGER NOT NULL DEFAULT 0," +
                "    year INTEGER," +
                "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";
        boolean success = execStatement(sql);
        if(!success){
            System.out.println("Error creating books table");
            return;
        }

        // create users table
        sql = "CREATE TABLE IF NOT EXISTS users (" +
                "    id SERIAL PRIMARY KEY," +
                "    username VARCHAR(15) UNIQUE," +
                "    first_name VARCHAR(255) NOT NULL," +
                "    last_name VARCHAR(30) NOT NULL," +
                "    password VARCHAR(32) NOT NULL UNIQUE," +
                "    admin BOOLEAN NOT NULL DEFAULT FALSE," +
                "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";
        success = execStatement(sql);
        if(!success){
            System.out.println("Error creating users table");
            return;
        }
    }
    public static User login(String username, String password){
        ResultSet rs = execQuery("SELECT first_name, last_name, admin from users WHERE (username = ? AND password = ?)", username, password);

        if(rs == null)
            return null;

        try{
            return new User(username, rs.getString("first_name"), rs.getString("first_name"), rs.getBoolean("admin"));
        } catch (SQLException e){
            return null;
        }
        
    }
}
