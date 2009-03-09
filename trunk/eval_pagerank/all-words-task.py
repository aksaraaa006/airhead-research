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

def addCleanWord(word, pos, index):
  """Given a word, pos and index into some parse tree, return it with a cleaned
  up part of speech tag, and if needed, split up the word into several words.
  In the case of splitting, the split words have no presumed part of speech
  since we assume that the tagger will not have handled this correctly."""
  if '-' in word or '/' in word:
    split_words = word.translate(trans, "").split()
    return [(w, None, index) for w in split_words]
  elif pos[0] in valid_pos:
    return [(word, valid_pos[pos[0]], index)]
  return []

def cleanSent(tagged_sent):
  """Given a list of tuples, containing words and the POS tags, we need to
  return another list of tuples.  Theses new tuples need to contain: the word,
  the POS tag, and the index in the tree.  This will uniquely identify every
  word in the tree."""
  clean_sent = []
  for index in range(len(tagged_sent)):
    (word, pos) = tagged_sent[index]
    clean_sent += addCleanWord(word, pos, index)
  return clean_sent

def syntaxScale(word_dict, (word1, word2)):
  """Given two words, provide a scaling factor which is dependent on how close
  the two words are in the parse tree.  For this we just need the tree path for
  each word.  The scale is determined by finding the depth of the lowest common
  parent,and then dividing this by the average of the depth of the two nodes.
  So common parents deeper into the tree will have a higher scale factor, and
  more ambigious parents will have a lower scale factor."""
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
  """Given a syntax tree, create a dictionary keyed by word and tree index such
  that it contains the tree path for that particular word.  This will permit
  easy access to computing the syntactic similarity of any two words."""
  word_dict = {}
  tags = syntax_tree.pos()
  for index in range(len(tags)):
    (word, pos) = tags[index]
    tree_position = syntax_tree.leaf_treeposition(index)
    word_dict[(word, index)] = tree_position
  return word_dict

def runWSD(test_cases, parse_trees, sim_func, use_scaling=True, use_limit=False):
  # go through each corpus file given, using indexes so we can latter refer to
  # the test_case's relevant.
  for i in range(len(parse_trees)):
    curr_sample = parse_trees[i]
    # Go through each parse tree using indexes, so again we can latter refer to
    # the relevant test case.
    for j in range(len(curr_sample)):
      if j not in test_cases[i]:
        continue
      # Build up a parse tree composed of the previous sentence, the current
      # sentence, and the next sentence, or as many of them that exist.
      context_tree = "(S "
      target_offset = 0
      if j > 0:
        context_tree += curr_sample[j-1]
        # Determine the offset in the full tree by counting the number of leaves
        # in any previous trees. This will let us index into the full tree
        # correctly.
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

      tree_words = cleanSent(tree.pos())

      # Add everything to the graph and disambiguate!
      wsd_graph = pr.PageRank(sim_func, use_limit*10)
      wsd_graph.buildMatrixGraph(tree_words, scaler)
      wsd_graph.convergeMatrix()
      # for each word to disambiguate in the case_dict, pull it out of the graph
      tagged_sent = tree.pos()
      for case_index in test_cases[i][j]:
        # For each case, pull it out of the tagged sentence, then clean it up
        # (in the case that it has non alpha characters, split it into multiple
        # words).  Then for all the words in the target case, find a lemma which
        # matches it best.
        (word, pos) = tagged_sent[case_index+target_offset]
        target_words = addCleanWord(word, pos, case_index+target_offset)
        lemmas = []
        for target_word in target_words:
          final_synset = wsd_graph.findBestMSynset(target_word)
          lemmas.append(pickLemma(final_synset, target_word))
        print "d%03d d%03d.s%03d.t%03d" %(i, i, j, case_index), "".join(lemmas)

def pickLemma(synset, target_word):
  """Given a target word and a possible synset for it, pick a lemma which
  matches based on the following heuristics: the lemma name matches the word
  exactly, the lemma name matches the lower case version, the lemma name matches
  the stemmed word, or the lemma matches the stemmed lower case version.  If
  synset is None, or none of it's lemmas work, try this same process on the most
  frequent sense for the word.  If this also fails, return "U"."""
  possible_names = [target_word[0], target_word[0].lower(),
      wn.morphy(target_word[0]), wn.morphy(target_word[0].lower())]
  if synset:
    if len(synset.lemmas) == 0:
      return synest.lemmas()[0].key + " "
    for lemma in synset.lemmas:
      if lemma.name in possible_names:
        return lemma.key + " "
  backups = wn.synsets(target_word[0], pos=target_word[1])
  if len(backups) > 0:
    for lemma in backups[0].lemmas:
      if lemma.name in possible_names:
        return lemma.key + " "
  return "U "

def usage():
  print """
  Usage eval-all-english.py [OPTION] test_case parse_trees 
  Options are:
  n: do not use syntax scaling
  s: set the similarity measure used, valid choices are: l, b, c 
  u: print this usage"""

if __name__ == "__main__":
  opts, args = getopt.getopt(sys.argv[1:], 'lns:u')
  sim_type = 'banner'
  use_syntax = True
  use_limit = False
  for option, value in opts:
    if option == '-n':
      use_syntax = False
    if option == '-s':
      sim_type = value
    if option == '-u':
      usage()
    if option == '-l':
      use_limit = True
  valid_sims = {'cl': (lambda x,y,z: scorer.comboSim(scorer.leskSim, x, y, z)),
                'cb': (lambda x,y,z: scorer.comboSim(scorer.bannerSim, x, y, z)),
                'banner': scorer.bannerSim,
                'lesk': scorer.leskSim,
                'jcn': scorer.jcnSim,
                'lch':scorer.lchSim }
  if sim_type not in valid_sims or len(args) != 2:
    usage()
    sys.exit(1)
  test_cases = pickle.load(open(args[0]))
  parse_trees = pickle.load(open(args[1]))
  runWSD(test_cases, parse_trees, valid_sims[sim_type], use_syntax, use_limit)
