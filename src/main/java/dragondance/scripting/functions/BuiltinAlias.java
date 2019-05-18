package dragondance.scripting.functions;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

@Retention(RUNTIME)
public @interface BuiltinAlias {
	String[] aliases();
}
