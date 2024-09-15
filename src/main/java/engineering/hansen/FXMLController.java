package engineering.hansen;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;

public class FXMLController {
    @FXML
    private VBox mainWindow;

    @FXML
    private ComboBox<String> settingCombo, shipCombo, aspectCombo;

    @FXML
    private TextField bearingField;

    @FXML
    private Slider rangeSlider, targetSizeSlider;

    @FXML
    private TableView<String> weaponsView;

    @FXML
    private TableColumn<String, String> weaponCol, mountCol, accuracyCol,
        baseDmgCol, maxDmgCol, fireCol;

    @FXML
    private TabPane tabPane;

    @FXML
    private Tab scriptTab, orderTab;

    @FXML
    private javafx.scene.web.WebView scriptWebView;

    protected void crashNicely(String title, String header, String content) {
        content += "\n\nClick 'Close' to exit.";
        Alert alert = new Alert(Alert.AlertType.ERROR, content,
                ButtonType.CLOSE);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.showAndWait();
        Platform.exit();
    }

    public void initialize() {

    }
}