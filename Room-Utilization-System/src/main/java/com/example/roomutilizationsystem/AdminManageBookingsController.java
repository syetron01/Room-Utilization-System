package com.example.roomutilizationsystem;

import javafx.beans.property.SimpleStringProperty; // Required for lambda usage
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory; // Keep for userCol, roleCol, timeCol, dayCol (using Booking getter)
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import java.util.Optional;
import java.time.format.DateTimeFormatter;

public class AdminManageBookingsController extends AdminBaseController {

    @FXML private TextField searchField;
    @FXML private TableView<Booking> bookingsTableView;

    // Inject Table Columns (Ensure fx:id is set for ALL columns)
    @FXML private TableColumn<Booking, String> userCol;
    @FXML private TableColumn<Booking, String> roleCol;
    @FXML private TableColumn<Booking, String> roomNoCol;
    @FXML private TableColumn<Booking, String> roomTypeCol;
    @FXML private TableColumn<Booking, String> timeCol;
    @FXML private TableColumn<Booking, String> dayCol;
    @FXML private TableColumn<Booking, Void> actionCol;

    // Inject Sidebar Buttons (Ensure fx:id is set in FXML)
    @FXML private Button homeButton;
    @FXML private Button addRoomsButton;
    @FXML private Button viewRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button logoutButton;

    private ObservableList<Booking> masterBookingList;
    private FilteredList<Booking> filteredBookingList;

    @FXML
    public void initialize() {
        // Setup sidebar buttons
        setupNavigationButtons(homeButton, addRoomsButton, viewRoomsButton, manageBookingsButton, logoutButton);
        // Disable the button for the current page
        if (manageBookingsButton != null) {
            manageBookingsButton.setDisable(true);
            manageBookingsButton.setStyle("-fx-background-radius: 100; -fx-background-color: #596572;");
            manageBookingsButton.setTextFill(javafx.scene.paint.Color.WHITE);
        }

        // --- Configure Table ---
        if (userCol != null) userCol.setCellValueFactory(new PropertyValueFactory<>("facultyUsername"));
        else System.err.println("AdminManageBookingsController: userCol is null. Check FXML fx:id.");

        if (roleCol != null) roleCol.setCellValueFactory(new PropertyValueFactory<>("facultyRole"));
        else System.err.println("AdminManageBookingsController: roleCol is null. Check FXML fx:id.");

        if (roomNoCol != null) {
            // Use Lambda instead of PropertyValueFactory
            roomNoCol.setCellValueFactory(cellData -> {
                Booking booking = cellData.getValue();
                return new SimpleStringProperty(booking != null ? booking.getRoomNumber() : ""); // Calls getter in Booking
            });
        } else {
            System.err.println("AdminManageBookingsController: roomNoCol is null. Check FXML fx:id.");
        }

        if (roomTypeCol != null) {
            // Use Lambda instead of PropertyValueFactory
            roomTypeCol.setCellValueFactory(cellData -> {
                Booking booking = cellData.getValue();
                return new SimpleStringProperty(booking != null ? booking.getRoomType() : ""); // Calls getter in Booking
            });
        } else {
            System.err.println("AdminManageBookingsController: roomTypeCol is null. Check FXML fx:id.");
        }

        if (timeCol != null) timeCol.setCellValueFactory(new PropertyValueFactory<>("timeScheduleDisplay"));
        else System.err.println("AdminManageBookingsController: timeCol is null. Check FXML fx:id.");

        // Using PropertyValueFactory for dayDisplay which is derived in Booking class
        if (dayCol != null) dayCol.setCellValueFactory(new PropertyValueFactory<>("dayDisplay"));
        else System.err.println("AdminManageBookingsController: dayCol is null. Check FXML fx:id.");

        // Add Action Buttons (Approve/Reject/Status)
        if (actionCol != null) setupActionColumn();
        else System.err.println("AdminManageBookingsController: actionCol is null. Check FXML fx:id.");

        // --- Load Data and Setup Filtering ---
        try {
            masterBookingList = FXCollections.observableArrayList(DataStore.getAllBookings());

            // Debug print (keep for verification)
            System.out.println("\n--- AdminManageBookingsController Loaded Bookings ---");
            if (masterBookingList == null || masterBookingList.isEmpty()) { System.out.println("No bookings loaded from DataStore."); }
            else { masterBookingList.forEach(booking -> { /* ... print details ... */ }); }
            System.out.println("-----------------------------------------------");

            filteredBookingList = new FilteredList<>(masterBookingList, p -> true);

            if (searchField != null) {
                searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                    filteredBookingList.setPredicate(booking -> {
                        if (newValue == null || newValue.isEmpty()) return true;
                        if (booking == null) return false;
                        String lowerCaseFilter = newValue.toLowerCase();
                        if (booking.getFacultyUsername() != null && booking.getFacultyUsername().toLowerCase().contains(lowerCaseFilter)) return true;
                        if (booking.getRoomNumber() != null && booking.getRoomNumber().toLowerCase().contains(lowerCaseFilter)) return true;
                        if (booking.getRoomType() != null && booking.getRoomType().toLowerCase().contains(lowerCaseFilter)) return true;
                        if (booking.getDate() != null && booking.getDate().toString().contains(lowerCaseFilter)) return true;
                        if (booking.getDayDisplay() != null && booking.getDayDisplay().toLowerCase().contains(lowerCaseFilter)) return true;
                        if (booking.getStatusDisplay() != null && booking.getStatusDisplay().toLowerCase().contains(lowerCaseFilter)) return true;
                        return false;
                    });
                });
            } else { System.err.println("AdminManageBookingsController: searchField is null. Check FXML fx:id."); }

