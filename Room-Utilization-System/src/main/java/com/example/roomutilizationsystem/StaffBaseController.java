package com.example.roomutilizationsystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.io.IOException;
// Import SceneNavigator, DataStore

public abstract class StaffBaseController {

    // --- Navigation Methods ---
    protected void navigateToViewAvailable(ActionEvent event) {
        try { SceneNavigator.navigateTo(event, "/fxml/StaffViewAvailableRooms.fxml"); } catch (IOException e) { handleNavError(e); }
    }

    protected void navigateToManageBookings(ActionEvent event) {
        try { SceneNavigator.navigateTo(event, "/fxml/StaffManageBookings.fxml"); } catch (IOException e) { handleNavError(e); }
    }

    // --- Logout ---
    @FXML // Make sure the logout button in FXML calls this
    protected void handleLogoutAction(ActionEvent event) {
        DataStore.logout(); // Clear the logged-in user
        try {
            SceneNavigator.navigateTo(event, "/fxml/Login.fxml"); // Go back to login
        } catch (IOException e) {
            handleNavError(e);
        }
    }

    private void handleNavError(IOException e) {
        e.printStackTrace(); // Log the error
        SceneNavigator.showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error", "Failed to load the requested page.");
    }

    // --- Utility to link buttons ---
    protected void setupNavigationButtons(Button viewRooms, Button manageBookings, Button logout) {
        if (viewRooms != null) viewRooms.setOnAction(this::navigateToViewAvailable);
        if (manageBookings != null) manageBookings.setOnAction(this::navigateToManageBookings);
        if (logout != null) logout.setOnAction(this::handleLogoutAction);
    }
}
