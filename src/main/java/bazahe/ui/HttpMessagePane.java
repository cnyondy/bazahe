package bazahe.ui;

import bazahe.httpparse.ContentType;
import bazahe.httpparse.Headers;
import bazahe.httpparse.ResponseHeaders;
import bazahe.store.HttpBodyStore;
import bazahe.ui.converter.AllTextMessageConverter;
import bazahe.ui.converter.JsonTextMessageConverter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.dongliu.commons.Joiner;
import net.dongliu.commons.RefValues;
import net.dongliu.commons.Strings;
import net.dongliu.commons.collection.Lists;
import net.dongliu.commons.io.InputOutputs;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @author Liu Dong
 */
@Log4j2
public class HttpMessagePane extends SplitPane {
    @FXML
    private TextArea requestsHeaderText;
    @FXML
    private TextArea responseHeaderText;
    @FXML
    private BorderPane bodyPane;
    @FXML
    private ToggleGroup selectBody;
    private ObjectProperty<HttpMessage> httpMessageProperty = new SimpleObjectProperty<>();

    private static List<TextMessageConverter> messageConverters;

    static {
        messageConverters = Lists.of(
                new JsonTextMessageConverter(),
                new AllTextMessageConverter()
        );
    }

    private static Joiner joiner = Joiner.of("\n");

    @FXML
    void initialize() {
        this.setDividerPositions(0.4);
        httpMessageProperty.addListener((o, old, newValue) -> {
            requestsHeaderText.setText(joiner.join(newValue.getRequestHeaders().getRawHeaders()));
            ResponseHeaders responseHeaders = newValue.getResponseHeaders();
            if (responseHeaders != null) {
                responseHeaderText.setText(joiner.join(responseHeaders.getRawHeaders()));
            }

            Toggle toggle = selectBody.selectedToggleProperty().get();
            setSelectBody(toggle);
        });

        selectBody.selectedToggleProperty().addListener((o, old, newValue) -> {
            setSelectBody(newValue);
        });
    }

    private void setSelectBody(Toggle toggle) {
        Object userData = toggle.getUserData();
        Headers headers;
        HttpBodyStore bodyStore;
        if ("RequestBody".equals(userData)) {
            headers = httpMessageProperty.get().getRequestHeaders();
            bodyStore = httpMessageProperty.get().getRequestBody();
        } else if ("ResponseBody".equals(userData)) {
            headers = httpMessageProperty.get().getResponseHeaders();
            bodyStore = httpMessageProperty.get().getResponseBody();
        } else {
            throw new RuntimeException();
        }

        if (headers == null || bodyStore == null || bodyStore.getSize() == 0) {
            bodyPane.setCenter(new Text());
            return;
        }
        setBody(headers, bodyStore);
    }

    @SneakyThrows
    private void setBody(@Nullable Headers headers, @Nullable HttpBodyStore bodyStore) {
        if (headers == null || bodyStore == null) {
            return;
        }
        if (!bodyStore.isClosed()) {
            bodyPane.setCenter(new Text("Still reading..."));
            return;
        }

        ContentType contentType = RefValues.ifNullThen(headers.contentType(), ContentType.UNKNOWN);
        // handle images
        if (contentType.isImage() && Strings.equalsAny(contentType.getMimeType().getSubType(),
                "bmp", "gif", "png", "jpeg")) {
            Image image = new Image(bodyStore.getInputStream());
            ImageView imageView = new ImageView();
            imageView.setImage(image);
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setStyle("-fx-background-color:transparent");
            scrollPane.setContent(imageView);
            bodyPane.setCenter(scrollPane);
            return;
        }

        // textual body
        TextMessageConverter converter = Lists.findFirst(messageConverters, c -> c.accept(contentType));
        if (converter != null) {
            String text = converter.convert(bodyStore.getInputStream(), contentType);
            TextArea textArea = new TextArea();
            textArea.setText(text);
            textArea.setEditable(false);
            bodyPane.setCenter(textArea);
            return;
        }

        // do not know how to handle
        Text t = new Text();
        long size = bodyStore.getSize();
        if (size > 0) {
            t.setText("Binary Body");
        }
        bodyPane.setCenter(t);
    }


    public ObjectProperty<HttpMessage> httpMessageProperty() {
        return httpMessageProperty;
    }

    @SneakyThrows
    public HttpMessagePane() {
        setOrientation(Orientation.HORIZONTAL);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/bazahe/http_message.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    @FXML
    @SneakyThrows
    void exportBody(ActionEvent e) {
        Toggle toggle = selectBody.selectedToggleProperty().get();
        HttpBodyStore bodyStore;
        if ("RequestBody".equals(toggle.getUserData())) {
            bodyStore = httpMessageProperty.get().getRequestBody();
        } else if ("ResponseBody".equals(toggle.getUserData())) {
            bodyStore = httpMessageProperty.get().getResponseBody();
        } else {
            throw new RuntimeException();
        }
        if (bodyStore == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("This http message has nobody");
            alert.showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showSaveDialog(getScene().getWindow());
        try (OutputStream out = new FileOutputStream(file)) {
            InputOutputs.copy(bodyStore.getInputStream(), out);
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText("Export Finished!");
        alert.showAndWait();
    }

    public void clear() {
        requestsHeaderText.setText("");
        responseHeaderText.setText("");
        bodyPane.setCenter(new Text());
    }
}
