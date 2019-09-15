package dragondance;

import dragondance.components.MainDockProvider;
import dragondance.eng.DragonHelper;
import dragondance.eng.Painter;
import dragondance.eng.session.Session;
import dragondance.eng.session.SessionManager;
import dragondance.scripting.DragonDanceScripting;
import ghidra.app.plugin.PluginCategoryNames;
import ghidra.app.plugin.ProgramPlugin;
import ghidra.framework.plugintool.*;
import ghidra.framework.plugintool.util.PluginStatus;
import ghidra.program.flatapi.FlatProgramAPI;
import ghidra.program.model.listing.Program;


//@formatter:off
@PluginInfo(
	status = PluginStatus.STABLE,
	packageName = DragondancePluginPackage.NAME,
	category = PluginCategoryNames.MISC,
	shortDescription = "code coverage visualizer",
	description = "this plugin visualizes binary coverage data that are " +
					"collected by Dynamorio or Intel Pin binary instrumentation tools"
)
//@formatter:on
public class DragondancePlugin extends ProgramPlugin {
	FlatProgramAPI api;
	MainDockProvider mainDock;
	
	
	public DragondancePlugin(PluginTool tool) {
		super(tool, true, true);
		
		mainDock = new MainDockProvider(tool,getName());
		
	}

	@Override
	public void init() {
		super.init();	
	}
	
	@Override
	public void programActivated(Program program) {
		super.programActivated(program);
		
		api = new FlatProgramAPI(this.currentProgram);
		
		DragonHelper.init(tool, api);
		
		if (Globals.EnableLogging) {
			Log.setEnable(true);
			
			if (Globals.EnableLoggingFileOutput)
				Log.enableFileLogging(true);
			
			if (Globals.EnableGhidraConsoleOutput)
				Log.enableGhidraConsoleLogging(true);
			
			if (Globals.DebugMode)
				Log.enableDebug(true);
			
			if (Globals.EnableStdoutLog)
				Log.enableStdoutLogging(true);
			
		}
		
		//Multi-session logic implemented but not usable from the GUI.
		//So create a single unnamed session to get things work right.
		Session.createNew("Unnamed session", DragonHelper.getProgramName());
		
		SessionManager.getActiveSession().setPainter(new Painter());
		
	}
	
	@Override
	public void programClosed(Program program) {
		
		DragonDanceScripting.discardScriptingSession(true);
		
		try {
			SessionManager.getActiveSession().close();
		} catch (Exception e) {
			
		}
		
		//TODO: this is temporary. 
		
		super.programClosed(program);
		
	}
	
	@Override
	public void dispose() {
		super.dispose();
		
		Log.done();
		
	}
}
