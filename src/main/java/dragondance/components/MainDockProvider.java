package dragondance.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.FileNotFoundException;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import docking.ActionContext;
import docking.ComponentProvider;
import docking.action.DockingAction;
import docking.action.MenuData;
import docking.action.ToolBarData;
import docking.widgets.table.GTable;
import dragondance.StringResources;
import dragondance.datasource.CoverageData;
import dragondance.datasource.CoverageDataSource;
import dragondance.eng.DragonHelper;
import dragondance.eng.session.Session;
import dragondance.eng.session.SessionManager;
import dragondance.exceptions.InvalidInstructionAddress;
import dragondance.exceptions.OperationAbortedException;
import dragondance.scripting.DragonDanceScripting;
import dragondance.util.TextGraphic;
import dragondance.util.Util;
import ghidra.framework.plugintool.PluginTool;
import resources.Icons;


public class MainDockProvider extends ComponentProvider implements GuiAffectedOpInterface, ComponentListener {

	private JPanel panel = null;
	private JTextArea txtScript = null;
	private JScrollPane scriptTextScrollPane=null;
	private PluginTool tool = null;
	private GTable table = null;
	private DefaultTableModel dtm = null;
	private JLabel infoLabel = null;
	private TextGraphic txtGraph = null;
	private JScrollPane scrollPane = null;
	
	private boolean scriptShown=false;
	
	int infoPanelWidth=425,infoPanelHeight=75;
	int covTableWidth=425,covTableHeight=175;
	
	private int lastX=15,lastY=5;
	
	public MainDockProvider(PluginTool tool,String owner) {
		super(tool, "Dragon Dance", owner);
		
		DragonDanceScripting.setGuiAffectedInterface(this);
		
		this.tool = tool;
		
		createUi();
		createActions();
		
		this.getComponent().addComponentListener(this);
		
	}
	
	private Font getFont(int size, int type, String ...fontNames) {
		Font font;
		
		for (String fontName : fontNames) {
			font = new Font(fontName,type,size);
			
			if (font.getFamily() != null)
				return font;
		}
		
		return this.getComponent().getFont().deriveFont(type, size);
	}
	
	private void createActions() {
		DockingAction actShell = new DockingAction("shell",getName()) {
			
			@Override
			public void actionPerformed(ActionContext context) {
				((MainDockProvider)context.getComponentProvider()).buildScriptingUi();
			}
		};
		

		
		
		DockingAction actAbout = new DockingAction("about",getName()) {

			@Override
			public void actionPerformed(ActionContext context) {
				DragonHelper.showMessage(StringResources.ABOUT);
			}
			
		};
		
		DockingAction actCheckNewVer = new DockingAction("checkupdate",getName()) {
			@Override
			public void actionPerformed(ActionContext context) {
				if (Util.checkForNewVersion()) {
					DragonHelper.showMessage(StringResources.NEW_VERSION);
				}
				else {
					DragonHelper.showMessage(StringResources.UP_TO_DATE);
				}
			}
		};
		
		actShell.setMenuBarData(
				new MenuData(new String[] { "Scripting shell" }, null, null));
		
		actAbout.setMenuBarData(
				new MenuData(new String[] {"About"},null,null));
		
		actCheckNewVer.setMenuBarData(
				new MenuData(new String[] {"Check for update"},null,null));
		
		tool.addLocalAction(this, actShell);
		tool.addLocalAction(this, actAbout);
		tool.addLocalAction(this, actCheckNewVer);
		
		DockingAction actImport = new DockingAction("Import coverage data",getName()) {

			@Override
			public void actionPerformed(ActionContext context) {
				((MainDockProvider)context.getComponentProvider()).importCoverageAsync();
			}
			
		};
		
		actImport.setToolBarData(new ToolBarData(Icons.ADD_ICON, null));
		
		tool.addLocalAction(this, actImport);
		actImport.setEnabled(true);
	}
	
	private String newLine(int count) {
		String nl = "";
		
		while (count-- > 0) {
			nl += System.lineSeparator();
		}
		
		return nl;
	}
	
	private Session getSession() {
		Session session = SessionManager.getActiveSession();
		
		if (session == null) {
			DragonHelper.showWarning("There is no active session");
			return null;
		}
		
		return session;
	}
	
	private void blit() {
		this.infoLabel.repaint();
	}
	
	private void writeStatusTextInfoPanel(String msg) {
		String[] lines = msg.split("\n");
		
		for (String line : lines) {
			txtGraph.textOut(line, Color.BLACK).newLine();
		}
		
		txtGraph.render(true);
		
		blit();
	}
	
