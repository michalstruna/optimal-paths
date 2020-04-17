package gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import paths.Crossroad;
import paths.CrossroadType;
import paths.ICrossroad;
import structures.BlockSortedFile;
import structures.IObjectFile;

import java.awt.*;
import java.awt.geom.Point2D;
import java.net.URL;
import java.util.ResourceBundle;

public class BlockFileController implements Initializable {

    private static final String SEPARATOR = "=========="; // Separator of actions in logger console.

    @FXML TextArea console;

    private String fileName;
    private BlockSortedFile<String, ICrossroad> file;

    public BlockFileController(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentConsole = console;

        file = new BlockSortedFile<>(
                fileName,
                ICrossroad::getId,
                String::hashCode,
                (action, value) -> log(action.toString() + ": " + (value == null ? "" : value.toString()))
        );
    }

    @FXML
    private void handleFindInterpolating(ActionEvent event) {
        FormDialog dialog = new FormDialog("Interpolační hledání", "Najít");
        dialog.addTextField("id", "ID");

        dialog.run(data -> {
            String id = (String) data.get("id");
            log(SEPARATOR);
            long start = System.currentTimeMillis();
            file.findInterpolating(id);
            log("Hledání trvalo [ms]: " + (System.currentTimeMillis() - start));
        });
    }

    @FXML
    private void handleNew(ActionEvent event) {
        log(SEPARATOR);
        file.build(new ICrossroad[0]);
    }

    @FXML
    private void handleGenerate(ActionEvent event) {
        FormDialog dialog = new FormDialog("Generování", "Vygenerovat");
        dialog.addSlider("crossroads", "Křižovatek", 0, 10000, 5000, 1000, 1);

        dialog.run(data -> {
            int crossroads = (int) data.get("crossroads");
            ICrossroad[] result = new ICrossroad[crossroads];

            for (int i = 0; i < crossroads; i++) {
                String id = getStringByIndex(i);
                Point2D position = new Point((int) Math.floor(Math.random() * 1000), (int) Math.floor(Math.random() * 1000));
                CrossroadType type = i % 3 == 0 ? CrossroadType.BASIC : (i % 3 == 1 ? CrossroadType.STATION : CrossroadType.BASIC);
                result[i] = new Crossroad(id, position, type);
            }

            IObjectFile<String, ICrossroad> file = new BlockSortedFile<>(fileName, c -> c.getId(), id -> id.hashCode());

            file.build(result);

            log("Vygenerováno " + crossroads + " křižovatek" + (crossroads == 0 ? "." : " s ID od " + result[0].getId() + " do " + result[result.length - 1].getId()));
        });
    }

    /**
     * Returns string of length 3 by index.
     * Examples: 0 => "aaa", 1 => "aab", 25 => "aaz", 26 => "aba", ...
     */
    private String getStringByIndex(int index) {
        StringBuilder result = new StringBuilder();
        int alphabetSize = 26;

        result.insert(0, (char)('a' + (index % alphabetSize)));
        index /= alphabetSize;
        result.insert(0, (char)('a' + (index % alphabetSize)));
        index /= alphabetSize;
        result.insert(0, (char)('a' + (index % alphabetSize)));

        return result.toString();
    }

    @FXML
    private void handleFindBinary(ActionEvent event) {
        FormDialog dialog = new FormDialog("Binární hledání", "Najít");
        dialog.addTextField("id", "ID");

        dialog.run(data -> {
            String id = (String) data.get("id");
            log(SEPARATOR);
            long start = System.currentTimeMillis();
            file.findBinary(id);
            log("Hledání trvalo [ms]: " + (System.currentTimeMillis() - start));
        });
    }

    @FXML
    private void handleRemove(ActionEvent event) {
        FormDialog dialog = new FormDialog("Odebrání křižovatky", "Odebrat");
        dialog.addTextField("id", "ID");

        dialog.run(data -> {
            String id = (String) data.get("id");
            log(SEPARATOR);
            file.remove(id);
        });
    }

    private static TextArea currentConsole; // Must be static because BlockFileController should not be serializable.

    private static void log(String text) {
        currentConsole.appendText(text + "\n");
    }

}