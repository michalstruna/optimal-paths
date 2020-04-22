package gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import paths.Crossroad;
import paths.CrossroadType;
import paths.ICrossroad;
import structures.BlockFileAction;
import structures.BlockSortedFile;

import java.awt.*;
import java.awt.geom.Point2D;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class BlockFileController implements Initializable {

    private static final String SEPARATOR = "=========="; // Separator of actions in logger console.
    private static Map<BlockFileAction, Integer> logMap;

    @FXML TextArea console;

    private String fileName;
    private BlockSortedFile<String, ICrossroad> file;
    private ICrossroad[] crossroads;

    public BlockFileController(String fileName) {
        this.fileName = fileName;
        logMap = new HashMap<>();
    }

    public BlockFileController(String fileName, ICrossroad[] crossroads) {
        this(fileName);
        this.crossroads = crossroads;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentConsole = console;

        file = new BlockSortedFile<>(
                fileName,
                ICrossroad::getId,
                String::hashCode,
                (action, value) -> {
                    log(action.toString() + ": " + (value == null ? "" : value.toString()));
                    logMap.put(action, logMap.containsKey(action) ? logMap.get(action) + 1 : 1);
                }
        );

        if (crossroads != null) {
            file.build(crossroads);
        }
    }

    @FXML
    private void handleFindInterpolating(ActionEvent event) {
        logMap.clear();
        FormDialog dialog = new FormDialog("Interpolační hledání", "Najít");
        dialog.addTextField("id", "ID");

        dialog.run(data -> {
            String id = (String) data.get("id");
            log(SEPARATOR);
            long start = System.currentTimeMillis();
            file.findInterpolating(id);
            logSearch(start);
        });
    }

    private void logSearch(long start) {
        Integer blockRead = logMap.get(BlockFileAction.BLOCK_READ);
        int totalBlockRead = (blockRead == null ? 0 : blockRead) + logMap.get(BlockFileAction.CONTROL_BLOCK_READ);
        log("Přečteno bloků celkem (včetně řídícího): " + totalBlockRead + ", hledání trvalo [ms]: " + (System.currentTimeMillis() - start));
    }

    @FXML
    private void handleNew(ActionEvent event) {
        log(SEPARATOR);
        file.build(new ICrossroad[0]);
    }

    @FXML
    private void handleGenerate(ActionEvent event) {
        int maxCount = 26 * 26 * 26; // 17 576 because of english alphabet has 26 characters, so there are 26 * 26 * 26 options for unique ID of length 3.
        FormDialog dialog = new FormDialog("Generování", "Vygenerovat");
        dialog.addNumberField("crossroads", "Křižovatek (max 17 576)", 5000, 0, maxCount);

        dialog.run(data -> {
            log(SEPARATOR);
            int crossroads = (int) data.get("crossroads");
            ICrossroad[] result = new ICrossroad[crossroads];

            for (int i = 0; i < crossroads; i++) {
                String id = getStringByIndex(i);
                Point2D position = new Point((int) Math.floor(Math.random() * 1000), (int) Math.floor(Math.random() * 1000));
                CrossroadType type = i % 3 == 0 ? CrossroadType.BASIC : (i % 3 == 1 ? CrossroadType.STATION : CrossroadType.BASIC);
                result[i] = new Crossroad(id, position, type);
            }

            file.build(result);

            log("Vygenerováno " + crossroads + " křižovatek" + (crossroads == 0 ? "." : " s ID od " + result[0].getId() + " do " + result[result.length - 1].getId()));
        });
    }

    /**
     * Returns string of length 3 by index.
     * Examples: 0 => "aaa", 1 => "aab", 25 => "aaz", 26 => "aba", 17575 => "zzz"
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
        logMap.clear();
        FormDialog dialog = new FormDialog("Binární hledání", "Najít");
        dialog.addTextField("id", "ID");

        dialog.run(data -> {
            String id = (String) data.get("id");
            log(SEPARATOR);
            long start = System.currentTimeMillis();
            file.findBinary(id);
            logSearch(start);
        });
    }

    @FXML
    private void handleRemove(ActionEvent event) {
        logMap.clear();
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