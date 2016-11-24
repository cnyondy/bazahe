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
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import lombok.SneakyThrows;
import lombok.val;
import net.dongliu.commons.Marshaller;
import net.dongliu.commons.Strings;

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

        TreeItem<RTreeItem> root = new TreeItem<>(new RTreeItem.Node(""));
        root.setExpanded(true);
        messageTree.setRoot(root);
        messageTree.setShowRoot(false);
        messageTree.setCellFactory(new TreeCellFactory());
        messageTree.setOnMouseClicked(new TreeViewMouseHandler());
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
        messageTree.setRoot(new TreeItem<>(new RTreeItem.Node("")));
    }

    @SneakyThrows
    private void manifestTree(Message message) {
        val root = messageTree.getRoot();
        String host = genericMultiCDNS(message.getHost());


        for (val item : root.getChildren()) {
            val node = (RTreeItem.Node) item.getValue();
            if (node.getPattern().equals(host)) {
                item.getChildren().add(new TreeItem<>(new RTreeItem.Leaf(message)));
                node.increaseChildren();
                return;
            }
        }

        val node = new RTreeItem.Node(host);
        val nodeItem = new TreeItem<RTreeItem>(node);
        root.getChildren().add(nodeItem);
        nodeItem.getChildren().add(new TreeItem<>(new RTreeItem.Leaf(message)));
        node.increaseChildren();
    }

    private String genericMultiCDNS(String host) {
        String first = Strings.before(host, ".");
        if (first.length() < 2) {
            return host;
        }
        if (!Strings.isAsciiLetter(first.charAt(0))) {
            return host;
        }
        char c = first.charAt(first.length() - 1);
        if (!Strings.isDigit(c)) {
            return host;
        }
        int idx = first.length() - 2;
        while (Strings.isDigit(first.charAt(idx))) {
            idx--;
        }
        return first.substring(0, idx + 1) + "*." + Strings.after(host, ".");
    }

}
