package bazahe.ui.controller;

import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import lombok.val;

/**
 * @author Liu Dong
 */
class TreeViewMouseHandler implements EventHandler<MouseEvent> {

    @Override
    @SuppressWarnings("unchecked")
    public void handle(MouseEvent event) {
        if (!event.getButton().equals(MouseButton.SECONDARY)) {
            return;
        }

        TreeView<RTreeItemValue> treeView = (TreeView<RTreeItemValue>) event.getSource();
        TreeItem<RTreeItemValue> treeItem = treeView.getSelectionModel().getSelectedItem();
        RTreeItemValue itemValue = treeItem.getValue();

        ContextMenu contextMenu = new ContextMenu();

        if (itemValue instanceof RTreeItemValue.LeafValue) {
            val leaf = (RTreeItemValue.LeafValue) itemValue;
            MenuItem copyMenu = new MenuItem("Copy URL");
            copyMenu.setOnAction(event1 -> {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(leaf.getMessage().getUrl());
                clipboard.setContent(content);
            });
            contextMenu.getItems().add(copyMenu);
        }

        MenuItem deleteMenu = new MenuItem("Delete");
        deleteMenu.setOnAction(event1 -> {
            TreeItem<RTreeItemValue> parent = treeItem.getParent();
            parent.getChildren().remove(treeItem);
        });
        contextMenu.getItems().add(deleteMenu);


        contextMenu.show(treeView, event.getScreenX(), event.getScreenY());
    }
}
