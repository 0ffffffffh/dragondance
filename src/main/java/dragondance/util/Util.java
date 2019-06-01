package dragondance.util;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import dragondance.Globals;
import dragondance.eng.DragonHelper;

public final class Util {
	public static String getObjectNameFromPath(String path) {
		int p1,p2;
		
		p1 = path.lastIndexOf(File.separator);
		p2 = path.lastIndexOf(".");
		
		if (p1 < 0) {
			p1 = 0;
		}
		
		if (p2 < 0) {
			p2 = path.length();
		}
		
		return path.substring(p1+1, p2);
	}
	
	public static String getDirectoryOfFile(String path) {
		File file = new File(path);
		
		if (file.isDirectory())
			return file.getAbsolutePath();
		
		return file.getParent();
	}
	
	private static int parseVersion(String ver) {
		
		if (ver == null)
			return 0;
		
		String[] parts = ver.trim().split("\\.");
		
		if (parts.length==0)
			return 0;
		
		int nver=0;
		
		try {
			nver = Integer.parseInt(parts[0]) << 16;
			
			if (parts.length>1)
				nver |= Integer.parseInt(parts[1]) << 8;
			
			if (parts.length>2)
				nver |= Integer.parseInt(parts[2]);
		}
		catch (NumberFormatException e) {
			return 0;
		}
		
		return nver;
	}
	
	public static boolean checkForNewVersion() {
		String content = 
				DragonHelper.getStringFromURL("https://raw.githubusercontent.com/0ffffffffh/dragondance/master/.version");
		
		if (content == null)
			return false;
		
		int lver,pver;
		
		lver = parseVersion(content);
		pver = parseVersion(Globals.version);
		
		if (lver <= pver)
			return false;
		
		return true;
	}
	
	public static String md5(String sval) {
		return md5(sval.getBytes());
	}
	
	public static String md5(byte[] value) {
		MessageDigest md = null;
		StringBuffer sb = new StringBuffer();
		
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		
		md.update(value);
		byte[] digest = md.digest();
		
		for (byte b : digest) {
			sb.append(String.format("%02x", b));
		}
		
		return sb.toString();
	}
}
