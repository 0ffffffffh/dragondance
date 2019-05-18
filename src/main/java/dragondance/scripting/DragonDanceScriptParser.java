package dragondance.scripting;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import dragondance.Log;
import dragondance.components.GuiAffectedOpInterface;
import dragondance.exceptions.ScriptParserException;
import dragondance.scripting.functions.BuiltinArg;
import dragondance.scripting.functions.BuiltinFunctionBase;

class DDSToken {
	public final static int TOK_IDENTIFIER=0;
	public final static int TOK_BUILTIN=1;
	public final static int TOK_ASSIGN=2;
	public final static int TOK_OPEN_PARAN=3;
	public final static int TOK_CLOSE_PARAN=4;
	public final static int TOK_COMMA=5;
	public final static int TOK_STRING=6;
	public final static int TOK_INTEGER=7;
	
	public final static String[] TOK_STRINGS = {
			"Identifier (argument)",
			"Builtin",
			"=",
			"(",
			")",
			",",
			"String",
			"Integer"
	};
	
	private String tok;
	private int type;
	private int line,pos;
	
	public DDSToken(String tok, int tokType, int line, int pos) {
		this.tok = tok;
		this.type = tokType;
		this.line = line;
		this.pos = pos;
	}
	
	public final int getType() {
		return this.type;
	}
	
	public final String getValue() {
		return this.tok;
	}
	
	public final long getAsNumber() {
		try {
			return Long.decode(this.tok).longValue();
		}
		catch (Exception e) {
			Log.println(e.getMessage());
			return 0;
		}
	}
	
	private ScriptParserException buildException(String format, Object ...args) {
		String msg = String.format(format, args);
		
		msg += "\n\n" + String.format("line: %d, position: %d",this.line,this.pos);
		
		return new ScriptParserException(msg);
	}
	
	public ScriptParserException illegalIdentifier() {
		return buildException(this.getValue() + " is an illegal identifier. it may contains invalid char or begins with digit");
	}
	
	public ScriptParserException unexpectedToken(String expected) {
		return buildException("\"%s\": expected but \"%s\" came.",expected,this.getValue());
	}
	
	public ScriptParserException expected(String expect) {
		return buildException("\"%s\" expected.",expect);
	}
	
	
	public ScriptParserException alreadyDeclared(String var) {
		return buildException("\"%s\" already declared");
	}
	
	public ScriptParserException missingArgument() {
		return buildException("builtin functions must have at least 2 arguments");
	}
	
	public ScriptParserException variableNotDeclared() {
		return buildException("\"%s\" is not declared",this.getValue());
	}
	
	public ScriptParserException suspicious(String msg) {
		return buildException("Suspicious behaviour: %s",msg);
	}
	
	@Override
	public String toString() {
		return String.format("tok: %s, type: %d", tok,type);
	}
	
	
}

public class DragonDanceScriptParser {
	
	private static GuiAffectedOpInterface guisvc;
	
	private List<DDSToken> tokens;
	private ListIterator<DDSToken> node = null;
	private ScriptExecutionUnit execUnit = null;
	private Stack<BuiltinFunctionBase> callingStack=null;
	private GuiAffectedOpInterface guiSvc=null;
	private int currLine=0,currPos=0;
	private DDSToken dummy = new DDSToken("",0,0,0);
	
	public static void setGuiSvc(GuiAffectedOpInterface gai) {
		guisvc = gai;
	}
	
	public static GuiAffectedOpInterface getGAI() {
		return guisvc;
	}
	
	public DragonDanceScriptParser() {
		this.tokens = new LinkedList<DDSToken>();
		this.callingStack = new Stack<BuiltinFunctionBase>();
		this.guiSvc = guisvc;
		
	}
	
	private void newExecUnit() {
		
		if (this.execUnit != null)
			DragonDanceScripting.addExecutionUnit(this.execUnit);
		
		this.execUnit = new ScriptExecutionUnit();
	}
	
	private ScriptExecutionUnit getExecUnit() {
		if (this.execUnit == null)
			this.execUnit = new ScriptExecutionUnit();
		
		return this.execUnit;
	}
	
	private void pushCallingStack(BuiltinFunctionBase func) {
		this.callingStack.push(func);
	}
	
	private BuiltinFunctionBase popCallingStack() {
		return this.callingStack.pop();
	}
	
	private void pushToken(String s, int type) {
		int pos = this.currPos;
		
		if (s.length()>1) {
			pos -= s.length();
		}
		
		tokens.add(new DDSToken(s,type,this.currLine,pos));
		
	}
	
