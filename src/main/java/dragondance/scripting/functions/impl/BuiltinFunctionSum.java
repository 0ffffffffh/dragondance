package dragondance.scripting.functions.impl;

import dragondance.datasource.CoverageData;
import dragondance.scripting.functions.BuiltinAlias;
import dragondance.scripting.functions.BuiltinFunctionBase;

@BuiltinAlias(aliases = {"or","union"})
public class BuiltinFunctionSum extends BuiltinFunctionBase {

	public BuiltinFunctionSum() {
		super("sum");
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
		
		setReturn(CoverageData.sum(finalArgs));
		
		return super.execute();
	}
}
