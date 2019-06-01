package dragondance.datasource;

public class ModuleInfo {
	private int id;
	private int c_id;
	private long base;
	private long end;
	private String path;
	
	public ModuleInfo(int mid, long mBase, long mEnd, String mPath) {
		this(mid,0,mBase,mEnd,mPath);
	}
	
	public ModuleInfo(int mid, int cid, long mBase, long mEnd, String mPath) {
		this.id=mid;
		this.c_id = cid;
		this.base = mBase;
		this.end = mEnd;
		this.path = mPath;
	}
	
	public static ModuleInfo make(String id, String cid, String mbase, String mend, String mpath) {
		ModuleInfo mod = new ModuleInfo(-1,-1,0,0,mpath);
		
		
		try
		{
			mod.id = Integer.parseInt(id);
			mod.base = Long.parseLong(mbase.replace("0x", ""), 16);
			mod.end = Long.parseLong(mend.replace("0x",""),16);
			
			if (cid != null && !cid.isEmpty())
				mod.c_id = Integer.parseInt(cid);
		}
		catch (NumberFormatException e) {
			return null;
		}
		
		return mod;
	}
	
	public static ModuleInfo make(String id, String mbase, String mend, String mpath) {
		return make(id,null,mbase,mend,mpath);
	}

	public final boolean hasContainingId() {
		return c_id > -1;
	}
	
	public int getContainingId() {
		return c_id;
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
