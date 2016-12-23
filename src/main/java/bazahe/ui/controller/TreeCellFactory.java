package bazahe.ui.controller;

import bazahe.def.Message;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.util.Callback;

/**
 * @author Liu Dong
 */
class TreeCellFactory implements Callback<TreeView<RTreeItemValue>, TreeCell<RTreeItemValue>> {

    @Override
    public TreeCell<RTreeItemValue> call(TreeView<RTreeItemValue> treeView) {
        TreeCell<RTreeItemValue> treeCell = new TreeCell<RTreeItemValue>() {
            @Override
            protected void updateItem(RTreeItemValue item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                } else {
                    String text;
                    if (item instanceof RTreeItemValue.NodeValue) {
                        text = ((RTreeItemValue.NodeValue) item).getPattern() + "(" + ((RTreeItemValue.NodeValue) 
                                item).getCount() + ")";
                    } else if (item instanceof RTreeItemValue.LeafValue) {
                        Message message = ((RTreeItemValue.LeafValue) item).getMessage();
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
