module Dakka {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml;

    opens engineering.hansen to javafx.fxml;
    exports engineering.hansen;
}