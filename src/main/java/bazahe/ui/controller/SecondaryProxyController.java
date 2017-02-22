package bazahe.ui.controller;

import bazahe.SecondaryProxy;
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

    public void setModel(SecondaryProxy secondaryProxy) {
        useSecondaryProxy.setSelected(secondaryProxy.isUse());
        hostField.setText(secondaryProxy.getHost());
        portFiled.setText(String.valueOf(secondaryProxy.getPort()));
        userField.setText(secondaryProxy.getUser());
        passwordField.setText(secondaryProxy.getPasssword());
//        proxyTypeGroup.selectToggle();
        String type = secondaryProxy.getType();
        if (type.equals("socks5") || type.isEmpty()) {
            socks5.setSelected(true);
        } else if (type.equals("http")) {
            http.setSelected(true);
        } else {
            throw new RuntimeException("unknown proxy type: " + type);
        }
    }

    public SecondaryProxy getModel() {
        SecondaryProxy secondaryProxy = new SecondaryProxy();
        secondaryProxy.setUse(useSecondaryProxy.isSelected());
        secondaryProxy.setHost(hostField.getText());
        secondaryProxy.setPort(NumberUtils.toInt(portFiled.getText()));
        secondaryProxy.setUser(userField.getText());
        secondaryProxy.setPasssword(passwordField.getText());
        RadioButton radioButton = (RadioButton) proxyTypeGroup.getSelectedToggle();
        secondaryProxy.setType(radioButton.getId());
        return secondaryProxy;
    }
}
