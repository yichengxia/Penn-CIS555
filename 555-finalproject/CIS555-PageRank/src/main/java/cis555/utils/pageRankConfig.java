package cis555.utils;
import org.apache.hadoop.conf.Configuration;


public class pageRankConfig {
	public static final String PREPROCESS_INPUT_DIR = "./Preprocess";
	public static final String PAGERANK_INPUT_DIR = "./PageRankInput";
	public static final String PAGERANK_OUTPUT_DIR = "./PageRankOutput";
	public static final String PREPROCESS_FILENAME = "/DocumentBlocks";
	public static final String PREPROCESS_OUTPUT_DIR = PAGERANK_INPUT_DIR + "/pre";
	
    public static final String PAGERANK_TABLE = "PAGERANK_TABLE";
    public static final String PREPROCESS_JOB = "Preprocess Job";
    public static final String INIT_JOB = "Initialization Job";
    public static final String PAGERANK_JOB = "PageRank Job";
    public static final String WRITER_JOB = "Writer Job";
    public static final String DUMMY_LINK = "dummy";
    public static final String NODE_COUNTER = "nodeCounter";
    public static final String DELTA = "delta";
    public static enum COUNTERS{
        nodeCounter,
        deltaCounter;
    }
    public static final Configuration config = new Configuration();
    public static final double DAMPING_FACTOR = 0.85;
    public static final int SPLIT = 3000;
    public static final int ITERATIONS = 20;
}
