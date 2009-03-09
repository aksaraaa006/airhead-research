#!/usr/bin/python

from nltk.corpus import wordnet_ic

brown_ic = wordnet_ic.ic('ic-brown17.dat')

import string

def comboSim(syn1, syn2, def_dict):
  try:
    if syn1.pos == 'v' and syn2.pos == 'v':
      return syn1.lch_similarity(syn2)
    if syn1.pos == 'n' and syn2.pos == 'n':
      return syn1.jcn_similarity(syn2, brown_ic)
  except OverflowError:
    pass
  return bannerSim(syn1, syn2, def_dict)

def lchSim(syn1, syn2, def_dict):
  return syn1.lch_similarity(syn2)

def jcnSim(syn1, syn2, def_dict):
  return syn1.jcn_similarity(syn2, brown_ic)

def bannerSim(syn1, syn2, def_dict):
  try:
    hyper1 = syn1.hypernyms()[0]
    hyper2 = syn2.hypernyms()[0]
    hypo1 = syn1.hyponyms()[0]
    hypo2 = syn2.hyponyms()[0]
    score = (bannerScore(syn1, syn2, def_dict[syn1], def_dict[syn2]) +
             bannerScore(hyper1, hyper2, def_dict[hyper1], def_dict[hyper2]) +
             bannerScore(hypo1, hypo2, def_dict[hypo1], def_dict[hypo2]) +
             bannerScore(hyper1, syn2, def_dict[hyper1], def_dict[syn2]) +
             bannerScore(syn1, hyper2, def_dict[syn1], def_dict[hyper2]))
  except IndexError:
    score = bannerScore(syn1, syn2, def_dict[syn1], def_dict[syn2])
  return score

def lengthMatch(def1, def2, i):
  iter1 = def1
  length = 0
  while iter1 and i < len(def2):
    if iter1[0] == def2[i]:
      length += 1
      def2[i] = None
    else:
      break
    iter1 = iter1[1:]
    i += 1
  return length * length

def bannerScore(syn1, syn2, def1, def2):
  def1 = list(def1)
  def2 = list(def2)
  score = 0
  while def1:
    for i in range(len(def2)):
      if def1[0] == def2[i]:
        score += lengthMatch(def1, def2, i)
        print def1, def2, score
    def1 = def1[1:]
  return score

def bannerScore1(syn1, syn2, def1, def2):
  def1 = list(def1)
  def2 = list(def2)
  score = 0
  old_score = 1
  while old_score != score:
    old_score = score
    i = 0
    j = 0
    match_count = 0
    for word in def1:
      delta_score = 0
      while j < len(def2) and delta_score == 0:
        if word and word == def2[j]:
          delta_score = 1
          def2[j] = None
          def1[i] = None
        else:
          j += 1
      i += 1
      if delta_score == 0 and match_count > 0:
        break
      elif delta_score == 0:
        j = 0
      else:
        match_count += delta_score * delta_score
    score += match_count * match_count
  return score 

def leskSim(syn1, syn2, def_dict):
  def1 = def_dict[syn1]
  def2 = def_dict[syn2]
  score = 0
  for word in def1:
    if not word:
      continue
    if word in def2:
      score += 1
  return score

if __name__ == "__main__":
  def1 = ["the", "cat", "loves", "dogs"]
  def2 = ["the", "dogs", "cat", "loves", "the"]
  print bannerScore(None, None, def1, def2)
  print bannerScore1(None, None, def1, def2)
