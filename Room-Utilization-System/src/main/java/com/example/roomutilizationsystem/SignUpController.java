package com.example.roomutilizationsystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;

// Import DataStore, User, SceneNavigator, UserRole

public class SignUpController {

    @FXML private TextField usernameBox;
    @FXML private PasswordField password1; // Use PasswordField
    @FXML private Button signUpButton; // Assumed fx:id="signUpButton" for the main button
    @FXML private Button loginLinkButton; // Assumed fx:id="loginLinkButton" for the 'Log In' button link

    @FXML
    public void initialize() {
        // Initialization code if needed
    }

    @FXML
    private void handleSignUpButtonAction(ActionEvent event) {
        String username = usernameBox.getText().trim();
        String password = password1.getText(); // No trim, allow spaces if desired

        if (username.isEmpty() || password.isEmpty()) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Sign Up Error", "Username and Password cannot be empty.");
            return;
        }

        // Basic validation (add more as needed)
        if (password.length() < 4) { // Example minimum length
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Sign Up Error", "Password must be at least 4 characters long.");
            return;
        }
        // Prevent creating user with role name 'admin' if it's reserved
        if (username.equalsIgnoreCase("admin")) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Sign Up Error", "Username 'admin' is reserved.");
            return;
        }


        // Attempt to add user (default role FACULTY)
        // DataStore.addUser now saves automatically if successful
        boolean success = DataStore.addUser(username, password, UserRole.FACULTY);

        if (success) {
            SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Sign Up Successful", "Account created successfully! Please log in.");
            try {
                // Navigate back to Login screen
                // Use full absolute path from classpath root
                // Pass the ActionEvent to navigateTo
                SceneNavigator.navigateTo(event, "/com/example/roomutilizationsystem/fxml/Login.fxml");
            } catch (IOException e) {
                e.printStackTrace();
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the login screen.");
            }
        } else {
            // addUser returns false if username exists or input is invalid
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Sign Up Failed", "Username '" + username + "' already exists or is invalid.");
        }
    }

    @FXML
    private void handleLoginLinkAction(ActionEvent event) {
        try {
            // Use full absolute path from classpath root
            // Pass the ActionEvent to navigateTo
            SceneNavigator.navigateTo(event, "/com/example/roomutilizationsystem/fxml/Login.fxml");
        } catch (IOException e) {
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the login screen.");
        }
    }
}