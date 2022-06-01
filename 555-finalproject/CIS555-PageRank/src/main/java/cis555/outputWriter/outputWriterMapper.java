package cis555.outputWriter;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import cis555.pageRank.pageRankMapper;
import cis555.utils.pageRankConfig;

import java.io.IOException;
import java.util.Random;

public class outputWriterMapper extends Mapper<Object, Text, IntWritable, Text> {

	private static Logger logger = LogManager.getLogger(outputWriterMapper.class);

	@Override 
	public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
        String line = value.toString();
        String[] strs = line.split("\t");
        String url = strs[0];

        String[] outUrls = strs[1].split(",");
        String pageRank = outUrls[outUrls.length - 1];

        int randomId = (int) (Math.random() * pageRankConfig.SPLIT);
        context.write(new IntWritable(randomId), new Text(url + "," + pageRank));
	}
}