            if(bookingsTableView != null) {
                bookingsTableView.setItems(filteredBookingList);
                bookingsTableView.setPlaceholder(new Label("No bookings found."));
            } else { System.err.println("AdminManageBookingsController: bookingsTableView is null. Check FXML fx:id."); }
        } catch (Exception e) {
            System.err.println("Error during AdminManageBookingsController initialization: " + e.getMessage());
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Initialization Error", "Failed to load booking data.");
            if (bookingsTableView != null) { bookingsTableView.setPlaceholder(new Label("Error loading data.")); }
        }
    }

    // --- setupActionColumn and updateBookingStatus methods (Keep as before) ---
    private void setupActionColumn() {
        // ... (Implementation remains the same) ...
        Callback<TableColumn<Booking, Void>, TableCell<Booking, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Booking, Void> call(final TableColumn<Booking, Void> param) {
                final TableCell<Booking, Void> cell = new TableCell<>() {
                    private final Button approveBtn = new Button("Approve");
                    private final Button rejectBtn = new Button("Reject");
                    private final Label statusLabel = new Label();
                    private final HBox pane = new HBox(5);
                    {
                        approveBtn.setOnAction((ActionEvent event) -> {
                            if (!isEmpty() && getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                                Booking booking = getTableView().getItems().get(getIndex());
                                if (booking != null) updateBookingStatus(booking, BookingStatus.APPROVED);
                            }
                        });
                        rejectBtn.setOnAction((ActionEvent event) -> {
                            if (!isEmpty() && getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                                Booking booking = getTableView().getItems().get(getIndex());
                                if (booking != null) updateBookingStatus(booking, BookingStatus.REJECTED);
                            }
                        });
                        approveBtn.setStyle("-fx-background-color: #8fbc8f; -fx-text-fill: white;");
                        rejectBtn.setStyle("-fx-background-color: #f08080; -fx-text-fill: white;");
                        approveBtn.setMinWidth(60);
                        rejectBtn.setMinWidth(60);
                        pane.setAlignment(javafx.geometry.Pos.CENTER);
                    }
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                            setGraphic(null);
                        } else {
                            Booking booking = getTableView().getItems().get(getIndex());
                            if (booking == null) { setGraphic(null); return; }
                            if (booking.getStatus() == BookingStatus.PENDING) {
                                pane.getChildren().setAll(approveBtn, rejectBtn);
                                setGraphic(pane);
                            } else {
                                statusLabel.setText(booking.getStatusDisplay());
                                switch (booking.getStatus()) {
                                    case APPROVED: statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;"); break;
                                    case REJECTED: case CANCELLED: statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;"); break;
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
        actionCol.setText("Action / Status");
    }

    private void updateBookingStatus(Booking booking, BookingStatus newStatus) {
        // ... (Implementation remains the same) ...
        if (booking == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Action");
        confirmation.setHeaderText("Update Booking Status?");
        confirmation.setContentText("Are you sure you want to set the status of booking ID "
                + booking.getBookingId() + " for user " + booking.getFacultyUsername()
                + " to " + newStatus + "?");
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Booking bookingToUpdate = DataStore.findBookingById(booking.getBookingId());
            if (bookingToUpdate != null) {
                bookingToUpdate.setStatus(newStatus);
                bookingsTableView.refresh();
                SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Success", "Booking status updated.");
            } else {
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Error", "Could not find the booking to update.");
                masterBookingList.setAll(DataStore.getAllBookings()); // Refresh list
                bookingsTableView.refresh();
            }
        }
    }
}