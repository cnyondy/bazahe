package bazahe.ui.controller;

import bazahe.def.Message;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.util.Callback;

/**
 * @author Liu Dong
 */
class TreeCellFactory implements Callback<TreeView<RTreeItem>, TreeCell<RTreeItem>> {

    @Override
    public TreeCell<RTreeItem> call(TreeView<RTreeItem> treeView) {
        TreeCell<RTreeItem> treeCell = new TreeCell<RTreeItem>() {
            @Override
            protected void updateItem(RTreeItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                } else {
                    String text;
                    if (item instanceof RTreeItem.Node) {
                        text = ((RTreeItem.Node) item).getPattern() + "(" + ((RTreeItem.Node) item).getCount() + ")";
                    } else if (item instanceof RTreeItem.Leaf) {
                        Message message = ((RTreeItem.Leaf) item).getMessage();
                        text = message.getDisplay();
                    } else {
                        text = "BUG..";
                    }
                    setText(text);
                }
            }
        };
        return treeCell;
    }
}
