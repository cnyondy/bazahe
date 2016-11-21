/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package bazahe.ui.pane;

import bazahe.def.ProxyConfig;
import bazahe.ui.controller.ConfigureController;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.GridPane;
import lombok.SneakyThrows;

/**
 * Show proxy configure.
 */
public class ProxyConfigDialog extends Dialog<ProxyConfig> {

    private final ObjectProperty<ProxyConfig> proxyConfig = new SimpleObjectProperty<>();

    @SneakyThrows
    public ProxyConfigDialog() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/bazahe/proxy_config.fxml"));
        GridPane gridPane = loader.load();
        ConfigureController controller = loader.getController();

        getDialogPane().setContent(gridPane);
        setTitle("Proxy Setting");
        final DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setContent(gridPane);

        setResultConverter((dialogButton) -> {
            ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonData.OK_DONE ? getProxyConfig(controller) : null;
        });

        proxyConfig.addListener((o, old, n) -> {
            controller.getHostField().setText(n.getHost());
            controller.getPortFiled().setText("" + n.getPort());
            controller.getTimeoutField().setText("" + n.getTimeout());
            controller.getKeyStoreField().setText(n.getKeyStore());
            controller.getKeyStorePasswordField().setText(new String(n.getKeyStorePassword()));
            controller.getAliasField().setText(n.getAlias());
        });
    }

    private ProxyConfig getProxyConfig(ConfigureController controller) {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setHost(controller.getHostField().getText());
        proxyConfig.setPort(Integer.parseInt(controller.getPortFiled().getText()));
        proxyConfig.setTimeout(Integer.parseInt(controller.getTimeoutField().getText()));
        proxyConfig.setKeyStore(controller.getKeyStoreField().getText());
        proxyConfig.setKeyStorePassword(controller.getKeyStorePasswordField().getText().toCharArray());
        proxyConfig.setAlias(controller.getAliasField().getText());
        return proxyConfig;
    }

    public ObjectProperty<ProxyConfig> proxyConfigProperty() {
        return proxyConfig;
    }
}