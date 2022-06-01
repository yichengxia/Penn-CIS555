package cis555.pageRank;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.util.*;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import cis555.storage.pageRankRDSController;
import cis555.utils.daoUtils;
import cis555.utils.pageRankConfig;


public class pageRankMapper extends Mapper<Object, Text, Text, Text> {
	
	private static Logger logger = LogManager.getLogger(pageRankMapper.class);

	
	@Override 
	public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
		String str = value.toString();
		String[] strs = str.split("\t");
		
		if (strs.length > 1) {
			String fromUrl = strs[0];
			String[] outUrls = strs[1].split(",");
			double pageRank = Double.parseDouble(outUrls[outUrls.length - 1]);
			
			String deltaStr = context.getConfiguration().get("delta");
			double delta = Double.parseDouble(deltaStr) / 100000;
			int nodesCount = Integer.parseInt(context.getConfiguration().get(pageRankConfig.NODE_COUNTER));
			double newPageRank = pageRank + (1 - pageRankConfig.DAMPING_FACTOR) * (delta / nodesCount);
			
			List<String> outLinks = new ArrayList();
			if (!outUrls[0].equals(pageRankConfig.DUMMY_LINK)) {
				outLinks = Arrays.asList(Arrays.copyOfRange(outUrls, 0, outUrls.length - 1));
			}
			
			context.write(new Text(fromUrl), new Text(daoUtils.getOutLinksString(outLinks) + "," + newPageRank));
			if (!outLinks.isEmpty()) {
				double linkpr = newPageRank / outLinks.size();
				for (String url : outLinks) {
					context.write(new Text(url), new Text(String.valueOf(linkpr)));
				}
			} else {
	            context.getCounter(pageRankConfig.COUNTERS.deltaCounter).increment((long) pageRank * 100000);
			}
		}
		
	}
}
