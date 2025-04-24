package com.example.roomutilizationsystem;

import javafx.beans.property.SimpleStringProperty; // Required for lambda usage
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory; // Still potentially used for other columns if added later
import javafx.util.Callback;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class AdminViewRoomsController extends AdminBaseController {

    @FXML private TextField searchField;
    @FXML private TableView<RoomSchedule> roomsTableView;

    // Inject Table Columns (Ensure fx:id is set in FXML for ALL columns)
    @FXML private TableColumn<RoomSchedule, Void> editCol;
    @FXML private TableColumn<RoomSchedule, String> roomNoCol;
    @FXML private TableColumn<RoomSchedule, String> roomTypeCol;
    @FXML private TableColumn<RoomSchedule, String> timeCol;
    @FXML private TableColumn<RoomSchedule, String> dayCol;
    @FXML private TableColumn<RoomSchedule, Void> removeCol;

    // Inject Sidebar Buttons (Ensure fx:id is set in FXML)
    @FXML private Button homeButton;
    @FXML private Button addRoomsButton;
    @FXML private Button viewRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button logoutButton;

    private ObservableList<RoomSchedule> masterScheduleList;
    private FilteredList<RoomSchedule> filteredScheduleList;

    @FXML
    public void initialize() {
        // Setup sidebar buttons
        setupNavigationButtons(homeButton, addRoomsButton, viewRoomsButton, manageBookingsButton, logoutButton);
        // Disable the button for the current page
        if (viewRoomsButton != null) {
            viewRoomsButton.setDisable(true);
            viewRoomsButton.setStyle("-fx-background-radius: 100; -fx-background-color: #596572;");
            viewRoomsButton.setTextFill(javafx.scene.paint.Color.WHITE);
        }

        // --- Configure Table ---
        if (roomNoCol != null) {
            // Use Lambda instead of PropertyValueFactory
            roomNoCol.setCellValueFactory(cellData -> {
                RoomSchedule schedule = cellData.getValue();
                return new SimpleStringProperty(schedule != null ? schedule.getRoomNumber() : "");
            });
        } else {
            System.err.println("AdminViewRoomsController: roomNoCol is null. Check FXML fx:id.");
        }

        if (roomTypeCol != null) {
            // Use Lambda instead of PropertyValueFactory
            roomTypeCol.setCellValueFactory(cellData -> {
                RoomSchedule schedule = cellData.getValue();
                return new SimpleStringProperty(schedule != null ? schedule.getRoomType() : "");
            });
        } else {
            System.err.println("AdminViewRoomsController: roomTypeCol is null. Check FXML fx:id.");
        }

        // Format time range using the method from RoomSchedule
        if (timeCol != null) {
            timeCol.setCellValueFactory(cellData -> {
                RoomSchedule schedule = cellData.getValue();
                return new SimpleStringProperty(schedule != null ? schedule.getTimeRangeString() : "");
            });
        } else {
            System.err.println("AdminViewRoomsController: timeCol is null. Check FXML fx:id.");
        }

        // Format day using the method from RoomSchedule
        if (dayCol != null) {
            dayCol.setCellValueFactory(cellData -> {
                RoomSchedule schedule = cellData.getValue();
                return new SimpleStringProperty(schedule != null ? schedule.getDayColDisplay() : "");
            });
        } else {
            System.err.println("AdminViewRoomsController: dayCol is null. Check FXML fx:id.");
        }

        // Add Edit Button Column
        if(editCol != null) setupActionColumn(editCol, "Edit", this::handleEditAction);
        else System.err.println("AdminViewRoomsController: editCol is null. Check FXML fx:id.");

        // Add Remove Button Column
        if(removeCol != null) setupActionColumn(removeCol, "Remove", this::handleRemoveAction);
        else System.err.println("AdminViewRoomsController: removeCol is null. Check FXML fx:id.");

        // --- Load Data and Setup Filtering ---
        try {
            masterScheduleList = FXCollections.observableArrayList(DataStore.getAllRoomSchedules());

            // Debug print (kept for verification)
            System.out.println("\n--- AdminViewRoomsController Loaded Schedules ---");
            if (masterScheduleList == null || masterScheduleList.isEmpty()) { System.out.println("No schedules loaded from DataStore."); }
            else { masterScheduleList.forEach(schedule -> { if (schedule == null) { System.out.println("  Loaded a null schedule object."); } else { System.out.println("  ID: " + schedule.getScheduleId() + ", Room: [" + schedule.getRoomNumber() + "]" + ", Type: [" + schedule.getRoomType() + "]" + ", Day: " + schedule.getDayOfWeek() + ", Time: " + schedule.getTimeRangeString()); } }); }
            System.out.println("-----------------------------------------------");

            filteredScheduleList = new FilteredList<>(masterScheduleList, p -> true);

            if (searchField != null) {
                searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                    filteredScheduleList.setPredicate(schedule -> {
                        if (newValue == null || newValue.isEmpty()) return true;
                        if (schedule == null) return false;
                        String lowerCaseFilter = newValue.toLowerCase();
                        if (schedule.getRoomNumber() != null && schedule.getRoomNumber().toLowerCase().contains(lowerCaseFilter)) return true;
                        if (schedule.getRoomType() != null && schedule.getRoomType().toLowerCase().contains(lowerCaseFilter)) return true;
                        if (schedule.getTimeRangeString() != null && schedule.getTimeRangeString().toLowerCase().contains(lowerCaseFilter)) return true;
                        if (schedule.getDayOfWeek() != null && schedule.getDayOfWeek().toString().toLowerCase().contains(lowerCaseFilter)) return true;
                        return false;
                    });
                });
            } else { System.err.println("AdminViewRoomsController: searchField is null. Check FXML fx:id."); }

            if (roomsTableView != null) {
                roomsTableView.setItems(filteredScheduleList);
                roomsTableView.setPlaceholder(new Label("No room schedules found. Add schedules using the 'Add Rooms' page."));
            } else { System.err.println("AdminViewRoomsController: roomsTableView is null. Check FXML fx:id."); }

        } catch (Exception e) {
            System.err.println("Error during AdminViewRoomsController initialization: " + e.getMessage());
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Initialization Error", "Failed to load room schedule data.");
            if (roomsTableView != null) { roomsTableView.setPlaceholder(new Label("Error loading data.")); }
        }
    }

    // Generic method to setup columns with a single button
    private void setupActionColumn(TableColumn<RoomSchedule, Void> column, String buttonText, java.util.function.Consumer<RoomSchedule> actionHandler) {
        Callback<TableColumn<RoomSchedule, Void>, TableCell<RoomSchedule, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<RoomSchedule, Void> call(final TableColumn<RoomSchedule, Void> param) {
                final TableCell<RoomSchedule, Void> cell = new TableCell<>() {
                    private final Button btn = new Button(buttonText);
                    {
                        btn.setOnAction((ActionEvent event) -> {
                            if (!isEmpty() && getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                                RoomSchedule schedule = getTableView().getItems().get(getIndex());
                                if (schedule != null) actionHandler.accept(schedule);
                            }
                        });
                        if ("Remove".equalsIgnoreCase(buttonText)) btn.setStyle("-fx-background-color: #f08080; -fx-text-fill: white;");
                        else if ("Edit".equalsIgnoreCase(buttonText)) btn.setStyle("-fx-background-color: #add8e6;");
                        btn.setMinWidth(70);
                    }
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty || getIndex() < 0 ? null : btn);
                    }
                };
                return cell;
            }
        };
        column.setCellFactory(cellFactory);
    }

    private void handleEditAction(RoomSchedule schedule) {
        SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Edit Schedule",
                "Editing schedule ID: " + schedule.getScheduleId() + "\n" +
                        "Room: " + schedule.getRoomNumber() + " (" + schedule.getRoomType() + ")\n" +
                        "Time: " + schedule.getTimeRangeString() + "\n" +
                        "Day: " + schedule.getDayColDisplay() + "\n\n" +
                        "(To modify, please Remove this schedule and Add a new one with the desired changes.)");
    }

    private void handleRemoveAction(RoomSchedule schedule) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Room Schedule?");
        confirmation.setContentText("Are you sure you want to delete this schedule?\n" +
                "Room: " + schedule.getRoomNumber() + ", Day: " + schedule.getDayColDisplay() +
                ", Time: " + schedule.getTimeRangeString() +
                "\n\nNote: This prevents future bookings for this exact schedule slot but does NOT automatically cancel existing bookings.");
        confirmation.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean removed = DataStore.removeRoomSchedule(schedule.getScheduleId());
            if (removed) {
                masterScheduleList.remove(schedule);
                SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Success", "Schedule removed successfully.");
            } else {
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Error", "Could not remove the schedule (it might have already been removed).");
            }
        }
    }
}