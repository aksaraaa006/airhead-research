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

package edu.ucla.sspace.cluster;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.util.ArrayList;
import java.util.List;


public class Cluster {

    protected Vector centroid;

    protected int itemCount;

    protected double oldValueWeight;

    protected double newValueWeight;

    public Cluster(Vector firstVector) {
        this(firstVector, 0, 0);
    }

    public Cluster(Vector firstVector, double oldWeight, double newWeight) {
        centroid = firstVector; 
        oldValueWeight = oldWeight;
        newValueWeight = newWeight;
        itemCount = 1;
    }

    public synchronized void addVector(Vector vector) {
        if (oldValueWeight == 0 || newValueWeight == 0)
            Vectors.add(centroid, vector);
        else 
            Vectors.addWithScalars(centroid, oldValueWeight,
                                   vector, newValueWeight);
        ++itemCount;
    }

    public synchronized double compareWithVector(Vector vector) {
        return Similarity.cosineSimilarity(centroid, vector);
    }

    public synchronized List<Vector> getMembers() {
        List<Vector> members = new ArrayList<Vector>(1);
        members.add(Vectors.immutableVector(centroid));
        return members;
    }

    public synchronized int getTotalMemberCount() {
        return itemCount;
    }

    public synchronized Vector getVector() {
        return Vectors.immutableVector(centroid);
    }
}
