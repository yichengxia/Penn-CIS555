package cis555.utils;

import java.io.File;
import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import cis555.pageRank.InitJob;
import cis555.pageRank.InitMapper;
import cis555.preprocess.preprocessJob;
import cis555.preprocess.preprocessMapper;
import cis555.preprocess.preprocessReducer;
import cis555.utils.MyJob;
import cis555.utils.pageRankConfig;


public class MyJob {
	
	Job job;
	
	public MyJob(String jobname) throws IOException {
		this.job = Job.getInstance(pageRankConfig.config, jobname);
	}
	
	public boolean run(String input, String output, Class mapperClass, Class reducerClass) throws IOException, 
								ClassNotFoundException, InterruptedException {
		
		job.setJarByClass(MyJob.class);
		job.setMapperClass(mapperClass);	//TODO: multithreaded mapper?
		if (reducerClass != null) {
			job.setReducerClass(reducerClass);
		}
		
		initDirectory(input);
		FileInputFormat.addInputPath(job, new Path(input));
		job.setInputFormatClass(TextInputFormat.class);
		
		initDirectory(output);
		FileOutputFormat.setOutputPath(job, new Path(output));
		job.setOutputFormatClass(TextOutputFormat.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		return job.waitForCompletion(true);
	}
	
    public Job getJob() {
        return this.job;
    }
	
    private void initDirectory(String dir) {
       	File directory = new File(dir);
    	if (!directory.exists()) {
    		directory.mkdir();
    	}
    }
}
