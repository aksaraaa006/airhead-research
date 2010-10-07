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

package edu.ucla.sspace.dv;

import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyPathAcceptor;

import edu.ucla.sspace.text.IteratorFactory;

import java.util.HashSet;
import java.util.Set;


/**      
 * A {@code DependencyPathAcceptor} that accepts the minimum set of path
 * templates specified by <a
 * href="http://www.nlpado.de/~sebastian/pub/papers/cl07_pado.pdf">Padó and
 * Lapata (2007)</a>.
 *
 * @see MediumMaltTemplateAcceptor
 * @see MaximumMaltTemplateAcceptor
 */
public class MinimumMaltTemplateAcceptor implements DependencyPathAcceptor {

    static final Set<String> MINIMUM_TEMPLATES = new HashSet<String>();

    static {
        // All the different type strings that could represent verbs in the
        // parsed output.
        Set<String> verbTypes = new HashSet<String>();
        verbTypes.add("VVD");
        verbTypes.add("VVG");
        verbTypes.add("VVN");
        verbTypes.add("VVP");
        verbTypes.add("VVZ");
        verbTypes.add("VBG");
        verbTypes.add("VBN");
        verbTypes.add("VBZ");
        verbTypes.add("VHZ");

        Set<String> nounTypes = new HashSet<String>();
        nounTypes.add("NN");
        nounTypes.add("NNS");
        nounTypes.add("NP");

        // Adjective patterns

        MINIMUM_TEMPLATES.add(toPattern("RB", "AMOD", "JJ"));

        for (String noun : nounTypes) {
            // Noun patterns
            MINIMUM_TEMPLATES.add(toPattern(noun, "NMOD", "JJ")); //
            MINIMUM_TEMPLATES.add(toPattern(noun, "NMOD", "IN"));
            MINIMUM_TEMPLATES.add(toPattern(noun, "OBJ", "V"));
            MINIMUM_TEMPLATES.add(toPattern(noun, "PROD", "V"));
            // MINIMUM_TEMPLATES.add(toPattern(noun, "pcomp-n", "Prep"));
            MINIMUM_TEMPLATES.add(toPattern(noun, "SBJ", "A"));
            MINIMUM_TEMPLATES.add(toPattern(noun, "SBJ", "V"));
            
            MINIMUM_TEMPLATES.add(toPattern("IN", "NMOD", noun));
            MINIMUM_TEMPLATES.add(toPattern("TO", "NMOD", noun));
            // MINIMUM_TEMPLATES.add(toPattern("IN", "pcomp-n", noun));
            MINIMUM_TEMPLATES.add(toPattern("JJ", "NMOD", noun));
            
            for (String noun2 : nounTypes) {
                MINIMUM_TEMPLATES.add(toPattern(noun, "COORD", noun2));
                MINIMUM_TEMPLATES.add(toPattern(noun, "PMOD", noun2));
                MINIMUM_TEMPLATES.add(toPattern(noun, "NMOD", noun2));
                MINIMUM_TEMPLATES.add(toPattern(noun, "SBJ", noun2));
            }
        }

        // ??
        //MINIMUM_TEMPLATES.add(toPattern(null, "lex-mod", "V"));

        // Preposition patterns
        MINIMUM_TEMPLATES.add(toPattern("IN", "AMOD", "JJ"));
        MINIMUM_TEMPLATES.add(toPattern("IN", "AMOD", "RB"));
        MINIMUM_TEMPLATES.add(toPattern("IN", "ADV", "IN"));
        MINIMUM_TEMPLATES.add(toPattern("IN", "ADV", "TO"));   
        MINIMUM_TEMPLATES.add(toPattern("TO", "AMOD", "JJ"));
        MINIMUM_TEMPLATES.add(toPattern("TO", "AMOD", "RB"));
        MINIMUM_TEMPLATES.add(toPattern("TO", "ADV", "IN"));
        MINIMUM_TEMPLATES.add(toPattern("TO", "ADV", "TO"));   


        for (String verb : verbTypes) {
            MINIMUM_TEMPLATES.add(toPattern(verb, "ADV", "RB"));
            // MINIMUM_TEMPLATES.add(toPattern(verb, "lex-mod", null));
            MINIMUM_TEMPLATES.add(toPattern(verb, "VMOD", "TO"));
            MINIMUM_TEMPLATES.add(toPattern(verb, "VMOD", "IN"));

            MINIMUM_TEMPLATES.add(toPattern("RB", "AMOD", verb));
            MINIMUM_TEMPLATES.add(toPattern("JJ", "PRD", verb));

            for (String noun : nounTypes) {
                MINIMUM_TEMPLATES.add(toPattern(verb, "OBJ", noun));
                MINIMUM_TEMPLATES.add(toPattern(verb, "PROD", noun));
                MINIMUM_TEMPLATES.add(toPattern(verb, "SBJ", noun));
            }
        }
    };
    
    /**
     * Creates the acceptor with its standard templates
     */
    public MinimumMaltTemplateAcceptor() { }
   
    /**
     * Returns {@code true} if the path matches one of the predefined templates
     *
     * @param path a dependency path
     *
     * @return {@code true} if the path matches a template
     */
    public boolean accepts(DependencyPath path) {
        return acceptsInternal(path);
    }
    
    /**
     * A package-private method that checks whether the path matches any of the
     * predefined templates.  This method is provided so other template classes
     * have access to the accept logic used by this class.
     *
     * @param path a dependency path
     *
     * @return {@code true} if the path matches a template
     */
    static boolean acceptsInternal(DependencyPath path) {
        // Filter out paths that can't match the template due to length
        if (path.length() != 2)
            return false;
        
        // Check that the nodes weren't filtered out.  If so reject the path
        // even if the part of speech and relation text may have matched a
        // template.
        if (path.getNode(0).word().equals(IteratorFactory.EMPTY_TOKEN)
                || path.getNode(0).word().equals(IteratorFactory.EMPTY_TOKEN))
            return false;

        String pos1 = path.getNode(0).pos();
        String rel = path.getRelation(0);
        String pos2 = path.getNode(1).pos();

        return MINIMUM_TEMPLATES.contains(toPattern(pos1, rel, pos2));
    }
    
    /**
     * Returns the pattern string for the provided parts of speech and relation.
     */
    static String toPattern(String pos1, String rel, String pos2) {
        return pos1 + ":" + rel + ":" + pos2;
    }

}