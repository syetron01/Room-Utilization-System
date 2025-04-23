package com.example.roomutilizationsystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.io.IOException;
// Import SceneNavigator

// Common methods that would exist in each Admin Controller
public abstract class AdminBaseController {

    // --- Navigation Methods ---
    protected void navigateToHome(ActionEvent event) {
        try { SceneNavigator.navigateTo(event, "fxml/adminHome.fxml"); } catch (IOException e) { handleNavError(e); }
    }

    protected void navigateToAddRooms(ActionEvent event) {
        try { SceneNavigator.navigateTo(event, "fxml/adminAddRooms.fxml"); } catch (IOException e) { handleNavError(e); }
    }

    protected void navigateToViewRooms(ActionEvent event) {
        try { SceneNavigator.navigateTo(event, "fxml/adminViewRooms.fxml"); } catch (IOException e) { handleNavError(e); }
    }

    protected void navigateToManageBookings(ActionEvent event) {
        try { SceneNavigator.navigateTo(event, "fxml/adminManageBookings.fxml"); } catch (IOException e) { handleNavError(e); }
    }

    // --- Logout ---
    @FXML // Make sure the logout button in FXML calls this
    protected void handleLogoutAction(ActionEvent event) {
        DataStore.logout(); // Clear the logged-in user
        try {
            SceneNavigator.navigateTo(event, "fxml/Login.fxml"); // Go back to login
        } catch (IOException e) {
            handleNavError(e);
        }
    }

    private void handleNavError(IOException e) {
        e.printStackTrace(); // Log the error
        SceneNavigator.showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error", "Failed to load the requested page.");
    }

    // --- Utility to link buttons ---
    // In each Admin controller's initialize(), you'd call methods like this
    // e.g., setupNavigationButtons(homeButton, addRoomsButton, viewRoomsButton, manageBookingsButton, logoutButton);
    protected void setupNavigationButtons(Button home, Button add, Button view, Button manage, Button logout) {
        if (home != null) home.setOnAction(this::navigateToHome);
        if (add != null) add.setOnAction(this::navigateToAddRooms);
        if (view != null) view.setOnAction(this::navigateToViewRooms);
        if (manage != null) manage.setOnAction(this::navigateToManageBookings);
        if (logout != null) logout.setOnAction(this::handleLogoutAction);
        // Add null checks in case a button isn't present on a specific page
    }
}
