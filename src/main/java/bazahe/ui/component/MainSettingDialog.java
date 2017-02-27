package bazahe.ui.component;

import bazahe.setting.MainSetting;
import bazahe.ui.controller.MainSettingController;
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
public class MainSettingDialog extends Dialog<MainSetting> {

    private final ObjectProperty<MainSetting> mainSetting = new SimpleObjectProperty<>();

    @SneakyThrows
    public MainSettingDialog() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_setting.fxml"));
        GridPane gridPane = loader.load();
        MainSettingController controller = loader.getController();

        getDialogPane().setContent(gridPane);
        setTitle("Settings");
        final DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setContent(gridPane);

        setResultConverter((dialogButton) -> {
            ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonData.OK_DONE ? controller.getModel() : null;
        });

        mainSetting.addListener((o, old, n) -> controller.setModel(n));

    }

    public MainSetting getMainSetting() {
        return mainSetting.get();
    }

    public ObjectProperty<MainSetting> mainSettingProperty() {
        return mainSetting;
    }

}
