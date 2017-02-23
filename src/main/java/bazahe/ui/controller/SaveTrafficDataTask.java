package bazahe.ui.controller;

import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;
import lombok.Cleanup;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Task for saving data
 *
 * @author Liu Dong
 */
public class SaveTrafficDataTask extends Task<Void> {
    private String path;
    private TreeItem<RTreeItemValue> root;

    public SaveTrafficDataTask(String path, TreeItem<RTreeItemValue> root) {
        this.path = path;
        this.root = root;
    }

    @Override
    protected Void call() throws Exception {
        updateMessage("calculate data count...");
        int total = 0;
        for (TreeItem<RTreeItemValue> domainItem : root.getChildren()) {
            for (TreeItem<RTreeItemValue> item : domainItem.getChildren()) {
                RTreeItemValue itemValue = item.getValue();
                if (itemValue instanceof RTreeItemValue.LeafValue) {
                    total++;
                }
            }
        }
        updateProgress(1, total + 1);

        updateMessage("save data...");
        int writed = 0;
        @Cleanup OutputStream out = new BufferedOutputStream(new FileOutputStream(path));
        @Cleanup ObjectOutputStream oos = new ObjectOutputStream(out);
        oos.writeInt(0xbaaebaae); // magic num
        oos.writeByte(1); // major version
        oos.writeByte(0); // minor version
        oos.writeInt(total); // value count
        for (TreeItem<RTreeItemValue> domainItem : root.getChildren()) {
            for (TreeItem<RTreeItemValue> item : domainItem.getChildren()) {
                RTreeItemValue itemValue = item.getValue();
                if (itemValue instanceof RTreeItemValue.LeafValue) {
                    RTreeItemValue.LeafValue value = (RTreeItemValue.LeafValue) itemValue;
                    oos.writeObject(value.getMessage());
                    oos.flush();
                    writed++;
                    updateProgress(writed + 1, total + 1);
                }
            }
        }
        updateMessage("done");

        return null;
    }
}
