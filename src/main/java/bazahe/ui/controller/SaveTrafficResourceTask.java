package bazahe.ui.controller;

import bazahe.httpparse.HttpMessage;
import bazahe.httpparse.Message;
import bazahe.store.BodyStore;
import bazahe.store.BodyStoreType;
import bazahe.ui.formater.FormURLEncodedFormatter;
import bazahe.ui.formater.JSONFormatter;
import bazahe.ui.formater.XMLFormatter;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import javafx.concurrent.Task;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;

import java.io.*;
import java.util.Collection;
import java.util.function.Function;

/**
 * Task for saving response resource
 *
 * @author Yondy
 */
@Log4j2
public class SaveTrafficResourceTask extends Task<Void> {
    private String path;
    private Collection<Message> messages;

    public SaveTrafficResourceTask(String path, Collection<Message> messages) {
        this.path = path;
        this.messages = messages;
    }

    @Override
    protected Void call() throws Exception {
        updateMessage("calculate data count...");
        int total = messages.size();
        updateProgress(1, total + 1);

        updateMessage("save data...");
        int writed = 0;

        for (Message message : messages) {
            saveResource(path, message);
            writed++;
            updateProgress(writed + 1, total + 1);
        }
        updateMessage("done");

        return null;
    }

    @SneakyThrows
    public static void saveResource(String path, Message message) {
        if (!(message instanceof HttpMessage)) {
            return;
        }

        logger.info(message.getUrl());

        val httpMessage = (HttpMessage) message;
        val filePath = getFilePath(path, httpMessage.getUrl());

        BodyStore bodyStore = httpMessage.getResponseBody();
        if (bodyStore == null || bodyStore.getSize() == 0) {
            logger.info("This http message has nobody");
            return;
        }

        File file = new File(filePath);
        file.getParentFile().mkdirs();

        try (InputStream in = bodyStore.getInputStream();
             OutputStream out = new FileOutputStream(file)) {
            if (bodyStore.getType() == BodyStoreType.json) {
                StringBuilder sb = new StringBuilder();
                sb.append(httpMessage.getRequestHeaders().getRequestLine().getMethod()).append(" ");
                sb.append(httpMessage.getUrl()).append("\r\n-\r\n");

                String text;
                if (httpMessage.getRequestBody() != null && httpMessage.getRequestBody().getSize() > 0) {
                    text = getContentString(httpMessage.getRequestBody());
                    sb.append(text);
                }
                sb.append("\r\n-\r\n");
                text = getContentString(bodyStore);
                sb.append(text).append("\r\n");

                val resStream = new ByteArrayInputStream(sb.toString().getBytes());
                ByteStreams.copy(resStream, out);
            } else {
                ByteStreams.copy(in, out);
            }
        }

    }

    private static String getContentString(BodyStore bodyStore) throws IOException {
        String text;
        try (Reader reader = new InputStreamReader(bodyStore.getInputStream(), bodyStore.getCharset())) {
            text = CharStreams.toString(reader);
            text = format(bodyStore, text);
        }

        return text;
    }

    private static String format(BodyStore bodyStore, String text) {
        Function<String, String> formatter = s -> s;
        BodyStoreType storeType = bodyStore.getType();
        if (storeType == BodyStoreType.json) {
            if (text.startsWith("{") && text.endsWith("}")) {
                formatter = new JSONFormatter(4);
            }
        } else if (storeType == BodyStoreType.xml) {
            formatter = new XMLFormatter(4);
        } else if (storeType == BodyStoreType.www_form) {
            formatter = new FormURLEncodedFormatter(bodyStore.getCharset());
        }

        return formatter.apply(text);
    }

    private static String getFilePath(String path, String url) {
        String s = url.replaceAll("^(http|https)://", "").replaceAll("\\?.+$", "").replace(":", "@");
        s = path + File.separatorChar + s;
        if (s.endsWith("/")) {
            s += "index.html";
        }

        return s;
    }

}
