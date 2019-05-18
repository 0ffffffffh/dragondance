package dragondance.scripting.functions.impl;

import dragondance.scripting.functions.BuiltinAlias;
import dragondance.scripting.functions.BuiltinFunctionBase;

@BuiltinAlias(aliases = {"xor"})
public class BuiltinFunctionDistinct extends BuiltinFunctionBase {

	public BuiltinFunctionDistinct() {
		super("distinct");
	}

}
