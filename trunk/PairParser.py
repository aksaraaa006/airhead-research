#!/usr/bin/python

import re
import string
import sys

def parsePairs(in_file):
  triple_file = open(in_file)
  pair_expr = "\(\('.*?', .*?\), \('.*?', .*?\)\)" 
  del_chars = "(),"
  empty_trans = string.maketrans("(),", '   ')
  safe_pairs = []
  for line in triple_file:
    pairs = []
    matches = re.findall(
      "(%s)" %(pair_expr), line)
    if not matches:
      continue
    for g in matches:
      l = g.replace("Synset", "").translate(empty_trans, del_chars).split('\'')
      [n1, s1, n2, s2] = [x.strip() for x in l if x != '' and x != ' ']
      pairs.append(((n1,s1),(n2,s2)))
    safe_pairs.append(pairs)
  return safe_pairs

if __name__ == "__main__":
  p = parsePairs(sys.argv[1])
  print p
