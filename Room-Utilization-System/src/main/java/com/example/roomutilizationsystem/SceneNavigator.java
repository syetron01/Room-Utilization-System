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
     * Navigates to a new scene defined by an FXML file.
     *
     * @param event           The ActionEvent that triggered the navigation (used to find the stage).
     * @param fxmlResourcePath The absolute path to the FXML file within the resources folder (e.g., "/com/example/roomutilizationsystem/fxml/Login.fxml").
     * @throws IOException If the FXML file cannot be loaded.
     */
    public static void navigateTo(javafx.event.ActionEvent event, String fxmlResourcePath) throws IOException {
        Objects.requireNonNull(event, "Event cannot be null");
        Objects.requireNonNull(fxmlResourcePath, "FXML resource path cannot be null");

        // Use getResourceAsStream for better error checking, then FXMLLoader.load(InputStream)
        // Or ensure the path is absolute from the classpath root.
        URL fxmlUrl = SceneNavigator.class.getResource(fxmlResourcePath);

        if (fxmlUrl == null) {
            // Try adding a leading slash if missing
            if (!fxmlResourcePath.startsWith("/")) {
                fxmlUrl = SceneNavigator.class.getResource("/" + fxmlResourcePath);
            }
            if (fxmlUrl == null) {
                System.err.println("Cannot find FXML resource: " + fxmlResourcePath + " or /" + fxmlResourcePath);
                throw new IOException("Cannot find FXML resource: " + fxmlResourcePath);
            }
        }
        System.out.println("Navigating to: " + fxmlUrl); // Debugging

        Parent root = FXMLLoader.load(fxmlUrl); // Objects.requireNonNull handled by FXMLLoader if URL is valid
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();

        if (stage == null) {
            throw new IllegalStateException("Could not find Stage from the event source.");
        }

        // Optional: Preserve window size or set explicitly
        double currentWidth = stage.getScene().getWidth();
        double currentHeight = stage.getScene().getHeight();
        stage.setScene(new Scene(root, currentWidth, currentHeight)); // Use current dimensions

        // Alternatively, use dimensions from the loaded root if preferred:
        // stage.setScene(new Scene(root));
        // stage.sizeToScene(); // Adjust stage size to the new scene

        stage.show();
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