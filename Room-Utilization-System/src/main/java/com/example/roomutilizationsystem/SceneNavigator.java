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

    // Updated navigation method
    public static void navigateTo(javafx.event.ActionEvent event, String fxmlResourcePath) throws IOException {
        // Ensure path starts with "/" to indicate root of resources
        if (!fxmlResourcePath.startsWith("/")) {
            fxmlResourcePath = "/" + fxmlResourcePath;
        }
        // Use getResource to find the FXML file in the classpath
        URL fxmlUrl = SceneNavigator.class.getResource(fxmlResourcePath);
        if (fxmlUrl == null) {
            throw new IOException("Cannot find FXML resource: " + fxmlResourcePath);
        }

        Parent root = FXMLLoader.load(Objects.requireNonNull(fxmlUrl));
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.setScene(new Scene(root)); // Re-use existing stage dimensions or set explicitly
        stage.show();
    }

    public static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null); // Keep it simple
        alert.setContentText(message);
        alert.showAndWait();
    }
}