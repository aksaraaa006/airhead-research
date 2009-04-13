package edu.ucla.sspace.mains;

import java.util.HashMap;
import java.util.Map;

/**
 * A utility class for parsing command line arguments
 */
public class ArgOptions {
    
    private final String[] args;

    private final Map<String,String> options;

    public ArgOptions(String[] args) {
	this.args = args;
	options = new HashMap<String,String>();
	parseOptions(args);
    }

    private void parseOptions(String[] args) {
	for (int i = 0; i < args.length; ++i) {
	    
	    String arg = args[i];

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
		    if (j <  args.length && !args[j].startsWith("-")) {
			value = args[j];
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
		char[] flags = args[i].toCharArray();
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
		if (j <  args.length && !args[j].startsWith("-")) {
		    value = args[j];
		    i = j; // skip in th next loop
		}

		
	    }	    
	    else {
		throw new IllegalArgumentException(
		    "Unrecognized argument formatting: " + args[i]);
	    }

	    options.put(option, value);
	}
    }
    
    public int size() {
	return options.size();
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