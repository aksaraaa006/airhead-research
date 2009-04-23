package edu.ucla.sspace.common;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * A utility class for parsing command line arguments
 */
public class ArgOptions {
    
    private final List<String> args;

    private final Map<String,String> options;

    public ArgOptions(String[] commandLineArgs) {
	args = new ArrayList<String>();;
	options = new HashMap<String,String>();
	parseOptions(commandLineArgs);
    }

    private void parseOptions(String[] commandLineArgs) {
	for (int i = 0; i < commandLineArgs.length; ++i) {
	    
	    String arg = commandLineArgs[i];

	    String option = null, value = null;

	    if (arg.startsWith("--")) {

		option = arg.substring(2);
		// check whether the formatting is 
		//   --opt=value
		// or
		//  --opt value
		int index = option.indexOf("=");
		if (index > -1) {
		    // NOTE: use substring instead of split in case the value
		    // also contains an '='
		    value = option.substring(index + 1);
		    option = option.substring(0, index);
		}
		else {
		    int j = i + 1;
		    // if there is another arg for a value, and that arg isn't
		    // itself an option, then use it as the value.
		    if (j <  commandLineArgs.length && 
			    !commandLineArgs[j].startsWith("-")) {
			value = commandLineArgs[j];
			i = j; // skip in th next loop
		    }
		}

		if (options.containsKey(option)) {
		    throw new IllegalArgumentException(
			"redefinition of option " + option);
		}
		options.put(option, value);
	    }
	    else if (arg.startsWith("-")) {
		char[] flags = commandLineArgs[i].toCharArray();
		for (char f : flags) {
		    String flag = String.valueOf(f);
		    if (options.containsKey(flag)) {
			// unsure what to do
		    }
		    option = flag;
		}
		// see if there is a value for the last flag, e.g.
		//   -f value

		int j = i + 1;
		// if there is another arg for a value, and that arg isn't
		// itself an option, then use it as the value.
		if (j <  commandLineArgs.length && !commandLineArgs[j].startsWith("-")) {
		    value = commandLineArgs[j];
		    i = j; // skip in th next loop
		}

		options.put(option, value);
	    }	    
	    else {
		// the string isn't attached to any option, so delcare it an
		// ordered argument
		args.add(arg);
	    }
	}
    }
    
    public int numArgs() {
	return args.size();
    }

    public int numOptions() {
	return options.size();
    }

    public String getArg(int argNum) {
	return args.get(argNum);
    }

    public Integer getIntOption(String key) {
	if (options.containsKey(key)) {
	    try {
		Integer i = Integer.parseInt(options.get(key));
		return i;
	    } catch (NumberFormatException nfe) {
		
	    }
	}
	return null;
    }

    public Boolean getBooleanOption(String key) {
	if (options.containsKey(key)) {
	    try {
		Boolean b = Boolean.parseBoolean(options.get(key));
		return b;
	       
	    } catch (Exception e) {
		// REMINDER: tighten this exception type
	    }
	}
	return null;
    }

    public String getStringOption(String key) {
	return options.get(key);
    }

    public boolean hasOption(String key) {
	return options.containsKey(key);
    }

}