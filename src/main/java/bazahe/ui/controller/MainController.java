package bazahe.ui.controller;

import bazahe.AppResources;
import bazahe.Context;
import bazahe.httpparse.HttpMessage;
import bazahe.httpparse.Message;
import bazahe.httpparse.WebSocketMessage;
import bazahe.httpproxy.ProxyServer;
import bazahe.ui.UIMessageListener;
import bazahe.ui.UIUtils;
import bazahe.ui.component.*;
import bazahe.utils.NetUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.controlsfx.control.PopOver;

import java.io.File;
import java.io.IOException;

import static java.util.stream.Collectors.*;

/**
 * @author Liu Dong
 */
@Log4j2
public class MainController {

    @FXML
    private MyButton saveFileButton;
    @FXML
    private MyButton openFileButton;
    @FXML
    private TreeView<RTreeItemValue> messageTree;
    @FXML
    private VBox root;
    @FXML
    private SplitPane splitPane;
    @FXML
    private SplitMenuButton proxyConfigureButton;
    @FXML
    private MyButton proxyControlButton;

    @FXML
    private Label listenedAddressLabel;

    @FXML
    private HttpMessagePane httpMessagePane;
    @FXML
    private WebSocketMessagePane webSocketMessagePane;

    private boolean proxyStart;

    private volatile ProxyServer proxyServer;
    private Context context = Context.getInstance();

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
        openFileButton.setDisable(true);
        saveFileButton.setDisable(true);
        try {
            proxyServer = new ProxyServer(context.getMainSetting(), context.getSslContextManager());
            proxyServer.setMessageListener(new UIMessageListener(this::addTreeItemMessage));
            proxyServer.start();
        } catch (Throwable t) {
            logger.error("Start proxy failed", t);
            UIUtils.showMessageDialog("Start proxy failed!");
            return;
        }
        Platform.runLater(() -> {
            proxyControlButton.setIconPath("/images/ic_stop_black_24dp_1x.png");
            proxyControlButton.setDisable(false);
            updateListenedAddress();
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
                openFileButton.setDisable(false);
                saveFileButton.setDisable(false);
                listenedAddressLabel.setText("");
                listenedAddressLabel.setOnMouseClicked(event -> {});
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

        val root = new TreeItem<RTreeItemValue>(new RTreeItemValue.NodeValue(""));
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

    /**
     * Load app mainSetting, and keyStore contains private key/certs
     */
    private void loadConfigAndKeyStore() {
        val task = new InitContextTask(context);
        UIUtils.runBackground(task, "Init mainSetting failed");
    }

    /**
     * Handle setting menu
     */
    @FXML
    @SneakyThrows
    void updateSetting(ActionEvent e) {
        val dialog = new MainSettingDialog();
        dialog.proxyConfigProperty().setValue(context.getMainSetting());
        val newConfig = dialog.showAndWait();
        if (newConfig.isPresent()) {
            val task = new SaveSettingTask(context, newConfig.get(), context.getSecondaryProxy());
            UIUtils.runBackground(task, "save settings failed");
        }
    }


    @FXML
    void setSecondaryProxy(ActionEvent e) {
        SecondaryProxyDialog dialog = new SecondaryProxyDialog();
        dialog.secondaryProxyProperty().setValue(context.getSecondaryProxy());
        val newConfig = dialog.showAndWait();
        if (newConfig.isPresent()) {
            val task = new SaveSettingTask(context, context.getMainSetting(), newConfig.get());
            UIUtils.runBackground(task, "save secondary proxy setting failed");
        }
    }

    /**
     * Get listened addresses, show in toolbar
     */
    private void updateListenedAddress() {
        val config = context.getMainSetting();
        String host = config.getHost().trim();
        int port = config.getPort();
        if (!host.isEmpty()) {
            Platform.runLater(() -> listenedAddressLabel.setText("Listened " + host + ":" + port));
            return;
        }
        Platform.runLater(() -> listenedAddressLabel.setText("Listened *" + ":" + port));
        val addresses = NetUtils.getAddresses();
        if (addresses.isEmpty()) {
            // not found valid network interface
            return;
        }


        String s = addresses.stream().map(p -> p.getKey() + " " + p.getValue() + ":" + port)
                .collect(joining("\n"));
        PopOver popOver = new PopOver();
        popOver.setArrowLocation(PopOver.ArrowLocation.TOP_RIGHT);
        Label label = new Label();
        label.setText(s);
        popOver.setContentNode(label);

        listenedAddressLabel.setOnMouseClicked(e -> popOver.show(listenedAddressLabel));
    }

    /**
     * Show message content in right area
     */
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

    /**
     * hide right area
     */
    private void hideContent() {
        httpMessagePane.setVisible(false);
        webSocketMessagePane.setVisible(false);
    }

    @FXML
    private void clearAll(ActionEvent e) {
        messageTree.setRoot(new TreeItem<>(new RTreeItemValue.NodeValue("")));
    }

    @SneakyThrows
    private void addTreeItemMessage(Message message) {
        val root = messageTree.getRoot();
        String host = NetUtils.genericMultiCDNS(message.getHost());


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


    @FXML
    void open(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Bazahe archive data", "*.baza"));
        File file = fileChooser.showOpenDialog(this.root.getScene().getWindow());
        if (file == null) {
            return;
        }

        val root = messageTree.getRoot();
        root.getChildren().clear();
        Task<Void> task = new LoadTask(file.getPath(), this::addTreeItemMessage);
        UIUtils.runBackground(task, "Load data failed!");
    }

    // save captured data to file
    @FXML
    void save(ActionEvent e) throws IOException {

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Bazahe archive data", "*.baza"));
        fileChooser.setInitialFileName("bazahe.baza");
        File file = fileChooser.showSaveDialog(this.root.getScene().getWindow());
        if (file == null) {
            return;
        }
        val root = messageTree.getRoot();
        val saveTask = new SaveTask(file.getPath(), root);
        UIUtils.runBackground(saveTask, "Save data failed!");
    }

}
