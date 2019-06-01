package dragondance.eng;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import docking.widgets.OptionDialog;
import docking.widgets.filechooser.GhidraFileChooser;
import docking.widgets.filechooser.GhidraFileChooserMode;
import dragondance.Globals;
import dragondance.StringResources;
import dragondance.exceptions.InvalidInstructionAddress;
import dragondance.util.Util;
import generic.concurrent.GThreadPool;
import generic.jar.ResourceFile;
import generic.json.JSONError;
import generic.json.JSONParser;
import generic.json.JSONToken;
import ghidra.app.plugin.core.colorizer.ColorizingService;
import ghidra.app.script.GhidraScript;
import ghidra.app.script.GhidraScriptProvider;
import ghidra.app.script.GhidraScriptUtil;
import ghidra.app.script.GhidraState;
import ghidra.app.services.ConsoleService;
import ghidra.app.services.GoToService;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.flatapi.FlatProgramAPI;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressSet;
import ghidra.program.model.listing.CodeUnit;
import ghidra.program.model.listing.Instruction;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.util.Msg;
import ghidra.util.SystemUtilities;
import ghidra.util.task.DummyCancellableTaskMonitor;
import ghidra.util.task.TaskMonitor;


public class DragonHelper {
	private static PluginTool tool = null;
	private static FlatProgramAPI fapi = null;
	private static GThreadPool tpool = null;
	
	
	public static void init(PluginTool pluginTool, FlatProgramAPI api) {
		DragonHelper.tool = pluginTool;
		DragonHelper.fapi = api;
	}
	
	public static int startTransaction(String name) {
		return fapi.getCurrentProgram().startTransaction(name);
	}
	
	public static void finishTransaction(int id, boolean commit) {
		fapi.getCurrentProgram().endTransaction(id, commit);
	}
	
	
	public static boolean runScript(String scriptName, String[] scriptArguments, GhidraState scriptState)
			throws Exception {
		
		//set dummy printwriter to satisfy ghidra scripting api
		
		StringWriter sw = new StringWriter();
		PrintWriter writer = new PrintWriter(sw);
		
		TaskMonitor monitor = new DummyCancellableTaskMonitor();
		
		Path p = Paths.get(
				System.getProperty("user.dir"),
				"Ghidra",
				"Extensions",
				"dragondance",
				"ghidra_scripts",
				scriptName
				);
		
		
		//List<ResourceFile> dirs = GhidraScriptUtil.getScriptSourceDirectories();
		
		ResourceFile scriptSource = new ResourceFile(p.toAbsolutePath().toString());
		
		
		if (scriptSource.exists()) {
			GhidraScriptProvider gsp = GhidraScriptUtil.getProvider(scriptSource);

			if (gsp == null) {
				writer.close();
				throw new IOException("Attempting to run subscript '" + scriptName +
					"': unable to run this script type.");
			}
			
			GhidraScript script = gsp.getScriptInstance(scriptSource, writer);
			script.setScriptArgs(scriptArguments);
			
			script.execute(scriptState, monitor, writer);

		}
		else
		{
			return false;
		}
		
		return true;
	}
	
	public static String getProgramName() {
		return fapi.getCurrentProgram().getDomainFile().getName();
	}
	
	
	public static GThreadPool getTPool() {
		if (tpool == null) {
			tpool = GThreadPool.getPrivateThreadPool("DragonDance");
			tpool.setMaxThreadCount(4);
		}
		
		return tpool;
	}
	
	public static void queuePoolWorkItem(Runnable r) {
		getTPool().submit(r);
	}
	
	public static boolean runOnSwingThread(Runnable r, boolean waitExecution) {
		if (waitExecution) {
			try {
				SwingUtilities.invokeAndWait(r);
			} catch (InvocationTargetException | InterruptedException e) {
				return false;
			}
		}
		else {
			SystemUtilities.runIfSwingOrPostSwingLater(r);
		}
		
		return true;
	}
	
	public static void showMessage(String message, Object...args) {
		if (isUiDispatchThread())
			Msg.showInfo(DragonHelper.class, null, "Dragon Dance", String.format(message, args));
		else
			showMessageOnSwingThread(message,args);
	}
	
	public static void showWarning(String message, Object...args) {
		if (isUiDispatchThread())
			Msg.showWarn(DragonHelper.class, null, "Dragon Dance", String.format(message, args));
		else
			showWarningOnSwingThread(message,args);
	}
	
	public static void showMessageOnSwingThread(String message, Object ...args) {
		
		try {
			SwingUtilities.invokeAndWait(() -> {
				showMessage(message, args);
			});
		} catch (InvocationTargetException | InterruptedException e) {
			
		}
	}
	
	public static void showWarningOnSwingThread(String message, Object ...args) {
		
		try {
			SwingUtilities.invokeAndWait(() -> {
				showWarning(message, args);
			});
		} catch (InvocationTargetException | InterruptedException e) {
			
		}
	}
	
