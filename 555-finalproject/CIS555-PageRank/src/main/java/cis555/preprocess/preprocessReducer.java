package cis555.preprocess;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import cis555.storage.pageRankRDSController;
import cis555.utils.pageRankConfig;

import java.io.IOException;

import org.apache.hadoop.io.Text;



public class preprocessReducer extends Reducer<Text, Text, Text, Text>{
	private static Logger logger = LogManager.getLogger(preprocessReducer.class);

	@Override 
	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		
		StringBuilder sb = new StringBuilder();	//TODO: change schema 
		
		for (Text value : values) {
			String urls = value.toString();
			if (urls.equals(pageRankConfig.DUMMY_LINK)) {
				sb.append(urls).append(",");
			}
		}
		
		if (sb.length() == 0) {
			sb.append(pageRankConfig.DUMMY_LINK).append(",");
		}
		
		sb.append("0");
		context.write(key, new Text(sb.toString()));
		logger.debug("Preprocess Reducer writing entry: url="+ key.toString() + ";value="+sb.toString());
		context.getCounter(pageRankConfig.COUNTERS.nodeCounter).increment(1);
	}
}
