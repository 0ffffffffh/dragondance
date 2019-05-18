package dragondance.eng.session;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import dragondance.Log;
import dragondance.datasource.CoverageData;
import dragondance.datasource.CoverageDataSource;
import dragondance.datasource.DynamorioDataSource;
import dragondance.datasource.PintoolDataSource;
import dragondance.eng.Painter;


public class Session {
		
	private String sessionName;
	private String imageName;
	private int coverageIndex=1;
	
	private List<CoverageData> coverageSources;
	
	private CoverageData activeCoverage=null;
	
	private Painter painter=null;
	
	public static Session createNew(String name, String imageName) {
		Session sess = new Session();
		
		sess.imageName = imageName;
		sess.sessionName = name;
		sess.coverageSources = new ArrayList<CoverageData>();
		
		SessionManager.registerSession(sess);
		
		return sess;
	}
	
	private Class<?> getDatasourceAdapter(int type) {
		switch (type)
		{
		case CoverageDataSource.SOURCE_TYPE_DYNA:
			return DynamorioDataSource.class;
		case CoverageDataSource.SOURCE_TYPE_PINTOOL:
			return PintoolDataSource.class;
		}
		
		return null;
	}
	
	public static String getCoverageTypeString(int typeValue) {
		switch (typeValue) {
		case CoverageDataSource.SOURCE_TYPE_DYNA:
			return "Dynamorio";
		case CoverageDataSource.SOURCE_TYPE_PINTOOL:
			return "Pintool";
		}
		
		return "Unknown";
	}
	
	public static Session open(String sessionDatabase) throws Exception {
		throw new Exception("Session.open not implemented yet.");
	}
	
	
	public CoverageData addCoverageData(String fileName) throws FileNotFoundException {
		int sourceType = CoverageDataSource.detectCoverageDataFileType(fileName);
		
		if (sourceType == -1)
			return null;
		
		Class<?> clazz = getDatasourceAdapter(sourceType);
		CoverageDataSource dataSource;
		CoverageData coverage;
		
		if (clazz == null)
			return null;
		
		
		try {
			dataSource = (CoverageDataSource)clazz.getDeclaredConstructor(String.class, String.class).newInstance(fileName, this.imageName);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {

			Log.println(e.getMessage());
			return null;
		}
		
		dataSource.setId(coverageIndex++);
		
		coverage = new CoverageData(dataSource);
		
		this.coverageSources.add(coverage);
		
		if (this.activeCoverage == null) {
			this.activeCoverage = coverage;
		}
		
		return coverage;
	}
	
	public CoverageData getCoverage(int id) {
		CoverageData covData;
		
		for (int i=0;i<this.coverageSources.size();i++) {
			covData = this.coverageSources.get(i);
			
			if (covData.getSourceId() == id) {
				return covData;
			}
		}
		
		return null;
	}
	
	public boolean removeCoverageData(int id) {
		int index=-1;
		CoverageData covData;
		
		for (int i=0;i<this.coverageSources.size();i++) {
			if (this.coverageSources.get(i).getSourceId() == id) {
				index=i;
				break;
			}
		}
		
		if (index < 0) {
			return false;
		}
		
		covData = this.coverageSources.remove(index);
		
		if (this.activeCoverage == covData) {
			this.activeCoverage.clearPaint();
			this.activeCoverage=null;
		}
		
		covData.closeNothrow();
		
		
		return true;
	}
	
	public CoverageData getActiveCoverage() {
		return this.activeCoverage;
	}
	
	public String getActiveCoverageName() {
		return "";
	}
	
	public boolean setActiveCoverage(int id) {
		CoverageData cov = getCoverage(id);
		
		return setActiveCoverage(cov);
	}
	
	public boolean setActiveCoverage(CoverageData coverage) {
		int oldMode=-1;
		
		if (this.activeCoverage != null) {
			this.activeCoverage.clearPaint();
			
			if (this.activeCoverage.isLogicalCoverageData())
				this.activeCoverage.closeNothrow();
		}
		
		this.activeCoverage = coverage;
		
		if (coverage != null) {
			if (this.activeCoverage.isLogicalCoverageData())
				oldMode = this.painter.setMode(Painter.PAINT_MODE_INTERSECTION);
			
			this.activeCoverage.paint(this.painter);
			
			if (oldMode > -1)
				this.painter.setMode(oldMode);
		}
		
		return true;
	}
	
	public boolean isActiveCoverage(CoverageData coverage) {
		return this.activeCoverage == coverage;
	}
	
	public void setPainter(Painter painter) {
		this.painter = painter;
	}
	
	public String getName() {
		return this.sessionName;
	}
	
	public CoverageData tryGetPreviouslyLoadedCoverage(String fileName) {
		for (CoverageData cov : this.coverageSources) {
			if (!cov.isLogicalCoverageData() && cov.getSourceFilePath().equals(fileName))
				return cov;
		}
		
		return null;
	}
	
	public CoverageData getCoverageByName(String name) {
		for (CoverageData cov : this.coverageSources) {
			if (cov.getName().equals(name)) {
				return cov;
			}
		}
		
		return null;
	}
	
	public void close() throws Exception {
		
		SessionManager.deregisterSession(this);
		
		for (CoverageData cd : this.coverageSources) {
			cd.close();
		}
		
		this.coverageSources.clear();
	}
}
