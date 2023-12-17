package cis555.outputWriter;

import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import cis555.pageRank.pageRankMapper;
import cis555.storage.pageRankRDSController;

import javax.crypto.spec.PSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class outputWriterReducer extends Reducer<IntWritable, Text, Text, Text> {

	private static Logger logger = LogManager.getLogger(outputWriterReducer.class);

	private pageRankRDSController controller = new pageRankRDSController();

	@Override 
	public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		
		List<String[]> res = new ArrayList();
		
		for (Text value : values) {
			String[] urls = value.toString().split(",");
			if ((urls[0].startsWith("http")) && !urls[0].contains("\"") && !urls[0].contains("'")) {
				res.add(urls);
				context.write(new Text(urls[0]), new Text(urls[1]));
			}
		}
		
		System.out.println(key.toString() + "," + res.size());
		if (!res.isEmpty()) {
			try {
				controller.addPageRanks(res);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
