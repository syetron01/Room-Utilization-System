// FILE: com/example/roomutilizationsystem/StaffViewAvailableRoomsController.java
package com.example.roomutilizationsystem;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory; // Keep for potential future use if needed
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter; // Needed for ComboBox converter

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.DateTimeException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream; // Keep if used elsewhere, not directly here anymore


public class StaffViewAvailableRoomsController extends StaffBaseController {

    // --- Search Criteria ---
    @FXML private DatePicker searchDatePicker;
    @FXML private ComboBox<String> searchRoomNumberComboBox;
    @FXML private ComboBox<String> searchRoomTypeComboBox;
    @FXML private ComboBox<Integer> searchDurationComboBox;
    @FXML private Button searchButton;
    @FXML private Button showAllButton;

    // --- Results Table ---
    @FXML private TableView<RoomSchedule> availableDefinitionsTable;

    // --- Table Columns ---
    @FXML private TableColumn<RoomSchedule, String> availRoomNoCol;
    @FXML private TableColumn<RoomSchedule, String> availRoomTypeCol;
    @FXML private TableColumn<RoomSchedule, String> availDaysCol;
    @FXML private TableColumn<RoomSchedule, String> availTimeCol;
    @FXML private TableColumn<RoomSchedule, String> availDateRangeCol;
    @FXML private TableColumn<RoomSchedule, Void> availBookCol;

    // --- Sidebar Buttons ---
    @FXML private Button viewAvailableRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button logoutButton;

    // --- Data & Formatters ---
    private ObservableList<RoomSchedule> displayedDefinitionsList = FXCollections.observableArrayList();
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter friendlyDateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");


    @FXML
    public void initialize() {
        System.out.println("StaffViewAvailableRoomsController initialize() called.");

        // Null checks for FXML injection
        try {
            Objects.requireNonNull(searchDatePicker, "searchDatePicker FXML ID not injected");
            Objects.requireNonNull(searchRoomNumberComboBox, "searchRoomNumberComboBox FXML ID not injected");
            Objects.requireNonNull(searchRoomTypeComboBox, "searchRoomTypeComboBox FXML ID not injected");
            Objects.requireNonNull(searchDurationComboBox, "searchDurationComboBox FXML ID not injected");
            Objects.requireNonNull(searchButton, "searchButton FXML ID not injected");
            Objects.requireNonNull(showAllButton, "showAllButton FXML ID not injected");
            Objects.requireNonNull(availableDefinitionsTable, "availableDefinitionsTable FXML ID not injected");
            Objects.requireNonNull(availRoomNoCol, "availRoomNoCol FXML ID not injected");
            Objects.requireNonNull(availRoomTypeCol, "availRoomTypeCol FXML ID not injected");
            Objects.requireNonNull(availDaysCol, "availDaysCol FXML ID not injected");
            Objects.requireNonNull(availTimeCol, "availTimeCol FXML ID not injected");
            Objects.requireNonNull(availDateRangeCol, "availDateRangeCol FXML ID not injected");
            Objects.requireNonNull(availBookCol, "availBookCol FXML ID not injected");
            Objects.requireNonNull(viewAvailableRoomsButton, "viewAvailableRoomsButton FXML ID not injected");
            Objects.requireNonNull(manageBookingsButton, "manageBookingsButton FXML ID not injected");
            Objects.requireNonNull(logoutButton, "logoutButton FXML ID not injected");
        } catch (NullPointerException e) {
            System.err.println("FATAL: FXML injection failed in StaffViewAvailableRoomsController. Check FXML IDs.");
            e.printStackTrace();
            handleInitializationError("UI components could not be loaded.");
            return; // Stop initialization
        }

        setupNavigationButtons(viewAvailableRoomsButton, manageBookingsButton, logoutButton);
        viewAvailableRoomsButton.setDisable(true);
        viewAvailableRoomsButton.setStyle("-fx-background-radius: 100; -fx-background-color: #596572; -fx-text-fill: white;");


        User currentUser = DataStore.getLoggedInUser();
        if (currentUser == null || currentUser.getRole() == UserRole.ADMIN) {
            handleAuthenticationFailure();
            return;
        }

        if (!setupSearchControls()) {
            handleInitializationError("Search controls could not be set up.");
            return;
        }

        if (!configureAndBindTable()) {
            handleInitializationError("Results table could not be configured.");
            return;
        }

        // Initial State: Show all active definitions
        handleShowAllActiveButtonAction(null); // Load initial data

        System.out.println("StaffViewAvailableRoomsController initialize() finished successfully.");
    }

