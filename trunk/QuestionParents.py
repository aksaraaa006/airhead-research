#!/usr/bin/python

from nltk.corpus import wordnet

import pickle
import SATSolver as ss
import sys

def getWordSimilarity(sat_word, possible_word):
  # NB: I sure hope both of these words are nouns!
  synsets1 = wordnet.synsets(sat_word, pos='n')
  if possible_word == "None":
    return 0
  synset2 = wordnet.synset(possible_word)
  
  highestSimilarity = 0
  for s1 in synsets1:
    similarity = ss.getScaledSimilarityMeasure(s1, synset2)
    if (similarity > highestSimilarity):
      highestSimilarity = similarity
          
  return highestSimilarity

def readSatQuestions(in_file):
  question_list = []
  sat_str = open(in_file).read()
  num_relations = 0
  for sat_question in sat_str.split("\n\n"):
    question = tuple(sat_question.split("\n"))
    first = tuple(question[1].split())
    options = tuple([tuple(q.split()) for q in question[2:7]])
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
  multi_options = []
  for (l_parent, r_parent) in parent_list:
    print "Checking question %s:%s with parents %s:%s" %(question[0],
        question[1], l_parent, r_parent)
    if isParent(l_parent, question[0]) and isParent(r_parent, question[1]):
      chosen_options = chooseOptions(options, l_parent, r_parent)
      if len(chosen_options) == 1 and chosen_options[0] == answer:
        print "answer %s:%s chosen for question" %(answer[0], answer[1])
        return 2, [] 
      elif len(chosen_options) > 0 and answer in chosen_options:
        print "several options chosen for parents %s:%s" %(l_parent, r_parent)
        ambigious = 1
        multi_options.append(chosen_options)
  return ambigious, multi_options

def similarityAnswer(sat_file, expanded_file):
  question_list = readSatQuestions(sat_file)
  expanded_tuples = pickle.load(open(expanded_file))
  exp_and_parents = [ (exp, paren) for (exp,paren) in expanded_tuples if
                      paren[0] != "None" and paren[1] != "None"]
  score = 0
  for (question, answer,options) in question_list:
    print "answering question: ", question
    good_expansions = []
    for (expand_list, (l_parent, r_parent)) in exp_and_parents:
      if isParent(l_parent, question[0]) and isParent(r_parent, question[1]):
        good_expansions.append(expand_list)
    option_scores = {}
    for opt in options:
      option_scores[opt] = 0
    for generalized_list in good_expansions:
      best_score = 0
      best_option = None
      print generalized_list
      for opt in options:
        sim = getSimilarity(generalized_list, opt)
        if sim > best_score:
          best_score = sim
          best_option = opt
      if best_option:
        option_scores[best_option] += 1
    best_score = 0
    best_answer = None
    for key in option_scores:
      if option_scores[key] > best_score:
        best_score = option_scores[key]
        best_answer = key
    print "best answer is: ", best_answer
    print "real answer is: ", answer
    if best_answer == answer:
      score += 1
  print score, len(question_list)

def getSimilarity(listOfAnalogousPairs, sourcePair):
    highestSimilarity = 0
    for (n1, s1), (n2, s2) in listOfAnalogousPairs:
        similarity = (getWordSimilarity(sourcePair[0],s1) +
                      getWordSimilarity(sourcePair[1],s2))
        if (similarity >= highestSimilarity):
            highestSimilarity = similarity
    return highestSimilarity

def answerSAT(sat_file, expanded_file):
  question_list = readSatQuestions(sat_file)
  parent_list = readParents(expanded_file)
  ambig_dict = {}
  score = 0
  multiple = 0
  for (question, answer,options) in question_list:
    answer, spares = attemptAnswer(question, answer, options, parent_list)
    if answer == 1:
      multiple += 1
      ambig_dict[question] = spares
    elif answer == 2:
      score += 1
  print ambig_dict
  print score, multiple, len(question_list)

if __name__ == "__main__":
  if sys.argv[1] == 's':
    print "testing SAT using similarity metric"
    similarityAnswer(sys.argv[2], sys.argv[3])
  elif sys.argv[1] == 'p':
    print "testing SAT using parent generalizations"
    answerSAT(sys.argv[2], sys.argv[3])
  else:
    print "Usage: ./QuestionParents.py [sp] sat_question_file expanded.pickle"
