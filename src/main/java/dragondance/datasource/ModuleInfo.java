package dragondance.datasource;

public class ModuleInfo {
	private int id;
	private long base;
	private long end;
	private String path;
	
	public ModuleInfo(int mid, long mBase, long mEnd, String mPath) {
		this.id=mid;
		this.base = mBase;
		this.end = mEnd;
		this.path = mPath;
	}
	
	public static ModuleInfo make(String id, String mbase, String mend, String mpath) {
		ModuleInfo mod = new ModuleInfo(0,0,0,mpath);
		
		
		mod.id = Integer.parseInt(id);
		mod.base = Long.parseLong(mbase.replace("0x", ""), 16);
		mod.end = Long.parseLong(mend.replace("0x",""),16);
		
		return mod;
	}

	public int getId() {
		return id;
	}
	
	public long getBase() {
		return base;
	}

	public long getEnd() {
		return end;
	}

	public String getPath() {
		return path;
	}
	
}
