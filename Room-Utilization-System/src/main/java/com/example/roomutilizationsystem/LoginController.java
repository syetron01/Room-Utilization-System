package com.example.roomutilizationsystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;

// Make sure necessary imports for DataStore, User, SceneNavigator are included

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField; // Use PasswordField for passwords
    @FXML private Button loginButton;
    @FXML private Hyperlink signUpLink; // Assuming fx:id="signUpLink" for the hyperlink

    @FXML
    public void initialize() {
        // Initialization code if needed
    }

    @FXML
    private void handleLoginButtonAction(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Login Error", "Username and Password cannot be empty.");
            return;
        }

        User user = DataStore.authenticateUser(username, password);

        if (user != null) {
            DataStore.setLoggedInUser(user); // Store logged-in user globally
            try {
                if (user.getRole() == UserRole.ADMIN) {
                    // Navigate to Admin Home Page
                    SceneNavigator.navigateTo(event, "/fxml/adminHomePage.fxml"); // Adjust path
                } else if (user.getRole() == UserRole.FACULTY) {
                    // Navigate to Staff View Available Rooms Page
                    SceneNavigator.navigateTo(event, "/fxml/StaffViewAvailableRooms.fxml"); // Adjust path
                }
            } catch (IOException e) {
                e.printStackTrace();
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the next screen.");
            }
        } else {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password.");
        }
    }

    @FXML
    private void handleSignUpLinkAction(ActionEvent event) {
        try {
            SceneNavigator.navigateTo(event, "/fxml/SignUp.fxml"); // Adjust path
        } catch (IOException e) {
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the sign-up screen.");
        }
    }
}
