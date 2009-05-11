#!/usr/bin/python

import numpy
import pickle
import sys

from heapq import heappush, heappop, heappushpop
def distance(v1, v2):
  return numpy.sqrt(numpy.power(v1 - v2, 2).sum())

def buildObjectSet(object_vectors):
  chosen_set = set()
  i = 0
  while len(chosen_set) < 60 and i < len(object_vectors):
    row = object_vectors[i]
    heap = []
    # Iterate through all the other object vectors, compute their euclidian
    # distance and save only the 3 closest objects to the current one.  This is
    # done by putting the negative distance into a heap, so the smallest value
    # in the heap is the furthes in distance from the object, and replacing that
    # as a closer object is found.
    for j, other_row in enumerate(object_vectors[i+1:]):
      sim = -1 * distance(row, other_row)
      if len(heap) < 3:
        heappush(heap, (sim, j))
      elif sim > heap[0][0]:
        heappushpop(heap, (sim, j))
    chosen_set.add(i)
    while heap:
      dist, index = heappop(heap)
      chosen_set.add(index)
    i += 1
  return [object_vectors[index] for index in chosen_set]

if __name__ == "__main__":
  if len(sys.argv) != 3:
    print "usage: ./build_object_set.py <object_file> <out_file>"
    sys.exit(1)
  objects = pickle.load(open(sys.argv[1]))
  chosen_objects = buildObjectSet(objects)
  pickle.dump(chosen_objects, open(sys.argv[2], 'w'))
