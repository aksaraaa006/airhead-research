#!/usr/bin/python

import numpy
import random
import sys

from learningAgent import learningAgent

VECTOR_SIZE = 60

class LearningGame():
  def __init__(self):
    self.map = []
    self.objects = []
    self.createObjects(60)
    self.createLearners(2)

  def createLearners(self, num_players):
    self.learners = [learningAgent(VECTOR_SIZE, self) for i in
                     range(num_players)]

  def createObjects(self, num_objects):
    for i in range(num_objects):
      new_object = [0 for i in range(VECTOR_SIZE)]
      new_object[0] = random.random()
      new_object[1] = random.random()
      new_object[2] = random.random()
      random.shuffle(new_object)
      self.objects.append(numpy.array(new_object))
  
  def playGame(self, num_times):
    for i in range(num_times):
      [speaker, hearer] = random.sample(self.learners, 2)
      word, context = speaker.generateUtterance()
      #print "speaker says: ", word, context
      hearer.recieveUtterance(word, context)
    for learner in self.learners:
      print "mappings for learner"
      learner.printMap()

if __name__ == "__main__":
  game = LearningGame()
  game.playGame(int(sys.argv[1]))

