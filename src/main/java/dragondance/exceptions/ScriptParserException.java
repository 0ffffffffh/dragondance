package dragondance.exceptions;

public class ScriptParserException extends Exception {
		
	public ScriptParserException(String msg, Object ...args) {
		super(String.format(msg, args));
	}
	
	
}
