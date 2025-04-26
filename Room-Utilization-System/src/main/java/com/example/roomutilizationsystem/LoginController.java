package com.example.roomutilizationsystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;
// Import SceneNavigator, DataStore, User, UserRole

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink signUpLink; // Assuming Hyperlink for 'Sign Up'

    @FXML
    public void initialize() {
        // Initialization code if needed
        // For example, setting a default user for testing:
        // usernameField.setText("admin");
        // passwordField.setText("admin123");
    }

    @FXML
    private void handleLoginButtonAction(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText(); // Do not trim password

        if (username.isEmpty() || password.isEmpty()) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Login Error", "Username and Password cannot be empty.");
            return;
        }

        User user = DataStore.authenticateUser(username, password);
        // DataStore.authenticateUser now calls DataStore.setLoggedInUser internally

        if (user != null) {
            try {
                String fxmlPath;
                if (user.getRole() == UserRole.ADMIN) {
                    // Use full absolute path from classpath root
                    fxmlPath = "/com/example/roomutilizationsystem/fxml/adminHome.fxml";
                    System.out.println("Navigating to Admin Home...");
                } else if (user.getRole() == UserRole.FACULTY) {
                    // Use full absolute path from classpath root
                    fxmlPath = "/com/example/roomutilizationsystem/fxml/StaffViewAvailableRooms.fxml";
                    System.out.println("Navigating to Staff View Available Rooms...");
                } else {
                    SceneNavigator.showAlert(Alert.AlertType.ERROR, "Login Error", "Unknown user role.");
                    DataStore.logout(); // Clear session if role is weird
                    return;
                }
                // Pass the ActionEvent to navigateTo
                SceneNavigator.navigateTo(event, fxmlPath);

            } catch (IOException e) {
                System.err.println("Failed to load FXML during navigation: " + e.getMessage());
                e.printStackTrace();
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the next screen.\nCheck FXML path or file existence.");
                DataStore.logout(); // Ensure logout on nav failure
            } catch (Exception e) { // Catch any other unexpected errors during navigation
                System.err.println("An unexpected error occurred during navigation after login.");
                e.printStackTrace();
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Navigation Error", "An unexpected error occurred: " + e.getMessage());
                DataStore.logout(); // Ensure logout on unexpected error
            }

        } else {
            // Authentication failed - DataStore.authenticateUser already called setLoggedInUser(null)
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password.");
        }
    }

    @FXML
    private void handleSignUpLinkAction(ActionEvent event) {
        try {
            // Use full absolute path from classpath root
            // Pass the ActionEvent to navigateTo
            SceneNavigator.navigateTo(event, "/com/example/roomutilizationsystem/fxml/SignUp.fxml");
        } catch (IOException e) {
            System.err.println("Failed to load SignUp.fxml: " + e.getMessage());
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the sign-up screen.");
        }
    }
}