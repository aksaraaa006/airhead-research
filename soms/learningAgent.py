#!/usr/bin/python

import math
import numpy
import random
import pylab

NUM_ROWS = 50 
NUM_COLS = 60 

M_MAP = 0
P_MAP = 1

def distance(v1, v2):
  return numpy.sqrt(numpy.power(v1 - v2, 2).sum(axis=1))

class learningAgent():
  def __init__(self, vector_size, phoneme_size, game, use_attention, use_context):
    # The nodes will each be an index in the following numpy arrays, holding the
    # location, the meaning vector and the learned words.
    num_nodes = 300
    self.meaning_vectors = numpy.random.random((num_nodes, vector_size))
    self.phoneme_vectors = numpy.random.random((num_nodes, phoneme_size))
    self.phoneme_attentions = numpy.ones_like(self.phoneme_vectors)
    self.attention_vectors = numpy.ones_like(self.meaning_vectors)
    self.mean = numpy.zeros_like(self.meaning_vectors)
    self.average = numpy.zeros_like(self.meaning_vectors)
    self.activations = numpy.zeros((2, num_nodes))
    self.heb_weights = numpy.zeros((num_nodes, num_nodes))
    # Locations still need to be set correctly
    self.locations = numpy.zeros((num_nodes, 2))
    locs = [(i,j) for i in range(NUM_ROWS) for j in range(NUM_COLS)]
    random.shuffle(locs)
    for i, location in enumerate(locs[:num_nodes]):
      self.locations[i][0] = location[0]
      self.locations[i][1] = location[1]
    self.learned_words = [None for i in range(num_nodes)]

    # Setting up some constants and time dependent variables.
    self.t = 0
    self.value_count = 0
    self.learning_rate = 1 
    self.n_range = 40 
    self.word_list = [] 
    self.use_attention = use_attention
    self.use_context = use_context
    self.game = game

  def generateUtterance(self):
    """Generate a context vector, which is the sum of some subject representation,
    along with the meaning vector of 3 other random objects which are in the
    scene.  Also generate a word which best describes the subject according to
    this agent's personal knowledge."""
    # Select which object to talk about, and 3 other objects to be part of the
    # context, all at random.
    print "generating utterance"
    object_frame = self.game.getContext(4)
    context = object_frame[0]

    m_data, best_word = self.produceWord(context)
    if not best_word: # or m_data[1] > 1.5:
      best_word = self.game.pickNewWord()
      self.learned_words[m_data[0]] = best_word

    self.learnPatterns(context, best_word[1])
    self.updateTimeValues()
    if self.use_context:
      for obj in object_frame[1:]:
        context += obj
    self.updateAttention(context, m_data[3])
    return context, best_word 

  def produceWord(self, object_rep, learning=True):
    m_data = self.getBestNode(self.meaning_vectors, self.attention_vectors, object_rep)
    m_som_best, m_min_dist, m_max_dist, m_near, m_dists = m_data

    if self.learned_words[m_som_best] != None:
      return m_data, self.learned_words[m_som_best]
    return m_data, None

  def updateMap(self, meaning, feature_vectors, attentions, map_type, data=None):
    if data == None:
      data = self.getBestNode(feature_vectors, attentions, meaning)
    best, min_dist, max_dist, near, dists = data

    meaning_diff = (attentions * meaning - feature_vectors)
    feature_vectors += self.learning_rate * (near * meaning_diff.transpose()).transpose()
    result = 1 - (dists - min_dist) / (max_dist - min_dist)
    self.activations[map_type] = (near * result.transpose()).transpose()
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
    m_data = self.updateMap(object_rep, self.meaning_vectors,
                            self.attention_vectors, M_MAP, m_data)
    return m_data, p_data
    p_data = self.updateMap(phoneme_rep, self.phoneme_vectors,
                            self.phoneme_attentions, P_MAP, p_data)
    return m_data, p_data
    self.heb_weights += self.learning_rate * numpy.outer(self.activations[M_MAP],
                                                         self.activations[P_MAP])
    # Now just normalize the weights.
    denoms = numpy.sqrt(numpy.power(self.heb_weights, 2).sum(axis=1))
    # Set zero row sums to be one to prevent divide by zero errors.
    denoms[denoms==0.0] = 1
    self.heb_weights = (self.heb_weights.transpose() / denoms).transpose()

    return m_data, p_data

  def getBestNode(self, map, attentions, input_vector):
    """Given some input and a map, go through each of the nodes and determine
    which one has a meaning vector closest to the input_vector.  Once finding
    this node, find all the nodes within the neighboorhood of this node, along
    with the node within this range which also has the largest distance from the
    input vector.  This will be used learning stages, not during production."""
    meaning_distances = distance(map, attentions * input_vector)
    best_node = 0
    min_dist = meaning_distances[0]
    max_dist = meaning_distances[0]
    for i, dist in enumerate(meaning_distances):
      if min_dist > dist:
        min_dist = dist
        best_node = i
      if max_dist < dist:
        max_dist = dist 
    print "best node: %d, min_dist: %d, max_dist: %d" %(best_node, min_dist,
        max_dist)
    
    loc_distances = distance(self.locations, self.locations[best_node])
    neighborhood = numpy.zeros_like(meaning_distances)
    for i, dist in enumerate(loc_distances):
      if dist < self.n_range:
        neighborhood[i] = 1
    return best_node, min_dist, max_dist, neighborhood, meaning_distances

  def updateTimeValues(self):
    if self.learning_rate > .01:
      self.learning_rate = .9*(1-self.t/1000)
    self.n_range = 50 * math.exp(-1 * self.t / 177)
    self.t += 1

  def receiveUtterance(self, word, context):
    print "receiving utterance"
    if word not in self.word_list:
      self.word_list.append(word)
    m_data, p_data  = self.learnPatterns(context, word[1])
    self.learned_words[m_data[0]] = word
    self.updateAttention(context, m_data[3])
    self.updateTimeValues()

  def printMap(self, out_dir, count):
    return
    pylab.cla()
    pylab.clf()
    pylab.axis([0, 60, 0, 60])
    for node in self.m_som:
      if node.value != None:
        pylab.text(node.loc[0], node.loc[1], node.value)
    #pylab.xlabel("node x position")
    #pylab.ylabel("node y position")
    pylab.show()
    pylab.savefig(out_dir + "learner_%d_mapping.png" %count)

  def updateAttention(self, meaning_vector, near):
    if not self.use_attention:
      return
    self.value_count += 1
    delta = (near * (self.meaning_vectors - self.average).transpose()).transpose()
    self.average += (delta/self.value_count)
    self.mean += delta * (meaning_vector - self.average)
    self.attention_vectors = 1 / (1 + (self.mean / self.value_count))
