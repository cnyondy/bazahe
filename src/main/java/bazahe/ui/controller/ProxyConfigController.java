package bazahe.ui.controller;

import bazahe.httpproxy.CAKeyStoreGenerator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Liu Dong
 */
public class ProxyConfigController {

    @FXML
    @Getter
    private Text message;
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
    private TextField keyStoreField;
    @FXML
    @Getter
    private TextField keyStorePasswordField;


    @FXML
    void choseFile(ActionEvent actionEvent) {
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

    @FXML
    @SneakyThrows
    private void generateNewKeyStore(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File directory = directoryChooser.showDialog(timeoutField.getScene().getWindow());
        if (directory != null) {
            Path keyStorePath = Paths.get(directory.getAbsolutePath(), "root_ca.p12");
            Path derCertPath = Paths.get(directory.getAbsolutePath(), "root_ca.crt");
            Path pemCertPath = Paths.get(directory.getAbsolutePath(), "root_ca.pem");

            CAKeyStoreGenerator generator = new CAKeyStoreGenerator();
            String password = "123456";
            generator.generate(password.toCharArray(), 3650);
            Files.write(keyStorePath, generator.getKeyStoreData());
            Files.write(derCertPath, generator.getDerCertData());
            Files.write(pemCertPath, generator.getPemCertData());

            keyStoreField.setText(keyStorePath.toString());
            keyStorePasswordField.setText(password);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("");
            alert.setContentText("Generate finished!");
            alert.showAndWait();
        }
    }
}
