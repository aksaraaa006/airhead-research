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

package edu.ucla.sspace.matrix;


/**
 * This class provides some simple statistical evaluation methods over a
 * matrix.  The average and standard devation of a matrix can be computed for
 * either all values, all values in a each row, or all values in each column.
 * This is models after the statstics methods provided by NumPy.
 */
public class Statistics {

    /**
     * The dimension over which the statistic should be evaluated.
     */
    public enum Dimension {
        ALL,
        ROW,
        COLUMN,
    }

    /**
     * Return a matrix containing the standard deviation for the dimension
     * specificed.  If average is null, the average will be computed first.
     * Otherwise it is checked that the average the required dimension size
     *
     * @param m The matrix containing values to evaluate.
     * @param average The matrix of average values along dim
     * @param dim The dimension across which analysis should take place.
     *
     * @return A matrix of the standard deviations.
     */
    public static Matrix std(Matrix m, Matrix average, Dimension dim) {
        // Generate the average if not provided.
        if (average == null) {
            average = average(m, dim);
        }
        // Handle a few error cases.
        if ((dim == Dimension.ALL &&
             (average.rows() != 1 || average.columns() != 1)) ||
            (dim == Dimension.ROW &&
             (average.rows() != m.rows() || average.columns() != 1)) ||
            (dim == Dimension.COLUMN &&
             (average.rows() != 1 || average.columns() != m.columns()))) {
            return null;
        }

        Matrix std = null;
        if (dim == Dimension.ALL) {
            double variance = 0;
            std = new ArrayMatrix(1, 1);
            for (int i = 0; i < m.rows(); ++i) {
                for (int j = 0; j < m.columns(); ++j) {
                    variance += Math.pow(m.get(i, j) - average.get(0, 0), 2);
                }
            }

            variance = variance / (double) (m.rows() * m.columns());
            std.set(0, 0, Math.sqrt(variance));
        } else if (dim == Dimension.ROW) {
            std = new ArrayMatrix(m.rows(), 1);
            for (int i = 0; i < m.rows(); ++i) {
                double variance = 0;
                for (int j = 0; j < m.columns(); ++j) {
                    variance += Math.pow(m.get(i, j) - average.get(i, 0), 2);
                }

                variance = variance / (double) m.columns();
                std.set(i, 0, Math.sqrt(variance));
            }

        } else if (dim == Dimension.COLUMN) {
            std = new ArrayMatrix(1, m.columns());

            for (int i = 0; i < m.rows(); ++i) {
                for (int j = 0; j < m.columns(); ++j) {
                    double variance = std.get(0, j);
                    variance += Math.pow(m.get(i, j) - average.get(0, j), 2);
                    std.set(0, j, variance);
                }

            }
            for (int i = 0; i < m.columns(); ++i) {
                double variance = std.get(0, i);
                variance = variance / (double) m.rows();
                std.set(0, i, Math.sqrt(variance));
            }
        }
        return std;
    }

    /**
     * Return a matrix containing the averages for the dimension
     * specificed.
     *
     * @param m The matrix containing values to evaluate.
     * @param dim The dimension across which analysis should take place.
     *
     * @return A matrix of the averages.
     */
    public static Matrix average(Matrix m, Dimension dim) {
        Matrix averageMatrix = null;
        if (dim == Dimension.ALL) {
            double average = 0;
            for (int i = 0; i < m.rows(); ++i) {
                for (int j = 0; j < m.columns(); ++j) {
                    average += m.get(i, j);
                }
            }
            averageMatrix = new ArrayMatrix(1, 1);
            average = average / (double) (m.rows() * m.columns());
            averageMatrix.set(1, 1, average);
        } else if (dim == Dimension.ROW) {
            averageMatrix = new ArrayMatrix(m.rows(), 1);
            for (int i = 0; i < m.rows(); ++i) {
                double average = 0;
                for (int j = 0; j < m.columns(); ++j) {
                    average += m.get(i, j);
                }
                average = average / (double) m.columns();
                averageMatrix.set(i, 0, average);
            }
        } else if (dim == Dimension.COLUMN) {
            averageMatrix = new ArrayMatrix(1, m.columns());
            for (int i = 0; i < m.rows(); ++i) {
                for (int j = 0; j < m.columns(); ++j) {
                    averageMatrix.set(0, j,
                                      m.get(i, j) + averageMatrix.get(0, j));
                }
            }
            for (int i = 0; i < m.columns(); ++i) {
                double average = averageMatrix.get(0, i);
                average = average / (double) m.rows();
                averageMatrix.set(0, i, average);
            }
        }
        return averageMatrix;
    }

