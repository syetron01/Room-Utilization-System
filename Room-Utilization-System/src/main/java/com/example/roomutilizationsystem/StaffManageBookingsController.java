package com.example.roomutilizationsystem;

import javafx.beans.property.SimpleStringProperty; // ADD THIS IMPORT
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
// import javafx.scene.control.cell.PropertyValueFactory; // No longer needed for these columns
import javafx.util.Callback;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


public class StaffManageBookingsController extends StaffBaseController {

    // --- TableViews ---
    @FXML private TableView<Booking> pendingBookingsTable;
    @FXML private TableView<Booking> approvedBookingsTable;

    // --- Columns for Pending Table (Ensure fx:id matches CORRECTED FXML) ---
    @FXML private TableColumn<Booking, String> penRoomNoCol;
    @FXML private TableColumn<Booking, String> penRoomTypeCol;
    @FXML private TableColumn<Booking, String> penTimeCol;
    @FXML private TableColumn<Booking, String> penDayCol; // Displays Date + (DayOfWeek)
    @FXML private TableColumn<Booking, Void> penActionCol; // Used for Cancel button

    // --- Columns for Approved Table (Ensure fx:id matches CORRECTED FXML) ---
    @FXML private TableColumn<Booking, String> appRoomNoCol;
    @FXML private TableColumn<Booking, String> appRoomTypeCol;
    @FXML private TableColumn<Booking, String> appTimeCol;
    @FXML private TableColumn<Booking, String> appDayCol; // Displays Date + (DayOfWeek)
    @FXML private TableColumn<Booking, String> appStatusCol; // Displays status text

    // --- Sidebar Buttons (Ensure fx:id matches variable name in FXML) ---
    @FXML private Button viewAvailableRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button logoutButton;

    // Holds ALL bookings for the current user (loaded from file via DataStore)
    private ObservableList<Booking> userBookings;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    @FXML
    public void initialize() {
        // Defend against NullPointerException if FXML injection fails
        try {
            Objects.requireNonNull(pendingBookingsTable, "pendingBookingsTable FXML ID not injected");
            Objects.requireNonNull(approvedBookingsTable, "approvedBookingsTable FXML ID not injected");
            Objects.requireNonNull(penRoomNoCol, "penRoomNoCol FXML ID not injected");
            Objects.requireNonNull(penRoomTypeCol, "penRoomTypeCol FXML ID not injected");
            Objects.requireNonNull(penTimeCol, "penTimeCol FXML ID not injected");
            Objects.requireNonNull(penDayCol, "penDayCol FXML ID not injected");
            Objects.requireNonNull(penActionCol, "penActionCol FXML ID not injected");
            Objects.requireNonNull(appRoomNoCol, "appRoomNoCol FXML ID not injected");
            Objects.requireNonNull(appRoomTypeCol, "appRoomTypeCol FXML ID not injected");
            Objects.requireNonNull(appTimeCol, "appTimeCol FXML ID not injected");
            Objects.requireNonNull(appDayCol, "appDayCol FXML ID not injected");
            Objects.requireNonNull(appStatusCol, "appStatusCol FXML ID not injected");
            Objects.requireNonNull(viewAvailableRoomsButton, "viewAvailableRoomsButton FXML ID not injected");
            Objects.requireNonNull(manageBookingsButton, "manageBookingsButton FXML ID not injected");
            Objects.requireNonNull(logoutButton, "logoutButton FXML ID not injected");
        } catch (NullPointerException e) {
            System.err.println("FATAL: FXML injection failed in StaffManageBookingsController. Check FXML IDs.");
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Initialization Error", "UI components could not be loaded. Check FXML file and controller bindings.");
            if(pendingBookingsTable != null) pendingBookingsTable.setDisable(true);
            if(approvedBookingsTable != null) approvedBookingsTable.setDisable(true);
            return;
        }

        setupNavigationButtons(viewAvailableRoomsButton, manageBookingsButton, logoutButton);
        if (manageBookingsButton != null) {
            manageBookingsButton.setDisable(true);
            manageBookingsButton.setStyle("-fx-background-radius: 100; -fx-background-color: #596572; -fx-text-fill: white;");
        }

        User currentUser = DataStore.getLoggedInUser();

        // --- Role Check ---
        if (currentUser == null || currentUser.getRole() == UserRole.ADMIN) {
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Access Denied", "Invalid user role for this page. Returning to login.");
            if (logoutButton != null && logoutButton.getScene() != null) {
                handleLogoutAction(new ActionEvent(logoutButton, logoutButton));
            } else {
                System.err.println("Cannot automatically logout - button/scene unavailable.");
            }
            return;
        }
        // --- End of Role Check ---

        userBookings = FXCollections.observableArrayList(DataStore.getBookingsByUser(currentUser));
        System.out.println("StaffManageBookings: Loaded " + userBookings.size() + " bookings for user " + currentUser.getUsername());

        // --- Configure Tables ---
        System.out.println("Configuring Pending Table...");
        configureBookingTable(pendingBookingsTable, penRoomNoCol, penRoomTypeCol, penTimeCol, penDayCol);
        setupCancelActionColumn(penActionCol);
        pendingBookingsTable.setItems(userBookings.filtered(b -> b.getStatus() == BookingStatus.PENDING));
        pendingBookingsTable.setPlaceholder(new Label("No pending bookings found."));

        System.out.println("Configuring Approved/Past Table...");
        configureBookingTable(approvedBookingsTable, appRoomNoCol, appRoomTypeCol, appTimeCol, appDayCol);
        configureStatusColumn(appStatusCol);
        approvedBookingsTable.setItems(userBookings.filtered(b -> b.getStatus() != BookingStatus.PENDING));
        approvedBookingsTable.setPlaceholder(new Label("No approved, rejected, or cancelled bookings found."));
    }

