package com.example.library.types;

public class User {
    public String username;
    public String firstName;
    public String lastName;
    public boolean admin;
    public User(String username, String firstName, String lastName, boolean admin){
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.admin = admin;
    }
}
