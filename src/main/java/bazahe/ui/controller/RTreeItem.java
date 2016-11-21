package bazahe.ui.controller;

import bazahe.def.HttpMessage;
import lombok.Getter;

/**
 * @author Liu Dong
 */
abstract class RTreeItem {

    static class Leaf extends RTreeItem {

        @Getter
        private final HttpMessage message;

        Leaf(HttpMessage message) {
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

        public void increaseChinldren() {
            count++;
        }
    }

}
