module Dakka {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml;
    requires javafx.web;

    opens engineering.hansen to javafx.fxml;
    exports engineering.hansen;
}