	private void setStatusText(String text) {
		if (DragonHelper.isUiDispatchThread())
			this.getTool().setStatusInfo(text);
		else {
			DragonHelper.runOnSwingThread(() -> 
			{
				getTool().setStatusInfo(text);
				},
			true);
		}
	}
	
	private void drawSelectedCoverageDataInfo(int id) {
		
		Session session = getSession();
		
		if (session == null)
			return;
		
		CoverageData cov = session.getCoverage(id);
		CoverageDataSource source = cov.getSource();
		
		txtGraph.pushFont("Arial", 14, Font.PLAIN);
		
		txtGraph.textOut("Module count: ", Color.BLUE).
			textOut("%d", Color.BLACK,source.getModuleCount()).newLine();
		
		txtGraph.textOut("Entry count: ", Color.BLUE).
			textOut("%d", Color.BLACK,source.getEntryCount()).newLine();
		
		txtGraph.textOut("Readed module count: ", Color.BLUE).
			textOut("%d", Color.BLACK,source.getReadedModuleCount()).newLine();
		
		txtGraph.textOut("Readed entry count: ", Color.BLUE).
			textOut("%d", Color.BLACK,source.getReadedEntryCount()).newLine();
		
		txtGraph.floatTextBlock();
		
		
		txtGraph.textOut("Initial range count: ", Color.RED).
			textOut("%d", Color.BLACK,cov.getInitialRangeCount()).newLine();

		txtGraph.textOut("Range count: ", Color.RED).
			textOut("%d", Color.BLACK,cov.getRangeCount()).newLine();
		
		txtGraph.textOut("Merged range count: ", Color.RED).
			textOut("%d", Color.BLACK,cov.getMergedRangeCount()).newLine();
		
		txtGraph.textOut("Max density: ", Color.RED).
			textOut("%d", Color.BLACK,cov.getMaxDensity()).newLine();
		
		txtGraph.render(true);
		
		blit();
		
	}
	
	private void addCoverageTable(CoverageData coverage) {
		dtm.addRow(new Object[] {
				coverage.getSourceId(),
				coverage.getName(),
				Session.getCoverageTypeString(coverage.getSource().getType())
				});
		
		dtm.fireTableDataChanged();
	}
	
	private void removeFromCoverageTable(int id) {
		int row = coverageIdToTableRow(id);
		
		dtm.removeRow(row);
		dtm.fireTableDataChanged();
	}
	
	private CoverageData importCoverage(String coverageFile) throws FileNotFoundException {
		CoverageData coverage;
		Session session = getSession();
		
		if (session == null)
			return null;
		
		coverage = session.addCoverageData(coverageFile);
		
		setStatusText("Loading...");
		
		if (!coverage.getSource().process()) {
			DragonHelper.showWarning("%s could not be processed",coverageFile);
			return null;
		}
		
		try {
			if (coverage.build()) {
				
				Runnable postBuildGuiOp = new Runnable() {
					@Override
					public void run() {
						addCoverageTable(coverage);
						writeStatusTextInfoPanel(StringResources.COVERAGE_IMPORTED_HINT);
						setStatusText("Done");
					}
				};
				
				if (DragonHelper.isUiDispatchThread())
					postBuildGuiOp.run();
				else
					DragonHelper.runOnSwingThread(postBuildGuiOp, true);
	
			}
		} catch (InvalidInstructionAddress e1) {
			
			session.removeCoverageData(coverage.getSourceId());
			
			String msg = e1.getMessage() + newLine(2) + StringResources.MISMATCHED_EXECUTABLE;
			DragonHelper.showWarning(msg, DragonHelper.getExecutableMD5Hash());
			
			session.removeCoverageData(coverage.getSourceId());
			
			return null;
		
		} catch (OperationAbortedException e1) {
			
			DragonHelper.showWarning( 
					"Operation could not be continue. (" +
					e1.getMessage() + ")");
			
			session.removeCoverageData(coverage.getSourceId());
			
			return null;
		}
		
		
		return coverage;
		
	}
	
	
	private void importCoverageAsync() {
		String file = DragonHelper.askFile(tool.getToolFrame(),"Select coverage data", "load it up!");
		
		if (file == null)
			return;
		
		DragonHelper.queuePoolWorkItem(() -> {
			try {
				importCoverage(file);
			} catch (FileNotFoundException e) {
				DragonHelper.showWarning("File not found");
			}
		});
	}
	
	private int coverageIdToTableRow(int id) {
		for (int i=0;i<dtm.getRowCount();i++) {
			if (((Number)dtm.getValueAt(i, 0)).intValue() == id)
				return i;
		}
		
		return -1;
	}
	
