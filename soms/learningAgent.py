#!/usr/bin/python

import math
import numpy
import random
import pylab

from math import sqrt
from numpy import linalg

NUM_ROWS = 50 
NUM_COLS = 60 

def inRange(node1, node2, range):
  return (sqrt(pow(node1[0]- node2[0],2) + pow(node1[1] - node2[1],2)) <
          range)

def distance(v1, v2):
  return sqrt(numpy.power(v1 - v2, 2).sum())

class learningAgent():
  def __init__(self, vector_size, phoneme_size, game):
    self.m_som = []
    self.p_som = []
    self.t = 0
    for i  in range(NUM_ROWS):
      for j in range(NUM_COLS):
        self.m_som.append(SOMNode(i,j,vector_size))
    for i  in range(NUM_ROWS):
      for j in range(NUM_COLS):
        self.p_som.append(SOMNode(i,j,phoneme_size))
    self.game = game
    self.heb_weights = numpy.zeros((NUM_ROWS*NUM_COLS, NUM_ROWS*NUM_COLS))
    self.learning_rate = 1 
    self.n_range = 25
    self.word_list = [] 

  def generateUtterance(self):
    """Generate a context vector, which is the sum of some subject representation,
    along with the meaning vector of 3 other random objects which are in the
    scene.  Also generate a word which best describes the subject according to
    this agent's personal knowledge."""
    # Select which object to talk about, and 3 other objects to be part of the
    # context, all at random.
    object_frame = random.sample(self.game.objects, 4)
    context = object_frame[0]

    suc, m_data, best_word = self.produceWord(context)
    if not suc:# or m_data[1] > 1.5:
      best_word = self.pickNewWord()
      m_data[0].value = best_word[0]

    self.learnPatterns(context, best_word[0])
    self.updateTimeValues()
    #print "spoke: ", best_word
    return context, best_word 

  def pickNewWord(self):
    word_index = random.randint(0, len(self.game.word_list) - 1)
    self.word_list.append(self.game.word_list[word_index])
    print "new word picked: ", self.word_list[-1]
    return self.word_list[-1]

  def produceWord(self, object_rep, learning=True):
    m_data = self.getBestNode(self.m_som, object_rep)
    m_som_best, m_min_dist, m_max_dist, m_near, m_rest = m_data

    #print object_rep
    #print "found node with phoneme_rep", m_data[0].loc, m_min_dist, m_data[0].value
    if m_som_best.value != None and m_min_dist < 1.5:
      return True, m_data, (m_som_best.value, None)
    if m_som_best.value == None:
      print "empty best value"
    return False, m_data, None

    if learning:
      self.updateMap(object_rep, self.m_som, m_data)

    semantic_activations = numpy.zeros(NUM_COLS*NUM_ROWS)
    for i in range(NUM_COLS*NUM_ROWS):
      semantic_activations[i] = self.m_som[i].activation
    phoneme_activations = numpy.dot(self.heb_weights,
                                    semantic_activations)
    max_activation = phoneme_activations.max()

    # There is a NaN error in here somewhere, but how?
    best_meaning = None 
    #print self.heb_weights
    #print semantic_activations
    #print max_activation
    for i in range(NUM_COLS*NUM_ROWS):
      if phoneme_activations[i] == max_activation:
        best_meaning = self.p_som[i].meaning

    best_distance = 100000
    best_word = None
    dist_sum = 0
    for (word, phoneme) in self.word_list:
      #print phoneme, best_meaning
      dist = distance(phoneme, best_meaning)
      dist_sum += 0
      if dist < best_distance:
        best_word = word, phoneme
        best_distance = dist
    if dist_sum == 0 or best_distance / dist_sum < .50:
      return False, m_data, None 
    else:
      return True, None, best_word

  def updateMap(self, meaning, map, data=None):
    if data == None:
      data = self.getBestNode(map, meaning)
    best, min_dist, max_dist, near, rest = data

    for node in near:
      node.updateMeaning(meaning, self.learning_rate)
      #node.updateActivation(meaning, best, min_dist, max_dist)
    for node in rest:
      node.clearActivation()

    #print min_dist
    return data

  def learnPatterns(self, object_rep, phoneme_rep,
                    m_data=None, p_data=None):
    """Central learning stage.  An object vector and a phoneme vector are
    expected to be given, additionally, if getBestNode has already been called,
    the returned data can optionall be passed in to skip redoing work.  With the
    best semantic node matching the object vector and the best phoneme node
    matching the phoneme vector, the nodes in that neighboorhood will have their
    activations will be updated, along with their meaning vectors.  After this,
    the associative weights will be updated and normalized."""
    m_data = self.updateMap(object_rep, self.m_som, m_data)
    return m_data
    #p_data = self.updateMap(phoneme_rep, self.p_som, p_data)
    #print "setting node with phoneme_rep", m_data[0].loc, phoneme_rep
    #for i in range(NUM_COLS*NUM_ROWS):
    #  for j in range(NUM_COLS*NUM_ROWS):
    #    self.heb_weights[i][j] = (self.learning_rate *
    #                              self.m_som[i].activation *
    #                              self.p_som[j].activation)
    #row_lengths = numpy.zeros(NUM_COLS*NUM_ROWS)
    #for i in range(NUM_COLS*NUM_ROWS):
    #  row_lengths[i] = math.sqrt(numpy.dot(self.heb_weights[i],
    #                                       self.heb_weights[i]))
    #for i in range(NUM_COLS*NUM_ROWS):
    #  for j in range(NUM_COLS*NUM_ROWS):
    #    self.heb_weights[i][j] = self.heb_weights[i][j] / row_lengths[i]

  def getBestNode(self, map, input_vector):
    """Given some input and a map, go through each of the nodes and determine
    which one has a meaning vector closest to the input_vector.  Once finding
    this node, find all the nodes within the neighboorhood of this node, along
    with the node within this range which also has the largest distance from the
    input vector.  This will be used learning stages, not during production."""
    min_sim = 1000000
    best_node = None
    distances = []
    neighborhood = []
    remaining = []
    for node in map:
      sim = node.similarity(input_vector)
      distances.append((node.loc, sim, node))
      if min_sim > sim:
        min_sim = sim
        best_node = node
    # Find the maximum distance for nodes within range of the best node.
    max_sim = 0
    for (loc, distance, node) in distances:
      if inRange(loc, best_node.loc, self.n_range):
        neighborhood.append(node)
        if max_sim < distance:
          max_sim = distance;
      else:
        remaining.append(node)

    return best_node, min_sim, max_sim, neighborhood, remaining

  def updateTimeValues(self):
    if self.learning_rate > .01:
      self.learning_rate = .9*(1-self.t/1000)
    if self.n_range > 0:
      self.n_range -= math.floor(self.t/100)
    self.t += 1

  def receiveUtterance(self, word, context):
    #print "heard: ", word
    if word not in self.word_list:
      self.word_list.append(word)
    m_data = self.learnPatterns(context, word[0])
    m_data[0].value = word[0] 
    self.updateTimeValues()

  def printMap(self):
    for node in self.m_som:
      if node.value != None:
        t = pylab.text(node.loc[0], node.loc[1], node.value)
    pylab.axis([0, 60, 0, 60])
    pylab.show()

class SOMNode():
  def __init__(self, x_pos, y_pos, vector_size):
    self.loc = (x_pos, y_pos)
    self.meaning = numpy.random.rand(vector_size)
    self.attention = numpy.ones_like(self.meaning)
    self.value = None

  def similarity(self, meaning_vector):
    """Compute the euclidian similarity of this nodes meaning with the given
    meaning_vector."""
    return distance(meaning_vector, self.meaning)

  def clearActivation(self):
    self.activation = 0

  def updateActivation(self, meaning_vector, best_node, min_dist, max_dist):
    meaning_dist = distance(meaning_vector, self.meaning)
    self.activation = 1 - ((meaning_dist - min_dist) / (max_dist - min_dist))

  def updateMeaning(self, meaning_vector, learning_rate):
    """Update the meaning of this node to incorporate the given meaning vector
    if this node is in the neighborhood of other_node."""
    self.meaning += (learning_rate *
                     (meaning_vector - self.meaning))
