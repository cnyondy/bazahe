package bazahe.ui;

import bazahe.httpparse.HttpMessage;
import bazahe.httpparse.Message;
import bazahe.httpparse.WebSocketMessage;
import bazahe.httpparse.RequestHeaders;
import bazahe.httpparse.ResponseHeaders;
import bazahe.httpproxy.MessageListener;
import bazahe.store.BodyStore;
import bazahe.store.BodyStoreType;
import javafx.application.Platform;
import lombok.extern.log4j.Log4j2;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * Listener to send message to ui
 *
 * @author Liu Dong
 */
@Log4j2
public class UIMessageListener implements MessageListener {
    private final Consumer<Message> consumer;
    private final ConcurrentMap<String, HttpMessage> httpMap;

    public UIMessageListener(Consumer<Message> consumer) {
        this.consumer = consumer;
        this.httpMap = new ConcurrentHashMap<>();
    }

    @Override
    public OutputStream onHttpRequest(String messageId, String host, String url, RequestHeaders requestHeaders) {
        BodyStore bodyStore = BodyStore.of(requestHeaders.contentType(), requestHeaders.contentEncoding());
        HttpMessage item = new HttpMessage(messageId, host, url, requestHeaders, bodyStore);
        item.setRequestTime(System.currentTimeMillis());
        this.httpMap.put(messageId, item);
        Platform.runLater(() -> consumer.accept(item));
        return bodyStore;
    }

    @Override
    public OutputStream onHttpResponse(String messageId, ResponseHeaders responseHeaders) {
        HttpMessage item = this.httpMap.get(messageId);
        if (item == null) {
            logger.error("Cannot found request item for id: {}", messageId);
            return null;
        }
        item.setResponseTime(System.currentTimeMillis());
        item.setResponseHeaders(responseHeaders);
        BodyStore bodyStore = BodyStore.of(responseHeaders.contentType(), responseHeaders.contentEncoding());
        item.setResponseBody(bodyStore);
        return bodyStore;
    }

    @Override
    public OutputStream onWebSocket(String messageId, String host, String url, int type, boolean request) {
        // TODO: currently the hacker way to use bodyStore
        BodyStore bodyStore = new BodyStore(type == 1 ? BodyStoreType.text : BodyStoreType.binary,
                StandardCharsets.UTF_8, null);
        WebSocketMessage message = new WebSocketMessage(messageId, host, url, type, request);
        message.setBodyStore(bodyStore);
        Platform.runLater(() -> consumer.accept(message));
        return bodyStore;
    }
}
