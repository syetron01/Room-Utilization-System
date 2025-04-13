package com.example.roomutilizationsystem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import java.util.Optional;

// Import DataStore, Booking, BookingStatus, SceneNavigator, AdminBaseController etc.

public class AdminManageBookingsController extends AdminBaseController {

    @FXML private TextField searchField;
    @FXML private TableView<Booking> bookingsTableView; // Use Booking class

    // Inject Table Columns (Make sure fx:id is set for each in FXML)
    @FXML private TableColumn<Booking, String> userCol;
    @FXML private TableColumn<Booking, String> roleCol;
    @FXML private TableColumn<Booking, String> roomNoCol;
    @FXML private TableColumn<Booking, String> roomTypeCol;
    @FXML private TableColumn<Booking, String> timeCol;
    @FXML private TableColumn<Booking, String> dayCol; // Should display Date + DayOfWeek
    @FXML private TableColumn<Booking, Void> actionCol; // Column for Approve/Reject buttons

    // Inject Sidebar Buttons
    @FXML private Button homeButton;
    @FXML private Button addRoomsButton;
    @FXML private Button viewRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button logoutButton;

    private ObservableList<Booking> masterBookingList;
    private FilteredList<Booking> filteredBookingList;

    @FXML
    public void initialize() {
        // Setup sidebar navigation
        setupNavigationButtons(homeButton, addRoomsButton, viewRoomsButton, manageBookingsButton, logoutButton);
        manageBookingsButton.setDisable(true); // Disable current page button

        // --- Configure Table ---
        // Map Booking properties to columns
        userCol.setCellValueFactory(new PropertyValueFactory<>("facultyUsername")); // Use derived property
        roleCol.setCellValueFactory(new PropertyValueFactory<>("facultyRole"));     // Use derived property
        roomNoCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));    // Use derived property
        roomTypeCol.setCellValueFactory(new PropertyValueFactory<>("roomType"));    // Use derived property
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timeScheduleDisplay")); // Use derived property
        // Combine Date and Day for Day column
        dayCol.setCellValueFactory(cellData -> {
            Booking booking = cellData.getValue();
            String formattedDate = booking.getDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
            String dayOfWeek = booking.getDate().getDayOfWeek().toString();
            // Capitalize day of week properly
            dayOfWeek = dayOfWeek.substring(0, 1).toUpperCase() + dayOfWeek.substring(1).toLowerCase();
            return new javafx.beans.property.SimpleStringProperty(formattedDate + " (" + dayOfWeek + ")");
        });


        // Add Action Buttons (Approve/Reject)
        setupActionColumn();

        // --- Load Data and Setup Filtering ---
        masterBookingList = FXCollections.observableArrayList(DataStore.getAllBookings());
        filteredBookingList = new FilteredList<>(masterBookingList, p -> true); // Initially show all

        // Bind filter predicate to search field text changes
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredBookingList.setPredicate(booking -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true; // Show all if search field is empty
                }
                String lowerCaseFilter = newValue.toLowerCase();
                // Check against various booking fields
                if (booking.getFacultyUsername().toLowerCase().contains(lowerCaseFilter)) return true;
                if (booking.getRoomNumber().toLowerCase().contains(lowerCaseFilter)) return true;
                if (booking.getRoomType().toLowerCase().contains(lowerCaseFilter)) return true;
                if (booking.getDate().toString().contains(lowerCaseFilter)) return true;
                if (booking.getDayDisplay().toLowerCase().contains(lowerCaseFilter)) return true;
                if (booking.getStatusDisplay().toLowerCase().contains(lowerCaseFilter)) return true;

                return false; // No match
            });
        });

        bookingsTableView.setItems(filteredBookingList);

        // Set placeholder text if table is empty
        bookingsTableView.setPlaceholder(new Label("No bookings found."));
    }

    private void setupActionColumn() {
        Callback<TableColumn<Booking, Void>, TableCell<Booking, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Booking, Void> call(final TableColumn<Booking, Void> param) {
                final TableCell<Booking, Void> cell = new TableCell<>() {

                    private final Button approveBtn = new Button("Approve");
                    private final Button rejectBtn = new Button("Reject");
                    private final Label statusLabel = new Label(); // To show status if not pending
                    private final javafx.scene.layout.HBox pane = new javafx.scene.layout.HBox(5); // Spacing 5

                    {
                        approveBtn.setOnAction((ActionEvent event) -> {
                            Booking booking = getTableView().getItems().get(getIndex());
                            updateBookingStatus(booking, BookingStatus.APPROVED);
                        });
                        rejectBtn.setOnAction((ActionEvent event) -> {
                            Booking booking = getTableView().getItems().get(getIndex());
                            updateBookingStatus(booking, BookingStatus.REJECTED);
                        });

                        // Basic styling (optional)
                        approveBtn.setStyle("-fx-background-color: #8fbc8f; -fx-text-fill: white;"); // Greenish
                        rejectBtn.setStyle("-fx-background-color: #f08080; -fx-text-fill: white;");   // Reddish
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            Booking booking = getTableView().getItems().get(getIndex());
                            if (booking.getStatus() == BookingStatus.PENDING) {
                                pane.getChildren().setAll(approveBtn, rejectBtn); // Show buttons only if pending
                                setGraphic(pane);
                            } else {
                                statusLabel.setText(booking.getStatusDisplay()); // Show current status
                                // Style the label based on status (optional)
                                switch (booking.getStatus()) {
                                    case APPROVED: statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;"); break;
                                    case REJECTED:
                                    case CANCELLED: statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;"); break;
                                    default: statusLabel.setStyle(""); break;
                                }
                                pane.getChildren().setAll(statusLabel);
                                setGraphic(pane);
                            }
                        }
                    }
                };
                return cell;
            }
        };
        actionCol.setCellFactory(cellFactory);
        actionCol.setText("Action / Status"); // Update column header
    }


    private void updateBookingStatus(Booking booking, BookingStatus newStatus) {
        // Confirm action
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Action");
        confirmation.setHeaderText("Update Booking Status?");
        confirmation.setContentText("Are you sure you want to set the status of booking ID "
                + booking.getBookingId() + " to " + newStatus + "?");

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Find the booking in the master list and update its status
            Booking bookingToUpdate = DataStore.findBookingById(booking.getBookingId());
            if (bookingToUpdate != null) {
                bookingToUpdate.setStatus(newStatus);

                // Refresh the specific row visually (or the whole table)
                bookingsTableView.refresh();
                SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Success", "Booking status updated.");
            } else {
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Error", "Could not find the booking to update.");
            }
        }
    }

}
