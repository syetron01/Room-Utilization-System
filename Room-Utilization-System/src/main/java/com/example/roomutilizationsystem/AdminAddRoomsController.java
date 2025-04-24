package com.example.roomutilizationsystem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    // Day ComboBox replaced with CheckBoxes
    // @FXML private ComboBox<DayOfWeek> dayComboBox; // Removed
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
        setupNavigationButtons(homeButton, addRoomsButton, viewRoomsButton, manageBookingsButton, logoutButton);
        addRoomsButton.setDisable(true);

        // Populate Room Types
        roomTypeComboBox.setItems(FXCollections.observableArrayList("Laboratory", "Lecture", "Meeting Room"));

        // Populate Time ComboBoxes (as before)
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
        if (selectedDays.size() > 3) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Please select a maximum of 3 days.");
            return;
        }


        LocalTime startTime;
        LocalTime endTime;
        try {
            int startMinute = Integer.parseInt(startMinuteStr);
            int endMinute = Integer.parseInt(endMinuteStr);
            startTime = combineTimeComponents(startHour, startMinute, startAmPm, "Start");
            endTime = combineTimeComponents(endHour, endMinute, endAmPm, "End");

            if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
                if(startAmPm.equals("PM") && endAmPm.equals("AM")) {
                    SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", "End time cannot be on the next day.");
                } else {
                    SceneNavigator.showAlert(Alert.AlertType.ERROR, "Input Error", "End time must be after start time.");
                }
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

        // --- Create and Add Schedule(s) ---
        int schedulesAdded = 0;
        List<String> addedDaysList = new ArrayList<>(); // For success message
        try {
            // Iterate through the selected days and add a schedule for each
            for (DayOfWeek day : selectedDays) {
                RoomSchedule newSchedule = new RoomSchedule(roomNumber, roomType, startTime, endTime, day);
                DataStore.addRoomSchedule(newSchedule); // Add to data store
                schedulesAdded++;
                addedDaysList.add(day.toString().substring(0, 1).toUpperCase() + day.toString().substring(1).toLowerCase());
            }

            if (schedulesAdded > 0) {
                DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("h:mm a");
                String daysString = String.join(", ", addedDaysList);
                SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Success",
                        schedulesAdded + " schedule(s) added successfully for: " + daysString + "\n" +
                                "Room: " + roomNumber + " (" + roomType + ")\n" +
                                "Time: " + startTime.format(displayFormatter) + " - " + endTime.format(displayFormatter)
                );
                clearFields();
            } else {
                // Should not happen if validation passed, but good to have
                SceneNavigator.showAlert(Alert.AlertType.WARNING, "No Schedule Added", "No schedules were added. Please check input.");
            }

        } catch (Exception e) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Save Error", "Could not save the schedule(s): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets a list of selected DayOfWeek values from the checkboxes.
     */
    private List<DayOfWeek> getSelectedDays() {
        List<DayOfWeek> selected = new ArrayList<>();
        if (mondayCheckBox.isSelected()) selected.add(DayOfWeek.MONDAY);
        if (tuesdayCheckBox.isSelected()) selected.add(DayOfWeek.TUESDAY);
        if (wednesdayCheckBox.isSelected()) selected.add(DayOfWeek.WEDNESDAY);
        if (thursdayCheckBox.isSelected()) selected.add(DayOfWeek.THURSDAY);
        if (fridayCheckBox.isSelected()) selected.add(DayOfWeek.FRIDAY);
        if (saturdayCheckBox.isSelected()) selected.add(DayOfWeek.SATURDAY);
        if (sundayCheckBox.isSelected()) selected.add(DayOfWeek.SUNDAY);
        return selected;
    }


    // combineTimeComponents method (Keep as before)
    private LocalTime combineTimeComponents(int hour12, int minute, String amPm, String fieldName) throws IllegalArgumentException {
        if (hour12 < 1 || hour12 > 12) throw new IllegalArgumentException(fieldName + " hour selection is invalid.");
        if (minute < 0 || minute > 59) throw new IllegalArgumentException(fieldName + " minute selection is invalid.");
        int hour24 = hour12;
        if ("AM".equalsIgnoreCase(amPm)) { if (hour24 == 12) hour24 = 0; }
        else if ("PM".equalsIgnoreCase(amPm)) { if (hour24 != 12) hour24 += 12; }
        else throw new IllegalArgumentException("Invalid AM/PM selection for " + fieldName + " time.");
        try { return LocalTime.of(hour24, minute); }
        catch (Exception e) { throw new IllegalArgumentException("Failed to create valid " + fieldName + " time from selections."); }
    }


    private void clearFields() {
        roomNumberField.clear();
        roomTypeComboBox.getSelectionModel().clearSelection();
        startHourComboBox.getSelectionModel().clearSelection();
        startMinuteComboBox.getSelectionModel().clearSelection();
        amPmStartComboBox.setValue("AM");
        endHourComboBox.getSelectionModel().clearSelection();
        endMinuteComboBox.getSelectionModel().clearSelection();
        amPmEndComboBox.setValue("AM");

        // Uncheck all day checkboxes
        if (dayCheckBoxes != null) { // Add null check for safety
            for (CheckBox cb : dayCheckBoxes) {
                cb.setSelected(false);
            }
        }

        roomNumberField.requestFocus();
    }
}