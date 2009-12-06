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

package edu.ucla.sspace.clustering;

import java.util.Properties;


public class OnlineClusteringGenerator {

    /**
     * A property prefix.
     */
    private static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.cluster.OnlineKMeansClustering";

    /**
     * The property for setting the weight of an exponential weighted moving
     * average.  This weight will be given to the historical data, and this
     * weight subtracted from 1 will be given to the new data points.
     */
    public static final String WEIGHTING_PROPERTY =
        PROPERTY_PREFIX + ".weights";

    public static final String MERGE_THRESHOLD_PROPERTY =
        PROPERTY_PREFIX + ".merge";

    public static final String DROP_THRESHOLD_PROPERTY =
        PROPERTY_PREFIX + ".drop";

    public static final String MAX_CLUSTERS_PROPERTY =
        PROPERTY_PREFIX + ".maxClusters";

    /**
     * The threshold for clustering
     */
    private final double clusterThreshold;

    /** 
     * The maximum number of clusters permitted.
     */
    private final int maxNumClusters;

    /**
     * The threshold for droping a cluster.
     */
    private final double dropThreshold;

    /**
     * A weight for an exponential weighted moving average.  If this value is
     * not set in the constructor, no moving average will be used.
     */
    private final double clusterWeight;

    public OnlineClusteringGenerator(Properties props) {
        clusterThreshold = Double.parseDouble(props.getProperty(
                    MERGE_THRESHOLD_PROPERTY, "1"));
        dropThreshold = Double.parseDouble(props.getProperty(
                    DROP_THRESHOLD_PROPERTY, "0"));
        maxNumClusters = Integer.parseInt(props.getProperty(
                    MAX_CLUSTERS_PROPERTY, "2"));
        clusterWeight = Double.parseDouble(props.getProperty(
                    WEIGHTING_PROPERTY, "0"));
    }

    public OnlineClustering getNewClusteringInstance() {
        return new OnlineKMeansClustering(clusterThreshold, dropThreshold,
                                          maxNumClusters, clusterWeight);
    }
}
