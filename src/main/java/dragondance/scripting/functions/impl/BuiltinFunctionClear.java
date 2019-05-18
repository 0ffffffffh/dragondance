package dragondance.scripting.functions.impl;

import dragondance.datasource.CoverageData;
import dragondance.scripting.functions.BuiltinFunctionBase;

public class BuiltinFunctionClear extends BuiltinFunctionBase {

	public BuiltinFunctionClear() {
		super("clear");
	}
	
	@Override
	public int requiredArgCount(boolean minimum) {
		return 0;
	}
	
	@Override
	public boolean hasReturnType() {
		return false;
	}
	
	@Override
	public CoverageData execute() {
		guiSvc.visualizeCoverage(null);
		return super.execute();
	}
	
}
