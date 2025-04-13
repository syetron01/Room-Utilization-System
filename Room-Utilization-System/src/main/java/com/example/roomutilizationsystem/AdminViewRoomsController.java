package com.example.roomutilizationsystem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

// Import DataStore, RoomSchedule, SceneNavigator, AdminBaseController etc.

public class AdminViewRoomsController extends AdminBaseController {

    @FXML private TextField searchField;
    @FXML private TableView<RoomSchedule> roomsTableView; // Use RoomSchedule

    // Inject Table Columns (Set fx:id in FXML)
    @FXML private TableColumn<RoomSchedule, String> roomNoCol;
    @FXML private TableColumn<RoomSchedule, String> roomTypeCol;
    @FXML private TableColumn<RoomSchedule, String> timeCol;
    @FXML private TableColumn<RoomSchedule, String> dayCol;
    @FXML private TableColumn<RoomSchedule, Void> editCol;   // For Edit button
    @FXML private TableColumn<RoomSchedule, Void> removeCol; // For Remove button

    // Inject Sidebar Buttons
    @FXML private Button homeButton;
    @FXML private Button addRoomsButton;
    @FXML private Button viewRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button logoutButton;

    private ObservableList<RoomSchedule> masterScheduleList;
    private FilteredList<RoomSchedule> filteredScheduleList;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");


    @FXML
    public void initialize() {
        setupNavigationButtons(homeButton, addRoomsButton, viewRoomsButton, manageBookingsButton, logoutButton);
        viewRoomsButton.setDisable(true); // Disable current page button

        // --- Configure Table ---
        roomNoCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        roomTypeCol.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        // Format time range
        timeCol.setCellValueFactory(cellData -> {
            RoomSchedule schedule = cellData.getValue();
            String timeRange = schedule.getStartTime().format(timeFormatter) + " - " + schedule.getEndTime().format(timeFormatter);
            return new javafx.beans.property.SimpleStringProperty(timeRange);
        });
        dayCol.setCellValueFactory(cellData -> {
            DayOfWeek day = cellData.getValue().getDayOfWeek();
            String dayStr = day.toString();
            // Capitalize properly
            dayStr = dayStr.substring(0, 1).toUpperCase() + dayStr.substring(1).toLowerCase();
            return new javafx.beans.property.SimpleStringProperty(dayStr);
        });


        // Add Edit Button Column
        setupActionColumn(editCol, "Edit", this::handleEditAction);

        // Add Remove Button Column
        setupActionColumn(removeCol, "Remove", this::handleRemoveAction);


        // --- Load Data and Setup Filtering ---
        masterScheduleList = FXCollections.observableArrayList(DataStore.getAllRoomSchedules());
        filteredScheduleList = new FilteredList<>(masterScheduleList, p -> true); // Show all initially

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredScheduleList.setPredicate(schedule -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                if (schedule.getRoomNumber().toLowerCase().contains(lowerCaseFilter)) return true;
                if (schedule.getRoomType().toLowerCase().contains(lowerCaseFilter)) return true;
                if (schedule.getTimeRangeString().toLowerCase().contains(lowerCaseFilter)) return true; // Check formatted time
                if (schedule.getDayOfWeek().toString().toLowerCase().contains(lowerCaseFilter)) return true;

                return false;
            });
        });

        roomsTableView.setItems(filteredScheduleList);
        roomsTableView.setPlaceholder(new Label("No room schedules found."));
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
                            RoomSchedule schedule = getTableView().getItems().get(getIndex());
                            actionHandler.accept(schedule);
                        });
                        // Optional: Add styling based on button text
                        if ("Remove".equalsIgnoreCase(buttonText)) {
                            btn.setStyle("-fx-background-color: #f08080; -fx-text-fill: white;");
                        } else {
                            btn.setStyle("-fx-background-color: #add8e6;"); // Light blue for Edit
                        }
                        btn.setMinWidth(70); // Ensure buttons have reasonable width
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
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

    private void handleEditAction(RoomSchedule schedule) {
        // **Implementation Note:** Editing is complex.
        // Option 1: Open a new Dialog/Window pre-filled with schedule data.
        // Option 2: Allow inline editing in the table (more complex).
        // Option 3: For simplicity here, just show info and remind user to delete and re-add.
        SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Edit Schedule",
                "Editing schedule ID: " + schedule.getScheduleId() + "\n" +
                        "Room: " + schedule.getRoomNumber() + " (" + schedule.getRoomType() + ")\n" +
                        "Time: " + schedule.getTimeRangeString() + "\n" +
                        "Day: " + schedule.getDayOfWeek() + "\n\n" +
                        "(Functionality to modify directly is not implemented in this demo. Please Remove and Add again if changes are needed.)");

        // To implement fully, you'd load a form/dialog here.
    }

    private void handleRemoveAction(RoomSchedule schedule) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Room Schedule?");
        confirmation.setContentText("Are you sure you want to delete this schedule?\n" + schedule +
                "\n\nNote: This will NOT delete existing bookings for this schedule, but will prevent future bookings.");
        confirmation.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);


        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean removed = DataStore.removeRoomSchedule(schedule.getScheduleId());
            if (removed) {
                masterScheduleList.remove(schedule); // Remove from the underlying list for the table
                // No need to call setItems again, the FilteredList updates automatically.
                SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Success", "Schedule removed successfully.");
            } else {
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Error", "Could not remove the schedule.");
            }
        }
    }
}
