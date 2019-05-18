package dragondance.scripting.functions;

import dragondance.scripting.ScriptVariable;

public class BuiltinArg {
	
	private Object argContainer;
	
	public BuiltinArg(ScriptVariable var) {
		this.argContainer=var;
	}
	
	public BuiltinArg(BuiltinFunctionBase func) {
		this.argContainer = func;
	}
	
	public BuiltinArg(String sarg) {
		this.argContainer = sarg;
	}
	
	public BuiltinArg(long larg) {
		this.argContainer = larg;
	}
	
	public boolean isBuiltinCall() {
		return this.argContainer instanceof BuiltinFunctionBase;
	}
	
	public boolean isVariable() {
		return this.argContainer instanceof ScriptVariable;
	}
	
	public boolean isString() {
		return this.argContainer instanceof String;
	}
	
	public boolean isInteger() {
		return this.argContainer instanceof Long;
	}
	
	public BuiltinFunctionBase getAsFunction() {
		return (BuiltinFunctionBase)this.argContainer;
	}
	
	public ScriptVariable getAsVariable() {
		return (ScriptVariable)this.argContainer;
	}
	
	public String getAsString() {
		return (String)this.argContainer;
	}
	
	public long getAsLong() {
		return ((Long)this.argContainer).longValue();
	}
}
