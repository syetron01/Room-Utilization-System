package com.example.roomutilizationsystem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration; // Import Duration
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Objects; // Import Objects for requireNonNull

public class AdminAddRoomsController extends AdminBaseController {

    // --- FXML Fields ---
    @FXML private TextField roomNumberField;
    @FXML private ComboBox<String> roomTypeComboBox;
    @FXML private ComboBox<Integer> startHourComboBox;
    @FXML private ComboBox<String> startMinuteComboBox;
    @FXML private ComboBox<String> amPmStartComboBox;
    @FXML private ComboBox<Integer> endHourComboBox;
    @FXML private ComboBox<String> endMinuteComboBox;
    @FXML private ComboBox<String> amPmEndComboBox;

    // Day CheckBoxes
    @FXML private CheckBox mondayCheckBox;
    @FXML private CheckBox tuesdayCheckBox;
    @FXML private CheckBox wednesdayCheckBox;
    @FXML private CheckBox thursdayCheckBox;
    @FXML private CheckBox fridayCheckBox;
    @FXML private CheckBox saturdayCheckBox;
    @FXML private CheckBox sundayCheckBox;

    @FXML private Button addRoomButton;
    @FXML private Button homeButton;
    @FXML private Button addRoomsButton;
    @FXML private Button viewRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button logoutButton;

    // Helper list for checkboxes
    private List<CheckBox> dayCheckBoxes;


    @FXML
    public void initialize() {
        // Call superclass method to set up navigation
        setupNavigationButtons(homeButton, addRoomsButton, viewRoomsButton, manageBookingsButton, logoutButton);
        addRoomsButton.setDisable(true); // Disable button for the current page

        // Populate Room Types
        roomTypeComboBox.setItems(FXCollections.observableArrayList("Laboratory", "Lecture", "Meeting Room"));

        // Populate Time ComboBoxes
        ObservableList<Integer> hours = FXCollections.observableArrayList(
                IntStream.rangeClosed(1, 12).boxed().collect(Collectors.toList())
        );
        startHourComboBox.setItems(hours);
        endHourComboBox.setItems(hours);
        ObservableList<String> minutes = FXCollections.observableArrayList("00", "15", "30", "45");
        startMinuteComboBox.setItems(minutes);
        endMinuteComboBox.setItems(minutes);
        ObservableList<String> amPmOptions = FXCollections.observableArrayList("AM", "PM");
        amPmStartComboBox.setItems(amPmOptions);
        amPmEndComboBox.setItems(amPmOptions);

        // Initialize the list of checkboxes for easier iteration
        dayCheckBoxes = List.of(
                mondayCheckBox, tuesdayCheckBox, wednesdayCheckBox, thursdayCheckBox,
                fridayCheckBox, saturdayCheckBox, sundayCheckBox
        );

        clearFields(); // Clear fields and set defaults
    }


    @FXML
    private void handleAddRoomButtonAction(ActionEvent event) {
        String roomNumber = roomNumberField.getText().trim();
        String roomType = roomTypeComboBox.getValue();
        Integer startHour = startHourComboBox.getValue();
        String startMinuteStr = startMinuteComboBox.getValue();
        String startAmPm = amPmStartComboBox.getValue();
        Integer endHour = endHourComboBox.getValue();
        String endMinuteStr = endMinuteComboBox.getValue();
        String endAmPm = amPmEndComboBox.getValue();

        // --- Collect Selected Days ---
        List<DayOfWeek> selectedDays = getSelectedDays();

        // --- Validation ---
        // Basic field validation
        if (roomNumber.isEmpty() || roomType == null ||
                startHour == null || startMinuteStr == null || startAmPm == null ||
                endHour == null || endMinuteStr == null || endAmPm == null)
        {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Please fill in all fields (Room, Type, Start Time, End Time).");
            return;
        }
        // Day selection validation
        if (selectedDays.isEmpty()) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Please select at least one day.");
            return;
        }
        // Removed the > 3 days limit as it's unusual for availability

