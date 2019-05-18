package dragondance.exceptions;

public class InvalidInstructionAddress extends Exception {

	public InvalidInstructionAddress(long addr) {
		super(String.format("Invalid instruction address (%x)",addr));
	}
	
}
