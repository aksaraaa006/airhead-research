#!/usr/bin/python

from nltk.corpus.reader.wordnet import WordNetCorpusReader

import networkx as nx
import nltk
import numpy
import string

class PageRank():
  def __init__(self, scorer, use_cuda=False):
    self.wordnet_graph = None
    self.node_ranks = {} 
    self.synset_defs = {}
    self.walk_chance = .75
    self.scorer = scorer
    self.use_cuda = use_cuda
    self.wn = WordNetCorpusReader(nltk.data.find('corpora/wordnet17'))

  def cleanDefinition(self, sense):
    empty_trans = string.maketrans('','')
    dels = "()':;,.?!\""
    return [self.wn.morphy(tagged_word) for tagged_word in
            sense.definition.translate(empty_trans,dels).lower().split()]

  def addEdges(self, synset1, synset2, scale):
    for noun_synset in synset1:
      for verb_synset in synset2:
        score = self.scorer(noun_synset, verb_synset, self.synset_defs)
        if score > 0:
          self.wordnet_graph.add_edge(noun_synset, verb_synset, data=score*scale)

  def findBestMSynset(self, target_word):
    synsets = self.wn.synsets(target_word[0], target_word[1])
    synset_keys = [(sense, target_word) for sense in synsets]
    top_score = 0
    top_synset = None
    for key in synset_keys:
      if key in self.synsets:
        (_, index) = self.synsets[key]
        if self.node_ranks[index] > top_score:
          top_score = self.node_ranks[index]
          top_synset = key[0]
    return top_synset

  def findBestSynset(self, synsets):
    top_score = 0
    top_synset = None
    for synset in synsets:
      if synset in self.node_ranks and self.node_ranks[synset] > top_score:
        top_score = self.node_ranks[synset]
        top_synset = synset
    return top_synset

  def addDefinition(self, synset):
    self.synset_defs[synset] = self.cleanDefinition(synset)
    try:
      hyper = synset.hypernyms()[0]
      hypo = synset.hyponyms()[0]
      self.synset_defs[hyper] = self.cleanDefinition(hyper)
      self.synset_defs[hypo] = self.cleanDefinition(hypo)
    except IndexError:
      pass

  def buildMatrixGraph(self, words, scaler):
    self.synsets = {}
    index = 0
    for word in words:
      word_senses = self.wn.synsets(word[0], pos=word[1])[:3]
      word_senses = [(sense, word) for sense in word_senses]
      for sense in word_senses:
        self.synsets[sense] = (word_senses, index)
        index += 1

    for (sense, word) in self.synsets:
      self.addDefinition(sense)

    dim = len(self.synsets.keys())
    self.matrix = numpy.zeros((dim, dim), dtype=numpy.float32)
    self.node_ranks = numpy.ones((dim), dtype=numpy.float32)

    unexamined_keys = self.synsets.keys()[1:]
    for (sense1, word1) in self.synsets:
      _, index1 = self.synsets[(sense1, word1)]
      for (sense2, word2) in unexamined_keys:
        ignore_list, index2 = self.synsets[(sense2, word2)]
        score = 0
        if (sense1, word1) not in ignore_list and abs(index1 - index2) < 10:
          scale = scaler(((word1[0], word1[2]), (word2[0], word2[2])))
          score = self.scorer(sense1, sense2, self.synset_defs) * scale
        self.matrix[index1][index2] = score
        self.matrix[index2][index1] = score
      unexamined_keys = unexamined_keys[1:]

    row_sum = self.matrix.sum(axis=1).reshape((-1,1))
    for i in row_sum:
      if i[0] == 0:
        i[0] = 1

    self.matrix = self.matrix / row_sum
    self.matrix = numpy.transpose(self.matrix)

  def convergeMatrix(self):
    if len(self.node_ranks) <= 0:
      return
    eps = .000001
    max_error = 1
    M = numpy.zeros(self.node_ranks.shape, dtype=numpy.float32) * (1-self.walk_chance)
    d = self.walk_chance
    while max_error > eps: 
      max_error = 0
      PR_NEW = M + d*numpy.dot(self.matrix, self.node_ranks)
      max_error = numpy.fabs(PR_NEW - self.node_ranks).max()
      self.node_ranks = PR_NEW

  def buildGraph(self, words, scaler):
    self.wordnet_graph = nx.Graph()
    self.synset_defs = {}
    self.node_ranks = {}
    self.synsets = {}
    for word in words:
      self.synsets[word] = (self.wn.synsets(word[0], pos=word[1])[:3])

    for key in self.synsets:
      for sense in self.synsets[key]:
        self.addDefinition(sense)

    key_words = words
    other_keys = key_words[1:]
    for key1 in key_words:
      for key2 in other_keys:
        self.addEdges(self.synsets[key1], self.synsets[key2],
                      scaler(((key1[0], key1[2]), (key2[0], key2[2]))))
      other_keys = other_keys[1:]

    for node in self.wordnet_graph:
      self.node_ranks[node] = 1
    return self.wordnet_graph

  def convergeGraph(self, iterations=30):
    epsilon = .000001
    max_error = 1
    while max_error > epsilon:
      max_error = 0 
      for node in self.wordnet_graph:
        neighboors = self.wordnet_graph.neighbors(node)
        summed_rank = 0
        for neighboor in neighboors:
          n_weights = 1
          for n in self.wordnet_graph.neighbors(neighboor):
            n_weights += self.wordnet_graph[neighboor][n]
          summed_rank += self.wordnet_graph[node][neighboor] * (self.node_ranks[neighboor] / float(n_weights))
        new_score = (1-self.walk_chance) + (self.walk_chance * summed_rank)
        error = abs(new_score - self.node_ranks[node])
        self.node_ranks[node] = new_score
        if error > max_error:
          max_error = error
