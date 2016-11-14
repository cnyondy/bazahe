package bazahe.ui;

import bazahe.httpparse.ContentType;
import bazahe.httpparse.Headers;
import bazahe.store.HttpBodyStore;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import lombok.Cleanup;
import lombok.SneakyThrows;
import net.dongliu.commons.Joiner;
import net.dongliu.commons.codec.Hexes;
import net.dongliu.commons.io.Closeables;
import net.dongliu.commons.io.InputOutputs;
import net.dongliu.commons.io.ReaderWriters;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

/**
 * @author Liu Dong
 */
public class HttpMessagePane extends SplitPane {
    @FXML
    private TextArea headerText;
    @FXML
    private TextArea bodyText;

    @SneakyThrows
    public HttpMessagePane() {
        setOrientation(Orientation.VERTICAL);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/bazahe/http_message.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    @SneakyThrows
    public void setValues(Headers headers, HttpBodyStore store) {
        List<String> strings = headers.toRawLines();
        headerText.setText(Joiner.of("\n").join(strings));
        if (!store.isClosed()) {
            bodyText.setText("Still reading...");
            return;
        }

        String contentEncoding = headers.contentEncoding();

        @Cleanup InputStream input = getInputStream(store, contentEncoding);

        ContentType contentType = headers.contentType();
        if (contentType == null) {
            contentType = ContentType.UNKNOWN;
        }
        if (contentType.isText()) {
            String text = ReaderWriters.readAll(new InputStreamReader(input,
                    contentType.getCharset() == null ? StandardCharsets.UTF_8 : contentType.getCharset()));
            bodyText.setText(text);
        } else {
            String hexes = Hexes.hexUpper(InputOutputs.readAll(input));
            bodyText.setText(hexes);
        }

    }

    private InputStream getInputStream(HttpBodyStore store, @Nullable String contentEncoding) throws IOException {
        InputStream input = store.getInputStream();
        try {
            if ("gzip".equalsIgnoreCase(contentEncoding)) {
                input = new GZIPInputStream(input);
            } else if ("deflate".equalsIgnoreCase(contentEncoding)) {
                input = new DeflaterInputStream(input);
            }
        } catch (Throwable t) {
            Closeables.close(input, t);
            throw t;
        }
        return input;
    }

    public StringProperty headerTextProperty() {
        return headerText.textProperty();
    }

    public StringProperty bodyTextProperty() {
        return bodyText.textProperty();
    }
}
