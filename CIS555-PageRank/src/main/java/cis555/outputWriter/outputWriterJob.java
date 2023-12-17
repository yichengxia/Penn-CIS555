package cis555.outputWriter;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import cis555.pageRank.pageRankJob;
import cis555.pageRank.pageRankMapper;
import cis555.pageRank.pageRankReducer;
import cis555.utils.MyJob;
import cis555.utils.pageRankConfig;

public class outputWriterJob {
	
	Job job;
	
	public outputWriterJob() throws IOException {
		this.job = Job.getInstance(pageRankConfig.config, pageRankConfig.PAGERANK_JOB);
	}
	
	public boolean run(String input, String output) throws IOException, 
								ClassNotFoundException, InterruptedException {
		
		job.setJarByClass(outputWriterJob.class);
		job.setMapperClass(outputWriterMapper.class);	//TODO: multithreaded mapper?
		job.setReducerClass(outputWriterReducer.class);
		
		FileInputFormat.addInputPath(job, new Path(input));
		job.setInputFormatClass(TextInputFormat.class);
		
		FileOutputFormat.setOutputPath(job, new Path(output));
		job.setOutputFormatClass(TextOutputFormat.class);
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		return job.waitForCompletion(true);
	}


}
