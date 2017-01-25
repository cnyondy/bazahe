package bazahe.ui.controller;

import bazahe.httpparse.HttpMessage;
import bazahe.httpparse.ResponseHeaders;
import bazahe.store.BodyStore;
import bazahe.store.BodyStoreType;
import bazahe.ui.UIUtils;
import bazahe.ui.component.HttpMessagePane;
import bazahe.ui.formater.FormURLEncodedFormatter;
import bazahe.ui.formater.JSONFormatter;
import bazahe.ui.formater.XMLFormatter;
import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import lombok.SneakyThrows;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

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
    private ComboBox<BodyStoreType> bodyTypeBox;
    @FXML
    private ComboBox<Charset> charsetBox;
    @FXML
    private ToggleButton beautifyButton;

    @FXML
    void initialize() {
        Joiner joiner = Joiner.on("\n");
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
        refreshBody(bodyStore);
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
    private void refreshBody(@Nullable BodyStore bodyStore) {
        if (bodyStore == null) {
            bodyPane.setCenter(new Text());
            return;
        }

        BodyStoreType storeType = bodyStore.getType();

        charsetBox.setValue(bodyStore.getCharset());
        charsetBox.setManaged(storeType.isText());
        charsetBox.setVisible(storeType.isText());

        boolean showBeautify = storeType == BodyStoreType.json || storeType == BodyStoreType.www_form ||
                storeType == BodyStoreType.xml;
        beautifyButton.setSelected(bodyStore.isBeaufify());
        beautifyButton.setManaged(showBeautify);
        beautifyButton.setVisible(showBeautify);

        bodyTypeBox.setValue(storeType);

        if (!bodyStore.isClosed()) {
            bodyPane.setCenter(new Text("Still reading..."));
            return;
        }

        if (bodyStore.getSize() == 0) {
            bodyPane.setCenter(new Text());
            return;
        }

        // handle images
        if (storeType.isImage()) {
            Node imagePane = UIUtils.getImagePane(bodyStore.getInputStream(), storeType);
            bodyPane.setCenter(imagePane);
            return;
        }

        // textual body
        if (storeType.isText()) {
            String text;
            try (Reader reader = new InputStreamReader(bodyStore.getInputStream(), bodyStore.getCharset())) {
                text = CharStreams.toString(reader);
            }

            // beautify
            if (bodyStore.isBeaufify()) {
                Function<String, String> formatter = getFormatter(bodyStore.getCharset(), storeType);
                text = formatter.apply(text);
            }

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

    private Function<String, String> getFormatter(Charset charset, BodyStoreType storeType) {
        Function<String, String> formatter;
        if (storeType == BodyStoreType.json) {
            formatter = new JSONFormatter(4);
        } else if (storeType == BodyStoreType.xml) {
            formatter = new XMLFormatter(4);
        } else if (storeType == BodyStoreType.www_form) {
            formatter = new FormURLEncodedFormatter(charset);
        } else {
            formatter = s -> s;
        }
        return formatter;
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
        try (InputStream in = bodyStore.getInputStream();
             OutputStream out = new FileOutputStream(file)) {
            ByteStreams.copy(in, out);
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
            case text:
            case www_form:
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
        int begin = url.lastIndexOf("/");
        String s;
        if (begin > 0) {
            int end = url.indexOf("?", begin);
            if (end < 0) {
                s = url.substring(begin + 1);
            } else {
                s = url.substring(begin + 1, end);
            }
        } else {
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
            refreshBody(bodyStore);
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
        if (bodyStore.isClosed() && bodyStore.getSize() != 0 && bodyStore.getType().isText()) {
            refreshBody(bodyStore);
        }
    }

    @FXML
    @SneakyThrows
    void beautify(ActionEvent e) {
        BodyStore bodyStore = currentBodyStore();
        if (bodyStore == null) {
            return;
        }
        bodyStore.setBeaufify(beautifyButton.isSelected());
        if (bodyStore.isClosed() && bodyStore.getSize() != 0 && bodyStore.getType().isText()) {
            refreshBody(bodyStore);
        }
    }
}
