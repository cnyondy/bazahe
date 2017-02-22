package bazahe.ui.controller;

import bazahe.Context;
import bazahe.MainSetting;
import bazahe.SecondaryProxy;
import javafx.concurrent.Task;

import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Liu Dong
 */
public class SaveSettingTask extends Task<Void> {

    private Context context;
    private MainSetting mainSetting;
    private SecondaryProxy secondaryProxy;

    public SaveSettingTask(Context context, MainSetting mainSetting, SecondaryProxy secondaryProxy) {
        this.context = context;
        this.mainSetting = mainSetting;
        this.secondaryProxy = secondaryProxy;
    }

    @Override
    protected Void call() throws Exception {
        updateProgress(0, 10);
        // if need to load new key store
        context.setMainSettingAndRefreshSSLContextManager(mainSetting);
        updateProgress(4, 10);
        context.setSecondaryProxyAndRefreshSocketFactory(secondaryProxy);
        updateProgress(5, 10);
        updateMessage("Save mainSetting to file");
        Path configPath = MainSetting.configPath();
        try (OutputStream os = Files.newOutputStream(configPath);
             BufferedOutputStream bos = new BufferedOutputStream(os);
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(mainSetting);
            oos.writeObject(secondaryProxy);
        }
        updateProgress(10, 10);
        return null;
    }
}
