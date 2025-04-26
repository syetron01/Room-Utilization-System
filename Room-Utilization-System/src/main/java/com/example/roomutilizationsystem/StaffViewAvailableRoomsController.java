package com.example.roomutilizationsystem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Comparator;
import javafx.stage.Stage;
import javafx.scene.Node;
import java.util.Objects; // Import Objects

// Import DataStore, RoomSchedule, Booking, SceneNavigator, StaffBaseController etc.


public class StaffViewAvailableRoomsController extends StaffBaseController {

    // --- Search Criteria ---
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> roomNumberComboBox; // Allow empty for 'any'
    @FXML private ComboBox<String> roomTypeComboBox;   // Allow empty for 'any'
    @FXML private ComboBox<Integer> hoursComboBox; // Booking duration 1, 2, or 3 hrs
    @FXML private Button searchButton;

    // --- Results Table ---
    // Now displays potential slots based on schedules, NOT necessarily available ones initially
    @FXML private TableView<AvailableSlot> availableRoomsTable;

    // Inject Table Columns (Set fx:id in FXML)
    @FXML private TableColumn<AvailableSlot, String> availRoomNoCol;
    @FXML private TableColumn<AvailableSlot, String> availRoomTypeCol;
    @FXML private TableColumn<AvailableSlot, String> availTimeCol;
    @FXML private TableColumn<AvailableSlot, String> availDayCol;
    @FXML private TableColumn<AvailableSlot, Void> availBookCol; // Book button still needs availability check

    // Inject Sidebar Buttons (Set fx:id in FXML)
    @FXML private Button viewAvailableRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button logoutButton;

    // Renamed list to reflect its initial content better
    private ObservableList<AvailableSlot> displayedSlotsList = FXCollections.observableArrayList();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE; // For consistent date formatting

    // Wrapper class still useful to hold schedule+time+duration info
    public static class AvailableSlot {
        private final RoomSchedule schedule;
        private final LocalTime availableStartTime;
        private final Duration duration;

        public AvailableSlot(RoomSchedule schedule, LocalTime availableStartTime, Duration duration) {
            this.schedule = Objects.requireNonNull(schedule, "Schedule cannot be null for AvailableSlot");
            this.availableStartTime = Objects.requireNonNull(availableStartTime, "AvailableStartTime cannot be null for AvailableSlot");
            this.duration = Objects.requireNonNull(duration, "Duration cannot be null for AvailableSlot");
        }

        public RoomSchedule getSchedule() { return schedule; }
        public LocalTime getAvailableStartTime() { return availableStartTime; }
        public Duration getDuration() { return duration; }

        // Properties for TableView binding (remain the same)
        public String getRoomNumber() { return schedule != null ? schedule.getRoomNumber() : ""; }
        public String getRoomType() { return schedule != null ? schedule.getRoomType() : ""; }
        public String getStartTimeDisplay() { return availableStartTime != null ? availableStartTime.format(DateTimeFormatter.ofPattern("h:mm a")) : ""; }
        public String getDayOfWeekDisplay() {
            if (schedule == null || schedule.getDayOfWeek() == null) return "";
            String dayStr = schedule.getDayOfWeek().toString();
            return dayStr.substring(0, 1).toUpperCase() + dayStr.substring(1).toLowerCase();
        }
        public String getFullScheduleTime() {
            return schedule != null ? schedule.getTimeRangeString() : "";
        }
        public String getEndTimeDisplay() {
            return (availableStartTime != null && duration != null) ? availableStartTime.plus(duration).format(DateTimeFormatter.ofPattern("h:mm a")) : "";
        }
        public String getTimeRangeDisplay() {
            String start = getStartTimeDisplay();
            String end = getEndTimeDisplay();
            return (start.isEmpty() || end.isEmpty()) ? "" : start + " - " + end;
        }
    }


