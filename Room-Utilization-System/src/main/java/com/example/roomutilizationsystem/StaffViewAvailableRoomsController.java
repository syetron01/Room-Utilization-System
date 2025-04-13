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

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


// Import DataStore, RoomSchedule, Booking, SceneNavigator, StaffBaseController etc.

public class StaffViewAvailableRoomsController extends StaffBaseController {

    // --- Search Criteria ---
    @FXML private DatePicker datePicker; // *** ESSENTIAL: Add this to FXML ***
    @FXML private ComboBox<String> roomNumberComboBox; // Allow empty for 'any'
    @FXML private ComboBox<String> roomTypeComboBox;   // Allow empty for 'any'
    @FXML private ComboBox<Integer> hoursComboBox; // Booking duration 1, 2, or 3 hrs
    // @FXML private ComboBox<DayOfWeek> dayComboBox; // Redundant if DatePicker is used
    @FXML private Button searchButton;

    // --- Results Table ---
    @FXML private TableView<AvailableSlot> availableRoomsTable; // Display AvailableSlot wrapper

    // Inject Table Columns (Set fx:id in FXML)
    @FXML private TableColumn<AvailableSlot, String> availRoomNoCol;
    @FXML private TableColumn<AvailableSlot, String> availRoomTypeCol;
    @FXML private TableColumn<AvailableSlot, String> availTimeCol; // Display available START time
    @FXML private TableColumn<AvailableSlot, String> availDayCol; // Display schedule's DayOfWeek
    @FXML private TableColumn<AvailableSlot, Void> availBookCol; // Book button

    // Inject Sidebar Buttons (Set fx:id in FXML)
    @FXML private Button viewAvailableRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button logoutButton;

    private ObservableList<AvailableSlot> availableSlotsList = FXCollections.observableArrayList();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");

    // Wrapper class to represent a bookable time slot derived from a RoomSchedule
    public static class AvailableSlot {
        private final RoomSchedule schedule;
        private final LocalTime availableStartTime; // The specific start time being offered
        private final Duration duration; // The duration being offered (matches user selection)

        public AvailableSlot(RoomSchedule schedule, LocalTime availableStartTime, Duration duration) {
            this.schedule = schedule;
            this.availableStartTime = availableStartTime;
            this.duration = duration;
        }

        public RoomSchedule getSchedule() { return schedule; }
        public LocalTime getAvailableStartTime() { return availableStartTime; }
        public Duration getDuration() { return duration; }

        // Properties for TableView binding
        public String getRoomNumber() { return schedule.getRoomNumber(); }
        public String getRoomType() { return schedule.getRoomType(); }
        public String getStartTimeDisplay() { return availableStartTime.format(DateTimeFormatter.ofPattern("h:mm a")); }
        public String getDayOfWeekDisplay() {
            String dayStr = schedule.getDayOfWeek().toString();
            return dayStr.substring(0, 1).toUpperCase() + dayStr.substring(1).toLowerCase();
        }
        public String getFullScheduleTime() { // For reference/tooltip maybe
            return schedule.getTimeRangeString();
        }
    }


