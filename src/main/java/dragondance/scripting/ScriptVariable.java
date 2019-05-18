package dragondance.scripting;

import dragondance.datasource.CoverageData;
import dragondance.exceptions.ScriptParserException;

public class ScriptVariable {
	
	private String name;
	private CoverageData coverageValue;
	
	public ScriptVariable(String name) throws ScriptParserException {
		this.name = name;
		
		if (!DragonDanceScripting.addVariable(this)) 
			throw new ScriptParserException("%s already declared",name);
	}
	
	public void setResultCoverage(CoverageData coverage) {
		
		if (this.coverageValue != null) {
			if (this.coverageValue.isLogicalCoverageData()) {
				this.coverageValue.closeNothrow();
			}
		}
		
		this.coverageValue = coverage;
	}
	
	public final String getName() {
		return this.name;
	}
	
	public CoverageData getValue() {
		return this.coverageValue;
	}
	
	@Override
	public String toString() {
		return this.name;
	}
	
	public final boolean isNull() {
		return this.coverageValue == null;
	}
	
	public void discard(boolean forceDeletePhysicalCoverage) {
		//dispose only logical result coverage object. 
		//dont touch the physical coverage data
		
		if (this.coverageValue != null) {
			if (this.coverageValue.isLogicalCoverageData())
				this.coverageValue.closeNothrow();
			else if (forceDeletePhysicalCoverage) {
				DragonDanceScripting.removeCoverage(this.coverageValue);
			}
		}
		
		//but the holder variable can be delete in any case
		DragonDanceScripting.removeVariable(this);
	}
	
	public void discard() {
		discard(false);
	}
}