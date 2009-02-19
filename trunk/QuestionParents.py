#!/usr/bin/python

from nltk.corpus import wordnet

import pickle
import sys

def readSatQuestions(in_file):
  question_list = []
  sat_str = open(in_file).read()
  num_relations = 0
  for sat_question in sat_str.split("\n\n"):
    question = sat_question.split("\n")
    first = question[1].split()
    options = [q.split() for q in question[2:7]]
    print options

    second = question[ord(question[7]) - ord('a') + 2].split()
    question_list.append((first, second, options))
  return question_list

def readParents(in_file):
  expanded_tuples = pickle.load(open(in_file))
  return [parents for (exp, parents) in expanded_tuples if "None" not in parents]

def isParent(parent, word):
  if parent == "None":
    return False
  parent = wordnet.synset(parent)
  for sense in wordnet.synsets(word):
    for hyper_path in sense.hypernym_paths():
      for hypernym in hyper_path:
        if hypernym == parent:
          return True
  return False

def chooseOptions(options, l_parent, r_parent):
  return [opt for opt in options if isParent(l_parent, opt[0]) and
                                    isParent(r_parent, opt[1])]

def attemptAnswer(question, answer, options, parent_list):
  """Return one of three values, 0, 1, or 2.  0 means the question was not
  answerable at all, 1 means it could be answered but with multiple options, and
  2 means it could be answered perfectly"""
  ambigious = 0
  for (l_parent, r_parent) in parent_list:
    print "Checking question %s:%s with parents %s:%s" %(question[0],
        question[1], l_parent, r_parent)
    if isParent(l_parent, question[0]) and isParent(r_parent, question[1]):
      chosen_options = chooseOptions(options, l_parent, r_parent)
      if len(chosen_options) == 1 and chosen_options[0] == answer:
        print "answer %s:%s chosen for question" %(answer[0], answer[1])
        return 2 
      elif len(chosen_options) > 0:
        print "several options chosen for parents %s:%s" %(l_parent, r_parent)
        ambigious = 1
  return ambigious

def answerSAT(sat_file, expanded_file):
  question_list = readSatQuestions(sat_file)
  parent_list = readParents(expanded_file)
  score = 0
  multiple = 0
  for (question, answer,options) in question_list:
    answer = attemptAnswer(question, answer, options, parent_list)
    if answer == 1:
      multiple += 1
    elif answer == 2:
      score += 1
  print score, multiple, len(question_list)

if __name__ == "__main__":
  answerSAT(sys.argv[1], sys.argv[2])
