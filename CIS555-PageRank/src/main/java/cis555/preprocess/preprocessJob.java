package cis555.preprocess;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import cis555.utils.MyJob;
import cis555.utils.pageRankConfig;

import org.apache.hadoop.mapreduce.lib.map.MultithreadedMapper;


public class preprocessJob {

	Job job;
	
	public preprocessJob() throws IOException {
		this.job = Job.getInstance(pageRankConfig.config, pageRankConfig.PREPROCESS_JOB);
	}
	
	public boolean run(String input, String output) throws IOException, 
								ClassNotFoundException, InterruptedException {
		
		job.setJarByClass(preprocessJob.class);
		job.setMapperClass(preprocessMapper.class);	//TODO: multithreaded mapper?
		job.setReducerClass(preprocessReducer.class);
		
		FileInputFormat.addInputPath(job, new Path(input)); //TODO
		job.setInputFormatClass(TextInputFormat.class);
		
		FileOutputFormat.setOutputPath(job, new Path(output));
		job.setOutputFormatClass(TextOutputFormat.class);
		
//		job.setMapOutputKeyClass(Text.class);
//		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		return job.waitForCompletion(true);
	}

}
