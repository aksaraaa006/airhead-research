#!/usr/bin/python

import math
import numpy
import random
import pylab

from math import sqrt
from numpy import linalg

def inRange(node1, node2, range):
  return (sqrt(pow(node1[0]- node2[0],2) + pow(node1[1] - node2[1],2)) <
          range)

def distance(v1, v2):
  return sqrt(numpy.power(v1 - v2, 2).sum())

class learningAgent():
  def __init__(self, vector_size, game):
    self.som = []
    self.t = 0
    self.letters = [chr(i) for i in range(ord('a'), ord('z'))]
    for i  in range(50):
      for j in range(60):
        self.som.append(SOMNode(i,j,.001, vector_size))
    self.game = game

  def getNearestNeighbors(self, k):
    word_mappings = {}
    for node in self.som:
      if node.word != "":
        word_mappings[node.word] = node.meaning

    nearest_neighbors = {} 
    for word in word_mappings:
      node_sims = []
      for other_word in word_mappings:
        node_sims.append((other_word,
                           distance(word_mappings[word],
                                    word_mappings[other_word])))
      node_sims.sort()
      nearest_neighbors[word] = node_sims[:k]
    return nearest_neighbors

  def generateUtterance(self):
    """Generate a context vector, which is the sum of some subject representation,
    along with the meaning vector of 3 other random objects which are in the
    scene.  Also generate a word which best describes the subject according to
    this agent's personal knowledge."""
    self.t += 1
    # Select which object to talk about, and 3 other objects to be part of the
    # context, all at random.
    object_frame = random.sample(self.game.objects, 4)
    context = object_frame[0]

    # Select the word which best describes the subject according the to agents
    # own SOM.
    best_node, min_sim, max_sim, neighbors = self.getBestNode(context)
    if min_sim > 1.5 or best_node.word == "":
      word = self.generateWord()
      best_node.word = word
    else:
      word = best_node.word
    # Reinforce the mapping to the known object meaning in the agent's map.
    for node in neighbors:
      node.updateMeaning(context)
      node.updateTimeValues(self.t)
    # Add in random objects in the context to make the problem more challenging
    #for i in range(1,4):
    #  context += object_frame[i]
    return word, context

  def getBestNode(self, context):
    min_sim = 1000000
    best_node = None
    distances = []
    neighborhood = []
    for node in self.som:
      sim = node.similarity(context)
      print sim
      distances.append((node.loc, sim, node))
      if min_sim > sim:
        min_sim = sim
        best_node = node
    # Find the maximum distance for nodes within range of the best node.
    max_sim = 0
    for (loc, distance, node) in distances:
      if inRange(loc, best_node.loc, best_node.n_range):
        neighborhood.append(node)
        if max_sim < distance:
          max_sim = distance;

    return best_node, min_sim, max_sim, neighborhood

  def generateWord(self):
    return "".join(random.sample(self.letters, 8))

  def recieveUtterance(self, word, context):
    best_node, min_sim, max_sim, neighbors = self.getBestNode(context)
    if best_node:
      best_node.word = word
      for node in neighbors:
        node.updateMeaning(context)
        node.updateTimeValues(self.t)
    else:
      print "no mapping found wtf"

  def printMap(self):
    for node in self.som:
      if node.word != "":
        t = pylab.text(node.loc[0], node.loc[1], node.word)
    pylab.axis([0, 60, 0, 60])
    pylab.show()

class SOMNode():
  def __init__(self, x_pos, y_pos, learning_delta, vector_size):
    self.loc = (x_pos, y_pos)
    self.n_range = 25 
    self.learning_rate = 1
    self.word = ""
    self.meaning = numpy.random.rand(vector_size)

  def updateTimeValues(self, time):
    if self.learning_rate > .01:
      self.learning_rate = .9*(1-time/1000)
    if self.n_range > 0:
      self.n_range -= math.floor(time/100)

  def similarity(self, meaning_vector):
    """Compute the euclidian similarity of this nodes meaning with the given
    meaning_vector."""
    return distance(meaning_vector, self.meaning)

  def updateMeaning(self, meaning_vector):
    """Update the meaning of this node to incorporate the given meaning vector
    if this node is in the neighborhood of other_node."""
    self.meaning += (self.learning_rate * (meaning_vector - self.meaning))
