package dragondance.datasource;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;

import dragondance.Log;

public class DynamorioDataSource extends CoverageDataSource {

	private int drCovVersion;
	private int moduleDefVersion;
	private String[] moduleInfoColumns;
	
	public DynamorioDataSource(String sourceFile, String mainModule) throws FileNotFoundException {
		super(sourceFile, mainModule,CoverageDataSource.SOURCE_TYPE_DYNA);
		
	}
	
	private void parseInformation() {
		String line;
		String[] parts;
		
		while ((line = readLine()) != null) {
			if (line.startsWith("DRCOV VERSION:")) {
				parts = splitMultiDelim(line,": ",false);
				this.drCovVersion = Integer.parseInt(parts[2]);
			}
			else if (line.startsWith("Module Table:")) {
				parts = splitMultiDelim(line,": ,",false);
				this.moduleDefVersion = Integer.parseInt(parts[3]);
				this.moduleCount = Integer.parseInt(parts[5]);
			}
			else if (line.startsWith("Columns:")) {
				parts = splitMultiDelim(line,": ,",false);
				this.moduleInfoColumns = Arrays.copyOfRange(parts, 1, parts.length);
				readModules();
			}
			else if (line.startsWith("BB Table:")) {
				parts = splitMultiDelim(line," ",false);
				this.entryTableSize = Integer.parseInt(parts[2]);
				readEntries();
				break;
			}
		}
	}
	
	private void readModules() {
		
		HashMap<String,Integer> map =
				new HashMap<String,Integer>();
		
		//Map columns name into the index.
		//So drcov file version may vary on different format version.
		//That is generic solution for all
		for (int i=0;i<this.moduleInfoColumns.length;i++) {
			
			switch (this.moduleInfoColumns[i]) {
			case "id":
				map.put("id",i);
				break;
			case "containing_id":
				map.put("containing_id",i);
				break;
			case "base":
			case "start":
				map.put("base", i);
				break;
			case "end":
				map.put("end",i);
				break;
			case "path":
				map.put("path", i);
				break;
			}
			
		}
		
		final boolean hasCid = map.containsKey("containing_id");
		String line;
		int readModuleCount=0;
		ModuleInfo mod=null;
		
		while ((line = readLine()) != null) {
			String[] parts = splitMultiDelim(line,",",true);
			
			if (hasCid) {
				mod = ModuleInfo.make(
						parts[map.get("id")],
						parts[map.get("containing_id")],
						parts[map.get("base")], 
						parts[map.get("end")], 
						parts[map.get("path")]
								);
			}
			else {
				mod = ModuleInfo.make(
						parts[map.get("id")],
						parts[map.get("base")], 
						parts[map.get("end")], 
						parts[map.get("path")]
								);
			}
			
			if (mod == null) {
				Log.warning("module not parsed for: %s", line);
				continue;
			}
			
			this.pushModule(mod);
			
			if (++readModuleCount == this.moduleCount)
				break;
		}
	}
	
	private void readEntries() {
		BlockEntry entry;
		
		while ((entry = this.readEntry()) != null) {
			this.pushEntry(entry);
		}
	}
	
	@Override
	public boolean process() {
		parseInformation();
		this.processed=true;
		
		return super.process();
	}
	
	public int getDrcovVersion() {
		return this.drCovVersion;
	}
	
	public int getFileDefinitionVersion() {
		return this.moduleDefVersion;
	}

}
