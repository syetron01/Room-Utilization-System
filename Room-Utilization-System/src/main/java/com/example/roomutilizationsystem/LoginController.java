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
        // No hardcoded data here
    }

    @FXML
    private void handleLoginButtonAction(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText(); // Do not trim password

        if (username.isEmpty() || password.isEmpty()) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Login Error", "Username and Password cannot be empty.");
            return;
        }

        // Data comes from DataStore, which loaded from users.txt
        User user = DataStore.authenticateUser(username, password);

        if (user != null) {
            try {
                String fxmlPath;
                UserRole role = user.getRole(); // Get the user's role

                // --- Updated Role-Based Navigation ---
                switch (role) {
                    case ADMIN:
                        fxmlPath = "/com/example/roomutilizationsystem/fxml/adminHome.fxml";
                        System.out.println("Navigating to Admin Home...");
                        break;

                    case FACULTY:
                    case TEACHER:
                    case STAFF:
                        // All non-admin roles go to the Staff/Faculty view
                        fxmlPath = "/com/example/roomutilizationsystem/fxml/StaffViewAvailableRooms.fxml";
                        System.out.println("Navigating to Staff/Faculty View (" + role + ")...");
                        break;

                    default:
                        // Should not happen if enum is exhaustive and loading is correct
                        SceneNavigator.showAlert(Alert.AlertType.ERROR, "Login Error", "Unknown user role detected: " + role);
                        DataStore.logout(); // Clear session
                        return;
                }
                // --- End of Updated Navigation ---

                SceneNavigator.navigateTo(event, fxmlPath);

            } catch (IOException e) {
                System.err.println("Failed to load FXML during navigation: " + e.getMessage());
                e.printStackTrace();
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the next screen.\nCheck FXML path or file existence.");
                DataStore.logout();
            } catch (Exception e) {
                System.err.println("An unexpected error occurred during navigation after login.");
                e.printStackTrace();
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Navigation Error", "An unexpected error occurred: " + e.getMessage());
                DataStore.logout();
            }

        } else {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password.");
        }
    }

    @FXML
    private void handleSignUpLinkAction(ActionEvent event) {
        try {
            // Pass the ActionEvent to navigateTo
            SceneNavigator.navigateTo(event, "/com/example/roomutilizationsystem/fxml/SignUp.fxml");
        } catch (IOException e) {
            System.err.println("Failed to load SignUp.fxml: " + e.getMessage());
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the sign-up screen.");
        }
    }
}