    /**
     * Configures the common data columns for a booking table using Lambdas.
     */
    private void configureBookingTable(TableView<Booking> table,
                                       TableColumn<Booking, String> roomNoCol,
                                       TableColumn<Booking, String> roomTypeCol,
                                       TableColumn<Booking, String> timeCol,
                                       TableColumn<Booking, String> dayCol) {

        // Room Number
        roomNoCol.setCellValueFactory(cellData -> {
            Booking booking = cellData.getValue();
            return new SimpleStringProperty(booking != null ? booking.getRoomNumber() : "");
        });

        // Room Type
        roomTypeCol.setCellValueFactory(cellData -> {
            Booking booking = cellData.getValue();
            return new SimpleStringProperty(booking != null ? booking.getRoomType() : "");
        });

        // Time Schedule
        timeCol.setCellValueFactory(cellData -> {
            Booking booking = cellData.getValue();
            return new SimpleStringProperty(booking != null ? booking.getTimeScheduleDisplay() : "");
        });

        // Date/Day
        dayCol.setCellValueFactory(cellData -> {
            Booking booking = cellData.getValue();
            return new SimpleStringProperty(booking != null ? booking.getDayDisplay() : "");
        });
    }

    /**
     * Configures the Status column for the approved/past table using Lambdas.
     * Uses PropertyValueFactory and styles the cell based on status.
     */
    private void configureStatusColumn(TableColumn<Booking, String> statusCol) {
        // Use Lambda for the value
        statusCol.setCellValueFactory(cellData -> {
            Booking booking = cellData.getValue();
            return new SimpleStringProperty(booking != null ? booking.getStatusDisplay() : "");
        });

        // Cell factory for styling remains largely the same
        statusCol.setCellFactory(column -> new TableCell<Booking, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Get the full Booking object to determine style accurately
                    Booking booking = getTableView().getItems().get(getIndex()); // Use getIndex() with filtered list
                    if (booking != null) {
                        switch (booking.getStatus()) {
                            case APPROVED:
                                setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                                break;
                            case REJECTED:
                            case CANCELLED:
                                setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                                break;
                            case PENDING:
                                setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                                break;
                            default:
                                setStyle("");
                                break;
                        }
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    /**
     * Sets up the "Action" column with a "Cancel" button for the Pending table.
     */
    private void setupCancelActionColumn(TableColumn<Booking, Void> column) {
        Callback<TableColumn<Booking, Void>, TableCell<Booking, Void>> cellFactory = param -> {
            final TableCell<Booking, Void> cell = new TableCell<>() {
                private final Button cancelBtn = new Button("Cancel");

                {
                    cancelBtn.setStyle("-fx-background-color: #f08080; -fx-text-fill: white; -fx-font-weight: bold;");
                    cancelBtn.setOnAction((ActionEvent event) -> {
                        int index = getIndex();
                        if (!isEmpty() && index >= 0 && index < getTableView().getItems().size()) {
                            Booking booking = getTableView().getItems().get(index);
                            if (booking != null) {
                                handleCancelBooking(booking);
                            }
                        }
                    });
                }

                @Override
                public void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                    } else {
                        Booking booking = getTableView().getItems().get(getIndex());
                        if (booking != null && booking.getStatus() == BookingStatus.PENDING) {
                            setGraphic(cancelBtn);
                        } else {
                            setGraphic(null);
                        }
                    }
                }
            };
            return cell;
        };
        column.setCellFactory(cellFactory);
    }

    /**
     * Handles the logic when the "Cancel" button is clicked for a pending booking.
     * Updates the status in DataStore and refreshes the UI.
     */
    private void handleCancelBooking(Booking booking) {
        if (booking == null || booking.getStatus() != BookingStatus.PENDING) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Cannot Cancel",
                    booking == null ? "No booking selected." : "This booking is no longer pending and cannot be cancelled.");
            if(pendingBookingsTable!=null) pendingBookingsTable.refresh();
            if(approvedBookingsTable!=null) approvedBookingsTable.refresh();
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Cancellation");
        confirmation.setHeaderText("Cancel Booking Request?");
        confirmation.setContentText("Are you sure you want to cancel this booking request?\n\n" +
                "Room: " + booking.getRoomNumber() + " (" + booking.getRoomType() + ")\n" +
                "Date: " + booking.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE) + "\n" +
                "Time: " + booking.getTimeScheduleDisplay());

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            Booking bookingToUpdate = DataStore.findBookingById(booking.getBookingId());

            if (bookingToUpdate != null && bookingToUpdate.getStatus() == BookingStatus.PENDING) {
                bookingToUpdate.setStatus(BookingStatus.CANCELLED);
                System.out.println("Booking status updated to CANCELLED in DataStore for ID: " + booking.getBookingId());

                // Refresh UI tables
                if (pendingBookingsTable != null) pendingBookingsTable.refresh();
                if (approvedBookingsTable != null) approvedBookingsTable.refresh();

                SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Success", "Booking cancelled successfully.");

            } else {
                System.err.println("Failed to cancel booking ID: " + booking.getBookingId() + " - Not found in DataStore or status changed.");
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Cancellation Failed", "Could not cancel the booking. It might have been processed by an admin or already cancelled.");
                if (pendingBookingsTable != null) pendingBookingsTable.refresh();
                if (approvedBookingsTable != null) approvedBookingsTable.refresh();
            }
        } else {
            System.out.println("User cancelled the cancellation dialog.");
        }
    }
}