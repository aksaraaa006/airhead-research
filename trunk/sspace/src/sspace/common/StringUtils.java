/*
 * Partially from:
 *
 * © 2007  Réal Gagnon <www.rgagnon.com>
 */

import java.util.*;

/**
 * From <a href="http://www.rgagnon.com/javadetails/java-0307.html">http://www.rgagnon.com/javadetails/java-0307.html</a>
 */
public class StringUtils {


    private StringUtils() {}
  
    private static final HashMap<String,String> htmlEntities;

    static {
	htmlEntities = new HashMap<String,String>();
	htmlEntities.put("&lt;","<")    ; htmlEntities.put("&gt;",">");
	htmlEntities.put("&amp;","&")   ; htmlEntities.put("&quot;","\"");
	htmlEntities.put("&agrave;","à"); htmlEntities.put("&Agrave;","À");
	htmlEntities.put("&acirc;","â") ; htmlEntities.put("&auml;","ä");
	htmlEntities.put("&Auml;","Ä")  ; htmlEntities.put("&Acirc;","Â");
	htmlEntities.put("&aring;","å") ; htmlEntities.put("&Aring;","Å");
	htmlEntities.put("&aelig;","æ") ; htmlEntities.put("&AElig;","Æ" );
	htmlEntities.put("&ccedil;","ç"); htmlEntities.put("&Ccedil;","Ç");
	htmlEntities.put("&eacute;","é"); htmlEntities.put("&Eacute;","É" );
	htmlEntities.put("&egrave;","è"); htmlEntities.put("&Egrave;","È");
	htmlEntities.put("&ecirc;","ê") ; htmlEntities.put("&Ecirc;","Ê");
	htmlEntities.put("&euml;","ë")  ; htmlEntities.put("&Euml;","Ë");
	htmlEntities.put("&iuml;","ï")  ; htmlEntities.put("&Iuml;","Ï");
	htmlEntities.put("&ocirc;","ô") ; htmlEntities.put("&Ocirc;","Ô");
	htmlEntities.put("&ouml;","ö")  ; htmlEntities.put("&Ouml;","Ö");
	htmlEntities.put("&oslash;","ø") ; htmlEntities.put("&Oslash;","Ø");
	htmlEntities.put("&szlig;","ß") ; htmlEntities.put("&ugrave;","ù");
	htmlEntities.put("&Ugrave;","Ù"); htmlEntities.put("&ucirc;","û");
	htmlEntities.put("&Ucirc;","Û") ; htmlEntities.put("&uuml;","ü");
	htmlEntities.put("&Uuml;","Ü")  ; htmlEntities.put("&nbsp;"," ");
	htmlEntities.put("&copy;","\u00a9");
	htmlEntities.put("&reg;","\u00ae");
	htmlEntities.put("&euro;","\u20a0");

	htmlEntities.put("&Agrave;","À");
	htmlEntities.put("&Aacute;","Á");
	htmlEntities.put("&Acirc;","Â");
	htmlEntities.put("&Atilde;","Ã");
	htmlEntities.put("&Auml;","Ä");
	htmlEntities.put("&Aring;","Å");
	htmlEntities.put("&AElig;","Æ");
	htmlEntities.put("&Ccedil;","Ç");
	htmlEntities.put("&Egrave;","È");
	htmlEntities.put("&Eacute;","É");
	htmlEntities.put("&Ecirc;","Ê");
	htmlEntities.put("&Euml;","Ë");
	htmlEntities.put("&Igrave;","Ì");
	htmlEntities.put("&Iacute;","Í");
	htmlEntities.put("&Icirc;","Î");
	htmlEntities.put("&Iuml;","Ï");
	htmlEntities.put("&ETH;","Ð");
	htmlEntities.put("&Ntilde;","Ñ");
	htmlEntities.put("&Ograve;","Ò");
	htmlEntities.put("&Oacute;","Ó");
	htmlEntities.put("&Ocirc;","Ô");
	htmlEntities.put("&Otilde;","Õ");
	htmlEntities.put("&Ouml;","Ö");
	htmlEntities.put("&Oslash;","Ø");
	htmlEntities.put("&Ugrave;","Ù");
	htmlEntities.put("&Uacute;","Ú");
	htmlEntities.put("&Ucirc;","Û");
	htmlEntities.put("&Uuml;","Ü");
	htmlEntities.put("&Yacute;","Ý");
	htmlEntities.put("&THORN;","Þ");
	htmlEntities.put("&szlig;","ß");
	htmlEntities.put("&agrave;","à");
	htmlEntities.put("&aacute;","á");
	htmlEntities.put("&acirc;","â");
	htmlEntities.put("&atilde;","ã");
	htmlEntities.put("&auml;","ä");
	htmlEntities.put("&aring;","å");
	htmlEntities.put("&aelig;","æ");
	htmlEntities.put("&ccedil;","ç");
    htmlEntities.put("&egrave;","è");
    htmlEntities.put("&eacute;","é");
    htmlEntities.put("&ecirc;","ê");
    htmlEntities.put("&euml;","ë");
    htmlEntities.put("&igrave;","ì");
    htmlEntities.put("&iacute;","í");
    htmlEntities.put("&icirc;","î");
    htmlEntities.put("&iuml;","ï");
    htmlEntities.put("&eth;","ð");
    htmlEntities.put("&ntilde;","ñ");
    htmlEntities.put("&ograve;","ò");
    htmlEntities.put("&oacute;","ó");
    htmlEntities.put("&ocirc;","ô");
    htmlEntities.put("&otilde;","õ");
    htmlEntities.put("&ouml;","ö");
    htmlEntities.put("&oslash;","ø");
    htmlEntities.put("&ugrave;","ù");
    htmlEntities.put("&uacute;","ú");
    htmlEntities.put("&ucirc;","û");
    htmlEntities.put("&uuml;","ü");
    htmlEntities.put("&yacute;","ý");
    htmlEntities.put("&thorn;","þ");
    htmlEntities.put("&yuml;","ÿ");    
    }
    
    
    /**
     * Returns the provided string where all HTML special characters
     * (e.g. <pre>&nbsp;</pre>) have been replaced with their utf8 equivalents.
     */
    public static final String unescapeHTML(String source) {
	  return unescapeHTML(source, 0);
    }

    public static final String unescapeHTML(String source, int start){
	int i,j;
	
	i = source.indexOf("&", start);
	if (i > -1) {
	    j = source.indexOf(";" ,i);
	    if (j > i) {
		String entityToLookFor = source.substring(i , j + 1);
		String value = (String)htmlEntities.get(entityToLookFor);
		if (value != null) {
		    source = new StringBuffer().append(source.substring(0 , i))
			.append(value)
			.append(source.substring(j + 1))
			.toString();
		    return unescapeHTML(source, i + 1); // recursive call
		}
	    }
	}
	return source;
    }

}
