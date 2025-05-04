// FILE: com/example/roomutilizationsystem/SignUpController.java
package com.example.roomutilizationsystem;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class SignUpController {

    // --- FXML Fields ---
    // @FXML private TextField nameField; // REMOVED
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<UserRole> roleComboBox;
    @FXML private Button signUpButton;
    @FXML private Hyperlink loginLink;

    @FXML
    public void initialize() {
        // Populate the Role ComboBox
        List<UserRole> availableRoles = List.of(UserRole.STAFF, UserRole.TEACHER, UserRole.FACULTY);
        if (roleComboBox != null) {
            roleComboBox.setItems(FXCollections.observableArrayList(availableRoles));
        } else {
            System.err.println("Warning: roleComboBox is null during initialization. Check FXML fx:id.");
        }
    }

    @FXML
    private void handleSignUpButtonAction(ActionEvent event) {
        // --- Get Input ---
        // String name = nameField.getText().trim(); // REMOVED
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        UserRole selectedRole = roleComboBox.getValue();

        // --- Basic Validation ---
        // Removed check for name.isEmpty()
        if (username.isEmpty() || password.isEmpty() || selectedRole == null) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Sign Up Error",
                    "Please fill in all fields (Username, Password, Role).");
            return;
        }
        if (password.length() < 4) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Sign Up Error", "Password must be at least 4 characters long.");
            return;
        }
        if (username.equalsIgnoreCase("admin")) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Sign Up Error", "Username 'admin' is reserved.");
            return;
        }
        if (selectedRole == UserRole.ADMIN) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Sign Up Error", "Cannot sign up as an Administrator.");
            roleComboBox.setValue(null);
            return;
        }

        // --- Attempt to add user ---
        System.out.println("Attempting to add user: " + username + ", Role: " + selectedRole);
        boolean success = DataStore.addUser(username, password, selectedRole);

        if (success) {
            // *** UPDATED ALERT MESSAGE ***
            // WARNING: Displaying the password, even temporarily in an alert,
            // is generally considered insecure practice. A real application
            // should avoid this. This is done strictly per the request.
            SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Sign Up Successful",
                    "Account created successfully!\n\n" +
                            "Username: " + username + "\n" +
                            "Password: " + password + "  (Remember this!)\n" + // Added password display - INSECURE!
                            "Role:     " + selectedRole + "\n\n" +
                            "Please log in.");
            clearFields();
            navigateToLogin(event);
        } else {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Sign Up Failed",
                    "Could not create account.\nThe username '" + username + "' might already exist, or another error occurred.\n(Check console logs for details)");
        }
    }

    @FXML
    private void handleLoginLinkAction(ActionEvent event) {
        navigateToLogin(event);
    }

    // --- Helper Methods ---

    private void navigateToLogin(ActionEvent event) {
        try {
            SceneNavigator.navigateTo(event, "/com/example/roomutilizationsystem/fxml/Login.fxml");
        } catch (IOException e) {
            System.err.println("Navigation Error: Failed to load Login.fxml: " + e.getMessage());
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the login screen.");
        }
    }

    private void clearFields() {
        // if (nameField != null) nameField.clear(); // REMOVED
        if (usernameField != null) usernameField.clear();
        if (passwordField != null) passwordField.clear();
        if (roleComboBox != null) roleComboBox.getSelectionModel().clearSelection();
        // Set focus to username field now
        if (usernameField != null) usernameField.requestFocus();
    }
}