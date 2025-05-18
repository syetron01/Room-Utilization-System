package com.example.roomutilizationsystem;

import javafx.fxml.FXML;
import javafx.scene.control.*;
// Other necessary imports: User, DataStore, SceneNavigator, AdminBaseController

public class AdminHomePageController extends AdminBaseController {

    @FXML private Label greetingLabel;
    // Ensure these fx:ids match the FXML TextAreas
    @FXML private Label totalRoomsLabel;
    @FXML private Label labRoomsLabel;
    @FXML private Label lectureRoomsLabel;

    // Sidebar Buttons - Ensure these fx:ids match the FXML Buttons
    @FXML private Button homeButton;
    @FXML private Button addRoomsButton;
    @FXML private Button viewRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button manageScheduleRequestsButton;

    // Logout Button - Ensure this fx:id matches the main logout button in FXML
    @FXML private Button logoutButton; // Should correspond to the button near the bottom left

    @FXML
    public void initialize() {
        // Setup sidebar navigation, including the logout button
        // This line links the handleLogoutAction from AdminBaseController
        setupNavigationButtons(homeButton, addRoomsButton, viewRoomsButton, manageBookingsButton, manageScheduleRequestsButton, logoutButton); // Pass the correct logoutButton reference

        // Disable the button for the current page
        if (homeButton != null) { // Add null check for safety
            homeButton.setDisable(true);
            // Optional: Style the disabled button if needed
            homeButton.setStyle("-fx-background-radius: 100; -fx-background-color: #596572;"); // Example style
            homeButton.setTextFill(javafx.scene.paint.Color.WHITE);
        }

        if (homeButton != null) {
            homeButton.setDisable(true); // Or just style it as active without disabling
            // Apply CSS class or inline style for active state
            homeButton.setStyle("-fx-background-radius: 25; -fx-background-color: #596572; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        }
        // Set greeting
        User loggedInUser = DataStore.getLoggedInUser();
        if (loggedInUser != null && greetingLabel != null) {
            // Capitalize username for display
            String username = loggedInUser.getUsername();
            String displayName = username.substring(0, 1).toUpperCase() + username.substring(1);
            greetingLabel.setText("Hi " + displayName + "!");
        } else if (greetingLabel != null) {
            greetingLabel.setText("Hi Admin!"); // Fallback
        }

        // Load and display stats
        refreshStats();
    }

    private void refreshStats() {
        long total = DataStore.getTotalRoomSchedules();
        long labs = DataStore.countSchedulesByType("Laboratory");
        long lectures = DataStore.countSchedulesByType("Lecture");

        if (totalRoomsLabel != null) totalRoomsLabel.setText(String.valueOf(total));
        if (labRoomsLabel != null) labRoomsLabel.setText(String.valueOf(labs));
        if (lectureRoomsLabel != null) lectureRoomsLabel.setText(String.valueOf(lectures));
    }



    // Note: The navigation methods (navigateToHome, navigateToAddRooms, etc.)
    // and handleLogoutAction are inherited from AdminBaseController
}