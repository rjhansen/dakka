package engineering.hansen;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class FXMLController {
    static class DakkaException extends RuntimeException {}
    static class NoHomeDir extends DakkaException {}
    static class NoConfigDir extends DakkaException {}
    static class NoShipsFound extends DakkaException {}

    private Document shipdoc = null;
    public Document getShipdoc() { return shipdoc; }

    @FXML
    private TreeView<String> fleetView, destroyedView, shipyardView;

    @FXML
    private VBox mainWindow;

    @FXML
    private Label statusLabel;

    protected void crashNicely(String title, String header, String content) {
        content += "\n\nClick 'Close' to exit.";
        Alert alert = new Alert(Alert.AlertType.ERROR, content,
                ButtonType.CLOSE);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.showAndWait();
        Platform.exit();
    }

    protected void parseXML() {
        try {
            if (System.getenv("HOME") == null)
                throw new NoHomeDir();
            if (!Paths.get(System.getenv("HOME"), ".config").toFile().exists())
                throw new NoConfigDir();
            var _ = new java.io.File(Paths.get(System.getenv("HOME"),
                    ".config", "dakka").toString()).mkdir();
            var shipfile = new File(Paths.get(System.getenv("HOME"),
                            ".config",
                            "dakka",
                            "ships.xml")
                    .toString());
            if (!shipfile.exists())
                throw new NoShipsFound();

            shipdoc = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(shipfile);
            shipdoc.getDocumentElement().normalize();
        } catch (Exception e) {
            switch (e) {
                case NoShipsFound _:
                    crashNicely("Error",
                            "No ships found",
                            "Your user installation is missing a " +
                                    "ships.xml file.  We can't proceed without it.");
                    break;
                case NoHomeDir _:
                    crashNicely("Operating system error",
                            "No user directory found",
                            "Your user directory seems to be missing.  " +
                                    "This is a serious and unrecoverable problem.");
                    break;
                case NoConfigDir _:
                    crashNicely("Operating system error",
                            "No user configuration directory found",
                            "Your user configuration directory seems to be " +
                                    "missing.  This is a serious and unrecoverable " +
                                    "problem.");
                    break;
                case ParserConfigurationException _, SAXException _:
                    crashNicely("XML parsing error",
                            "Can't parse ship registries",
                            "Your ship registries are corrupted.  This is " +
                                    "a serious and unrecoverable problem.");
                    break;
                case IOException _:
                    crashNicely("File read error",
                            "Can't read ship registries",
                            "Your ship registries can't be read.  This is " +
                                    "a serious and unrecoverable problem.");
                    break;
                default:
                    crashNicely("Unknown", e.toString(), "");
                    break;
            }
        }
    }

    private void addDeleteToShipyardMenu(final ContextMenu cm) {
        final var di = new MenuItem("Delete");
        final var setting = shipyardView.getSelectionModel().getSelectedItem().getValue();
        cm.getItems().add(di);
        di.setOnAction(_ -> {
            var alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Are you sure?");
            alert.setHeaderText("Danger, Will Robinson!");
            alert.setContentText("This action can’t be undone. The “" +
                    setting + "” shipyards will be unavailable " +
                    "for the rest of your session.\n\nDo you want to continue?");
            alert.showAndWait().filter(resp -> resp == ButtonType.OK).ifPresent(_ ->
                    shipyardView.getSelectionModel().getSelectedItems().forEach(
                            shipyard -> shipyardView.getRoot().getChildren().remove(shipyard)));
        });
    }

    private void addSendToFleetToShipyardMenu(final ContextMenu cm) {
        final var ci = new MenuItem("Send to the fleet…");
        cm.getItems().add(ci);
        ci.setOnAction(_ -> {
            final var tid = new TextInputDialog();
            tid.setHeaderText("We welcome a new ship to the fleet!");
            tid.setContentText("Name: ");
            tid.showAndWait().ifPresent(name -> {
                final var shipClass = shipyardView.getSelectionModel().getSelectedItem().getValue();
                final var fleetRoot = fleetView.getRoot();
                final var fleetItems = fleetRoot.getChildren().stream().filter(
                        thing -> Objects.equals(thing.getValue(), shipClass)
                ).toList();
                if (fleetItems.isEmpty()) {
                    final var fleetClass = new TreeItem<>(shipClass);
                    final var fleetShip = new TreeItem<>(name);
                    fleetView.getRoot().getChildren().add(fleetClass);
                    fleetClass.getChildren().add(fleetShip);
                } else {
                    final var fleetShip = new TreeItem<>(name);
                    fleetItems.getFirst().getChildren().add(fleetShip);
                }
            });
        });
    }

    private void createShipyardMenus() {
        shipyardView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                var cm = new ContextMenu();
                var item = shipyardView.getSelectionModel().getSelectedItem();
                if (item == null) return;
                if (Objects.equals(item.getParent().getValue(), "Settings")) {
                    addDeleteToShipyardMenu(cm);
                } else {
                    addSendToFleetToShipyardMenu(cm);
                }
                cm.show(shipyardView, event.getScreenX(), event.getScreenY());
            }
        });
    }

    public void populateShipyardView() {
        TreeItem<String> root = new TreeItem<>("Settings");
        Map<String, HashSet<String>> ships = new HashMap<>();
        var domlist = shipdoc.getElementsByTagName("data");

        for (int i = 0 ; i < domlist.getLength(); i++) {
            var setting = "Unknown setting";
            var shipclass = "Unknown class";
            var datanode = domlist.item(i);
            for (int j = 0; j < datanode.getChildNodes().getLength(); j++) {
                var node = datanode.getChildNodes().item(j);
                switch (node.getNodeName()) {
                    case "setting":
                        setting = node.getTextContent();
                        break;
                    case "class":
                        shipclass = node.getTextContent();
                        break;
                    default:
                        break;
                }
            }
            if (!ships.containsKey(setting))
                ships.put(setting, new HashSet<>());
            ships.get(setting).add(shipclass);
        }
        ships.keySet().stream().sorted().forEach(key -> root.getChildren().add(new TreeItem<>(key)));
        for (var node: root.getChildren()) {
            ships.get(node.getValue()).stream().sorted().forEach(
                    value -> node.getChildren().add(new TreeItem<>(value)));
        }
        shipyardView.setRoot(root);
        shipyardView.setShowRoot(false);
        shipyardView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        createShipyardMenus();
    }

    private void addDeleteToFleetMenu(final ContextMenu cm) {
        final var di = new MenuItem("Remove all ships of this class");
        cm.getItems().add(di);
        di.setOnAction(_ -> {
            final var alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Are you sure?");
            alert.setHeaderText("Danger, Will Robinson!");
            alert.setContentText("This action can’t be undone. All ships of class “" +
                    fleetView.getSelectionModel().getSelectedItem().getValue() +
                    "” will be removed from your fleet.\n\n " +
                    "Do you want to continue?");
            alert.showAndWait().filter(resp -> resp == ButtonType.OK).ifPresent(
                    _ -> fleetView.getSelectionModel().getSelectedItems().forEach(
                            shipClass -> fleetView.getRoot().getChildren().remove(shipClass)));
        });
    }

    private void addDismissToFleetMenu(final ContextMenu cm) {
        final var ri = new MenuItem("Dismiss from fleet");
        cm.getItems().add(ri);
        ri.setOnAction(_ -> {
            final var alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Are you sure?");
            alert.setHeaderText("Danger, Will Robinson!");
            alert.setContentText("This action can’t be undone. Are you sure you want " +
                    "to dismiss the " +
                    fleetView.getSelectionModel().getSelectedItem().getValue() +
                    "?");
            alert.
                    showAndWait()
                    .filter(resp -> resp == ButtonType.OK)
                    .ifPresent(_ -> {
                        fleetView
                                .getSelectionModel()
                                .getSelectedItems()
                                .forEach(
                                        shipItem -> shipItem
                                                .getParent()
                                                .getChildren()
                                                .remove(shipItem));
                        var removeThese = fleetView
                                .getRoot()
                                .getChildren()
                                .stream()
                                .filter(TreeItem::isLeaf)
                                .toList();

                        for (var v: removeThese)
                           fleetView.getRoot().getChildren().remove(v);
                    });
        });
    }

    private void addDestroyToFleetMenu(final ContextMenu cm) {
        final var di = new MenuItem("Destroy");
        cm.getItems().add(di);
        di.setOnAction(_ -> {
            final var alert = new Alert(Alert.AlertType.CONFIRMATION);
            final var shipClass = fleetView.getSelectionModel().getSelectedItem().getParent().getValue();
            final var shipName = fleetView.getSelectionModel().getSelectedItem().getValue();
            alert.setTitle("Are you sure?");
            alert.setHeaderText("Danger, Will Robinson!");
            alert.setContentText("This action can’t be undone. Are you sure you want " +
                    "to send the " + shipName + " to the Eternal Patrol?");
            alert.showAndWait().filter(resp -> resp == ButtonType.OK).ifPresent(
                    _ -> {
                        fleetView
                                .getSelectionModel()
                                .getSelectedItems()
                                .forEach(shipItem -> shipItem.getParent().getChildren().remove(shipItem));
                        var removeThese = fleetView
                                .getRoot()
                                .getChildren()
                                .stream()
                                .filter(TreeItem::isLeaf)
                                .toList();
                        for (var v: removeThese)
                            fleetView.getRoot().getChildren().remove(v);
                        var destroyedKeys = new HashSet<String>();
                        destroyedView
                                .getRoot()
                                .getChildren()
                                .stream()
                                .map(TreeItem::getValue)
                                .forEach(destroyedKeys::add);
                        if (! destroyedKeys.contains(shipClass)) {
                            final var classItem = new TreeItem<>(shipClass);
                            final var shipItem = new TreeItem<>(shipName);
                            classItem.getChildren().add(shipItem);
                            destroyedView.getRoot().getChildren().add(classItem);
                        } else {
                            destroyedView
                                    .getRoot()
                                    .getChildren()
                                    .stream()
                                    .filter(x -> Objects.equals(x.getValue(), shipClass))
                                    .forEach(x -> x.getChildren().add(new TreeItem<>(shipName)));
                        }
                    });

            fleetView.getRoot().getChildren().stream().filter(TreeItem::isLeaf)
                    .forEach(bar -> bar.getChildren().remove(bar));
        });
    }

    private void addFireOrderToFleetMenu(final ContextMenu cm) {
        final var di = new MenuItem("Enter fire order");
        cm.getItems().add(di);
    }

    private void createFleetMenus() {
        fleetView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                final var cm = new ContextMenu();
                final var item = fleetView.getSelectionModel().getSelectedItem();
                if (item == null) return;
                if (Objects.equals(item.getParent().getValue(), "Ready for Duty")) {
                    addDeleteToFleetMenu(cm);
                } else {
                    addFireOrderToFleetMenu(cm);
                    addDismissToFleetMenu(cm);
                    addDestroyToFleetMenu(cm);
                }
                cm.show(fleetView, event.getScreenX(), event.getScreenY());
            }
        });
    }

    private void createDestroyedMenus() {
    }

    private void populateFleetView() {
        fleetView.setRoot(new TreeItem<>("Ready for Duty"));
        fleetView.setShowRoot(false);
        fleetView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        createFleetMenus();
    }

    private void populateDestroyedView() {
        destroyedView.setRoot(new TreeItem<>("Destroyed"));
        destroyedView.setShowRoot(false);
        destroyedView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        createDestroyedMenus();
    }

    public void initialize() {
        parseXML();
        populateShipyardView();
        populateFleetView();
        populateDestroyedView();
    }
}