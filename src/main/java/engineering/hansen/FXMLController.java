package engineering.hansen;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;

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
    private TableView<DataRow> weaponsView;

    @FXML
    private TableColumn<DataRow, String> weaponCol, mountCol;

    @FXML
    private TableColumn<DataRow, Integer> accuracyCol, baseDmgCol, maxDmgCol;

    @FXML
    private TableColumn<DataRow, Boolean> fireCol;

    @FXML
    private TabPane tabPane;

    @FXML
    private Tab scriptTab, orderTab;

    @FXML
    private javafx.scene.web.WebView scriptWebView;

    private int lastSettingIndex = 0;
    private int lastShipIndex = 0;
    private int lastRange = 0;
    private int lastTargetSize = 0;
    private final HashMap<String, HashMap<String, Ship>> shipsBySetting = new HashMap<>();
    private final Pattern bearingPattern = Pattern.compile("^\\s*([0-6]),\\s*(\\d+)\\s*$");
    private final ObservableList<DataRow> ol = FXCollections.observableArrayList();
    private final ChangeListener<Boolean> cl = new ChangeListener<>() {

        @Override
        public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
            updateScript();
        }
    };

    public static void crashNicely(String title, String header, String content) {
        content += "\n\nClick 'Close' to exit.";
        Alert alert = new Alert(Alert.AlertType.ERROR, content,
                ButtonType.CLOSE);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.showAndWait();
        Platform.exit();
    }

    private void populateShipsForSetting(Connection con, String setting) {
        try (var stmt = con.prepareStatement("select rowid from settings where name = ?")) {
            stmt.setString(1, setting);
            ResultSet rs = stmt.executeQuery();
            int setting_id = 0;
            while (rs.next()) setting_id = rs.getInt("rowid");
            var nextStmt = con.prepareStatement("select rowid, name from ship_classes where setting = ?");
            nextStmt.setInt(1, setting_id);
            ResultSet rs2 = nextStmt.executeQuery();
            while (rs2.next())
                shipsBySetting
                        .get(setting)
                        .put(rs2.getString("name"),
                                new Ship(con, rs2.getInt("rowid")));
        } catch (SQLException e) {
            crashNicely("SQL error", "SQL error", e.toString());
        }

    }

    private void populateSettingsAndShips() {
        var db_url = "jdbc:sqlite:/home/rjh/.config/dakka/ships.sq3";
        try (Connection con = DriverManager.getConnection(db_url);
             Statement stmt = con.createStatement()) {
            stmt.execute("select name from settings");
            var rs = stmt.getResultSet();
            while (rs.next()) {
                var setting = rs.getString("name");
                shipsBySetting.put(setting, new HashMap<>());
                settingCombo.getItems().add(setting);
            }
            for (int i = 0 ; i < settingCombo.getItems().size() ; i++) {
                populateShipsForSetting(con, settingCombo.getItems().get(i));
            }
            settingCombo.getSelectionModel().select(0);
            String setting = settingCombo.getSelectionModel().getSelectedItem();
            shipsBySetting.get(setting).keySet().stream().sorted().forEach(v -> shipCombo.getItems().add(v));
            shipCombo.getSelectionModel().select(0);
        } catch (SQLException e) {
            crashNicely("SQL error", "SQL error", e.toString());
        }
    }

    private void updateScript() {
        var setting = settingCombo.getSelectionModel().getSelectedItem();
        var shipclass = shipCombo.getSelectionModel().getSelectedItem();
        var ship = shipsBySetting.get(setting).get(shipclass);
        var attrs = new HashMap<String, HashSet<String>>();

        for (var m: ship.getMounts())
            for (var w: m.getWeapons()) {
                if (!attrs.containsKey(w.getName())) attrs.put(w.getName(), new HashSet<>());
                Arrays.stream(w.getAttributes()).forEach(x -> attrs.get(w.getName()).add(x));
            }

        if (weaponsView.getItems().filtered(DataRow::getFire).size() == 0) {
            scriptWebView.getEngine().loadContent("<html><head><title>Script</title></head><body></body></html>");
            return;
        }

        var tmpl = """
  <html>
  <head>
    <title>Spoken script</title>
  </head>
  <body>
    <h2 style='text-align: center;'>Are you ready to receive?</h2>
    <p>
      My <i>""" + shipCombo.getSelectionModel().getSelectedItem() +
                "</i>-class vessel is practicing pro-active retribution on your " +
                aspectCombo.getSelectionModel().getSelectedItem().toLowerCase() +
                " with the following weapons:" + """
    </p>
    <ol>""";
        for (var row: weaponsView.getItems().filtered(DataRow::getFire)) {
            tmpl += "      <li>" + row.getWeapon() + ", hits on a " +
                    (row.getAccuracy() + (int) targetSizeSlider.getValue()) +
                    "-plus, damage " + row.getDamage() + ", maximum penetration " +
                    row.getPenetration() + ".";
            var attrSet = attrs.get(row.getWeapon());
            if (!attrSet.isEmpty())
                tmpl += "  It has the attributes ‘" + String.join(", ", attrSet) + "’.";
            tmpl += "</li>\n";
        }
        tmpl += "    </ol>\n  </body>\n</html>";
        scriptWebView.getEngine().loadContent(tmpl);
    }

    private void updateUI() {
        var matcher = bearingPattern.matcher(bearingField.getText());
        if (! matcher.matches()) {
            weaponsView.getItems().clear();
            return;
        }
        var row = Integer.parseInt(matcher.group(1));
        var col = Integer.parseInt(matcher.group(2));
        if (row < 0 || row > 6) return;
        switch (row) {
            case 0: if (col != 0) return; break;
            case 1: if (! (col >= 0 && col < 6)) return; break;
            case 2: if (! (col >= 0 && col < 12)) return; break;
            case 3: if (! (col >= 0 && col < 12)) return; break;
            case 4: if (! (col >= 0 && col < 12)) return; break;
            case 5: if (! (col >= 0 && col < 6)) return; break;
            case 6: if (col != 0) return; break;
            default: return;
        }

        var setting = settingCombo.getSelectionModel().getSelectedItem();
        var shipclassname = shipCombo.getSelectionModel().getSelectedItem();
        var ship = shipsBySetting.get(setting).get(shipclassname);
        var range = (int) rangeSlider.getValue();
        weaponsView.getItems().clear();

        ol.clear();
        for (var m: ship.getMounts()) {
            if (!m.getArcs()[row][col]) continue;
            for (var w: m.getWeapons()) {
                var rb = w.getRangeBandFor(range);
                if (rb == null) continue;

                String mountName = m.getName();
                String weaponName = w.getName();
                int acc = rb.getAccuracy();
                int dmg = rb.getDamage();
                int pen = rb.getPenetration();
                boolean fire = false;
                var dr = new DataRow(mountName, weaponName, acc, dmg, pen, fire);
                dr.fireProperty().addListener(cl);
                ol.add(dr);
            }
        }
        weaponsView.setItems(ol);
        updateScript();
    }

    public void initialize() {
        populateSettingsAndShips();
        var aspects = new String[] { "Bow", "Stern", "Port", "Starboard", "Sail", "Keel" };
        for (var aspect: aspects) aspectCombo.getItems().add(aspect);
        aspectCombo.getSelectionModel().select(0);

        mountCol.setStyle( "-fx-alignment: CENTER;");
        accuracyCol.setStyle( "-fx-alignment: CENTER;");
        baseDmgCol.setStyle( "-fx-alignment: CENTER;");
        maxDmgCol.setStyle( "-fx-alignment: CENTER;");
        fireCol.setStyle( "-fx-alignment: CENTER;");

        mountCol.setCellValueFactory(new PropertyValueFactory<>("mount"));
        weaponCol.setCellValueFactory(new PropertyValueFactory<>("weapon"));
        accuracyCol.setCellValueFactory(new PropertyValueFactory<>("accuracy"));
        baseDmgCol.setCellValueFactory(new PropertyValueFactory<>("damage"));
        maxDmgCol.setCellValueFactory(new PropertyValueFactory<>("penetration"));
        fireCol.setCellValueFactory(new PropertyValueFactory<>("fire"));
        fireCol.setCellFactory( tc -> new CheckBoxTableCell<>());

        settingCombo.setOnAction(event -> {
            var index = settingCombo.getSelectionModel().getSelectedIndex();
            if (index == lastSettingIndex) return;
            lastSettingIndex = index;
            lastShipIndex = 0;
            String setting = settingCombo.getSelectionModel().getSelectedItem();
            shipsBySetting.get(setting).keySet().stream().sorted().forEach(v -> shipCombo.getItems().add(v));
            shipCombo.getSelectionModel().select(0);
            updateUI();
        });
        shipCombo.setOnAction(event -> {
            var index = shipCombo.getSelectionModel().getSelectedIndex();
            if (index == lastShipIndex) return;
            lastShipIndex = index;
            updateUI();
        });
        bearingField.setOnAction(event -> {
            var matcher = bearingPattern.matcher(bearingField.getText());
            if (! matcher.matches()) return;
            updateUI();
        });
        rangeSlider.valueProperty().addListener(((observable, oldNum, newNum) -> {
            if (newNum.intValue() == lastRange) return;
            lastRange = newNum.intValue();
            updateUI();
        }));
        targetSizeSlider.valueProperty().addListener(((observable, oldNum, newNum) -> {
            if (newNum.intValue() == lastTargetSize) return;
            lastTargetSize = newNum.intValue();
            updateUI();
        }));
        aspectCombo.setOnAction(event -> {
            updateUI();
        });
    }

    public void exit() {
        Platform.exit();
    }
}