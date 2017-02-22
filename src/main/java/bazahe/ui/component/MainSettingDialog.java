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

import bazahe.MainSetting;
import bazahe.ui.controller.MainSettingController;
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
public class MainSettingDialog extends Dialog<MainSetting> {

    private final ObjectProperty<MainSetting> proxyConfig = new SimpleObjectProperty<>();

    @SneakyThrows
    public MainSettingDialog() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/bazahe/main_setting.fxml"));
        GridPane gridPane = loader.load();
        MainSettingController controller = loader.getController();

        getDialogPane().setContent(gridPane);
        setTitle("Proxy Setting");
        final DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setContent(gridPane);

        setResultConverter((dialogButton) -> {
            ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonData.OK_DONE ? controller.getModel() : null;
        });

        proxyConfig.addListener((o, old, n) -> controller.setModel(n));

    }

    public MainSetting getProxyConfig() {
        return proxyConfig.get();
    }

    public ObjectProperty<MainSetting> proxyConfigProperty() {
        return proxyConfig;
    }

}
