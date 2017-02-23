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

import bazahe.SecondaryProxySetting;
import bazahe.ui.controller.SecondaryProxyController;
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
public class SecondaryProxyDialog extends Dialog<SecondaryProxySetting> {

    private final ObjectProperty<SecondaryProxySetting> secondaryProxy = new SimpleObjectProperty<>();

    @SneakyThrows
    public SecondaryProxyDialog() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/bazahe/secondary_proxy_setting.fxml"));
        GridPane gridPane = loader.load();
        SecondaryProxyController controller = loader.getController();

        getDialogPane().setContent(gridPane);
        setTitle("Proxy Setting");
        final DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setContent(gridPane);

        setResultConverter((dialogButton) -> {
            ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonData.OK_DONE ? controller.getModel() : null;
        });

        secondaryProxy.addListener((o, old, n) -> controller.setModel(n));

    }

    public SecondaryProxySetting getSecondaryProxy() {
        return secondaryProxy.get();
    }

    public ObjectProperty<SecondaryProxySetting> secondaryProxyProperty() {
        return secondaryProxy;
    }

}
