package dragondance.datasource;

import java.io.FileNotFoundException;

/*
 Database file format
 
 */

public class CommonDatabaseDataSource extends CoverageDataSource {

	public CommonDatabaseDataSource(String sourceFile) throws FileNotFoundException {
		super(sourceFile, null,-1);
	}
	
	

}
