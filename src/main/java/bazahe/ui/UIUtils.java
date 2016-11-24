package bazahe.ui;

import javafx.beans.binding.Bindings;
import javafx.scene.control.Alert;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.InputStream;

/**
 * @author Liu Dong
 */
public class UIUtils {

    /**
     * Get a Pane show image, in center
     */
    public static Control getImagePane(InputStream inputStream) {
        Image image = new Image(inputStream);
        ImageView imageView = new ImageView();
        imageView.setImage(image);

        ScrollPane scrollPane = new ScrollPane();
        StackPane stackPane = new StackPane(imageView);
        stackPane.minWidthProperty().bind(Bindings.createDoubleBinding(() ->
                scrollPane.getViewportBounds().getWidth(), scrollPane.viewportBoundsProperty()));
        stackPane.minHeightProperty().bind(Bindings.createDoubleBinding(() ->
                scrollPane.getViewportBounds().getHeight(), scrollPane.viewportBoundsProperty()));

        scrollPane.setContent(stackPane);
        scrollPane.setStyle("-fx-background-color:transparent");
        return scrollPane;
    }

    public static void showMessageDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.setHeaderText("");
        alert.showAndWait();
    }
}
