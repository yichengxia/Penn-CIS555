package edu.upenn.cis.cis455.xpathengine;

import java.util.ArrayDeque;
import java.util.Deque;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.upenn.cis.stormlite.tuple.Node;

public class XPathEngineImpl implements XPathEngine {

    Logger logger = LogManager.getLogger(XPathEngineImpl.class);

    private Node dummyNode;
    private Node[] nodes;
    private boolean[] valid;

    public Node getDummyNode() {
        return dummyNode;
    }

    public Node[] getNodes() {
        return nodes;
    }

    public boolean[] getValid() {
        return valid;
    }

    @Override
    public void setXPaths(String[] expressions) {
        if (expressions == null || expressions.length == 0) {
            logger.error("Null XPath expression(s) found.");
            return;
        }
        dummyNode = new Node("dummyNode");
        int n = expressions.length;
        nodes = new Node[n];
        valid = new boolean[n];
        for (int i = 0; i < n; i++) {
            Node head = null;
            try {
                head = getHead(expressions[i]);
            } catch (Exception e){
                logger.error("Exception when getting head of expressions.");
            }
            if (head != null && head != dummyNode) {
                nodes[i] = head;
                valid[i] = true;
            }
        }
    }

    private Node getHead(String expression) {
        if (expression == null || expression.length() <= 1 || !expression.startsWith("/")) {
            logger.error("Failed to parse expression: {}.", expression);
            return dummyNode;
        } else {
            expression = expression.substring(1);
        }
        Deque<Character> deque = new ArrayDeque<>();
        int[] pointer = new int[1];
        Node head = getNode(expression, deque, pointer);
        if (head == dummyNode || !deque.isEmpty()) {
            return dummyNode;
        }
        if (expression.length() <= pointer[0]) {
            return head;
        }
        expression = expression.substring(pointer[0]);
        Node next = getHead(expression);
        if (next == dummyNode) {
            return dummyNode;
        } else {
            head.next = next;
            return head;
        }
    }

    private Node getNode(String expression, Deque<Character> deque, int[] pointer) {
        if (!deque.isEmpty()) {
            return dummyNode;
        }
        Node node = null;
        while (expression.length() > pointer[0]) {
            if (node == null && (pointer[0] >= expression.length() || expression.charAt(pointer[0]) == '/' ||
                expression.charAt(pointer[0]) == '[')) {
                node = new Node(new String(expression.toCharArray(), 0, pointer[0]));
            }
            if (pointer[0] >= expression.length() || expression.charAt(pointer[0]) == '/'){
                break;
            }
            if (expression.charAt(pointer[0]) == '[') {
                while (expression.charAt(pointer[0]) == '['){
                    if (node == null) {
                        return dummyNode;
                    }
                    deque.offerFirst('[');
                    pointer[0]++;
                    try {
                        search(node, deque, pointer, expression);
                    } catch (Exception e) {
                        logger.error("Exception when searching nodes.");
                    }
                    if (expression.length() <= pointer[0]) {
                        break;
                    }
                }
                pointer[0]--;
            }
            if (!deque.isEmpty()) {
                return dummyNode;
            }
            pointer[0]++;
        }
        return node;
    }

    private void search(Node node, Deque<Character> deque, int[] pointer, String expression) {
        String next = null;
        int start = pointer[0], code = 0;
        while (pointer[0] < expression.length()){ 
            char c = expression.charAt(pointer[0]);
            if (c == '"') {
                logger.info("Evaluating \"");
                if ((code & 0b01) == 1) {
                    pointer[0]++;
                    next = getNext(pointer, expression);
                    if ((code & 0b10) == 0b10) {
                        if (!deque.isEmpty() && deque.peekFirst() == ',') {
                            deque.pollFirst();
                        }
                    }
                }
            } else if (c == ']') {
                logger.info("Evaluating ]");
                if (!deque.isEmpty() && deque.peekFirst() == '[') {
                    deque.pollFirst();
                    if ((code & 0b10) == 0b10) {
                        node.addConstainsList(next);
                    } else {
                        node.addTextList(next);
                    }
                    pointer[0]++;
                    return;
                }
            } else if (c == ',') {
                logger.info("Evaluating ,");
                if (!((!deque.isEmpty() && deque.peekFirst() != '(') || code != 0b11)) {
                    deque.offerFirst(',');
                }
            } else if (c == '(') {
                logger.info("Evaluating (");
                String str = new String(expression.toCharArray(), start, pointer[0] - start).replace("\\s+", "");
                if ((code & 0b10) == 0b10) {
                    code = str.equals("text") ? (code | 0b01) : code;
                } else {
                    if (str.equals("text")) {
                        code |= 0b01;
                    } else if (str.equals("contains")) {
                        code |= 0b10;
                    }
                    start = pointer[0] + 1;
                }
                deque.offerFirst('(');
            } else if (c == ')') {
                logger.info("Evaluating )");
                if (deque.peekFirst() == '(') {
                    deque.pollFirst();
                }
            }
            pointer[0]++;
        }
    }

    private String getNext(int[] pointer, String expression) {
        int start = pointer[0];
        while (expression.length() > pointer[0]) {
            if (expression.charAt(pointer[0]) == '\\') {
                pointer[0] += 2;
            } else if (expression.charAt(pointer[0]) == '"') {
                return new String(expression.toCharArray(), start, pointer[0] - start);
            } else {
                pointer[0]++;
            }
        }
        return "";
    }

    @Override
    public boolean[] evaluateEvent(OccurrenceEvent event) {
        if (valid == null) {
            return null;
        }
        int n = nodes.length;
        boolean[] eval = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (valid[i]) {
                eval[i] = matchElement(nodes[i], event.getElement());
            }
        }
        return eval;
    }

    private boolean matchElement(Node node, Element element) {
        if (node == null) {
            return true;
        }
        if (element == null) {
            return false;
        }
        logger.info("Matching node: {} with element: {}", node.toString(), element.toString());
        Elements elements = element.children();
        String nodename = node.getNodename().toLowerCase();
        for (Element e : elements) {
            if (e.nodeName().toLowerCase().equals(nodename)) {
                String text = e.ownText();
                if (matchText(text, node) && matchElement(node.next, e)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchText(String text, Node node) {
        logger.info("Matching node: {} with text: {}", node.toString(), text);
        for (String t : node.getTextList()) {
            if (text == null || !t.equals(text)) {
                return false;
            }
        }
        for (String c : node.getConstainsList()) {
            if (text == null || !text.contains(c)) {
                return false;
            }
        }
        return true;
    }
}
