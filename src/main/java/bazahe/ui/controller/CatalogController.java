package bazahe.ui.controller;

import bazahe.httpparse.Message;
import bazahe.ui.component.CatalogPane;
import bazahe.utils.NetWorkUtils;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.val;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Liu Dong
 */
public class CatalogController {
    @FXML
    private StackPane stackPane;
    @FXML
    private ListView<Message> messageList;
    @FXML
    private TreeView<RTreeItemValue> messageTree;
    @FXML
    private ToggleGroup viewTypeGroup;

    @FXML
    private CatalogPane root;
    private Collection<Message> messages;

    @Setter
    private Consumer<Message> listener;

    @FXML
    void initialize() {
        messageList.setCellFactory(listView -> new ListCell<Message>() {
            @Override
            public void updateItem(Message item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(item.getDisplay());
                }
            }
        });
        messageList.getSelectionModel().selectedItemProperty().addListener((ov, o, n) -> listener.accept(n));

        val root = new TreeItem<RTreeItemValue>(new RTreeItemValue.NodeValue(""));
        root.setExpanded(true);
        messageTree.setRoot(root);
        messageTree.setShowRoot(false);
        messageTree.setCellFactory(new TreeCellFactory());
        messageTree.setOnMouseClicked(new TreeViewMouseHandler());
        messageTree.getSelectionModel().selectedItemProperty().addListener((ov, o, n) -> {
            if (n == null || n.getValue() instanceof RTreeItemValue.NodeValue) {
                listener.accept(null);
            } else {
                RTreeItemValue.LeafValue value = (RTreeItemValue.LeafValue) n.getValue();
                listener.accept(value.getMessage());
            }
        });


        viewTypeGroup.selectedToggleProperty().addListener((ov, o, n) -> {
            String type = (String) n.getUserData();
            if (type.equals("list")) {
                stackPane.getChildren().remove(messageList);
                stackPane.getChildren().add(messageList);
            } else if (type.equals("tree")) {
                stackPane.getChildren().remove(messageTree);
                stackPane.getChildren().add(messageTree);
            }
        });
    }


    void clearAll() {
        messageList.getItems().clear();
        messageTree.setRoot(new TreeItem<>(new RTreeItemValue.NodeValue("")));
    }

    @SneakyThrows
    void addTreeItemMessage(Message message) {
        messageList.getItems().add(message);
        val root = messageTree.getRoot();
        String host = NetWorkUtils.genericMultiCDNS(message.getHost());


        for (val item : root.getChildren()) {
            val node = (RTreeItemValue.NodeValue) item.getValue();
            if (node.getPattern().equals(host)) {
                item.getChildren().add(new TreeItem<>(new RTreeItemValue.LeafValue(message)));
                node.increaseChildren();
                return;
            }
        }

        val node = new RTreeItemValue.NodeValue(host);
        val nodeItem = new TreeItem<RTreeItemValue>(node);
        root.getChildren().add(nodeItem);
        nodeItem.getChildren().add(new TreeItem<>(new RTreeItemValue.LeafValue(message)));
        node.increaseChildren();
    }

    public Collection<Message> getMessages() {
        ObservableList<Message> items = messageList.getItems();
        return new ArrayList<>(items);
    }

    private static class TreeCellFactory implements Callback<TreeView<RTreeItemValue>, TreeCell<RTreeItemValue>> {

        @Override
        public TreeCell<RTreeItemValue> call(TreeView<RTreeItemValue> treeView) {
            return new TreeCell<RTreeItemValue>() {
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
        }
    }


    private class TreeViewMouseHandler implements EventHandler<MouseEvent> {
        @Override
        @SuppressWarnings("unchecked")
        public void handle(MouseEvent event) {
            if (!event.getButton().equals(MouseButton.SECONDARY)) {
                return;
            }

            TreeItem<RTreeItemValue> treeItem = messageTree.getSelectionModel().getSelectedItem();
            if (treeItem == null) {
                return;
            }
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
                // also remove from list view
                RTreeItemValue value = treeItem.getValue();
                List<Message> removed = new ArrayList<>();
                if (value instanceof RTreeItemValue.LeafValue) {
                    Message message = ((RTreeItemValue.LeafValue) value).getMessage();
                    removed.add(message);
                } else {
                    for (TreeItem<RTreeItemValue> child : treeItem.getChildren()) {
                        Message message = ((RTreeItemValue.LeafValue) child.getValue()).getMessage();
                        removed.add(message);
                    }
                }
                messageList.getItems().removeAll(removed);

            });
            contextMenu.getItems().add(deleteMenu);


            contextMenu.show(root, event.getScreenX(), event.getScreenY());
        }
    }
}


