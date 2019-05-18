package dragondance;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;

import dragondance.eng.DragonHelper;

public class Log {
	final static int LOGGER_ENABLED = 				1 << 0;
	
	final static int LOGGER_LOG_GHIDRA_CONSOLE = 	1 << 8;
	final static int LOGGER_LOG_FILE =				1 << 9;
	final static int LOGGER_LOG_STDOUT=				1 << 10;
	
	final static int LOGGER_INFO = 					1 << 24;
	final static int LOGGER_DEBUG = 				1 << 25;
	final static int LOGGER_VERBOSE = 				1 << 26;
	
	private static int logFlag=0;
	private static String logFile = "dragondance.log";
	
	private static FileOutputStream fos;
	private static PrintWriter pw;
	
	private static boolean createLogFile() {
		
		if (fos != null)
			return true;
		
		try {
			fos = new FileOutputStream(logFile,true);
		} catch (FileNotFoundException | SecurityException e) {
			return false;
		}
		
		pw = new PrintWriter(fos,true);
		
		pw.println("Logging started at " + LocalDateTime.now().toString());
		
		return true;
	}
	
	private static void closeLogFile() {
		
		if (pw != null)
		{
			pw.close();
			pw=null;
			fos=null;
		}
	}
	
	
	private static void flagSetReset(final int v, boolean enabled) {
		if (enabled)
			logFlag |= v;
		else
			logFlag &= ~v;
	}
	
	private static final boolean hasFlag(final int flag) {
		return (logFlag & flag) == flag;
	}
	
	public static void setEnable(boolean enabled) {
		
		flagSetReset(LOGGER_ENABLED,enabled);
		
		if (!enabled && hasFlag(LOGGER_LOG_FILE))
			enableFileLogging(false);
	}
	
	public static void enableGhidraConsoleLogging(boolean enabled) {
		flagSetReset(LOGGER_LOG_GHIDRA_CONSOLE,enabled);
	}
	
	public static void enableFileLogging(boolean enabled) {
		
		if (enabled) {
			if (!createLogFile())
				return;
		}
		else
			closeLogFile();
		
		flagSetReset(LOGGER_LOG_FILE,enabled);
	}
	
	public static void enableStdoutLogging(boolean enabled) {
		flagSetReset(LOGGER_LOG_STDOUT,enabled);
	}
	
	public static void enableInfo(boolean enable) {
		flagSetReset(LOGGER_INFO,enable);
	}
	
	public static void enableVerbose(boolean enable) {
		flagSetReset(LOGGER_VERBOSE,enable);
	}
	
	public static void enableDebug(boolean enable) {
		flagSetReset(LOGGER_DEBUG,enable);
	}
	
	public static void print(String format, Object... args) {
		String slog;
		
		
		if (!hasFlag(LOGGER_ENABLED))
			return;
		
		//emulate %p formatter in java.
		String pRepl = System.getProperty("os.arch").
				contains("64") ? "0x%016x" : "0x%08x";
		format = format.replace("%p", pRepl);
		
		slog = String.format(format, args);
		
		if (hasFlag(LOGGER_LOG_GHIDRA_CONSOLE))
			DragonHelper.printConsole(slog);
		
		if (hasFlag(LOGGER_LOG_FILE)) {
			pw.print(slog);
			pw.flush();
		}
		
		//Put the stdout (in debug mode this content printed into eclipse console)
		
		if (hasFlag(LOGGER_LOG_STDOUT))
			System.out.print(slog);
		
	}
	
	public static void println(String format, Object... args) {
		print(format + "\n",args);
	}
	
	public static void info(String format, Object... args) {
		
		if (!hasFlag(LOGGER_INFO))
			return;
		
		println(format,args);
	}
	
	public static void verbose(String format, Object... args) {
		
		if (!hasFlag(LOGGER_VERBOSE))
			return;
		
		println(format,args);
	}
	
	public static void debug(String format, Object... args) {
		
		if (!hasFlag(LOGGER_DEBUG))
			return;
		
		println(format,args);
	}
	
	public static boolean isEnabled() {
		return hasFlag(LOGGER_ENABLED);
	}
	
	public static void done() {
		logFlag = 0;
		
		closeLogFile();
	}
	
}
