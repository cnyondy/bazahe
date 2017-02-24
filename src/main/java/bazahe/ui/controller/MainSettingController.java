package bazahe.ui.controller;

import bazahe.setting.MainSetting;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.apache.commons.lang3.math.NumberUtils;

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
        mainSetting.setPort(NumberUtils.toInt(portFiled.getText()));
        mainSetting.setTimeout(NumberUtils.toInt(timeoutField.getText()));
        return mainSetting;
    }
    
}
