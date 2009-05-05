#!/usr/bin/python

import numpy
import pickle
import sys

def buildFeatureSet(keys, features):
  feature_values = {}
  for line in keys.readlines():
    word, feature_num = line.split()
    feature_values[word] = int(feature_num)

  vector_size = max(feature_values.values()) + 1
  word_features = []
  for line in features.readlines():
    feature_vector = [0 for i in range(vector_size)] 
    words = line.split()
    for word in words[1:]:
      feature_vector[feature_values[word]] = 1
    word_features.append(feature_vector)
  feature_matrix = numpy.array(word_features)
  size = len(word_features)
  u,s,v = numpy.linalg.svd(feature_matrix)
  u = u[numpy.ix_(range(size), range(100))]
  pickle.dump(u, open("object_features", 'w'))

if __name__ == "__main__":
  if len(sys.argv) != 3:
    print "usage: ./feature_maker.py <key-file> <feature-file>"
    sys.exit(1)
  key_file = open(sys.argv[1])
  feature_file = open(sys.argv[2])
  buildFeatureSet(key_file, feature_file)
