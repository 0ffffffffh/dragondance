package dragondance.scripting.functions.impl;

import dragondance.datasource.CoverageData;
import dragondance.scripting.functions.BuiltinAlias;
import dragondance.scripting.functions.BuiltinFunctionBase;

@BuiltinAlias(aliases = {"xor"})
public class BuiltinFunctionDistinct extends BuiltinFunctionBase {

	public BuiltinFunctionDistinct() {
		super("distinct");
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
		
		setReturn(CoverageData.distinct(finalArgs));
		
		return super.execute();
	}

	
}
