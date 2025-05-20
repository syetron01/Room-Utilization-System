package com.example.roomutilizationsystem;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import java.util.Objects;

public class AdminViewRoomsController extends AdminBaseController {

    @FXML private TextField searchField;
    @FXML private TableView<RoomSchedule> roomsTableView;

    @FXML private TableColumn<RoomSchedule, String> roomNoCol;
    @FXML private TableColumn<RoomSchedule, String> roomTypeCol;
    @FXML private TableColumn<RoomSchedule, String> daysOfWeekCol;
    @FXML private TableColumn<RoomSchedule, String> timeCol;
    @FXML private TableColumn<RoomSchedule, String> startDateCol;
    @FXML private TableColumn<RoomSchedule, String> endDateCol;
    @FXML private TableColumn<RoomSchedule, String> definitionTypeCol;
    @FXML private TableColumn<RoomSchedule, Void> statusCol;

    @FXML private Button homeButton;
    @FXML private Button addRoomsButton;
    @FXML private Button viewRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button manageScheduleRequestsButton;
    @FXML private Button logoutButton;

    private ObservableList<RoomSchedule> masterScheduleList;
    private FilteredList<RoomSchedule> filteredScheduleList;

    @FXML
    public void initialize() {
        try {
            Objects.requireNonNull(roomsTableView, "roomsTableView FXML ID not injected");
            Objects.requireNonNull(roomNoCol, "roomNoCol FXML ID not injected");
            Objects.requireNonNull(roomTypeCol, "roomTypeCol FXML ID not injected");
            Objects.requireNonNull(daysOfWeekCol, "daysOfWeekCol FXML ID not injected");
            Objects.requireNonNull(timeCol, "timeCol FXML ID not injected");
            Objects.requireNonNull(startDateCol, "startDateCol FXML ID not injected");
            Objects.requireNonNull(endDateCol, "endDateCol FXML ID not injected");
            Objects.requireNonNull(definitionTypeCol, "definitionTypeCol FXML ID not injected");
            Objects.requireNonNull(statusCol, "statusCol FXML ID not injected");
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
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Initialization Error", "UI components could not be loaded.");
            if(roomsTableView != null) roomsTableView.setDisable(true);
            if(searchField != null) searchField.setDisable(true);
            return;
        }

        setupNavigationButtons(homeButton, addRoomsButton, viewRoomsButton, manageBookingsButton, manageScheduleRequestsButton, logoutButton);
        viewRoomsButton.setDisable(true);
        viewRoomsButton.setStyle("-fx-background-radius: 100; -fx-background-color: #596572; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        configureTableColumns();
        loadAndFilterData();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredScheduleList.setPredicate(schedule -> filterPredicate(schedule, newValue));
        });
    }

    private void configureTableColumns() {
        roomNoCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRoomNumber()));
        roomTypeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRoomType()));
        daysOfWeekCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDaysOfWeekDisplay()));
        timeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTimeColDisplay()));
        startDateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDefinitionStartDateDisplay()));
        endDateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDefinitionEndDateDisplay()));
        definitionTypeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDefinitionTypeDisplay()));
        setupStatusColumn();
    }

    private void setupStatusColumn() {
        Callback<TableColumn<RoomSchedule, Void>, TableCell<RoomSchedule, Void>> cellFactory = param -> {
            final TableCell<RoomSchedule, Void> cell = new TableCell<>() {
                private final ToggleButton toggleBtn = new ToggleButton();
                private final HBox pane = new HBox(toggleBtn);

                {
                    pane.setAlignment(Pos.CENTER);
                    toggleBtn.setMinWidth(80); // Adjusted width
                    toggleBtn.setOnAction((ActionEvent event) -> {
                        int index = getIndex();
                        if (index >= 0 && index < getTableView().getItems().size()) {
                            RoomSchedule scheduleDef = getTableView().getItems().get(index);
                            if (scheduleDef != null) {
                                boolean newStatus = toggleBtn.isSelected();
                                scheduleDef.setActive(newStatus);
                                DataStore.saveRoomSchedulesOnly();
                                updateButtonAppearance(scheduleDef);
                                // roomsTableView.refresh(); // Not strictly needed if only this cell changes based on its own item's state
                                System.out.println("Schedule ID " + scheduleDef.getScheduleId() + " status set to " + scheduleDef.getStatusDisplay());
                                SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Status Updated",
                                        "Schedule " + scheduleDef.getScheduleId() + " is now " + scheduleDef.getStatusDisplay() + ".");
                            }
                        }
                    });
                }

                private void updateButtonAppearance(RoomSchedule scheduleDef) {
                    if (scheduleDef.isActive()) {
                        toggleBtn.setText("Active");
                        toggleBtn.setSelected(true);
                        toggleBtn.setStyle("-fx-base: #a0d9a0; -fx-text-fill: black; -fx-font-weight: bold;"); // Lighter green
                    } else {
                        toggleBtn.setText("Inactive");
                        toggleBtn.setSelected(false);
                        toggleBtn.setStyle("-fx-base: #f0a0a0; -fx-text-fill: black; -fx-font-weight: bold;"); // Lighter red
                    }
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        RoomSchedule scheduleDef = getTableRow().getItem();
                        updateButtonAppearance(scheduleDef);
                        setGraphic(pane);
                    }
                }
            };
            return cell;
        };
        statusCol.setCellFactory(cellFactory);
    }

    private void loadAndFilterData() {
        masterScheduleList = FXCollections.observableArrayList(DataStore.getAllRoomSchedules());
        filteredScheduleList = new FilteredList<>(masterScheduleList, p -> true);
        roomsTableView.setItems(filteredScheduleList);
        if (masterScheduleList.isEmpty()) {
            roomsTableView.setPlaceholder(new Label("No schedule definitions found. Add availability via 'Add Rooms'."));
        } else {
            roomsTableView.setPlaceholder(new Label("No schedule definitions match your search."));
        }
    }

    private boolean filterPredicate(RoomSchedule schedule, String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return true;
        }
        if (schedule == null) {
            return false;
        }
        String lowerCaseFilter = searchText.toLowerCase().trim();

        if (schedule.getRoomNumber().toLowerCase().contains(lowerCaseFilter)) return true;
        if (schedule.getRoomType().toLowerCase().contains(lowerCaseFilter)) return true;
        if (schedule.getDaysOfWeekDisplay().toLowerCase().contains(lowerCaseFilter)) return true;
        if (schedule.getTimeColDisplay().toLowerCase().contains(lowerCaseFilter)) return true;
        if (schedule.getDefinitionStartDateDisplay().contains(lowerCaseFilter)) return true;
        if (schedule.getDefinitionEndDateDisplay().contains(lowerCaseFilter)) return true;
        if (schedule.getDefinitionTypeDisplay().toLowerCase().contains(lowerCaseFilter)) return true;
        if (schedule.getScheduleId().toLowerCase().contains(lowerCaseFilter)) return true;
        if (schedule.getStatusDisplay().toLowerCase().contains(lowerCaseFilter)) return true; // Search by status text

        return false;
    }
}