package dragondance.scripting.functions.impl;

import dragondance.datasource.CoverageData;
import dragondance.scripting.functions.BuiltinFunctionBase;

public class BuiltinFunctionShow extends BuiltinFunctionBase {

	public BuiltinFunctionShow() {
		super("show");
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
		CoverageData[] finalArgs = prepareFinalArguments();
			
		guiSvc.visualizeCoverage(finalArgs[0]);
		
		return super.execute();
	}

}
