package com.example.roomutilizationsystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.io.IOException;
// Import SceneNavigator, DataStore

// Common methods that would exist in each Staff Controller
public abstract class StaffBaseController {

    // --- Navigation Methods ---

    // Modified to ensure navigation happens via SceneNavigator to reload the FXML
    protected void navigateToViewAvailable(ActionEvent event) {
        try {
            // Using SceneNavigator to reload the FXML and controller,
            // which will cause the initialize method to run again
            SceneNavigator.navigateTo(event, "/com/example/roomutilizationsystem/fxml/StaffViewAvailableRooms.fxml");
        } catch (IOException e) {
            handleNavError(e);
        }
    }

    // Keep as is - navigation to manage bookings also uses SceneNavigator
    protected void navigateToManageBookings(ActionEvent event) {
        try {
            SceneNavigator.navigateTo(event, "/com/example/roomutilizationsystem/fxml/StaffManageBookings.fxml");
        } catch (IOException e) {
            handleNavError(e);
        }
    }

    // --- Logout ---
    @FXML // Make sure the logout button in FXML calls this
    protected void handleLogoutAction(ActionEvent event) {
        DataStore.logout(); // Clear the logged-in user
        try {
            // Using SceneNavigator to reload the FXML and controller
            SceneNavigator.navigateTo(event, "/com/example/roomutilizationsystem/fxml/Login.fxml"); // Go back to login
        } catch (IOException e) {
            handleNavError(e);
        }
    }

    // Keep as is - standard error handler
    private void handleNavError(IOException e) {
        e.printStackTrace(); // Log the error
        // Use the static helper from SceneNavigator
        SceneNavigator.showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error", "Failed to load the requested page.");
    }

    // --- Utility to link buttons ---
    // Called from the initialize method of concrete Staff controllers
    protected void setupNavigationButtons(Button viewRooms, Button manageBookings, Button logout) {
        if (viewRooms != null) viewRooms.setOnAction(this::navigateToViewAvailable);
        if (manageBookings != null) manageBookings.setOnAction(this::navigateToManageBookings);
        if (logout != null) logout.setOnAction(this::handleLogoutAction);
        // Add null checks in case a button isn't present on a specific page inheriting this base class
        // (Though typically all navigation buttons would be present on sidebar pages)
    }
}