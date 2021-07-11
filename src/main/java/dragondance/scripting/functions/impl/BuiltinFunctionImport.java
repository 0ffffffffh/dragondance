package dragondance.scripting.functions.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import dragondance.datasource.CoverageData;
import dragondance.eng.session.Session;
import dragondance.eng.session.SessionManager;
import dragondance.exceptions.DragonDanceScriptRuntimeException;
import dragondance.scripting.DragonDanceScripting;
import dragondance.scripting.functions.BuiltinAlias;
import dragondance.scripting.functions.BuiltinFunctionBase;


@BuiltinAlias(aliases = { "load","get" })
public class BuiltinFunctionImport extends BuiltinFunctionBase {

	public BuiltinFunctionImport() {
		super("import");
	}
	
	@Override
	public int requiredArgCount(boolean minimum) {
		return 1; 
	}
	
	
	private String prepareFilePath(String inpFile) {
		File file = new File(inpFile);
		
		if (file.exists())
			return inpFile;
		
		if (file.isAbsolute())
			return inpFile;
		
		if (DragonDanceScripting.workingDirectory == null)
			return inpFile;
		
		return Paths.get(DragonDanceScripting.workingDirectory, inpFile).toString();
	}
	
	private CoverageData loadCoverage(String fileOrName) throws FileNotFoundException {
		String prepFile;
		String varName = super.getNameAssigneeVar();
		CoverageData coverage=null;
		Session session = SessionManager.getActiveSession();
		
		if (session == null)
			return null;
		
		if (fileOrName.contains(File.separator) | fileOrName.contains(".")) {
			prepFile = prepareFilePath(fileOrName);
			
			coverage = session.tryGetPreviouslyLoadedCoverage(prepFile);
			
			if (coverage != null)
				return coverage;
			
			return guiSvc.loadCoverage(prepFile, varName);
		}
		
		return session.getCoverageByName(fileOrName);
	}
	
	@Override
	public CoverageData execute() {
		String[] finalArgs = getStringArguments();
		
		CoverageData coverage;
		
		try {
			coverage = loadCoverage(finalArgs[0]);
		} catch (FileNotFoundException e) {
			throw new DragonDanceScriptRuntimeException(String.format("\"%s\" not found", finalArgs[0]));
		}
			
		setReturn(coverage);
		
		return super.execute();
	}
}