    /**
     * Return a matrix containing the standard deviation for the dimension
     * specificed.  If average is null, the average will be computed first.
     * Otherwise it is checked that the average the required dimension size
     *
     * @param m The matrix containing values to evaluate.
     * @param average The matrix of average values along dim
     * @param dim The dimension across which analysis should take place.
     * @param errorCode If values equal this value they will not be counted.
     *
     * @return A matrix of the standard deviations.
     */
    public static Matrix std(Matrix m, Matrix average,
                             Dimension dim, int errorCode) {
        // Generate the average if not provided.
        if (average == null) {
            average = average(m, dim, errorCode);
        }
        // Handle a few error cases.
        if ((dim == Dimension.ALL &&
             (average.rows() != 1 || average.columns() != 1)) ||
            (dim == Dimension.ROW &&
             (average.rows() != m.rows() || average.columns() != 1)) ||
            (dim == Dimension.COLUMN &&
             (average.rows() != 1 || average.columns() != m.columns()))) {
            return null;
        }
        Matrix std = null;
        if (dim == Dimension.ALL) {
            double variance = 0;
            std = new ArrayMatrix(1, 1);
            int validSize = 0;
            for (int i = 0; i < m.rows(); ++i) {
                for (int j = 0; j < m.columns(); ++j) {
                    if (m.get(i,j) == errorCode)
                        continue;
                    validSize++;
                    variance += Math.pow(m.get(i, j) - average.get(0, 0), 2);
                }
            }
            variance = variance / (double) validSize;
            std.set(0, 0, Math.sqrt(variance));
        } else if (dim == Dimension.ROW) {
            std = new ArrayMatrix(m.rows(), 1);
            for (int i = 0; i < m.rows(); ++i) {
                double variance = 0;
                int validSize = 0;
                for (int j = 0; j < m.columns(); ++j) {
                    if (m.get(i,j) == errorCode)
                        continue;
                    validSize++;
                    variance += Math.pow(m.get(i, j) - average.get(i, 0), 2);
                }
                variance = variance / (double) validSize;
                std.set(i, 0, Math.sqrt(variance));
            }
        } else if (dim == Dimension.COLUMN) {
            std = new ArrayMatrix(1, m.columns());
            Matrix validSize = new ArrayMatrix(1, m.columns());
            for (int i = 0; i < m.rows(); ++i) {
                for (int j = 0; j < m.columns(); ++j) {
                    if (m.get(i,j) == errorCode)
                        continue;
                    double variance = std.get(0, j);
                    variance += Math.pow(m.get(i, j) - average.get(0, j), 2);
                    std.set(0, j, variance);
                    validSize.set(0, j, validSize.get(0, j)+1);
                }
            }
            for (int i = 0; i < m.columns(); ++i) {
                double variance = std.get(0, i);
                variance = variance / validSize.get(0, i);
                std.set(0, i, Math.sqrt(variance));
            }
        }
        return std;
    }

    /**
     * Return a matrix containing the averages for the dimension
     * specificed.
     *
     * @param m The matrix containing values to evaluate.
     * @param dim The dimension across which analysis should take place.
     * @param errorCode If values equal this value they will not be counted.
     *
     * @return A matrix of the averages.
     */
    public static Matrix average(Matrix m, Dimension dim, int errorCode) {
        Matrix averageMatrix = null;
        if (dim == Dimension.ALL) {
            int validSize = 0;
            double average = 0;
            for (int i = 0; i < m.rows(); ++i) {
                for (int j = 0; j < m.columns(); ++j) {
                    if (m.get(i, j) == errorCode)
                        continue;
                    validSize++;
                    average += m.get(i, j);
                }
            }
            averageMatrix = new ArrayMatrix(1, 1);
            average = average / (double) validSize;
            averageMatrix.set(1, 1, average);
        } else if (dim == Dimension.ROW) {
            averageMatrix = new ArrayMatrix(m.rows(), 1);
            for (int i = 0; i < m.rows(); ++i) {
                double average = 0;
                int validSize = 0;
                for (int j = 0; j < m.columns(); ++j) {
                    if (m.get(i, j) == errorCode)
                        continue;
                    validSize++;
                    average += m.get(i, j);
                }
                average = average / (double) validSize;
                averageMatrix.set(i, 0, average);
            }
        } else if (dim == Dimension.COLUMN) {
            averageMatrix = new ArrayMatrix(1, m.columns());
            Matrix validSize = new ArrayMatrix(1, m.columns());
            for (int i = 0; i < m.rows(); ++i) {
                for (int j = 0; j < m.columns(); ++j) {
                    if (m.get(i, j) == errorCode)
                        continue;
                    validSize.set(0, j, validSize.get(0, j)+1);
                    averageMatrix.set(0, j,
                                      m.get(i, j) + averageMatrix.get(0, j));
                }
            }
            for (int i = 0; i < m.columns(); ++i) {
                double average = averageMatrix.get(0, i);
                average = average / validSize.get(0, i);
                averageMatrix.set(0, i, average);
            }
        }
        return averageMatrix;
    }
}
