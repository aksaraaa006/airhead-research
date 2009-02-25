#!/usr/bin/python
# For use with senseval 3 all english data sets.  Expects two arguments, a
# pickle file which contains a dictionary, where the keys are a tuple of the
# lexterm and the word id for the test case, and the entry is a tuple of a
# nltk Tree and the target word.  The second argument should be the desination
# file for a pickled list of results

from nltk.corpus.reader.wordnet import WordNetCorpusReader
from time import time
from xml.dom.minidom import parse

import getopt
import nltk
import pagerank as pr
import pickle
import scorer
import sys

wn = WordNetCorpusReader(nltk.data.find('corpora/wordnet17'))

def buildWordMapper(in_file):
  map_xml = parse(in_file)
  map_dict = {}
  for sense_map in map_xml.getElementsByTagName('sense'):
    try:
      lemmas = sense_map.attributes['wn'].value
      id = sense_map.attributes['id'].value
      for lemma in lemmas.split(';'):
        map_dict[lemma] = id
    except KeyError:
      pass
  return map_dict

def changeTag(pos):
  if pos.startswith('J'):
    return 'a'
  if pos.startswith('N'):
    return 'n'
  if pos.startswith('V'):
    return 'v'

def cleanSent(tagged_sent):
  return [(word, changeTag(pos), i) for (word, pos, i) in tagged_sent if
          pos.startswith('J') or pos.startswith('N') or pos.startswith('V')]

def syntaxScale(word_dict, (word1, word2)):
  if word1 not in word_dict or word2 not in word_dict:
    return 0
  (tree_path1, _) = word_dict[word1]
  (tree_path2, _) = word_dict[word2]
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
    word_dict[(word, index)] = (tree_position, pos)
  return word_dict

def runWSD(test_cases, use_scaling=True, use_banner=True):
  for case_key in test_cases.keys()[:500]:
    (item_name, id) = case_key
    (tree, target_word) = test_cases[case_key]
    if use_scaling:
      word_info = buildSentInfo(tree)
      scaler = (lambda x: syntaxScale(word_info, x))
    else:
      scaler = (lambda x: 1)

    if use_banner:
      sim_func = scorer.bannerSim
    else:
      sim_func = scorer.leskSim
    tree_tags = tree.pos()
    tree_words = []
    for index in range(len(tree_tags)):
      tree_words.append((tree_tags[index][0], tree_tags[index][1], index))
      if tree_tags[index][0] == target_word:
        target_tuple = (target_word, case_key[0][-1], index)
    wsd_graph = pr.PageRank(sim_func)
    wsd_graph.buildMatrixGraph(cleanSent(tree_words), scaler)
    wsd_graph.convergeMatrix()
    final_synset = wsd_graph.findBestMSynset(target_tuple)
    #wsd_graph.buildGraph(cleanSent(tree_words), scaler)
    #wsd_graph.convergeGraph()
    #final_synset = wsd_graph.findBestSynset(wn.synsets(target_word))
    # Get the sense as given by PageRank and find a suitable lemma
    answer = "U"
    target_lemma = id.split(".")[0]
    if final_synset:
      for lemma in final_synset.lemmas:
        if lemma.name == target_lemma:
          answer = lemma.key
    if answer == "U":
      # Fallback strategy is to use Most Frequent Sense according to WordNet
      synsets = wn.synsets(target_word, pos=case_key[0][-1])
      if synsets:
        for lemma in synsets[0].lemmas:
          if lemma.name == target_lemma:
            answer = lemma.key
    print item_name, id, answer 

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
      Usage eval-all-english.py [OPTION] test_case map_xml
      Options are:
      n: do not use syntax scaling
      l: use Lesk similarity metric instead of banner's
      u: print this usage"""
  test_cases = pickle.load(open(args[0]))
  runWSD(test_cases, use_syntax, use_banner)
