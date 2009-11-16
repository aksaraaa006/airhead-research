#!/usr/bin/python

import sys

def all_perms(str):
  if len(str) <=1:
    yield str
  else:
    for perm in all_perms(str[1:]):
      for i in range(len(perm)+1):
        yield perm[:i] + str[0:1] + perm[i:]

if __name__ == "__main__":
  results = open(sys.argv[1])
  result_map = {}
  conflated_map = {}
  conflated_count = {}
  term_count = {}
  for line in results:
    conflated_sense, count = line.split("|")
    conflated, sense, original = conflated_sense.split("-")
    count = int(count)
    sense = int(sense)
    if conflated in result_map:
      result_map[conflated][(sense, original)] = count
      conflated_map[conflated].append(original)
      conflated_count[conflated] += count
    else:
      result_map[conflated] = {(sense, original):count}
      conflated_map[conflated] = [original]
      conflated_count[conflated] = float(count)
    if original in term_count:
      term_count[original] += count
    else:
      term_count[original] = count

  for k in conflated_map:
    word_set = list(set(conflated_map[k]))
    max_word_count = 0
    for word in word_set:
      if term_count[word] > max_word_count:
        max_word_count = term_count[word]
    baseline = max_word_count / conflated_count[k]

    word_scores = result_map[k]
    scores = []
    permutations = [p for p in all_perms(range(len(word_set)))]
    for perm in permutations:
      score = 0
      for i, sense in enumerate(perm):
        if (sense, word_set[i]) in word_scores:
          score += word_scores[(sense, word_set[i])]
      scores.append(score/conflated_count[k])

    max_score = 0
    max_index = 0
    for i, s in enumerate(scores):
      if s > max_score:
        max_score = s
        max_index = i
    print k, max_score, permutations[max_index], baseline, (max_score -
        baseline)

