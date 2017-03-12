package bazahe.ui.controller;

import bazahe.setting.MainSetting;
import bazahe.utils.StringUtils;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

/**
 * @author Liu Dong
 */
public class MainSettingController {
    @FXML
    private CheckBox autoSave;
    @FXML
    private TextField savePath;
    @FXML
    private TextField hostField;
    @FXML
    private TextField portFiled;
    @FXML
    private TextField timeoutField;

    public void setModel(MainSetting mainSetting) {
        hostField.setText(mainSetting.getHost());
        portFiled.setText("" + mainSetting.getPort());
        timeoutField.setText("" + mainSetting.getTimeout());
        savePath.setText("" + mainSetting.getPath());
        autoSave.setSelected(mainSetting.isAutoSave());
    }

    public MainSetting getModel() {
        return new MainSetting(hostField.getText(), StringUtils.toInt(portFiled.getText()),
                StringUtils.toInt(timeoutField.getText()), savePath.getText(),
                autoSave.isSelected());
    }

}
