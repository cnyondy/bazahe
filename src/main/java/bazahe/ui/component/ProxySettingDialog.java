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
package bazahe.ui.component;

import bazahe.setting.ProxySetting;
import bazahe.ui.controller.ProxySettingController;
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
 * Show second proxy setting.
 */
public class ProxySettingDialog extends Dialog<ProxySetting> {

    private final ObjectProperty<ProxySetting> proxySetting = new SimpleObjectProperty<>();

    @SneakyThrows
    public ProxySettingDialog() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/proxy_setting.fxml"));
        GridPane gridPane = loader.load();
        ProxySettingController controller = loader.getController();

        getDialogPane().setContent(gridPane);
        setTitle("Proxy Setting");
        final DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setContent(gridPane);

        setResultConverter((dialogButton) -> {
            ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonData.OK_DONE ? controller.getModel() : null;
        });

        proxySetting.addListener((o, old, n) -> controller.setModel(n));

    }

    public ProxySetting getProxySetting() {
        return proxySetting.get();
    }

    public ObjectProperty<ProxySetting> proxySettingProperty() {
        return proxySetting;
    }

}
