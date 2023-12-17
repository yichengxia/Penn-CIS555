package cis555.pageRank;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.util.*;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import cis555.storage.pageRankRDSController;
import cis555.utils.daoUtils;
import cis555.utils.pageRankConfig;

public class pageRankReducer extends Reducer<Text, Text, Text, Text> {

	private static Logger logger = LogManager.getLogger(pageRankReducer.class);

	@Override 
	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		String fromUrl = key.toString();
        List<String> outLinks = new ArrayList<>();
        double sum = 0;
        boolean isCrawled = false;
        
		for (Text value : values) {
			String str = value.toString();
			if  (str.contains(",")) {
				String[] outUrls = str.split(",");
                if (!pageRankConfig.DUMMY_LINK.equals(outUrls[0])) {
                    isCrawled = true;
                    outLinks = Arrays.asList(Arrays.copyOfRange(outUrls, 0, outUrls.length - 1));
                }
			} else {
				sum += Double.parseDouble(str);
			}
		}
		
		int nodesCount = Integer.parseInt(context.getConfiguration().get(pageRankConfig.NODE_COUNTER));
		double damping = pageRankConfig.DAMPING_FACTOR;
        double newPageRank = (damping / nodesCount) + (1 - damping) * sum;
        
        if (!isCrawled) {
			context.write(new Text(fromUrl), new Text(daoUtils.getOutLinksString(outLinks) + "," + newPageRank));
        }
	}
}
