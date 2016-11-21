package bazahe.ui.controller;

import bazahe.def.HttpMessage;
import bazahe.def.ProxyConfig;
import bazahe.httpparse.RequestHeaders;
import bazahe.httpparse.RequestLine;
import bazahe.httpproxy.ProxyServer;
import bazahe.ui.AppResources;
import bazahe.ui.UIHttpMessageListener;
import bazahe.ui.pane.HttpMessagePane;
import bazahe.ui.pane.ProxyConfigDialog;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Optional;

/**
 * @author Liu Dong
 */
public class MainController {
    @FXML
    private StackPane contentPane;
    @FXML
    private ListView<HttpMessage> requestList;
    @FXML
    private VBox root;
    @FXML
    private SplitPane splitPane;
    @FXML
    private Button proxyConfigureButton;
    @FXML
    private Button proxyControlButton;
    @FXML
    private HttpMessagePane httpMessagePane;

    private boolean proxyStart;

    private volatile ProxyServer proxyServer;
    private ProxyConfig config;

    @FXML
    void configureProxy(ActionEvent e) {
        ProxyConfigDialog dialog = new ProxyConfigDialog();
        dialog.proxyConfigProperty().setValue(config);
        Optional<ProxyConfig> newConfig = dialog.showAndWait();
        if (newConfig.isPresent()) {
            config = newConfig.get();
        }
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
        proxyConfigureButton.setDisable(true);
        proxyControlButton.setDisable(true);
        proxyServer = new ProxyServer(config);
        proxyServer.setHttpMessageListener(new UIHttpMessageListener(item -> requestList.getItems().add(item)));
        new Thread(() -> {
            proxyServer.start();
            Platform.runLater(() -> {
                proxyControlButton.setText("Stop");
                proxyControlButton.setDisable(false);
            });
        }).start();
    }

    private void stopProxy() {
        proxyStart = false;
        proxyControlButton.setDisable(true);
        new Thread(() -> {
            proxyServer.stop();
            Platform.runLater(() -> {
                proxyControlButton.setText("Start");
                proxyControlButton.setDisable(false);
                proxyConfigureButton.setDisable(false);
            });
        }).start();
    }

    @FXML
    void initialize() {
        AppResources.registerTask(() -> {
            if (proxyServer != null) {
                proxyServer.stop();
            }
        });
        splitPane.setDividerPositions(0.2, 0.6);
        requestList.setCellFactory(param -> new ListCell<HttpMessage>() {
            @Override
            protected void updateItem(HttpMessage item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                } else {
                    RequestHeaders requestHeaders = item.getRequestHeaders();
                    RequestLine requestLine = requestHeaders.getRequestLine();
                    String text = requestLine.getMethod() + " " + item.getUrl();
                    setText(text);
                }
            }
        });
        requestList.getSelectionModel().selectedItemProperty().addListener((ov, oldValue, newValue) -> {
            if (newValue == null) {
                httpMessagePane.setVisible(false);
            } else {
                httpMessagePane.httpMessageProperty().set(newValue);
                httpMessagePane.setVisible(true);
            }
        });
        config = ProxyConfig.getDefault();
    }

    @FXML
    private void clearAll(ActionEvent actionEvent) {
        requestList.getItems().clear();
    }
}
