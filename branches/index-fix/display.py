#!/usr/bin/python

import numpy
import sys
import os
import pylab

if __name__ == "__main__":
  arg_index = int(sys.argv[1])
  title = sys.argv[2]

  result_files = sys.argv[3:]
  result_set = []
  counts = []
  avgs = []
  stds = []
  mins = []
  maxs = []
  for result_file in result_files:
    base, extension = result_file.split(".")
    name = base.split("_")
    cluster_count = int(name[arg_index])
    counts.append(cluster_count)
    results = []

    f = open(result_file)
    for line in f:
      results.append(float(line.split()[-1]))
    print result_file
    n = numpy.array(results)
    avgs.append(numpy.average(n))
    stds.append(numpy.std(n))
    mins.append(min(results))
    maxs.append(max(results))
    result_set.append((cluster_count, results))

  e = .1*abs(pylab.randn(len(avgs)))
  print counts
  print avgs
  print maxs
  print mins
  pylab.errorbar(counts, avgs, yerr=stds, fmt='bo', label="averages")
  pylab.plot(counts, maxs, 'ro', label="maximums")
  pylab.plot(counts, mins, 'go', label="minimums")
  pylab.legend()

  f = open("%s_variation.dat" %title, 'w')
  f.write("#dimension average std max min\n")
  for i in range(len(counts)):
    f.write("%d\t%f\t%f\t%f\t%f\n"
            %(counts[i], avgs[i], stds[i], maxs[i], mins[i]))
  f.close()
  pylab.savefig("%s_variation.eps" %title)
