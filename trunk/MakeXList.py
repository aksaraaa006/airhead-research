#!/usr/bin/python

import re
import string
import sys

def parsePairs(triple_list, verbs):
  pair_expr = "\(\('.*?', .*?\), \('.*?', .*?\)\)" 
  del_chars = "(),"
  empty_trans = string.maketrans("", '')
  split_list = triple_list.split()
  verb_synset = split_list[-3]
  if verb_synset != "None":
    verbs.append(verb_synset.replace("Synset('", "").translate(empty_trans,
        del_chars).split('.')[0])

  matches = re.findall("(%s)" %(pair_expr), triple_list)
  if not matches:
    return
  print "X-LIST:"
  for g in matches:
    l = g.replace("Synset", "").translate(empty_trans, del_chars).split('\'')
    try:
      [n1, s1, n2, s2] = [x.strip() for x in l if x != '' and x != ' ']
      print n1, ",", n2
    except ValueError:
      pass

if __name__ == "__main__":
  gen_file = open(sys.argv[1])
  verb_list = []
  for line in gen_file:
    parsePairs(line, verb_list)

  print "VERBS:"
  for verb in verb_list:
    print verb
