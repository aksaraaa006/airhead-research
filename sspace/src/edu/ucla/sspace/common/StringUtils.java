/*
 * Copyright 2009 David Jurgens
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.ucla.sspace.common;

import java.util.HashMap;
import java.util.Map;

/**
 * A collection of static methods for processing text.
 *
 * @author David Jurgens
 */
public class StringUtils {

    /**
     * Uninstantiable
     */ 
    private StringUtils() {}
    
    /**
     * A mapping from HTML codes for escaped special characters to their unicode
     * character equivalents.
     */
    private static final Map<String,String> HTML_CODE_TO_STRING
	= new HashMap<String,String>();

    static {
	HTML_CODE_TO_STRING.put("&Agrave;","À");
	HTML_CODE_TO_STRING.put("&Aacute;","Á");
	HTML_CODE_TO_STRING.put("&Acirc;","Â");
	HTML_CODE_TO_STRING.put("&Atilde;","Ã");
	HTML_CODE_TO_STRING.put("&Auml;","Ä");
	HTML_CODE_TO_STRING.put("&Aring;","Å");
	HTML_CODE_TO_STRING.put("&AElig;","Æ");
	HTML_CODE_TO_STRING.put("&Ccedil;","Ç");
	HTML_CODE_TO_STRING.put("&Egrave;","È");
	HTML_CODE_TO_STRING.put("&Eacute;","É");
	HTML_CODE_TO_STRING.put("&Ecirc;","Ê");
	HTML_CODE_TO_STRING.put("&Euml;","Ë");
	HTML_CODE_TO_STRING.put("&Igrave;","Ì");
	HTML_CODE_TO_STRING.put("&Iacute;","Í");
	HTML_CODE_TO_STRING.put("&Icirc;","Î");
	HTML_CODE_TO_STRING.put("&Iuml;","Ï");
	HTML_CODE_TO_STRING.put("&ETH;","Ð");
	HTML_CODE_TO_STRING.put("&Ntilde;","Ñ");
	HTML_CODE_TO_STRING.put("&Ograve;","Ò");
	HTML_CODE_TO_STRING.put("&Oacute;","Ó");
	HTML_CODE_TO_STRING.put("&Ocirc;","Ô");
	HTML_CODE_TO_STRING.put("&Otilde;","Õ");
	HTML_CODE_TO_STRING.put("&Ouml;","Ö");
	HTML_CODE_TO_STRING.put("&Oslash;","Ø");
	HTML_CODE_TO_STRING.put("&Ugrave;","Ù");
	HTML_CODE_TO_STRING.put("&Uacute;","Ú");
	HTML_CODE_TO_STRING.put("&Ucirc;","Û");
	HTML_CODE_TO_STRING.put("&Uuml;","Ü");
	HTML_CODE_TO_STRING.put("&Yacute;","Ý");
	HTML_CODE_TO_STRING.put("&THORN;","Þ");
	HTML_CODE_TO_STRING.put("&szlig;","ß");
	HTML_CODE_TO_STRING.put("&agrave;","à");
	HTML_CODE_TO_STRING.put("&aacute;","á");
	HTML_CODE_TO_STRING.put("&acirc;","â");
	HTML_CODE_TO_STRING.put("&atilde;","ã");
	HTML_CODE_TO_STRING.put("&auml;","ä");
	HTML_CODE_TO_STRING.put("&aring;","å");
	HTML_CODE_TO_STRING.put("&aelig;","æ");
	HTML_CODE_TO_STRING.put("&ccedil;","ç");
	HTML_CODE_TO_STRING.put("&egrave;","è");
	HTML_CODE_TO_STRING.put("&eacute;","é");
	HTML_CODE_TO_STRING.put("&ecirc;","ê");
	HTML_CODE_TO_STRING.put("&euml;","ë");
	HTML_CODE_TO_STRING.put("&igrave;","ì");
	HTML_CODE_TO_STRING.put("&iacute;","í");
	HTML_CODE_TO_STRING.put("&icirc;","î");
	HTML_CODE_TO_STRING.put("&iuml;","ï");
	HTML_CODE_TO_STRING.put("&eth;","ð");
	HTML_CODE_TO_STRING.put("&ntilde;","ñ");
	HTML_CODE_TO_STRING.put("&ograve;","ò");
	HTML_CODE_TO_STRING.put("&oacute;","ó");
	HTML_CODE_TO_STRING.put("&ocirc;","ô");
	HTML_CODE_TO_STRING.put("&otilde;","õ");
	HTML_CODE_TO_STRING.put("&ouml;","ö");
	HTML_CODE_TO_STRING.put("&oslash;","ø");
	HTML_CODE_TO_STRING.put("&ugrave;","ù");
	HTML_CODE_TO_STRING.put("&uacute;","ú");
	HTML_CODE_TO_STRING.put("&ucirc;","û");
	HTML_CODE_TO_STRING.put("&uuml;","ü");
	HTML_CODE_TO_STRING.put("&yacute;","ý");
	HTML_CODE_TO_STRING.put("&thorn;","þ");
	HTML_CODE_TO_STRING.put("&yuml;","ÿ");    
    }
    
    
    /**
     * Returns the provided string where all HTML special characters
     * (e.g. <pre>&nbsp;</pre>) have been replaced with their utf8 equivalents.
     *
     * @param source a String possibly containing escaped HTML characters
     */
    public static final String unescapeHTML(String source) {

	StringBuilder sb = new StringBuilder(source.length());

	// position markers for the & and ;
	int start = -1, end = -1;
	
	// the end position of the last escaped HTML character
	int last = 0;

	start = source.indexOf("&");
	end = source.indexOf(";", start);
	
	while (start > -1 && end > start) {
	    String encoded = source.substring(start, end + 1);
	    String decoded = HTML_CODE_TO_STRING.get(encoded);
	    if (decoded != null) {
		// append the string containing all characters from the last escaped
		// character to the current one
		sb.append(source.substring(last, start)).append(decoded);
		last = end + 1;
	    }
	    
	    start = source.indexOf("&", start);
	    end = source.indexOf(";", start);
	}
	// if there weren't any substitutions, don't both to create a new String
	if (sb.length() == 0)
	    return source;

	// otherwise finish the substitution by appending all the text from the
	// last substitution until the end of the string
	sb.append(source.substring(last));
	return sb.toString();
    }

  public static boolean isValid(String word) {
    return true;
  }

  public static String cleanup(String word) {
    // remove all non-letter characters
    word = word.replaceAll("\\W", "");
    // make the string lower case
    return word.toLowerCase();
  }
}
