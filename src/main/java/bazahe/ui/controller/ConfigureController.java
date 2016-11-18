package bazahe.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import lombok.Getter;

import java.io.File;

/**
 * @author Liu Dong
 */
public class ConfigureController {

    @FXML
    @Getter
    private TextField hostField;
    @FXML
    @Getter
    private TextField timeoutField;
    @FXML
    @Getter
    private TextField keyStoreField;
    @FXML
    @Getter
    private TextField portFiled;

    @FXML
    void choseFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        if (keyStoreField.getText() != null && !keyStoreField.getText().isEmpty()) {
            File file = new File(keyStoreField.getText());
            if (file.exists()) {
                fileChooser.setInitialDirectory(file.getParentFile());
            }
        }
        File file = fileChooser.showOpenDialog(timeoutField.getScene().getWindow());
        if (file != null) {
            keyStoreField.setText(file.getAbsolutePath());
        }
    }
}
