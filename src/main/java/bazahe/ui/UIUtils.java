package bazahe.ui;

import bazahe.store.BodyStoreType;
import bazahe.ui.component.ProgressDialog;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.InputStream;

/**
 * @author Liu Dong
 */
@Log4j2
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
            return new Text("Unsupported image format");
        }
    }

    public static void showMessageDialog(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText(message);
            alert.setHeaderText("");
            alert.showAndWait();
        });
    }


    public static <T> void runBackground(Task<T> task, String failedMessage) {
        ProgressDialog progressDialog = new ProgressDialog();
        progressDialog.bindTask(task);

        task.setOnSucceeded(e -> Platform.runLater(progressDialog::close));
        task.setOnFailed(e -> {
            Platform.runLater(progressDialog::close);
            Throwable throwable = task.getException();
            logger.error(failedMessage, throwable);
            UIUtils.showMessageDialog(failedMessage + ": " + throwable.getMessage());
        });

        Thread thread = new Thread(task);
        thread.start();
        progressDialog.show();
    }
}
