package dragondance.scripting.functions.impl;

import dragondance.datasource.CoverageData;
import dragondance.scripting.DragonDanceScripting;
import dragondance.scripting.functions.BuiltinFunctionBase;

public class BuiltinFunctionCwd extends BuiltinFunctionBase {

	public BuiltinFunctionCwd() {
		super("cwd");
	}
	
	@Override
	public int requiredArgCount(boolean minimum) {
		return 1;
	}
	
	@Override
	public boolean hasReturnType() {
		return false;
	}
	
	@Override
	public CoverageData execute() {
		String[] finalArgs = getStringArguments();
		
		DragonDanceScripting.setWorkingDirectory(finalArgs[0]);
		
		return super.execute();
	}
	
}
