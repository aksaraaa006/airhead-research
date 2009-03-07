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

def lengthMatch(def1, def2):
  iter1 = def1
  iter2 = def2
  length = 0
  while iter1 and iter2:
    if iter1[0] == iter2[0]:
      length += 1
    else:
      break
    iter1 = iter1[1:]
    iter2 = iter2[1:]
  return length * length

def bannerScore(syn1, syn2, def1, def2):
  def1 = list(def1)
  def2 = list(def2)
  score = 0
  sub_score = 0
  while def1:
    iter_def2 = def2
    while iter_def2:
      if def1[0] == iter_def2[0]:
        sub_score += lengthMatch(def1, iter_def2)
      iter_def2 = iter_def2[1:]
    def1 = def1[1:]
  score += sub_score
  return score

#def bannerScore(syn1, syn2, def1, def2):
#  def1 = list(def1)
#  def2 = list(def2)
#  score = 0
#  old_score = 1
#  while old_score != score:
#    old_score = score
#    i = 0
#    j = 0
#    match_count = 0
#    for word in def1:
#      delta_score = 0
#      while j < len(def2) and delta_score == 0:
#        if word and word == def2[j]:
#          delta_score = 1
#          def2[j] = None
#          def1[i] = None
#        else:
#          j += 1
#      i += 1
#      if delta_score == 0 and match_count > 0:
#        break
#      elif delta_score == 0:
#        j = 0
#      else:
#        match_count += delta_score * delta_score
#    score += match_count * match_count
#  return score 

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

