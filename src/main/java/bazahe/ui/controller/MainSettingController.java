package bazahe.ui.controller;

import bazahe.setting.MainSetting;
import bazahe.utils.StringUtils;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/**
 * @author Liu Dong
 */
public class MainSettingController {

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
    }

    public MainSetting getModel() {
        MainSetting mainSetting = new MainSetting();
        mainSetting.setHost(hostField.getText());
        mainSetting.setPort(StringUtils.toInt(portFiled.getText()));
        mainSetting.setTimeout(StringUtils.toInt(timeoutField.getText()));
        return mainSetting;
    }

}
