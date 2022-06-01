package edu.upenn.cis.cis555.storage;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * This is the class for Amazon S3 controller.
 */
public class DocS3Controller implements AutoCloseable {

	private static Logger logger = LoggerFactory.getLogger(DocS3Controller.class);
	private static int BLOCK_SIZE = 20 * 1024 * 1024;

	private static final String BUCKET_NAME = "cis555www-2";
	private static Region region = Region.US_EAST_1;

	private static AwsCredentialsProvider creds = () ->
		AwsBasicCredentials.create("AKIA3EWWERKOXY4DM2XM", "mwt/zxE42FzRD5wJKlAlGaNGNH/cJnSIvSV+GPVo");
	private static final S3Client s3 = S3Client.builder().region(region).credentialsProvider(creds).build();

	private DocS3Block currBlock = null;
	private String nodeId = null;
	private final String storageDir = "./temp/";

	public DocS3Controller(String nodeId) {
		if (nodeId.length() >= 10) {
			throw new RuntimeException("Node Identifier too long");
		}
		new File(storageDir).mkdirs();
		this.nodeId = nodeId;
		newBlock();
	}

	private synchronized void newBlock() {
		currBlock = new DocS3Block(nodeId + "-" + System.currentTimeMillis()/1000);
		logger.info("New Block Created: {}", currBlock.getBlockName());
	}

	private synchronized void check(int contentSize) {
		if (currBlock.getTotalBytes() + DocS3Block.getHeaderLength() + contentSize > BLOCK_SIZE) {
			final DocS3Block blockToSave = currBlock;
			new Thread(()-> {
				blockToSave.saveToFile(storageDir);
				logger.info("Current Block {} with {} documents is saved.", blockToSave.getBlockName(), blockToSave.getEntityCount());
				uploadFileToS3(Paths.get(storageDir, blockToSave.getBlockName()), blockToSave.getBlockName());
				logger.info("Current Block {} has been uploaded to S3.", blockToSave.getBlockName());
				Paths.get(storageDir, blockToSave.getBlockName()).toFile().delete();
				blockToSave.reset();
			}).start();
			this.newBlock();
		}
	}

	public final List<Object> addDoc(String url, short contentType, byte[] contentBytes, byte[] docId, byte[] urlId) {
		check(contentBytes.length);
		String blockName = currBlock.getBlockName();
		int blockIndex = currBlock.addDoc(url, contentType, contentBytes, docId, urlId);
		return Arrays.asList(blockIndex, blockName);
	}

	private static void uploadFileToS3(Path path, String blockName) {
		PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(BUCKET_NAME).key(blockName).build();
		PutObjectResponse putObjectResponse = s3.putObject(putObjectRequest, path);
		logger.info("Successfully up load to S3. Response: {}", putObjectResponse.eTag());
	}
	
	@Override
	public synchronized void close() {
		if (currBlock != null && currBlock.getEntityCount() > 0) {
			final DocS3Block blockToSave = currBlock;
			blockToSave.saveToFile(storageDir);
			logger.info("Current Block " + currBlock.getBlockName() + " has " + currBlock.getEntityCount() + " entities, which is saved automatically.");
			uploadFileToS3(Paths.get(storageDir, blockToSave.getBlockName()), blockToSave.getBlockName());
			logger.info("Current Block " + currBlock.getBlockName() + " has been uploaded to S3.");
			Paths.get(storageDir, blockToSave.getBlockName()).toFile().delete();
			blockToSave.reset();
		} else {
			logger.info("Current Block " + currBlock.getBlockName() + " has " + currBlock.getEntityCount() + " entity to save.");
		}
	};
}
