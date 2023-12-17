package edu.upenn.cis.cis555.crawler.remote;

import edu.upenn.cis.cis555.packet.ControlPacket;
import spark.Spark;

/**
 * This is the web interface to show crawler list.
 */
public class WebInterface {
    public static void main(String[] args) {
        Master master = new Master();
        Spark.port(8081);
        Spark.get("/", (request, response) -> {
            response.redirect("/crawler/list", 301);
            return null;
        });
        Spark.get("/crawler/list", (request, response) -> {
            String header = "<head><title>Crawler List</title>\r\n"+
            "<style type=\"text/css\">\n"+
            "   body {\n"+
            "    background-color: #E6E6FA;\n"+
            "    opacity:0.7;\n"+
            "   }\n"+
            "   h2 {color:grey; font:26px Lobster;}\n"+
            "</style>\n"+
            "</head>\n";
            StringBuffer sb = new StringBuffer();
            sb.append("<h2>Crawlers Status</h2>");
            sb.append("<table>");
            sb.append("<tr> <th>Client</th> <th>Address</th> <th>Status</th> <th>HTML Num</th><th>Last Update Date</th></tr>");
            long curr = System.nanoTime();
            for (WorkerInfo workerInfo : master.getWorkerInfo()) {
                master.sendControlPacket(workerInfo.name, ControlPacket.reporting);
                sb.append("<tr>");
                sb.append("<td>" + workerInfo.name + "</td>");
                sb.append("<td>" + workerInfo.addr + "</td>");
                sb.append("<td>" + workerInfo.status + "</td>");
                if (workerInfo.stats == null) {
                    sb.append("<td>Nan</td>");
                } else {
                    sb.append("<td>" + workerInfo.stats.counts.stream().mapToLong(Long::longValue).sum() + "</td>");
                }
                sb.append("<td>" + (curr - workerInfo.timestamp) / 1000000000.0 + "s ago</td>");
                sb.append("</tr>");
            }
            sb.append("</table>");
            String statusPage = sb.toString();
            return header + "<body>" + statusPage + "</body>";
        });
    }
}
