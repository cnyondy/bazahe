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
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.SneakyThrows;
import net.dongliu.commons.Marshaller;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * @author Liu Dong
 */
public class MainController {
    @FXML
    private TreeView<RTreeItem> requestTree;
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
    @FXML
    private CheckBox groupedCheckBox;

    private boolean proxyStart;

    private volatile ProxyServer proxyServer;
    private ProxyConfig config;
    private Path configPath = Paths.get(System.getProperty("user.home"), ".bazahe_config");

    @FXML
    @SneakyThrows
    void configureProxy(ActionEvent e) {
        ProxyConfigDialog dialog = new ProxyConfigDialog();
        dialog.proxyConfigProperty().setValue(config);
        Optional<ProxyConfig> newConfig = dialog.showAndWait();
        if (newConfig.isPresent()) {
            config = newConfig.get();
            byte[] data = Marshaller.marshal(config);
            Files.write(configPath, data);
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
        proxyServer.setHttpMessageListener(new UIHttpMessageListener(item -> {
            requestList.getItems().add(item);
            manifestTree(item);
        }));
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
    @SneakyThrows
    void initialize() {
        AppResources.registerTask(() -> {
            if (proxyServer != null) {
                proxyServer.stop();
            }
        });

        if (Files.exists(configPath)) {
            config = (ProxyConfig) Marshaller.unmarshal(Files.readAllBytes(configPath));
        } else {
            config = ProxyConfig.getDefault();
        }


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


        TreeItem<RTreeItem> root = new TreeItem<>(new RTreeItem.Node(""));
        root.setExpanded(true);
        requestTree.setRoot(root);
        requestTree.setShowRoot(false);
        requestTree.setCellFactory(param -> new TreeCell<RTreeItem>() {
            @Override
            protected void updateItem(RTreeItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                } else {
                    String text;
                    if (item instanceof RTreeItem.Node) {
                        text = ((RTreeItem.Node) item).getPattern() + "(" + ((RTreeItem.Node) item).getCount() + ")";
                    } else if (item instanceof RTreeItem.Leaf) {
                        HttpMessage message = ((RTreeItem.Leaf) item).getMessage();
                        RequestHeaders requestHeaders = message.getRequestHeaders();
                        RequestLine requestLine = requestHeaders.getRequestLine();
                        text = requestLine.getMethod() + " " + message.getUrl();
                    } else {
                        text = "BUG..";
                    }
                    setText(text);
                }
            }
        });
        requestTree.getSelectionModel().selectedItemProperty().addListener((ov, o, n) -> {
            if (n == null || n.getValue() instanceof RTreeItem.Node) {
                httpMessagePane.setVisible(false);
            } else {
                RTreeItem.Leaf value = (RTreeItem.Leaf) n.getValue();
                httpMessagePane.httpMessageProperty().set(value.getMessage());
                httpMessagePane.setVisible(true);
            }
        });


    }

    @FXML
    private void clearAll(ActionEvent actionEvent) {
        requestList.getItems().clear();
        requestTree.setRoot(new TreeItem<>(new RTreeItem.Node("")));
    }

    @FXML
    private void group(ActionEvent actionEvent) {
        ObservableList<Node> children = contentPane.getChildren();
        if (groupedCheckBox.isSelected()) {
            children.remove(requestTree);
            children.add(requestTree);
        } else {
            children.remove(requestList);
            children.add(requestList);
        }
    }

    @SneakyThrows
    private void manifestTree(HttpMessage httpMessage) {
        TreeItem<RTreeItem> root = requestTree.getRoot();
        URL url = new URL(httpMessage.getUrl());
        String host = url.getHost();

        for (TreeItem<RTreeItem> item : root.getChildren()) {
            RTreeItem.Node node = (RTreeItem.Node) item.getValue();
            if (node.getPattern().equals(host)) {
                item.getChildren().add(new TreeItem<>(new RTreeItem.Leaf(httpMessage)));
                node.increaseChinldren();
                return;
            }
        }

        RTreeItem.Node node = new RTreeItem.Node(host);
        TreeItem<RTreeItem> nodeItem = new TreeItem<>(node);
        root.getChildren().add(nodeItem);
        nodeItem.getChildren().add(new TreeItem<>(new RTreeItem.Leaf(httpMessage)));
        node.increaseChinldren();
    }
}
