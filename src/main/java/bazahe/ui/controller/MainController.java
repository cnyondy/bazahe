package bazahe.ui.controller;

import bazahe.def.HttpMessage;
import bazahe.def.Message;
import bazahe.def.ProxyConfig;
import bazahe.def.WebSocketMessage;
import bazahe.httpproxy.ProxyServer;
import bazahe.httpproxy.SSLContextManager;
import bazahe.ui.AppResources;
import bazahe.ui.UIMessageListener;
import bazahe.ui.UIUtils;
import bazahe.ui.component.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import net.dongliu.commons.Marshaller;
import net.dongliu.commons.Strings;
import net.dongliu.commons.collection.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * @author Liu Dong
 */
@Log4j2
public class MainController {

    @FXML
    private TreeView<RTreeItemValue> messageTree;
    @FXML
    private VBox root;
    @FXML
    private SplitPane splitPane;
    @FXML
    private MyButton proxyConfigureButton;
    @FXML
    private MyButton proxyControlButton;
    @FXML
    private HttpMessagePane httpMessagePane;
    @FXML
    private WebSocketMessagePane webSocketMessagePane;

    private boolean proxyStart;

    private volatile ProxyServer proxyServer;
    private volatile ProxyConfig config;
    private volatile SSLContextManager sslContextManager;

    @FXML
    @SneakyThrows
    void configureProxy(ActionEvent e) {
        ProxyConfigDialog dialog = new ProxyConfigDialog();
        dialog.proxyConfigProperty().setValue(config);
        Optional<ProxyConfig> newConfig = dialog.showAndWait();
        if (newConfig.isPresent()) {
            config = newConfig.get();
            byte[] data = Marshaller.marshal(config);
            Path configPath = ProxyConfig.getConfigPath();
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
        try {
            proxyServer = new ProxyServer(config, sslContextManager);
            proxyServer.setMessageListener(new UIMessageListener(item -> Platform.runLater(() -> addTreeItemMessage
                    (item))));
            proxyServer.start();
        } catch (Throwable t) {
            log.error("Start proxy failed", t);
            UIUtils.showMessageDialog("Start proxy failed!");
            return;
        }
        Platform.runLater(() -> {
            proxyControlButton.setIconPath("/images/ic_stop_black_24dp_1x.png");
            proxyControlButton.setDisable(false);
        });
    }

    private void stopProxy() {
        proxyStart = false;
        proxyControlButton.setDisable(true);
        new Thread(() -> {
            proxyServer.stop();
            Platform.runLater(() -> {
                proxyControlButton.setIconPath("/images/ic_play_circle_outline_black_24dp_1x.png");
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

        TreeItem<RTreeItemValue> root = new TreeItem<>(new RTreeItemValue.NodeValue(""));
        root.setExpanded(true);
        messageTree.setRoot(root);
        messageTree.setShowRoot(false);
        messageTree.setCellFactory(new TreeCellFactory());
        messageTree.setOnMouseClicked(new TreeViewMouseHandler());
        messageTree.getSelectionModel().selectedItemProperty().addListener((ov, o, n) -> {
            if (n == null || n.getValue() instanceof RTreeItemValue.NodeValue) {
                hideContent();
            } else {
                RTreeItemValue.LeafValue value = (RTreeItemValue.LeafValue) n.getValue();
                showMessage(value.getMessage());
            }
        });

        loadConfigAndKeyStore();
    }

    private void loadConfigAndKeyStore() {
        val task = new InitTask();

        ProgressDialog progressDialog = new ProgressDialog();
        progressDialog.bindTask(task);

        task.setOnSucceeded(e -> {
            Platform.runLater(progressDialog::close);
            try {
                Pair<ProxyConfig, SSLContextManager> result = task.get();
                config = result.first();
                sslContextManager = result.second();
            } catch (Exception e1) {
                log.error("", e1);
            }
        });
        task.setOnFailed(e -> {
            Platform.runLater(progressDialog::close);
            Throwable throwable = task.getException();
            log.error("Init failed", throwable);
            UIUtils.showMessageDialog("Init config failed!");
        });

        Thread thread = new Thread(task);
        thread.start();
        progressDialog.show();
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
        messageTree.setRoot(new TreeItem<>(new RTreeItemValue.NodeValue("")));
    }

    @SneakyThrows
    private void addTreeItemMessage(Message message) {
        val root = messageTree.getRoot();
        String host = genericMultiCDNS(message.getHost());


        for (val item : root.getChildren()) {
            val node = (RTreeItemValue.NodeValue) item.getValue();
            if (node.getPattern().equals(host)) {
                item.getChildren().add(new TreeItem<>(new RTreeItemValue.LeafValue(message)));
                node.increaseChildren();
                return;
            }
        }

        val node = new RTreeItemValue.NodeValue(host);
        val nodeItem = new TreeItem<RTreeItemValue>(node);
        root.getChildren().add(nodeItem);
        nodeItem.getChildren().add(new TreeItem<>(new RTreeItemValue.LeafValue(message)));
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

    @FXML
    void open(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Bazahe archive data", "*.baza"));
        File file = fileChooser.showOpenDialog(this.root.getScene().getWindow());
        if (file == null) {
            return;
        }

        TreeItem<RTreeItemValue> root = messageTree.getRoot();
        root.getChildren().clear();
        ProgressDialog progressDialog = new ProgressDialog();
        Task<Void> saveTask = new LoadTask(file.getPath(), this::addTreeItemMessage);

        progressDialog.bindTask(saveTask);
        saveTask.setOnSucceeded(e -> {
            Platform.runLater(progressDialog::close);
        });
        saveTask.setOnFailed(e -> {
            Platform.runLater(progressDialog::close);
            Throwable throwable = saveTask.getException();
            log.error("Load data failed", throwable);
            UIUtils.showMessageDialog("Loading data failed!");
        });

        Thread thread = new Thread(saveTask);
        thread.start();
        progressDialog.show();
    }

    // save captured data to file
    @FXML
    void save(ActionEvent event) throws IOException {

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Bazahe archive data", "*.baza"));
        fileChooser.setInitialFileName("bazahe.baza");
        File file = fileChooser.showSaveDialog(this.root.getScene().getWindow());
        if (file == null) {
            return;
        }
        TreeItem<RTreeItemValue> root = messageTree.getRoot();
        ProgressDialog progressDialog = new ProgressDialog();
        Task<Void> saveTask = new SaveTask(file.getPath(), root);

        progressDialog.bindTask(saveTask);
        saveTask.setOnSucceeded(e -> {
            Platform.runLater(progressDialog::close);
        });
        saveTask.setOnFailed(e -> {
            Platform.runLater(progressDialog::close);
            Throwable throwable = saveTask.getException();
            log.error("Save data failed", throwable);
            UIUtils.showMessageDialog("Save data failed!");
        });

        Thread thread = new Thread(saveTask);
        thread.start();
        progressDialog.show();
    }
}
