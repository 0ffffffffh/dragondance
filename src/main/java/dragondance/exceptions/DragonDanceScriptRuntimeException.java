package dragondance.exceptions;

public class DragonDanceScriptRuntimeException extends RuntimeException {
	public DragonDanceScriptRuntimeException(String message) {
		super("Dragon Dance script runtime error: " + message);
	}
}
