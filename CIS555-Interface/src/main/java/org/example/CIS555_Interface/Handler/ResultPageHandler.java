package org.example.CIS555_Interface.Handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;


import org.example.CIS555_Interface.Interface.*;
import spark.Request;
import spark.Response;
import spark.Route;

public class ResultPageHandler implements Route {
	@Override
	public Object handle(Request request, Response response) throws Exception {
		System.out.println("in result page handler");
		String res = "";
		String query = request.queryParams("query");
		String pagenum = request.queryParams("pageNum");
		int currPage = 1;
		if (pagenum != null) {
			currPage = Integer.parseInt(pagenum);
		}

		if (query == null || query.length() == 0) {
			response.redirect("/");
			return "";
		}
		SearchEngine engine = new SearchEngine(query);
		try {
			File myObj = new File(InterfaceMain.getPagePath() + "resultPage.html");
			Scanner myReader = new Scanner(myObj);

			int resultPageNum = 0;
			// inserting dynamic contents to the page
			while (myReader.hasNextLine()) {
				String data = myReader.nextLine();
				
				if (data.trim().equals("^&*for_search^&*")) {
					//get search result
					data = engine.searchQuery(currPage);
					resultPageNum = engine.getTotalPageNum();
				} else if (data.trim().equals("^&*for_page_number^&*")) {
					System.out.println("total num of result page is: " + resultPageNum);
					System.out.println("target page is: " + currPage);
					StringBuilder sb = new StringBuilder();
					int startPageNum = currPage - 2 < 1 ? 1 : currPage - 2;
					int endPageNum = 0;
					
					sb.append("  <div class=\"pagination\">\r\n" + "       <ul>\r\n");
					if (currPage == 1) {
						sb.append("<li><a class=\"disabled\"><<</a></li>\r\n");
						sb.append("<li><a class=\"disabled\"><</a></li>\r\n");
					} else {
						sb.append("<li><a class=\"active\" href=\"\\result?query=" + query + "&pageNum=" + 1
								+ "\"><<</a></li>");
						sb.append("<li><a class=\"active\" href=\"\\result?query=" + query + "&pageNum="
								+ (currPage - 1) + "\"><</a></li>");
					}
					
					//show 5 candidate pages
					if (startPageNum == 1) {
						endPageNum = Math.min(5, resultPageNum);
					} else {// start = page number - 2
						endPageNum = Math.min(currPage + 2, resultPageNum);
					}
					for (int i = startPageNum; i <= endPageNum; i++) {
						if (i == currPage) {
							sb.append("<li><a class=\"pagination-active\" href=\"" + "\\result?query=" + query
									+ "&pageNum=" + i + "\">" + i + "</a></li>\r\n");
						} else {
							sb.append("<li><a href=\"" + "\\result?query=" + query + "&pageNum=" + i + "\">" + i
									+ "</a></li>\r\n");
						}
					}
					if (currPage == resultPageNum) {
						sb.append("<li><a class=\"disabled\">></a></li>\r\n");
						sb.append("<li><a class=\"disabled\">>></a></li>\r\n");
					} else {
						sb.append("<li><a class=\"active\" href=\"" + "\\result?query=" + query + "&pageNum="
								+ (currPage + 1) + "\">></a></li>\r\n");
						sb.append("<li><a class=\"active\" href=\"" + "\\result?query=" + query + "&pageNum="
								+ resultPageNum + "\">>></a></li>\r\n");
					}
					sb.append("</ul>\r\n</div>");
					data = sb.toString();
				} else if (data.trim().equals("^&*for_input^&*")) {
					data = "<input name=\"query\" type=\"search\" class=\"query\" maxlength=\"512\" autocomplete=\"off\" title=\"Search\" aria-label=\"Search\" dir=\"ltr\" spellcheck=\"false\" autofocus=\"autofocus\" placeholder=\""
							+ query + "\"></input>";
				}
				res += data;
			}
			myReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
		return res;
	}

}
