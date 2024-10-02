module Dakka {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;

    opens engineering.hansen to javafx.fxml;
    exports engineering.hansen;
}