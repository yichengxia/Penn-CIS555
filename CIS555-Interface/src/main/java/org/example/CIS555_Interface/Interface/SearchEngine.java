package org.example.CIS555_Interface.Interface;

import java.sql.SQLException;
import java.util.*;

import org.example.CIS555_Interface.Storage.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


public class SearchEngine {
	private String query;
	private static final int NUM_PER_PAGE = 10;
	private int totalPageNum = 0; 
	private int startIdx = 0; 
	private int numPageToShow = 0; 

	public SearchEngine(String query) {
		this.query = query;
	}
	
	public int getTotalPageNum() {
		return this.totalPageNum;
	}

	public String searchQuery(int targetPageNum) throws SQLException {
		System.out.println("search for query:" +this.query+ " page:"+targetPageNum);
		if (this.query == null) {
			System.out.println("Query is empty");
			return null;
		}

		Map<String, Double> tfidfs = getTFIDF();
		if(tfidfs == null) {
			System.out.println("tfidfs result is null");
			return null;
		}
		if (tfidfs.size() == 0) {
			return "<div class=\"serp__no-results\">\n" +
					"<h1>No Result Found<h1>" +
					"    <h3><strong>Couldn't find any matches for \"" + this.query + "\"</strong></h3>\n" +
					"    <p> Please double check your search for any typos or spelling errors or try a different search term"+          
					"</div>\n";
		}

		List<List<String>> urls = InterfaceMain.docRDSController.queryUrlsByDocIds(new ArrayList(tfidfs.keySet()));
		System.out.println("get urls");

		List<Double> pageRank = getPageRank(urls);

		//get doc info data 
		List<ResultItem> files = sortFiles(tfidfs, urls, pageRank);
		this.totalPageNum = (int) Math.ceil(files.size() / NUM_PER_PAGE);//should be returned

		//get real doc content
		List<ArrayList<String>> fileContents = getContent(files, targetPageNum);
		System.out.println("get doc content");

		return writeResToPage(files, fileContents);
		//discard spam sites?---------
	}

