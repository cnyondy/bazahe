package bazahe.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

/**
 * @author Liu Dong
 */
@Log4j2
public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        VBox root = FXMLLoader.load(getClass().getResource("/bazahe/main.fxml"));
        Scene scene = new Scene(root, 1200, 800);
        stage.setTitle("Bazahe");
        stage.setScene(scene);
        stage.show();
        root.requestFocus();

        stage.setOnCloseRequest(e -> {
            for (Runnable task : AppResources.tasks) {
                try {
                    task.run();
                } catch (Throwable t) {
                    log.error("", t);
                }
            }
            Platform.exit();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

}
