package com.example.roomutilizationsystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
// Other necessary imports: User, DataStore, SceneNavigator, AdminBaseController

public class AdminHomePageController extends AdminBaseController {

    @FXML private Label greetingLabel;
    // Ensure these fx:ids match the FXML TextAreas
    @FXML private TextArea totalRoomsTextArea;
    @FXML private TextArea labRoomsTextArea;
    @FXML private TextArea lectureRoomsTextArea;

    // Sidebar Buttons - Ensure these fx:ids match the FXML Buttons
    @FXML private Button homeButton;
    @FXML private Button addRoomsButton;
    @FXML private Button viewRoomsButton;
    @FXML private Button manageBookingsButton;

    // Logout Button - Ensure this fx:id matches the main logout button in FXML
    @FXML private Button logoutButton; // Should correspond to the button near the bottom left

    @FXML
    public void initialize() {
        // Setup sidebar navigation, including the logout button
        // This line links the handleLogoutAction from AdminBaseController
        setupNavigationButtons(homeButton, addRoomsButton, viewRoomsButton, manageBookingsButton, logoutButton); // Pass the correct logoutButton reference

        // Disable the button for the current page
        if (homeButton != null) { // Add null check for safety
            homeButton.setDisable(true);
            // Optional: Style the disabled button if needed
            homeButton.setStyle("-fx-background-radius: 100; -fx-background-color: #596572;"); // Example style
            homeButton.setTextFill(javafx.scene.paint.Color.WHITE);
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

        // Ensure TextAreas exist and set text
        if (totalRoomsTextArea != null) totalRoomsTextArea.setText(String.valueOf(total));
        if (labRoomsTextArea != null) labRoomsTextArea.setText(String.valueOf(labs));
        if (lectureRoomsTextArea != null) lectureRoomsTextArea.setText(String.valueOf(lectures));

        // Make text areas non-editable and center text (optional styling)
        configureTextArea(totalRoomsTextArea);
        configureTextArea(labRoomsTextArea);
        configureTextArea(lectureRoomsTextArea);
    }

    // Helper for styling text areas
    private void configureTextArea(TextArea textArea) {
        if (textArea != null) {
            textArea.setEditable(false);
            textArea.setWrapText(true); // Prevent horizontal scrollbars if number is large
            // Consider adding style class in CSS instead for better separation
            // textArea.setStyle("-fx-text-alignment: center; -fx-font-size: 36px;"); // Set alignment and size
            // You might need to adjust padding/font size to ensure numbers fit well.
        }
    }

    // Note: The navigation methods (navigateToHome, navigateToAddRooms, etc.)
    // and handleLogoutAction are inherited from AdminBaseController
}