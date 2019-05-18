package dragondance.scripting;

import dragondance.components.GuiAffectedOpInterface;
import dragondance.exceptions.ScriptParserException;
import dragondance.scripting.functions.BuiltinArg;
import dragondance.scripting.functions.BuiltinFunctionBase;


public class ScriptExecutionUnit {
	private ScriptVariable assigneeVar;
	private BuiltinFunctionBase function;
	
	public void initAssigneeVarName(String name) throws ScriptParserException {
		if (DragonDanceScripting.isVariableDeclared(name)) {
			this.assigneeVar = DragonDanceScripting.getVariable(name);
		}
		else {
			this.assigneeVar = new ScriptVariable(name);
		}
	}
	
	public void initFunction(String builtinName, GuiAffectedOpInterface gai) {
		this.function = DragonDanceScripting.newInstance(builtinName, gai);
	}
	
	public void pushArgument(BuiltinArg arg) {
		this.function.putArg(arg);
	}
	
	public boolean hasAssignee() {
		return this.assigneeVar != null;
	}
	
	public boolean hasFunction() {
		return this.function != null;
	}
	
	public boolean isItExpectFunction() {
		return hasAssignee() && !hasFunction();
	}
	
	public boolean isCompleted() {
		return hasAssignee() && hasFunction();
	}
	
	public BuiltinFunctionBase getFunction() {
		return this.function;
	}
	
	public boolean execute() {
		
		if (this.function.execute() == null) {
			
			if (!this.function.hasReturnType())
				return true;
			
			return false;
		}
		
		if (hasAssignee())
			this.assigneeVar.setResultCoverage(this.function.getReturn());
		
		return true;
	}
	
	public void discard() {
		this.function.discard();
		this.function = null;
	}
}
