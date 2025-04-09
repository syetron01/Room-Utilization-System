module com.example.roomutilizationsystem {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.roomutilizationsystem to javafx.fxml;
    exports com.example.roomutilizationsystem;
}