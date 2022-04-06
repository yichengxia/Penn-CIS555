package edu.upenn.cis.cis455.xpathengine;

import static org.junit.Assert.*;

import java.time.Instant;

import org.junit.Test;

import edu.upenn.cis.stormlite.tuple.Node;

public class XPathEngineTest {

    @Test
    public void timeTest() {
        String time = Instant.ofEpochMilli(System.currentTimeMillis()).toString();
        System.out.println(time);
        assertEquals("2022", time.substring(0, 4));
    }

    @Test
    public void setXpathTest1() {
        String xpath = "/rss/channel/title[contains(text(),\"sports\")]";
        String[] expressions = new String[1];
        expressions[0] = xpath;
        XPathEngineImpl xPathEngine = new XPathEngineImpl();
        xPathEngine.setXPaths(expressions);
        Node node = xPathEngine.getNodes()[0];
        assertEquals("rss", node.getNodename());
    }

    @Test
    public void setXpathTest2() {
        String xpath = "/a/b/c[text()  =    \"whiteSpacesShouldNotMatter\"]";
        String[] expressions = new String[1];
        expressions[0] = xpath;
        XPathEngineImpl xPathEngine = new XPathEngineImpl();
        xPathEngine.setXPaths(expressions);
        Node node = xPathEngine.getNodes()[0];
        assertEquals("whiteSpacesShouldNotMatter", node.next.next.getTextList().get(0));
    }

    @Test
    public void setXpathTest3() {
        String xpath = "/xyz/abc[contains(text(),\"someSubstring\")]";
        String[] expressions = new String[1];
        expressions[0] = xpath;
        XPathEngineImpl xPathEngine = new XPathEngineImpl();
        xPathEngine.setXPaths(expressions);
        Node node = xPathEngine.getNodes()[0];
        assertEquals("someSubstring", node.next.getConstainsList().get(0));
    }

    @Test
    public void evaluateEvent1() {
        String xpath = "/rss/channel/title[contains(text(),\"sports\")]";
        String[] expressions = new String[1];
        expressions[0] = xpath;
        XPathEngineImpl xPathEngine = new XPathEngineImpl();
        xPathEngine.setXPaths(expressions);
        OccurrenceEvent oe = new OccurrenceEvent(OccurrenceEvent.Type.Open, "rss");
        boolean[] res = xPathEngine.evaluateEvent(oe);
        assertFalse(res[0]);
    }

    @Test
    public void evaluateEvent2() {
        String xpath = "/a/b/c[text()  =    \"whiteSpacesShouldNotMatter\"]";
        String[] expressions = new String[1];
        expressions[0] = xpath;
        XPathEngineImpl xPathEngine = new XPathEngineImpl();
        xPathEngine.setXPaths(expressions);
        OccurrenceEvent oe = new OccurrenceEvent(OccurrenceEvent.Type.Open, "a");
        boolean[] res = xPathEngine.evaluateEvent(oe);
        assertFalse(res[0]);
    }
}