	public static boolean showYesNoMessage(String title, String messageFormat, Object...args) {
		
		String message;
		
		message = String.format(messageFormat, args);
		
		AtomicBoolean yesno = new AtomicBoolean();

		Runnable r = () -> {
			int choice = OptionDialog.showYesNoDialog(null, title, message);
			yesno.set(choice == OptionDialog.OPTION_ONE);
		};

		SystemUtilities.runSwingNow(r);
		
		return yesno.get();
	}
	
	public static void printConsole(String format, Object... args) {
		
		String message = String.format(format, args);
		
		if (tool == null) {
			return;
		}

		ConsoleService console = tool.getService(ConsoleService.class);
		
		if (console == null) {
			return;
		}
		
		console.print(message);
		
	}
	
	public static boolean isValidExecutableSectionAddress(long addr) {
		if (addr < getImageBase().getOffset())
			return false;
		
		if (addr >= getImageEnd().getOffset())
			return false;
		
		return isCodeSectionAddress(addr);
	}
	
	
	public static boolean goToAddress(long addr) {
		GoToService gotoService = tool.getService(GoToService.class);
		
		if (gotoService==null) 
			return false;
		
		if (!isValidExecutableSectionAddress(addr)) {
			showWarning("%x is not valid offset.",addr);
			return false;
		}
		

		if (getInstructionNoThrow(getAddress(addr),true) == null) {
			return false;
		}
		
		return gotoService.goTo(getAddress(addr));
	}
	
	public static Address getAddress(long addrValue) {
		return fapi.toAddr(addrValue);
	}
	
	public static String askFile(Component parent, String title, String okButtonText) {
		
		GhidraFileChooser gfc = new GhidraFileChooser(parent);
		
		if (!Globals.LastFileDialogPath.isEmpty()) {
			File def = new File(Globals.LastFileDialogPath);
			gfc.setSelectedFile(def);
		}
		
		gfc.setTitle(title);
		gfc.setApproveButtonText(okButtonText);
		gfc.setFileSelectionMode(GhidraFileChooserMode.FILES_ONLY);
		
		File file = gfc.getSelectedFile();
		
		if (file == null) {
			return null;
		}
		
		if (!file.exists())
			return null;
		
		Globals.LastFileDialogPath =  Util.getDirectoryOfFile(file.getAbsolutePath());
		
		if (Globals.LastFileDialogPath == null)
			Globals.LastFileDialogPath = System.getProperty("user.dir");
		
		return file.getAbsolutePath();
	}
	
	public static AddressSet makeAddressSet(long addr, int size) {
		Address bAddr, eAddr;
		
		bAddr = fapi.toAddr(addr);
		eAddr = bAddr.add(size);
		
		AddressSet addrSet = new AddressSet();
		addrSet.add(bAddr, eAddr);
		
		return addrSet;
	}
	
	public static String getExecutableMD5Hash() {
		return fapi.getCurrentProgram().getExecutableMD5();
	}
	
	public static Address getImageBase() {
		if (Globals.WithoutGhidra)
			return getAddress(0x10000000);
		
		return fapi.getCurrentProgram().getImageBase();
	}
	
	public static Address getImageEnd() {
		return fapi.getCurrentProgram().getMaxAddress();
	}
	
	public static InstructionContext getInstruction(long addr, boolean throwEx) throws InvalidInstructionAddress {
		return getInstruction(fapi.toAddr(addr),throwEx);
	}
	
	public static InstructionContext getInstruction(Address addr, boolean throwEx) throws InvalidInstructionAddress {
		return getInstruction(addr,throwEx,false);
	}
	
	private static InstructionContext getInstructionNoThrow(Address addr, boolean icall) {
		InstructionContext inst = null;
		
		try {
			inst = getInstruction(addr,false,icall);
		}
		catch (Exception e) {}
		
		return inst;
	}
	
	private static InstructionContext getInstruction(Address addr, boolean throwEx, boolean icall) throws InvalidInstructionAddress {
		InstructionContext ictx;
		
		if (addr == null) 
			return null;
		
		long naddr=addr.getOffset();
		
		Instruction inst = fapi.getInstructionAt(addr);
		CodeUnit cu;
		
		if (inst == null) {
			
			if (icall) {
				
				if (throwEx)
					throw new InvalidInstructionAddress(naddr);
				
				return null;
			}
			
			if (!isCodeSectionAddress(naddr)) {
				
				if (throwEx)
					throw new InvalidInstructionAddress(naddr);
				
				return null;
			}
			else if (isInDisassembledRange(naddr)) {
				//invalid instruction
				if (throwEx)
					throw new InvalidInstructionAddress(naddr);
				
				return null;
			}
			
			DragonHelper.goToAddress(naddr);
			
			boolean choice = DragonHelper.showYesNoMessage("Warning", 
					StringResources.INVALID_CODE_ADDRESS_FIX_MESSAGE,
					naddr);
			
			if (choice) {
				
				if (!disassemble(naddr)) {
					
					if (throwEx)
						throw new InvalidInstructionAddress(naddr);
					
					return null;
				}
				
				
				return getInstruction(addr,throwEx,true);
			}
			
			
			return null;
		}
		
		cu = fapi.getCurrentProgram().getListing().getCodeUnitAt(addr);
		
		ictx = new InstructionContext(inst,cu);
		
		return ictx;
	}
	
