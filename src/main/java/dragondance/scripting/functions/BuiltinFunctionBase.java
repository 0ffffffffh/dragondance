package dragondance.scripting.functions;

import java.util.ArrayList;
import java.util.List;

import dragondance.Log;
import dragondance.components.GuiAffectedOpInterface;
import dragondance.datasource.CoverageData;

public abstract class BuiltinFunctionBase {
	private String name;
	private List<String> aliases;
	
	private List<BuiltinArg> args;
	private CoverageData retVal;
	
	protected GuiAffectedOpInterface guiSvc;
	
	public BuiltinFunctionBase(String name) {
		this.name = name;
		this.args = new ArrayList<BuiltinArg>();
	}
	
	public void putArg(BuiltinArg arg1, BuiltinArg arg2) {
		this.args.add(arg1);
		this.args.add(arg2);
	}
	
	public void putArg(BuiltinArg ...fargs) {
		for (BuiltinArg arg : fargs)
			this.args.add(arg);
	}
	
	public CoverageData getReturn() {
		return this.retVal;
	}
	
	public int argCount() {
		return this.args.size();
	}
	
	protected void addAlias(String alias) {
		if (this.aliases == null)
			this.aliases = new ArrayList<String>();
		
		this.aliases.add(alias);
	}
	
	protected void setReturn(CoverageData cov) {
		this.retVal = cov;
	}
	
	protected CoverageData[] prepareFinalArguments() {
		CoverageData[] covArgs = new CoverageData[this.args.size()];
		int index=0;
		
		for (BuiltinArg arg : this.args) {
			if (arg.isBuiltinCall()) {
				covArgs[index++] = arg.getAsFunction().execute();
			}
			else if (arg.isVariable()) {
				covArgs[index++] = arg.getAsVariable().getValue();
			}
		}
		
		return covArgs;
	}
	
	protected Object[] prepareArguments() {
		Object[] sargs = new Object[this.args.size()];
		int index=0;
		
		for (BuiltinArg arg : this.args) {
			if (arg.isBuiltinCall()) {
				sargs[index++] = arg.getAsFunction().execute();
			}
			else if (arg.isVariable()) {
				sargs[index++] = arg.getAsVariable();
			}
			else if (arg.isInteger()) {
				sargs[index++] = arg.getAsLong();
			}
			else
				sargs[index++] =arg.getAsString();
			
		}
		
		return sargs;
	}
	
	protected String[] getStringArguments() {
		String[] strArgs = new String[this.args.size()];
		int index=0;
		
		for (BuiltinArg arg : this.args) {
			strArgs[index++] = arg.getAsString();
		}
		
		return strArgs;
	}
	
	public List<String> getAliases() {
		return this.aliases;
	}
	
	public boolean hasAlias() {
		return this.aliases != null;
	}
	
	public void setGuiApi(GuiAffectedOpInterface guiSvc) {
		this.guiSvc = guiSvc;
	}
	
	public int requiredArgCount(boolean minimum) {
		return 0;
	}
	
	public boolean hasReturnType() {
		return true;
	}
	
	public CoverageData execute() {
		Log.debug("builtin \"%s\" executed", this.name);
		return this.retVal;
	}
	
	public void discard() {
		if (this.aliases != null)
			this.aliases.clear();
		
		this.args.clear();
	}
}
