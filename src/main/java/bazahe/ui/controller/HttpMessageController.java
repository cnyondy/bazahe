package bazahe.ui.controller;

import bazahe.def.HttpMessage;
import bazahe.httpparse.ContentType;
import bazahe.httpparse.Headers;
import bazahe.httpparse.ResponseHeaders;
import bazahe.store.BodyStore;
import bazahe.store.BodyStoreType;
import bazahe.ui.TextMessageConverter;
import bazahe.ui.UIUtils;
import bazahe.ui.pane.HttpMessagePane;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Control;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
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
        BodyStore bodyStore;
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
    private void setBody(@Nullable Headers headers, @Nullable BodyStore bodyStore) {
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
            Control imagePane = UIUtils.getImagePane(bodyStore.getInputStream());
            bodyPane.setCenter(imagePane);
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
        HttpMessage httpMessage = root.httpMessageProperty().get();
        Toggle toggle = selectBody.selectedToggleProperty().get();
        String fileName = "";
        BodyStore bodyStore;
        if ("RequestBody".equals(toggle.getUserData())) {
            bodyStore = httpMessage.getRequestBody();
        } else if ("ResponseBody".equals(toggle.getUserData())) {
            bodyStore = httpMessage.getResponseBody();
            fileName = getFileName(httpMessage.getUrl());
            if (!fileName.contains(".")) {
                // no extension
                fileName = addExtension(fileName, bodyStore.getBodyStoreType());
            }
        } else {
            throw new RuntimeException();
        }
        if (bodyStore == null) {
            UIUtils.showMessageDialog("This http message has nobody");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(fileName);
        File file = fileChooser.showSaveDialog(requestsHeaderText.getScene().getWindow());
        try (OutputStream out = new FileOutputStream(file)) {
            InputOutputs.copy(bodyStore.getInputStream(), out);
        }
        UIUtils.showMessageDialog("Export Finished!");
    }

    private String addExtension(String suggestFileName, BodyStoreType bodyStoreType) {
        switch (bodyStoreType) {
            case html:
                return suggestFileName + ".html";
            case xml:
                return suggestFileName + ".xml";
            case json:
                return suggestFileName + ".json";
            case plainText:
            case formEncoded:
                return suggestFileName + ".txt";
            case jpeg:
                return suggestFileName + ".jpeg";
            case png:
                return suggestFileName + ".png";
            case gif:
                return suggestFileName + ".gif";
            case bmp:
                return suggestFileName + ".bmp";
            default:
                return suggestFileName;

        }
    }

    private String getFileName(String url) {
        String s = Strings.afterLast(url, "/");
        s = Strings.before(s, "?");
        return s;
    }
}
