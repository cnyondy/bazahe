package bazahe.ui.controller;

import bazahe.SecondaryProxySetting;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * For set secondary proxy
 *
 * @author Liu Dong
 */
public class SecondaryProxyController {

    @FXML
    private CheckBox useSecondaryProxy;
    @FXML
    private RadioButton socks5;
    @FXML
    private RadioButton http;
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
        useSecondaryProxy.setSelected(false);
        useSecondaryProxy.selectedProperty().addListener((b, o, n) -> enable(n));
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

    public void setModel(SecondaryProxySetting secondaryProxySetting) {
        useSecondaryProxy.setSelected(secondaryProxySetting.isUse());
        hostField.setText(secondaryProxySetting.getHost());
        portFiled.setText(String.valueOf(secondaryProxySetting.getPort()));
        userField.setText(secondaryProxySetting.getUser());
        passwordField.setText(secondaryProxySetting.getPasssword());
//        proxyTypeGroup.selectToggle();
        String type = secondaryProxySetting.getType();
        if (type.equals("socks5") || type.isEmpty()) {
            socks5.setSelected(true);
        } else if (type.equals("http")) {
            http.setSelected(true);
        } else {
            throw new RuntimeException("unknown proxy type: " + type);
        }
    }

    public SecondaryProxySetting getModel() {
        SecondaryProxySetting secondaryProxySetting = new SecondaryProxySetting();
        secondaryProxySetting.setUse(useSecondaryProxy.isSelected());
        secondaryProxySetting.setHost(hostField.getText());
        secondaryProxySetting.setPort(NumberUtils.toInt(portFiled.getText()));
        secondaryProxySetting.setUser(userField.getText());
        secondaryProxySetting.setPasssword(passwordField.getText());
        RadioButton radioButton = (RadioButton) proxyTypeGroup.getSelectedToggle();
        secondaryProxySetting.setType(radioButton.getId());
        return secondaryProxySetting;
    }
}
