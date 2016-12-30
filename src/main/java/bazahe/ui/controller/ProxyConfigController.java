package bazahe.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import lombok.Getter;

import java.io.File;

/**
 * @author Liu Dong
 */
public class ProxyConfigController {

    @FXML
    @Getter
    private TextField hostField;
    @FXML
    @Getter
    private TextField portFiled;
    @FXML
    @Getter
    private TextField timeoutField;
    @FXML
    @Getter
    private CheckBox useCustomKeyStoreCheckBox;
    @FXML
    @Getter
    private TextField keyStoreField;
    @FXML
    @Getter
    private Button chooseFileButton;
    @FXML
    @Getter
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
