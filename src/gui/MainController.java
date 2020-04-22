package gui;

import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import paths.*;
import structures.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MainController implements Initializable {

    @FXML TableView<ICrossroad> crossroadsList;
    @FXML TableView<IPath> pathsList;
    @FXML AnchorPane areasArea;
    @FXML AnchorPane routesArea;
    @FXML Canvas canvas;
    @FXML Label pathLabel;
    @FXML Label pathCrossroadsLabel;
    @FXML Label pathCrossroadsCountLabel;

    private IForest forest;
    private IRenderer renderer;

    private ICrossroad[] crossroads;
    private IPath[] paths;
    private List<ICrossroad> highlightedCrossroads;
    private IGraphPath<ICrossroad, IPath, Double> highlightedPath;
    private Stage stage;

    private IRange<Point2D, Double> area;
    private boolean withUpdate = true;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        renderer = new Renderer(canvas, area -> {
            if (area != null) {
                List<ICrossroad> crossroadsInArea = Arrays.asList(forest.getCrossroads(area));
                Set<IPath> pathsInArea = new HashSet<>();

                for (ICrossroad c : crossroadsInArea) {
                    pathsInArea.addAll(Arrays.asList(forest.getPaths(c)));
                }

                highlightedCrossroads = crossroadsInArea;
                setHighlightedPath(new GraphPath<>(crossroadsInArea, new ArrayList<>(pathsInArea), pathsInArea.stream().mapToDouble(path -> path.getSize()).sum()));
                render();
            }

            this.area = area;
            render();
        });

        forest = new Forest(this::update);
    }

    private void render() {
        renderer.render(
                crossroads,
                paths,
                highlightedCrossroads,
                highlightedPath == null ? null : highlightedPath.getEdges().toArray(new IPath[0]),
                area
        );
    }

    private void update() {
        if (withUpdate) {
            Comparator<ICrossroad> comparator = Comparator.comparing(crossroad -> crossroad.getId());
            crossroads = forest.getCrossroads();
            Arrays.sort(crossroads, comparator);
            paths = forest.getPaths();
            pathsList.getItems().setAll(paths);
            crossroadsList.getItems().setAll(crossroads);
            renderer.rescale(crossroads);

            render();
        }
    }

    private String formatDistance(double distance) {
        return String.format("%.1f", distance);
    }

    @FXML
    private void handleAddCrossroad(ActionEvent event) {
        FormDialog dialog = new FormDialog("Přidat křižovatku", "Přidat");
        dialog.addTextField("id", "ID");
        dialog.addNumberField("x", "Souřadnice X");
        dialog.addNumberField("y", "Souřadnice Y");
        dialog.addChoiceBox("type", "Typ", CrossroadType.values());

        dialog.run(data -> {
            ICrossroad crossroad = new Crossroad((String) data.get("id"), new Point((int) data.get("x"), (int) data.get("y")), (CrossroadType) data.get("type"));

            try {
                forest.addCrossroad(crossroad);
            } catch (Exception exception) {
                Message.showError("Přidání křižovatky se nezdařilo", exception.getMessage());
            }
        });
    }

    @FXML
    private void handleEditCrossroad(ActionEvent event) {
        ICrossroad selected = crossroadsList.getSelectionModel().getSelectedItem();

        if (selected != null) {
            FormDialog dialog = new FormDialog("Upravit křižovatku", "Upravit");
            dialog.addTextField("id", "ID", selected.getId());
            dialog.addNumberField("x", "Souřadnice X", (int) selected.getCoords().getX());
            dialog.addNumberField("y", "Souřadnice Y", (int) selected.getCoords().getY());
            dialog.addChoiceBox("type", "Typ", CrossroadType.values(), selected.getType());

            dialog.run(data -> {
                ICrossroad crossroad = new Crossroad((String) data.get("id"), new Point((int) data.get("x"), (int) data.get("y")), (CrossroadType) data.get("type"));

                try {
                    forest.updateCrossroad(selected.getId(), crossroad);
                } catch (Exception exception) {
                    Message.showError("Úprava křižovatky se nezdařila", exception.getMessage());
                }
            });
        }
    }

    @FXML
    private void handleDeleteCrossroad(ActionEvent event) {
        ICrossroad selected = crossroadsList.getSelectionModel().getSelectedItem();

        if (selected != null) {
            try {
                highlightedCrossroads = null;
                forest.removeCrossroad(selected.getId());
            } catch (Exception exception) {
                Message.showError("Smazání křižovatky se nezdařilo", exception.getMessage());
            }
        }
    }

    @FXML
    private void handleAddPath(ActionEvent event) {
        FormDialog dialog = new FormDialog("Přidat cestu", "Přidat");
        dialog.addChoiceBox("from", "Odkud", crossroads);
        dialog.addChoiceBox("to", "Kam", crossroads);
        dialog.addCheckBox("isEnabled", "Funkční", true);

        dialog.run(data -> {
            try {
                IPath path = new Path((ICrossroad) data.get("from"), (ICrossroad) data.get("to"), (boolean) data.get("isEnabled"));
                forest.addPath(path);
            } catch (Exception exception) {
                Message.showError("Přidání cesty se nezdařilo", exception.getMessage());
            }
        });
    }

    @FXML
    private void handleFindPath(ActionEvent event) {
        FormDialog dialog = new FormDialog("Hledání cesty", "Najít");
        dialog.addChoiceBox("from", "Odkud", crossroads);
        dialog.addChoiceBox("to", "Kam", crossroads);
        dialog.addCheckBox("onlyDirect", "Pouze přímá cesta");

        dialog.run(data -> {
            ICrossroad from = (ICrossroad) data.get("from");
            ICrossroad to = (ICrossroad) data.get("to");

            if ((boolean) data.get("onlyDirect")) {
                IPath path = forest.getPath(from.getId(), to.getId());

                if (path == null) {
                    Message.showWarning("Cesta neexistuje", "Přímá cesta mezi " + from.getId() + " a " + to.getId() + " nebyla nalezena.");
                    pathsList.getSelectionModel().clearSelection();
                } else {
                    pathsList.getSelectionModel().select(path);
                }
            } else {
                pathsList.getSelectionModel().clearSelection();

                try {
                    IGraphPath<ICrossroad, IPath, Double> path = forest.findShortestPath(from.getId(), to.getId());
                    setHighlightedPath(path);
                } catch (NoSuchElementException exception) {
                    setHighlightedPath(null);
                    Message.showWarning("Cesta neexistuje", "Cesta mezi " + from.getId() + " a " + to.getId() + " nebyla nalezena.");
                }
            }
        });
    }

    @FXML
    private void handleEditPath(ActionEvent event) {
        IPath selected = pathsList.getSelectionModel().getSelectedItem();

        if (selected != null) {
            FormDialog dialog = new FormDialog("Upravit cestu", "Upravit");
            dialog.addChoiceBox("from", "Odkud", crossroads, selected.getFrom());
            dialog.addChoiceBox("to", "Kam", crossroads, selected.getTo());
            dialog.addCheckBox("isEnabled", "Funkční", selected.isEnabled());

            dialog.run(data -> {
                try {
                    IPath path = new Path((ICrossroad) data.get("from"), (ICrossroad) data.get("to"), (boolean) data.get("isEnabled"));
                    forest.updatePath(selected.getFrom().getId(), selected.getTo().getId(), path);
                } catch (Exception exception) {
                    Message.showError("Úprava cesty se nezdařila", exception.getMessage());
                }
            });
        }
    }

    @FXML
    private void handleDeletePath(ActionEvent event) {
        IPath selected = pathsList.getSelectionModel().getSelectedItem();

        if (selected != null) {
            try {
                forest.removePath(selected.getFrom().getId(), selected.getTo().getId());
            } catch (Exception exception) {
                Message.showError("Smazání cesty se nezdařilo", exception.getMessage());
            }
        }
    }

    @FXML
    private void handleShowRoutingMatrix(ActionEvent event) {

        try {
            IRoutingMatrix<ICrossroad> routingMatrix = forest.getRoutingMatrix();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("matrix.fxml"));
            loader.setController(new MatrixController(routingMatrix));
            Parent root = loader.load();
            Stage stage = new Stage();

            stage.setTitle("Směrovací matice");
            stage.setScene(new Scene(root, 450, 450));
            stage.show();
        }
        catch (Exception exception) {
            Message.showError("Výpočet směrovací matice se nezdařil", exception.getMessage());
        }

    }

    @FXML
    private void handleExit(ActionEvent event) {
        System.exit(0);
    }

    @FXML
    private void handleNew(ActionEvent event) {
        highlightedCrossroads = null;
        setHighlightedPath(null);
        forest.clear();
    }

    @FXML
    private void handleGenerate(ActionEvent event) {
        FormDialog dialog = new FormDialog("Generování mapy", "Vygenerovat");
        dialog.addSlider("crossroads", "Křižovatek", 0, 50, 30);
        dialog.addSlider("landings", "Odpočívadel", 0, 50, 20);
        dialog.addSlider("stations", "Zastávek", 0, 50, 10);
        dialog.addSlider("edges", "Četnost cest", 0, 5, 2, 1, 0);
        dialog.addSlider("broken", "Rozbitých cest [%]", 0, 100, 5);

        dialog.run(data -> {
            setHighlightedPath(null);
            highlightedCrossroads = null;
            double mapRatio = canvas.getWidth() / canvas.getHeight();
            double broken = ((int) data.get("broken")) / 100.0;

            forest.generate((int) data.get("crossroads"), (int) data.get("landings"), (int) data.get("stations"), (int) data.get("edges"), broken, mapRatio);
        });
    }

    public void setupStage(Stage stage) {
        this.stage = stage;
        update();

        stage.heightProperty().addListener((observable, oldHeight, height) -> {
            handleResize(stage.getWidth(), height.doubleValue());
        });

        stage.widthProperty().addListener((observable, oldWidth, width) -> {
            handleResize(width.doubleValue(), stage.getHeight());
        });

        crossroadsList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            highlightedCrossroads = new ArrayList<>();

            if (newValue != null) {
                highlightedCrossroads.add(newValue);
            }

            render();
        });

        pathsList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            setHighlightedPath(newValue == null ? null : new ForestPath(newValue));
        });

        setupCrossroadsList();
        setupPathsList();
    }

    private void setHighlightedPath(IGraphPath<ICrossroad, IPath, Double> path) {
        highlightedPath = path;
        pathLabel.setText(path == null ? "Žádná" : "Velikost: " + formatDistance(path.getSize()));
        pathCrossroadsLabel.setText(path == null ? "" : path.getNodes().stream().map(n -> n.getId()).collect(Collectors.joining(", ")));
        pathCrossroadsCountLabel.setText(path == null ? "" : "Křižovatek: " + path.getNodes().size());
        render();
    }

    private void setupCrossroadsList() {
        TableColumn idColumn = new TableColumn("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<ICrossroad, String>("id"));

        TableColumn typeColumn = new TableColumn("Typ");
        typeColumn.setMinWidth(50);
        typeColumn.setCellValueFactory(new PropertyValueFactory<ICrossroad, String>("type"));

        TableColumn xColumn = new TableColumn("x");
        xColumn.setCellValueFactory(data -> new SimpleIntegerProperty((int) ((TableColumn.CellDataFeatures<ICrossroad, Integer>)data).getValue().getCoords().getX()));

        TableColumn yColumn = new TableColumn("y");
        yColumn.setCellValueFactory(data -> new SimpleIntegerProperty((int) ((TableColumn.CellDataFeatures<ICrossroad, Integer>)data).getValue().getCoords().getY()));

        crossroadsList.getColumns().setAll(idColumn, typeColumn, xColumn, yColumn);
    }

    private void setupPathsList() {
        TableColumn fromColumn = new TableColumn("Z");
        fromColumn.setCellValueFactory(data -> new SimpleStringProperty(((TableColumn.CellDataFeatures<IPath, String>)data).getValue().getFrom().getId()));

        TableColumn toColumn = new TableColumn("Do");
        toColumn.setCellValueFactory(data -> new SimpleStringProperty(((TableColumn.CellDataFeatures<IPath, String>)data).getValue().getTo().getId()));

        TableColumn okColumn = new TableColumn("Ok");
        okColumn.setCellValueFactory(data -> new SimpleStringProperty(((TableColumn.CellDataFeatures<IPath, String>)data).getValue().isEnabled() ? "✓" : ""));

        TableColumn sizeColumn = new TableColumn("Délka");
        sizeColumn.setCellValueFactory(data -> new SimpleDoubleProperty(Math.round(((TableColumn.CellDataFeatures<IPath, Double>)data).getValue().getSize() * 10) / 10.0));

        pathsList.getColumns().setAll(fromColumn, toColumn, okColumn, sizeColumn);
    }

    private void handleResize(double width, double height) {
        double areaHeight = (height - 185) / 2;
        areasArea.setPrefHeight(areaHeight);
        routesArea.setPrefHeight(areaHeight);
        canvas.setHeight(height - 70);
        canvas.setWidth(width - 240);
        renderer.resize();
        render();
    }

    @FXML
    private void handleAbout(ActionEvent event) {
        Message.showInfo("O aplikaci", "Aplikace je realizací zadání z předmětu NNDSA (Datové struktury a algoritmy) na téma Efektivní dynamické výpočty optimálních tras přemístění. Autorem je Michal Struna.");
    }

    @FXML
    private void handleShowLabels(ActionEvent event) {
        renderer.setWithLabels(((CheckMenuItem) event.getTarget()).isSelected());
        render();
    }

    @FXML
    private void handleShowGrid(ActionEvent event) {
        renderer.setWithGrid(((CheckMenuItem) event.getTarget()).isSelected());
        render();
    }

    @FXML
    private void handleShowLegend(ActionEvent event) {
        renderer.setWithLegend(((CheckMenuItem) event.getTarget()).isSelected());
        render();
    }

    @FXML
    private void handleSave(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Uložit mapu");
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                forest.save(file.getAbsolutePath());
            } catch (Exception exception) {
                Message.showError("Uložení se nezdařilo", exception.getMessage());
            }
        }
    }

    @FXML
    private void handleLoad(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Načíst mapu");
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                setHighlightedPath(null);
                highlightedCrossroads = null;
                forest.load(file.getAbsolutePath());
            } catch (Exception exception) {
                Message.showError("Načtení se nezdařilo", exception.getMessage());
            }
        }
    }

    @FXML
    private void handleNewBlockFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Vytvořit blokový soubor");
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("block_file.fxml"));
                loader.setController(new BlockFileController(file.getAbsolutePath()));
                Parent root = loader.load();
                Stage stage = new Stage();

                stage.setTitle("Blokový soubor " + file.getName());
                stage.setScene(new Scene(root, 530, 720));
                stage.show();
            } catch (Exception exception) {
                Message.showError("Vytvoření se nezdařilo", exception.getMessage());
            }
        }
    }

    @FXML
    private void handleExportBlockFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Vyexportovat křižovatky do blokového souboru");
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("block_file.fxml"));
                loader.setController(new BlockFileController(file.getAbsolutePath(), crossroads));
                Parent root = loader.load();
                Stage stage = new Stage();

                stage.setTitle("Blokový soubor " + file.getName());
                stage.setScene(new Scene(root, 530, 720));
                stage.show();
            } catch (Exception exception) {
                Message.showError("Export se nezdařil", exception.getMessage());
            }
        }
    }

    @FXML
    private void handleOpenBlockFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Otevřít blokový soubor");
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("block_file.fxml"));
                loader.setController(new BlockFileController(file.getAbsolutePath()));
                Parent root = loader.load();
                Stage stage = new Stage();

                stage.setTitle("Blokový soubor " + file.getName());
                stage.setScene(new Scene(root, 530, 720));
                stage.show();
            } catch (Exception exception) {
                Message.showError("Načtení se nezdařilo", exception.getMessage());
            }
        }
    }

    @FXML
    private void handleFindCrossroadCoords(ActionEvent event) {
        FormDialog dialog = new FormDialog("Hledání křižovatky podle souřadnic", "Najít");
        dialog.addNumberField("x", "Souřadnice X");
        dialog.addNumberField("y", "Souřadnice Y");

        dialog.run(data -> {
            int x = (int) data.get("x");
            int y = (int) data.get("y");
            ICrossroad crossroad = forest.getCrossroad(new Point(x, y));

            if (crossroad == null) {
                Message.showWarning("Křižovatka nenalezena", "Křižovatka na pozici [" + x + ", " + y + "] nebyla nalezena.");
            } else {
                crossroadsList.getSelectionModel().select(crossroad);
                crossroadsList.scrollTo(crossroad);
            }
        });
    }

    @FXML
    private void handleFindCrossroadId(ActionEvent event) {
        FormDialog dialog = new FormDialog("Hledání křižovatky podle ID", "Najít");
        dialog.addTextField("id", "ID");

        dialog.run(data -> {
            String crossroadId = (String) data.get("id");
            ICrossroad crossroad = forest.getCrossroad((String) data.get("id"));

            if (crossroad == null) {
                Message.showWarning("Křižovatka nenalezena", "Křižovatka " + crossroadId + " nebyla nalezena.");
            } else {
                crossroadsList.getSelectionModel().select(crossroad);
                crossroadsList.scrollTo(crossroad);
            }
        });
    }

    @FXML
    private void handleDeletePaths(ActionEvent event) {
        if (highlightedPath == null) {
            return;
        }

        for (IPath path : highlightedPath.getEdges()) {
            forest.removePath(path.getFrom().getId(), path.getTo().getId());
        }

        setHighlightedPath(null);
        highlightedCrossroads = null;
        render();
    }

    @FXML
    private void handleDeleteCrossroads(ActionEvent event) {
        if (highlightedCrossroads == null) {
            return;
        }

        for (ICrossroad crossroad : highlightedCrossroads) {
            forest.removeCrossroad(crossroad.getId());
        }

        setHighlightedPath(null);
        highlightedCrossroads = null;
        update();
    }

    @FXML
    private void handleSetPathsEnabled(ActionEvent event) {
        setPathsEnabled(true);
    }

    @FXML
    private void handleSetPathsDisabled(ActionEvent event) {
        setPathsEnabled(false);
    }

    private void setPathsEnabled(boolean isEnabled) {
        if (highlightedPath == null) {
            return;
        }

        withUpdate = false;

        for (IPath path : highlightedPath.getEdges()) {
            path.setEnabled(isEnabled);
            forest.updatePath(path.getFrom().getId(), path.getTo().getId(), path);
        }

        setHighlightedPath(null);
        highlightedCrossroads = null;
        withUpdate = true;
        render();
    }

}