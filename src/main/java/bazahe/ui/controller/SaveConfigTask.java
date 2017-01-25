package bazahe.ui.controller;

import bazahe.Context;
import bazahe.ProxyConfig;
import bazahe.httpproxy.SSLContextManager;
import javafx.concurrent.Task;

import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Liu Dong
 */
public class SaveConfigTask extends Task<Void> {

    private ProxyConfig config;
    private Context context;

    public SaveConfigTask(ProxyConfig config, Context context) {
        this.config = config;
        this.context = context;
    }

    @Override
    protected Void call() throws Exception {
        updateProgress(0, 10);
        // if need to load new key store
        Path path = Paths.get(config.usedKeyStore());
        SSLContextManager sslContextManager = context.getSslContextManager();
        Path oldPath = Paths.get(sslContextManager.getKeyStorePath());
        if (!Files.isSameFile(path, oldPath)) {
            updateMessage("Load new key store file");
            SSLContextManager newSSLContextManager = new SSLContextManager(config.usedKeyStore(),
                    config.getKeyStorePassword());
            context.setSslContextManager(newSSLContextManager);
        }

        updateProgress(4, 10);
        context.setConfig(config);
        updateMessage("Save config to file");
        Path configPath = ProxyConfig.configPath();
        try (OutputStream os = Files.newOutputStream(configPath);
             BufferedOutputStream bos = new BufferedOutputStream(os);
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(config);
        }
        updateProgress(10, 10);
        return null;
    }
}
