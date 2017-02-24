package bazahe.ui.controller;

import bazahe.setting.ProxySetting;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * For set secondary proxy
 *
 * @author Liu Dong
 */
public class ProxySettingController {

    @FXML
    private CheckBox useProxy;
    @FXML
    private RadioButton socks5Radio;
    @FXML
    private RadioButton httpRadio;
    @FXML
    private TextField passwordField;
    @FXML
    private TextField userField;
    @FXML
    private TextField hostField;
    @FXML
    private TextField portFiled;
    @FXML
    private ToggleGroup proxyTypeGroup;


    @FXML
    void initialize() {
        enable(false);
        useProxy.setSelected(false);
        useProxy.selectedProperty().addListener((b, o, n) -> enable(n));
    }

    private void enable(Boolean n) {
        hostField.setDisable(!n);
        portFiled.setDisable(!n);
        userField.setDisable(!n);
        passwordField.setDisable(!n);
        for (Toggle toggle : proxyTypeGroup.getToggles()) {
            RadioButton radioButton = (RadioButton) toggle;
            radioButton.setDisable(!n);
        }
    }

    public void setModel(ProxySetting proxySetting) {
        useProxy.setSelected(proxySetting.isUse());
        hostField.setText(proxySetting.getHost());
        portFiled.setText(String.valueOf(proxySetting.getPort()));
        userField.setText(proxySetting.getUser());
        passwordField.setText(proxySetting.getPassword());
//        proxyTypeGroup.selectToggle();
        String type = proxySetting.getType();
        if (type.equals("socks5") || type.isEmpty()) {
            socks5Radio.setSelected(true);
        } else if (type.equals("http")) {
            httpRadio.setSelected(true);
        } else {
            throw new RuntimeException("unknown proxy type: " + type);
        }
    }

    public ProxySetting getModel() {
        ProxySetting proxySetting = new ProxySetting();
        proxySetting.setUse(useProxy.isSelected());
        proxySetting.setHost(hostField.getText());
        proxySetting.setPort(NumberUtils.toInt(portFiled.getText()));
        proxySetting.setUser(userField.getText());
        proxySetting.setPassword(passwordField.getText());
        RadioButton radioButton = (RadioButton) proxyTypeGroup.getSelectedToggle();
        proxySetting.setType((String) radioButton.getUserData());
        return proxySetting;
    }
}
