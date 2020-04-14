package gui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Pair;
import paths.ICrossroad;
import structures.IRoutingMatrix;

import java.net.URL;
import java.util.ResourceBundle;

public class MatrixController implements Initializable {

    @FXML
    TableView<TableRow> matrixTable;

    private IRoutingMatrix<ICrossroad> matrix;

    public MatrixController(IRoutingMatrix<ICrossroad> matrix) {
        this.matrix = matrix;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        matrixTable.getColumns().clear();

        TableColumn<TableRow, String> firstColumn = new TableColumn<>("Cíl");
        firstColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getKey().getId()));
        matrixTable.getColumns().add(firstColumn);

        for (int i = 0; i < matrix.getNodes().length; i++) {
            matrixTable.getColumns().add(createColumn(i));
        }

        TableRow[] tableData = new TableRow[matrix.getNodes().length];

        for (int i = 0; i < matrix.getNodes().length; i++) {
            tableData[i] = new TableRow(matrix.getNodes()[i], matrix.getRouting()[i]);
        }

        matrixTable.getItems().setAll(tableData);
    }

    /**
     * Create column of table view.
     * @param i Index of column.
     */
    private TableColumn<TableRow, String> createColumn(int i) {
        TableColumn<TableRow, String> column = new TableColumn<>(matrix.getNodes()[i].getId());
        column.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getValue()[i] == null ? "-" : param.getValue().getValue()[i].getId()));
        return column;
    }

    private class TableRow extends Pair<ICrossroad, ICrossroad[]> {

        public TableRow(ICrossroad key, ICrossroad[] value) {
            super(key, value);
        }

    }

}