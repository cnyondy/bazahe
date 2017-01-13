package bazahe.ui.controller;

import bazahe.def.WebSocketMessage;
import bazahe.store.BodyStore;
import bazahe.ui.component.WebSocketMessagePane;
import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import lombok.SneakyThrows;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * @author Liu Dong
 */
public class WebSocketMessageController {
    @FXML
    private Text description;
    @FXML
    private WebSocketMessagePane root;

    @FXML
    void initialize() {
        Joiner joiner = Joiner.on("\n");
        root.messageProperty().addListener((ob, o, n) -> {
            if (n == null) {
                description.setText("");
                root.setCenter(new Text(""));
                return;
            }
            description.setText("WebSocket " + (n.isRequest() ? "Request" : "Response"));
            if (n.getType() == 2) {
                root.setCenter(new Text("Binary message"));
                return;
            }
            Node node = getTextArea(n);
            root.setCenter(node);
        });
    }

    @SneakyThrows
    private Node getTextArea(WebSocketMessage n) {
        BodyStore bodyStore = n.getBodyStore();
        if (!bodyStore.isClosed()) {
            return new Text("Still writing...");
        }

        TextArea textArea = new TextArea();
        try (InputStreamReader reader = new InputStreamReader(bodyStore.getInputStream(), StandardCharsets.UTF_8)) {
            String text = CharStreams.toString(reader);
            textArea.setText(text);
            return textArea;
        }
    }

    @FXML
    @SneakyThrows
    private void exportBody(ActionEvent e) {
        ObjectProperty<WebSocketMessage> messageObjectProperty = root.messageProperty();
        BodyStore bodyStore = messageObjectProperty.get().getBodyStore();
        if (bodyStore == null || bodyStore.getSize() == 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("This WebSocket message has nobody");
            alert.setHeaderText("");
            alert.showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showSaveDialog(root.getScene().getWindow());
        try (InputStream in = bodyStore.getInputStream();
             OutputStream out = new FileOutputStream(file)) {
            ByteStreams.copy(in, out);
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText("Export Finished!");
        alert.setHeaderText("");
        alert.showAndWait();
    }
}
