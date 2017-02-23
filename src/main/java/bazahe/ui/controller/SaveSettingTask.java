package bazahe.ui.controller;

import bazahe.Context;
import bazahe.KeyStoreSetting;
import bazahe.MainSetting;
import bazahe.SecondaryProxySetting;
import javafx.concurrent.Task;

import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * @author Liu Dong
 */
public class SaveSettingTask extends Task<Void> {

    private Context context;
    private MainSetting mainSetting;
    private KeyStoreSetting keyStoreSetting;
    private SecondaryProxySetting secondaryProxySetting;

    public SaveSettingTask(Context context, MainSetting mainSetting,
                           KeyStoreSetting keyStoreSetting,
                           SecondaryProxySetting secondaryProxySetting) {
        this.context = context;
        this.mainSetting = requireNonNull(mainSetting);
        this.keyStoreSetting = requireNonNull(keyStoreSetting);
        this.secondaryProxySetting = requireNonNull(secondaryProxySetting);
    }

    @Override
    protected Void call() throws Exception {
        updateProgress(0, 10);
        // if need to load new key store
        context.setMainSetting(mainSetting);
        updateProgress(1, 10);
        context.setKeyStoreSetting(keyStoreSetting);
        updateProgress(5, 10);
        context.setSecondaryProxySetting(secondaryProxySetting);
        updateProgress(7, 10);
        updateMessage("Save mainSetting to file");
        Path configPath = MainSetting.configPath();
        try (OutputStream os = Files.newOutputStream(configPath);
             BufferedOutputStream bos = new BufferedOutputStream(os);
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(mainSetting);
            oos.writeObject(keyStoreSetting);
            oos.writeObject(secondaryProxySetting);
        }
        updateProgress(10, 10);
        return null;
    }
}
