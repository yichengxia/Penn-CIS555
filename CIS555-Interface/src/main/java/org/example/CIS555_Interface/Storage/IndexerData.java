package org.example.CIS555_Interface.Storage;

import java.util.HashMap;
import java.util.Map;

public class IndexerData {
	// docId, TF
	private Map<String, Double> tfs = new HashMap<String, Double>();
	private double idf;
	private String word;
	
	public IndexerData() {
	}
	
	public IndexerData(double idf, String word, Map<String, Double> tfs) {
		this.idf = (idf == 0 ? 1 : idf);
		this.word = word;
		this.tfs = tfs;
	}

	public double getIdf() {
		return idf;
	}

	public void setIdf(double idf) {
		this.idf = idf;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public Map<String, Double> getTfs() {
		return tfs;
	}

	public void setTfs(Map<String, Double> tfs) {
		this.tfs = tfs;
	}
	
	
	public void addDoc(String docId, double tf) {
		tfs.put(docId, tf);
	}
	
}
