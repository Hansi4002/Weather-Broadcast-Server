module lk.ijse.weatherbroadcastsystem {
    requires javafx.controls;
    requires javafx.fxml;


    opens lk.ijse.weatherbroadcastsystem to javafx.fxml;
    exports lk.ijse.weatherbroadcastsystem;
    exports lk.ijse.weatherbroadcastsystem.controller;
    opens lk.ijse.weatherbroadcastsystem.controller to javafx.fxml;
}