    @FXML
    public void initialize() {
        System.out.println("StaffViewAvailableRoomsController initialize() called.");
        setupNavigationButtons(viewAvailableRoomsButton, manageBookingsButton, logoutButton);
        viewAvailableRoomsButton.setDisable(true);

        // --- User Authentication Check ---
        User currentUser = DataStore.getLoggedInUser();
        if (currentUser == null || currentUser.getRole() != UserRole.FACULTY) {
            handleAuthenticationFailure(); // Extracted error handling logic
            return; // Stop initialization
        }

        // --- Setup Search Controls ---
        if (!setupSearchControls()) { // Extracted setup logic
            handleInitializationError("Search controls could not be set up.");
            return; // Stop initialization
        }

        // --- Configure Table ---
        if (!configureAndBindTable()) { // Extracted table setup logic
            handleInitializationError("Table could not be configured.");
            return; // Stop initialization
        }

        // --- Set up DatePicker listener ---
        // Add a listener to the datePicker to reload schedules when the date changes
        if (datePicker != null) {
            datePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    System.out.println("Date changed to: " + newValue + ". Reloading schedules."); // DEBUG
                    loadSchedulesForDate(newValue, false); // Load schedules for the new date, don't filter initially
                }
            });
        }

        // --- Initial Load: Show all schedules for today ---
        System.out.println("Performing initial load for today's schedules.");
        loadSchedulesForDate(LocalDate.now(), false); // Load schedules for today, don't apply filters

        System.out.println("StaffViewAvailableRoomsController initialize() finished.");
    }

    // --- Helper Methods for Initialization ---

    private void handleAuthenticationFailure() {
        SceneNavigator.showAlert(Alert.AlertType.ERROR, "Access Denied", "No authorized faculty user logged in. Returning to login screen.");
        try {
            Stage currentStage = findStage(); // Helper to find stage
            if (currentStage != null) {
                SceneNavigator.navigateTo(currentStage, "/com/example/roomutilizationsystem/fxml/Login.fxml");
            } else {
                System.err.println("Could not obtain Stage for navigation fallback in auth check.");
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not obtain window to load login screen.");
            }
        } catch (IOException e) {
            System.err.println("Failed to navigate to Login screen during auth check fallback: " + e.getMessage());
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to load the login screen.");
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during auth check fallback: " + e.getMessage());
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred during startup check.");
        }
    }

    private Stage findStage() {
        // Try to find the stage from various components
        Node[] potentialNodes = {datePicker, availableRoomsTable, viewAvailableRoomsButton, searchButton};
        for (Node node : potentialNodes) {
            if (node != null && node.getScene() != null && node.getScene().getWindow() instanceof Stage) {
                return (Stage) node.getScene().getWindow();
            }
        }
        System.err.println("Warning: Could not find Stage from known UI components.");
        return null; // Could not find stage
    }

    private boolean setupSearchControls() {
        if (datePicker == null) {
            System.err.println("StaffViewAvailableRoomsController: DatePicker fx:id is null.");
            return false;
        }
        datePicker.setValue(LocalDate.now());
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
                if (isDisabled()) setTooltip(new Tooltip("Cannot select past dates."));
            }
        });

        // Populate ComboBoxes (Room Numbers, Types, Hours)
        populateComboBox(roomNumberComboBox, DataStore.getDistinctRoomNumbers(), "Any");
        populateComboBox(roomTypeComboBox, DataStore.getDistinctRoomTypes(), "Any");
        if (hoursComboBox != null) {
            hoursComboBox.setItems(FXCollections.observableArrayList(1, 2, 3));
            hoursComboBox.setValue(1); // Default duration for search filter
        } else { System.err.println("hoursComboBox is null."); return false; }

        if (searchButton != null) {
            searchButton.setOnAction(this::handleSearchButtonAction);
        } else { System.err.println("searchButton is null."); return false;}

        return true; // Setup successful
    }

    private void populateComboBox(ComboBox<String> comboBox, List<String> items, String defaultValue) {
        if (comboBox != null) {
            comboBox.getItems().clear(); // Clear previous items
            comboBox.getItems().add(defaultValue); // Add "Any" or similar default
            comboBox.getItems().addAll(items);
            comboBox.setValue(defaultValue);
        } else {
            System.err.println("Attempted to populate a null ComboBox.");
        }
    }

    private boolean configureAndBindTable() {
        if (availableRoomsTable == null) {
            System.err.println("availableRoomsTable is null.");
            return false;
        }
        configureAvailableSlotsTableColumns(); // Extracted column setup
        if(availBookCol != null) setupBookActionColumn(availBookCol);
        else { System.err.println("availBookCol is null."); return false; }

        availableRoomsTable.setItems(displayedSlotsList);
        availableRoomsTable.setPlaceholder(new Label("Loading schedules...")); // Initial placeholder
        return true; // Setup successful
    }

    private void handleInitializationError(String message) {
        System.err.println("Initialization Error: " + message);
        SceneNavigator.showAlert(Alert.AlertType.ERROR, "Initialization Error", message);
        // Optionally disable controls or take other actions
        if (searchButton != null) searchButton.setDisable(true);
        if (availableRoomsTable != null) availableRoomsTable.setPlaceholder(new Label("Error during initialization."));
    }


    // --- Core Logic: Load schedules for a specific date ---
    /**
     * Loads schedules into the table for the given date.
     * Does NOT filter by availability or user criteria unless applyFilters is true.
     * @param date The date for which to load schedules.
     * @param applyFilters If true, applies room number, type, and duration filters.
     */
    private void loadSchedulesForDate(LocalDate date, boolean applyFilters) {
        if (date == null) {
            System.err.println("loadSchedulesForDate called with null date.");
            displayedSlotsList.clear();
            if (availableRoomsTable != null) availableRoomsTable.setPlaceholder(new Label("Please select a valid date."));
            return;
        }
        System.out.println("Loading schedules for date: " + date + ", Apply Filters: " + applyFilters); // DEBUG

        displayedSlotsList.clear(); // Clear previous results

        DayOfWeek selectedDay = date.getDayOfWeek();
        String roomNumberFilter = null;
        String roomTypeFilter = null;
        Duration desiredDuration = null;

        // Get filter values ONLY if applyFilters is true
        if (applyFilters) {
            roomNumberFilter = getComboBoxValue(roomNumberComboBox, "Any");
            roomTypeFilter = getComboBoxValue(roomTypeComboBox, "Any");
            Integer hours = hoursComboBox != null ? hoursComboBox.getValue() : null;
            if (hours != null && hours >= 1 && hours <= 3) {
                desiredDuration = Duration.ofHours(hours);
            } else {
                // If filters are applied, duration is mandatory for search
                SceneNavigator.showAlert(Alert.AlertType.WARNING, "Filter Error", "Please select a valid duration (1-3 hours) when searching.");
                if (availableRoomsTable != null) availableRoomsTable.setPlaceholder(new Label("Select duration (1-3 hours) and search again."));
                return;
            }
            System.out.println("Applying filters: Room=" + roomNumberFilter + ", Type=" + roomTypeFilter + ", Duration=" + desiredDuration); // DEBUG
        }

        final String finalRoomNumberFilter = roomNumberFilter;
        final String finalRoomTypeFilter = roomTypeFilter;
        final Duration finalDesiredDuration = desiredDuration;

        // Iterate through all defined schedules
        DataStore.getAllRoomSchedules().stream()
                .filter(s -> s != null && s.getDayOfWeek() == selectedDay) // Filter by Day of Week
                .filter(s -> s.getStartTime() != null && s.getEndTime() != null && s.getEndTime().isAfter(s.getStartTime())) // Ensure valid times
                // Apply user filters IF requested
                .filter(s -> !applyFilters || (finalRoomNumberFilter == null || (s.getRoomNumber() != null && s.getRoomNumber().equalsIgnoreCase(finalRoomNumberFilter))))
                .filter(s -> !applyFilters || (finalRoomTypeFilter == null || (s.getRoomType() != null && s.getRoomType().equalsIgnoreCase(finalRoomTypeFilter))))
                .filter(s -> {
                    if (!applyFilters || finalDesiredDuration == null) {
                        return true; // Don't filter by duration if not searching or duration invalid
                    }
                    try {
                        Duration scheduleDuration = Duration.between(s.getStartTime(), s.getEndTime());
                        return scheduleDuration.equals(finalDesiredDuration); // Exact duration match required for search
                    } catch (Exception e) {
                        System.err.println("Error calculating duration for schedule " + s.getScheduleId() + " during filter: " + e.getMessage());
                        return false;
                    }
                })
                .forEach(schedule -> {
                    try {
                        // Create slot based on schedule's start time and duration
                        Duration duration = Duration.between(schedule.getStartTime(), schedule.getEndTime());
                        if (duration.isNegative() || duration.isZero()) {
                            System.err.println("Skipping schedule " + schedule.getScheduleId() + " due to invalid duration: " + duration);
                            return; // Continue to next schedule
                        }
                        displayedSlotsList.add(new AvailableSlot(schedule, schedule.getStartTime(), duration));
                    } catch (Exception e) {
                        System.err.println("Error creating AvailableSlot for schedule " + schedule.getScheduleId() + ": " + e.getMessage());
                    }
                });


        // Sort the displayed list
        displayedSlotsList.sort(Comparator
                .comparing(AvailableSlot::getRoomNumber)
                .thenComparing(AvailableSlot::getAvailableStartTime)
        );

        System.out.println("Load complete. Displaying " + displayedSlotsList.size() + " schedule slots."); // DEBUG

        // Update placeholder text
        if (availableRoomsTable != null) {
            String mode = applyFilters ? "criteria" : "day " + date.format(dateFormatter);
            if (displayedSlotsList.isEmpty()) {
                availableRoomsTable.setPlaceholder(new Label("No schedules found for the selected " + mode + "."));
            } else {
                // Placeholder usually hidden, but set informative text anyway
                availableRoomsTable.setPlaceholder(new Label("Select a schedule row and click 'Book' (checks availability)."));
            }
        }
    }

    // Helper to get ComboBox value, treating default as null for filtering
    private String getComboBoxValue(ComboBox<String> comboBox, String defaultValue) {
        if (comboBox == null) return null;
        String value = comboBox.getValue();
        if (value == null || value.equals(defaultValue) || value.trim().isEmpty()) {
            return null; // Treat default/empty as no filter
        }
        return value;
    }


    // --- Table Column Configuration ---
    private void configureAvailableSlotsTableColumns() {
        // --- Configuration remains the same, binds AvailableSlot properties to columns ---
        if (availRoomNoCol != null) availRoomNoCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        else System.err.println("availRoomNoCol fx:id is null.");

        if (availRoomTypeCol != null) availRoomTypeCol.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        else System.err.println("availRoomTypeCol fx:id is null.");

        if (availTimeCol != null) {
            availTimeCol.setCellValueFactory(cellData -> {
                AvailableSlot slot = cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(slot != null ? slot.getTimeRangeDisplay() : "");
            });
            // Tooltip logic remains useful
            availTimeCol.setCellFactory(col -> new TableCell<AvailableSlot, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(item);
                    setTooltip(null); // Reset
                    if (!empty && item != null && getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        AvailableSlot slot = getTableView().getItems().get(getIndex());
                        if (slot != null && slot.getSchedule() != null && slot.getDuration() != null) {
                            long hours = slot.getDuration().toHours();
                            long minutes = slot.getDuration().toMinutesPart();
                            String durationStr = "";
                            if (hours > 0) durationStr += hours + " hr" + (hours > 1 ? "s" : "");
                            if (minutes > 0) durationStr += (hours > 0 ? " " : "") + minutes + " min";
                            setTooltip(new Tooltip("Full Schedule: " + slot.getFullScheduleTime() + "\nDuration: " + durationStr));
                        }
                    }
                }
            });
        } else System.err.println("availTimeCol fx:id is null.");

        if (availDayCol != null) availDayCol.setCellValueFactory(new PropertyValueFactory<>("dayOfWeekDisplay"));
        else System.err.println("availDayCol fx:id is null.");
    }

    // --- Book Action Column Setup ---
    private void setupBookActionColumn(TableColumn<AvailableSlot, Void> column) {
        // --- Setup remains the same, but the action handler (handleBookAction) will do the availability check ---
        Callback<TableColumn<AvailableSlot, Void>, TableCell<AvailableSlot, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<AvailableSlot, Void> call(final TableColumn<AvailableSlot, Void> param) {
                final TableCell<AvailableSlot, Void> cell = new TableCell<>() {
                    private final Button btn = new Button("Book");
                    {
                        btn.setStyle("-fx-background-color: #87ceeb;"); // Sky blue
                        btn.setOnAction((ActionEvent event) -> {
                            int index = getIndex();
                            if (!isEmpty() && index >= 0 && index < getTableView().getItems().size()) {
                                AvailableSlot selectedSlot = getTableView().getItems().get(index);
                                if (selectedSlot != null) {
                                    handleBookAction(selectedSlot); // Action now checks availability
                                } else {
                                    System.err.println("Book button clicked on null slot data at index: " + index);
                                    SceneNavigator.showAlert(Alert.AlertType.WARNING, "Selection Error", "Could not get data for the selected row.");
                                }
                            } else {
                                System.err.println("Book button clicked on empty/invalid row index: " + index);
                            }
                        });
                    }
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };
        column.setCellFactory(cellFactory);
    }


    // --- Event Handler for Search Button ---
    @FXML
    private void handleSearchButtonAction(ActionEvent event) {
        LocalDate selectedDate = datePicker != null ? datePicker.getValue() : null;

        // Basic validation for date and duration (required for search)
        if (selectedDate == null) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Please select a date to search.");
            return;
        }
        if (selectedDate.isBefore(LocalDate.now())) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Cannot search for past dates.");
            return;
        }
        Integer hours = hoursComboBox != null ? hoursComboBox.getValue() : null;
        if (hours == null || hours < 1 || hours > 3) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Please select booking duration (1-3 hours) for search.");
            return;
        }

        // Call the loading function WITH filters enabled
        loadSchedulesForDate(selectedDate, true);
    }


    // --- Event Handler for Book Button Click ---
    private void handleBookAction(AvailableSlot slotToBook) {
        if (slotToBook == null || slotToBook.getSchedule() == null || slotToBook.getAvailableStartTime() == null || slotToBook.getDuration() == null) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Booking Error", "Invalid schedule data selected.");
            return;
        }

        User currentUser = DataStore.getLoggedInUser();
        LocalDate selectedDate = datePicker != null ? datePicker.getValue() : null; // Get date from picker

        // --- Pre-Checks ---
        if (currentUser == null || currentUser.getRole() != UserRole.FACULTY) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Booking Error", "User session error. Please log out and log in again.");
            handleAuthenticationFailure(); // Try to navigate to login
            return;
        }
        if (selectedDate == null) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Booking Error", "No date selected in the DatePicker.");
            return;
        }
        if (selectedDate.isBefore(LocalDate.now())) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Booking Failed", "Cannot book rooms in the past.");
            loadSchedulesForDate(selectedDate, false); // Refresh view for the selected (past) date
            return;
        }

        RoomSchedule schedule = slotToBook.getSchedule();
        LocalTime startTime = slotToBook.getAvailableStartTime();
        // Recalculate endTime based on slot's duration, not schedule's end time if they differ (shouldn't now)
        LocalTime endTime = startTime.plus(slotToBook.getDuration());
        Duration bookingDuration = slotToBook.getDuration();

        // --- CRITICAL: Check Availability NOW ---
        if (!DataStore.isTimeSlotAvailable(schedule.getRoomNumber(), selectedDate, startTime, endTime)) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Booking Unavailable",
                    "Sorry, the time slot " + startTime.format(timeFormatter) + " - " + endTime.format(timeFormatter) +
                            " for room " + schedule.getRoomNumber() + " on " + selectedDate.format(dateFormatter) +
                            " is already booked or unavailable.\nPlease refresh or choose another slot.");
            // Refresh the view for the current date to show updated status potentially
            loadSchedulesForDate(selectedDate, false); // Refresh without filters
            return; // Stop booking process
        }

        // --- Confirmation Dialog ---
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Booking");
        confirmation.setHeaderText("Book Room: " + schedule.getRoomNumber() + " (" + schedule.getRoomType() + ")");
        confirmation.setContentText(
                "Date: " + selectedDate.format(dateFormatter) + " (" + selectedDate.getDayOfWeek() + ")\n" +
                        "Time: " + startTime.format(timeFormatter) + " - " + endTime.format(timeFormatter) + "\n" +
                        "Duration: " + bookingDuration.toHours() + " hours\n\n" +
                        "Submit booking request (requires Admin approval)?"
        );

        Optional<ButtonType> result = confirmation.showAndWait();

        // --- Process Confirmation ---
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // --- FINAL Availability Check (Race Condition Mitigation) ---
            if (!DataStore.isTimeSlotAvailable(schedule.getRoomNumber(), selectedDate, startTime, endTime)) {
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Booking Failed", "Sorry, this time slot became unavailable while confirming. Please refresh and try again.");
                loadSchedulesForDate(selectedDate, false); // Refresh without filters
                return;
            }

            // --- Create and Add Booking ---
            Booking newBooking = new Booking(currentUser, schedule, selectedDate, startTime, endTime);
            boolean success = DataStore.addBooking(newBooking); // addBooking performs internal checks

            if (success) {
                SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Booking Submitted",
                        "Your booking request is pending Admin approval.\n" +
                                "Check status in 'Manage Bookings'.");
                // Refresh view for the current date after booking
                loadSchedulesForDate(selectedDate, false); // Refresh without filters
            } else {
                // addBooking failed (e.g., conflict found by DataStore, duration rule violation)
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Booking Failed", "Could not submit request. It might conflict with an approved booking or violate rules. Please refresh and try again.");
                loadSchedulesForDate(selectedDate, false); // Refresh without filters
            }
        } else {
            System.out.println("Booking cancelled by user."); // DEBUG
        }
    }
}