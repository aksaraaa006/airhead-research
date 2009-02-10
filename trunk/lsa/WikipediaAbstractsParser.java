
import java.io.*;
import java.util.*;

public class WikipediaAbstractsParser {

    private static final Map HTML_ENTITIES;
    static {
	HTML_ENTITIES = new HashMap<String,String>();
	HTML_ENTITIES.put("&lt;","<")    ; HTML_ENTITIES.put("&gt;",">");
	HTML_ENTITIES.put("&amp;","&")   ; HTML_ENTITIES.put("&quot;","\"");
	HTML_ENTITIES.put("&agrave;","à"); HTML_ENTITIES.put("&Agrave;","À");
	HTML_ENTITIES.put("&acirc;","â") ; HTML_ENTITIES.put("&auml;","ä");
	HTML_ENTITIES.put("&Auml;","Ä")  ; HTML_ENTITIES.put("&Acirc;","Â");
	HTML_ENTITIES.put("&aring;","å") ; HTML_ENTITIES.put("&Aring;","Å");
	HTML_ENTITIES.put("&aelig;","æ") ; HTML_ENTITIES.put("&AElig;","Æ" );
	HTML_ENTITIES.put("&ccedil;","ç"); HTML_ENTITIES.put("&Ccedil;","Ç");
	HTML_ENTITIES.put("&eacute;","é"); HTML_ENTITIES.put("&Eacute;","É" );
	HTML_ENTITIES.put("&egrave;","è"); HTML_ENTITIES.put("&Egrave;","È");
	HTML_ENTITIES.put("&ecirc;","ê") ; HTML_ENTITIES.put("&Ecirc;","Ê");
	HTML_ENTITIES.put("&euml;","ë")  ; HTML_ENTITIES.put("&Euml;","Ë");
	HTML_ENTITIES.put("&iuml;","ï")  ; HTML_ENTITIES.put("&Iuml;","Ï");
	HTML_ENTITIES.put("&ocirc;","ô") ; HTML_ENTITIES.put("&Ocirc;","Ô");
	HTML_ENTITIES.put("&ouml;","ö")  ; HTML_ENTITIES.put("&Ouml;","Ö");
	HTML_ENTITIES.put("&oslash;","ø") ; HTML_ENTITIES.put("&Oslash;","Ø");
	HTML_ENTITIES.put("&szlig;","ß") ; HTML_ENTITIES.put("&ugrave;","ù");
	HTML_ENTITIES.put("&Ugrave;","Ù"); HTML_ENTITIES.put("&ucirc;","û");
	HTML_ENTITIES.put("&Ucirc;","Û") ; HTML_ENTITIES.put("&uuml;","ü");
	HTML_ENTITIES.put("&Uuml;","Ü")  ; HTML_ENTITIES.put("&nbsp;"," ");
	HTML_ENTITIES.put("&copy;","\u00a9");
	HTML_ENTITIES.put("&reg;","\u00ae");
	HTML_ENTITIES.put("&euro;","\u20a0");
    }

    static final String unescapeHTML(String source, int start){
	int i,j;
	
	i = source.indexOf("&", start);
	if (i > -1) {
	    j = source.indexOf(";" ,i);
	    if (j > i) {
		String entityToLookFor = source.substring(i , j + 1);
		String value = (String)HTML_ENTITIES.get(entityToLookFor);
		if (value != null) {
		    source = new StringBuffer().append(source.substring(0 , i))
			.append(value)
			.append(source.substring(j + 1))
			.toString();
		    return unescapeHTML(source, start + 1); // recursive call
		}
	    }
	}
	return source;
    }


    public static void main(String[] args) {
	if (args.length != 2) {
	    System.out.println("usage java <input-file> <output-dir>");
	    System.exit(0);
	}
	
	try {
	    File outputDir = new File(args[1]);
	    if (!outputDir.isDirectory())
		throw new Error("output directory must be a directory: " + 
				args[1]);
	    BufferedReader br = new BufferedReader(new FileReader(args[0]));
	    String line = null;
	    while ((line = br.readLine()) != null) {
		if (line.startsWith("<title>")) {
		    String rem = line.substring(18);
		    int index = rem.indexOf("<");
		    if (index < 0)
			throw new Error("Malformed title: " + line);
		    String term = rem.substring(0, index);
		    term = term.replaceAll("/","slash");
		    System.out.println(term);
		    File output = new File(outputDir, term + ".document");
		    parseTerm(term, br, output);
		}
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

    static void parseTerm(String term, BufferedReader br, File outputFile) throws Exception {
	String line = null;
	PrintWriter output = new PrintWriter(outputFile);
	output.println(term);
	while ((line = br.readLine()) != null && !line.startsWith("</doc>")) {
	    if (line.startsWith("<abstract>")) {
		int endIndex = line.lastIndexOf("</");
		String htmlText = line.substring(10, endIndex);
		String normalText = unescapeHTML(htmlText,0);
		//System.out.println("\t" + normalText);
		output.println(normalText);
	    }
	    else if (line.startsWith("<sublink")) {
		String rest = line.substring(32);
		int endIndex = rest.indexOf("</anchor>");
		if (endIndex < 0) {
		    // this case occurs when the sublink is an circual link to
		    // the current page
		    continue;
		}
		String sublinkTerm = rest.substring(0,endIndex);
		//System.out.println("\t" + sublinkTerm);
		if (!(sublinkTerm.equals("See also") ||
		      sublinkTerm.equals("Notes") ||
		      sublinkTerm.equals("References") ||
		      sublinkTerm.equals("Citations") ||
		      sublinkTerm.equals("Further Reading") ||
		      sublinkTerm.equals("External links") ||
		      sublinkTerm.equals("Additional Information"))) {

		    output.println(sublinkTerm);
		}
	    }
	}

	output.close();
    }
}
