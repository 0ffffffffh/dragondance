package dragondance;

public final class StringResources {
	private static final String nl = System.lineSeparator();
	
	public static final String INVALID_CODE_ADDRESS_FIX_MESSAGE = "%x is valid executable code address but " +
					"ghidra can't recognize it as an address of an instruction. probably due to incorrectly analyze" +
					nl + nl + 
					"but it can be fixed with disassembling this section. you can choose yes to fix, " +
					"or choose no to review yourself. (the address located)";
	
	public static final String MISMATCHED_EXECUTABLE = "possible reasons:" + nl + nl +
					"1) the binary could be different version of the program" + nl + 
					"the MD5 hash of the currently loaded image is \"%s\"" + nl + nl + 
					"2) you forgot to reload the updated binary into ghidra." + nl + nl +
					"3) you tried to load a coverage data to the mismatched program";
	
	
	public static final String ABOUT = "Dragon Dance (" + Globals.version + ")" + nl + nl +
					"by oguz kartal" + nl + 
					"http://oguzkartal.net/";
	
	
	public static final String ATLEAST_2_COVERAGES = "You have to select at least 2 coverages to do this operation";
	
	public static final String COVERAGE_IMPORTED_HINT = "Coverage data imported" + nl +
														"You can select list item" + nl +
														"to see coverage details";
}
