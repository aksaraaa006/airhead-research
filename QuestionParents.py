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
    question_list.append(((first[0], first[1]), (second[0], second[1]), options))
  return question_list

def readParents(in_file):
  expanded_tuples = pickle.load(open(in_file))
  return [parents for (exp, parents) in expanded_tuples]

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
  for (l_parent, r_parent) in parent_list:
    chosen_options = chooseOptions(options, l_parent, r_parent)
    if (isParent(l_parent, question[0]) and isParent(r_parent, question[1]) and
        len(chosen_options) == 1 and chosen_options[0] == answer):
      return True
  return False

def answerSAT(sat_file, expanded_file):
  question_list = readSatQuestions(sat_file)
  parent_list = readParents(expanded_file)
  score = 0
  for (question, answer,options) in question_list:
    if attemptAnswer(question, answer, options, parent_list):
      score += 1
  print score, len(question_list)

if __name__ == "__main__":
  answerSAT(sys.argv[1], sys.argv[2])
