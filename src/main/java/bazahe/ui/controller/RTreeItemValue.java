package bazahe.ui.controller;

import bazahe.httpparse.Message;
import lombok.Getter;

/**
 * @author Liu Dong
 */
abstract class RTreeItemValue {

    static class LeafValue extends RTreeItemValue {

        @Getter
        private final Message message;

        LeafValue(Message message) {
            this.message = message;
        }
    }

    static class NodeValue extends RTreeItemValue {
        @Getter
        private final String pattern;
        @Getter
        private int count;

        NodeValue(String pattern) {
            this.pattern = pattern;
        }

        public void increaseChildren() {
            count++;
        }
    }

}
