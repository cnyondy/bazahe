package bazahe.ui.controller;

import bazahe.def.HttpMessage;
import bazahe.def.Message;
import bazahe.def.ProxyConfig;
import bazahe.def.WebSocketMessage;
import bazahe.httpproxy.ProxyServer;
import bazahe.ui.AppResources;
import bazahe.ui.UIMessageListener;
import bazahe.ui.pane.HttpMessagePane;
import bazahe.ui.pane.ProxyConfigDialog;
import bazahe.ui.pane.WebSocketMessagePane;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * @author Liu Dong
 */
public class MainController {

    @FXML
    private TreeView<RTreeItem> messageTree;
    @FXML
    private StackPane contentPane;
    @FXML
    private ListView<Message> messageListView;
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
    private WebSocketMessagePane webSocketMessagePane;
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
        proxyServer.setMessageListener(new UIMessageListener(item -> Platform.runLater(() -> {
            messageListView.getItems().add(item);
            manifestTree(item);
        })));
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
        messageListView.setCellFactory(param -> new ListCell<Message>() {
            @Override
            protected void updateItem(Message item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                } else {
                    setText(item.getDisplay());
                }
            }
        });
        messageListView.getSelectionModel().selectedItemProperty().addListener((ov, oldValue, newValue) -> {
            if (newValue == null) {
                hideContent();
            } else {
                showMessage(newValue);
            }
        });


        TreeItem<RTreeItem> root = new TreeItem<>(new RTreeItem.Node(""));
        root.setExpanded(true);
        messageTree.setRoot(root);
        messageTree.setShowRoot(false);
        messageTree.setCellFactory(param -> new TreeCell<RTreeItem>() {
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
                        Message message = ((RTreeItem.Leaf) item).getMessage();
                        text = message.getDisplay();
                    } else {
                        text = "BUG..";
                    }
                    setText(text);
                }
            }
        });
        messageTree.getSelectionModel().selectedItemProperty().addListener((ov, o, n) -> {
            if (n == null || n.getValue() instanceof RTreeItem.Node) {
                hideContent();
            } else {
                RTreeItem.Leaf value = (RTreeItem.Leaf) n.getValue();
                showMessage(value.getMessage());
            }
        });


    }

    private void showMessage(Message message) {
        if (message instanceof HttpMessage) {
            httpMessagePane.httpMessageProperty().set((HttpMessage) message);
            httpMessagePane.setVisible(true);
            webSocketMessagePane.setVisible(false);
        } else if (message instanceof WebSocketMessage) {
            webSocketMessagePane.messageProperty().set((WebSocketMessage) message);
            httpMessagePane.setVisible(false);
            webSocketMessagePane.setVisible(true);
        }
    }

    private void hideContent() {
        httpMessagePane.setVisible(false);
        webSocketMessagePane.setVisible(false);
    }

    @FXML
    private void clearAll(ActionEvent actionEvent) {
        messageListView.getItems().clear();
        messageTree.setRoot(new TreeItem<>(new RTreeItem.Node("")));
    }

    @FXML
    private void group(ActionEvent actionEvent) {
        ObservableList<Node> children = contentPane.getChildren();
        if (groupedCheckBox.isSelected()) {
            children.remove(messageTree);
            children.add(messageTree);
        } else {
            children.remove(messageListView);
            children.add(messageListView);
        }
    }

    @SneakyThrows
    private void manifestTree(Message message) {
        TreeItem<RTreeItem> root = messageTree.getRoot();
        String host = message.getHost();

        for (TreeItem<RTreeItem> item : root.getChildren()) {
            RTreeItem.Node node = (RTreeItem.Node) item.getValue();
            if (node.getPattern().equals(host)) {
                item.getChildren().add(new TreeItem<>(new RTreeItem.Leaf(message)));
                node.increaseChildren();
                return;
            }
        }

        RTreeItem.Node node = new RTreeItem.Node(host);
        TreeItem<RTreeItem> nodeItem = new TreeItem<>(node);
        root.getChildren().add(nodeItem);
        nodeItem.getChildren().add(new TreeItem<>(new RTreeItem.Leaf(message)));
        node.increaseChildren();
    }
}
