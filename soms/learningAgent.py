#!/usr/bin/python

import math
import numpy
import random
import pylab

NUM_ROWS = 50 
NUM_COLS = 60 

def distance(v1, v2):
  return numpy.sqrt(numpy.power(v1 - v2, 2).sum(axis=1))

class learningAgent():
  def __init__(self, vector_size, phoneme_size, game, use_attention, use_context):
    # The nodes will each be an index in the following numpy arrays, holding the
    # location, the meaning vector and the learned words.
    num_nodes = NUM_ROWS*NUM_COLS
    self.meaning_vectors = numpy.random.random((num_nodes, vector_size))
    self.attention_vectors = numpy.ones_like(self.meaning_vectors)
    self.mean = numpy.zeros_like(self.meaning_vectors)
    self.average = numpy.zeros_like(self.meaning_vectors)
    # Locations still need to be set correctly
    self.locations = numpy.zeros((num_nodes, 2))
    locs = [(i,j) for i in range(NUM_ROWS) for j in range(NUM_COLS)]
    random.shuffle(locs)
    for i, location in enumerate(locs):
      self.locations[i][0] = location[0]
      self.locations[i][1] = location[1]
    self.learned_words = [None for i in range(num_nodes)]

    # Setting up some constants and time dependent variables.
    self.t = 0
    self.learning_rate = 1 
    self.n_range = 25
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
    object_frame = random.sample(self.game.objects, 4)
    context = object_frame[0]

    m_data, best_word = self.produceWord(context)
    if not best_word or m_data[1] > 1.5:
      best_word = self.game.pickNewWord()
      self.learned_words[m_data[0]] = best_word[0]

    self.learnPatterns(context, best_word[0])
    self.updateTimeValues()
    if self.use_context:
      for obj in object_frame[1:]:
        context += obj
    return context, best_word 

  def pickNewWord(self):
    word_index = random.randint(0, len(self.game.word_list) - 1)
    self.word_list.append(self.game.word_list[word_index])
    print "new word picked: ", self.word_list[-1]
    return self.word_list[-1]

  def produceWord(self, object_rep, learning=True):
    m_data = self.getBestNode(self.meaning_vectors, object_rep)
    m_som_best, m_min_dist, m_max_dist, m_near = m_data

    if self.learned_words[m_som_best] != None:
      return m_data, (self.learned_words[m_som_best], None)
    return m_data, None

  def updateMap(self, meaning, feature_vectors, data=None):
    if data == None:
      data = self.getBestNode(feature_vectors, meaning)
    best, min_dist, max_dist, near = data

    meaning_diff = (self.attention_vectors * meaning - feature_vectors)
    feature_vectors += self.learning_rate * (near * meaning_diff.transpose()).transpose()
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
    m_data = self.updateMap(object_rep, self.meaning_vectors, m_data)
    return m_data

  def getBestNode(self, map, input_vector):
    """Given some input and a map, go through each of the nodes and determine
    which one has a meaning vector closest to the input_vector.  Once finding
    this node, find all the nodes within the neighboorhood of this node, along
    with the node within this range which also has the largest distance from the
    input vector.  This will be used learning stages, not during production."""
    meaning_distances = distance(map, input_vector)
    best_node = 0
    min_dist = meaning_distances[0]
    for i, dist in enumerate(meaning_distances):
      if min_dist > dist:
        min_dist = dist
        best_node = i
    
    loc_distances = distance(self.locations, self.locations[best_node])
    max_dist = meaning_distances[0]
    neighborhood = numpy.zeros_like(meaning_distances)
    for i, dist in enumerate(loc_distances):
      if dist < self.n_range:
        neighborhood[i] = 1
        if max_dist < meaning_distances[i]:
          max_dist = meaning_distances[i]
    return best_node, min_dist, max_dist, neighborhood

  def updateTimeValues(self):
    if self.learning_rate > .01:
      self.learning_rate = .9*(1-self.t/1000)
    if self.n_range > 0:
      self.n_range -= math.floor(self.t/100)
    self.t += 1

  def receiveUtterance(self, word, context):
    if word not in self.word_list:
      self.word_list.append(word)
    m_data = self.learnPatterns(context, word[0])
    self.learned_words[m_data[0]] = word[0]
    if self.use_attention:
      m_data[0].updateAttention(context)
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

  def updateAttention(self):
    self.value_count += 1
    delta = self.meaning_vectors - self.average
    self.average += (delta/self.value_count)
    self.mean += delta * (meaning_vector - self.average)
    self.attention = 1 / (1 + (self.mean / self.value_count))
    self.attention = 1 / (1 + variance)
