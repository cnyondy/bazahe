package bazahe.ui.controller;

import bazahe.httpparse.Message;
import javafx.application.Platform;
import javafx.concurrent.Task;
import lombok.Cleanup;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.function.Consumer;

/**
 * @author Liu Dong
 */
public class LoadTask extends Task<Void> {
    private String path;
    private Consumer<Message> consumer;

    public LoadTask(String path, Consumer<Message> consumer) {
        this.path = path;
        this.consumer = consumer;
    }

    @Override
    protected Void call() throws Exception {
        updateMessage("loading...");

        @Cleanup InputStream in = new BufferedInputStream(new FileInputStream(path));
        @Cleanup ObjectInputStream ois = new ObjectInputStream(in);
        int magicNum = ois.readInt();
        int majorVersion = ois.readByte();
        int minorVersoin = ois.readByte();
        int total = ois.readInt();
        int readed = 0;
        while (readed < total) {
            Message message = (Message) ois.readObject();
            Platform.runLater(() -> consumer.accept(message));
            readed++;
            updateProgress(readed, total);
        }

        return null;
    }
}
