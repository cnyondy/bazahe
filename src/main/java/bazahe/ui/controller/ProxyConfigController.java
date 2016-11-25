package bazahe.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import lombok.Getter;

/**
 * @author Liu Dong
 */
public class ProxyConfigController {

//    @FXML
//    @Getter
//    private Text message;
    @FXML
    @Getter
    private TextField hostField;
    @FXML
    @Getter
    private TextField portFiled;
    @FXML
    @Getter
    private TextField timeoutField;
//    @FXML
//    @Getter
//    private TextField keyStoreField;
//    @FXML
//    @Getter
//    private TextField keyStorePasswordField;

//
//    @FXML
//    void choseFile(ActionEvent actionEvent) {
//        FileChooser fileChooser = new FileChooser();
//        if (keyStoreField.getText() != null && !keyStoreField.getText().isEmpty()) {
//            File file = new File(keyStoreField.getText());
//            if (file.exists()) {
//                fileChooser.setInitialDirectory(file.getParentFile());
//            }
//        }
//        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PKCS12 KeyStore File", "*.p12"));
//        File file = fileChooser.showOpenDialog(timeoutField.getScene().getWindow());
//        if (file != null) {
//            keyStoreField.setText(file.getAbsolutePath());
//        }
//    }
//
//    @FXML
//    @SneakyThrows
//    private void generateNewKeyStore(ActionEvent actionEvent) {
//        DirectoryChooser directoryChooser = new DirectoryChooser();
//        File directory = directoryChooser.showDialog(timeoutField.getScene().getWindow());
//        if (directory != null) {
//            Path keyStorePath = Paths.get(directory.getAbsolutePath(), "root_ca.p12");
//            Path derCertPath = Paths.get(directory.getAbsolutePath(), "root_ca.crt");
//            Path pemCertPath = Paths.get(directory.getAbsolutePath(), "root_ca.pem");
//
//            CAKeyStoreGenerator generator = new CAKeyStoreGenerator();
//            generator.generate(Constants.keyStorePassword, Constants.rootCertificateValidates);
//            Files.write(keyStorePath, generator.getKeyStoreData());
//            Files.write(derCertPath, generator.getDerCertData());
//            Files.write(pemCertPath, generator.getPemCertData());
//
//            keyStoreField.setText(keyStorePath.toString());
//            keyStorePasswordField.setText(new String(Constants.keyStorePassword));
//
//            Alert alert = new Alert(Alert.AlertType.INFORMATION);
//            alert.setHeaderText("");
//            alert.setContentText("Generate finished!");
//            alert.showAndWait();
//        }
//    }
}
