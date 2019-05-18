package dragondance.scripting.functions.impl;

import dragondance.datasource.CoverageData;
import dragondance.exceptions.DragonDanceScriptRuntimeException;
import dragondance.scripting.ScriptVariable;
import dragondance.scripting.functions.BuiltinAlias;
import dragondance.scripting.functions.BuiltinFunctionBase;

@BuiltinAlias(aliases = {"del"})
public class BuiltinFunctionDiscard extends BuiltinFunctionBase {

	public BuiltinFunctionDiscard() {
		super("discard");
	}
	
	@Override
	public int requiredArgCount(boolean minimum) {
		if (minimum)
			return 1;
		
		return -1;
	}
	
	@Override
	public boolean hasReturnType() {
		return false;
	}
	
	@Override
	public CoverageData execute() {
		Object[] finalArgs = prepareArguments();
		
		for (Object arg : finalArgs) {
			if (arg instanceof CoverageData) {
				((CoverageData)arg).closeNothrow();
			}
			else if (arg instanceof ScriptVariable) {
				((ScriptVariable)arg).discard(true);
			}
			else
				throw new DragonDanceScriptRuntimeException("invalid arg type for builtin");
			
		}
		
		
		return super.execute();
	}
	

}
