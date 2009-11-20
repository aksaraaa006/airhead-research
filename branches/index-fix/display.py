#!/usr/bin/python

import sys
import os
from matplotlib import pyplot

if __name__ == "__main__":
  result_files = sys.argv[1:]
  result_set = []
  counts = []
  avgs = []
  mins = []
  maxs = []
  for result_file in result_files:
    name = result_file.split("_")
    cluster_count = int(name[2])
    counts.append(cluster_count)
    results = []
    f = open(result_file)
    for line in f:
      results.append(float(line.split()[-1]))
    avgs.append(sum(results) / float(len(results)))
    mins.append(min(results))
    maxs.append(max(results))
    result_set.append((cluster_count, results))

  print counts
  print avgs
  print maxs
  print mins
  pyplot.plot(counts, avgs, 'b', label="averages")
  pyplot.plot(counts, maxs, 'r', label="maximums")
  pyplot.plot(counts, mins, 'g', label="minimums")
  pyplot.legend()

  pyplot.show()

  print result_set
