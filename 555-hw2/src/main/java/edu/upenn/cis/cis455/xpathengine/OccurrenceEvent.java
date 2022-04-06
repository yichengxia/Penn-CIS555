package edu.upenn.cis.cis455.xpathengine;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

/**
 This class encapsulates the tokens we care about parsing in XML (or HTML)
 */
public class OccurrenceEvent {
	enum Type {Open, Close, Text};
	
	Type type;
	String value;
	Element element;
	
	public OccurrenceEvent(Type t, String value) {
		this.type = t;
		this.value = value;
	}

	public OccurrenceEvent(String value, String type) {
		this.value = value;
		this.type = Type.Text;
		element = type.equals("xml") ? Jsoup.parse(value, "", Parser.xmlParser()) : Jsoup.parse(value, "", Parser.htmlParser());
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	public Element getElement() {
		return element;
	}

	public void setElement(Element element) {
		this.element = element;
	}

	public String toString() {
		if (type == Type.Open) 
			return "<" + value + ">";
		else if (type == Type.Close)
			return "</" + value + ">";
		else
			return value;
	}
}
