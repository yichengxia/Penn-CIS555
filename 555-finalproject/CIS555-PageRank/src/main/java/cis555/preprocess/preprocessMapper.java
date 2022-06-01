package cis555.preprocess;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.*;

import cis555.storage.DocRDSController;
import cis555.storage.DocS3Block;
import cis555.storage.DocS3Controller;
import cis555.storage.DocS3Entity;
import cis555.storage.pageRankRDSController;
import cis555.utils.daoUtils;

import java.io.IOException;

import org.apache.hadoop.io.Text;



public class preprocessMapper extends Mapper<Object, Text, Text, Text> {
	
	private static Logger logger = LogManager.getLogger(preprocessMapper.class);

	DocRDSController docRDSController = new DocRDSController();
    DocS3Controller docS3Controller = new DocS3Controller();
	
	@Override 
	public void map (Object key, Text value, Context context) throws IOException, InterruptedException {
		
		String blockName = value.toString();
		logger.info("Processing block: " + blockName);
		
		try {
            DocS3Block docS3Block = docS3Controller.getEntireDocBlock(blockName);
            Iterator<DocS3Entity> it = docS3Block.iterator();
                        
            while (it.hasNext()) {
            	DocS3Entity entity = it.next();
            	if (entity.getContentType() == 0) { // contentType 0 == html 
            		String id = DocS3Entity.toHexString(entity.getUrlId());
            		String url = docRDSController.queryUrlByUrlId(id);
            		String outLinks = daoUtils.getOutLinksString(entity.getContentBytes(), url);
            		logger.debug("Preprocess Mapper writing entry: url=" + url + ";outlinks=" + outLinks);
            		context.write(new Text(url), new Text(outLinks));
            		// TODO: consider other schema?
            	}
            }
               
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
