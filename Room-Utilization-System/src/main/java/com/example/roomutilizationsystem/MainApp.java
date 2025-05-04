package com.example.roomutilizationsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the initial FXML file (Login screen)
            // MAKE SURE THIS PATH IS CORRECT RELATIVE TO YOUR RESOURCES FOLDER
            // Use the absolute path from the classpath root
            URL fxmlUrl = getClass().getResource("/com/example/roomutilizationsystem/fxml/Login.fxml"); // Correct path

            if (fxmlUrl == null) {
                String errorMsg = "Cannot find FXML file: /com/example/roomutilizationsystem/fxml/Login.fxml\nCheck application resources and path.";
                System.err.println(errorMsg);
                // Show a user-friendly error and exit gracefully
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Startup Error", errorMsg);
                // Exit JavaFX platform
                System.exit(1); // Use System.exit to terminate the application fully
                return; // Should not be reached after System.exit, but good practice
            }
            // FXMLLoader.load requires the URL
            Parent root = FXMLLoader.load(fxmlUrl); // FXMLLoader handles null URL itself if URL is valid

            // Set the scene
            Scene scene = new Scene(root);
            // Adjust stage size to the scene size defined in FXML
            primaryStage.sizeToScene();

            primaryStage.setTitle("Room Utilization System");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false); // Optional: prevent resizing
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Failed to load Login.fxml");
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Startup Error", "Failed to load the initial application screen.\nDetails: " + e.getMessage());
            System.exit(1); // Exit on fatal error
        } catch (NullPointerException e) {
            System.err.println("Null pointer during FXML loading. Check paths and FXML content.");
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Startup Error", "An internal error occurred during startup (NPE).\nCheck FXML file content or missing @FXML injections.");
            System.exit(1); // Exit on fatal error
        } catch (Exception e) { // Catch any other unexpected errors during startup
            System.err.println("An unexpected error occurred during startup.");
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Startup Error", "An unexpected error occurred: " + e.getMessage());
            System.exit(1); // Exit on fatal error
        }
    }

    @Override
    public void stop() throws Exception {
        // This method is called when the application is closing (e.g., window closed)
        // DataStore.saveData() is already called on changes, so explicit save here is optional
        System.out.println("Application stopping.");
        super.stop();
        // Ensure all non-daemon threads shut down. System.exit is more reliable for full termination.
        // Platform.exit(); // Alternative to System.exit(0) if only JavaFX threads need stopping
        System.exit(0); // Exit cleanly
    }


    public static void main(String[] args) {
        // DataStore static initializer runs when the DataStore class is first
        // loaded by the JVM, which typically happens before launch().
        // This ensures data is loaded *before* controllers try to access it.
        // You could explicitly call DataStore.loadData() here, but the static block is fine.
        launch(args);
    }
}