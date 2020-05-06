package dragondance.datasource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import dragondance.Log;
import dragondance.util.Util;

enum ByteMapTypes
{
	Uint32,
	UShort
}

public class CoverageDataSource implements AutoCloseable{
	
	public static final int SOURCE_TYPE_DYNA = 0;
	public static final int SOURCE_TYPE_PINTOOL = 1;
	
	
	protected int moduleCount=0;
	protected int entryTableSize=0;
	
	protected List<ModuleInfo> modules;
	protected List<BlockEntry> entries;
	
	protected String mainModuleName = null;
	protected ModuleInfo mainModule = null;
	
	protected boolean isEof = false;
	protected boolean processed = false;
	private FileInputStream fis = null;
	private ByteBuffer buf = null;
	private String filePath;
	private int id = 0;
	private int type=-1;
	private String name;
	
	public CoverageDataSource(String sourceFile, String mainModule,int type) throws FileNotFoundException {
		this.mainModuleName=mainModule;
		this.type=type;
		
		File file = new File(sourceFile);
		
		if (!file.exists())
			throw new FileNotFoundException(sourceFile);
		
		this.filePath = sourceFile;
		this.name = Util.getObjectNameFromPath(sourceFile);
		
		this.fis = new FileInputStream(file);
		this.buf = ByteBuffer.allocate(1 * 1024 * 1024);
		
		this.modules = new ArrayList<ModuleInfo>();
		this.entries = new ArrayList<BlockEntry>();
		
		readIntoBuffer();
		
	}
	
	public static int detectCoverageDataFileType(String file) throws FileNotFoundException {
		CoverageDataSource cds;
		int type = -1;
		
		cds = new CoverageDataSource(file,null,-1);
		
		String s = cds.readLine();
		
		if (s.startsWith("DDPH-PINTOOL"))
			type = SOURCE_TYPE_PINTOOL;
		else if (s.startsWith("DRCOV VERSION:"))
			type = SOURCE_TYPE_DYNA;
		
		
		try {
			cds.close();
		} catch (Exception e) { } 
		
		return type;
	}
	
	private boolean readIntoBuffer() {
		
		this.buf.clear();
		int readLen=0;
		
		if (this.isEof)
			return false;
		
		try {
			readLen = this.fis.read(this.buf.array(), 0, this.buf.capacity());
			
			if (readLen == -1) {
				this.isEof=true;
				this.fis.close();
				return false;
			}
			
			this.buf.limit(readLen);
			
		} catch (IOException e) {
			Log.println(e.getMessage());
			return false;
		}
		
		return true;
	}
	
	private byte readByte() {
		byte b;
		
		if (this.buf.hasRemaining())
			b = this.buf.get();
		else {
			
			if (!readIntoBuffer())
				return -1;
			
			b = this.buf.get();
		}
		
		return b;
	}
	
	private int readBytes(byte[] rbuf, int bufOffset, int len) {
		
		int readLen=0;
		
		if (!this.buf.hasRemaining()) {
			if (!readIntoBuffer())
				return -1;
		}
		
		if (this.buf.remaining() < len) {
			int rem=this.buf.remaining();
			this.buf.get(rbuf, bufOffset, rem);
			
			readLen = rem;
			
			if (!readIntoBuffer()) {
				return readLen;
			}
			
			readLen += readBytes(rbuf,readLen,len-rem);
			
		}
		else {
			this.buf.get(rbuf,bufOffset,len);
			readLen = len;
		}
		
		
		return readLen;
	}
	
	protected String[] splitMultiDelim(String str, String delims, boolean trimItem) {
		int p=0,slen=str.length();
		String s;
		
		if (slen == 0)
			return null;
		
		ArrayList<String> parts = new ArrayList<String>();
		
		for (int i=0;i<slen;i++) {
			if (delims.indexOf(str.charAt(i)) != -1) {
				
				if (p != i) {
					s = str.substring(p,i);
					
					if (trimItem)
						s = s.trim();
					
					parts.add(s);
					
				}
				p = i + 1;
			}
		}
		
		if (p != slen) {
			s = str.substring(p,slen);
			
			if (trimItem)
				s = s.trim();
			
			parts.add(s);
		}
		
		return parts.toArray(new String[parts.size()]);
	}
	
	protected String readLine() {
		boolean gotLine=false;
		String s = null;
		StringBuilder sb = new StringBuilder();
		byte b = 0;
		
		while (!gotLine) {
			
			b = readByte();
			
			if (b == -1 && this.isEof)
				return null;
			
			if (b == '\n') {
				
				if (sb.length() == 0)
					return "";
				
				if (sb.charAt(sb.length()-1) == '\r') {
					sb.deleteCharAt(sb.length()-1);
				}
				
				s = sb.toString();
				sb.setLength(0);
				
				gotLine=true;
			}
			else
				sb.append((char)b);
			
		}
		
		return s;
	}
	
