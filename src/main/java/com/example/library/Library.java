package com.example.library;

import com.example.library.types.User;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Library extends Application {
    private static final String salt = "you-suck-at-hacking+";

    public void start(Stage Log_in) throws FileNotFoundException {
        menu();
        Database.createTables();
        Database.seedDB();
    }

    private static String hashPassword(String password){
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
    private User login(String username, String password){
        String hashedPassword = hashPassword(password);
        return Database.login(username, hashedPassword);
    }

    public void menu() throws FileNotFoundException {
        Stage Log_in = new Stage();
        Log_in.setTitle("Library");
        Rectangle rect = new Rectangle(1280, 720);
        Image background = new Image(new FileInputStream("src/main/resources/images/log.png"));
        rect.setFill(new ImagePattern(background));

        TextField username = new TextField();
        username.setPromptText("Enter Username");
        username.setLayoutX(510);
        username.setLayoutY(285);
        username.setMaxSize(260, 75);
        username.setMinSize(260, 75);
        username.setStyle("""
            -fx-background-color: unset;
            -fx-text-fill: #000000;
            -fx-font-size: 24px;
            -fx-font-weight: bold;
                """);
        PasswordField password = new PasswordField();
        password.setPromptText("Enter password");
        password.setLayoutX(510);
        password.setLayoutY(376);
        password.setMaxSize(260, 75);
        password.setMinSize(260, 75);
        password.setStyle("""
            -fx-background-color: #ffffff;
            -fx-text-fill: #000000;
            -fx-font-size: 24px;
            -fx-font-weight: bold;
            -fx-background-radius: 30px;
                """);

        TextField textField = new TextField();
        textField.setPromptText("Enter password");
        textField.setLayoutX(510);
        textField.setLayoutY(376);
        textField.setMaxSize(260, 75);
        textField.setMinSize(260, 75);
        textField.setStyle("""
            -fx-background-color: #ffffff;
            -fx-text-fill: #000000;
            -fx-font-size: 24px;
            -fx-font-weight: bold;
            -fx-background-radius: 20px;
                """);
        password.textProperty().bindBidirectional(textField.textProperty());
        CheckBox checkBox = new CheckBox();
        checkBox.setLayoutY(465);
        checkBox.setLayoutX(675);
        checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                textField.toFront();
            }
            else {
                password.toFront();
            }
        });


        Button log = new Button("Log In");
        log.setLayoutX(574);
        log.setLayoutY(505);
        log.setMaxSize(130, 50);
        log.setMinSize(130, 50);
        log.setStyle("""
            -fx-background-color: unset;
            -fx-background-radius: 25px;
            -fx-text-fill: #000000;
            -fx-font-size: 17px;
            -fx-font-weight: bold;
            -fx-border-color: #000000;
            -fx-border-width: 3px;
            -fx-border-radius: 25px;
                """);
        log.setCursor(Cursor.HAND);
        log.setOnMouseEntered(mouseEvent-> {
            log.setStyle("""
                -fx-background-color: #000000;
                -fx-background-radius: 20px;
                -fx-text-fill: #ffffff;
                -fx-font-size: 20px;
                -fx-font-weight: bold;
                -fx-border-color: #ffffff;
                -fx-border-width: 4px;
                -fx-border-radius: 20px;
            """);
        });
        log.setOnMouseExited(mouseEvent-> {
            log.setStyle("""
                -fx-background-color: unset;
                -fx-background-radius: 25px;
                -fx-text-fill: #000000;
                -fx-font-size: 17px;
                -fx-font-weight: bold;
                -fx-border-color: #000000;
                -fx-border-width: 3px;
                -fx-border-radius: 25px;
            """);
        });

        log.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                String name = username.getText();
                String pass = password.getText();
                User user = login(name, pass);
                if (user == null){
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Password does not match username");
                    alert.setHeaderText("Wrong Password!");
                    alert.setTitle("Library Error!");
                    alert.showAndWait();
                } else if (user.admin){
                        try {
                            admin.admin_page(Log_in, name);
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                }else {
                    try {
                        Users.user_page(Log_in, user.firstName, user.username);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        });

        Group root = new Group(rect, textField, username, password, log, checkBox);

        Scene scene_1 = new Scene(root);

        Log_in.setScene(scene_1);
        Log_in.setResizable(false);
        Log_in.show();
    }



    public static void main(String[] args) {
        launch();
    }
}
