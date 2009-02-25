#!/usr/bin/python
# A script which runs on the lexical sample task, needs to be reformulated to
# work with the all data task

from nltk.corpus.reader.wordnet import WordNetCorpusReader

import nltk
import pickle
import sys

wn = WordNetCorpusReader(nltk.data.find('corpora/wordnet17'))

if __name__ == "__main__":
  test_cases = pickle.load(open(sys.argv[1]))
  solutions = []
  for case_key in test_cases.keys()[:500]:
    (_, target_word) = test_cases[case_key]
    synsets = wn.synsets(target_word, pos=case_key[0][-1])
    answer = "U"
    target_lemma = case_key[1].split(".")[0]
    if synsets:
      for lemma in synsets[0].lemmas:
        if lemma.name == target_lemma:
          answer = lemma.key
    solutions.append("%s %s %s" %(case_key[0], case_key[1], answer))
  solutions.sort()
  for sol in solutions:
    print sol
