package cis555;

import java.io.File;
import java.io.IOException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import cis555.outputWriter.outputWriterJob;
import cis555.outputWriter.outputWriterMapper;
import cis555.outputWriter.outputWriterReducer;
import cis555.pageRank.InitJob;
import cis555.pageRank.InitMapper;
import cis555.pageRank.pageRankJob;
import cis555.pageRank.pageRankMapper;
import cis555.pageRank.pageRankReducer;
import cis555.preprocess.preprocessJob;
import cis555.preprocess.preprocessMapper;
import cis555.preprocess.preprocessReducer;
import cis555.utils.MyJob;
import cis555.utils.daoUtils;
import cis555.utils.pageRankConfig;

import org.apache.hadoop.conf.Configuration;

public class pageRankMain {
	
	public static Configuration config = new Configuration();
	
	public static void main(String[] args) {
		
		initDirectory(pageRankConfig.PAGERANK_INPUT_DIR);
		initDirectory(pageRankConfig.PAGERANK_OUTPUT_DIR);
		
		try {
			
			daoUtils.getAllDocBlockNames();
			MyJob preprocessJob = new MyJob(pageRankConfig.PREPROCESS_JOB);
			boolean isDone = preprocessJob.run(pageRankConfig.PREPROCESS_INPUT_DIR, pageRankConfig.PREPROCESS_OUTPUT_DIR, preprocessMapper.class, preprocessReducer.class);

			if (!isDone) {
				System.exit(1);
			}
			
			MyJob InitJob = new MyJob(pageRankConfig.INIT_JOB);
			isDone = InitJob.run(pageRankConfig.PREPROCESS_OUTPUT_DIR, pageRankConfig.PAGERANK_INPUT_DIR + "/0", InitMapper.class, null);
			
			if (!isDone) {
				System.exit(1);
			}
			
			String inputPath = pageRankConfig.PAGERANK_INPUT_DIR + "/0";
			double previousDelta = 0.0;
			
	        for (int i = 0; i < pageRankConfig.ITERATIONS; i++) {
	        	pageRankConfig.config.set(pageRankConfig.DELTA, String.valueOf(previousDelta));
	            MyJob pagerankJob = new MyJob(pageRankConfig.PAGERANK_JOB);
	            String outputPath = pageRankConfig.PAGERANK_INPUT_DIR + "/" + (i + 1);
	            isDone = pagerankJob.run(inputPath, outputPath, pageRankMapper.class, pageRankReducer.class);

	            if (!isDone) {
	                System.exit(1);
	            }

	            previousDelta = (double) pagerankJob.getJob().getCounters().findCounter(pageRankConfig.COUNTERS.deltaCounter).getValue();
	            pagerankJob.getJob().getCounters().findCounter(pageRankConfig.COUNTERS.deltaCounter).setValue(0);
	            inputPath = outputPath;
	        }


	        MyJob writerJob = new MyJob(pageRankConfig.WRITER_JOB);
	        isDone = writerJob.run(inputPath, pageRankConfig.PREPROCESS_OUTPUT_DIR, outputWriterMapper.class, outputWriterReducer.class);

	        if (!isDone) {
	            System.exit(1);
	        }

	        System.out.println("PageRank Finished!");
			
		} catch (IOException e) {	
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    public static void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }
    
    public static void initDirectory(String dir) {
    	File directory = new File(dir);
    	if (!directory.exists()) {
    		directory.mkdir();
    	} else {
    		deleteDirectory(directory);
    	}
    }
}
