package bazahe.ui;

import bazahe.httpparse.RequestHeaders;
import bazahe.httpparse.ResponseHeaders;
import bazahe.httpproxy.HttpMessageListener;
import bazahe.store.HttpBodyStore;
import javafx.application.Platform;
import lombok.extern.log4j.Log4j2;

import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * Listener to send message to ui
 *
 * @author Liu Dong
 */
@Log4j2
public class UIHttpMessageListener implements HttpMessageListener {
    private final Consumer<HttpMessage> consumer;
    private final ConcurrentMap<String, HttpMessage> map;

    public UIHttpMessageListener(Consumer<HttpMessage> consumer) {
        this.consumer = consumer;
        this.map = new ConcurrentHashMap<>();
    }

    @Override
    public OutputStream onRequest(String id, RequestHeaders requestHeaders) {
        HttpMessage item = new HttpMessage();
        item.setId(id);
        this.map.put(id, item);
        item.setRequestHeaders(requestHeaders);
        HttpBodyStore bodyStore = new HttpBodyStore(requestHeaders.contentType(), requestHeaders.contentEncoding());
        item.setRequestBody(bodyStore);
        Platform.runLater(() -> consumer.accept(item));
        return bodyStore;
    }

    @Override
    public OutputStream onResponse(String id, ResponseHeaders responseHeaders) {
        HttpMessage item = this.map.get(id);
        if (item == null) {
            log.error("Cannot found request item for id: {]", id);
            return null;
        }
        item.setResponseHeaders(responseHeaders);
        HttpBodyStore bodyStore = new HttpBodyStore(responseHeaders.contentType(), responseHeaders.contentEncoding());
        item.setResponseBody(bodyStore);
        return bodyStore;
    }
}
