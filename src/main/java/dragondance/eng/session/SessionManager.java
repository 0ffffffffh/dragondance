package dragondance.eng.session;

import java.util.ArrayList;
import java.util.List;

public class SessionManager {
	private static List<Session> sessions;
	private static Session activeSession;
	
	static {
		SessionManager.sessions = new ArrayList<Session>();
	}
	
	public static void registerSession(Session session) {
		sessions.add(session);
		
		if (sessions.size()==1) {
			activeSession=session;
		}
	}
	
	public static void deregisterSession(Session session) {
		
		if (session == null)
			return;
		
		sessions.remove(session);
	}
	
	public static Session getActiveSession() {
		return activeSession;
	}
	
	public static boolean switchSession(String name) {
		return false;
	}
}
