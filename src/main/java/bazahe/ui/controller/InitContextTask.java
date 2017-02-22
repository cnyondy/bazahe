package bazahe.ui.controller;

import bazahe.Constants;
import bazahe.Context;
import bazahe.MainSetting;
import bazahe.SecondaryProxy;
import bazahe.httpproxy.CAKeyStoreGenerator;
import bazahe.ui.UIUtils;
import javafx.concurrent.Task;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * When start bazehe, run this task
 *
 * @author Liu Dong
 */
@Log4j2
public class InitContextTask extends Task<Void> {
    private Context context;

    public InitContextTask(Context context) {
        this.context = context;
    }

    @Override
    public Void call() throws Exception {
        // load mainSetting
        updateMessage("Loading mainSetting file...");
        Path configPath = MainSetting.configPath();
        updateProgress(1, 10);
        MainSetting mainSetting;
        SecondaryProxy secondaryProxy;
        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath);
                 BufferedInputStream bin = new BufferedInputStream(in);
                 ObjectInputStream oin = new ObjectInputStream(bin)) {
                mainSetting = (MainSetting) oin.readObject();
                secondaryProxy = (SecondaryProxy) oin.readObject();
            }
        } else {
            mainSetting = MainSetting.getDefault();
            secondaryProxy = new SecondaryProxy();
        }
        updateProgress(3, 10);

        updateMessage("Loading key store file...");
        Path keyStorePath = Paths.get(mainSetting.usedKeyStore());
        char[] keyStorePassword = mainSetting.usedPassword();
        if (!Files.exists(keyStorePath)) {
            if (!mainSetting.isUseCustomKeyStore()) {
                logger.info("Generate new key store file");
                updateMessage("Generating new key store...");
                // generate one new key store
                CAKeyStoreGenerator generator = new CAKeyStoreGenerator();
                generator.generate(keyStorePassword, Constants.rootCertificateValidates);
                byte[] keyStoreData = generator.getKeyStoreData();
                Files.write(keyStorePath, keyStoreData);
            } else {
                UIUtils.showMessageDialog("KeyStore file not found");
                //TODO: How to deal with this?
            }

        }
        context.setMainSettingAndRefreshSSLContextManager(mainSetting);
        updateProgress(8, 10);
        context.setSecondaryProxyAndRefreshSocketFactory(secondaryProxy);
        updateProgress(10, 10);
        return null;
    }
}
