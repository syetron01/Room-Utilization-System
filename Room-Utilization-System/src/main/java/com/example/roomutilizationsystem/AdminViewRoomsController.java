// FILE: com/example/roomutilizationsystem/AdminViewRoomsController.java
package com.example.roomutilizationsystem;

// Add this import
import javafx.beans.property.SimpleStringProperty;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
// PropertyValueFactory is no longer needed for these columns
// import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox; // Keep if using HBox in action columns
import javafx.util.Callback;

import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.List; // Added for filterPredicate clarity
import java.util.stream.Collectors; // Added for filterPredicate clarity


public class AdminViewRoomsController extends AdminBaseController {

    @FXML private TextField searchField;
    @FXML private TableView<RoomSchedule> roomsTableView;

    // Columns for RoomSchedule Definitions (Ensure fx:id match FXML)
    @FXML private TableColumn<RoomSchedule, String> roomNoCol;
    @FXML private TableColumn<RoomSchedule, String> roomTypeCol;
    @FXML private TableColumn<RoomSchedule, String> daysOfWeekCol;
    @FXML private TableColumn<RoomSchedule, String> timeCol;
    @FXML private TableColumn<RoomSchedule, String> startDateCol;
    @FXML private TableColumn<RoomSchedule, String> endDateCol;
    @FXML private TableColumn<RoomSchedule, String> definitionTypeCol;
    @FXML private TableColumn<RoomSchedule, Void> removeCol;

    // Sidebar Buttons
    @FXML private Button homeButton;
    @FXML private Button addRoomsButton;
    @FXML private Button viewRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button manageScheduleRequestsButton;
    @FXML private Button logoutButton;

    // Data lists for DEFINITIONS
    private ObservableList<RoomSchedule> masterScheduleList;
    private FilteredList<RoomSchedule> filteredScheduleList;

