package com.example.roomutilizationsystem;

import javafx.beans.property.SimpleStringProperty; // REQUIRED for Lambdas
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
// import javafx.scene.control.cell.PropertyValueFactory; // No longer needed for these columns
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import java.util.Optional;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class AdminManageBookingsController extends AdminBaseController {

    @FXML private TextField searchField;
    @FXML private TableView<Booking> bookingsTableView;

    // Inject Table Columns (Ensure fx:id is set for ALL columns in the corrected FXML)
    @FXML private TableColumn<Booking, String> userCol;
    @FXML private TableColumn<Booking, String> roleCol;
    @FXML private TableColumn<Booking, String> roomNoCol;
    @FXML private TableColumn<Booking, String> roomTypeCol;
    @FXML private TableColumn<Booking, String> timeCol;
    @FXML private TableColumn<Booking, String> dayCol; // Will display Date + (DayOfWeek) via getDayDisplay()
    @FXML private TableColumn<Booking, Void> actionCol; // For Approve/Reject/Status display

    // Inject Sidebar Buttons (Ensure fx:id is set in FXML)
    @FXML private Button homeButton;
    @FXML private Button addRoomsButton;
    @FXML private Button viewRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button manageScheduleRequestsButton;
    @FXML private Button logoutButton;

    private ObservableList<Booking> masterBookingList;
    private FilteredList<Booking> filteredBookingList;

    @FXML
    public void initialize() {

        // Null checks for FXML fields
        try {
            Objects.requireNonNull(searchField, "searchField FXML ID not injected");
            Objects.requireNonNull(bookingsTableView, "bookingsTableView FXML ID not injected");
            Objects.requireNonNull(userCol, "userCol FXML ID not injected");
            Objects.requireNonNull(roleCol, "roleCol FXML ID not injected");
            Objects.requireNonNull(roomNoCol, "roomNoCol FXML ID not injected");
            Objects.requireNonNull(roomTypeCol, "roomTypeCol FXML ID not injected");
            Objects.requireNonNull(timeCol, "timeCol FXML ID not injected");
            Objects.requireNonNull(dayCol, "dayCol FXML ID not injected");
            Objects.requireNonNull(actionCol, "actionCol FXML ID not injected");
            Objects.requireNonNull(homeButton, "homeButton FXML ID not injected");
            Objects.requireNonNull(addRoomsButton, "addRoomsButton FXML ID not injected");
            Objects.requireNonNull(viewRoomsButton, "viewRoomsButton FXML ID not injected");
            Objects.requireNonNull(manageBookingsButton, "manageBookingsButton FXML ID not injected");
            Objects.requireNonNull(manageScheduleRequestsButton, "manageScheduleRequestsButton not injected");
            Objects.requireNonNull(logoutButton, "logoutButton FXML ID not injected");
        } catch (NullPointerException e) {
            System.err.println("FATAL: FXML injection failed in AdminManageBookingsController. Check FXML IDs.");
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Initialization Error", "UI components could not be loaded.");
            if(searchField != null) searchField.setDisable(true);
            if(bookingsTableView != null) bookingsTableView.setDisable(true);
            return;
        }

        setupNavigationButtons(homeButton, addRoomsButton, viewRoomsButton, manageBookingsButton, manageScheduleRequestsButton, logoutButton);
        if (manageBookingsButton != null) {
            manageBookingsButton.setDisable(true);
            manageBookingsButton.setStyle("-fx-background-radius: 100; -fx-background-color: #596572; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
            manageBookingsButton.setTextFill(javafx.scene.paint.Color.WHITE);
        }
        // --- Configure Table Columns using LAMBDAS ---
        userCol.setCellValueFactory(cellData -> {
            Booking booking = cellData.getValue();
            return new SimpleStringProperty(booking != null ? booking.getFacultyUsername() : "");
        });

        roleCol.setCellValueFactory(cellData -> {
            Booking booking = cellData.getValue();
            return new SimpleStringProperty(booking != null ? booking.getFacultyRole() : "");
        });

        roomNoCol.setCellValueFactory(cellData -> {
            Booking booking = cellData.getValue();
            return new SimpleStringProperty(booking != null ? booking.getRoomNumber() : "");
        });

        roomTypeCol.setCellValueFactory(cellData -> {
            Booking booking = cellData.getValue();
            return new SimpleStringProperty(booking != null ? booking.getRoomType() : "");
        });

        timeCol.setCellValueFactory(cellData -> {
            Booking booking = cellData.getValue();
            return new SimpleStringProperty(booking != null ? booking.getTimeScheduleDisplay() : "");
        });

        dayCol.setCellValueFactory(cellData -> {
            Booking booking = cellData.getValue();
            return new SimpleStringProperty(booking != null ? booking.getDayDisplay() : "");
        });

        // Action/Status Column (dynamically populated using cell factory)
        setupActionColumn();

        // --- Load Data and Setup Filtering ---
        try {
            masterBookingList = FXCollections.observableArrayList(DataStore.getAllBookings());
            System.out.println("\n--- AdminManageBookingsController Loaded " + (masterBookingList != null ? masterBookingList.size() : 0) + " Bookings ---");
            filteredBookingList = new FilteredList<>(masterBookingList, p -> true);

            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredBookingList.setPredicate(booking -> {
                    if (newValue == null || newValue.isEmpty()) return true;
                    if (booking == null) return false;
                    String lowerCaseFilter = newValue.toLowerCase().trim();
                    if (booking.getFacultyUsername() != null && booking.getFacultyUsername().toLowerCase().contains(lowerCaseFilter)) return true;
                    if (booking.getRoomNumber() != null && booking.getRoomNumber().toLowerCase().contains(lowerCaseFilter)) return true;
                    if (booking.getRoomType() != null && booking.getRoomType().toLowerCase().contains(lowerCaseFilter)) return true;
                    if (booking.getDate() != null && booking.getDate().toString().contains(lowerCaseFilter)) return true;
                    if (booking.getDayDisplay() != null && booking.getDayDisplay().toLowerCase().contains(lowerCaseFilter)) return true;
                    if (booking.getStatusDisplay() != null && booking.getStatusDisplay().toLowerCase().contains(lowerCaseFilter)) return true;
                    if (booking.getTimeScheduleDisplay() != null && booking.getTimeScheduleDisplay().toLowerCase().contains(lowerCaseFilter)) return true;
                    if (booking.getFacultyRole() != null && booking.getFacultyRole().toLowerCase().contains(lowerCaseFilter)) return true;
                    return false;
                });
            });

            bookingsTableView.setItems(filteredBookingList);
            bookingsTableView.setPlaceholder(new Label("No bookings found matching your criteria."));

        } catch (Exception e) {
            System.err.println("Error during AdminManageBookingsController data loading/filtering: " + e.getMessage());
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Data Load Error", "Failed to load or filter booking data.");
            if (bookingsTableView != null) {
                bookingsTableView.setPlaceholder(new Label("Error loading booking data."));
            }
        }
    }

    private void setupActionColumn() {
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
                            int index = getIndex();
                            if (!isEmpty() && index >= 0 && index < getTableView().getItems().size()) {
                                Booking booking = getTableView().getItems().get(index);
                                if (booking != null) {
                                    updateBookingStatus(booking, BookingStatus.APPROVED);
                                }
                            }
                        });
                        rejectBtn.setOnAction((ActionEvent event) -> {
                            int index = getIndex();
                            if (!isEmpty() && index >= 0 && index < getTableView().getItems().size()) {
                                Booking booking = getTableView().getItems().get(index);
                                if (booking != null) {
                                    updateBookingStatus(booking, BookingStatus.REJECTED);
                                }
                            }
                        });

                        approveBtn.setStyle("-fx-background-color: #8fbc8f; -fx-text-fill: white; -fx-font-weight: bold;");
                        rejectBtn.setStyle("-fx-background-color: #f08080; -fx-text-fill: white; -fx-font-weight: bold;");
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
                            if (booking == null) {
                                setGraphic(null);
                                return;
                            }

                            if (booking.getStatus() == BookingStatus.PENDING) {
                                pane.getChildren().setAll(approveBtn, rejectBtn);
                                setGraphic(pane);
                            } else {
                                statusLabel.setText(booking.getStatusDisplay());
                                switch (booking.getStatus()) {
                                    case APPROVED:
                                        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                                        break;
                                    case REJECTED:
                                    case CANCELLED:
                                        statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                                        break;
                                    default:
                                        statusLabel.setStyle("");
                                        break;
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
        if (booking == null) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Action");
        confirmation.setHeaderText("Update Booking Status?");
        confirmation.setContentText("Are you sure you want to set the status of booking ID "
                + booking.getBookingId() + " for user '" + booking.getFacultyUsername()
                + "' (" + booking.getRoomNumber() + " on " + booking.getDate() + ")"
                + " to " + newStatus + "?");

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            Booking bookingToUpdate = DataStore.findBookingById(booking.getBookingId());
            if (bookingToUpdate != null) {
                bookingToUpdate.setStatus(newStatus);
                bookingsTableView.refresh();
                SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Success", "Booking status updated to " + newStatus + ".");
            } else {
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Error", "Could not find the booking (ID: " + booking.getBookingId() + ") to update. It might have been removed.");
                masterBookingList.setAll(DataStore.getAllBookings());
                bookingsTableView.refresh();
            }
        }
    }
}