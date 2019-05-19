package dragondance.components;

import java.io.FileNotFoundException;

import dragondance.datasource.CoverageData;

public interface GuiAffectedOpInterface {
	public CoverageData loadCoverage(String coverageDataFile) throws FileNotFoundException;
	public boolean removeCoverage(int id);
	public boolean visualizeCoverage(CoverageData coverage);
	public boolean goTo(long offset);
}
