#!/usr/bin/python

from nltk.corpus import wordnet as wn

import pickle
import sys
import PairParser

def addAllScores(score_dict, sense):
  try:
    if sense != 'None':
      synset = wn.synset(sense)
      updateScores(score_dict, synset, synset.max_depth() + 1)
  except ValueError:
    pass

def updateScores(score_dict, sense, max_depth):
  if sense in score_dict:
    print "updating senes!!!!"
    score_dict[sense] += (sense.max_depth()+1) / float(max_depth)
  else:
    score_dict[sense] = (sense.max_depth()+1) / float(max_depth)
  for hypernym in sense.hypernyms():
    updateScores(score_dict, hypernym, max_depth)

def getBestParent(score_dict):
  top_score = 0
  top_sense = None
  for key in score_dict:
    if score_dict[key] > top_score:
      top_sense = key
  return top_sense

if __name__ == "__main__":
  generated_lists = PairParser.parsePairs(sys.argv[1])
  parent_pairs = []
  for expansion_list in generated_lists:
    parent1_scores = {}
    parent2_scores = {}
    for ((n1, sense1), (n2, sense2)) in expansion_list:
      addAllScores(parent1_scores, sense1)
      addAllScores(parent2_scores, sense2)
    parent_pairs.append((getBestParent(parent1_scores),
                         getBestParent(parent2_scores)))
  print parent_pairs
