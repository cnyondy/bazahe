package bazahe.ui.pane;

import bazahe.def.HttpMessage;
import bazahe.ui.TextMessageConverter;
import bazahe.ui.controller.HttpMessageController;
import bazahe.ui.converter.AllTextMessageConverter;
import bazahe.ui.converter.JsonTextMessageConverter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.dongliu.commons.collection.Lists;

import java.util.List;

/**
 * @author Liu Dong
 */
@Log4j2
public class HttpMessagePane extends SplitPane {
    private ObjectProperty<HttpMessage> httpMessageProperty = new SimpleObjectProperty<>();

    @Getter
    private List<TextMessageConverter> messageConverters = Lists.of(
            new JsonTextMessageConverter(),
            new AllTextMessageConverter()
    );

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
