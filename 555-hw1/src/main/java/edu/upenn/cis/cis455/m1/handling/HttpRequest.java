package edu.upenn.cis.cis455.m1.handling;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.m1.server.HttpTask;
import edu.upenn.cis.cis455.m2.interfaces.Request;
import edu.upenn.cis.cis455.m2.interfaces.Session;
import edu.upenn.cis.cis455.m2.server.WebService;

public class HttpRequest extends Request {
    
    static final Logger logger = LogManager.getLogger(HttpRequest.class);

    private Map<String, String> cookies;
    private Map<String, Object> attributes;
    private String queryRaw;
    private String sessionId;

    public static Request parseRequest(HttpTask httpTask, Map<String, String> headers, Map<String, List<String>> params, String uri) {
        Request request = new HttpRequest();
        request.setRequestMethod(headers.get("Method"));
        request.setPort(httpTask.getSocket().getLocalPort());
        request.setPathInfo(httpTask.getDirectory());
        request.setUri(uri);
        request.setHeaders(headers);
        if (headers.get("User-Agent") != null) {
            request.setUserAgent(headers.get("User-Agent"));
        }
        if (headers.get("Content-Length") != null) {
            request.setContentLength(Integer.parseInt(headers.get("Content-Length")));
        }
        if (headers.get("protocolVersion") != null) {
            request.setProtocol(headers.get("protocolVersion"));
        }
        if (headers.get("Content-Type") != null) {
            request.setContentType(headers.get("Content-Type"));
        }
        if (headers.get("Host") != null) {
            request.setHost(headers.get("Host"));
        }
        try {
            String temp_val = uri.substring(uri.lastIndexOf(".") + 1);
            if (temp_val.toLowerCase().equals("html")) {
                request.setContentType("text/html");
            } else if (temp_val.toLowerCase().equals("jpg") || temp_val.toLowerCase().equals("jpeg")) {
                request.setContentType("image/jpeg");
            } else if (temp_val.toLowerCase().equals("txt")) {
                request.setContentType("text/plain");
            }
        } catch (IndexOutOfBoundsException e) {
            logger.info("Index Out Of Bounds Exception: not a needed file.");
        }
        return request;
    }

    @Override
    public String requestMethod() {
        return requestMethod;
    }

    @Override
    public String host() {
        return host;
    }

    @Override
    public String userAgent() {
        return userAgent;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public String pathInfo() {
        return pathInfo;
    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public String uri() {
        return uri;
    }

    @Override
    public String protocol() {
        return protocol;
    }

    @Override
    public String contentType() {
        return contentType;
    }

    @Override
    public String ip() {
        return ip;
    }

    @Override
    public String body() {
        return body;
    }

    @Override
    public int contentLength() {
        return contentLength;
    }

    @Override
    public String headers(String name) {
        return headers.get(name) != null ? headers.get(name) : null;
    }

    @Override
    public Set<String> headers() {
        Set<String> set = new HashSet<>();
        headers.entrySet().forEach(e -> set.add(e.getKey() + ": " + e.getValue()));
        return set;
    }

	@Override
	public Session session() {
		if (sessionId != null) {
            return WebService.getInstance().getSession(sessionId);
        } else if (this.headers.get("cookie") != null){
            String cookie = this.headers.get("cookie");
            this.cookies = parseCookie(cookie);
            this.sessionId = this.cookies.get("JSESSIONID");
            return WebService.getInstance().getSession(sessionId);
        }
        return session(true);
	}

    private Map<String, String> parseCookie(String cookie) {
        Map<String, String> cookies = new HashMap<>();
        String[] segments = cookie.split(";");
        for (String segment : segments) {
            String[] part = segment.split("=");
            if (part.length == 2) {
                cookies.put(part[0].strip(), part[1].strip());
            } else{
                logger.error("Cookie error. Cookie: {}", cookie);
                throw WebService.getInstance().halt(400, "400 Bad Request");
            }
        }
        return cookies;
    }

	@Override
	public Session session(boolean create) {
		if (create) {
            sessionId = WebService.getInstance().createSession();
        }
        return create ? WebService.getInstance().getSession(sessionId) : null;
	}

	@Override
	public Map<String, String> params() {
		return paramMap;
	}

    @Override
    public void setparams(Map<String, String> paramMap) {
        this.paramMap = paramMap;
    }

	@Override
	public String queryParams(String param) {
		if (queryParams.get(param).get(0) == null) {
            logger.error("Invalid parameter for the query");
            return null;
        }
        return queryParams.get(param).get(0);
	}

	@Override
	public List<String> queryParamsValues(String param) {
		return queryParams.get(param);
	}

	@Override
	public Set<String> queryParams() {
		return queryParams.keySet();
	}

	@Override
	public String queryString() {
		return this.queryRaw;
	}

	@Override
	public void attribute(String attrib, Object val) {
		attributes.put(attrib, val);
	}

	@Override
	public Object attribute(String attrib) {
		return attributes.get(attrib);
	}

	@Override
	public Set<String> attributes() {
		return attributes.keySet();
	}

	@Override
	public Map<String, String> cookies() {
		return cookies;
	}

    @Override
    public String getSessionId() {
        return this.sessionId;
    }
}
