package bazahe.ui.controller;

import bazahe.def.Context;
import bazahe.def.ProxyConfig;
import bazahe.httpproxy.CAKeyStoreGenerator;
import bazahe.httpproxy.SSLContextManager;
import bazahe.ui.Constants;
import bazahe.ui.UIUtils;
import javafx.concurrent.Task;
import lombok.extern.log4j.Log4j2;
import net.dongliu.commons.Marshaller;

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
        // load config
        updateMessage("Loading config file...");
        Path configPath = ProxyConfig.configPath();
        updateProgress(1, 10);
        ProxyConfig config;
        if (Files.exists(configPath)) {
            config = (ProxyConfig) Marshaller.unmarshal(Files.readAllBytes(configPath));
        } else {
            config = ProxyConfig.getDefault();
        }
        context.setConfig(config);
        updateProgress(3, 10);

        updateMessage("Loading key store file...");
        Path keyStorePath = Paths.get(config.usedKeyStore());
        char[] keyStorePassword = config.usedPassword();
        if (!Files.exists(keyStorePath)) {
            if (!config.isUseCustomKeyStore()) {
                logger.info("Generate new key store file");
                updateMessage("Generating new key store...");
                // generate one new key store
                CAKeyStoreGenerator generator = new CAKeyStoreGenerator();
                generator.generate(keyStorePassword, Constants.rootCertificateValidates);
                byte[] keyStoreData = generator.getKeyStoreData();
                Files.write(keyStorePath, keyStoreData);
                updateProgress(8, 10);
            } else {
                UIUtils.showMessageDialog("KeyStore file not found");
                //TODO: How to deal with this?
            }

        } else {
            updateProgress(5, 10);
        }

        updateMessage("Loading key store file...");
        SSLContextManager sslContextManager = new SSLContextManager(keyStorePath.toString(), keyStorePassword);
        updateProgress(10, 10);
        context.setSslContextManager(sslContextManager);
        return null;
    }
}
