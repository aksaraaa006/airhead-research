#!/usr/bin/python
# A simple script to extract all the parse trees for the test data and place
# them into a list of lists.  each corpus will have it's own list of parse
# trees, and given some word to disambiguation, and it's id key, you should be
# able to index into this list of lists by the two indicies given in the key.
# The created list of lists will be pickled into the second argument
# The first argument should be the directory where three fixed tree files
# reside.

import pickle
import sys

def extractParses(in_file):
  end_text = "\n( "
  tree_text = open(in_file).read()
  split_trees = tree_text.split(end_text)[1:]
  corpus_trees = [ "(" + tree.replace("\n", "") for tree in split_trees]
  return corpus_trees

if __name__ == "__main__":
  tree_files = ['cl23.mrg', 'wsj_1695.mrg', 'wsj_1778.mrg']
  corpora = []
  for tree in tree_files:
    corpora.append(extractParses(sys.argv[1] + tree))
    print len(corpora[-1])
  pickle.dump(corpora, open(sys.argv[2], 'w'))
