package bazahe.ui;

import bazahe.store.BodyStoreType;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import lombok.SneakyThrows;

import java.io.InputStream;

/**
 * @author Liu Dong
 */
public class UIUtils {

    /**
     * Get a Pane show otherImage, in center
     */
    @SneakyThrows
    public static Node getImagePane(InputStream inputStream, BodyStoreType type) {
        if (type == BodyStoreType.jpeg || type == BodyStoreType.png || type == BodyStoreType.gif
                || type == BodyStoreType.bmp) {
            ImageView imageView = new ImageView();
            Image image = new Image(inputStream);
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
        } else {
            Text text = new Text("Unsupported image format");
            return text;
        }
    }

    public static void showMessageDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.setHeaderText("");
        alert.showAndWait();
    }
}
