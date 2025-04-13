package com.example.roomutilizationsystem;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

// Import DataStore, RoomSchedule, SceneNavigator, AdminBaseController, etc.

public class AdminAddRoomsController extends AdminBaseController { // Inherit base functionality

    @FXML private TextField roomNumberField;
    @FXML private ComboBox<String> roomTypeComboBox;
    @FXML private TextField startTimeField; // e.g., "9:30"
    @FXML private TextField endTimeField;   // e.g., "11:00"
    @FXML private ComboBox<String> amPmStartComboBox; // Consider removing/replacing
    @FXML private ComboBox<String> amPmEndComboBox;   // Consider removing/replacing
    @FXML private ComboBox<DayOfWeek> dayComboBox;
    @FXML private Button addRoomButton;

    // Inject Sidebar Buttons (add fx:id in FXML)
    @FXML private Button homeButton;
    @FXML private Button addRoomsButton; // This page's button
    @FXML private Button viewRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button logoutButton; // Assumed fx:id in FXML

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm"); // 24-hour format for input

    @FXML
    public void initialize() {
        // Setup sidebar navigation
        setupNavigationButtons(homeButton, addRoomsButton, viewRoomsButton, manageBookingsButton, logoutButton);
        addRoomsButton.setDisable(true); // Disable button for current page

        // Populate ComboBoxes
        roomTypeComboBox.setItems(FXCollections.observableArrayList("Laboratory", "Lecture", "Meeting Room")); // Add more types if needed
        dayComboBox.setItems(FXCollections.observableArrayList(DayOfWeek.values())); // All days

        // --- Handling Time Input (Simplified to 24-hour format) ---
        // The UI with separate time field and AM/PM combo is awkward.
        // Recommendation: Use a single TextField expecting HH:mm (24h) or use a dedicated TimePicker control.
        // For this example, we'll parse HH:mm from startTimeField and endTimeField.
        // Remove the AM/PM combo boxes or hide them if sticking to this FXML.
        startTimeField.setPromptText("HH:mm (e.g., 13:00)");
        endTimeField.setPromptText("HH:mm (e.g., 14:30)");

        // Clear fields initially
        clearFields();
    }

    @FXML
    private void handleAddRoomButtonAction(ActionEvent event) {
        String roomNumber = roomNumberField.getText().trim();
        String roomType = roomTypeComboBox.getValue();
        String startTimeStr = startTimeField.getText().trim();
        String endTimeStr = endTimeField.getText().trim();
        DayOfWeek day = dayComboBox.getValue();

        // --- Validation ---
        if (roomNumber.isEmpty() || roomType == null || startTimeStr.isEmpty() || endTimeStr.isEmpty() || day == null) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Please fill in all fields.");
            return;
        }

        LocalTime startTime, endTime;
        try {
            startTime = LocalTime.parse(startTimeStr, timeFormatter);
            endTime = LocalTime.parse(endTimeStr, timeFormatter);
        } catch (DateTimeParseException e) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", "Invalid time format. Please use HH:mm (e.g., 09:00 or 14:30).");
            return;
        }

        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", "End time must be after start time.");
            return;
        }

        // --- Create and Add Schedule ---
        RoomSchedule newSchedule = new RoomSchedule(roomNumber, roomType, startTime, endTime, day);
        DataStore.addRoomSchedule(newSchedule);

        SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Success", "Room schedule added successfully:\n" + newSchedule);
        clearFields();
    }

    private void clearFields() {
        roomNumberField.clear();
        roomTypeComboBox.getSelectionModel().clearSelection();
        startTimeField.clear();
        endTimeField.clear();
        dayComboBox.getSelectionModel().clearSelection();
    }
}