	private int getSelectedCoverageId() {
		int selIndex = table.getSelectedRow();
		
		if (selIndex > -1)
		{
			int id = ((Number)dtm.getValueAt(selIndex, 0)).intValue();
			return id;
		}
		
		return 0;
	}
	
	private int[] getSelectedCoverageIds() {
		int [] selIndexes = table.getSelectedRows();
		int [] ids = new int[selIndexes.length];
		int i=0;
		
		if (selIndexes.length > 0) {
			for (int index : selIndexes) {
				ids[i++] = ((Number)dtm.getValueAt(index, 0)).intValue();
			}
		}
		
		return ids;
	}
	
	private void onDeleteCoverageItemClick() {
		Session session = getSession();
		
		if (session == null)
			return;
		
		int[] ids  = getSelectedCoverageIds();
		
		if (ids.length == 0)
			return;
		
		for (int id : ids) {
			if (session.removeCoverageData(id))
				removeFromCoverageTable(id);
		}
		
		
	}
	
	private void onSwitchCoverageItemClick() {
		Session session = null;
		int id = getSelectedCoverageId();
		
		session = getSession();
		
		if (session == null)
			return;
		
		if (id > 0) {
			session.setActiveCoverage(id);
		}
	}
	
	private CoverageData[] getSelectedCoverageObjects() {
		Session session = getSession();
		
		if (session == null)
			return null;
		
		int[] ids = getSelectedCoverageIds();
		
		if (ids.length < 2) {
			DragonHelper.showWarning(StringResources.ATLEAST_2_COVERAGES);
			return null;
		}
		
		CoverageData[] coverages = new CoverageData[ids.length];
		int i=0;
		
		for (int id : ids) {
			coverages[i++] = session.getCoverage(id);
		}
	
		return coverages;
	}
	
	private final static int OMT_INTERSECT=0;
	private final static int OMT_DIFF=1;
	private final static int OMT_DISTINCT=2;
	private final static int OMT_SUM=3;
	
	private void showMultiCoverageOperation(int type) {
		Session session = getSession();
		CoverageData[] coverages = null;
		CoverageData result = null;
		
		if (session == null) {
			return;
		}
		
		coverages = getSelectedCoverageObjects();
		
		switch (type) {
		case OMT_INTERSECT:
			result = CoverageData.intersect(coverages);
			break;
		case OMT_DIFF:
			result = CoverageData.difference(coverages);
			break;
		case OMT_DISTINCT:
			result = CoverageData.distinct(coverages);
			break;
		case OMT_SUM:
			result = CoverageData.sum(coverages);
			break;
		}
		
		if (result != null) {
			session.setActiveCoverage(result);
		}
		
	}
	
	
	private void buildInfoPanel() {
		
		this.txtGraph = new TextGraphic(infoPanelWidth,infoPanelHeight, panel.getBackground());
		
		this.infoLabel = new JLabel() {
			@Override
			public void paintComponent(Graphics g) {
				txtGraph.dispatch(g);
			}
		};
		
		
		this.infoLabel.setBorder(BorderFactory.createCompoundBorder());
		this.infoLabel.setBounds(15, lastY + 10, infoPanelWidth,infoPanelHeight);
		panel.add(this.infoLabel);
		
	}
	
	
	private void buildScriptingUi() {
		
		
		if (this.txtScript == null) {
			
			this.lastX += 15;
			
			this.txtScript = new JTextArea();
			this.txtScript.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
			this.txtScript.setWrapStyleWord(true);
			
			this.scriptTextScrollPane = new JScrollPane(this.txtScript);
			
			this.scriptTextScrollPane.setBounds(this.lastX, 5, getComponent().getWidth() - this.lastX - 20 , this.covTableHeight);
			
			this.txtScript.addKeyListener(new KeyListener() {
	
				@Override
				public void keyTyped(KeyEvent e) {
				}
	
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER && 
							(e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) == InputEvent.ALT_DOWN_MASK) {
						
						
						DragonDanceScripting.execute(txtScript.getText());
						
					}
				}
	
				@Override
				public void keyReleased(KeyEvent e) {
					
				}});
			
			
			Font fnt = null;
			
			fnt = getFont(14,Font.PLAIN,"Consolas","Courier New","Times New Roman");
			
			this.txtScript.setFont(fnt);
			
			this.panel.add(this.scriptTextScrollPane);
			
			this.scriptTextScrollPane.setVisible(true);
			this.txtScript.setVisible(true);
			
			this.getComponent().repaint();
			
