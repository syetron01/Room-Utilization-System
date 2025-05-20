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
import java.util.Optional;

import static com.example.roomutilizationsystem.DataStore.addRoomScheduleDefinition;
import static com.example.roomutilizationsystem.DataStore.getAllScheduleRequests;

public class AdminManageScheduleRequestsController extends AdminBaseController {

    @FXML private TextField searchField;
    @FXML private TableView<ScheduleRequest> scheduleRequestsTableView;

    // Table Columns
    @FXML private TableColumn<ScheduleRequest, String> requestIdCol;
    @FXML private TableColumn<ScheduleRequest, String> requestedByCol;
    @FXML private TableColumn<ScheduleRequest, String> roomNoCol;
    @FXML private TableColumn<ScheduleRequest, String> roomTypeCol;
    @FXML private TableColumn<ScheduleRequest, String> daysCol;
    @FXML private TableColumn<ScheduleRequest, String> timeCol;
    @FXML private TableColumn<ScheduleRequest, String> dateRangeCol;
    @FXML private TableColumn<ScheduleRequest, String> statusCol;
    @FXML private TableColumn<ScheduleRequest, Void> actionCol;

    // Sidebar Buttons
    @FXML private Button homeButton;
    @FXML private Button addRoomsButton;
    @FXML private Button viewRoomsButton;
    @FXML private Button manageBookingsButton;
    @FXML private Button manageScheduleRequestsButton; // This page's button
    @FXML private Button logoutButton;

    private ObservableList<ScheduleRequest> masterRequestList;
    private FilteredList<ScheduleRequest> filteredRequestList;

    @FXML
    public void initialize() {

        // Null checks for FXML elements
        try {
            Objects.requireNonNull(searchField, "searchField not injected");
            Objects.requireNonNull(scheduleRequestsTableView, "scheduleRequestsTableView not injected");
            Objects.requireNonNull(requestIdCol, "requestIdCol not injected");
            Objects.requireNonNull(requestedByCol, "requestedByCol not injected");
            Objects.requireNonNull(roomNoCol, "roomNoCol not injected");
            Objects.requireNonNull(roomTypeCol, "roomTypeCol not injected");
            Objects.requireNonNull(daysCol, "daysCol not injected");
            Objects.requireNonNull(timeCol, "timeCol not injected");
            Objects.requireNonNull(dateRangeCol, "dateRangeCol not injected");
            Objects.requireNonNull(statusCol, "statusCol not injected");
            Objects.requireNonNull(actionCol, "actionCol not injected");

            Objects.requireNonNull(homeButton, "homeButton not injected");
            Objects.requireNonNull(addRoomsButton, "addRoomsButton not injected");
            Objects.requireNonNull(viewRoomsButton, "viewRoomsButton not injected");
            Objects.requireNonNull(manageBookingsButton, "manageBookingsButton not injected");
            Objects.requireNonNull(manageScheduleRequestsButton, "manageScheduleRequestsButton not injected");
            Objects.requireNonNull(logoutButton, "logoutButton not injected");
        } catch (NullPointerException e) {
            System.err.println("FATAL: FXML injection failed: " + e.getMessage());
            e.printStackTrace();
            SceneNavigator.showAlert(Alert.AlertType.ERROR, "Initialization Error", "UI components could not be loaded.");
            return;
        }

        setupNavigationButtons(homeButton, addRoomsButton, viewRoomsButton, manageBookingsButton, manageScheduleRequestsButton, logoutButton);
        if (manageScheduleRequestsButton != null) {
            manageScheduleRequestsButton.setDisable(true);
            manageScheduleRequestsButton.setStyle("-fx-background-radius: 100; -fx-background-color: #596572; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        }

        configureTableColumns();
        loadAndFilterData();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredRequestList.setPredicate(request -> filterPredicate(request, newValue));
        });
    }