	private void pushToken(char c, int type) {
		pushToken(Character.toString(c),type);
	}
	
	
	private boolean isBufferedChar(char c) {
		if (Character.isLetterOrDigit(c))
			return true;
		
		if (c == '-' || c == '+')
			return true;
		
		return false;
	}
	
	private void processStringBufferAsToken(StringBuffer sb, boolean quoteHint) {
		String sval = sb.toString();
		sb.setLength(0);
		
		
		if (DragonDanceScripting.isBuiltin(sval)) {
			pushToken(sval,DDSToken.TOK_BUILTIN);
		}
		else if (quoteHint) {
			pushToken(sval,DDSToken.TOK_STRING);
		}
		else {
			
			try {
				Long.decode(sval);
				pushToken(sval,DDSToken.TOK_INTEGER);
			}
			catch (Exception e) {
				pushToken(sval,DDSToken.TOK_IDENTIFIER);
			}
			
			
		}
		
	}
	
	private boolean tokenize(String script) {
		StringBuffer sbuf = new StringBuffer();
		char c;
		boolean quoteOn=false;
		
		for (int i=0;i<script.length();i++) {
			c = script.charAt(i);
			
			if (!quoteOn && c == '\n') {
				this.currLine++;
				this.currPos=0;
			}
			
			if (quoteOn || isBufferedChar(c)) {
				
				if (quoteOn && c == '\"') {
					processStringBufferAsToken(sbuf,true);
					quoteOn=false;
					
				}
				else
					sbuf.append(c);
			}
			else {
				if (sbuf.length()>0) {
					processStringBufferAsToken(sbuf,false);
				}
				
				if (c == '\"') 
					quoteOn=true;
				else if (c == '(') 
					pushToken(c, DDSToken.TOK_OPEN_PARAN);
				else if (c == ')')
					pushToken(c, DDSToken.TOK_CLOSE_PARAN);
				else if (c == '=')
					pushToken(c, DDSToken.TOK_ASSIGN);
				else if (c == ',')
					pushToken(c, DDSToken.TOK_COMMA);
			}
			
			this.currPos++;
		}
		
		if (sbuf.length() > 0) {
			processStringBufferAsToken(sbuf,quoteOn);
		}
		
		return this.tokens.size() > 0;
	}
	
	
	private String getExpectedTokenStrings(int [] types) {
		String s = "";
		
		if (types.length < 1)
			return "token";
		
		if (types.length == 1)
			return DDSToken.TOK_STRINGS[types[0]];
		
		for (int i=0;i<types.length;i++) {
			s += DDSToken.TOK_STRINGS[i];
			
			if (i != types.length-1) {
				if (i == types.length-2)
					s += " or ";
				else
					s += ", ";
			}
		}
		
		return s;
	}
	
	private DDSToken nextToken(boolean justHint, int ...expected) throws ScriptParserException {
		
		DDSToken tok;
		boolean valid=false;
		
		if (!this.node.hasNext()) {
			if (this.node.hasPrevious()) {
				tok = this.node.previous();
			}
			else
				tok = this.dummy;
			
			throw tok.expected(getExpectedTokenStrings(expected));
		}
		
		tok = this.node.next();
		
		if (expected.length > 0 && !justHint) {
			for (int i=0;i<expected.length;i++) {
				if (tok.getType() == expected[i]) {
					valid=true;
					break;
				}
			}
		}
		else {
			valid=true;
		}
		
		
		if (!valid)
			throw tok.unexpectedToken(getExpectedTokenStrings(expected));
		
		
		return tok;
	}
	
	private boolean isBuiltinSatisfiedByArglist(BuiltinFunctionBase builtin) {
		return builtin.argCount() >= builtin.requiredArgCount(true);
	}
	