        LocalTime startTime;
        LocalTime endTime;
        try {
            int startMinute = Integer.parseInt(startMinuteStr);
            int endMinute = Integer.parseInt(endMinuteStr);
            startTime = combineTimeComponents(startHour, startMinute, startAmPm, "Start");
            endTime = combineTimeComponents(endHour, endMinute, endAmPm, "End");

            if (!endTime.isAfter(startTime)) { // Use isAfter for strict check
                // Check if the user intended next day (e.g. 10 PM to 2 AM) - the current time parser doesn't handle this
                if(startAmPm.equalsIgnoreCase("PM") && endAmPm.equalsIgnoreCase("AM")) {
                    SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", "Schedules cannot span across midnight/to the next day.");
                } else {
                    SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", "End time must be after start time.");
                }
                return;
            }

            // Basic check: schedule duration >= 15 mins
            if (Duration.between(startTime, endTime).toMinutes() < 15) {
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", "Schedule duration must be at least 15 minutes.");
                return;
            }


        } catch (NumberFormatException e){
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", "Invalid minute format selected.");
            return;
        } catch (IllegalArgumentException e) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", e.getMessage());
            return;
        } catch (Exception e) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", "An unexpected error occurred processing time.");
            e.printStackTrace();
            return;
        }

        // --- Attempt to Create and Add Schedule(s) with Conflict Check ---
        int schedulesAdded = 0;
        List<String> addedDaysList = new ArrayList<>(); // For success message
        List<String> conflictDaysList = new ArrayList<>(); // For conflict message

        for (DayOfWeek day : selectedDays) {
            // Create a NEW schedule object for each day
            RoomSchedule newSchedule = new RoomSchedule(roomNumber, roomType, startTime, endTime, day);
            // Use the updated DataStore method that includes conflict checking
            boolean success = DataStore.addRoomSchedule(newSchedule);

            if (success) {
                schedulesAdded++;
                // Get display name of the day
                addedDaysList.add(day.toString().substring(0, 1).toUpperCase() + day.toString().substring(1).toLowerCase());
            } else {
                // Get display name of the day
                conflictDaysList.add(day.toString().substring(0, 1).toUpperCase() + day.toString().substring(1).toLowerCase());
            }
        }

        // --- Show Results ---
        if (schedulesAdded > 0) {
            DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("h:mm a");
            String daysString = String.join(", ", addedDaysList);
            SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Success",
                    schedulesAdded + " schedule(s) added successfully for: " + daysString + "\n" +
                            "Room: " + roomNumber + " (" + roomType + ")\n" +
                            "Time: " + startTime.format(displayFormatter) + " - " + endTime.format(displayFormatter)
            );
            clearFields(); // Clear fields only on overall success or partial success
        }

        if (!conflictDaysList.isEmpty()) {
            String conflictDaysString = String.join(", ", conflictDaysList);
            // If any schedules were added, show a warning alongside the success.
            // If NO schedules were added but there were conflicts, show an error.
            Alert.AlertType alertType = (schedulesAdded > 0) ? Alert.AlertType.WARNING : Alert.AlertType.ERROR;
            String headerText = (schedulesAdded > 0) ? "Partial Success / Schedule Conflict" : "Schedule Conflict Error";
            String message = "Could not add schedule for the following day(s) due to overlap with an existing schedule:\n" +
                    conflictDaysString + "\n" +
                    "Room: " + roomNumber + " (" + roomType + ")";

            SceneNavigator.showAlert(alertType, headerText, message);
            // Don't clear fields if there was a conflict, allows user to adjust and retry.
            if (schedulesAdded == 0) { // If NO schedules added, don't clear
                // Fields already not cleared in this path, so no action needed.
            } else { // If SOME schedules added, fields were cleared above. Re-populate conflicted days if needed?
                // For simplicity, we clear fields on any success. If conflicts happened, user needs to re-enter anyway.
            }
        }

        // Fallback for unexpected cases (though validation should prevent this)
        if (schedulesAdded == 0 && conflictDaysList.isEmpty() && !selectedDays.isEmpty()) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Operation Failed", "An unexpected error occurred. No schedules were added or reported as conflicts.");
        }
    }

    /**
     * Gets a list of selected DayOfWeek values from the checkboxes.
     */
    private List<DayOfWeek> getSelectedDays() {
        List<DayOfWeek> selected = new ArrayList<>();
        // Add DayOfWeek enum values based on checkbox selection
        if (mondayCheckBox != null && mondayCheckBox.isSelected()) selected.add(DayOfWeek.MONDAY);
        if (tuesdayCheckBox != null && tuesdayCheckBox.isSelected()) selected.add(DayOfWeek.TUESDAY);
        if (wednesdayCheckBox != null && wednesdayCheckBox.isSelected()) selected.add(DayOfWeek.WEDNESDAY);
        if (thursdayCheckBox != null && thursdayCheckBox.isSelected()) selected.add(DayOfWeek.THURSDAY);
        if (fridayCheckBox != null && fridayCheckBox.isSelected()) selected.add(DayOfWeek.FRIDAY);
        if (saturdayCheckBox != null && saturdayCheckBox.isSelected()) selected.add(DayOfWeek.SATURDAY);
        if (sundayCheckBox != null && sundayCheckBox.isSelected()) selected.add(DayOfWeek.SUNDAY);
        return selected;
    }


    // combineTimeComponents method (Keep as before, improved error messages)
    private LocalTime combineTimeComponents(int hour12, int minute, String amPm, String fieldName) throws IllegalArgumentException {
        Objects.requireNonNull(amPm, fieldName + " AM/PM selection is missing.");
        if (hour12 < 1 || hour12 > 12) throw new IllegalArgumentException(fieldName + " hour selection is invalid: " + hour12);
        if (minute < 0 || minute > 59) throw new IllegalArgumentException(fieldName + " minute selection is invalid: " + minute); // Should not happen with combo box

        int hour24 = hour12;
        if ("AM".equalsIgnoreCase(amPm)) {
            if (hour24 == 12) hour24 = 0; // 12 AM is 00:xx in 24hr
        }
        else if ("PM".equalsIgnoreCase(amPm)) {
            if (hour24 != 12) hour24 += 12; // 1 PM is 13:xx, ..., 11 PM is 23:xx
        }
        else throw new IllegalArgumentException("Invalid AM/PM selection for " + fieldName + " time: " + amPm);

        try {
            // Check if the combined time is valid (e.g., LocalTime.of(24, 0) is invalid)
            return LocalTime.of(hour24, minute);
        }
        catch (java.time.DateTimeException e) {
            throw new IllegalArgumentException("Failed to create valid " + fieldName + " time from selections: " + hour12 + ":" + String.format("%02d", minute) + " " + amPm);
        }
    }


    private void clearFields() {
        roomNumberField.clear();
        roomTypeComboBox.getSelectionModel().clearSelection();
        startHourComboBox.getSelectionModel().clearSelection();
        startMinuteComboBox.getSelectionModel().clearSelection();
        amPmStartComboBox.setValue("AM"); // Set default
        endHourComboBox.getSelectionModel().clearSelection();
        endMinuteComboBox.getSelectionModel().clearSelection();
        amPmEndComboBox.setValue("AM"); // Set default

        // Uncheck all day checkboxes
        if (dayCheckBoxes != null) {
            for (CheckBox cb : dayCheckBoxes) {
                if (cb != null) cb.setSelected(false);
            }
        }

        roomNumberField.requestFocus();
    }
}