package bazahe.ui;

import bazahe.httpproxy.HttpMessageListener;
import bazahe.httpproxy.ProxyServer;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

/**
 * @author Liu Dong
 */
@Log4j2
public class ProxyService extends Service<Void> {
    @Setter
    private String host = "";
    @Setter
    private int port = 1024;
    @Setter
    private HttpMessageListener httpMessageListener;


    @Override
    protected Task<Void> createTask() {

        return new Task<Void>() {
            private volatile ProxyServer proxyServer;

            @Override
            protected Void call() throws IOException {
                proxyServer = new ProxyServer(host, port);
                proxyServer.setHttpMessageListener(httpMessageListener);
                proxyServer.run();
                return null;
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean flag = super.cancel(mayInterruptIfRunning);
                if (!flag) {
                    return false;
                }
                proxyServer.stop();
                return true;
            }

            @Override
            protected void cancelled() {
                super.cancelled();
                log.debug("Proxy task canceled");
            }
        };
    }


}
