package org.example.CIS555_Interface.Handler;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import spark.Request;
import spark.Response;
import spark.Route;

public class HomePageHandler implements Route{
    private final static String homepageStr = "<!DOCTYPE html>\n"
    		+ "<html lang=\"en\">\n"
    		+ "<head>\n"
    		+ "  <meta charset=\"utf-8\">\n"
    		+ "  <title>Penn Search</title>\n"
    		+ "  <!--Google Fonts-->\n"
    		+ "  <link href='https://fonts.googleapis.com/css?family=Monoton' rel='stylesheet' type='text/css'>\n"
    		+ "  <link href='https://fonts.googleapis.com/css?family=Lobster' rel='stylesheet' type='text/css'>\n"
    		+ "  <!--Font Awesome-->\n"
    		+ "  <link href=\"//maxcdn.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.min.css\" rel=\"stylesheet\">\n"
    		+ "  <link rel=\"stylesheet\" type=\"text/css\" href=\"css/homePage.css\">\n"
    		+ "</head>\n"
    		+ "<body>\n"
    		+ "  <div class=\"pat\">\n"
    		+ "    <div class=\"wrapper\">\n"
    		+ "      <div class=\"form\">\n"
    		+ "        <h1>www Search</h1>\n"
    		+ "        <form class=\"index__form\" action=\"/result\" methode=\"GET\">\n"
    		+ "          <input type=\"search\" class=\"search\" name = \"query\" placeholder=\"Search\"></input>\n"
    		+ "          <div class=\"buttons\">\n"
    		+ "            <button class=\"button\" type=\"submit\"><i class=\"fa fa-search \"></i>Search</button>\n"
    		+ "          </div>\n"
    		+ "        </form>\n"
    		+ "        </div>\n"
    		+ "      </div>\n"
    		+ "      <div class=\"footer\">\n"
    		+ "        <div class=\"footer-wrap\">\n"
    		+ "          <ul>\n"
    		+ "            <li><a href=\"#\">Group www</a></li>\n"
    		+ "          </ul>\n"
    		+ "        </div>\n"
    		+ "      </div>\n"
    		+ "    </div>\n"
    		+ "  </div>\n"
    		+ "</body>\n"
    		+ "</html>";

	@Override
	public Object handle(Request request, Response response) throws Exception {
        /*StringBuilder res = new StringBuilder();
        try {
        	File homePage = new File(InterfaceMain.getPagePath() + "homePage.html");
            Scanner myReader = new Scanner(new File(InterfaceMain.getPagePath() + "homePage.html"));
            while (myReader.hasNextLine()) {
                String line = myReader.nextLine();
                res.append(line);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("index.html file problem");
            e.printStackTrace();
        }
        return res.toString();*/
		return homepageStr;
	}
}
