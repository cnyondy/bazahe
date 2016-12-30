package bazahe.ui.controller;

import bazahe.def.Context;
import bazahe.def.ProxyConfig;
import bazahe.httpproxy.SSLContextManager;
import javafx.concurrent.Task;
import net.dongliu.commons.Marshaller;

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
        byte[] data = Marshaller.marshal(config);
        Path configPath = ProxyConfig.configPath();
        Files.write(configPath, data);

        updateProgress(10, 10);
        return null;
    }
}
