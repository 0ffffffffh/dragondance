package dragondance.scripting.functions.impl;

import dragondance.datasource.CoverageData;
import dragondance.scripting.functions.BuiltinAlias;
import dragondance.scripting.functions.BuiltinFunctionBase;

@BuiltinAlias(aliases = {"and"})
public class BuiltinFunctionIntersect extends BuiltinFunctionBase {

	public BuiltinFunctionIntersect() {
		super("intersect");
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
		
		setReturn(CoverageData.intersect(finalArgs));
		
		return super.execute();
	}

}
