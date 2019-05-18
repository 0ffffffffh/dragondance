package dragondance.scripting.functions.impl;

import dragondance.datasource.CoverageData;
import dragondance.scripting.functions.BuiltinFunctionBase;


public class BuiltinFunctionDiff extends BuiltinFunctionBase {

	public BuiltinFunctionDiff() {
		super("diff");
	}
	
	@Override
	public int requiredArgCount(boolean minimum) {
		if (minimum)
			return 2;
		
		return -1;
	}
	
	@Override
	public CoverageData execute() {
		CoverageData[] finalArgs = prepareFinalArguments();
		
		setReturn(CoverageData.difference(finalArgs));
		
		return super.execute();
	}
}
