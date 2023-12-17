package org.example.CIS555_Interface.Interface;

import java.util.List;

//structure to stroe search results
public class ResultItem {
    String url;
    String docId;
    double tfidf;
    double pr;
    double score;
    public List<String> others;

    public ResultItem(String docId, String url, double tfidf, double pr, double score) {
        this.docId = docId;
        this.url = url;
        this.tfidf = tfidf;
        this.pr = pr;
        this.score = score;
    }

    public String getDocId() {
        return docId;
    }
}
