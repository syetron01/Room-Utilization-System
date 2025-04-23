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
            // --- Use Absolute Path from Classpath Root ---
            String fxmlPath = "/com/example/roomutilizationsystem/fxml/Login.fxml";
            URL fxmlUrl = getClass().getResource(fxmlPath);

            if (fxmlUrl == null) {
                System.err.println("CRITICAL ERROR: Cannot find FXML file at specified path: " + fxmlPath);
                // Show an alert or log before exiting
                SceneNavigator.showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Startup Error", "Cannot load main interface file. Application will exit.");
                return; // Exit if the main screen can't load
            }

            System.out.println("Loading initial FXML from: " + fxmlUrl); // Debugging
            Parent root = FXMLLoader.load(Objects.requireNonNull(fxmlUrl));

            // Set scene size explicitly or let it size to content
            Scene scene = new Scene(root, 1280, 720); // Match your desired initial size

            primaryStage.setTitle("Room Utilization System");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false); // As specified in FXML example
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Failed to load FXML during startup: " + e.getMessage());
            e.printStackTrace();
            SceneNavigator.showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Startup Error", "An error occurred while loading the application interface:\n" + e.getMessage());
        } catch (NullPointerException e) {
            System.err.println("Null pointer during FXML loading. Check FXML content and controller initialization.");
            e.printStackTrace();
            SceneNavigator.showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Startup Error", "A null pointer error occurred during startup.");
        } catch (Exception e) {
            // Catch any other unexpected exceptions during startup
            System.err.println("An unexpected error occurred during startup: " + e.getMessage());
            e.printStackTrace();
            SceneNavigator.showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Startup Error", "An unexpected error occurred: \n" + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Initialize DataStore (preload data) - happens automatically via static block
        System.out.println("Application starting...");
        launch(args);
        System.out.println("Application finished.");
    }
}