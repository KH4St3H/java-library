package com.example.library;

import com.example.library.types.User;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Database {
    private static final String url = "jdbc:postgresql://192.168.41.125:5432/library";
    private static final String username = "library-test";
    private static final String password = "library-test";
    private static Connection connect(){
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

    public static void seedDB(){
        // create admin
        execQuery("INSERT INTO users(username, first_name, last_name, password, admin) VALUES(" +
                "'admin', 'Mehrshad', 'Firouzian', ?, TRUE)", hashPassword("admin"));
    }

    private static boolean execStatement(String sql){

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
                "    author VARCHAR(255) NOT NULL," +
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
                "    password VARCHAR(32) NOT NULL," +
                "    department VARCHAR(30)," +
                "    level VARCHAR(30)," +
                "    admin BOOLEAN NOT NULL DEFAULT FALSE," +
                "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";
        success = execStatement(sql);
        if(!success){
            System.out.println("Error creating users table");
            return;
        }
        // create borrow table
        sql = "create table if not exists borrow(" +
            "   user_id VARCHAR(30)," + 
            "   book_id INT," +
            "   constraint fk_user foreign KEY(user_id) references users(username) on delete CASCADE," +
            "   constraint fk_book foreign KEY(book_id) references books(id) on delete CASCADE," +
            "   start_data TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "   return_date TIMESTAMP" +
            ");";
           success = execStatement(sql);
        if(!success){
            System.out.println("Error creating borrow table");
            return;
        }
    }

     public static boolean addUser(String stdNumber, String fullName, String level, String department, String password){
        String[] name = fullName.split(" ", 2);

        var conn = Database.connect();
        try (var stmt = conn.prepareStatement("INSERT INTO users(username, first_name, last_name, password, department, level) VALUES(?, ?, ?, ?, ?, ?);")) {
            stmt.setString(1, stdNumber);
            stmt.setString(2, name[0]);
            stmt.setString(3, name[1]);
            stmt.setString(4, hashPassword(password));
            stmt.setString(5, department);
            stmt.setString(6, level);
            stmt.execute();

            return true;
        } catch(SQLException e){
            System.out.println(e.getMessage());
            return false;
        }
        
    }

     public static boolean addBook(String title, String author, String category, int count){
        String[] args = {title, author, category};

        var conn = Database.connect();
        try (var stmt = conn.prepareStatement("INSERT INTO books(title, author, category, total_count) VALUES(?, ?, ?, ?);")) {
            for (int i = 0; i <args.length; i++) {
                stmt.setString(i+1, args[i]);
            }
            stmt.setInt(4, count);
            stmt.execute();

            return true;
        } catch(SQLException e){
            System.out.println(e.getMessage());
            return false;
        }
        
    }

    public static ObservableList<Users> getAllUsers(){
        var sql = "SELECT" +
            "    B.*, " +
            "    SUM(case when O.return_date is null and O.book_id is not null then 1 else 0 end) AS borrowed" +
            "   FROM " +
            "        users B" +
            "    LEFT JOIN " +
            "        borrow  O" +
            "    ON " +
            "        O.user_id = B.username" +
            "    where (" +
            "        B.admin = FALSE)" +
            "   GROUP BY " +
            "   B.id ;";

        ObservableList<Users> list = FXCollections.observableArrayList();
        var conn = Database.connect();
        try (var stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()){
                try{
                    list.add(new Users(rs.getString("first_name"), rs.getString("last_name"),
                                rs.getString("username"), rs.getString("level"), rs.getString("department"), rs.getInt("borrowed")));
                
                } catch (SQLException e){
                    System.out.println(e.getMessage());
                }
            }
        } catch(SQLException e){
            System.out.println(e.getMessage());
        }
        return list;
        
    }

    public static ObservableList<Users> searchUsers(String username){
        var sql = "SELECT" +
                "    B.*, " +
                "SUM(case when O.return_date is null and O.book_id is not null then 1 else 0 end) AS borrowed" +
                "   FROM " +
                "        users B" +
                "    LEFT JOIN " +
                "        borrow  O" +
                "    ON " +
                "        O.user_id = B.username" +
                "    where (" + "B.username like ?"+
                "       AND B.admin = FALSE)" +
                "   GROUP BY " +
                "   B.id ;";

        ObservableList<Users> list = FXCollections.observableArrayList();
        var conn = Database.connect();
        try (var stmt = conn.prepareStatement(sql)) {
            username = "%" + username + "%";
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
                try{
                    list.add(new Users(rs.getString("first_name"), rs.getString("last_name"),
                            rs.getString("username"), rs.getString("level"), rs.getString("department"), rs.getInt("borrowed")));

                } catch (SQLException e){
                    System.out.println(e.getMessage());
                }
            }
        } catch(SQLException e){
            System.out.println(e.getMessage());
        }
        return list;

    }

    public static boolean returnBook(String username, String book_id){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
         var conn = Database.connect();
         try (var stmt = conn.prepareStatement("UPDATE borrow SET return_date = ? WHERE (user_id = ? AND book_id = ?);")) {
             stmt.setTimestamp(1, timestamp);
             stmt.setString(2, username);
             stmt.setInt(3, Integer.parseInt(book_id));
             stmt.execute();

             return true;
         } catch(SQLException e){
             System.out.println("**********************");
             System.out.println(e.getMessage());
             return false;
         }
    }

    public static ObservableList<borrowed_books> getAllBorrowedBooks(){
        var sql = "SELECT borrow.*, books.title FROM borrow LEFT JOIN books ON books.id = borrow.book_id ORDER BY start_date";

        ObservableList<borrowed_books> list = FXCollections.observableArrayList();
        var conn = Database.connect();
        try (var stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()){
                try{
                    list.add(new borrowed_books(rs.getInt("book_id"), rs.getString("title"),
                            rs.getString("user_id"), rs.getString("start_date"), rs.getString("return_date")));
                
                } catch (SQLException e){
                    System.out.println(e.getMessage());
                }
            }
        } catch(SQLException e){
            System.out.println(e.getMessage());
        }
        return list;
        
    }

    public static ObservableList<books> getAllBooks(){
        var sql = "SELECT" +
            "    B.*, " +
            "    SUM(case when O.return_date is null and O.book_id is not null then 1 else 0 end) AS borrowed" +
            "   FROM " +
            "        books  B" +
            "    LEFT JOIN " +
            "        borrow  O" +
            "    ON " +
            "        O.book_id = B.id" +
            "   GROUP BY " +
            "   B.id ;";

        ObservableList<books> list = FXCollections.observableArrayList();
        var conn = Database.connect();
        try (var stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()){
                try{
                    int totalCount = rs.getInt("total_count");
                    int borrowed = rs.getInt("borrowed");
                    System.out.println();
                    list.add(new books(rs.getInt("id"), rs.getString("title"),
                                rs.getString("author"), rs.getString("category"), totalCount - borrowed, borrowed));
                
                } catch (SQLException e){
                    System.out.println(e.getMessage());
                }
            }
        } catch(SQLException e){
            System.out.println(e.getMessage());
        }
        return list;
        
    }

    public static ObservableList<books> searchBooks(String title){
        var sql = "SELECT" +
                "    B.*, " +
                "    SUM(case when O.return_date is null and O.book_id is not null then 1 else 0 end) AS borrowed" +
                "   FROM " +
                "        books  B" +
                "    LEFT JOIN " +
                "        borrow  O" +
                "    ON " +
                "        O.book_id = B.id" +
                "    WHERE " +
                "        B.title like ? "+
                "   GROUP BY " +
                "   B.id ;";

        ObservableList<books> list = FXCollections.observableArrayList();
        var conn = Database.connect();
        try (var stmt = conn.prepareStatement(sql)) {
            title = "%" + title + "%";
            stmt.setString(1, title);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
                try{
                    int totalCount = rs.getInt("total_count");
                    int borrowed = rs.getInt("borrowed");
                    System.out.println();
                    list.add(new books(rs.getInt("id"), rs.getString("title"),
                            rs.getString("author"), rs.getString("category"), totalCount - borrowed, borrowed));

                } catch (SQLException e){
                    System.out.println(e.getMessage());
                }
            }
        } catch(SQLException e){
            System.out.println(e.getMessage());
        }
        return list;

    }
    
    public static String lendBook(int bookId, String username){
        var conn = Database.connect();
        var sql = "SELECT" +
            "    B.*, " +
            "    SUM(case when O.return_date is null and O.book_id is not null then 1 else 0 end) AS borrowed" +
            "   FROM " +
            "        books  B" +
            "    LEFT JOIN " +
            "        borrow  O" +
            "    ON " +
            "        O.book_id = B.id" +
            "    WHERE " +
            "        O.book_id = ?" +
            "   GROUP BY " +
            "   B.id ;";


        try (var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            ResultSet st = stmt.executeQuery();
            if (!st.next())
                return "Book does not exist!";
            if(st.getInt("borrowed") == st.getInt("total_count"))
                return "All books are taken";

        } catch(SQLException e){
            return e.getMessage();
        }


        try (var stmt = conn.prepareStatement("INSERT INTO borrow(book_id, user_id) VALUES(?, ?);")) {
            stmt.setInt(1, bookId);
            stmt.setString(2, username);
            stmt.execute();

            return null;
        } catch(SQLException e){
            System.out.println("**********************");
            System.out.println(e.getMessage());
            return e.getMessage();
        }
    }

    public static books searchBook(String title){
        String[] args = {title};

        var conn = Database.connect();
        try (var stmt = conn.prepareStatement("SELECT * FROM users WHERE (title LIKE '%?%')")) {
            for (int i = 0; i <args.length; i++) {
                stmt.setString(i+1, args[i]);
            }
            var rs = stmt.executeQuery();
            rs.next();
            try{

                // String firstName = rs.getString("first_name");
                // System.out.println(firstName);

              // books(int Id, String Name, String author, String Category, int available, int lent);
                int count = rs.getInt("total_count");



                return new books(rs.getInt("id"), rs.getString("title"), rs.getString("author"), rs.getString("category"), count, count);
            
            } catch (SQLException e){
                System.out.println(e.getMessage());
                return null;
            }
        } catch(SQLException e){
            return null;
        }
        
    }
    
    public static User login(String username, String password){
        String[] args = {username, password};

        var conn = Database.connect();
        try (var stmt = conn.prepareStatement("SELECT first_name, last_name, admin from users WHERE (username = ? AND password = ?)")) {
            for (int i = 0; i <args.length; i++) {
                stmt.setString(i+1, args[i]);
            }
            var rs = stmt.executeQuery();
            rs.next();
            try{

                String firstName = rs.getString("first_name");
                System.out.println(firstName);
                return new User(username, firstName, rs.getString("last_name"), rs.getBoolean("admin"));
            
            } catch (SQLException e){
                System.out.println(e.getMessage());
                return null;
            }
        } catch(SQLException e){
            return null;
        }
        
    }
}
