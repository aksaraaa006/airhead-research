#!/usr/bin/python

import numpy
import pickle
import sys

def buildFeatureSet(keys, features, word_file):
  # Extract the first 60 words from the child learning word list.
  word_list = []
  for line in word_file.readlines()[:100]:
    words = line.split()
    word_list.append((words[0], words[1]))

  # Extract the word to feature number mappings.
  feature_values = {}
  for line in keys.readlines():
    word, feature_num = line.split()
    feature_values[word] = int(feature_num)

  # Translate the word to feature list to a numpy vector.
  vector_size = max(feature_values.values()) + 1
  word_features = []
  word_mappings = {}
  line_count = 0
  for line in features.readlines():
    feature_vector = [0 for i in range(vector_size)] 
    words = line.split()
    word_mappings[words[0]] = line_count 
    line_count += 1
    for word in words[1:]:
      feature_vector[feature_values[word]] = 1
    word_features.append(feature_vector)

  print [(word, word_mappings[word[0]]) for word in word_list if word[0] in
         word_mappings]
  # Save the word, and it's feature vector for all words in the word list.
  used_words = [(word, numpy.array(word_features[word_mappings[word[0]]]))
                for word in word_list if word[0] in word_mappings]
  pickle.dump(used_words, open("object_features", 'w'))
  return

  # Run the svd on all the feature vectors to reduce the dimensionality to 100
  # features.
  feature_matrix = numpy.array(word_features)
  size = len(word_features)
  u,s,v = numpy.linalg.svd(feature_matrix)
  u = u[numpy.ix_(range(size), range(100))]


if __name__ == "__main__":
  if len(sys.argv) != 4:
    print "usage: ./feature_maker.py <key-file> <feature-file> <word-list>"
    sys.exit(1)
  key_file = open(sys.argv[1])
  feature_file = open(sys.argv[2])
  word_list = open(sys.argv[3])
  buildFeatureSet(key_file, feature_file, word_list)
