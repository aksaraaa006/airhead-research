/*
 * Copyright 2010 Keith Stevens
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

import edu.ucla.sspace.util.Pair;

import java.util.List;


/**
 * A {@link DependencyPathWeight} that scores paths based on the relations in
 * the path.  Paths are weighted asf follows:
 * <ul>
 *   </li>"subj" in path, score = 5;
 *   </li>"obj" in path, score = 4;
 *   </li>"obl" in path, score = 3;
 *   </li>"gen" in path, score = 2;
 *   </li>all other paths, score = 1;
 * </ul>
 *
 * @author Keith Stevens
 */
public class RelationPathWeight implements DependencyPathWeight {

    /**
     * {@inheritDoc}
     */
    public double scorePath(List<Pair<String>> path) {
        double score = 1;
        for (Pair<String> wordRelation : path) {
            if (wordRelation.y.equals("SBJ"))
                score = 5;
            if (wordRelation.y.equals("OBJ") && score < 4)
                score = 4;
            if (wordRelation.y.equals("OBL") && score < 3)
                score = 3;
            if (wordRelation.y.equals("GEN") && score < 2)
                score = 2;
        }
        return score;
    }
}
