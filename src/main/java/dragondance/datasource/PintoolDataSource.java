package dragondance.datasource;

import java.io.FileNotFoundException;

import dragondance.Log;

public class PintoolDataSource extends CoverageDataSource {

	public PintoolDataSource(String sourceFile, String mainModule) throws FileNotFoundException {
		super(sourceFile, mainModule,CoverageDataSource.SOURCE_TYPE_PINTOOL);
	}

	private void parseInformation() {
		String line;
		String[] parts;
		
		while ((line = readLine()) != null) {
			if (line.startsWith("EntryCount:")) {
				parts = splitMultiDelim(line,":, ",false);
				this.entryTableSize = Integer.parseInt(parts[1]);
				this.moduleCount = Integer.parseInt(parts[3]);
			}
			else if (line.startsWith("MODULE_TABLE")) {
				readModules();
			}
			else if (line.startsWith("ENTRY_TABLE")) {
				readEntries();
				break;
			}
		}
	}
	
	private void readModules() {		
		String line;
		int readModuleCount=0;
		
		while ((line = readLine()) != null) {
			String[] parts = splitMultiDelim(line,",", true);
			
			ModuleInfo mod = ModuleInfo.make(
					parts[0], 
					parts[1], 
					parts[2], 
					parts[3] );
	
			if (mod == null) {
				Log.warning("Module not parsed for: %s", line);
				continue;
			}
			
			this.pushModule(mod);
			
			if (++readModuleCount == this.moduleCount)
				break;
		}
	}
	
	private void readEntries() {
		BlockEntry entry;
		
		while ((entry = this.readEntryExtended()) != null) {
			this.pushEntry(entry);
		}
	}
	
	@Override
	public boolean process() {
		parseInformation();
		this.processed=true;
		
		return super.process();
	}
	
}
