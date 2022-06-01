package cis555.utils;

import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import cis555.storage.DocS3Controller;
import cis555.storage.pageRankRDSController;

public class daoUtils {
	private static Logger logger = LogManager.getLogger(daoUtils.class);

	
    public static void getAllDocBlockNames() {
        List<String> blockNames = DocS3Controller.listFilesInS3();

        try {
            PrintWriter writer = new PrintWriter(pageRankConfig.PREPROCESS_INPUT_DIR + pageRankConfig.PREPROCESS_FILENAME, "UTF-8");
            for (String blockName : blockNames) {
                writer.println(blockName);
            }

            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
	public static List<String> getOutLinks(byte[] content, String url) {
		Set<String> res = new HashSet();
		
		try { //TODO: 
			Document document = Jsoup.parse(new String(content));
			Elements links = document.select("a[href]"); 
			
			for (Element link: links) {
				String outUrl = link.attr("href"); 
				res.add(outUrl);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return new ArrayList<String>(res);
	}
	
	public static String getOutLinksString(List<String> outLinks) {
		if (outLinks.isEmpty()) {
			return pageRankConfig.DUMMY_LINK;	//TODO: dummy link?
		}
		
		StringBuilder res = new StringBuilder();
		for (String link : outLinks) {
			res.append(link);
			res.append(",");
		}
		
		res.deleteCharAt(res.length()-1); //TODO: stream
		return res.toString();
	}
	
	public static String getOutLinksString(byte[] content, String url) {
		List<String> outLinks = getOutLinks(content, url);
		return getOutLinksString(outLinks);
	}
	
	public static String toUrlId(String url) {
	    try {
	        MessageDigest md = MessageDigest.getInstance("SHA-1");
	        md.update(url.getBytes());
	        byte[] sha1Value = md.digest();
	        String ret = String.format("%1$40s", (new BigInteger(1, sha1Value)).toString(16)).replace(' ', '0');
	        return ret;
	    } catch (NoSuchAlgorithmException e) {
	    	e.printStackTrace();
	    }
	    return null;
	}
		
    public static void main(String[] args) {
        getAllDocBlockNames();
     }
}
