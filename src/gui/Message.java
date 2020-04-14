package gui;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

public class Message {

    private Message() {

    }

    /**
     * Show error dialog.
     */
    public static void showError(String header, String text) {
        show(Alert.AlertType.ERROR, "Nastala chyba", header, text);
    }

    /**
     * Show warning dialog.
     */
    public static void showWarning(String header, String text) {
        show(Alert.AlertType.WARNING, "Upozornění", header, text);
    }

    /**
     * Show info dialog.
     */
    public static void showInfo(String header, String text) {
        show(Alert.AlertType.INFORMATION, "Informace", header, text);
    }

    /**
     * Show dialog.
     * @param type Type of dialog.
     * @param title Title of dialog.
     * @param header Text of dialog header.
     * @param text Content of dialog.
     */
    private static void show(Alert.AlertType type, String title, String header, String text) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);

        HBox content = new HBox();
        content.setPadding(new Insets(10));
        Text contentText = new Text(text);
        contentText.setWrappingWidth(500);
        contentText.setLineSpacing(2);
        content.getChildren().add(contentText);
        alert.getDialogPane().setContent(content);

        alert.showAndWait();
    }

}
