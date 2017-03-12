package bazahe.ui.controller;

import bazahe.Context;
import bazahe.ShutdownHooks;
import bazahe.httpparse.HttpMessage;
import bazahe.httpparse.Message;
import bazahe.httpparse.WebSocketMessage;
import bazahe.httpproxy.AppKeyStoreGenerator;
import bazahe.httpproxy.ProxyServer;
import bazahe.ui.UIMessageListener;
import bazahe.ui.UIUtils;
import bazahe.ui.component.*;
import bazahe.utils.NetWorkUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.controlsfx.control.PopOver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;

import static java.util.stream.Collectors.joining;

/**
 * @author Liu Dong
 */
@Log4j2
public class MainController {

    @FXML
    private CatalogPane catalogPane;
    private CatalogController catalogController;
    @FXML
    private SplitMenuButton setKeyStoreButton;
    @FXML
    private SplitMenuButton saveFileButton;
    @FXML
    private MyButton openFileButton;
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
            proxyServer.setMessageListener(new UIMessageListener(catalogController::addTreeItemMessage));
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
                listenedAddressLabel.setOnMouseClicked(event -> {
                });
            });
        }).start();
    }

    @FXML
    @SneakyThrows
    void initialize() {
        ShutdownHooks.registerTask(() -> {
            if (proxyServer != null) {
                proxyServer.stop();
            }
        });

        catalogController = catalogPane.getController();
        catalogController.setListener(message -> {
            if (message == null) {
                hideContent();
            } else {
                showMessage(message);
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
        dialog.mainSettingProperty().setValue(context.getMainSetting());
        val newConfig = dialog.showAndWait();
        if (newConfig.isPresent()) {
            val task = new SaveSettingTask(context, newConfig.get(), context.getKeyStoreSetting(),
                    context.getProxySetting());
            UIUtils.runBackground(task, "save settings failed");
        }
    }

    @FXML
    void setKeyStore(ActionEvent e) {
        val dialog = new KeyStoreSettingDialog();
        dialog.keyStoreSettingProperty().setValue(context.getKeyStoreSetting());
        val newConfig = dialog.showAndWait();
        if (newConfig.isPresent()) {
            val task = new SaveSettingTask(context, context.getMainSetting(), newConfig.get(),
                    context.getProxySetting());
            UIUtils.runBackground(task, "save key store failed");
        }
    }

    @FXML
    void setProxy(ActionEvent e) {
        ProxySettingDialog dialog = new ProxySettingDialog();
        dialog.proxySettingProperty().setValue(context.getProxySetting());
        val newConfig = dialog.showAndWait();
        if (newConfig.isPresent()) {
            val task = new SaveSettingTask(context, context.getMainSetting(), context.getKeyStoreSetting(),
                    newConfig.get());
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
        val addresses = NetWorkUtils.getAddresses();
        if (addresses.isEmpty()) {
            // not found valid network interface
            return;
        }


        String s = addresses.stream().map(p -> p.getName() + " " + p.getIp() + ":" + port)
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
        catalogController.clearAll();
    }

    @FXML
    void open(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        File dir = new File(context.getMainSetting().getPath());
        if (dir.exists()) {
            fileChooser.setInitialDirectory(dir);
        }
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Bazahe archive data", "*.baza"));
        File file = fileChooser.showOpenDialog(this.root.getScene().getWindow());
        if (file == null) {
            return;
        }

        catalogController.clearAll();
        Task<Void> task = new LoadTask(file.getPath(), catalogController::addTreeItemMessage);
        UIUtils.runBackground(task, "Load data failed!");
    }

    // save captured data to file
    @FXML
    void save(ActionEvent e) throws IOException {
        FileChooser fileChooser = new FileChooser();
        File dir = new File(context.getMainSetting().getPath());
        if (dir.exists()) {
            fileChooser.setInitialDirectory(dir);
        }
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Bazahe archive data", "*.baza"));
        fileChooser.setInitialFileName("bazahe.baza");
        File file = fileChooser.showSaveDialog(this.root.getScene().getWindow());
        if (file == null) {
            return;
        }
        Collection<Message> messages = catalogController.getMessages();
        val saveTask = new SaveTrafficDataTask(file.getPath(), messages);
        UIUtils.runBackground(saveTask, "Save data failed!");
    }


    @FXML
    @SneakyThrows
    void exportPem(ActionEvent e) {
        AppKeyStoreGenerator appKeyStoreGenerator = Context.getInstance().getSslContextManager()
                .getAppKeyStoreGenerator();
        byte[] data = appKeyStoreGenerator.exportCACertificate(true);
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pem file", "*.pem"));
        fileChooser.setInitialFileName("bazahe.pem");
        File file = fileChooser.showSaveDialog(this.root.getScene().getWindow());
        if (file == null) {
            return;
        }
        Files.write(file.toPath(), data);
    }

    @FXML
    @SneakyThrows
    void exportCrt(ActionEvent e) {
        AppKeyStoreGenerator appKeyStoreGenerator = Context.getInstance().getSslContextManager()
                .getAppKeyStoreGenerator();
        byte[] data = appKeyStoreGenerator.exportCACertificate(false);
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Crt file", "*.crt"));
        fileChooser.setInitialFileName("bazahe.crt");
        File file = fileChooser.showSaveDialog(this.root.getScene().getWindow());
        if (file == null) {
            return;
        }
        Files.write(file.toPath(), data);
    }

    @FXML
    @SneakyThrows
    public void saveResources(ActionEvent actionEvent) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        File dir = new File(context.getMainSetting().getPath());
        if (dir.exists()) {
            dirChooser.setInitialDirectory(dir);
        }
        File file = dirChooser.showDialog(this.root.getScene().getWindow());
        if (file == null) {
            return;
        }
        Collection<Message> messages = catalogController.getMessages();
        val saveTask = new SaveTrafficResourceTask(file.getPath(), messages);
        UIUtils.runBackground(saveTask, "Save resource failed!");
    }
}
