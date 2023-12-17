package cis555.storage;

public class pageRankRDSEntity {
	String url;
	double pageRank;
	
	public pageRankRDSEntity(String url, double pageRank) {
		this.url = url;
		this.pageRank = pageRank;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public double getPageRank() {
		return pageRank;
	}

	public void setPageRank(double pageRank) {
		this.pageRank = pageRank;
	}
	
	@Override
	public String toString() {
		return "pageRankEntity: " + "url=" + url + "; pageRank=" + pageRank;
	}
}