			this.scriptShown=true;
		}
		else {
			this.scriptShown = !this.scriptShown;
			
			this.scriptTextScrollPane.setVisible(this.scriptShown);
			
			if (!this.scriptShown)
				DragonDanceScripting.discardScriptingSession(true);
		}
	}
	
	private void buildCoverageListView() {
		
		JPopupMenu contextMenu;
		JMenuItem miDelete,miSwitch;
		JMenuItem miShowIntersected,miShowDifferences,miShowDistinct,miShowSum;
		
		miDelete = new JMenuItem("Delete");
		miSwitch = new JMenuItem("Switch to");
		
		miShowIntersected = new JMenuItem("Intersection");
		miShowDifferences = new JMenuItem("Difference");
		miShowDistinct = new JMenuItem("Distinct");
		miShowSum = new JMenuItem("Sum");
		
		miDelete.addActionListener(e -> {
			onDeleteCoverageItemClick();
		});
		
		miSwitch.addActionListener(e -> {
			onSwitchCoverageItemClick();
		});
		
		miShowIntersected.addActionListener(e -> {
			showMultiCoverageOperation(OMT_INTERSECT);
		});
		
		miShowDifferences.addActionListener(e -> {
			showMultiCoverageOperation(OMT_DIFF);
		});
		
		miShowDistinct.addActionListener(e -> {
			showMultiCoverageOperation(OMT_DISTINCT);
		});
		
		miShowSum.addActionListener(e -> {
			showMultiCoverageOperation(OMT_SUM);
		});
		
		contextMenu = new JPopupMenu();
		
		contextMenu.add(miDelete);
		contextMenu.add(miSwitch);
		contextMenu.add(new JSeparator());
		contextMenu.add(miShowIntersected);
		contextMenu.add(miShowDifferences);
		contextMenu.add(miShowDistinct);
		contextMenu.add(miShowSum);
		
		
		
		
		dtm = new DefaultTableModel();
		
		dtm.addColumn("Coverage Id");
		dtm.addColumn("Name");
		dtm.addColumn("Source type");
		
		
		table = new GTable(dtm);
		table.setComponentPopupMenu(contextMenu);
		
		scrollPane = new JScrollPane(table);

		scrollPane.setBounds(15, lastY, this.covTableWidth, this.covTableHeight);
		table.setBounds(scrollPane.getBounds());
		
		lastY += this.covTableHeight;
		
		table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				
				int id = getSelectedCoverageId();
				
				if (id > 0)
					drawSelectedCoverageDataInfo(id);
			}
			
		});
		
		panel.add(scrollPane);
	}
	
	
	private void createUi() {
		this.panel = new JPanel();
		this.panel.setLayout(null);
		this.panel.setSize(700, 500);
		
		buildCoverageListView();

		buildInfoPanel();
		
		lastX += this.covTableWidth ;
		
		setVisible(true);
		
	}

	@Override
	public JComponent getComponent() {
		return this.panel;
	}
	
	@Override
	public void componentActivated() {
	
	}
	
	@Override
	public void componentHidden() {
	
	}
	
	@Override
	public void componentShown() {
	
	}
	
	//ComponentListener implementations
	
	@Override
	public void componentResized(ComponentEvent e) {
		if (this.scriptShown) {
			Rectangle oldbound;
			oldbound = this.scriptTextScrollPane.getBounds();
			int scriptTextWidth = getComponent().getWidth() - oldbound.x;
			
			scriptTextWidth -= 20;
			
			oldbound.width = scriptTextWidth;
			
			this.scriptTextScrollPane.setBounds(oldbound);
		}
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		
	}

	@Override
	public void componentShown(ComponentEvent e) {
		
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		
	}

	//------------------------------------
	//Gui related operation interface to the dragondance scripting
	@Override
	public CoverageData loadCoverage(String coverageDataFile) throws FileNotFoundException {
		return importCoverage(coverageDataFile);
	}

	@Override
	public boolean removeCoverage(int id) {
		
		Session session = getSession();
		
		if (session == null)
			return false;
		
		if (session.removeCoverageData(id))
			removeFromCoverageTable(id);
		else
			return false;
		
		return true;
	}

	@Override
	public boolean visualizeCoverage(CoverageData coverage) {
		
		Session session = getSession();
		
		if (session == null) {
			return false;
		}
		
		if (coverage == null) {
			session.setActiveCoverage(null);
			return true;
		}
		
		if (coverage.getRangeCount() > 0) {
			return session.setActiveCoverage(coverage);
		}
		
		coverage.closeNothrow();
		
		DragonHelper.showWarning("result coverage empty, so there is no data to show");
		
		return false;
	}
	
	@Override
	public boolean goTo(long offset) {
		boolean success = DragonHelper.goToAddress(DragonHelper.getImageBase().getOffset() + offset);
		
		if (!success) {
			DragonHelper.showWarning("offset 0x%x is not valid",offset);
		}
		
		return success;
	}



}
