#!/usr/bin/python

from nltk.corpus import wordnet as wn

import pickle
import re
import string
import sys

def parsePairs(triple_list):
  pair_expr = "\(\('.*?', .*?\), \('.*?', .*?\)\)" 
  del_chars = "(),"
  empty_trans = string.maketrans("", '')
  pairs = []
  matches = re.findall("(%s)" %(pair_expr), triple_list)
  if not matches:
    return [] 
  for g in matches:
    l = g.replace("Synset", "").translate(empty_trans, del_chars).split('\'')
    try:
      [n1, s1, n2, s2] = [x.strip() for x in l if x != '' and x != ' ']
      pairs.append(((n1,s1),(n2,s2)))
    except ValueError:
      pass
  return pairs

def addAllScores(score_dict, sense):
  try:
    if sense != 'None':
      synset = wn.synset(sense)
      updateScores(score_dict, synset, synset.max_depth() + 1)
  except ValueError:
    pass

def updateScores(score_dict, sense, max_depth):
  if sense in score_dict:
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
  if top_sense:
    return top_sense.name
  else:
    return "None"

def buildGenList(in_file):
  gen_file = open(in_file)
  parent_pairs = []
  for line in gen_file:
    expansion_list = parsePairs(line)
    parent1_scores = {}
    parent2_scores = {}
    for ((n1, sense1), (n2, sense2)) in expansion_list:
      addAllScores(parent1_scores, sense1)
      addAllScores(parent2_scores, sense2)
    parent_pairs.append((expansion_list,
                         (getBestParent(parent1_scores),
                          getBestParent(parent2_scores))))
    print parent_pairs[-1]
  return parent_pairs

if __name__ == "__main__":
  expanded_with_parents = buildGenList(sys.argv[1])
  pickle.dump(expanded_with_parents, open(sys.argv[2], 'w'))
