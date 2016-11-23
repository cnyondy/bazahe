package bazahe.ui.controller;

import bazahe.def.HttpMessage;
import bazahe.httpparse.ContentType;
import bazahe.httpparse.Headers;
import bazahe.httpparse.ResponseHeaders;
import bazahe.store.HttpBodyStore;
import bazahe.ui.TextMessageConverter;
import bazahe.ui.pane.HttpMessagePane;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import lombok.SneakyThrows;
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
public class HttpMessageController {
    @FXML
    private HttpMessagePane root;
    @FXML
    private TextArea requestsHeaderText;
    @FXML
    private TextArea responseHeaderText;
    @FXML
    private BorderPane bodyPane;
    @FXML
    private ToggleGroup selectBody;

    @FXML
    void initialize() {
        Joiner joiner = Joiner.of("\n");
        ObjectProperty<HttpMessage> httpMessageProperty = root.httpMessageProperty();
        if (httpMessageProperty == null) {
            return;
        }
        httpMessageProperty.addListener((o, old, newValue) -> {
            requestsHeaderText.setText(joiner.join(newValue.getRequestHeaders().toRawLines()));
            ResponseHeaders responseHeaders = newValue.getResponseHeaders();
            if (responseHeaders != null) {
                responseHeaderText.setText(joiner.join(responseHeaders.toRawLines()));
            }

            Toggle toggle = selectBody.selectedToggleProperty().get();
            setSelectBody(toggle);
        });

        selectBody.selectedToggleProperty().addListener((o, old, newValue) -> {
            setSelectBody(newValue);
        });
    }

    private void setSelectBody(Toggle toggle) {
        ObjectProperty<HttpMessage> httpMessageProperty = root.httpMessageProperty();
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

        if (headers == null || bodyStore == null) {
            bodyPane.setCenter(new Text());
            return;
        }
        if (!bodyStore.isClosed()) {
            bodyPane.setCenter(new Text("Still reading..."));
            return;
        }
        if (bodyStore.getSize() == 0) {
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
                "bmp", "gif", "png", "jpeg", "ico")) {
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
        List<TextMessageConverter> messageConverters = root.getMessageConverters();
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

    @FXML
    @SneakyThrows
    void exportBody(ActionEvent e) {
        ObjectProperty<HttpMessage> httpMessageProperty = root.httpMessageProperty();
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
            alert.setHeaderText("");
            alert.showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showSaveDialog(requestsHeaderText.getScene().getWindow());
        try (OutputStream out = new FileOutputStream(file)) {
            InputOutputs.copy(bodyStore.getInputStream(), out);
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText("Export Finished!");
        alert.setHeaderText("");
        alert.showAndWait();
    }
}
