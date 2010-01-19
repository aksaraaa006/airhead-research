/*
 * Copyright 2009 Keith Stevens 
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

package edu.ucla.sspace.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.Iterator;


/**
 * A {@code DirectoryCorpusReader} for the usenet corpus.
 *
 * @author Keith Stevens
 */
public class UsenetCorpusReader extends DirectoryCorpusReader {

    private static final String END_OF_DOCUMENT =
        "---END.OF.DOCUMENT---";

    /**
     * A reader for extracting content from the bloglines corpus.
     */
    private BufferedReader bloglinesReader;

    private long currentTimestamp;

    private final boolean useTimestamps;

    public UsenetCorpusReader(String corpusFileName) {
        this(corpusFileName, false);
    }

    /**
     * Creates a new {@code UsenetCorpusReader} from a given file name.
     */
    public UsenetCorpusReader(String corpusFileName,
                              boolean includeTimestamps) {
        super(corpusFileName);
        useTimestamps = includeTimestamps;
        init();
    }

    /**
     * Sets up a {@code BufferedReader} to read through a single file with
     * multiple blog entries.
     */
    protected void setupCurrentDoc(String currentDocName) {
        try {
            bloglinesReader =
                new BufferedReader(new FileReader(currentDocName));

            // Extract the time stamp of the current doc.  All usenet files are
            // named in the format "text.text.date.txt".  the date portion is
            // formated with YYYYMMDD followed with extra information.
            String[] parsedName = currentDocName.split("\\.");
            String date = parsedName[parsedName.length - 2];
            Calendar calendar = new GregorianCalendar(
                    Integer.parseInt(date.substring(0, 4)),
                    Integer.parseInt(date.substring(4, 6)),
                    Integer.parseInt(date.substring(6, 8)));
            currentTimestamp = calendar.getTimeInMillis();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Iterates over the utterances in a file and appends the words to create a
     * new document.
     */
    protected String advanceInDoc() {
        String line = null;
        StringBuilder content = new StringBuilder();
        try {
            // Read through a single content block, and possibly a the
            // timestamp, to extract a single document.
            while ((line = bloglinesReader.readLine()) != null) {
                if (line.contains(END_OF_DOCUMENT)) {
                    String cleaned = cleanDoc(content.toString());
                    if (useTimestamps)
                        return String.format(
                                "%d %s", currentTimestamp, cleaned);
                    else
                        return cleaned;
                } else
                    content.append(line);
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
        // There was no content left in this document.
        return null;
    }
}