    @FXML
    public void initialize() {

        // Defend against NullPointerException if FXML injection fails
        try {
            Objects.requireNonNull(roomsTableView, "roomsTableView FXML ID not injected");
            Objects.requireNonNull(roomNoCol, "roomNoCol FXML ID not injected");
            Objects.requireNonNull(roomTypeCol, "roomTypeCol FXML ID not injected");
            Objects.requireNonNull(daysOfWeekCol, "daysOfWeekCol FXML ID not injected");
            Objects.requireNonNull(timeCol, "timeCol FXML ID not injected");
            Objects.requireNonNull(startDateCol, "startDateCol FXML ID not injected");
            Objects.requireNonNull(endDateCol, "endDateCol FXML ID not injected");
            Objects.requireNonNull(definitionTypeCol, "definitionTypeCol FXML ID not injected");
            Objects.requireNonNull(removeCol, "removeCol FXML ID not injected");
            Objects.requireNonNull(searchField, "searchField FXML ID not injected");
            Objects.requireNonNull(homeButton, "homeButton FXML ID not injected");
            Objects.requireNonNull(addRoomsButton, "addRoomsButton FXML ID not injected");
            Objects.requireNonNull(viewRoomsButton, "viewRoomsButton FXML ID not injected");
            Objects.requireNonNull(manageBookingsButton, "manageBookingsButton FXML ID not injected");
            Objects.requireNonNull(manageScheduleRequestsButton, "manageScheduleRequestsButton FXML ID not injected");
            Objects.requireNonNull(logoutButton, "logoutButton FXML ID not injected");

        } catch (NullPointerException e) {
            System.err.println("FATAL: FXML injection failed in AdminViewRoomsController. Check FXML IDs.");
            e.printStackTrace();
            // Optionally show an alert and disable the screen
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Initialization Error", "UI components could not be loaded. Check FXML file and controller bindings.");
            if(roomsTableView != null) roomsTableView.setDisable(true);
            if(searchField != null) searchField.setDisable(true);
            return; // Stop initialization
        }


        setupNavigationButtons(homeButton, addRoomsButton, viewRoomsButton, manageBookingsButton, manageScheduleRequestsButton, logoutButton);
        viewRoomsButton.setDisable(true); // Disable current page button
        viewRoomsButton.setStyle("-fx-background-radius: 100; -fx-background-color: #596572; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;"); // Style disabled button


        configureTableColumns(); // Setup columns for RoomSchedule definition properties

        loadAndFilterData(); // Load definitions and set up filtering

        // Add listener to search field
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredScheduleList.setPredicate(schedule -> filterPredicate(schedule, newValue));
        });
    }

    // --- UPDATED configureTableColumns to use Lambdas ---
    private void configureTableColumns() {
        // Use lambda expressions for CellValueFactory - more robust with modules
        roomNoCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue() != null ? cellData.getValue().getRoomNumber() : "")
        );
        roomTypeCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue() != null ? cellData.getValue().getRoomType() : "")
        );
        daysOfWeekCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue() != null ? cellData.getValue().getDaysOfWeekDisplay() : "")
        );
        timeCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue() != null ? cellData.getValue().getTimeColDisplay() : "")
        );
        startDateCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue() != null ? cellData.getValue().getDefinitionStartDateDisplay() : "")
        );
        endDateCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue() != null ? cellData.getValue().getDefinitionEndDateDisplay() : "")
        );
        definitionTypeCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue() != null ? cellData.getValue().getDefinitionTypeDisplay() : "")
        );

        // Setup Remove Button Column (remains the same)
        setupRemoveColumn();
    }
    // --- End of UPDATED configureTableColumns ---


    private void setupRemoveColumn() {
        Callback<TableColumn<RoomSchedule, Void>, TableCell<RoomSchedule, Void>> cellFactory = param -> {
            final TableCell<RoomSchedule, Void> cell = new TableCell<>() {
                private final Button removeBtn = new Button("Remove");
                {
                    removeBtn.setStyle("-fx-background-color: #f08080; -fx-text-fill: white;"); // Red color
                    removeBtn.setOnAction((ActionEvent event) -> {
                        // Defensive check for index validity
                        int index = getIndex();
                        if (index >= 0 && index < getTableView().getItems().size()) {
                            RoomSchedule scheduleDef = getTableView().getItems().get(index);
                            if (scheduleDef != null) {
                                handleRemoveButton(scheduleDef);
                            } else {
                                System.err.println("Remove button clicked on null data at index: " + index);
                                SceneNavigator.showAlert(Alert.AlertType.WARNING, "Error", "Cannot remove null item.");
                            }
                        } else {
                            System.err.println("Remove button clicked on invalid row index: " + index);
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    // Show button only for non-empty rows within valid index range
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                    } else {
                        setGraphic(removeBtn);
                    }
                }
            };
            return cell;
        };
        removeCol.setCellFactory(cellFactory);
    }

    private void loadAndFilterData() {
        // Load ALL schedule DEFINITIONS from DataStore
        masterScheduleList = FXCollections.observableArrayList(DataStore.getAllRoomSchedules());
        System.out.println("AdminViewRooms: Loaded " + masterScheduleList.size() + " schedule definitions."); // Debug

        // Wrap in FilteredList for searching
        filteredScheduleList = new FilteredList<>(masterScheduleList, p -> true); // Initially show all

        // Bind the filtered list to the TableView
        roomsTableView.setItems(filteredScheduleList);
        if (masterScheduleList.isEmpty()) {
            roomsTableView.setPlaceholder(new Label("No schedule definitions found. Add availability via 'Add Rooms'."));
        } else {
            // Clear placeholder if data exists, TableView handles empty filtered list internally
            roomsTableView.setPlaceholder(null);
        }
    }

    // Predicate for filtering schedule definitions
    private boolean filterPredicate(RoomSchedule schedule, String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return true; // Show all if search text is empty
        }
        // Ensure schedule is not null before accessing its methods
        if (schedule == null) {
            return false;
        }
        String lowerCaseFilter = searchText.toLowerCase().trim();

        // Check against various fields using safe null checks where needed
        if (schedule.getRoomNumber() != null && schedule.getRoomNumber().toLowerCase().contains(lowerCaseFilter)) return true;
        if (schedule.getRoomType() != null && schedule.getRoomType().toLowerCase().contains(lowerCaseFilter)) return true;
        if (schedule.getDaysOfWeekDisplay() != null && schedule.getDaysOfWeekDisplay().toLowerCase().contains(lowerCaseFilter)) return true;
        if (schedule.getTimeColDisplay() != null && schedule.getTimeColDisplay().toLowerCase().contains(lowerCaseFilter)) return true;
        if (schedule.getDefinitionStartDateDisplay() != null && schedule.getDefinitionStartDateDisplay().contains(lowerCaseFilter)) return true;
        if (schedule.getDefinitionEndDateDisplay() != null && schedule.getDefinitionEndDateDisplay().contains(lowerCaseFilter)) return true;
        if (schedule.getDefinitionTypeDisplay() != null && schedule.getDefinitionTypeDisplay().toLowerCase().contains(lowerCaseFilter)) return true;
        if (schedule.getScheduleId() != null && schedule.getScheduleId().toLowerCase().contains(lowerCaseFilter)) return true;

        return false; // No match found
    }

    // Handles removing the ENTIRE schedule definition
    private void handleRemoveButton(RoomSchedule scheduleDef) {
        if (scheduleDef == null) {
            System.err.println("Attempted to remove a null schedule definition.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Removal");
        confirmation.setHeaderText("Remove Entire Schedule Definition?");
        // Use getters for confirmation message content
        confirmation.setContentText("Are you sure you want to remove the entire availability definition:\n" +
                "ID: " + scheduleDef.getScheduleId() + "\n" +
                "Room: " + scheduleDef.getRoomNumber() + " (" + scheduleDef.getRoomType() + ")\n" +
                "Days: " + scheduleDef.getDaysOfWeekDisplay() + "\n" +
                "Time: " + scheduleDef.getTimeColDisplay() + "\n" +
                "Range: " + scheduleDef.getDefinitionStartDateDisplay() + " to " + scheduleDef.getDefinitionEndDateDisplay() + "\n\n" +
                "Existing bookings for slots within this definition will NOT be cancelled automatically.");
        confirmation.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE); // Ensure dialog fits text

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean removed = DataStore.removeRoomSchedule(scheduleDef.getScheduleId()); // removeRoomSchedule handles saving
            if (removed) {
                // Since removeRoomSchedule modifies the underlying source list
                // that masterScheduleList is based on (assuming DataStore returns a direct list reference or copy),
                // removing from masterScheduleList directly reflects the change for the FilteredList.
                // However, reloading is the absolute safest way if DataStore implementation changes.
                // Let's stick to removing from the local observable list for efficiency IF DataStore.remove modifies the list used by getAll.
                // If DataStore.getAll creates a NEW list each time, then reloading is necessary.
                // Assuming DataStore works on the live list:
                // masterScheduleList.remove(scheduleDef); // No longer needed if we reload

                // Reloading data to ensure UI matches persistent state
                loadAndFilterData(); // Reload definitions from DataStore

                SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Success", "Schedule definition removed successfully.");
            } else {
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Error", "Failed to remove schedule definition (ID: " + scheduleDef.getScheduleId() + "). It might have already been removed.");
                loadAndFilterData(); // Reload to be sure
            }
        }
    }

    // Base controller navigation methods are inherited
}