#!/usr/bin/python
# A simple script to read in sentences from the lexical sample seneval task and
# convert each group of sentences to trees.

from nltk import Tree
from subprocess import Popen
from xml.dom.minidom import parse

import os
import pickle
import re
import sys

def buildTestCases(xml_file):
  senseval_xml = parse(xml_file)
  lex_items = senseval_xml.getElementsByTagName('lexelt')
  test_cases = {}
  for lex in lex_items:
    item_name = lex.attributes['item'].value
    for instance in lex.getElementsByTagName('instance'):
      id = instance.attributes['id'].value
      context = instance.getElementsByTagName('context')[0]
      target_word = context.getElementsByTagName('head')[0].childNodes[0].data
      text = context.childNodes[0].data
      text += " " + target_word + " "
      text += context.childNodes[2].data
      test_cases[(item_name, id)] = (text, target_word)
  return test_cases

def makeTrees(test_cases):
  for key in test_cases:
    (text, word) = test_cases[key]

    to_parse_file = open('.tmp.to.parse','w')
    to_parse_file.write(text)
    to_parse_file.close()

    result_file = open('.tmp.syntax.tree', 'w')

    parser_dir = os.environ['HOME'] + '/devel/src/stanford-parser-2008-10-26'
    Popen(['sh', '%s/lexparser.sh' %parser_dir, '.tmp.to.parse'],
        stdout=result_file).wait()
    result_file.close()

    result_file = open('.tmp.syntax.tree')
    syntax_trees = result_file.read().split("\n\n")
    result_file.close()
    valid_trees = '(ST ' + ''.join([t.replace("\n","") for t in syntax_trees
                                      if re.match('^(\(ROOT.*\n)', t)]) + ')'
    tree = Tree(valid_trees)
    test_cases[key] = (tree, word)

if __name__ == "__main__":
  test_cases = buildTestCases(sys.argv[1])
  makeTrees(test_cases)
  pickle.dump(test_cases, open(sys.argv[2], 'w'))
