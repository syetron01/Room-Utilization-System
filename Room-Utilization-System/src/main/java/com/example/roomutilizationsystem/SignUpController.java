package com.example.roomutilizationsystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;

// Make sure necessary imports for DataStore, User, SceneNavigator, UserRole are included

public class SignUpController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField; // Use PasswordField
    @FXML private Button signUpButton; // Assumed fx:id="signUpButton" for the main button
    @FXML private Button loginLinkButton; // Assumed fx:id="loginLinkButton" for the 'Log In' button link

    @FXML
    public void initialize() {
        // Set fx:id for the Sign Up button in FXML to "signUpButton"
        // Set fx:id for the Log In button link in FXML to "loginLinkButton"
    }

    @FXML
    private void handleSignUpButtonAction(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText(); // No trim, allow spaces if desired

        if (username.isEmpty() || password.isEmpty()) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Sign Up Error", "Username and Password cannot be empty.");
            return;
        }

        // Basic validation (add more as needed)
        if (password.length() < 4) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Sign Up Error", "Password must be at least 4 characters long.");
            return;
        }

        // Attempt to add user (default role FACULTY)
        boolean success = DataStore.addUser(username, password, UserRole.FACULTY);

        if (success) {
            SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Sign Up Successful", "Account created successfully! Please log in.");
            try {
                // Navigate back to Login screen
                SceneNavigator.navigateTo(event, "/fxml/Login.fxml"); // Adjust path
            } catch (IOException e) {
                e.printStackTrace();
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the login screen.");
            }
        } else {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Sign Up Failed", "Username '" + username + "' already exists.");
        }
    }

    @FXML
    private void handleLoginLinkAction(ActionEvent event) {
        try {
            SceneNavigator.navigateTo(event, "/fxml/Login.fxml"); // Adjust path
        } catch (IOException e) {
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the login screen.");
        }
    }
}


