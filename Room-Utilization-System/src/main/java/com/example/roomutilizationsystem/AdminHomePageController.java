package com.example.roomutilizationsystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

// Import DataStore, SceneNavigator, AdminBaseController etc.

public class AdminHomePageController extends AdminBaseController {

    @FXML private Label greetingLabel; // Optional: fx:id="greetingLabel"
    @FXML private TextArea totalRoomsTextArea; // fx:id="totalRoomsTextArea"
    @FXML private TextArea labRoomsTextArea;   // fx:id="labRoomsTextArea"
    @FXML private TextArea lectureRoomsTextArea; // fx:id="lectureRoomsTextArea"

    // Inject Sidebar Buttons
    @FXML private Button homeButton;
    @FXML private Button addRoomsButton;
    @FXML private Button viewRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button logoutButton;

    @FXML
    public void initialize() {
        // Setup sidebar navigation
        setupNavigationButtons(homeButton, addRoomsButton, viewRoomsButton, manageBookingsButton, logoutButton);
        homeButton.setDisable(true); // Disable button for current page

        // Set greeting
        User loggedInUser = DataStore.getLoggedInUser();
        if (loggedInUser != null && greetingLabel != null) {
            greetingLabel.setText("Hi " + loggedInUser.getUsername() + "!"); // Or just "Hi Admin!"
        } else if (greetingLabel != null) {
            greetingLabel.setText("Hi Admin!");
        }


        // Load and display stats
        refreshStats();
    }

    private void refreshStats() {
        long total = DataStore.getTotalRoomSchedules();
        long labs = DataStore.countSchedulesByType("Laboratory");
        long lectures = DataStore.countSchedulesByType("Lecture");

        // Ensure TextAreas have fx:id set in FXML
        if (totalRoomsTextArea != null) totalRoomsTextArea.setText(String.valueOf(total));
        if (labRoomsTextArea != null) labRoomsTextArea.setText(String.valueOf(labs));
        if (lectureRoomsTextArea != null) lectureRoomsTextArea.setText(String.valueOf(lectures));

        // Make text areas non-editable if they are just for display
        if (totalRoomsTextArea != null) totalRoomsTextArea.setEditable(false);
        if (labRoomsTextArea != null) labRoomsTextArea.setEditable(false);
        if (lectureRoomsTextArea != null) lectureRoomsTextArea.setEditable(false);
    }
}
