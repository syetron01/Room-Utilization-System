package com.example.roomutilizationsystem;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory; // Make sure this is imported
import javafx.util.Callback;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class StaffManageBookingsController extends StaffBaseController {

    // --- TableViews ---
    // Ensure fx:id="pendingBookingsTable" in FXML
    @FXML private TableView<Booking> pendingBookingsTable;
    // Ensure fx:id="approvedBookingsTable" in FXML
    @FXML private TableView<Booking> approvedBookingsTable;

    // --- Columns for Pending Table (Ensure fx:id matches variable name in FXML) ---
    @FXML private TableColumn<Booking, String> penRoomNoCol;
    @FXML private TableColumn<Booking, String> penRoomTypeCol;
    @FXML private TableColumn<Booking, String> penTimeCol;
    @FXML private TableColumn<Booking, String> penDayCol;
    @FXML private TableColumn<Booking, Void> penActionCol;

    // --- Columns for Approved Table (Ensure fx:id matches variable name in FXML) ---
    @FXML private TableColumn<Booking, String> appRoomNoCol;
    @FXML private TableColumn<Booking, String> appRoomTypeCol;
    @FXML private TableColumn<Booking, String> appTimeCol;
    @FXML private TableColumn<Booking, String> appDayCol;
    @FXML private TableColumn<Booking, String> appStatusCol;

    // --- Sidebar Buttons (Ensure fx:id matches variable name in FXML) ---
    @FXML private Button viewAvailableRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button logoutButton;

    // Holds ALL bookings for the current user
    private ObservableList<Booking> userBookings;

    // Formatter for the Date (Day) column - consistent usage
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    @FXML
    public void initialize() {
        // Setup sidebar button actions and disable the current page's button
        setupNavigationButtons(viewAvailableRoomsButton, manageBookingsButton, logoutButton);
        if (manageBookingsButton != null) {
            manageBookingsButton.setDisable(true);
            // Optional: Style disabled button
            manageBookingsButton.setStyle("-fx-background-radius: 100; -fx-background-color: #596572; -fx-text-fill: white;");
        }

        // Get logged-in user and load their bookings
        User currentUser = DataStore.getLoggedInUser();
        if (currentUser == null || currentUser.getRole() != UserRole.FACULTY) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Access Denied", "No faculty user logged in. Returning to login.");
            if (logoutButton != null) {
                logoutButton.fire(); // Use logout logic to go back
            }
            return; // Stop initialization
        }
        userBookings = FXCollections.observableArrayList(DataStore.getBookingsByUser(currentUser));
        System.out.println("Loaded " + userBookings.size() + " bookings for user " + currentUser.getUsername()); // Debug

        // --- Configure Tables ---
        // Setup columns and data for the Pending table
        System.out.println("Configuring Pending Table...");
        configureBookingTable(pendingBookingsTable, penRoomNoCol, penRoomTypeCol, penTimeCol, penDayCol);
        setupCancelActionColumn(penActionCol); // Adds the "Cancel" button column
        if (pendingBookingsTable != null) {
            pendingBookingsTable.setItems(userBookings.filtered(b -> b.getStatus() == BookingStatus.PENDING));
            pendingBookingsTable.setPlaceholder(new Label("No pending bookings."));
        } else {
            System.err.println("ERROR: pendingBookingsTable is null!");
        }


        // Setup columns and data for the Approved/Past table
        System.out.println("Configuring Approved/Past Table...");
        configureBookingTable(approvedBookingsTable, appRoomNoCol, appRoomTypeCol, appTimeCol, appDayCol);
        configureStatusColumn(appStatusCol); // Setup the status display column
        if (approvedBookingsTable != null) {
            // Show everything EXCEPT Pending
            approvedBookingsTable.setItems(userBookings.filtered(b -> b.getStatus() != BookingStatus.PENDING));
            approvedBookingsTable.setPlaceholder(new Label("No approved, rejected, or cancelled bookings."));
        } else {
            System.err.println("ERROR: approvedBookingsTable is null!");
        }

    }

    /**
     * Configures the common data columns for a booking table.
     * IMPORTANT: This relies on the TableColumn variables being correctly injected via @FXML.
     */
    private void configureBookingTable(TableView<Booking> table,
                                       TableColumn<Booking, String> roomNoCol,
                                       TableColumn<Booking, String> roomTypeCol,
                                       TableColumn<Booking, String> timeCol,
                                       TableColumn<Booking, String> dayCol) {

        if (table == null) { System.err.println("configureBookingTable: Attempted to configure a null TableView!"); return; }

        // Configure Room Number Column
        if (roomNoCol != null) {
            // PropertyValueFactory looks for getRoomNumber() method in Booking class
            roomNoCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        } else { System.err.println("configureBookingTable: Room Number column is null - check FXML fx:id for table " + table.getId()); }

        // Configure Room Type Column
        if (roomTypeCol != null) {
            // PropertyValueFactory looks for getRoomType() method in Booking class
            roomTypeCol.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        } else { System.err.println("configureBookingTable: Room Type column is null - check FXML fx:id for table " + table.getId()); }

        // Configure Time Schedule Column
        if (timeCol != null) {
            // PropertyValueFactory looks for getTimeScheduleDisplay() or getBookedTimeRangeString() method in Booking class
            // Ensure ONE of these methods exists in your Booking class and returns the desired time format (e.g., "9:00 AM - 11:00 AM")
            timeCol.setCellValueFactory(new PropertyValueFactory<>("timeScheduleDisplay")); // Or "bookedTimeRangeString"
        } else { System.err.println("configureBookingTable: Time Schedule column is null - check FXML fx:id for table " + table.getId()); }

        // Configure Date (Day) Column using a Lambda for custom formatting
        if (dayCol != null) {
            dayCol.setCellValueFactory(cellData -> {
                Booking booking = cellData.getValue();
                // Basic null checks for safety
                if (booking == null || booking.getDate() == null) {
                    return new SimpleStringProperty(""); // Return empty string if data is missing
                }
                try {
                    String formattedDate = booking.getDate().format(dateFormatter); // Use YYYY-MM-DD
                    String dayOfWeek = booking.getDate().getDayOfWeek().toString();
                    // Capitalize the day name (e.g., "Monday")
                    dayOfWeek = dayOfWeek.substring(0, 1).toUpperCase() + dayOfWeek.substring(1).toLowerCase();
                    return new SimpleStringProperty(formattedDate + " (" + dayOfWeek + ")");
                } catch (Exception e) {
                    // Catch potential errors during formatting
                    System.err.println("Error formatting date for booking ID " + (booking != null ? booking.getBookingId() : "null") + ": " + e.getMessage());
                    return new SimpleStringProperty("Error");
                }
            });
        } else { System.err.println("configureBookingTable: Date (Day) column is null - check FXML fx:id for table " + table.getId()); }
    }

    /**
     * Configures the Status column for the approved/past bookings table.
     */
    private void configureStatusColumn(TableColumn<Booking, String> statusCol) {
        if (statusCol != null) {
            // PropertyValueFactory looks for getStatusDisplay() method in Booking class
            statusCol.setCellValueFactory(new PropertyValueFactory<>("statusDisplay"));

            // Optional: Add cell factory to style the text based on status
            statusCol.setCellFactory(column -> new TableCell<Booking, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(item); // Set the text (status name)
                    setStyle(""); // Reset style to default

                    if (item != null && !empty) {
                        // Get the full Booking object for this row to check its status enum
                        // Handle potential index out of bounds if list changes rapidly
                        int currentIndex = getIndex();
                        if (currentIndex >= 0 && currentIndex < getTableView().getItems().size()) {
                            Booking booking = getTableView().getItems().get(currentIndex);
                            if (booking != null) { // Null check for the booking itself
                                switch (booking.getStatus()) {
                                    case APPROVED:
                                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                                        break;
                                    case REJECTED:
                                    case CANCELLED:
                                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                                        break;
                                    // PENDING shouldn't be in this table, but handle just in case
                                    case PENDING:
                                        setStyle("-fx-text-fill: orange;");
                                        break;
                                    default:
                                        // Keep default style
                                        break;
                                }
                            }
                        }
                    } else {
                        // Ensure empty cells have no text
                        setText(null);
                    }
                }
            });
        } else {
            System.err.println("configureStatusColumn: Status column (appStatusCol) is null - check FXML fx:id.");
        }
    }

    /**
     * Sets up the "Action" column with a "Cancel" button for the Pending table.
     */
    private void setupCancelActionColumn(TableColumn<Booking, Void> column) {
        if (column == null) {
            System.err.println("setupCancelActionColumn: Action Column (penActionCol) is null! Check FXML fx:id.");
            return;
        }

        Callback<TableColumn<Booking, Void>, TableCell<Booking, Void>> cellFactory = param -> {
            final TableCell<Booking, Void> cell = new TableCell<>() {
                private final Button cancelBtn = new Button("Cancel");
                { // Instance initializer block for the button setup
                    cancelBtn.setStyle("-fx-background-color: #f08080; -fx-text-fill: white;"); // Light red background
                    cancelBtn.setOnAction((ActionEvent event) -> {
                        int index = getIndex(); // Get the row index
                        // Check if the index is valid for the current table items
                        if (index >= 0 && index < getTableView().getItems().size()) {
                            Booking booking = getTableView().getItems().get(index); // Get booking for this row
                            if (booking != null) {
                                handleCancelBooking(booking); // Call the cancel handler
                            } else {
                                System.err.println("Cancel button clicked on row with null booking data (index: " + index + ")");
                            }
                        } else {
                            System.err.println("Cancel button clicked on invalid row index: " + index);
                        }
                    });
                }

                // This method is called whenever the cell needs to be updated
                @Override
                public void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    // Show button only for non-empty rows within valid bounds
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null); // No button in empty rows
                    } else {
                        setGraphic(cancelBtn); // Show the button
                    }
                }
            };
            return cell;
        };

        column.setCellFactory(cellFactory); // Apply the factory to the column
    }

    /**
     * Handles the logic when the "Cancel" button is clicked for a pending booking.
     */
    private void handleCancelBooking(Booking booking) {
        if (booking == null) {
            System.err.println("handleCancelBooking called with null booking.");
            return;
        }
        // Double check it's actually pending before showing confirmation
        if (booking.getStatus() != BookingStatus.PENDING) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Cannot Cancel", "This booking is no longer pending and cannot be cancelled.");
            // Refresh tables in case the status changed just now
            if(pendingBookingsTable!=null) pendingBookingsTable.refresh();
            if(approvedBookingsTable!=null) approvedBookingsTable.refresh();
            return;
        }

        // Confirmation dialog
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Cancellation");
        confirmation.setHeaderText("Cancel Booking?");
        // Provide more details in the confirmation message
        confirmation.setContentText("Are you sure you want to cancel this booking request?\n\n" +
                "Room: " + booking.getRoomNumber() + " (" + booking.getRoomType() + ")\n" +
                "Date: " + booking.getDate().format(dateFormatter) + "\n" +
                "Time: " + booking.getTimeScheduleDisplay()); // Use display method

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.out.println("User confirmed cancellation for booking ID: " + booking.getBookingId()); // Debug
            // Find the booking in the central DataStore to ensure we update the persisted record
            Booking bookingToUpdate = DataStore.findBookingById(booking.getBookingId());

            if (bookingToUpdate != null && bookingToUpdate.getStatus() == BookingStatus.PENDING) {
                // Update the status in the DataStore (this should trigger save)
                bookingToUpdate.setStatus(BookingStatus.CANCELLED);
                System.out.println("Booking status updated to CANCELLED in DataStore."); // Debug

                // --- Refresh UI ---
                // The userBookings list holds the master data for this controller.
                // Updating the status on the object *might* be enough if the filtered lists
                // correctly re-evaluate, but explicitly removing and adding or refreshing
                // the tables is often more reliable. Let's refresh the tables.

                // A simple refresh is often sufficient if the underlying list filtering is correct
                if (pendingBookingsTable != null) pendingBookingsTable.refresh();
                if (approvedBookingsTable != null) approvedBookingsTable.refresh();
                // The cancelled booking should now disappear from 'Pending' and appear in 'Approved/Past'

                SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Success", "Booking cancelled successfully.");

            } else {
                // Booking couldn't be found or was no longer pending
                System.err.println("Failed to cancel booking ID: " + booking.getBookingId() + " - Not found or status changed."); // Debug
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Cancellation Failed",
                        "Could not cancel the booking. It might have been processed by an admin already or couldn't be found.");
                // Refresh tables anyway, in case the status changed elsewhere
                if (pendingBookingsTable != null) pendingBookingsTable.refresh();
                if (approvedBookingsTable != null) approvedBookingsTable.refresh();
            }
        } else {
            System.out.println("User cancelled the cancellation dialog."); // Debug
        }
    }

}