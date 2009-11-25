#!/usr/bin/python

import numpy
import sys
import os
import pylab

if __name__ == "__main__":
  result_file = sys.argv[1]

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

  pylab.savefig("conflation_results.png")
