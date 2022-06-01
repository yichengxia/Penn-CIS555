package indexer;

public enum SQLQueries {
	ADD_WORD("INSERT INTO INDEX_TABLE (word, docId, tf) " + "VALUES (?, ?, ?);"),
	ADD_BLOCK("INSERT INTO BLOCK_INDEX_TABLE (blockName, timeLastIndexed) VALUES (?, ?);"),
	CHECK_BLOCK_INDEX("SELECT COUNT(*) FROM BLOCK_INDEX_TABLE WHERE blockName = ?;");

	private String query;

	SQLQueries(String query) {
		this.query = query;
	}
	
	public void seyQuery(String query) {
		this.query = query;
	}
	
	public String getQuery() {
		return this.query;
	}
}
