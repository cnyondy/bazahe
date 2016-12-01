package bazahe.ui.controller;

import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
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
        
        TreeView<RTreeItem> treeView = (TreeView<RTreeItem>) event.getSource();
        RTreeItem treeItem = treeView.getSelectionModel().getSelectedItem().getValue();
        if (!(treeItem instanceof RTreeItem.Leaf)) {
            return;
        }

        val leaf = (RTreeItem.Leaf) treeItem;
        ContextMenu contextMenu = new ContextMenu();
        MenuItem menuItem = new MenuItem("Copy URL");
        menuItem.setOnAction(event1 -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(leaf.getMessage().getUrl());
            clipboard.setContent(content);
        });
        
        
        

        contextMenu.getItems().addAll(menuItem);
        contextMenu.show(treeView, event.getScreenX(), event.getScreenY());
    }
}
