package edu.upenn.cis.stormlite.tuple;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Node {
    
    private String nodename;
    private List<String> textList;
    private List<String> constainsList;
    public Node next;

    public Node(String nodename) {
        this.nodename = nodename.replaceAll("\\s+", "");
        textList = new ArrayList<>();
        constainsList = new ArrayList<>();
    }

    public String getNodename() {
        return nodename;
    }

    public void setNodename(String nodename) {
        this.nodename = nodename;
    }

    public List<String> getTextList() {
        return textList;
    }

    public void setTextList(List<String> textList) {
        this.textList = textList;
    }

    public List<String> getConstainsList() {
        return constainsList;
    }

    public void setConstainsList(List<String> constainsList) {
        this.constainsList = constainsList;
    }

    public void addTextList(String s) {
        Properties properties = new Properties();
        try (Reader reader = new StringReader("x=" + s + "\n")) {
            properties.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        textList.add(properties.getProperty("x"));
    }

    public void addConstainsList(String s) {
        Properties properties = new Properties();
        try (Reader reader = new StringReader("x=" + s + "\n")) {
            properties.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        constainsList.add(properties.getProperty("x"));
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("/" + nodename + "$");
        for (String text : textList) {
            sb.append("[text()=\"" + text + "\"]");
        }
        for (String contains : constainsList) {
            sb.append("[contains(text(), \"" + contains + "\")]");
        }
        if (next != null) {
            sb.append(" -> " + next.toString());
        }
        return sb.toString();
    }
}
