package bazahe.ui.controller;

import bazahe.httpparse.Message;
import javafx.concurrent.Task;
import lombok.Cleanup;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Collection;

/**
 * Task for saving data
 *
 * @author Liu Dong
 */
public class SaveTrafficDataTask extends Task<Void> {
    private String path;
    private Collection<Message> messages;

    public SaveTrafficDataTask(String path, Collection<Message> messages) {
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
        @Cleanup OutputStream out = new BufferedOutputStream(new FileOutputStream(path));
        @Cleanup ObjectOutputStream oos = new ObjectOutputStream(out);
        oos.writeInt(0xbaaebaae); // magic num
        oos.writeByte(1); // major version
        oos.writeByte(0); // minor version
        oos.writeInt(total); // value count
        for (Message message : messages) {
            oos.writeObject(message);
            oos.flush();
            writed++;
            updateProgress(writed + 1, total + 1);
        }
        updateMessage("done");

        return null;
    }
}
