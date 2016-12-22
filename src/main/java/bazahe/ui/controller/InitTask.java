package bazahe.ui.controller;

import bazahe.def.ProxyConfig;
import bazahe.httpproxy.CAKeyStoreGenerator;
import bazahe.httpproxy.SSLContextManager;
import bazahe.ui.Constants;
import javafx.concurrent.Task;
import lombok.extern.log4j.Log4j2;
import net.dongliu.commons.Marshaller;
import net.dongliu.commons.collection.Pair;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * When start bazehe, run this task
 *
 * @author Liu Dong
 */
@Log4j2
public class InitTask extends Task<Pair<ProxyConfig, SSLContextManager>> {

    @Override
    public Pair<ProxyConfig, SSLContextManager> call() throws Exception {
        // load config
        updateMessage("Loading config file...");
        Path configPath = ProxyConfig.getConfigPath();
        updateProgress(1, 10);
        ProxyConfig config;
        if (Files.exists(configPath)) {
            config = (ProxyConfig) Marshaller.unmarshal(Files.readAllBytes(configPath));
        } else {
            config = ProxyConfig.getDefault();
        }
        updateProgress(3, 10);

        updateMessage("Loading key store file...");
        Path keyStorePath = ProxyConfig.getDefaultKeyStorePath();
        char[] keyStorePassword = Constants.keyStorePassword;
        if (!Files.exists(keyStorePath)) {
            logger.info("Generate new key store file");
            updateMessage("Generating new key store...");
            // generate one key store
            CAKeyStoreGenerator generator = new CAKeyStoreGenerator();
            generator.generate(keyStorePassword, Constants.rootCertificateValidates);
            byte[] keyStoreData = generator.getKeyStoreData();
            Files.write(keyStorePath, keyStoreData);
            updateProgress(8, 10);
        } else {
            updateProgress(5, 10);
        }

        updateMessage("Loading key store file...");
        SSLContextManager sslContextManager = new SSLContextManager(keyStorePath.toString(), keyStorePassword);
        updateProgress(10, 10);
        return Pair.of(config, sslContextManager);
    }
}