	public static boolean isUiDispatchThread() {
		return EventQueue.isDispatchThread();
	}
	
	public static boolean disassemble(long addr) {
		
		boolean result;
		AtomicBoolean ret = new AtomicBoolean();
		
		int transId = startTransaction("DgDisasm");
		
		if (EventQueue.isDispatchThread()) {
			result = fapi.disassemble(getAddress(addr));
		}
		else {
			
			Runnable r = () -> {

					ret.set(fapi.disassemble(getAddress(addr)));

			};
			
			runOnSwingThread(r, true);
			
			result = ret.get();
		}
		
		
		finishTransaction(transId,result);
		
		return result;
	}
	
	public static List<MemoryBlock> getExecutableMemoryBlocks() {
		MemoryBlock[] blocks = fapi.getCurrentProgram().getMemory().getBlocks();
		List<MemoryBlock> memList = new ArrayList<MemoryBlock>();
		
		for (MemoryBlock block : blocks) {
			if (block.isExecute()) {
				memList.add(block);
			}
		}
		
		return memList;
	}
	
	public static boolean isCodeSectionAddress(long addr) {
		//.text, .init .fini __text
		boolean status=false;
		
		List<MemoryBlock> execBlocks = getExecutableMemoryBlocks();
		
		for (MemoryBlock mb : execBlocks) {
			if (addr >= mb.getStart().getOffset() && addr < mb.getEnd().getOffset()) {
				status=true;
				break;
			}
		}
		
		execBlocks.clear();
		
		return status;
	}
	
	public static boolean isInDisassembledRange(long addr) {
		int iMax=15;
		
		Address gaddr;
		InstructionContext inst;
		
		gaddr = getAddress(addr);
		
		inst = getInstructionNoThrow(gaddr,true);
		
		if (inst != null)
			return true;
		
		while (iMax-- > 0) {
			gaddr = gaddr.subtract(1);
			inst = getInstructionNoThrow(gaddr,true);
			
			if (inst != null)
				break;
		}
		
		if (inst == null)
			return false;
		
		gaddr = gaddr.add(inst.getSize());
		
		inst = getInstructionNoThrow(gaddr,true);
		
		if (inst == null)
			return false;
		
		if (addr <= inst.getAddress()) {
			return true;
		}
		
		iMax = 15;
		
		while (iMax-- > 0) {
			gaddr = gaddr.add(inst.getSize());
			
			inst = getInstructionNoThrow(gaddr,true);
			
			if (inst != null) {
				if (inst.getAddress() <= addr) {
					return true;
				}
			}
			else
				break;
		}
		
		
		return false;
	}
	
	public static boolean setInstructionBackgroundColor(long addr, Color color) {
		
		Address ba;
		
		ColorizingService colorService = tool.getService(ColorizingService.class);
		
		if (colorService == null) {
			return false;
		}
		
		ba = getAddress(addr);
		
		colorService.setBackgroundColor(ba, ba, color);
		
		return true;
	}
	
	public static boolean clearInstructionBackgroundColor(long addr) {
		
		Address ba;
		
		ColorizingService colorService = tool.getService(ColorizingService.class);
		
		if (colorService == null) {
			return false;
		}
		
		ba = getAddress(addr);
		
		colorService.clearBackgroundColor(ba, ba);
		
		return true;
	}
	
	public static String getStringFromURL(String url) {
		try {
			URL u = new URL(url);
			
	        StringBuilder sb = new StringBuilder();
	        BufferedReader reader = new BufferedReader(new InputStreamReader(u.openStream()));
	            
	        String nextLine = "";
	        
	        while ((nextLine = reader.readLine()) != null) {
	        	sb.append(nextLine + "\n");
	        }

	        return sb.toString();
			
		} catch (IOException e) {
			
		} 
		
		return null;
	}
	
	public static Object parseJson(String jsonData) {
		char[] cbuf = new char[jsonData.length()];
		Object obj = null;
		JSONParser parser = new JSONParser();
		List<JSONToken> tokens = new ArrayList<JSONToken>();
		jsonData.getChars(0, jsonData.length(), cbuf, 0);
		
		if (parser.parse(cbuf, tokens) != JSONError.JSMN_SUCCESS) {
			return null;
		}
		
		try {
			obj = parser.convert(cbuf, tokens);
		}
		catch (Exception ex) {
			tokens.clear();
			return null;
		}
		
		tokens.clear();
		return obj;
	}
}
