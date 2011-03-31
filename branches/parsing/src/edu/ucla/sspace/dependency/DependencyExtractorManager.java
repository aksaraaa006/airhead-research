/*
 * Copyright 2010 David Jurgens 
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

package edu.ucla.sspace.dependency;

import java.util.HashMap;
import java.util.Map;


/**
 * A coordinating utility class for managing all the {@link DependencyExtractor}
 * instances responsible for reading all of the different dependency parsing
 * output formats.  This class provides access to a default {@code
 * DependencyExtractor}, which is the set either explicitly, or is set when the
 * first {@link DependencyExtractor} is added.  This class also provides access
 * to {@link DependencyExtractor} instances by name in the event than a user
 * should need to process a format generated by a specific parser.
 */
public class DependencyExtractorManager {

    // REMDINER: it might be useful to have the Manager create the extractor if
    // it isn't already present, but can be created on the fly.  -david

    /**
     * The default extractor to return.
     */
    private static DependencyExtractor defaultExtractor;

    /**
     * A mapping from the parser name to the {@link DependencyExtractor} can
     * read to the extractor instance.
     */
    private static Map<String,DependencyExtractor> nameToExtractor =
        new HashMap<String,DependencyExtractor>();

    /**
     * Uninstantiable
     */
    private DependencyExtractorManager() { }

    /**
     * Adds the provided {@link DependencyExtractor} to the set of available
     * extractors.  If an existing extractor has the same name, the existing
     * extactor is overwritten.  If no extractors have been added, the provided
     * extractor becomes the default extractor.
     *
     * @param name the name of the parser that generated the parse trees.
     * @param extractor the an extractor instance
     *
     * @throws NullPointerException if either {@code name} or {@code extractor}
     *         are {@code null}.
     */
    public static synchronized void 
            addExtractor(String name, DependencyExtractor extractor) {
        
        addExtractor(name, extractor, defaultExtractor == null);
    }

    /**
     * Adds the provided {@link DependencyExtractor} to the set of available
     * extractors, optionally setting the extractor as the default.  If an
     * existing extractor has the same name, the existing extactor is
     * overwritten.
     *
     * @param name the name of the parser that generated the parse trees.
     * @param extractor the extractor instance
     * @param isDefault {@code true} if this extractor should be the default
     *        returned by this class
     *
     * @throws NullPointerException if either {@code name} or {@code extractor}
     *         are {@code null}.
     */
    public static synchronized void
            addExtractor(String name, DependencyExtractor extractor, 
                         boolean isDefault) {
               
        if (extractor == null)
            throw new NullPointerException("Extractor cannot be null" + name);
        if (name == null)
            throw new NullPointerException("Extractor cannot have null name");
        nameToExtractor.put(name, extractor);
        if (isDefault)
            defaultExtractor = extractor;
    }
    
    /**
     * Returns the extractor with the specified name.  The name typically refers
     * to the parser that generated the output files that the extractor can read.
     *
     * @param name the name associated with an extractor.
     *
     * @throws IllegalArgumentException if no extractor is known for the {@code
     *         name}
     */
    public static synchronized DependencyExtractor getExtractor(String name) {
        DependencyExtractor e = nameToExtractor.get(name);
        if (e == null)
            throw new IllegalArgumentException("No extactor with name " + name);
        return e;
    }

    /**
     * Returns the default extractor used by this manager.
     *
     * @throws IllegalStateException if no default extractor has been set
     */
    public static synchronized DependencyExtractor getDefaultExtractor() {
        if (defaultExtractor == null)
            throw new IllegalStateException("No extractors available");
        return defaultExtractor;
    }
}