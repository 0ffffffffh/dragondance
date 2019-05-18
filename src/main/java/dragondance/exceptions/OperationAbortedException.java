package dragondance.exceptions;

public class OperationAbortedException extends Exception {

	public OperationAbortedException() {
		this("Operation aborted");
	}
	
	public OperationAbortedException(String message) {
		super(message);
	}
	
	
}
