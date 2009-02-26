#!/usr/bin/python

from nltk.corpus.reader.wordnet import WordNetCorpusReader
from nltk import Tree

import nltk
import getopt
import pagerank as pr
import pickle
import scorer
import string
import sys

wn = WordNetCorpusReader(nltk.data.find('corpora/wordnet17'))
valid_pos = { 'J':'a', 'N':'n', 'V':'v', 'R':'r' }
trans = string.maketrans('-/\\', '   ')

def addCleanWord(word, pos, index, l):
  if '-' in word or '/' in word:
    split_words = word.translate(trans, "").split()
    for w in split_words:
      l.append((w, None, index))
  elif pos[0] in valid_pos:
    l.append((word, valid_pos[pos[0]], index))

def setupTargetWords(test_cases, tagged_sent):
  target_cases = []
  for case_index in test_cases:
    (word, pos) = tagged_sent[case_index]
    addCleanWord(word, pos, case_index, target_cases)
  return target_cases

def cleanSent(tagged_sent):
  """Given a list of tuples, containing words and the POS tags, we need to
  return another list of tuples.  Theses new tuples need to contain: the word,
  the POS tag, and the index in the tree.  This will uniquely identify every
  word in the tree."""
  clean_sent = []
  for index in range(len(tagged_sent)):
    (word, pos) = tagged_sent[index]
    addCleanWord(word, pos, index, clean_sent)
  return clean_sent

def syntaxScale(word_dict, (word1, word2)):
  if word1 not in word_dict or word2 not in word_dict:
    return 0
  tree_path1 = word_dict[word1]
  tree_path2 = word_dict[word2]
  path_length1 = len(tree_path1)
  path_length2 = len(tree_path2)
  nodes_matched = 0
  for node in tree_path1:
    if not tree_path2:
      continue
    if node == tree_path2[0]:
      nodes_matched += 1
      tree_path2 = tree_path2[1:]
    else:
      break
  return nodes_matched / (float(path_length1 + path_length2) / 2.0)

def buildSentInfo(syntax_tree):
  word_dict = {}
  tags = syntax_tree.pos()
  for index in range(len(tags)):
    (word, pos) = tags[index]
    tree_position = syntax_tree.leaf_treeposition(index)
    word_dict[(word, index)] = tree_position
  return word_dict

def runWSD(test_cases, parse_trees, use_scaling=True, use_banner=True):
  # go through each corpus file given, using indexes so we can latter refer to
  # the test_case's relevant.
  #for i in range(len(parse_trees)):
    #curr_sample = parse_trees[i]
    curr_sample = parse_trees[1]
    # Go through each parse tree using indexes, so again we can latter refer to
    # the relevant test case.
    #for j in range(len(curr_sample)):
    for j in range(40, 41):

      # Build up a parse tree composed of the previous sentence, the current
      # sentence, and the next sentence, or as many of them that exist.
      context_tree = "(S "
      target_offset = 0
      if j > 0:
        context_tree += curr_sample[j-1]
        offset_tree = Tree(curr_sample[j-1])
        target_offset = len(offset_tree.leaves())
      context_tree += curr_sample[j]
      if j+1 < len(curr_sample):
        context_tree += curr_sample[j+1]
      context_tree += " )"
      tree = Tree(context_tree)

      # Determine which options should be passed into the page rank graph
      if use_scaling:
        word_info = buildSentInfo(tree)
        scaler = (lambda x: syntaxScale(word_info, x))
      else:
        scaler = (lambda x: 1)

      if use_banner:
        sim_func = scorer.bannerSim
      else:
        sim_func = scorer.leskSim

      tree_words = cleanSent(tree.pos())
      target_words = setupTargetWords(
          [case_index+target_offset for case_index in test_cases[1][j]], tree.pos())

      # Add everything to the graph and disambiguate!
      wsd_graph = pr.PageRank(sim_func)
      wsd_graph.buildMatrixGraph(tree_words, scaler)
      wsd_graph.convergeMatrix()
      # for each word to disambiguate in the case_dict, pull it out of the graph
      for target in target_words:
        final_synset = wsd_graph.findBestMSynset(target)
        # How the fuck am i going to pick the right lemma?
        print target, final_synset

if __name__ == "__main__":
  opts, args = getopt.getopt(sys.argv[1:], 'nlu')
  use_banner = True
  use_syntax = True
  for option, value in opts:
    if option == '-n':
      use_syntax = False
    if option == '-l':
      use_banner = False
    if option == '-u':
      print """
      Usage eval-all-english.py [OPTION] test_case parse_trees 
      Options are:
      n: do not use syntax scaling
      l: use Lesk similarity metric instead of banner's
      u: print this usage"""
  test_cases = pickle.load(open(args[0]))
  parse_trees = pickle.load(open(args[1]))
  runWSD(test_cases, parse_trees, use_syntax, use_banner)
