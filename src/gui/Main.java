package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = loader.load();
        MainController controller = loader.getController();
        controller.setupStage(primaryStage);

        primaryStage.setTitle("Efektivní dynamické výpočty optimálních tras");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(720);
        primaryStage.setMinHeight(550);
        primaryStage.setMaximized(true);
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
