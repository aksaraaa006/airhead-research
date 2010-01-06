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

import edu.ucla.sspace.vector.Vector;

import java.util.Properties;


/**
 * A utility class for generating a new {@code OnlineKMeansClustering}
 * instance. This class supports the following properties:
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #WEIGHTING_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_WEIGHT}
 *
 * <dd style="padding-top: .5em">This variable sets the weight given to the mean
 * vector in a rolling average of vectors.</p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #MERGE_THRESHOLD_PROPERTY }
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_MERGE_THRESHOLD}
 *
 * <dd style="padding-top: .5em">This variable sets the threshold for merging
 * two clusters. </p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #DROP_THRESHOLD_PROPERTY }
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_DROP_THREHSHOLD}
 *
 * <dd style="padding-top: .5em">This variable sets the size requirement, as a
 * precentage, for dropping a cluster.</p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #MAX_CLUSTERS_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_MAX_CLUSTERS}
 *
 * <dd style="padding-top: .5em">This variable sets the maximum number of
 * clusters used.</p>
 *
 * </dl>
 *
 * @author Keith Stevens
 */
public class OnlineClusteringGenerator<T extends Vector> {

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

    /**
     * The property for setting the threshold for merging two clusters.
     */
    public static final String MERGE_THRESHOLD_PROPERTY =
        PROPERTY_PREFIX + ".merge";

    /**
     * The property size threshold, as a percentage, for dropping a cluster.
     */
    public static final String DROP_THRESHOLD_PROPERTY =
        PROPERTY_PREFIX + ".drop";

    /**
     * The property for the maximum number of clusters.
     */
    public static final String MAX_CLUSTERS_PROPERTY =
        PROPERTY_PREFIX + ".maxClusters";

    /**
     * The default weight, which will use no weighted average.
     */
    public static final String DEFAULT_WEIGHT = "0";

    /**
     * The default merge threshold.
     */
    public static final String DEFAULT_MERGE_THRESHOLD = "1";

    /**
     * The default drop threshold, where no clusters will be dropped.
     */
    public static final String DEFAULT_DROP_THREHSHOLD = "0";

    /**
     * The default number of clusters.
     */
    public static final String DEFAULT_MAX_CLUSTERS = "2";

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

    /**
     * Creates a new generator using the system properties.
     */
    public OnlineClusteringGenerator() {
        this(System.getProperties());
    }

    /**
     * Creates a new generator using the given properties.
     */
    public OnlineClusteringGenerator(Properties props) {
        clusterThreshold = Double.parseDouble(props.getProperty(
                    MERGE_THRESHOLD_PROPERTY, DEFAULT_MERGE_THRESHOLD));
        dropThreshold = Double.parseDouble(props.getProperty(
                    DROP_THRESHOLD_PROPERTY, DEFAULT_DROP_THREHSHOLD));
        maxNumClusters = Integer.parseInt(props.getProperty(
                    MAX_CLUSTERS_PROPERTY, DEFAULT_MAX_CLUSTERS));
        clusterWeight = Double.parseDouble(props.getProperty(
                    WEIGHTING_PROPERTY, DEFAULT_WEIGHT));
    }

    /**
     * Generates a new instance of a {@code OnlineClustering} based on the
     * values used to construct this generator.
     */
    public OnlineClustering<T> getNewClusteringInstance() {
        return new OnlineKMeansClustering<T>(clusterThreshold, dropThreshold,
                                             maxNumClusters, clusterWeight);
    }
}
