#!/usr/bin/python

import string

BANNER_CACHE = {}
def bannerSim(syn1, syn2, def_dict):
  if (syn1, syn2) in BANNER_CACHE:
    return BANNER_CACHE[(syn1, syn2)]
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
  BANNER_CACHE[(syn1, syn2)] = score
  return score

def bannerScore(syn1, syn2, def1, def2):
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

