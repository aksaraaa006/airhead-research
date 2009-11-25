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
    conflated = conflated.strip()
    sense = sense.strip()
    original = original.strip()

    count = int(count)
    sense = int(sense)

    if conflated in conflated_map:
      sense_map = conflated_map[conflated]
    else:
      sense_map = {}
      conflated_map[conflated] = sense_map

    if sense in sense_map:
      term_map = sense_map[sense]
    else:
      term_map = {}
      sense_map[sense] = term_map

    if original in term_map:
      term_map[original] += count
    else:
      term_map[original] = count

    if original in term_count:
      term_count[original] += count
    else:
      term_count[original] = count

  for k in conflated_map:
    sense_map = conflated_map[k]
    total_count = 0
    accuracy_count = 0
    assignments = []
    words = []
    for s in sense_map:
      term_map = sense_map[s]
      max_count = 0
      max_term = ""
      for o in term_map:
        count = term_map[o]
        if count > max_count:
          max_count = count
          max_term = o
        total_count += count
        words.append(o)
      assignments.append(max_term)
      accuracy_count += max_count

    words = list(set(words))
    max_base = 0
    for word in words:
      if term_count[word] > max_base:
        max_base = term_count[word]

    baseline = max_base / float(total_count)
    accuracy = accuracy_count / float(total_count)

    print k, assignments, accuracy, baseline, (accuracy - baseline)

