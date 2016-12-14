package bazahe.ui.component;

import bazahe.def.WebSocketMessage;
import bazahe.ui.controller.WebSocketMessageController;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

/**
 * @author Liu Dong
 */
@Log4j2
public class WebSocketMessagePane extends BorderPane {
    private ObjectProperty<WebSocketMessage> message = new SimpleObjectProperty<>();

    public ObjectProperty<WebSocketMessage> messageProperty() {
        return message;
    }

    @SneakyThrows
    public WebSocketMessagePane() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/bazahe/web_socket_message.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.load();
        WebSocketMessageController controller = fxmlLoader.getController();
    }
}
