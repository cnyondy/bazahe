package bazahe.ui.component;

import bazahe.KeyStoreSetting;
import bazahe.ui.controller.KeyStoreSettingController;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.GridPane;
import lombok.SneakyThrows;

/**
 * Show proxy configure.
 */
public class KeyStoreSettingDialog extends Dialog<KeyStoreSetting> {

    private final ObjectProperty<KeyStoreSetting> keyStoreSetting = new SimpleObjectProperty<>();

    @SneakyThrows
    public KeyStoreSettingDialog() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/bazahe/key_store_setting.fxml"));
        GridPane gridPane = loader.load();
        KeyStoreSettingController controller = loader.getController();

        getDialogPane().setContent(gridPane);
        setTitle("KeyStore Setting");
        final DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setContent(gridPane);

        setResultConverter((dialogButton) -> {
            ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonData.OK_DONE ? controller.getModel() : null;
        });

        keyStoreSetting.addListener((o, old, n) -> controller.setModel(n));

    }

    public KeyStoreSetting getKeyStoreSetting() {
        return keyStoreSetting.get();
    }

    public ObjectProperty<KeyStoreSetting> keyStoreSettingProperty() {
        return keyStoreSetting;
    }

}