    private void configureTableColumns() {
        requestIdCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRequestId()));
        requestedByCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRequestedByUsername()));
        roomNoCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRoomNumber()));
        roomTypeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRoomType()));
        daysCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDaysOfWeekDisplay()));
        timeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTimeRangeString()));
        dateRangeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDefinitionDateRangeDisplay()));

        statusCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatusDisplay()));
        statusCol.setCellFactory(column -> new TableCell<ScheduleRequest, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty || getTableRow() == null || getTableRow().getItem() == null) { // Added checks for getTableRow and getItem
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    ScheduleRequest sr = getTableRow().getItem(); // Use getTableRow().getItem()
                    // ScheduleRequest sr = getTableView().getItems().get(getIndex()); // getIndex() can be unreliable with filtered lists
                    if (sr != null) {
                        switch (sr.getStatus()) {
                            case APPROVED:
                                setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                                break;
                            case REJECTED:
                                setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                                break;
                            case PENDING_APPROVAL:
                                setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                                break;
                            default:
                                setStyle("");
                        }
                    }
                }
            }
        });

        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button approveBtn = new Button("Approve");
            private final Button rejectBtn = new Button("Reject");
            private final HBox pane = new HBox(5, approveBtn, rejectBtn);

            {
                approveBtn.setStyle("-fx-background-color: #8fbc8f; -fx-text-fill: white; -fx-font-weight: bold;");
                rejectBtn.setStyle("-fx-background-color: #f08080; -fx-text-fill: white; -fx-font-weight: bold;");
                approveBtn.setMinWidth(60);
                rejectBtn.setMinWidth(60);
                pane.setAlignment(Pos.CENTER);

                approveBtn.setOnAction(event -> {
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        ScheduleRequest request = getTableRow().getItem();
                        handleApproveRequest(request);
                    }
                });
                rejectBtn.setOnAction(event -> {
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        ScheduleRequest request = getTableRow().getItem();
                        handleRejectRequest(request);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    ScheduleRequest request = getTableRow().getItem();
                    if (request.getStatus() == ScheduleRequestStatus.PENDING_APPROVAL) {
                        setGraphic(pane);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void loadAndFilterData() {
        masterRequestList = FXCollections.observableArrayList(getAllScheduleRequests());
        filteredRequestList = new FilteredList<>(masterRequestList, p -> true);
        scheduleRequestsTableView.setItems(filteredRequestList);
        scheduleRequestsTableView.setPlaceholder(new Label("No schedule requests found."));
    }

    private boolean filterPredicate(ScheduleRequest request, String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) return true;
        if (request == null) return false;
        String lowerCaseFilter = searchText.toLowerCase().trim();

        if (request.getRequestId().toLowerCase().contains(lowerCaseFilter)) return true;
        if (request.getRequestedByUsername().toLowerCase().contains(lowerCaseFilter)) return true;
        if (request.getRoomNumber().toLowerCase().contains(lowerCaseFilter)) return true;
        if (request.getRoomType().toLowerCase().contains(lowerCaseFilter)) return true;
        if (request.getDaysOfWeekDisplay().toLowerCase().contains(lowerCaseFilter)) return true;
        if (request.getTimeRangeString().toLowerCase().contains(lowerCaseFilter)) return true;
        if (request.getDefinitionDateRangeDisplay().toLowerCase().contains(lowerCaseFilter)) return true;
        if (request.getStatusDisplay().toLowerCase().contains(lowerCaseFilter)) return true;
        return false;
    }

    private void handleApproveRequest(ScheduleRequest request) {
        if (request == null || request.getStatus() != ScheduleRequestStatus.PENDING_APPROVAL) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Action Invalid", "This request is not pending approval.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Approval");
        confirmation.setHeaderText("Approve Schedule Request?");
        confirmation.setContentText("Are you sure you want to approve this schedule request and create the room availability?\n" +
                "Request ID: " + request.getRequestId() + "\n" +
                "Room: " + request.getRoomNumber() + " (" + request.getRoomType() + ")\n" +
                "Time: " + request.getTimeRangeString() + " on " + request.getDaysOfWeekDisplay() + "\n" +
                "For: " + request.getDefinitionDateRangeDisplay());
        confirmation.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            RoomSchedule newSchedule = new RoomSchedule( // RoomSchedule is an inner class of DataStore
                    request.getRoomNumber(),
                    request.getRoomType(),
                    request.getStartTime(),
                    request.getEndTime(),
                    request.getDaysOfWeek(),
                    request.getDefinitionStartDate(),
                    request.getDefinitionEndDate()
            );

            if (addRoomScheduleDefinition(newSchedule)) {
                request.setStatus(ScheduleRequestStatus.APPROVED);
                SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Approval Successful",
                        "Schedule request " + request.getRequestId() + " approved. Room schedule definition created with ID: " + newSchedule.getScheduleId());
            } else {
                SceneNavigator.showAlert(Alert.AlertType.ERROR, "Approval Failed - Conflict",
                        "Could not approve schedule request " + request.getRequestId() +
                                ". The proposed schedule conflicts with an existing approved room schedule definition for Room " +
                                request.getRoomNumber() + ".\nPlease review existing schedules in 'View Rooms'. The request remains pending.");
            }
            scheduleRequestsTableView.refresh();
        }
    }

    private void handleRejectRequest(ScheduleRequest request) {
        if (request == null || request.getStatus() != ScheduleRequestStatus.PENDING_APPROVAL) {
            SceneNavigator.showAlert(Alert.AlertType.WARNING, "Action Invalid", "This request is not pending approval.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Rejection");
        confirmation.setHeaderText("Reject Schedule Request?");
        confirmation.setContentText("Are you sure you want to reject schedule request ID: " + request.getRequestId() + "?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            request.setStatus(ScheduleRequestStatus.REJECTED);
            SceneNavigator.showAlert(Alert.AlertType.INFORMATION, "Rejection Successful", "Schedule request " + request.getRequestId() + " has been rejected.");
            scheduleRequestsTableView.refresh();
        }
    }
}