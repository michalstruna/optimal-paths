package gui;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.converter.IntegerStringConverter;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
public class FormDialog {

    private ButtonType okButtonType;

    private Dialog<Map<String, Object>> dialog;
    private GridPane root;

    private Map<String, Slider> sliders;
    private Map<String, TextField> textFields;
    private Map<String, Spinner> spinners;
    private Map<String, ChoiceBox> choiceBoxes;
    private Map<String, CheckBox> checkBoxes;

    private int rowsCount;

    public FormDialog(String title, String okButtonLabel) {
        sliders = new HashMap<>();
        textFields = new HashMap<>();
        spinners = new HashMap<>();
        choiceBoxes = new HashMap<>();
        checkBoxes = new HashMap<>();

        dialog = new Dialog<>();
        dialog.setTitle(title);
        okButtonType = new ButtonType(okButtonLabel, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        root = new GridPane();
        root.setHgap(10);
        root.setVgap(10);

        rowsCount = 0;
    }

    /**
     * Add text field to dialog.
     */
    public void addTextField(String name, String text) {
        addTextField(name, text, "");
    }

    /**
     * Add text field to dialog.
     */
    public void addTextField(String name, String text, String value) {
        int row = rowsCount++;
        Label label = new Label(text);
        TextField field = new TextField(value);
        field.textProperty().addListener(x -> checkSubmit());

        root.add(label, 0, row);
        root.add(field, 2, row);

        textFields.put(name, field);
    }

    /**
     * Add choice
     * box to dialog.
     */
    public <T> void addChoiceBox(String name, String text, T[] values) {
        addChoiceBox(name, text, values, values.length > 0 ? values[0] : null);
    }

    /**
     * Add choice box to dialog.
     */
    public <T> void addChoiceBox(String name, String text, T[] values, T value) {
        int row = rowsCount++;
        Label label = new Label(text);

        ChoiceBox choiceBox = new ChoiceBox();
        choiceBox.getItems().addAll(values);
        choiceBox.getSelectionModel().select(value);

        root.add(label, 0, row);
        root.add(choiceBox, 2, row);
        choiceBoxes.put(name, choiceBox);
    }

    /**
     * Add check box to dialog.
     */
    public void addCheckBox(String name, String text) {
        addCheckBox(name, text, false);
    }

    /**
     * Add check box to dialog.
     */
    public void addCheckBox(String name, String text, boolean value) {
        int row = rowsCount++;

        CheckBox checkBox = new CheckBox(text);
        checkBox.setSelected(value);

        root.add(checkBox, 2, row);
        checkBoxes.put(name, checkBox);
    }

    /**
     * Add number field to dialog.
     */
    public void addNumberField(String name, String text) {
        addNumberField(name, text, 0);
    }

    /**
     * Add number field to dialog.
     */
    public void addNumberField(String name, String text, int value) {
        int row = rowsCount++;
        Label label = new Label(text);


        Spinner field = new Spinner<>(0, Integer.MAX_VALUE, value);
        field.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE, value));

        TextFormatter<Integer> numberFormatter = new TextFormatter<>(new IntegerStringConverter(), value, c -> {
            if (c.isContentChange()) {
                ParsePosition parsePosition = new ParsePosition(0);
                NumberFormat.getIntegerInstance().parse(c.getControlNewText(), parsePosition);

                if (parsePosition.getIndex() == 0 || parsePosition.getIndex() < c.getControlNewText().length()) {
                    return null;
                }
            }

            return c;
        });

        field.focusedProperty().addListener((observable, oldValue, newValue) -> {
            field.increment(0);
        });

        field.getEditor().setTextFormatter(numberFormatter);
        field.setEditable(true);

        root.add(label, 0, row);
        root.add(field, 2, row);
        spinners.put(name, field);
    }

    /**
     * Add slider to dialog.
     */
    public void addSlider(String name, String text, int min, int max, int value) {
        addSlider(name, text, min, max, value, 10, 10);
    }

    /**
     * Add slider to dialog.
     */
    public void addSlider(String name, String text, int min, int max, int value, int major, int minor) {
        int row = rowsCount++;

        Label label = new Label(text);

        Label current = new Label("(" + value + ")");
        current.setPrefWidth(40);
        current.setAlignment(Pos.CENTER_RIGHT);

        Slider slider = new Slider(min, max, value);
        slider.setMajorTickUnit(major);
        slider.setMinorTickCount(minor);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setPrefWidth(250);

        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int intValue = (int) Math.round(newValue.doubleValue());
            slider.setValue(intValue);
            current.setText("(" + intValue + ")");
        });

        root.add(label, 0, row);
        root.add(current, 1, row);
        root.add(slider, 2, row);
        sliders.put(name, slider);
    }

    /**
     * Show dialog and wait for submit.
     * @param onSubmit Function that accepts map in parameter. Key are field names and values are field values.
     */
    public void run(Consumer<Map<String, Object>> onSubmit) {
        checkSubmit();

        dialog.getDialogPane().setContent(root);

        dialog.setResultConverter(button -> {
            if (button == okButtonType) {
                return getValues();
            }

            return null;
        });

        Optional<Map<String, Object>> result = dialog.showAndWait();
        result.ifPresent(data -> onSubmit.accept(data));
    }

    /**
     * Get all values of form.
     * @return Map where keys are names of fields and values are values of fields.
     */
    private Map<String, Object> getValues() {
        Map<String, Object> values = new HashMap<>();

        for (Map.Entry<String, Slider> slider : sliders.entrySet()) {
            values.put(slider.getKey(), (int) Math.round(slider.getValue().getValue()));
        }

        for (Map.Entry<String, TextField> field : textFields.entrySet()) {
            values.put(field.getKey(), field.getValue().getText());
        }

        for (Map.Entry<String, Spinner> field : spinners.entrySet()) {
            values.put(field.getKey(), field.getValue().getValue());
        }

        for (Map.Entry<String, ChoiceBox> choiceBox : choiceBoxes.entrySet()) {
            values.put(choiceBox.getKey(), choiceBox.getValue().getValue());
        }

        for (Map.Entry<String, CheckBox> checkBox : checkBoxes.entrySet()) {
            values.put(checkBox.getKey(), checkBox.getValue().isSelected());
        }

        return values;
    }

    /**
     * Check if all fields are filled and enable/disable submit button.
     */
    private void checkSubmit() {
        boolean isEnabled = true;

        for (TextField field : textFields.values()) {
            if (field.getText().isEmpty()) {
                isEnabled = false;
                break;
            }
        }

        if (isEnabled) {
            for (ChoiceBox choiceBox : choiceBoxes.values()) {
                if (choiceBox.getSelectionModel().getSelectedItem() == null) {
                    isEnabled = false;
                    break;
                }
            }
        }

        dialog.getDialogPane().lookupButton(okButtonType).setDisable(!isEnabled);
    }

}
