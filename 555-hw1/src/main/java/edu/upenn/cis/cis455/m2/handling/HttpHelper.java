package edu.upenn.cis.cis455.m2.handling;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.m2.interfaces.Session;

public class HttpHelper {
    
    final static Logger logger = LogManager.getLogger(HttpHelper.class);

	public static volatile Map<String, Session> idSessionMap = new HashMap<>();

    public static Session getSession(String id) {
        if (id == null) {
            return null;
        }
        Session session = idSessionMap.getOrDefault(id, null);
        if (session == null) {
            return null;
        }
        session.access();
        if (!session.valid()) {
            idSessionMap.remove(session.id());
        }
        return idSessionMap.getOrDefault(id, null);
    }

    public static String createSession() {
        Session session = new HttpSession();
        idSessionMap.put(session.id(), session);
        return session.id();
    }
   
    public static void putSession(String sessionId, Session session) {
        idSessionMap.put(sessionId, session);
    }
}
