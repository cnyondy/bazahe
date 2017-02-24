package bazahe.ui.component;

import bazahe.ui.controller.CatalogController;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

/**
 * @author Liu Dong
 */
@Log4j2
public class CatalogPane extends BorderPane {

    @Getter
    private CatalogController controller;

    @SneakyThrows
    public CatalogPane() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/bazahe/catalog_view.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.load();
        controller = fxmlLoader.getController();
    }
}