    // --- Helper Methods ---
    private void handleAuthenticationFailure() {
        System.err.println("Authentication Failure: Invalid user role or not logged in.");
        SceneNavigator.showAlert(Alert.AlertType.ERROR, "Access Denied", "You do not have permission to access this page or your session expired. Returning to login screen.");
        Stage currentStage = findStage();
        if (currentStage != null) {
            try {
                SceneNavigator.navigateTo(currentStage, "/com/example/roomutilizationsystem/fxml/Login.fxml");
            } catch (IOException e) { System.err.println("CRITICAL: Failed to navigate to Login screen during auth failure fallback: " + e.getMessage()); e.printStackTrace(); SceneNavigator.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the login screen. Please restart the application."); }
            catch (Exception e) { System.err.println("CRITICAL: An unexpected error occurred during auth failure navigation: " + e.getMessage()); e.printStackTrace(); SceneNavigator.showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred trying to return to login. Please restart."); }
        } else { System.err.println("CRITICAL: Could not obtain Stage for navigation fallback during auth failure. UI might be stuck."); SceneNavigator.showAlert(Alert.AlertType.ERROR, "Critical Error", "Cannot return to login screen automatically. Please close and restart the application."); }
    }

    private Stage findStage() {
        Node[] potentialNodes = { searchDatePicker, availableDefinitionsTable, viewAvailableRoomsButton, manageBookingsButton, logoutButton, searchButton, showAllButton, searchRoomNumberComboBox, searchRoomTypeComboBox, searchDurationComboBox };
        for (Node node : potentialNodes) { try { if (node != null && node.getScene() != null && node.getScene().getWindow() instanceof Stage) { return (Stage) node.getScene().getWindow(); } } catch (Exception e) { System.err.println("Minor error finding stage from node (" + (node != null ? node.getId() : "null") + "): " + e.getMessage()); } }
        System.err.println("Warning: Could not find Stage from known UI components in StaffViewAvailableRoomsController."); return null;
    }

    private void handleInitializationError(String message) {
        System.err.println("Initialization Error in StaffViewAvailableRoomsController: " + message);
        SceneNavigator.showAlert(Alert.AlertType.ERROR, "Initialization Error", "Could not initialize the page correctly: " + message + "\nPlease check console logs or contact support.");
        if (searchButton != null) { searchButton.setDisable(true); }
        if (showAllButton != null) { showAllButton.setDisable(true); }
        if (availableDefinitionsTable != null) { availableDefinitionsTable.setPlaceholder(new Label("Error during initialization. Cannot display schedules.")); availableDefinitionsTable.getItems().clear(); availableDefinitionsTable.setDisable(true); }
        if(searchDatePicker != null) searchDatePicker.setDisable(true);
        if(searchRoomNumberComboBox != null) searchRoomNumberComboBox.setDisable(true);
        if(searchRoomTypeComboBox != null) searchRoomTypeComboBox.setDisable(true);
        if(searchDurationComboBox != null) searchDurationComboBox.setDisable(true);
    }


    private boolean setupSearchControls() {
        try {
            // Date Picker - Default to today, but not strictly required for "Show All"
            searchDatePicker.setValue(LocalDate.now());
            searchDatePicker.setDayCellFactory(picker -> new DateCell() { @Override public void updateItem(LocalDate date, boolean empty) { super.updateItem(date, empty); setDisable(empty || date.isBefore(LocalDate.now())); if (isDisabled()) setTooltip(new Tooltip("Cannot select past dates.")); } });

            // Room Number/Type ComboBoxes
            populateComboBox(searchRoomNumberComboBox, DataStore.getDistinctRoomNumbers(), "Any");
            populateComboBox(searchRoomTypeComboBox, DataStore.getDistinctRoomTypes(), "Any");

            // Duration ComboBox Setup
            ObservableList<Integer> durationOptions = FXCollections.observableArrayList();
            durationOptions.add(null); // Represent "Any" duration
            durationOptions.addAll(List.of(1, 2, 3));
            searchDurationComboBox.setItems(durationOptions);
            searchDurationComboBox.setConverter(new StringConverter<Integer>() {
                @Override public String toString(Integer hours) { return (hours == null) ? "Any" : hours.toString(); }
                @Override public Integer fromString(String string) { if ("Any".equalsIgnoreCase(string) || string == null || string.trim().isEmpty()) return null; try { return Integer.parseInt(string.trim()); } catch (NumberFormatException e) { return null; } }
            });
            searchDurationComboBox.setValue(null); // Default to "Any"

            // Search Button
            searchButton.setOnAction(this::handleSearchButtonAction);

            // Assign action to Show All Button
            showAllButton.setOnAction(this::handleShowAllActiveButtonAction);

        } catch (Exception e) {
            System.err.println("Unexpected error during search control setup: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void populateComboBox(ComboBox<String> comboBox, List<String> items, String defaultValue) {
        if (comboBox != null) {
            comboBox.getItems().clear();
            comboBox.getItems().add(defaultValue);
            if (items != null) comboBox.getItems().addAll(items);
            comboBox.setValue(defaultValue);
        } else { System.err.println("Attempted to populate a null String ComboBox."); }
    }

    private boolean configureAndBindTable() {
        try {
            Objects.requireNonNull(availableDefinitionsTable, "availableDefinitionsTable FXML ID missing");
            Objects.requireNonNull(availBookCol, "availBookCol FXML ID missing");
            configureAvailableDefinitionsTableColumns();
            setupBookActionColumn(availBookCol);
            availableDefinitionsTable.setItems(displayedDefinitionsList);
            // Initial placeholder set in initialize after this call
        } catch (NullPointerException npe) {
            System.err.println("Initialization Error: A required TableView or Column FXML ID is missing. " + npe.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected error during table configuration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void configureAvailableDefinitionsTableColumns() {
        availRoomNoCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue() != null ? cellData.getValue().getRoomNumber() : ""));
        availRoomTypeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue() != null ? cellData.getValue().getRoomType() : ""));
        availDaysCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue() != null ? cellData.getValue().getDaysOfWeekDisplay() : ""));
        availTimeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue() != null ? cellData.getValue().getTimeColDisplay() : ""));
        availDateRangeCol.setCellValueFactory(cellData -> {
            RoomSchedule schedule = cellData.getValue();
            if (schedule == null) return new SimpleStringProperty("");
            String startStr = schedule.getDefinitionStartDateDisplay();
            String endStr = schedule.getDefinitionEndDateDisplay();
            return startStr.equals(endStr) ? new SimpleStringProperty(startStr) : new SimpleStringProperty(startStr + " to " + endStr);
        });
        availDateRangeCol.setText("Availability Range");
    }

    private void setupBookActionColumn(TableColumn<RoomSchedule, Void> column) {
        Callback<TableColumn<RoomSchedule, Void>, TableCell<RoomSchedule, Void>> cellFactory = param -> {
            final TableCell<RoomSchedule, Void> cell = new TableCell<>() {
                private final Button btn = new Button("Book This Slot");
                {
                    btn.setStyle("-fx-background-color: #87ceeb; -fx-text-fill: black;");
                    btn.setOnAction((ActionEvent event) -> {
                        int index = getIndex();
                        if (!isEmpty() && index >= 0 && index < getTableView().getItems().size()) {
                            RoomSchedule selectedDefinition = getTableView().getItems().get(index);
                            if (selectedDefinition != null) {
                                handleBookPatternAction(selectedDefinition);
                            } else { System.err.println("Book button clicked on null definition data at index: " + index); SceneNavigator.showAlert(Alert.AlertType.WARNING, "Selection Error", "Could not get data for the selected row."); }
                        } else { System.err.println("Book button clicked on empty/invalid row index: " + index); }
                    });
                }
                @Override
                public void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                    } else {
                        // Enable book button ONLY if a valid future date is selected in the DatePicker
                        LocalDate selectedDate = searchDatePicker.getValue();
                        boolean isDateValid = selectedDate != null && !selectedDate.isBefore(LocalDate.now());
                        btn.setDisable(!isDateValid);
                        setGraphic(btn);
                        // Add tooltip if button is disabled
                        if (!isDateValid) {
                            btn.setTooltip(new Tooltip("Please select a valid future date to enable booking."));
                        } else {
                            btn.setTooltip(null); // Remove tooltip if enabled
                        }
                    }
                }
            };
            return cell;
        };
        column.setCellFactory(cellFactory);
        // Add listener to date picker to refresh table cells (re-evaluates button disable state)
        searchDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (availableDefinitionsTable != null) {
                availableDefinitionsTable.refresh();
            }
        });
    }


    @FXML
    private void handleSearchButtonAction(ActionEvent event) {
        LocalDate searchDate = searchDatePicker.getValue();
        String roomNumberFilter = getComboBoxValue(searchRoomNumberComboBox, "Any");
        String roomTypeFilter = getComboBoxValue(searchRoomTypeComboBox, "Any");
        Integer selectedDurationHours = searchDurationComboBox.getValue(); // null means "Any"

        // Date is REQUIRED for a specific search
        if (searchDate == null) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Please select a date to search for specific availability.");
            return;
        }
        if (searchDate.isBefore(LocalDate.now())) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Cannot search for past dates.");
            return;
        }

        // Perform search for the specific date
        findAndDisplayMatchingDefinitions(searchDate, roomNumberFilter, roomTypeFilter, selectedDurationHours, false); // false = NOT show all active
    }

    @FXML
    private void handleShowAllActiveButtonAction(ActionEvent event) {
        // Reset filters visually (optional but good UX)
        searchDatePicker.setValue(LocalDate.now()); // Reset date picker to today
        searchRoomNumberComboBox.setValue("Any");
        searchRoomTypeComboBox.setValue("Any");
        searchDurationComboBox.setValue(null); // Set back to "Any"

        // Fetch and display all active definitions
        System.out.println("Showing all active schedule definitions...");
        findAndDisplayMatchingDefinitions(null, null, null, null, true); // true = show all active
    }


    private String getComboBoxValue(ComboBox<String> comboBox, String defaultValue) {
        if (comboBox == null) return null;
        String value = comboBox.getValue();
        return (value == null || value.equals(defaultValue) || value.trim().isEmpty()) ? null : value;
    }

    private void findAndDisplayMatchingDefinitions(LocalDate searchDate, String roomNumberFilter, String roomTypeFilter, Integer selectedDurationHours, boolean showAllActive) {

        String logSearchType = showAllActive ? "all active" : "filtered for date " + searchDate;
        System.out.println("Searching (" + logSearchType + ") definitions. Room: " + roomNumberFilter + ", Type: " + roomTypeFilter + ", Duration (hrs): " + selectedDurationHours);
        displayedDefinitionsList.clear();
        LocalDate today = LocalDate.now();

        List<RoomSchedule> matchingDefs = DataStore.getAllRoomSchedules().stream()
                .filter(def -> {
                    if (showAllActive) {
                        // Show All Active: definition's end date must be today or later
                        return !def.getDefinitionEndDate().isBefore(today);
                    } else {
                        // Specific Date Search: must apply on the given searchDate
                        if (searchDate == null) return false; // Should not happen if called from search handler
                        return def.appliesOnDate(searchDate);
                    }
                })
                .filter(def -> roomNumberFilter == null || def.getRoomNumber().equalsIgnoreCase(roomNumberFilter))
                .filter(def -> roomTypeFilter == null || def.getRoomType().equalsIgnoreCase(roomTypeFilter))
                .filter(def -> { // Duration Filter
                    if (selectedDurationHours == null) return true; // "Any" duration
                    try {
                        Duration definitionDuration = Duration.between(def.getStartTime(), def.getEndTime());
                        return !definitionDuration.isNegative() && definitionDuration.toHours() >= selectedDurationHours;
                    } catch (Exception e) { System.err.println("Error calculating duration for schedule " + def.getScheduleId() + ": " + e.getMessage()); return false; }
                })
                .sorted(Comparator.comparing(RoomSchedule::getRoomNumber).thenComparing(RoomSchedule::getStartTime))
                .collect(Collectors.toList());

        displayedDefinitionsList.addAll(matchingDefs);
        System.out.println("Found " + displayedDefinitionsList.size() + " matching definitions.");

        if (availableDefinitionsTable != null) {
            String message;
            if (showAllActive) {
                message = displayedDefinitionsList.isEmpty()
                        ? "No active schedule definitions found in the system."
                        : "Showing all active definitions. Select a date above before clicking 'Book This Slot'.";
            } else { // Specific date search
                message = displayedDefinitionsList.isEmpty()
                        ? "No available schedules found matching your criteria for " + (searchDate != null ? searchDate.format(friendlyDateFormatter) : "the selected date") + "."
                        : "Select a row and click 'Book This Slot' to proceed.";
            }
            availableDefinitionsTable.setPlaceholder(new Label(message));
        }
    }

    // --- Booking Logic ---

    private static class PotentialBookingSlot {
        final LocalDate date; final LocalTime startTime; final LocalTime endTime;
        PotentialBookingSlot(LocalDate d, LocalTime s, LocalTime e) { date = d; startTime = s; endTime = e; }
        @Override public String toString() { return date.format(friendlyDateFormatter) + " " + startTime.format(timeFormatter) + "-" + endTime.format(timeFormatter); }
    }

    private void handleBookPatternAction(RoomSchedule selectedDefinition) {
        User currentUser = DataStore.getLoggedInUser();
        if (currentUser == null) { handleAuthenticationFailure(); return; }

        // Booking requires a specific date from the DatePicker
        LocalDate bookingDate = searchDatePicker.getValue();
        if (bookingDate == null || bookingDate.isBefore(LocalDate.now())) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Booking Error", "Please select a valid future date in the Date picker before booking.");
            return;
        }

        LocalTime bookingStartTime = selectedDefinition.getStartTime();
        LocalTime bookingEndTime = selectedDefinition.getEndTime();

        // Re-verify the selected definition applies on the chosen booking date
        if (!selectedDefinition.appliesOnDate(bookingDate)) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Booking Error",
                    "The selected schedule pattern (" + selectedDefinition.getDaysOfWeekDisplay() + ") is not available on the chosen date: "
                            + bookingDate.format(friendlyDateFormatter) + " (" + bookingDate.getDayOfWeek() +").\nPlease select a different date or schedule.");
            return;
        }

        List<PotentialBookingSlot> slotsToCheck = List.of(new PotentialBookingSlot(bookingDate, bookingStartTime, bookingEndTime));
        List<String> conflicts = new ArrayList<>();
        PotentialBookingSlot slot = slotsToCheck.get(0);

        // Check availability using DataStore
        if (!DataStore.isTimeSlotAvailable(selectedDefinition.getRoomNumber(), slot.date, slot.startTime, slot.endTime)) {
            conflicts.add(slot.toString());
        }

        if (!conflicts.isEmpty()) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Booking Conflict",
                    "This time slot on " + bookingDate.format(friendlyDateFormatter) + " is already booked and unavailable:\n\n"
                            + String.join("\n", conflicts));
        } else {
            // Slot is available - Proceed to Confirmation and Booking
            confirmAndCreateBookings(currentUser, selectedDefinition, slotsToCheck);
        }
    }

    private void confirmAndCreateBookings(User currentUser, RoomSchedule definition, List<PotentialBookingSlot> slotsToBook) {
        String summary;
        if (slotsToBook.isEmpty()){ System.err.println("ConfirmAndCreateBookings called with empty slot list."); return; }
        PotentialBookingSlot slot = slotsToBook.get(0); // Will always be one slot in this flow
        summary = "Date: " + slot.date.format(friendlyDateFormatter) + "\n" +
                "Time: " + slot.startTime.format(timeFormatter) + " - " + slot.endTime.format(timeFormatter);

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Booking Request");
        confirmation.setHeaderText("Submit Booking Request?");
        confirmation.setContentText("Please confirm you want to request the following booking for:\n\n" +
                "Room: " + definition.getRoomNumber() + " (" + definition.getRoomType() + ")\n" +
                summary + "\n\n" +
                "Status will be PENDING until approved by an administrator.");
        confirmation.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            int bookingsCreated = 0;
            int bookingsFailed = 0;
            List<String> failureMessages = new ArrayList<>();

            // Loop runs once for the single slot
            for (PotentialBookingSlot currentSlot : slotsToBook) {
                if (!DataStore.isTimeSlotAvailable(definition.getRoomNumber(), currentSlot.date, currentSlot.startTime, currentSlot.endTime)) {
                    bookingsFailed++; failureMessages.add("Slot unavailable: " + currentSlot.toString());
                    System.err.println("Race condition detected or slot became unavailable for: " + currentSlot.toString()); continue;
                }
                Booking newBooking = new Booking(currentUser, definition, currentSlot.date, currentSlot.startTime, currentSlot.endTime);
                boolean success = DataStore.addBooking(newBooking); // Assumes addBooking saves
                if (success) { bookingsCreated++; }
                else { bookingsFailed++; failureMessages.add("Failed to save: " + currentSlot.toString()); System.err.println("DataStore.addBooking failed for slot: " + currentSlot.toString()); }
            }

            String resultTitle; String resultMessage; Alert.AlertType alertType;
            if (bookingsFailed == 0 && bookingsCreated > 0) { alertType = Alert.AlertType.INFORMATION; resultTitle = "Booking Submitted"; resultMessage = bookingsCreated + " booking request(s) submitted successfully.\nYour bookings are PENDING approval."; }
            else if (bookingsCreated > 0 && bookingsFailed > 0) { alertType = Alert.AlertType.WARNING; resultTitle = "Partial Booking Submission"; resultMessage = bookingsCreated + " booking request(s) submitted.\n" + bookingsFailed + " failed:\n" + String.join("\n", failureMessages); }
            else if (bookingsFailed > 0) { alertType = Alert.AlertType.ERROR; resultTitle = "Booking Failed"; resultMessage = "Could not submit booking request.\nReason(s):\n" + String.join("\n", failureMessages); }
            else { alertType = Alert.AlertType.INFORMATION; resultTitle = "No Action"; resultMessage = "No booking requests were processed."; }
            SceneNavigator.showAlert(alertType, resultTitle, resultMessage);

            // Refresh the view by showing all active again
            handleShowAllActiveButtonAction(null);

        } else { System.out.println("User cancelled booking confirmation."); }
    }

} // End of StaffViewAvailableRoomsController class