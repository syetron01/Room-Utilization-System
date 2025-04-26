package com.example.roomutilizationsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the initial FXML file (Login screen)
            // Make sure the path is correct relative to the resources folder
            URL fxmlUrl = getClass().getResource("fxml/Login.fxml");
            if (fxmlUrl == null) {
                System.err.println("Cannot find FXML file: /fxml/Login.fxml");
                return; // Or throw an exception
            }
            Parent root = FXMLLoader.load(Objects.requireNonNull(fxmlUrl));

            Scene scene = new Scene(root, 1280, 720); // Match your FXML size

            primaryStage.setTitle("Room Utilization System");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false); // Optional: prevent resizing
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Failed to load Login.fxml");
            e.printStackTrace();
            // Show an error dialog to the user if needed
        } catch (NullPointerException e) {
            System.err.println("Null pointer during FXML loading. Check paths and FXML content.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}