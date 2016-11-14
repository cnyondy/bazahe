package bazahe.ui;

import bazahe.httpparse.RequestHeaders;
import bazahe.httpparse.RequestLine;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * @author Liu Dong
 */
public class MainController {
    @FXML
    private ListView<RequestListItem> requestList;
    @FXML
    private VBox root;
    @FXML
    private SplitPane splitPane;
    @FXML
    private Button proxyConfigureButton;
    @FXML
    private Button proxyControlButton;
    @FXML
    private HttpMessagePane requestPane;
    @FXML
    private HttpMessagePane responsePane;

    private boolean proxyStart;
    private ProxyService proxyService;

    @FXML
    void configureProxy(ActionEvent e) {
    }

    @FXML
    void proxyControl(ActionEvent e) {
        if (proxyStart) {
            stopProxy();
        } else {
            startProxy();
        }
    }

    private void startProxy() {
        proxyStart = true;
        proxyControlButton.setText("Stop");
        proxyConfigureButton.setDisable(true);
        proxyService = new ProxyService();
        proxyService.setHttpMessageListener(new UIHttpMessageListener(item -> requestList.getItems().add(item)));
        proxyService.start();
    }

    private void stopProxy() {
        proxyStart = false;
        proxyControlButton.setText("Start");
        proxyConfigureButton.setDisable(false);
        if (!proxyService.cancel()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error");
            alert.setHeaderText("Stop proxy error");
            alert.setContentText("Cannot stop proxy; Please restart the application manually");
            alert.showAndWait();
        }
    }

    @FXML
    void initialize() {
        splitPane.setDividerPositions(0.2, 0.6);
        requestList.setCellFactory(param -> new ListCell<RequestListItem>() {
            @Override
            protected void updateItem(RequestListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                } else {
                    RequestHeaders requestHeaders = item.getRequestHeaders();
                    RequestLine requestLine = requestHeaders.getRequestLine();
                    String text = requestLine.getMethod() + " " + requestLine.getUrl();
                    setText(text);
                }
            }
        });
        requestList.getSelectionModel().selectedItemProperty().addListener((ov, oldValue, newValue) -> {
            requestPane.setValues(newValue.getRequestHeaders(), newValue.getRequestBody());
            if (newValue.getResponseHeaders() != null) {
                responsePane.setValues(newValue.getResponseHeaders(), newValue.getResponseBody());
            }
        });
    }

}
