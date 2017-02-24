package bazahe.ui.controller;

import bazahe.setting.KeyStoreSetting;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * @author Liu Dong
 */
public class KeyStoreSettingController {

    @FXML
    private CheckBox useCustomCheckBox;
    @FXML
    private TextField keyStoreField;
    @FXML
    private Button chooseFileButton;
    @FXML
    private PasswordField keyStorePasswordField;


    @FXML
    void initialize() {
        useCustomCheckBox.selectedProperty().addListener((w, o, n) -> setUseCustom(n));
    }

    private void setUseCustom(boolean selected) {
        keyStoreField.setDisable(!selected);
        chooseFileButton.setDisable(!selected);
        keyStorePasswordField.setDisable(!selected);
    }

    public void setModel(KeyStoreSetting setting) {
        useCustomCheckBox.setSelected(setting.isUseCustom());
        keyStoreField.setText(setting.getKeyStore());
        keyStorePasswordField.setText(setting.getKeyStorePassword());
    }


    public KeyStoreSetting getModel() {
        KeyStoreSetting setting = new KeyStoreSetting();
        setting.setUseCustom(useCustomCheckBox.isSelected());
        setting.setKeyStore(keyStoreField.getText());
        setting.setKeyStorePassword(keyStorePasswordField.getText());
        return setting;
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
        File file = fileChooser.showOpenDialog(chooseFileButton.getScene().getWindow());
        if (file != null) {
            keyStoreField.setText(file.getAbsolutePath());
        }
    }
}