	//gosh java!, why you have not unsigned types?
	private int b2uint(byte b) {
		return (b) & 0xFF;
	}
	
	private Number[] mapBytesToValues(byte[] bytes, ByteMapTypes... types) {
		Number[] values = new Number[types.length];
		
		int index=0,pos=0;
		
		for (ByteMapTypes  bmt : types) {
			switch (bmt) {
			case Uint32:
				int ival = b2uint(bytes[index]) | b2uint(bytes[index+1]) << 8 | b2uint(bytes[index+2]) << 16 | b2uint(bytes[index+3]) << 24;
				values[pos++] = ival;
				index += 4;
				break;
			case UShort:
				short sval = (short) (b2uint(bytes[index]) | b2uint(bytes[index+1]) << 8);
				values[pos++] = sval;
				index += 2;
				break;
			}
			
		}
		
		return values;
	}
	
	private Number[] readBlockEntryNative(int readSize, ByteMapTypes... mapTypes) {
		int readLen=0;
		byte[] entryBuf = new byte[readSize];
		
		readLen = readBytes(entryBuf,0,readSize);
		
		if (readLen == -1) {
			this.isEof=true;
			return null;
		}
		
		Number[] mapValues = mapBytesToValues(
				entryBuf,
				mapTypes);
		
		return mapValues;
	}
	
	protected BlockEntry readEntry() {

		Number[] mapValues = readBlockEntryNative(8,
				ByteMapTypes.Uint32,ByteMapTypes.UShort, ByteMapTypes.UShort);
		
		if (mapValues == null)
			return null;
		
		
		BlockEntry be = new BlockEntry(
				mapValues[0].intValue(),
				mapValues[1].intValue(),
				mapValues[2].intValue(),
				0);
		
		return be;
	}
	
	protected BlockEntry readEntryExtended() {
		Number[] mapValues = readBlockEntryNative(12,
				ByteMapTypes.Uint32,ByteMapTypes.UShort, ByteMapTypes.UShort, ByteMapTypes.Uint32);
		
		if (mapValues == null) 
			return null;
		
		BlockEntry be = new BlockEntry(
				mapValues[0].intValue(),
				mapValues[1].intValue(),
				mapValues[2].intValue(),
				mapValues[3].intValue());
		
		return be;
	}
	
	protected void pushModule(ModuleInfo mod) {
		if (this.mainModuleName != null && 
				this.mainModule == null && 
				mod.getPath().toLowerCase().endsWith(this.mainModuleName.toLowerCase())
				) 
		{
			this.mainModule = mod;
			
			Log.info("Main module id: %d",this.mainModule.getId());
		}
		
		this.modules.add(mod);
	}
	
	private boolean isMainModuleEntry(BlockEntry entry) {
		final boolean hasCid = this.mainModule.hasContainingId();
		
		if (!hasCid)
			return this.mainModule.getId() == entry.getModuleId();
		
		return this.mainModule.getContainingId() == entry.getModuleId();
	}
	
	protected void pushEntry(BlockEntry entry) {
		if (this.mainModule == null || (this.mainModule != null && isMainModuleEntry(entry))) {
			this.entries.add(entry);
		}
	}
	
	public boolean process() {
		
		if (this.processed) {
			Log.info("%s processed.",this.getClass().getName().replace("DataSource", ""));
			Log.info("Module count: %d, Entry count: %d",this.moduleCount,this.entryTableSize);
			
			return true;
		}
		
		return false;
	}
	
	public final boolean isProcessed() {
		return this.processed;
	}
	
	public int getType() {
		return this.type;
	}
	
	public int getId() {
		return this.id;
	}
	
	public void setId(int cid) {
		if (this.id == 0)
			this.id = cid;
	}
	
	
	@Override
	public void close() throws Exception {
		this.modules.clear();
		this.entries.clear();
		this.fis.close();
	}
	
	public final int getModuleCount() {
		return this.moduleCount;
	}
	
	public final int getReadedModuleCount() {
		return this.modules.size();
	}
	
	public final int getEntryCount() {
		return this.entryTableSize;
	}
	
	public final int getReadedEntryCount() {
		return this.entries.size();
	}
	
	public final int getMainModuleId() {
		if (this.mainModule == null)
			return -1;
		
		return this.mainModule.getId();
	}
	
	public final String getName() {
		return this.name;
	}
	
	public final String getFilePath() {
		return this.filePath;
	}
}
