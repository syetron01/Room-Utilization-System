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
import java.time.DateTimeException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StaffCustomBookingsController extends StaffBaseController {

    // --- FXML Fields for Recurring Booking Request Tab ---
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
    @FXML private DatePicker recurringStartDatePicker;
    @FXML private DatePicker recurringEndDatePicker;
    @FXML private Button submitRecurringRequestButton;

    // --- FXML Fields for One Time Booking Request Tab ---
    @FXML private TextField roomNumberField1;
    @FXML private ComboBox<String> roomTypeComboBox1;
    @FXML private DatePicker oneTimeDatePicker;
    @FXML private ComboBox<Integer> startHourComboBox1;
    @FXML private ComboBox<String> startMinuteComboBox1;
    @FXML private ComboBox<String> amPmStartComboBox1;
    @FXML private ComboBox<Integer> endHourComboBox1;
    @FXML private ComboBox<String> endMinuteComboBox1;
    @FXML private ComboBox<String> amPmEndComboBox1;
    @FXML private Button submitOneTimeRequestButton;

    // --- FXML Fields for Navigation/Common (from StaffBaseController) ---
    @FXML private Button viewAvailableRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button customBookingsButton; // This page's button
    @FXML private Button logoutButton;

    private List<CheckBox> dayCheckBoxes;
    private static final DateTimeFormatter DATE_FORMATTER_FRIENDLY = DateTimeFormatter.ofPattern("MM/dd/yyyy");


    @FXML
    public void initialize() {
        // (Null checks remain the same as before)
        try {
            Objects.requireNonNull(roomNumberField, "roomNumberField not injected");
            Objects.requireNonNull(roomTypeComboBox, "roomTypeComboBox not injected");
            Objects.requireNonNull(startHourComboBox, "startHourComboBox not injected");
            Objects.requireNonNull(startMinuteComboBox, "startMinuteComboBox not injected");
            Objects.requireNonNull(amPmStartComboBox, "amPmStartComboBox not injected");
            Objects.requireNonNull(endHourComboBox, "endHourComboBox not injected");
            Objects.requireNonNull(endMinuteComboBox, "endMinuteComboBox not injected");
            Objects.requireNonNull(amPmEndComboBox, "amPmEndComboBox not injected");
            Objects.requireNonNull(mondayCheckBox, "mondayCheckBox not injected");
            Objects.requireNonNull(tuesdayCheckBox, "tuesdayCheckBox not injected");
            Objects.requireNonNull(wednesdayCheckBox, "wednesdayCheckBox not injected");
            Objects.requireNonNull(thursdayCheckBox, "thursdayCheckBox not injected");
            Objects.requireNonNull(fridayCheckBox, "fridayCheckBox not injected");
            Objects.requireNonNull(saturdayCheckBox, "saturdayCheckBox not injected");
            Objects.requireNonNull(sundayCheckBox, "sundayCheckBox not injected");
            Objects.requireNonNull(recurringStartDatePicker, "recurringStartDatePicker not injected");
            Objects.requireNonNull(recurringEndDatePicker, "recurringEndDatePicker not injected");
            Objects.requireNonNull(submitRecurringRequestButton, "submitRecurringRequestButton not injected");

            Objects.requireNonNull(roomNumberField1, "roomNumberField1 not injected");
            Objects.requireNonNull(roomTypeComboBox1, "roomTypeComboBox1 not injected");
            Objects.requireNonNull(oneTimeDatePicker, "oneTimeDatePicker not injected");
            Objects.requireNonNull(startHourComboBox1, "startHourComboBox1 not injected");
            Objects.requireNonNull(startMinuteComboBox1, "startMinuteComboBox1 not injected");
            Objects.requireNonNull(amPmStartComboBox1, "amPmStartComboBox1 not injected");
            Objects.requireNonNull(endHourComboBox1, "endHourComboBox1 not injected");
            Objects.requireNonNull(endMinuteComboBox1, "endMinuteComboBox1 not injected");
            Objects.requireNonNull(amPmEndComboBox1, "amPmEndComboBox1 not injected");
            Objects.requireNonNull(submitOneTimeRequestButton, "submitOneTimeRequestButton not injected");

            Objects.requireNonNull(viewAvailableRoomsButton, "viewAvailableRoomsButton not injected");
            Objects.requireNonNull(manageBookingsButton, "manageBookingsButton not injected");
            Objects.requireNonNull(customBookingsButton, "customBookingsButton not injected");
            Objects.requireNonNull(logoutButton, "logoutButton not injected");
        } catch (NullPointerException e) {
            System.err.println("FATAL: FXML injection failed in StaffCustomBookingsController: " + e.getMessage());
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Initialization Error", "UI components could not be loaded.");
            if(submitRecurringRequestButton != null) submitRecurringRequestButton.setDisable(true);
            if(submitOneTimeRequestButton != null) submitOneTimeRequestButton.setDisable(true);
            return;
        }

        setupNavigationButtons(viewAvailableRoomsButton, manageBookingsButton, customBookingsButton, logoutButton);
        if (customBookingsButton != null) {
            customBookingsButton.setDisable(true);
            customBookingsButton.setStyle("-fx-background-radius: 100; -fx-background-color: #596572; -fx-text-fill: white;");
        }

        ObservableList<String> roomTypes = FXCollections.observableArrayList("Laboratory", "Lecture");
        ObservableList<Integer> hours = FXCollections.observableArrayList(IntStream.rangeClosed(1, 12).boxed().collect(Collectors.toList()));
        ObservableList<String> minutes = FXCollections.observableArrayList("00", "15", "30", "45");
        ObservableList<String> amPmOptions = FXCollections.observableArrayList("AM", "PM");

        roomTypeComboBox.setItems(roomTypes);
        startHourComboBox.setItems(hours); endHourComboBox.setItems(hours);
        startMinuteComboBox.setItems(minutes); endMinuteComboBox.setItems(minutes);
        amPmStartComboBox.setItems(amPmOptions); amPmStartComboBox.setValue("AM");
        amPmEndComboBox.setItems(amPmOptions); amPmEndComboBox.setValue("AM");
        dayCheckBoxes = List.of(mondayCheckBox, tuesdayCheckBox, wednesdayCheckBox, thursdayCheckBox, fridayCheckBox, saturdayCheckBox, sundayCheckBox);
        recurringStartDatePicker.setDayCellFactory(picker -> new DateCell() { @Override public void updateItem(LocalDate date, boolean empty) { super.updateItem(date, empty); setDisable(empty || date.isBefore(LocalDate.now())); } });
        recurringStartDatePicker.setValue(LocalDate.now());
        recurringEndDatePicker.setDayCellFactory(picker -> new DateCell() { @Override public void updateItem(LocalDate date, boolean empty) { super.updateItem(date, empty); LocalDate startDate = recurringStartDatePicker.getValue(); setDisable(empty || date.isBefore(LocalDate.now()) || (startDate != null && date.isBefore(startDate))); } });
        recurringEndDatePicker.setValue(LocalDate.now().plusWeeks(1));
        recurringStartDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null && recurringEndDatePicker.getValue() != null && recurringEndDatePicker.getValue().isBefore(newDate)) {
                recurringEndDatePicker.setValue(newDate);
            }
            recurringEndDatePicker.setDayCellFactory(picker -> new DateCell() {
                @Override public void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    LocalDate startDate = recurringStartDatePicker.getValue();
                    setDisable(empty || item.isBefore(LocalDate.now()) || (startDate != null && item.isBefore(startDate)));
                }
            });
        });

        roomTypeComboBox1.setItems(roomTypes);
        startHourComboBox1.setItems(hours); endHourComboBox1.setItems(hours);
        startMinuteComboBox1.setItems(minutes); endMinuteComboBox1.setItems(minutes);
        amPmStartComboBox1.setItems(amPmOptions); amPmStartComboBox1.setValue("AM");
        amPmEndComboBox1.setItems(amPmOptions); amPmEndComboBox1.setValue("AM");
        oneTimeDatePicker.setDayCellFactory(picker -> new DateCell() { @Override public void updateItem(LocalDate date, boolean empty) { super.updateItem(date, empty); setDisable(empty || date.isBefore(LocalDate.now())); } });
        oneTimeDatePicker.setValue(LocalDate.now());

        submitRecurringRequestButton.setOnAction(this::handleSubmitRecurringRequestButtonAction);
        submitOneTimeRequestButton.setOnAction(this::handleSubmitOneTimeRequestButtonAction);

        clearRecurringFields();
        clearOneTimeFields();
    }

    @FXML
    private void handleSubmitRecurringRequestButtonAction(ActionEvent event) {
        User currentUser = DataStore.getLoggedInUser();
        if (currentUser == null) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Authentication Error", "User not logged in.");
            return;
        }

        String roomNumber = roomNumberField.getText().trim();
        String roomType = roomTypeComboBox.getValue();
        LocalTime startTime, endTime;
        Set<DayOfWeek> selectedDays = getSelectedDays();
        LocalDate definitionStartDate = recurringStartDatePicker.getValue();
        LocalDate definitionEndDate = recurringEndDatePicker.getValue();

        if (roomNumber.isEmpty() || roomType == null || selectedDays.isEmpty() || definitionStartDate == null || definitionEndDate == null) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Please fill all fields: Room No, Type, select Day(s), and Date Range.");
            return;
        }
        if (definitionEndDate.isBefore(definitionStartDate)) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "End Date cannot be before Start Date."); return;
        }
        if (definitionStartDate.isBefore(LocalDate.now())) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Start Date cannot be in the past."); return;
        }

        try {
            startTime = combineTimeComponents(startHourComboBox.getValue(), Integer.parseInt(startMinuteComboBox.getValue()), amPmStartComboBox.getValue(), "Start");
            endTime = combineTimeComponents(endHourComboBox.getValue(), Integer.parseInt(endMinuteComboBox.getValue()), amPmEndComboBox.getValue(), "End");
            if (!endTime.isAfter(startTime)) {
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", "End time must be after start time."); return;
            }
            Duration duration = Duration.between(startTime, endTime);
            long minutes = duration.toMinutes();
            if (minutes < 15 || minutes > 180 * 2) { // Allow longer schedule definitions, e.g., up to 6 hours
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", "Proposed schedule duration must be between 15 minutes and 6 hours."); return;
            }
        } catch (NumberFormatException e) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", "Please select valid start and end times (Hour, Minute, AM/PM)."); return;
        } catch (IllegalArgumentException e) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Time Input Error", e.getMessage()); return;
        }

        // Create ONE ScheduleRequest for the entire recurring pattern
        DataStore.ScheduleRequest scheduleProposal = new DataStore.ScheduleRequest(
                currentUser.getUsername(), roomNumber, roomType,
                startTime, endTime, selectedDays,
                definitionStartDate, definitionEndDate
        );

        if (DataStore.addScheduleRequest(scheduleProposal)) {
            SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Schedule Proposal Submitted",
                    "Your recurring schedule proposal has been submitted successfully.\n" +
                            "It is PENDING admin approval.\nDetails: Room " + roomNumber + ", " +
                            scheduleProposal.getDaysOfWeekDisplay() + ", " + scheduleProposal.getTimeRangeString() + "\n" +
                            "For dates: " + definitionStartDate.format(DATE_FORMATTER_FRIENDLY) + " to " + definitionEndDate.format(DATE_FORMATTER_FRIENDLY));
            clearRecurringFields();
        } else {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Submission Failed",
                    "Could not submit the schedule proposal. An unexpected error occurred.");
        }
    }

    @FXML
    private void handleSubmitOneTimeRequestButtonAction(ActionEvent event) {
        User currentUser = DataStore.getLoggedInUser();
        if (currentUser == null) { /* Handle not logged in */ return; }

        String roomNumber = roomNumberField1.getText().trim();
        String roomType = roomTypeComboBox1.getValue();
        LocalDate selectedDate = oneTimeDatePicker.getValue();
        LocalTime startTime, endTime;

        if (roomNumber.isEmpty() || roomType == null || selectedDate == null) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Please fill all fields: Room No, Type, and select a Date.");
            return;
        }
        if (selectedDate.isBefore(LocalDate.now())) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Cannot propose a schedule for a past date."); return;
        }

        try {
            startTime = combineTimeComponents(startHourComboBox1.getValue(), Integer.parseInt(startMinuteComboBox1.getValue()), amPmStartComboBox1.getValue(), "Start");
            endTime = combineTimeComponents(endHourComboBox1.getValue(), Integer.parseInt(endMinuteComboBox1.getValue()), amPmEndComboBox1.getValue(), "End");
            if (!endTime.isAfter(startTime)) {
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", "End time must be after start time."); return;
            }
            Duration duration = Duration.between(startTime, endTime);
            long minutes = duration.toMinutes();
            if (minutes < 15 || minutes > 180 * 2) { // Allow longer schedule definitions
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", "Proposed schedule duration must be between 15 minutes and 6 hours."); return;
            }
        } catch (NumberFormatException e) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", "Please select valid start and end times (Hour, Minute, AM/PM)."); return;
        } catch (IllegalArgumentException e) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Time Input Error", e.getMessage()); return;
        }

        // For a one-time schedule proposal, the definition start and end dates are the same (selectedDate)
        // and daysOfWeek will contain only the DayOfWeek of selectedDate.
        Set<DayOfWeek> dayForOneTime = EnumSet.of(selectedDate.getDayOfWeek());
        DataStore.ScheduleRequest scheduleProposal = new DataStore.ScheduleRequest(
                currentUser.getUsername(), roomNumber, roomType,
                startTime, endTime, dayForOneTime,
                selectedDate, selectedDate // Start and End date are the same for one-time
        );

        if (DataStore.addScheduleRequest(scheduleProposal)) {
            SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Schedule Proposal Submitted",
                    "Your one-time schedule proposal for " + selectedDate.format(DATE_FORMATTER_FRIENDLY) +
                            " (" + selectedDate.getDayOfWeek().toString().substring(0,1) + selectedDate.getDayOfWeek().toString().substring(1).toLowerCase() + ")" +
                            " from " + startTime.format(DateTimeFormatter.ofPattern("h:mm a")) + " to " + endTime.format(DateTimeFormatter.ofPattern("h:mm a")) +
                            " has been submitted.\nIt is PENDING admin approval.");
            clearOneTimeFields();
        } else {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Submission Failed",
                    "Could not submit the one-time schedule proposal. An unexpected error occurred.");
        }
    }

    // Helper methods (getSelectedDays, combineTimeComponents, clearRecurringFields, clearOneTimeFields)
    // remain the same as in the previous version of StaffCustomBookingsController.
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

    private LocalTime combineTimeComponents(Integer hour12, int minute, String amPm, String fieldName) throws IllegalArgumentException {
        if (hour12 == null || amPm == null) {
            throw new IllegalArgumentException(fieldName + " time is incomplete. Please select Hour and AM/PM.");
        }
        if (hour12 < 1 || hour12 > 12) {
            throw new IllegalArgumentException(fieldName + " hour must be between 1 and 12.");
        }
        if (!List.of(0, 15, 30, 45).contains(minute)) {
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