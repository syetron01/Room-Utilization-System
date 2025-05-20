package com.example.roomutilizationsystem;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class StaffViewAvailableRoomsController extends StaffBaseController {

    @FXML private DatePicker searchDatePicker;
    @FXML private ComboBox<String> searchRoomNumberComboBox;
    @FXML private ComboBox<String> searchRoomTypeComboBox;
    @FXML private ComboBox<Integer> searchDurationComboBox;
    @FXML private Button searchButton;
    @FXML private Button showAllButton;

    @FXML private TableView<RoomSchedule> availableDefinitionsTable;

    @FXML private TableColumn<RoomSchedule, String> availRoomNoCol;
    @FXML private TableColumn<RoomSchedule, String> availRoomTypeCol;
    @FXML private TableColumn<RoomSchedule, String> availDaysCol;
    @FXML private TableColumn<RoomSchedule, String> availTimeCol;
    @FXML private TableColumn<RoomSchedule, String> availDateRangeCol;
    @FXML private TableColumn<RoomSchedule, Void> availBookCol;

    @FXML private Button viewAvailableRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button customBookingsButton;
    @FXML private Button logoutButton;

    private ObservableList<RoomSchedule> displayedDefinitionsList = FXCollections.observableArrayList();
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter friendlyDateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");


    @FXML
    public void initialize() {
        System.out.println("StaffViewAvailableRoomsController initialize() called.");

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
            Objects.requireNonNull(customBookingsButton, "customBookingsButton FXML ID not injected");
            Objects.requireNonNull(manageBookingsButton, "manageBookingsButton FXML ID not injected");
            Objects.requireNonNull(logoutButton, "logoutButton FXML ID not injected");
        } catch (NullPointerException e) {
            System.err.println("FATAL: FXML injection failed in StaffViewAvailableRoomsController. Check FXML IDs.");
            e.printStackTrace();
            handleInitializationError("UI components could not be loaded.");
            return;
        }

        setupNavigationButtons(viewAvailableRoomsButton, manageBookingsButton, customBookingsButton, logoutButton);
        if (viewAvailableRoomsButton != null) {
            viewAvailableRoomsButton.setDisable(true);
            viewAvailableRoomsButton.setStyle("-fx-background-radius: 100; -fx-background-color: #596572; -fx-text-fill: white;");
        }

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
        handleShowAllActiveButtonAction(null);
        System.out.println("StaffViewAvailableRoomsController initialize() finished successfully.");
    }

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
            searchDatePicker.setValue(LocalDate.now());
            searchDatePicker.setDayCellFactory(picker -> new DateCell() { @Override public void updateItem(LocalDate date, boolean empty) { super.updateItem(date, empty); setDisable(empty || date.isBefore(LocalDate.now())); if (isDisabled()) setTooltip(new Tooltip("Cannot select past dates.")); } });

            populateComboBox(searchRoomNumberComboBox, DataStore.getDistinctRoomNumbers(), "Any");
            populateComboBox(searchRoomTypeComboBox, DataStore.getDistinctRoomTypes(), "Any");

            ObservableList<Integer> durationOptions = FXCollections.observableArrayList();
            durationOptions.add(null);
            durationOptions.addAll(List.of(1, 2, 3));
            searchDurationComboBox.setItems(durationOptions);
            searchDurationComboBox.setConverter(new StringConverter<>() {
                @Override public String toString(Integer hours) { return (hours == null) ? "Any" : hours.toString(); }
                @Override public Integer fromString(String string) { if ("Any".equalsIgnoreCase(string) || string == null || string.trim().isEmpty()) return null; try { return Integer.parseInt(string.trim()); } catch (NumberFormatException e) { return null; } }
            });
            searchDurationComboBox.setValue(null);

            searchButton.setOnAction(this::handleSearchButtonAction);
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
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        LocalDate selectedDate = searchDatePicker.getValue();
                        boolean isDateValid = selectedDate != null && !selectedDate.isBefore(LocalDate.now());
                        btn.setDisable(!isDateValid);
                        setGraphic(btn);
                        if (!isDateValid) {
                            btn.setTooltip(new Tooltip("Please select a valid future date to enable booking."));
                        } else {
                            btn.setTooltip(null);
                        }
                    }
                }
            };
            return cell;
        };
        column.setCellFactory(cellFactory);
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
        Integer selectedDurationHours = searchDurationComboBox.getValue();

        if (searchDate == null) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Please select a date to search for specific availability.");
            return;
        }
        if (searchDate.isBefore(LocalDate.now())) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Input Error", "Cannot search for past dates.");
            return;
        }
        findAndDisplayMatchingDefinitions(searchDate, roomNumberFilter, roomTypeFilter, selectedDurationHours, false);
    }

    @FXML
    private void handleShowAllActiveButtonAction(ActionEvent event) {
        searchDatePicker.setValue(LocalDate.now());
        searchRoomNumberComboBox.setValue("Any");
        searchRoomTypeComboBox.setValue("Any");
        searchDurationComboBox.setValue(null);
        System.out.println("Showing all active schedule definitions...");
        findAndDisplayMatchingDefinitions(null, null, null, null, true);
    }

    private String getComboBoxValue(ComboBox<String> comboBox, String defaultValue) {
        if (comboBox == null) return null;
        String value = comboBox.getValue();
        return (value == null || value.equals(defaultValue) || value.trim().isEmpty()) ? null : value;
    }

    private void findAndDisplayMatchingDefinitions(LocalDate searchDate, String roomNumberFilter, String roomTypeFilter, Integer selectedDurationHours, boolean showAllActive) {
        String logSearchType = showAllActive ? "all active" : "filtered for date " + (searchDate != null ? searchDate.format(friendlyDateFormatter) : "N/A");
        System.out.println("Searching (" + logSearchType + ") definitions. Room: " + roomNumberFilter + ", Type: " + roomTypeFilter + ", Duration (hrs): " + selectedDurationHours);
        displayedDefinitionsList.clear();
        LocalDate today = LocalDate.now();

        List<RoomSchedule> matchingDefs = DataStore.getAllRoomSchedules().stream()
                .filter(RoomSchedule::isActive) // FILTER FOR ACTIVE SCHEDULES
                .filter(def -> {
                    if (showAllActive) {
                        return !def.getDefinitionEndDate().isBefore(today);
                    } else {
                        if (searchDate == null) return false;
                        return def.appliesOnDate(searchDate);
                    }
                })
                .filter(def -> roomNumberFilter == null || def.getRoomNumber().equalsIgnoreCase(roomNumberFilter))
                .filter(def -> roomTypeFilter == null || def.getRoomType().equalsIgnoreCase(roomTypeFilter))
                .filter(def -> {
                    if (selectedDurationHours == null) return true;
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
            } else {
                message = displayedDefinitionsList.isEmpty()
                        ? "No available schedules found matching your criteria for " + (searchDate != null ? searchDate.format(friendlyDateFormatter) : "the selected date") + "."
                        : "Select a row and click 'Book This Slot' to proceed.";
            }
            availableDefinitionsTable.setPlaceholder(new Label(message));
        }
    }

    private static class PotentialBookingSlot {
        final LocalDate date; final LocalTime startTime; final LocalTime endTime;
        PotentialBookingSlot(LocalDate d, LocalTime s, LocalTime e) { date = d; startTime = s; endTime = e; }
        @Override public String toString() { return date.format(friendlyDateFormatter) + " " + startTime.format(timeFormatter) + "-" + endTime.format(timeFormatter); }
    }

    private void handleBookPatternAction(RoomSchedule selectedDefinition) {
        User currentUser = DataStore.getLoggedInUser();
        if (currentUser == null) { handleAuthenticationFailure(); return; }

        LocalDate bookingDate = searchDatePicker.getValue();
        if (bookingDate == null || bookingDate.isBefore(LocalDate.now())) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Booking Error", "Please select a valid future date in the Date picker before booking.");
            return;
        }

        LocalTime bookingStartTime = selectedDefinition.getStartTime();
        LocalTime bookingEndTime = selectedDefinition.getEndTime();

        if (!selectedDefinition.appliesOnDate(bookingDate)) { // appliesOnDate now checks isActive
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Booking Error",
                    "The selected schedule pattern (" + selectedDefinition.getDaysOfWeekDisplay() + ") is not available on the chosen date: "
                            + bookingDate.format(friendlyDateFormatter) + " (" + bookingDate.getDayOfWeek() +").\nPlease select a different date or schedule, or this schedule may be inactive.");
            return;
        }

        PotentialBookingSlot slot = new PotentialBookingSlot(bookingDate, bookingStartTime, bookingEndTime);

        if (!DataStore.isTimeSlotAvailable(selectedDefinition.getRoomNumber(), slot.date, slot.startTime, slot.endTime)) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Booking Conflict",
                    "This time slot on " + bookingDate.format(friendlyDateFormatter) + " is already booked and unavailable:\n\n" + slot.toString());
        } else {
            confirmAndCreateBookings(currentUser, selectedDefinition, List.of(slot));
        }
    }

    private void confirmAndCreateBookings(User currentUser, RoomSchedule definition, List<PotentialBookingSlot> slotsToBook) {
        if (slotsToBook.isEmpty()){ System.err.println("ConfirmAndCreateBookings called with empty slot list."); return; }
        PotentialBookingSlot slot = slotsToBook.get(0);
        String summary = "Date: " + slot.date.format(friendlyDateFormatter) + "\n" +
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

            for (PotentialBookingSlot currentSlot : slotsToBook) {
                // Re-check DataStore.findCoveringScheduleDefinition which considers isActive
                Optional<RoomSchedule> coveringDef = DataStore.findCoveringScheduleDefinition(definition.getRoomNumber(), currentSlot.date, currentSlot.startTime);
                if (coveringDef.isEmpty() || !currentSlot.endTime.isAfter(currentSlot.startTime) || currentSlot.endTime.isAfter(coveringDef.get().getEndTime())) {
                    bookingsFailed++; failureMessages.add("Slot no longer available or schedule inactive: " + currentSlot.toString());
                    System.err.println("Slot became unavailable or schedule inactive for: " + currentSlot.toString()); continue;
                }

                if (!DataStore.isTimeSlotAvailable(definition.getRoomNumber(), currentSlot.date, currentSlot.startTime, currentSlot.endTime)) {
                    bookingsFailed++; failureMessages.add("Slot unavailable (booked by another): " + currentSlot.toString());
                    System.err.println("Race condition or slot became booked for: " + currentSlot.toString()); continue;
                }

                Booking newBooking = new Booking(currentUser, definition, currentSlot.date, currentSlot.startTime, currentSlot.endTime);
                boolean success = DataStore.addBooking(newBooking);
                if (success) { bookingsCreated++; }
                else { bookingsFailed++; failureMessages.add("Failed to save: " + currentSlot.toString()); System.err.println("DataStore.addBooking failed for slot: " + currentSlot.toString()); }
            }

            String resultTitle; String resultMessage; Alert.AlertType alertType;
            if (bookingsFailed == 0 && bookingsCreated > 0) { alertType = Alert.AlertType.INFORMATION; resultTitle = "Booking Submitted"; resultMessage = bookingsCreated + " booking request(s) submitted successfully.\nYour bookings are PENDING approval."; }
            else if (bookingsCreated > 0 && bookingsFailed > 0) { alertType = Alert.AlertType.WARNING; resultTitle = "Partial Booking Submission"; resultMessage = bookingsCreated + " booking request(s) submitted.\n" + bookingsFailed + " failed:\n" + String.join("\n", failureMessages); }
            else if (bookingsFailed > 0) { alertType = Alert.AlertType.ERROR; resultTitle = "Booking Failed"; resultMessage = "Could not submit booking request.\nReason(s):\n" + String.join("\n", failureMessages); }
            else { alertType = Alert.AlertType.INFORMATION; resultTitle = "No Action"; resultMessage = "No booking requests were processed."; }
            SceneNavigator.showAlert(alertType, resultTitle, resultMessage);

            handleShowAllActiveButtonAction(null);
        } else { System.out.println("User cancelled booking confirmation."); }
    }
}