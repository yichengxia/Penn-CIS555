package cis555.pageRank;


import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import cis555.preprocess.preprocessJob;
import cis555.preprocess.preprocessMapper;
import cis555.preprocess.preprocessReducer;
import cis555.utils.MyJob;
import cis555.utils.pageRankConfig;

public class InitJob {


	Job job;
	
	public InitJob() throws IOException {
		this.job = Job.getInstance(pageRankConfig.config, pageRankConfig.INIT_JOB);
	}
	
	public boolean run(String input, String output) throws IOException, 
								ClassNotFoundException, InterruptedException {
		
		job.setJarByClass(InitJob.class);
		job.setMapperClass(InitMapper.class);	//TODO: multithreaded mapper?
		//job.setReducerClass(preprocessReducer.class);
		
		FileInputFormat.addInputPath(job, new Path(input));
		job.setInputFormatClass(TextInputFormat.class);
		
		FileOutputFormat.setOutputPath(job, new Path(output));
		job.setOutputFormatClass(TextOutputFormat.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		return job.waitForCompletion(true);
	}

}
