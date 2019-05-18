package dragondance.scripting.functions.impl;

import dragondance.datasource.CoverageData;
import dragondance.exceptions.DragonDanceScriptRuntimeException;
import dragondance.scripting.functions.BuiltinFunctionBase;

public class BuiltinFunctionGoto extends BuiltinFunctionBase {

	public BuiltinFunctionGoto() {
		super("goto");
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
		Object arg = super.prepareArguments()[0];
		
		if (arg instanceof Long) {
			guiSvc.goTo(((Long)arg).longValue());
		}
		else
			throw new DragonDanceScriptRuntimeException("invalid arg type for goto");
		
		return super.execute();
	}
	
}
