package com.example.roomutilizationsystem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Import DataStore, Booking, BookingStatus, SceneNavigator, StaffBaseController etc.

public class StaffManageBookingsController extends StaffBaseController {

    @FXML private TableView<Booking> pendingBookingsTable;
    @FXML private TableView<Booking> approvedBookingsTable; // And potentially Rejected/Cancelled

    // Inject Columns for Pending Table (Set fx:id in FXML)
    @FXML private TableColumn<Booking, String> penRoomNoCol;
    @FXML private TableColumn<Booking, String> penRoomTypeCol;
    @FXML private TableColumn<Booking, String> penTimeCol;
    @FXML private TableColumn<Booking, String> penDayCol; // Date + Day
    @FXML private TableColumn<Booking, Void> penActionCol; // Cancel Button

    // Inject Columns for Approved Table (Set fx:id in FXML)
    @FXML private TableColumn<Booking, String> appRoomNoCol;
    @FXML private TableColumn<Booking, String> appRoomTypeCol;
    @FXML private TableColumn<Booking, String> appTimeCol;
    @FXML private TableColumn<Booking, String> appDayCol; // Date + Day
    @FXML private TableColumn<Booking, String> appStatusCol; // Just display status


    // Inject Sidebar Buttons (Set fx:id in FXML)
    @FXML private Button viewAvailableRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button logoutButton;

    private ObservableList<Booking> userBookings;

    @FXML
    public void initialize() {
        setupNavigationButtons(viewAvailableRoomsButton, manageBookingsButton, logoutButton);
        manageBookingsButton.setDisable(true); // Disable current page button

        User currentUser = DataStore.getLoggedInUser();
        if (currentUser == null || currentUser.getRole() != UserRole.FACULTY) {
            // Should not happen if login logic is correct, but good practice
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Access Denied", "No faculty user logged in.");
            // Optionally navigate back to login
            logoutButton.fire(); // Simulate logout click
            return;
        }


        // --- Load User's Bookings ---
        userBookings = FXCollections.observableArrayList(DataStore.getBookingsByUser(currentUser));

        // --- Configure Pending Table ---
        configureBookingTable(pendingBookingsTable, penRoomNoCol, penRoomTypeCol, penTimeCol, penDayCol);
        setupCancelActionColumn(penActionCol); // Add cancel button
        pendingBookingsTable.setItems(userBookings.filtered(b -> b.getStatus() == BookingStatus.PENDING));
        pendingBookingsTable.setPlaceholder(new Label("No pending bookings."));


        // --- Configure Approved/Other Table ---
        // This table might show Approved, Rejected, Cancelled
        configureBookingTable(approvedBookingsTable, appRoomNoCol, appRoomTypeCol, appTimeCol, appDayCol);
        // Add a simple status column
        if (appStatusCol != null) { // Check if the column exists in FXML
            appStatusCol.setCellValueFactory(new PropertyValueFactory<>("statusDisplay"));
            // Optional: Style based on status
            appStatusCol.setCellFactory(column -> new TableCell<Booking, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(item);
                    setStyle(""); // Reset style
                    if (item != null && !empty) {
                        Booking booking = getTableView().getItems().get(getIndex());
                        switch(booking.getStatus()) {
                            case APPROVED: setStyle("-fx-text-fill: green;"); break;
                            case REJECTED:
                            case CANCELLED: setStyle("-fx-text-fill: red;"); break;
                            default: break;
                        }
                    }
                }
            });
        }
        approvedBookingsTable.setItems(userBookings.filtered(b -> b.getStatus() != BookingStatus.PENDING));
        approvedBookingsTable.setPlaceholder(new Label("No approved or past bookings."));


        // Refresh tables if a booking status changes elsewhere
        // (Could use JavaFX properties/bindings for more robust updates)
    }

    // Helper to configure common columns
    private void configureBookingTable(TableView<Booking> table,
                                       TableColumn<Booking, String> roomNoCol,
                                       TableColumn<Booking, String> roomTypeCol,
                                       TableColumn<Booking, String> timeCol,
                                       TableColumn<Booking, String> dayCol) {
        if (table == null) return;
        if (roomNoCol != null) roomNoCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        if (roomTypeCol != null) roomTypeCol.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        if (timeCol != null) timeCol.setCellValueFactory(new PropertyValueFactory<>("bookedTimeRangeString")); // Use booking time
        if (dayCol != null) {
            dayCol.setCellValueFactory(cellData -> {
                Booking booking = cellData.getValue();
                String formattedDate = booking.getDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
                String dayOfWeek = booking.getDate().getDayOfWeek().toString();
                dayOfWeek = dayOfWeek.substring(0, 1).toUpperCase() + dayOfWeek.substring(1).toLowerCase();
                return new javafx.beans.property.SimpleStringProperty(formattedDate + " (" + dayOfWeek + ")");
            });
        }
    }


    // Setup Cancel button column for Pending table
    private void setupCancelActionColumn(TableColumn<Booking, Void> column) {
        if (column == null) return;
        Callback<TableColumn<Booking, Void>, TableCell<Booking, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Booking, Void> call(final TableColumn<Booking, Void> param) {
                final TableCell<Booking, Void> cell = new TableCell<>() {
                    private final Button cancelBtn = new Button("Cancel");
                    {
                        cancelBtn.setStyle("-fx-background-color: #f08080; -fx-text-fill: white;");
                        cancelBtn.setOnAction((ActionEvent event) -> {
                            Booking booking = getTableView().getItems().get(getIndex());
                            handleCancelBooking(booking);
                        });
                    }
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : cancelBtn);
                    }
                };
                return cell;
            }
        };
        column.setCellFactory(cellFactory);
    }

    private void handleCancelBooking(Booking booking) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Cancellation");
        confirmation.setHeaderText("Cancel Booking?");
        confirmation.setContentText("Are you sure you want to cancel booking ID: " + booking.getBookingId() + "?");

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            Booking bookingToUpdate = DataStore.findBookingById(booking.getBookingId());
            if (bookingToUpdate != null && bookingToUpdate.getStatus() == BookingStatus.PENDING) {
                bookingToUpdate.setStatus(BookingStatus.CANCELLED);

                // Refresh the view - easiest is to re-filter the main list
                // This is slightly inefficient but simple for this demo.
                // A better way involves removing/adding between filtered lists.
                pendingBookingsTable.getItems().remove(bookingToUpdate);
                approvedBookingsTable.getItems().add(bookingToUpdate); // Add to the 'other' list

                SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Success", "Booking cancelled.");
            } else {
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Error", "Could not cancel booking (maybe it was already approved/rejected?).");
            }
        }
    }
}
