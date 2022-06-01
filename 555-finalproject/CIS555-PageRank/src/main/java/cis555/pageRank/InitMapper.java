package cis555.pageRank;

import java.io.IOException;
import java.util.*;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import cis555.storage.pageRankRDSController;
import cis555.utils.daoUtils;
import cis555.utils.pageRankConfig;

public class InitMapper extends Mapper<Object, Text, Text, Text> {
	
	private static Logger logger = LogManager.getLogger(InitMapper.class);
	
	@Override 
	public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
		String str = value.toString();
		String[] strs = str.split("\t");
		
		if (strs.length > 1) {
			String fromUrl = strs[0];
			String[] outUrls = strs[1].split(",");
			List<String> outLinks = new ArrayList();
			if (!outUrls[0].equals(pageRankConfig.DUMMY_LINK)) {
				outLinks = Arrays.asList(Arrays.copyOfRange(outUrls, 0, outUrls.length-1));
			}
			
			double pageRank = 1.0 / Integer.parseInt(context.getConfiguration().get(pageRankConfig.NODE_COUNTER));
			
			if (!outLinks.isEmpty()) {
				logger.debug("Init Mapper writing entry: url=" + fromUrl + ";value=" + daoUtils.getOutLinksString(outLinks)+ ";pagerank="+ pageRank);
				context.write(new Text(fromUrl), new Text(daoUtils.getOutLinksString(outLinks) + "," + pageRank));
			}
		}
	}

}