	//return docId : TFIDFscore
	public Map<String, Double> getTFIDF() {
		//receive indexer results
		List<IndexerData> indexerResults = null;
		try {
			indexerResults = InterfaceMain.indexerDB.getIndexFromQuery(this.query);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (indexerResults==null || indexerResults.size() == 0) {
			System.out.println("Indexer returns no results.");
			return null;
		}


		Map<String, Double> tfidfs = new HashMap<String, Double>();
		for (int i = 0 ; i < indexerResults.size(); i++) {
			IndexerData indexerResult = indexerResults.get(i);
			// docId : TF
			Map<String, Double> tfMap = indexerResult.getTfs();
			for (String docId : tfMap.keySet()) {
				// curr is the tfidfs sum of former words
				double curr = tfidfs.getOrDefault(docId, (double) 0);
				tfidfs.put(docId, curr + tfMap.get(docId) * indexerResult.getIdf() );
			}
		}
		return tfidfs;
	}

	public List<Double> getPageRank(List<List<String>> urls) {
		List<String> allUrls = new ArrayList<>();
		
		// infos is true, one docId MORE urls
		for (int i = 0 ; i < urls.size(); i++) {
			List<String> urlL = urls.get(i);
			if (urlL.size() != 1) {
				System.out.println("url list size:" + urlL.size()+ urlL.toString());
			}
			allUrls.addAll(urlL);
		}
		
		List<Double> res;
		try {
			res = InterfaceMain.pageRankController.queryPRbyURLs(allUrls);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return res;
	}

	public List<ResultItem> sortFiles(Map<String, Double> tfidfs, List<List<String>> urls, List<Double> prs) {
		List<ResultItem> res = new ArrayList<ResultItem>();

		Double maxTfidf = -Double.MAX_VALUE;
		Double maxPr = -Double.MAX_VALUE;
		for (String s : tfidfs.keySet()) { 
        	maxPr = Math.max(maxTfidf, tfidfs.get(s));
        }
        for(Double val : prs) {
        	maxTfidf = Math.max(maxPr, val);
        }

        int idx = 0;
        int urlidx = 0;
        for (String docId : tfidfs.keySet()) {
        	List<String> urlL = urls.get(idx);
        	Double tfidf = tfidfs.get(docId);
        	if (urlL!=null && urlL.size()>0) {
        		Double currMaxPr = -Double.MAX_VALUE;
        		String maxUrl = "";
        		for (int i = 0; i < urlL.size(); i++) {
        			//System.out.println("value------:" + prs.get(urlidx) + " " + currMaxPr);
        			if (prs.get(urlidx) >= currMaxPr) {
	                    currMaxPr = prs.get(urlidx);
	                    maxUrl = urlL.get(i);
	                    //System.out.println("-----------11111111:" + maxUrl);
	                }
	                urlidx++;
        		}
        		Double score = (tfidf/maxTfidf) * 0.7 + (currMaxPr/maxPr) * 0.3;
        		ResultItem resItem = new ResultItem(docId, maxUrl, tfidf, currMaxPr, score);
        		if (urlL.size() > 1) {
	                resItem.others = new ArrayList<>();
	                for (String s: urlL) {
	                	if (!s.equals(maxUrl)) {
	                		resItem.others.add(s);
	                	}
	                }
	            } else {
	            	resItem.others = null;
	            }
	            res.add(resItem);
        	}
        	idx += 1;
        }

        Comparator<ResultItem> cmp = (ResultItem c1, ResultItem c2) -> Double.compare(c2.score, c1.score);
        Collections.sort(res, cmp);
        return res;
	}

	public List<ArrayList<String>> getContent(List<ResultItem> docs, Integer targetPageNum) {
		this.startIdx = NUM_PER_PAGE * (targetPageNum - 1);
        this.numPageToShow = Math.min(NUM_PER_PAGE, docs.size() - startIdx);
        ArrayList<String> docNeeded = new ArrayList<>();
        for (int i=0; i<numPageToShow; i++) {
        	docNeeded.add(docs.get(i+startIdx).getDocId());
        }

        //get doc contents
        //ArrayList<String> contents= new ArrayList<String>();
        ArrayList<String> titles= new ArrayList<String>();
        ArrayList<String> intro= new ArrayList<String>();
        for (int i = 0; i < numPageToShow; i++) {
        	String fileContent = getContentForId(docNeeded.get(i));
            //contents.add(fileContent);

            //get titles and brief introduction
            Document doc = Jsoup.parse(fileContent);
            if (doc.title() != null) {
            	titles.add(doc.title());
            } else {
            	titles.add("No title");
            }
            String des = doc.select("meta[name=description]").attr("content");
            if (des != null) {
                intro.add(des);
            } else {
                intro.add(doc.body().text().substring(0, Math.min(150, doc.body().text().length())));
            }
        }
        List<ArrayList<String>> res = new ArrayList<ArrayList<String>>();
        res.add(titles);
        res.add(intro);
        return res;
    }

    public String getContentForId(String docId) {
    	List<DocRDSEntity> docIndexList = null;
        try {
            docIndexList = InterfaceMain.docRDSController.queryDocByDocId(docId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (docIndexList==null || docIndexList.size() == 0) {
            System.out.println("docId " + docId + " cannot be found.");
            return null;
        }
        DocRDSEntity docRDSEntity = docIndexList.get(0);
        DocS3Entity docS3Entity = null;
		try {
			docS3Entity = InterfaceMain.docS3Controller.querySingleDoc(docRDSEntity.getDocBlockName(), docRDSEntity.getDocBlockIndex());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
        return new String(docS3Entity.getContentBytes());
    }

    public String writeResToPage(List<ResultItem> docs, List<ArrayList<String>> fileContents) {
    	List<String> titles = fileContents.get(0);
		List<String> intro = fileContents.get(1);
    	StringBuilder sb = new StringBuilder();
    	System.out.println("docs size:" + numPageToShow + " " + titles.size() + " " + intro.size()+" " + docs.size()+" startIdx:"+startIdx);
    	for (int i=this.startIdx; i<this.startIdx+this.numPageToShow; i++) {
    		ResultItem doc= docs.get(i);
    		String docurl = null;
    		if(doc != null) {
    			docurl = doc.url;
    		}
    		sb.append("<a href=\"" + docurl + "\" target=\"_blank\">\n");
            sb.append("<div class=\"title\">" + titles.get(i-this.startIdx) + "</div>\n");
            sb.append("<div class=\"url\">" + docurl + "</div>\n");
            sb.append("</a>\n <div><span class=\"description\">" + intro.get(i-this.startIdx) + "</span>\n</div>");
    	}
    	return sb.toString();
    }

}