    @FXML
    public void initialize() {
        setupNavigationButtons(viewAvailableRoomsButton, manageBookingsButton, logoutButton);
        viewAvailableRoomsButton.setDisable(true); // Disable current page button

        // --- Setup Search Controls ---
        if (datePicker == null) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "UI Error", "DatePicker is missing in the FXML. Cannot proceed.");
            // Disable search/booking functionality
            if(searchButton != null) searchButton.setDisable(true);
            if(availableRoomsTable != null) availableRoomsTable.setPlaceholder(new Label("Error: DatePicker missing."));
            return; // Stop initialization
        }

        datePicker.setValue(LocalDate.now()); // Default to today
        // Prevent selecting past dates
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });


        // Populate Room Numbers and Types (allow 'Any')
        List<String> roomNumbers = DataStore.getAllRoomSchedules().stream()
                .map(RoomSchedule::getRoomNumber)
                .distinct().sorted().collect(Collectors.toList());
        roomNumberComboBox.getItems().add("Any");
        roomNumberComboBox.getItems().addAll(roomNumbers);
        roomNumberComboBox.setValue("Any");

        List<String> roomTypes = DataStore.getAllRoomSchedules().stream()
                .map(RoomSchedule::getRoomType)
                .distinct().sorted().collect(Collectors.toList());
        roomTypeComboBox.getItems().add("Any");
        roomTypeComboBox.getItems().addAll(roomTypes);
        roomTypeComboBox.setValue("Any");


        // Populate Hours (1 to 3)
        hoursComboBox.setItems(FXCollections.observableArrayList(1, 2, 3));
        hoursComboBox.setValue(1); // Default to 1 hour

        // --- Configure Available Rooms Table ---
        configureAvailableSlotsTable();
        setupBookActionColumn(availBookCol);

        availableRoomsTable.setItems(availableSlotsList);
        availableRoomsTable.setPlaceholder(new Label("Select criteria and click Search."));
    }


    private void configureAvailableSlotsTable() {
        availRoomNoCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        availRoomTypeCol.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        availTimeCol.setCellValueFactory(new PropertyValueFactory<>("startTimeDisplay")); // Show the specific bookable start time
        availDayCol.setCellValueFactory(new PropertyValueFactory<>("dayOfWeekDisplay")); // Show the schedule's day
        // Optional: Add tooltip showing full schedule range
        availTimeCol.setCellFactory(col -> new TableCell<AvailableSlot, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    AvailableSlot slot = getTableView().getItems().get(getIndex());
                    setTooltip(new Tooltip("Full Schedule: " + slot.getFullScheduleTime()));
                }
            }
        });
    }

    private void setupBookActionColumn(TableColumn<AvailableSlot, Void> column) {
        Callback<TableColumn<AvailableSlot, Void>, TableCell<AvailableSlot, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<AvailableSlot, Void> call(final TableColumn<AvailableSlot, Void> param) {
                final TableCell<AvailableSlot, Void> cell = new TableCell<>() {
                    private final Button btn = new Button("Book");
                    {
                        btn.setStyle("-fx-background-color: #87ceeb;"); // Sky blue
                        btn.setOnAction((ActionEvent event) -> {
                            AvailableSlot selectedSlot = getTableView().getItems().get(getIndex());
                            handleBookAction(selectedSlot);
                        });
                    }
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : btn);
                    }
                };
                return cell;
            }
        };
        column.setCellFactory(cellFactory);
    }


    @FXML
    private void handleSearchButtonAction(ActionEvent event) {
        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate == null) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Please select a date.");
            return;
        }
        DayOfWeek selectedDay = selectedDate.getDayOfWeek();

        String roomNumberFilter = roomNumberComboBox.getValue();
        if ("Any".equals(roomNumberFilter)) roomNumberFilter = null; // Use null for no filter

        String roomTypeFilter = roomTypeComboBox.getValue();
        if ("Any".equals(roomTypeFilter)) roomTypeFilter = null; // Use null for no filter

        Integer hours = hoursComboBox.getValue();
        if (hours == null || hours < 1 || hours > 3) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Please select booking duration (1-3 hours).");
            return;
        }
        Duration desiredDuration = Duration.ofHours(hours);

        availableSlotsList.clear(); // Clear previous results

        // Find schedules matching the basic criteria (day, room, type)
        String finalRoomTypeFilter = roomTypeFilter;
        String finalRoomNumberFilter = roomNumberFilter;
        List<RoomSchedule> potentialSchedules = DataStore.roomSchedules.stream()
                .filter(s -> s.getDayOfWeek() == selectedDay)
                .filter(s -> finalRoomNumberFilter == null || s.getRoomNumber().equalsIgnoreCase(finalRoomNumberFilter))
                .filter(s -> finalRoomTypeFilter == null || s.getRoomType().equalsIgnoreCase(finalRoomTypeFilter))
                .collect(Collectors.toList());


        // For each potential schedule, find concrete available slots of the desired duration
        for (RoomSchedule schedule : potentialSchedules) {
            LocalTime slotStart = schedule.getStartTime();
            LocalTime scheduleEnd = schedule.getEndTime();

            // Iterate through possible start times within the schedule window
            while (!slotStart.plus(desiredDuration).isAfter(scheduleEnd)) {
                LocalTime slotEnd = slotStart.plus(desiredDuration);

                // Check if THIS specific slot [slotStart, slotEnd) is available on selectedDate
                if (DataStore.isTimeSlotAvailable(schedule.getRoomNumber(), selectedDate, slotStart, slotEnd)) {
                    // Add this specific slot to the results table
                    availableSlotsList.add(new AvailableSlot(schedule, slotStart, desiredDuration));
                }

                // Move to the next possible start time (e.g., check every 30 mins, or just after the current slot ends)
                // Simple approach: move to the end of the current potential slot.
                // A more granular approach could check every 15/30 minutes.
                slotStart = slotEnd;
                // Let's try checking every 30 minutes for more options:
                // slotStart = slotStart.plusMinutes(30);
            }
        }


        if (availableSlotsList.isEmpty()) {
            availableRoomsTable.setPlaceholder(new Label("No available slots found for the selected criteria."));
        }
    }


    private void handleBookAction(AvailableSlot slotToBook) {
        User currentUser = DataStore.getLoggedInUser();
        LocalDate selectedDate = datePicker.getValue(); // Re-get the date just in case

        if (currentUser == null || currentUser.getRole() != UserRole.FACULTY || selectedDate == null) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Booking Error", "Cannot proceed with booking. User or Date invalid.");
            return;
        }

        // Confirm booking details
        RoomSchedule schedule = slotToBook.getSchedule();
        LocalTime startTime = slotToBook.getAvailableStartTime();
        LocalTime endTime = startTime.plus(slotToBook.getDuration());

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Booking");
        confirmation.setHeaderText("Book Room: " + schedule.getRoomNumber() + " (" + schedule.getRoomType() + ")");
        confirmation.setContentText(
                "Date: " + selectedDate.format(DateTimeFormatter.ISO_DATE) + " (" + selectedDate.getDayOfWeek() + ")\n" +
                        "Time: " + startTime.format(timeFormatter) + " - " + endTime.format(timeFormatter) + " (" + slotToBook.getDuration().toHours() + " hours)\n\n" +
                        "Submit booking request (requires Admin approval)?"
        );

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Double-check availability right before booking (in case someone else booked it)
            if (!DataStore.isTimeSlotAvailable(schedule.getRoomNumber(), selectedDate, startTime, endTime)) {
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Booking Failed", "Sorry, this time slot was just booked by someone else. Please refresh and try again.");
                // Refresh the search results
                searchButton.fire();
                return;
            }

            // Create the booking (Status: PENDING)
            Booking newBooking = new Booking(currentUser, schedule, selectedDate, startTime, endTime);
            boolean success = DataStore.addBooking(newBooking); // addBooking already checks conflicts, but we double-checked

            if (success) {
                SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Booking Submitted",
                        "Your booking request has been submitted successfully!\n" +
                                "It is now pending Admin approval.\n" +
                                "You can check the status in 'Manage Bookings'.");
                // Refresh the available slots view as this one is now taken (pending)
                searchButton.fire(); // Re-run search to update the table
            } else {
                // This might happen if the conflict check in addBooking finds something missed by the first check
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Booking Failed", "Could not submit booking due to an unexpected conflict. Please refresh and try again.");
                searchButton.fire();
            }
        }
    }
}
