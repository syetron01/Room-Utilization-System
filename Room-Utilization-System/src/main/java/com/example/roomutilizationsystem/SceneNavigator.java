package com.example.roomutilizationsystem;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class SceneNavigator {

    /**
     * Navigates to a new scene defined by an FXML file using a provided Stage.
     * Useful when an ActionEvent is not available (e.g., initial load, programmatic navigation).
     *
     * @param stage           The Stage to set the new Scene on.
     * @param fxmlResourcePath The absolute path to the FXML file within the resources folder (e.g., "/com/example/roomutilizationsystem/fxml/Login.fxml").
     * @throws IOException If the FXML file cannot be loaded.
     * @throws NullPointerException if stage or fxmlResourcePath is null.
     */
    public static void navigateTo(Stage stage, String fxmlResourcePath) throws IOException {
        Objects.requireNonNull(stage, "Stage cannot be null for navigation.");
        Objects.requireNonNull(fxmlResourcePath, "FXML resource path cannot be null.");

        // Use getClass().getResource() from this class to find the resource URL
        URL fxmlUrl = SceneNavigator.class.getResource(fxmlResourcePath);

        if (fxmlUrl == null) {
            // Provide a more informative error message if the resource is not found
            String errorMessage = "Cannot find FXML resource: " + fxmlResourcePath;
            System.err.println(errorMessage);
            throw new IOException(errorMessage);
        }
        System.out.println("Navigating to: " + fxmlUrl); // Debugging

        Parent root = FXMLLoader.load(fxmlUrl); // FXMLLoader handles null URL itself if URL is valid

        // Set the new scene on the stage
        Scene newScene = new Scene(root);
        // Adjust stage size to the scene size defined in FXML (or preferred size)
        stage.setScene(newScene);
        stage.sizeToScene(); // Adjust stage size to the new scene based on FXML

        stage.show();
    }

    /**
     * Navigates to a new scene defined by an FXML file using an ActionEvent.
     * This method is typically used for button clicks or other UI events.
     *
     * @param event           The ActionEvent that triggered the navigation (used to find the stage).
     * @param fxmlResourcePath The absolute path to the FXML file within the resources folder.
     * @throws IOException If the FXML file cannot be loaded.
     * @throws NullPointerException if event or fxmlResourcePath is null.
     * @throws IllegalStateException if the Stage cannot be found from the event source (e.g., node is not in a scene).
     */
    public static void navigateTo(javafx.event.ActionEvent event, String fxmlResourcePath) throws IOException {
        // Ensure event is not null early
        Objects.requireNonNull(event, "ActionEvent cannot be null for navigation via ActionEvent.");

        Node source = (Node) event.getSource();
        // Check if the source node is actually in a scene and has a window (Stage)
        Scene currentScene = source.getScene();
        if (currentScene == null) {
            throw new IllegalStateException("Event source node is not attached to a Scene.");
        }
        Stage stage = (Stage) currentScene.getWindow();
        if (stage == null) {
            throw new IllegalStateException("Could not find Stage from the event source.");
        }

        // Delegate to the navigateTo method that accepts a Stage
        navigateTo(stage, fxmlResourcePath);
    }

    /**
     * Shows a standard JavaFX Alert dialog.
     *
     * @param type    The type of alert (e.g., INFORMATION, WARNING, ERROR).
     * @param title   The title of the alert window.
     * @param message The main message content of the alert.
     */
    public static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null); // Keep header simple
        alert.setContentText(message);
        alert.showAndWait();
    }
}