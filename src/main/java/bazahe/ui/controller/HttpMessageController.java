package bazahe.ui.controller;

import bazahe.def.HttpMessage;
import bazahe.httpparse.ResponseHeaders;
import bazahe.store.BodyStore;
import bazahe.store.BodyStoreType;
import bazahe.ui.UIUtils;
import bazahe.ui.component.HttpMessagePane;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import lombok.SneakyThrows;
import net.dongliu.commons.Joiner;
import net.dongliu.commons.Strings;
import net.dongliu.commons.io.InputOutputs;
import net.dongliu.commons.io.ReaderWriters;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author Liu Dong
 */
public class HttpMessageController {
    @FXML
    private ComboBox<BodyStoreType> bodyTypeBox;
    @FXML
    private ComboBox<Charset> charsetBox;
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
            setSelectBody();
        });

        selectBody.selectedToggleProperty().addListener((o, old, newValue) -> setSelectBody());

        charsetBox.getItems().addAll(StandardCharsets.UTF_8, StandardCharsets.UTF_16, StandardCharsets.US_ASCII,
                StandardCharsets.ISO_8859_1,
                Charset.forName("GB18030"), Charset.forName("GBK"), Charset.forName("GB2312"),
                Charset.forName("BIG5"));
        bodyTypeBox.getItems().addAll(BodyStoreType.values());
    }

    private void setSelectBody() {
        BodyStore bodyStore = currentBodyStore();
        setBody(bodyStore);
    }

    private BodyStore currentBodyStore() {
        Toggle toggle = selectBody.selectedToggleProperty().get();
        BodyStore bodyStore;
        ObjectProperty<HttpMessage> httpMessageProperty = root.httpMessageProperty();
        Object userData = toggle.getUserData();
        if ("RequestBody".equals(userData)) {
            bodyStore = httpMessageProperty.get().getRequestBody();
        } else if ("ResponseBody".equals(userData)) {
            bodyStore = httpMessageProperty.get().getResponseBody();
        } else {
            throw new RuntimeException();
        }
        return bodyStore;
    }

    @SneakyThrows
    private void setBody(@Nullable BodyStore bodyStore) {
        if (bodyStore == null) {
            bodyPane.setCenter(new Text());
            return;
        }
        charsetBox.setValue(bodyStore.getCharset());
        bodyTypeBox.setValue(bodyStore.getType());

        if (!bodyStore.isClosed()) {
            bodyPane.setCenter(new Text("Still reading..."));
            return;
        }

        if (bodyStore.getSize() == 0) {
            bodyPane.setCenter(new Text());
            return;
        }

        // handle images
        if (bodyStore.isImage()) {
            Node imagePane = UIUtils.getImagePane(bodyStore.getInputStream(), bodyStore.getType());
            bodyPane.setCenter(imagePane);
            return;
        }

        // textual body
        if (bodyStore.isText()) {
            String text = ReaderWriters.readAll(new InputStreamReader(bodyStore.getInputStream(),
                    bodyStore.getCharset()));
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
            fileName = getFileName(httpMessage.getUrl(), httpMessage.getHost());
            if (!fileName.contains(".")) {
                // no extension
                fileName = addExtension(fileName, bodyStore.getType());
            }
        } else {
            throw new RuntimeException();
        }
        if (bodyStore == null || bodyStore.getSize() == 0) {
            UIUtils.showMessageDialog("This http message has nobody");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(fileName);
        File file = fileChooser.showSaveDialog(requestsHeaderText.getScene().getWindow());
        if (file == null) {
            return;
        }
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

    private String getFileName(String url, String host) {
        String s = Strings.afterLast(url, "/");
        s = Strings.before(s, "?");
        if (s.isEmpty()) {
            s = host;
        }
        return s;
    }

    @FXML
    @SneakyThrows
    void setMimeType(ActionEvent e) {
        BodyStore bodyStore = currentBodyStore();
        if (bodyStore == null) {
            return;
        }
        bodyStore.setType(bodyTypeBox.getSelectionModel().getSelectedItem());
        if (bodyStore.isClosed() && bodyStore.getSize() != 0) {
            setBody(bodyStore);
        }
    }

    @FXML
    @SneakyThrows
    void setCharset(ActionEvent e) {
        BodyStore bodyStore = currentBodyStore();
        if (bodyStore == null) {
            return;
        }
        bodyStore.setCharset(charsetBox.getSelectionModel().getSelectedItem());
        if (bodyStore.isClosed() && bodyStore.getSize() != 0 && bodyStore.isText()) {
            setBody(bodyStore);
        }
    }

}