	private void handleBuiltinArguments(BuiltinFunctionBase ownerFunc) throws ScriptParserException {
		DDSToken tok;
		boolean done=false;
	
		
		while (!done) {
			
			tok = nextToken(true, 
					DDSToken.TOK_IDENTIFIER, 
					DDSToken.TOK_STRING,
					DDSToken.TOK_INTEGER, 
					DDSToken.TOK_CLOSE_PARAN
					);
			
			if (tok.getType() == DDSToken.TOK_CLOSE_PARAN) {
				if (!isBuiltinSatisfiedByArglist(ownerFunc))
					throw tok.missingArgument();
			}
			else if (tok.getType() != DDSToken.TOK_IDENTIFIER && 
					tok.getType() != DDSToken.TOK_STRING && 
					tok.getType() != DDSToken.TOK_BUILTIN &&
					tok.getType() != DDSToken.TOK_INTEGER) {
				
				throw tok.unexpectedToken("string, integer, coverage variable or builtin call as an argument");
			}
			
			if (tok.getType() == DDSToken.TOK_IDENTIFIER) {
				if (!DragonDanceScripting.isVariableDeclared(tok.getValue()))
					throw tok.variableNotDeclared();
				
				ScriptVariable sv = DragonDanceScripting.getVariable(tok.getValue());
				BuiltinArg arg = new BuiltinArg(sv);
				ownerFunc.putArg(arg);
			}
			else if (tok.getType() == DDSToken.TOK_STRING) {
				BuiltinArg arg = new BuiltinArg(tok.getValue());
				ownerFunc.putArg(arg);
			}
			else if (tok.getType() == DDSToken.TOK_INTEGER) {
				BuiltinArg arg = new BuiltinArg(tok.getAsNumber());
				ownerFunc.putArg(arg);
			}
			else if (tok.getType() == DDSToken.TOK_BUILTIN) { //TOK_BUILTIN
				
				nextToken(false,DDSToken.TOK_OPEN_PARAN);
				
				BuiltinFunctionBase builtin = DragonDanceScripting.newInstance(tok.getValue(),this.guiSvc);
				BuiltinArg arg = new BuiltinArg(builtin);
				ownerFunc.putArg(arg);
				
				//handleBuiltinArgs
				
				if (this.callingStack.size() > 8) {
					throw tok.suspicious(
							String.format("is it really needed nested call depth? (%d)",this.callingStack.size()));
				}
				
				pushCallingStack(builtin);
				
				handleBuiltinArguments(builtin);
				
				nextToken(false,DDSToken.TOK_CLOSE_PARAN);
				
				popCallingStack();
			}
			else if (tok.getType() == DDSToken.TOK_CLOSE_PARAN) {
				node.previous();
				done=true;
				continue;
			}
			
			if (!node.hasNext()) {
				if (!isBuiltinSatisfiedByArglist(ownerFunc)) 
					throw tok.expected(",");
				
				throw tok.expected(")");
			}
			
			tok = node.next();
			
			if (!isBuiltinSatisfiedByArglist(ownerFunc)) {
				if (tok.getType() != DDSToken.TOK_COMMA) {
					if (tok.getType() == DDSToken.TOK_CLOSE_PARAN)
						throw tok.missingArgument();
					
					throw tok.unexpectedToken(",");
				}
			}
			else if (tok.getType() == DDSToken.TOK_CLOSE_PARAN) {
				done=true;
				node.previous();
			}
			
		}
	}
	
	private void handleBuiltinCall(DDSToken tok) throws ScriptParserException {
		
		nextToken(false,DDSToken.TOK_OPEN_PARAN);
		
		//push paran stack
		
		//init execution unit only top builtin function 
		if (this.callingStack.size() == 0)
			getExecUnit().initFunction(tok.getValue(), this.guiSvc);
		
		pushCallingStack(getExecUnit().getFunction());
		
		BuiltinFunctionBase ownerFunc;
		
		ownerFunc = this.callingStack.peek();
		
		handleBuiltinArguments(ownerFunc);
		
		nextToken(false,DDSToken.TOK_CLOSE_PARAN);
		
		newExecUnit();
		
		popCallingStack();
	}
	
	private void handleAssignee(DDSToken tok) throws ScriptParserException {
		DDSToken ntok;
		
		if (Character.isDigit(tok.getValue().charAt(0)))
			throw tok.illegalIdentifier();
		
		if (!node.hasNext())
			throw tok.expected("=");
		
		ntok = node.next();
		
		if (ntok.getType() != DDSToken.TOK_ASSIGN)
			throw ntok.unexpectedToken("=");
		
		getExecUnit().initAssigneeVarName(tok.getValue());
	}
	
	
	private boolean parse() throws ScriptParserException {
	
		DDSToken tok;
		
		node = this.tokens.listIterator();
		
		while (node.hasNext()) {
			tok = node.next();
			
			if (tok.getType() == DDSToken.TOK_IDENTIFIER) {
				handleAssignee(tok);
			}
			else if (tok.getType() == DDSToken.TOK_BUILTIN) {
				handleBuiltinCall(tok);
			}
			
		}
		
		
		return true;
	}
	
	public boolean start(String script) throws ScriptParserException {
		
		if (!tokenize(script))
			return false;
		
		if (!parse())
			return false;
		
		return true;
	}
	
	public void discard() {
		tokens.clear();
		callingStack.clear();
	}
}
