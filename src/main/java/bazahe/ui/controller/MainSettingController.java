package bazahe.ui.controller;

import bazahe.MainSetting;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;

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
    @FXML
    private CheckBox useCustomKeyStoreCheckBox;
    @FXML
    private TextField keyStoreField;
    @FXML
    private Button chooseFileButton;
    @FXML
    private PasswordField keyStorePasswordField;


    @FXML
    void initialize() {
        useCustomKeyStoreCheckBox.selectedProperty().addListener((w, o, n) -> setUseCustom(n));
    }

    private void setUseCustom(boolean selected) {
        keyStoreField.setDisable(!selected);
        chooseFileButton.setDisable(!selected);
        keyStorePasswordField.setDisable(!selected);
    }

    public void setModel(MainSetting mainSetting) {
        hostField.setText(mainSetting.getHost());
        portFiled.setText("" + mainSetting.getPort());
        timeoutField.setText("" + mainSetting.getTimeout());
        useCustomKeyStoreCheckBox.setSelected(mainSetting.isUseCustomKeyStore());
        keyStoreField.setText(mainSetting.getKeyStore());
        keyStorePasswordField.setText(new String(mainSetting.getKeyStorePassword()));
    }


    public MainSetting getModel() {
        MainSetting mainSetting = new MainSetting();
        mainSetting.setHost(hostField.getText());
        mainSetting.setPort(NumberUtils.toInt(portFiled.getText()));
        mainSetting.setTimeout(NumberUtils.toInt(timeoutField.getText()));
        mainSetting.setUseCustomKeyStore(useCustomKeyStoreCheckBox.isSelected());
        mainSetting.setKeyStore(keyStoreField.getText());
        mainSetting.setKeyStorePassword(keyStorePasswordField.getText().toCharArray());
        return mainSetting;
    }


    @FXML
    void choseFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        if (keyStoreField.getText() != null && !keyStoreField.getText().isEmpty()) {
            File file = new File(keyStoreField.getText());
            if (file.exists()) {
                fileChooser.setInitialDirectory(file.getParentFile());
            }
        }
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PKCS12 KeyStore File", "*.p12"));
        File file = fileChooser.showOpenDialog(timeoutField.getScene().getWindow());
        if (file != null) {
            keyStoreField.setText(file.getAbsolutePath());
        }
    }
}
