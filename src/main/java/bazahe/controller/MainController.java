package bazahe.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import lombok.SneakyThrows;

/**
 * @author Liu Dong
 */
public class MainController {
    @FXML
    private PasswordField passwordField;
    @FXML
    private Text actionTarget;

    @FXML
    private GridPane gridPane;

    @FXML
    @SneakyThrows
    private void handleSubmitButtonAction(ActionEvent e) {
        actionTarget.setText("signed in");
    }
}
