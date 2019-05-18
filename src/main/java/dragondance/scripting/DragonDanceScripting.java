package dragondance.scripting;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dragondance.Log;
import dragondance.components.GuiAffectedOpInterface;
import dragondance.datasource.CoverageData;
import dragondance.eng.DragonHelper;
import dragondance.exceptions.ScriptParserException;
import dragondance.scripting.functions.BuiltinAlias;
import dragondance.scripting.functions.BuiltinFunctionBase;
import dragondance.scripting.functions.impl.BuiltinFunctionClear;
import dragondance.scripting.functions.impl.BuiltinFunctionCwd;
import dragondance.scripting.functions.impl.BuiltinFunctionDiff;
import dragondance.scripting.functions.impl.BuiltinFunctionDiscard;
import dragondance.scripting.functions.impl.BuiltinFunctionDistinct;
import dragondance.scripting.functions.impl.BuiltinFunctionGoto;
import dragondance.scripting.functions.impl.BuiltinFunctionImport;
import dragondance.scripting.functions.impl.BuiltinFunctionIntersect;
import dragondance.scripting.functions.impl.BuiltinFunctionShow;
import dragondance.scripting.functions.impl.BuiltinFunctionSum;
import dragondance.util.Util;


public class DragonDanceScripting {
	public final static HashMap<String,Class<?>> builtinFunctions;
	public static List<ScriptExecutionUnit> executionUnits;
	public static HashMap<String, ScriptVariable> variables;
	public static String workingDirectory;
	public static String scriptHash = "";
	
	static {
		builtinFunctions = new HashMap<String,Class<?>>();
		executionUnits = new ArrayList<ScriptExecutionUnit>();
		variables = new HashMap<String, ScriptVariable>();
		
		registerBuiltin("intersect", BuiltinFunctionIntersect.class);
		registerBuiltin("diff",BuiltinFunctionDiff.class);
		registerBuiltin("sum",BuiltinFunctionSum.class);
		registerBuiltin("distinct",BuiltinFunctionDistinct.class);
		registerBuiltin("import",BuiltinFunctionImport.class);
		registerBuiltin("cwd",BuiltinFunctionCwd.class);
		registerBuiltin("show",BuiltinFunctionShow.class);
		registerBuiltin("discard",BuiltinFunctionDiscard.class);
		registerBuiltin("goto",BuiltinFunctionGoto.class);
		registerBuiltin("clear",BuiltinFunctionClear.class);
	}
	
	private static void discardExecutionUnits() {
		for (ScriptExecutionUnit seu : executionUnits) 
			seu.discard();
		
		executionUnits.clear();
		
		scriptHash = "";
	}
	
	private static void registerBuiltin(String name, Class<?> clazz) {
		
		BuiltinAlias aliases = clazz.getAnnotation(BuiltinAlias.class);
		
		builtinFunctions.put(name, clazz);
		
		if (aliases != null) {
			for (String alias : aliases.aliases()) {
				builtinFunctions.put(alias,clazz);
			}
		}
	}
	
	public static boolean isBuiltin(String str) {
		return builtinFunctions.containsKey(str.toLowerCase());
	}
	
	public static BuiltinFunctionBase newInstance(String func, GuiAffectedOpInterface guisvc) {
		BuiltinFunctionBase funcImpl;
		Class<?> clazz = null;
		
		func = func.toLowerCase();
		
		if (!builtinFunctions.containsKey(func)) {
			return null;
		}
		
		clazz = builtinFunctions.get(func);
		
		try {
			funcImpl = (BuiltinFunctionBase)clazz.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			Log.println("type activation error: %s",e.getMessage());
			return null;
		}
		
		funcImpl.setGuiApi(guisvc);
		
		return funcImpl;
	}
	
	public static void addExecutionUnit(ScriptExecutionUnit unit) {
		executionUnits.add(unit);
	}
	
	public static void removeVariable(ScriptVariable var) {
		variables.remove(var.getName().toLowerCase());
	}
	
	public static void removeVariableByName(String name) {
		name = name.toLowerCase();
		
		if (!variables.containsKey(name))
			return;
		
		variables.remove(name);
	}
	
	public static boolean addVariable(ScriptVariable var) {
		if (variables.containsKey(var.getName().toLowerCase()))
			return false;
		
		variables.put(var.getName().toLowerCase(), var);
		return true;
	}
	
	public static boolean isVariableDeclared(String varName) {
		return variables.containsKey(varName.toLowerCase());
	}
	
	public static ScriptVariable getVariable(String varName) {
		if (!isVariableDeclared(varName))
			return null;
		
		return variables.get(varName.toLowerCase());
	}
	
	public static void setGuiAffectedInterface(GuiAffectedOpInterface gai) {
		DragonDanceScriptParser.setGuiSvc(gai);
	}
	
	public static void discardScriptingSession(boolean discardVariablesAlso) {
		discardExecutionUnits();
		
		if (discardVariablesAlso) {
			Object[] vars = variables.values().toArray();
			
			//We don't need to clear variables map. cuz discard method removes
			//the scriptvariable from the variable map
			
			for (Object var : vars)
				((ScriptVariable)var).discard();
			
		}
	}
	
	public static boolean execute(String script) {
		
		boolean result = true;
		String shash = Util.md5(script);
		
		if (!shash.equals(scriptHash)) {
			
			discardExecutionUnits();
			
			DragonDanceScriptParser parser = new DragonDanceScriptParser();
			
			try {
				parser.start(script);
			} catch (ScriptParserException e1) {
				DragonHelper.showWarning(e1.getMessage());
				return false;
			}
			
			scriptHash = shash;
		}
		
		try {
			for (ScriptExecutionUnit execUnit : executionUnits) {
				if (!execUnit.execute()) {
					result = false;
					break;
				}
			}
		}
		catch (Exception e) {
			
			DragonHelper.showWarning(e.getMessage());
			
			discardExecutionUnits();
			
			return false;
		}
		
		return result;
	}
	
	public static void setWorkingDirectory(String dir) {
		workingDirectory = dir;
	}
	
	public static void removeCoverage(CoverageData cov) {
		
		if (cov.isLogicalCoverageData())
			return;
		
		DragonDanceScriptParser.getGAI().removeCoverage(cov.getSourceId());
	}
	
}
