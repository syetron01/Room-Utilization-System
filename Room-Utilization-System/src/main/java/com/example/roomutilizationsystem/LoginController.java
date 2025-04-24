package com.example.roomutilizationsystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;

// Make sure necessary imports for DataStore, User, UserRole, SceneNavigator are included

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink signUpLink;

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
            DataStore.setLoggedInUser(user);
            try {
                String fxmlPath;
                if (user.getRole() == UserRole.ADMIN) {
                    // --- FIX: Use full absolute path from classpath root ---
                    fxmlPath = "/com/example/roomutilizationsystem/fxml/adminHome.fxml"; // <-- Correct path based on image
                    // NOTE: Your image shows adminHome.fxml, but the error mentioned adminHomePage.fxml.
                    //       Use the EXACT filename you have. If it's adminHome.fxml use that:
                    // fxmlPath = "/com/example/roomutilizationsystem/fxml/adminHome.fxml";
                } else if (user.getRole() == UserRole.FACULTY) {
                    // --- FIX: Use full absolute path from classpath root ---
                    fxmlPath = "/com/example/roomutilizationsystem/fxml/StaffViewAvailableRooms.fxml";// <-- Correct path
                } else {
                    SceneNavigator.showAlert(Alert.AlertType.ERROR, "Login Error", "Unknown user role.");
                    return;
                }
                SceneNavigator.navigateTo(event, fxmlPath);

            } catch (IOException e) {
                System.err.println("Failed to load FXML: " + e.getMessage() + " (Path: " + (e.getMessage() != null && e.getMessage().contains("Cannot find") ? "Check FXML path!" : "Unknown I/O Error") + ")");
                e.printStackTrace();
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the next screen. Please check FXML path: \n" + (e.getMessage() != null ? e.getMessage().substring(e.getMessage().indexOf(':')+1).trim() : "Path issue"));
            } catch (NullPointerException npe) {
                System.err.println("Navigation error: FXML Path was null.");
                npe.printStackTrace();
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Internal error during navigation.");
            }

        } else {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password.");
        }
    }

    @FXML
    private void handleSignUpLinkAction(ActionEvent event) {
        try {
            // --- FIX: Use full absolute path from classpath root ---
            SceneNavigator.navigateTo(event, "/com/example/roomutilizationsystem/fxml/SignUp.fxml"); // <-- Correct path
        } catch (IOException e) {
            System.err.println("Failed to load FXML: " + e.getMessage());
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the sign-up screen.");
        }
    }
}