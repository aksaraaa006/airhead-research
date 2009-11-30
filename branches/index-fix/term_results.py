#!/usr/bin/python

import numpy
import sys
import os
import pylab

if __name__ == "__main__":
  result_file = sys.argv[1]
  title = sys.argv[2]

  results = []
  baselines = []
  conflations = []

  result_tuples = []

  f = open(result_file)
  for line in f:
    split_line = line.split()
    t = (float(split_line[-2]), float(split_line[-3]), split_line[0])
    result_tuples.append(t)
  result_tuples.sort()

  results = [t[1] for t in result_tuples]
  baselines = [t[0] for t in result_tuples]
  range = range(len(results))

  pylab.plot(range, results, 'ro')
  pylab.plot(range, baselines, 'go')

  f = open("%s_conflation_results.dat" %title, 'w')
  f.write("#term_index\taccuracy\tbaseline\n")
  for i in range:
    f.write("%d\t%f\t%f\n" %(i, results[i], baselines[i]))
  f.close()

  pylab.savefig("%s_conflation_results.eps" %title)
