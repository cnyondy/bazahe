package bazahe.ui.component;

import bazahe.httpparse.HttpMessage;
import bazahe.ui.controller.HttpMessageController;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

/**
 * @author Liu Dong
 */
@Log4j2
public class HttpMessagePane extends SplitPane {
    private ObjectProperty<HttpMessage> httpMessageProperty = new SimpleObjectProperty<>();

    public ObjectProperty<HttpMessage> httpMessageProperty() {
        return httpMessageProperty;
    }

    @SneakyThrows
    public HttpMessagePane() {
        setOrientation(Orientation.HORIZONTAL);
        setDividerPositions(0.4);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/bazahe/http_message.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.load();
        HttpMessageController controller = fxmlLoader.getController();
    }
}
