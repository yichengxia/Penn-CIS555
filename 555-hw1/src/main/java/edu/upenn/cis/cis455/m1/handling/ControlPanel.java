package edu.upenn.cis.cis455.m1.handling;

import java.util.List;

import edu.upenn.cis.cis455.m1.server.HttpWorker;
import edu.upenn.cis.cis455.m2.server.WebService;

public class ControlPanel {
    
    public static byte[] getControlPanel(){
        List<HttpWorker> httpWorkers = WebService.getInstance().getThreadWorkers();
        String html = "<!DOCTYPE html>\n<html>\n<head>\n<body>\n<h1>Control Panel</h1>\n<h2>by Yicheng Xia</h2>\n" +
            "<ul>%s\n</ul>\n%s\n</body>\n</html>";
        String httpWorkersString = getHttpWorkers(httpWorkers);
        String controlPanel = String.format(html, httpWorkersString, "<a href=\"/shutdown\"><button>Shutdown</button></a>\n");
        return controlPanel.getBytes();
    }

    public static String getHttpWorkers(List<HttpWorker> httpWorkers) {
        StringBuffer sb = new StringBuffer();
        for (HttpWorker httpWorker : httpWorkers) {
            String threadID = httpWorker.getThreadID();
            boolean status = httpWorker.getStatus();
            String statusString = status && httpWorker.getHttpIoHandler() != null ? httpWorker.getHttpIoHandler().getUri() : "ready to work";
            sb.append(String.format("<li>%s -> %s</li>", threadID, statusString));
        }
        return sb.toString();
    }

    public static byte[] get204(){
        String html = "<!DOCTYPE html>\n<html>\n<head>\n<body>\n<h1>404 Not Found</h1>\n<h2>by Yicheng Xia</h2>\n</body>\n</html>";
        return html.getBytes();
    }

    public static byte[] get404(){
        String html = "<!DOCTYPE html>\n<html>\n<head>\n<body>\n<h1>404 Not Found</h1>\n<h2>by Yicheng Xia</h2>\n</body>\n</html>";
        return html.getBytes();
    }

    public static byte[] get500(){
        String html = "<!DOCTYPE html>\n<html>\n<head>\n<body>\n<h1>500 Server Error</h1>\n<h2>by Yicheng Xia</h2>\n</body>\n</html>";
        return html.getBytes();
    }
}
