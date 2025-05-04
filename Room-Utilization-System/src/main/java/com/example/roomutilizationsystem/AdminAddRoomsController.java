// FILE: com/example/roomutilizationsystem/AdminAddRoomsController.java
package com.example.roomutilizationsystem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.time.DateTimeException; // Keep this for combineTimeComponents
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AdminAddRoomsController extends AdminBaseController {

    // --- FXML Fields for Recurring Booking Tab ---
    @FXML private TextField roomNumberField;
    @FXML private ComboBox<String> roomTypeComboBox;
    @FXML private ComboBox<Integer> startHourComboBox;
    @FXML private ComboBox<String> startMinuteComboBox;
    @FXML private ComboBox<String> amPmStartComboBox;
    @FXML private ComboBox<Integer> endHourComboBox;
    @FXML private ComboBox<String> endMinuteComboBox;
    @FXML private ComboBox<String> amPmEndComboBox;
    @FXML private CheckBox mondayCheckBox;
    @FXML private CheckBox tuesdayCheckBox;
    @FXML private CheckBox wednesdayCheckBox;
    @FXML private CheckBox thursdayCheckBox;
    @FXML private CheckBox fridayCheckBox;
    @FXML private CheckBox saturdayCheckBox;
    @FXML private CheckBox sundayCheckBox;
    @FXML private DatePicker recurringStartDatePicker; // Definition Start Date
    @FXML private DatePicker recurringEndDatePicker;   // Definition End Date
    @FXML private Button addRoomButton; // Button for recurring tab

    // --- FXML Fields for One Time Booking Tab ---
    @FXML private TextField roomNumberField1;
    @FXML private ComboBox<String> roomTypeComboBox1;
    @FXML private DatePicker oneTimeDatePicker; // Specific Date for one-time definition
    @FXML private ComboBox<Integer> startHourComboBox1;
    @FXML private ComboBox<String> startMinuteComboBox1;
    @FXML private ComboBox<String> amPmStartComboBox1;
    @FXML private ComboBox<Integer> endHourComboBox1;
    @FXML private ComboBox<String> endMinuteComboBox1;
    @FXML private ComboBox<String> amPmEndComboBox1;
    @FXML private Button addRoomButton1; // Button for one-time tab

    // --- FXML Fields for Navigation/Common ---
    @FXML private Button homeButton;
    @FXML private Button addRoomsButton;
    @FXML private Button viewRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button logoutButton;

    // Helper list for checkboxes
    private List<CheckBox> dayCheckBoxes;
    // Date formatter for user messages
    private static final DateTimeFormatter DATE_FORMATTER_FRIENDLY = DateTimeFormatter.ofPattern("MM/dd/yyyy");


    @FXML
    public void initialize() {
        // Defend against NullPointerException if FXML injection fails
        try {
            Objects.requireNonNull(roomNumberField, "roomNumberField FXML ID not injected");
            Objects.requireNonNull(roomTypeComboBox, "roomTypeComboBox FXML ID not injected");
            Objects.requireNonNull(startHourComboBox, "startHourComboBox FXML ID not injected");
            Objects.requireNonNull(startMinuteComboBox, "startMinuteComboBox FXML ID not injected");
            Objects.requireNonNull(amPmStartComboBox, "amPmStartComboBox FXML ID not injected");
            Objects.requireNonNull(endHourComboBox, "endHourComboBox FXML ID not injected");
            Objects.requireNonNull(endMinuteComboBox, "endMinuteComboBox FXML ID not injected");
            Objects.requireNonNull(amPmEndComboBox, "amPmEndComboBox FXML ID not injected");
            Objects.requireNonNull(mondayCheckBox, "mondayCheckBox FXML ID not injected");
            Objects.requireNonNull(tuesdayCheckBox, "tuesdayCheckBox FXML ID not injected");
            Objects.requireNonNull(wednesdayCheckBox, "wednesdayCheckBox FXML ID not injected");
            Objects.requireNonNull(thursdayCheckBox, "thursdayCheckBox FXML ID not injected");
            Objects.requireNonNull(fridayCheckBox, "fridayCheckBox FXML ID not injected");
            Objects.requireNonNull(saturdayCheckBox, "saturdayCheckBox FXML ID not injected");
            Objects.requireNonNull(sundayCheckBox, "sundayCheckBox FXML ID not injected");
            Objects.requireNonNull(recurringStartDatePicker, "recurringStartDatePicker FXML ID not injected");
            Objects.requireNonNull(recurringEndDatePicker, "recurringEndDatePicker FXML ID not injected");
            Objects.requireNonNull(addRoomButton, "addRoomButton FXML ID not injected");

            Objects.requireNonNull(roomNumberField1, "roomNumberField1 FXML ID not injected");
            Objects.requireNonNull(roomTypeComboBox1, "roomTypeComboBox1 FXML ID not injected");
            Objects.requireNonNull(oneTimeDatePicker, "oneTimeDatePicker FXML ID not injected");
            Objects.requireNonNull(startHourComboBox1, "startHourComboBox1 FXML ID not injected");
            Objects.requireNonNull(startMinuteComboBox1, "startMinuteComboBox1 FXML ID not injected");
            Objects.requireNonNull(amPmStartComboBox1, "amPmStartComboBox1 FXML ID not injected");
            Objects.requireNonNull(endHourComboBox1, "endHourComboBox1 FXML ID not injected");
            Objects.requireNonNull(endMinuteComboBox1, "endMinuteComboBox1 FXML ID not injected");
            Objects.requireNonNull(amPmEndComboBox1, "amPmEndComboBox1 FXML ID not injected");
            Objects.requireNonNull(addRoomButton1, "addRoomButton1 FXML ID not injected");

            Objects.requireNonNull(homeButton, "homeButton FXML ID not injected");
            Objects.requireNonNull(addRoomsButton, "addRoomsButton FXML ID not injected");
            Objects.requireNonNull(viewRoomsButton, "viewRoomsButton FXML ID not injected");
            Objects.requireNonNull(manageBookingsButton, "manageBookingsButton FXML ID not injected");
            Objects.requireNonNull(logoutButton, "logoutButton FXML ID not injected");

        } catch (NullPointerException e) {
            System.err.println("FATAL: FXML injection failed in AdminAddRoomsController. Check FXML IDs.");
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Initialization Error", "UI components could not be loaded. Check FXML file and controller bindings.");
            // Disable buttons if init fails partially
            if(addRoomButton != null) addRoomButton.setDisable(true);
            if(addRoomButton1 != null) addRoomButton1.setDisable(true);
            return; // Stop initialization
        }


        setupNavigationButtons(homeButton, addRoomsButton, viewRoomsButton, manageBookingsButton, logoutButton);
        addRoomsButton.setDisable(true); // Disable button for the current view

        // Define shared options for ComboBoxes
        ObservableList<String> roomTypes = FXCollections.observableArrayList("Laboratory", "Lecture");
        ObservableList<Integer> hours = FXCollections.observableArrayList(IntStream.rangeClosed(1, 12).boxed().collect(Collectors.toList()));
        ObservableList<String> minutes = FXCollections.observableArrayList("00", "15", "30", "45");
        ObservableList<String> amPmOptions = FXCollections.observableArrayList("AM", "PM");

        // --- Populate Recurring Tab Components ---
        roomTypeComboBox.setItems(roomTypes);
        startHourComboBox.setItems(hours);
        endHourComboBox.setItems(hours);
        startMinuteComboBox.setItems(minutes);
        endMinuteComboBox.setItems(minutes);
        amPmStartComboBox.setItems(amPmOptions); amPmStartComboBox.setValue("AM");
        amPmEndComboBox.setItems(amPmOptions); amPmEndComboBox.setValue("AM");

        // Group CheckBoxes
        dayCheckBoxes = List.of(mondayCheckBox, tuesdayCheckBox, wednesdayCheckBox, thursdayCheckBox, fridayCheckBox, saturdayCheckBox, sundayCheckBox);

        // Initialize Recurring DatePickers for Definition Range
        recurringStartDatePicker.setDayCellFactory(picker -> new DateCell() { @Override public void updateItem(LocalDate date, boolean empty) { super.updateItem(date, empty); setDisable(empty || date.isBefore(LocalDate.now())); } });
        recurringStartDatePicker.setValue(LocalDate.now());
        recurringEndDatePicker.setDayCellFactory(picker -> new DateCell() { @Override public void updateItem(LocalDate date, boolean empty) { super.updateItem(date, empty); LocalDate startDate = recurringStartDatePicker.getValue(); setDisable(empty || date.isBefore(LocalDate.now()) || (startDate != null && date.isBefore(startDate))); } });
        recurringEndDatePicker.setValue(LocalDate.now().plusWeeks(1));
        recurringStartDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null && recurringEndDatePicker != null) {
                if (recurringEndDatePicker.getValue() == null || recurringEndDatePicker.getValue().isBefore(newDate)) {
                    recurringEndDatePicker.setValue(newDate);
                }
                recurringEndDatePicker.setDayCellFactory(picker -> new DateCell() {
                    @Override public void updateItem(LocalDate date, boolean empty) {
                        super.updateItem(date, empty);
                        LocalDate startDate = recurringStartDatePicker.getValue();
                        setDisable(empty || date.isBefore(LocalDate.now()) || (startDate != null && date.isBefore(startDate)));
                    }
                });
            }
        });

        // --- Populate One Time Tab Components ---
        roomTypeComboBox1.setItems(roomTypes);
        startHourComboBox1.setItems(hours);
        endHourComboBox1.setItems(hours);
        startMinuteComboBox1.setItems(minutes);
        endMinuteComboBox1.setItems(minutes);
        amPmStartComboBox1.setItems(amPmOptions); amPmStartComboBox1.setValue("AM");
        amPmEndComboBox1.setItems(amPmOptions); amPmEndComboBox1.setValue("AM");

        // Initialize One-Time DatePicker
        oneTimeDatePicker.setDayCellFactory(picker -> new DateCell() { @Override public void updateItem(LocalDate date, boolean empty) { super.updateItem(date, empty); setDisable(empty || date.isBefore(LocalDate.now())); } });
        oneTimeDatePicker.setValue(LocalDate.now());

        // Assign actions to the Add buttons
        addRoomButton.setOnAction(this::handleAddRecurringDefinitionButtonAction);
        addRoomButton1.setOnAction(this::handleAddOneTimeDefinitionButtonAction);

        // Clear fields initially
        clearRecurringFields();
        clearOneTimeFields();
    }


    @FXML
    private void handleAddRecurringDefinitionButtonAction(ActionEvent event) {
        // --- Get Data ---
        String roomNumber = roomNumberField.getText().trim();
        String roomType = roomTypeComboBox.getValue();
        Integer startHour = startHourComboBox.getValue();
        String startMinuteStr = startMinuteComboBox.getValue();
        String startAmPm = amPmStartComboBox.getValue();
        Integer endHour = endHourComboBox.getValue();
        String endMinuteStr = endMinuteComboBox.getValue();
        String endAmPm = amPmEndComboBox.getValue();
        Set<DayOfWeek> selectedDays = getSelectedDays();
        LocalDate definitionStartDate = recurringStartDatePicker.getValue();
        LocalDate definitionEndDate = recurringEndDatePicker.getValue();

        // --- Basic Validation ---
        if (roomNumber.isEmpty() || roomType == null || startHour == null || startMinuteStr == null || startAmPm == null || endHour == null || endMinuteStr == null || endAmPm == null || definitionStartDate == null || definitionEndDate == null) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Please fill in all required fields."); return;
        }
        if (selectedDays.isEmpty()) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Please select at least one Day for the recurring schedule definition."); return;
        }
        if (definitionEndDate.isBefore(definitionStartDate)) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Definition End Date cannot be before Start Date."); return;
        }
        if (definitionStartDate.isBefore(LocalDate.now())) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Definition Start Date cannot be in the past."); return;
        }

        // --- Parse and Validate Times & Duration ---
        LocalTime startTime, endTime;
        try {
            startTime = combineTimeComponents(startHour, Integer.parseInt(startMinuteStr), startAmPm, "Start");
            endTime = combineTimeComponents(endHour, Integer.parseInt(endMinuteStr), endAmPm, "End");
            if (!endTime.isAfter(startTime)) {
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", "End time must be after start time.");
                return;
            }
            // *** DURATION VALIDATION ***
            Duration duration = Duration.between(startTime, endTime);
            long minutes = duration.toMinutes();
            // Enforce exact 1, 2, or 3 hour durations
            if (minutes != 60 && minutes != 120 && minutes != 180) {
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", "Schedule duration must be exactly 1, 2, or 3 hours.");
                return;
            }

        } catch (IllegalArgumentException e) { // Catches NumberFormatException and others from combineTimeComponents
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Time Input Error", "Error parsing time: " + e.getMessage());
            return;
        }

        // --- Create and Add Definition ---
        RoomSchedule recurringDefinition = new RoomSchedule(
                roomNumber, roomType, startTime, endTime,
                selectedDays, definitionStartDate, definitionEndDate
        );
        boolean success = DataStore.addRoomScheduleDefinition(recurringDefinition);

        // --- Show Result ---
        if (success) {
            SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Success",
                    "Recurring schedule definition added successfully for Room " + roomNumber + ".\n" +
                            "Details: " + recurringDefinition.getDaysOfWeekDisplay() + ", " +
                            recurringDefinition.getTimeRangeString() + "\n" +
                            "Range: " + recurringDefinition.getDefinitionStartDateDisplay() + " to " + recurringDefinition.getDefinitionEndDateDisplay() + "\n" +
                            "ID: " + recurringDefinition.getScheduleId());
            clearRecurringFields();
        } else {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Schedule Definition Conflict",
                    "Could not add the schedule definition. It overlaps with an existing definition for Room " + roomNumber + ".\n" +
                            "Check existing schedules in 'View Rooms'.");
        }
    }


    @FXML
    private void handleAddOneTimeDefinitionButtonAction(ActionEvent event) {
        // --- Get Data ---
        String roomNumber = roomNumberField1.getText().trim();
        String roomType = roomTypeComboBox1.getValue();
        LocalDate selectedDate = oneTimeDatePicker.getValue();
        Integer startHour = startHourComboBox1.getValue();
        String startMinuteStr = startMinuteComboBox1.getValue();
        String startAmPm = amPmStartComboBox1.getValue();
        Integer endHour = endHourComboBox1.getValue();
        String endMinuteStr = endMinuteComboBox1.getValue();
        String endAmPm = amPmEndComboBox1.getValue();

        // --- Basic Validation ---
        if (roomNumber.isEmpty() || roomType == null || selectedDate == null || startHour == null || startMinuteStr == null || startAmPm == null || endHour == null || endMinuteStr == null || endAmPm == null) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Please fill in all fields for the one-time schedule."); return;
        }
        if (selectedDate.isBefore(LocalDate.now())) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Cannot add availability for a past date."); return;
        }

        // --- Parse and Validate Times & Duration ---
        LocalTime startTime, endTime;
        try {
            startTime = combineTimeComponents(startHour, Integer.parseInt(startMinuteStr), startAmPm, "Start");
            endTime = combineTimeComponents(endHour, Integer.parseInt(endMinuteStr), endAmPm, "End");
            if (!endTime.isAfter(startTime)) {
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", "End time must be after start time.");
                return;
            }
            // *** DURATION VALIDATION ***
            Duration duration = Duration.between(startTime, endTime);
            long minutes = duration.toMinutes();
            // Enforce exact 1, 2, or 3 hour durations
            if (minutes != 60 && minutes != 120 && minutes != 180) {
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", "Schedule duration must be exactly 1, 2, or 3 hours.");
                return;
            }
        } catch (IllegalArgumentException e) { // Catches NumberFormatException and others from combineTimeComponents
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Time Input Error", "Error parsing time: " + e.getMessage());
            return;
        }

        // --- Create and Add Definition ---
        DayOfWeek day = selectedDate.getDayOfWeek();
        Set<DayOfWeek> singleDaySet = EnumSet.of(day);
        RoomSchedule oneTimeDefinition = new RoomSchedule(
                roomNumber, roomType, startTime, endTime,
                singleDaySet, selectedDate, selectedDate // Definition start/end date are the same
        );
        boolean success = DataStore.addRoomScheduleDefinition(oneTimeDefinition);

        // --- Show Result ---
        if (success) {
            DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("h:mm a");
            SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Success",
                    "One-time availability definition added successfully for: " + selectedDate.format(DATE_FORMATTER_FRIENDLY) + "\n" +
                            "Room: " + roomNumber + " (" + roomType + ")\n" +
                            "Time: " + startTime.format(displayFormatter) + " - " + endTime.format(displayFormatter) + "\n" +
                            "ID: " + oneTimeDefinition.getScheduleId());
            clearOneTimeFields();
        } else {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Schedule Definition Conflict",
                    "Could not add the one-time availability definition for " + selectedDate.format(DATE_FORMATTER_FRIENDLY) + ".\n" +
                            "It overlaps with an existing definition for Room " + roomNumber + ".\n" +
                            "Check existing schedules in 'View Rooms'.");
        }
    }

    // --- Helper Methods ---

    private Set<DayOfWeek> getSelectedDays() {
        Set<DayOfWeek> selected = EnumSet.noneOf(DayOfWeek.class);
        if (mondayCheckBox.isSelected()) selected.add(DayOfWeek.MONDAY);
        if (tuesdayCheckBox.isSelected()) selected.add(DayOfWeek.TUESDAY);
        if (wednesdayCheckBox.isSelected()) selected.add(DayOfWeek.WEDNESDAY);
        if (thursdayCheckBox.isSelected()) selected.add(DayOfWeek.THURSDAY);
        if (fridayCheckBox.isSelected()) selected.add(DayOfWeek.FRIDAY);
        if (saturdayCheckBox.isSelected()) selected.add(DayOfWeek.SATURDAY);
        if (sundayCheckBox.isSelected()) selected.add(DayOfWeek.SUNDAY);
        return selected;
    }

    private LocalTime combineTimeComponents(int hour12, int minute, String amPm, String fieldName) throws IllegalArgumentException {
        Objects.requireNonNull(amPm, fieldName + " AM/PM selection is missing.");
        if (hour12 < 1 || hour12 > 12) {
            throw new IllegalArgumentException(fieldName + " hour must be between 1 and 12.");
        }
        if (!List.of(0, 15, 30, 45).contains(minute)) { // Ensure minute is one of the allowed values
            throw new IllegalArgumentException(fieldName + " minute selection (" + minute + ") is invalid. Must be 00, 15, 30, or 45.");
        }

        int hour24 = hour12;
        if ("AM".equalsIgnoreCase(amPm)) { if (hour24 == 12) hour24 = 0; }
        else if ("PM".equalsIgnoreCase(amPm)) { if (hour24 != 12) hour24 += 12; }
        else { throw new IllegalArgumentException("Invalid AM/PM value: " + amPm); }

        try {
            return LocalTime.of(hour24, minute);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Failed to create a valid " + fieldName + " time from " + hour12 + ":" + String.format("%02d", minute) + " " + amPm + ".", e);
        }
    }

    private void clearRecurringFields() {
        if (roomNumberField != null) roomNumberField.clear();
        if (roomTypeComboBox != null) roomTypeComboBox.getSelectionModel().clearSelection();
        if (startHourComboBox != null) startHourComboBox.getSelectionModel().clearSelection();
        if (startMinuteComboBox != null) startMinuteComboBox.getSelectionModel().clearSelection();
        if (amPmStartComboBox != null) amPmStartComboBox.setValue("AM");
        if (endHourComboBox != null) endHourComboBox.getSelectionModel().clearSelection();
        if (endMinuteComboBox != null) endMinuteComboBox.getSelectionModel().clearSelection();
        if (amPmEndComboBox != null) amPmEndComboBox.setValue("AM");
        if (recurringStartDatePicker != null) recurringStartDatePicker.setValue(LocalDate.now());
        if (recurringEndDatePicker != null) recurringEndDatePicker.setValue(LocalDate.now().plusWeeks(1));
        if (dayCheckBoxes != null) { dayCheckBoxes.forEach(cb -> cb.setSelected(false)); }
        if (roomNumberField != null) roomNumberField.requestFocus();
    }

    private void clearOneTimeFields() {
        if (roomNumberField1 != null) roomNumberField1.clear();
        if (roomTypeComboBox1 != null) roomTypeComboBox1.getSelectionModel().clearSelection();
        if (oneTimeDatePicker != null) oneTimeDatePicker.setValue(LocalDate.now());
        if (startHourComboBox1 != null) startHourComboBox1.getSelectionModel().clearSelection();
        if (startMinuteComboBox1 != null) startMinuteComboBox1.getSelectionModel().clearSelection();
        if (amPmStartComboBox1 != null) amPmStartComboBox1.setValue("AM");
        if (endHourComboBox1 != null) endHourComboBox1.getSelectionModel().clearSelection();
        if (endMinuteComboBox1 != null) endMinuteComboBox1.getSelectionModel().clearSelection();
        if (amPmEndComboBox1 != null) amPmEndComboBox1.setValue("AM");
        if (roomNumberField1 != null) roomNumberField1.requestFocus();
    }

}