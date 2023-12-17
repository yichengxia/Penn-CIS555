package indexer;

public class WordIndexEntity {
	private String word;
	private String docId; 
	private double freqs;
	
	public WordIndexEntity(String docId, String word, double tf) {
		this.word = word;
		this.docId = docId;
		this.freqs = Math.log(1 + tf);
	}
	
	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public String getDocId() {
		return docId;
	}

	public void setDocId(String docId) {
		this.docId = docId;
	}

	public double getFreqs() {
		return freqs;
	}

	public void setFreqs(double freqs) {
		this.freqs = freqs;
	}
}
