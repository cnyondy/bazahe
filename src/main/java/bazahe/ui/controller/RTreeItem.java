package bazahe.ui.controller;

import bazahe.def.Message;
import lombok.Getter;

/**
 * @author Liu Dong
 */
abstract class RTreeItem {

    static class Leaf extends RTreeItem {

        @Getter
        private final Message message;

        Leaf(Message message) {
            this.message = message;
        }
    }

    static class Node extends RTreeItem {
        @Getter
        private final String pattern;
        @Getter
        private int count;

        Node(String pattern) {
            this.pattern = pattern;
        }

        public void increaseChildren() {
            count++;
        }
    }

}
