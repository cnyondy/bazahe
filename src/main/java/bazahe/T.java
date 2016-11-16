package bazahe;


import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * @author Liu Dong
 */
public class T extends Application {


    @Override
    public void start(Stage stage) throws Exception {
        BorderPane pane = new BorderPane();
        ImageView imageView = new ImageView();
        Image image = new Image("file:/Users/dongliu/Downloads/a.jpg");
        imageView.setImage(image);
        pane.setCenter(imageView);
        Scene scene = new Scene(pane, 800, 600);
        stage.setTitle("Bazahe");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();

    }
}
