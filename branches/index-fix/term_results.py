#!/usr/bin/python

import numpy
import sys
import os
import pylab

if __name__ == "__main__":
  result_file = sys.argv[1]

  results = []
  conflations = []

  f = open(result_file)
  for line in f:
    split_line = line.split()
    results.append(float(split_line[-1]))
    conflations.append(split_line[0])

  pylab.plot(conflations, results, 'ro', label="conflations")
  pylab.legend()

  pylab.savefig("conflation_results.png" %title)
