package indexer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import storage.DocS3Block;
import storage.DocS3Controller;
import storage.DocS3Entity;

public class Indexer {
	static HashSet<String> stopWordsSet = new HashSet<String>();
	static HashMap<String, Double> counts = null;
	static HashMap<String, Double> finalOutputs = null;
	
	// LongWrittable - Document Id
	// Text - Document
	// Text - Word 
	// IntWrittable - number of times term is seen
	public static class WordCountMapper extends Mapper<Object, Text, Text, DoubleWritable> {
		@Override
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException
		{
			String curLine = "";
			if (value != null) {
				curLine = value.toString().replaceAll("\\p{Punct}", "");
			}
			String curFileName = ((FileSplit) context.getInputSplit()).getPath().getName();
			
			StringTokenizer strTok = new StringTokenizer(curLine);
			Text curText = new Text();
			DoubleWritable one = new DoubleWritable(1);
			
			while(strTok.hasMoreTokens()) {
				String curTok = strTok.nextToken().toLowerCase();
				if (storage.Utils.shouldAddCount(curTok, stopWordsSet)) {
					curText.set(curTok + "\t" + curFileName);
					context.write(curText, one);
				}
			}
		}
	}
	
	
	public static class WordCountReducer extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {
		
		@Override
		public void reduce(Text key, Iterable <DoubleWritable> values, Context context) throws IOException, InterruptedException {
			int sum = 0;
			Iterator<DoubleWritable> vals = values.iterator();
			String[] wordFile = key.toString().split("\t");
			String fileName = wordFile[1];
			while(vals.hasNext()) {
				sum ++; 	
				vals.next();
			}	
			double tf = sum/ counts.get(fileName);
			finalOutputs.put(key.toString(), tf);
		}
	} 
	
	public static class DocWordCountMapper extends Mapper<Object, Text, Text, DoubleWritable> {
		@Override
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException
		{
			String line = value.toString().replaceAll("\\p{Punct}", "");
			
			String fileName = ((FileSplit) context.getInputSplit()).getPath().getName();
			
			StringTokenizer strTok = new StringTokenizer(line);
			String curTok;
			Text curr = new Text(fileName);
			double count = 0;
			while(strTok.hasMoreTokens()) {
				curTok = strTok.nextToken().toLowerCase();
				if (storage.Utils.shouldAddCount(curTok, stopWordsSet)) {
					count++;
				}
			}
			context.write(curr, new DoubleWritable(count));
		}
	}
	
	public static class DocWordCountReducer extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {
		@Override
		public void reduce(Text key, Iterable <DoubleWritable> values, Context context) throws IOException, InterruptedException {
			double sum = 0;
			Iterator<DoubleWritable> vals = values.iterator();
			while(vals.hasNext()) {
				sum += vals.next().get();
			}	
			counts.put(key.toString(), sum);		
		}
	} 
	
	private static boolean runDocWordCount(String input, String output) throws IllegalArgumentException, IOException, ClassNotFoundException, InterruptedException {
		Configuration conf = new Configuration();
		
		Job job = Job.getInstance(conf, "docWordCount");
		job.setJarByClass(Indexer.class);
		job.setMapperClass(DocWordCountMapper.class);
		job.setReducerClass(DocWordCountReducer.class);
		job.setNumReduceTasks(1);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(DoubleWritable.class);
		
		FileInputFormat.addInputPath(job, new Path(input));
		FileOutputFormat.setOutputPath(job, new Path(output));

		boolean status = job.waitForCompletion(true);
		return status;
	} 
	
	private static boolean runWordCount(String input, String output) throws IllegalArgumentException, IOException, ClassNotFoundException, InterruptedException {
		Configuration conf = new Configuration();
		
		Job job = Job.getInstance(conf, "WordCount");
		job.setJarByClass(Indexer.class);
		job.setMapperClass(WordCountMapper.class);
		job.setReducerClass(WordCountReducer.class);
		job.setNumReduceTasks(10);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(DoubleWritable.class);
		
		FileInputFormat.addInputPath(job, new Path(input));
		FileOutputFormat.setOutputPath(job, new Path(output));

		boolean status = job.waitForCompletion(true);
		return status;
	} 
	
	public static  void initStopWords() {
		String[] stopWordsArray = storage.Utils.stopWordsArray;
//		for(int i = 0; i < stopWordsArray.length; i++) {
//			System.out.println(stopWordsArray[i]);
//		}
		
		for (String stop: stopWordsArray) {	
			stopWordsSet.add(stop);
		}
	}
	
	public static  void main(String args[]) throws IOException, ClassNotFoundException, InterruptedException {
		
		initStopWords();
		counts = new HashMap<String, Double>();
		finalOutputs = new HashMap<String, Double>();
		String storageDirectory = args[0];
		
		File inputs = new File(storageDirectory + "/inputs");
		File wordCounts = new File(storageDirectory + "/wordCounts");
		File outputs = new File(storageDirectory + "/outputs");
		
		ArrayList<String> currBlocks = new ArrayList<String>();
		boolean foundBlock = false;
		List<String> blockNames = DocS3Controller.listFilesInS3();
		
//		for(int j = 0; j < blockNames.size(); j++) {
//			System.out.println(blockNames.get(j));
//		}
		
		try (DocS3Controller docS3Controller = new DocS3Controller()) {
			
			for (String bName: blockNames) {
				System.out.println(bName);
				try {
					if (WordIndexController.checkBlockIndexed(bName) > 0) {
						continue;
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {
					System.out.println("Adding block: " + bName);
					WordIndexController.addBlock(bName);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				DocS3Block docS3Block = docS3Controller.getEntireDocBlock(bName);
				Iterator<DocS3Entity> it = docS3Block.iterator();
				//System.out.println(docS3Block.getEntityCount());
				while (it.hasNext()) {
					DocS3Entity entity = it.next();
					inputs.mkdir();
					File f = new File(inputs.toString() + "/" + DocS3Entity.toHexString(entity.getDocId()));
					f.createNewFile();
					try (FileOutputStream outputStream = new FileOutputStream(f)) {
						Document doc_content = Jsoup.parse(new String (entity.getContentBytes()));
						Elements textItems = storage.Utils.getTextItems(doc_content);
						
					    for (Element ele : textItems) {
					    	outputStream.write((ele.text() + "\n").getBytes());
					    }								
					}
				}
				try {
					WordIndexController.addBlock(bName);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				currBlocks.add(bName);
				foundBlock = true;
				if (foundBlock) {
					boolean docWCount_status = runDocWordCount(inputs.toString(), wordCounts.toString());
					FileUtils.deleteDirectory(wordCounts);
					if (!docWCount_status) { 
						System.out.println("Having problem running doc word count");
						FileUtils.deleteDirectory(outputs);
				        FileUtils.cleanDirectory(inputs);
						System.exit(1);
					}
					boolean wCount_status = runWordCount(inputs.toString(), outputs.toString());	

					
					if (!wCount_status) { 
						System.out.println("Having problem running word count");
						System.exit(1);
					}

					try {
						System.out.println("adding words");
						WordIndexController.addWordIndex(finalOutputs);
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					FileUtils.deleteDirectory(outputs);
			        FileUtils.cleanDirectory(inputs);
					counts = new HashMap<String, Double>();
					finalOutputs = new HashMap<String, Double>();
					foundBlock = false;
					currBlocks.clear();
				}
				
			}
		}
	}
}
