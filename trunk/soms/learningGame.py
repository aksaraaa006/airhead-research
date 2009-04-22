#!/usr/bin/python

import numpy
import random
import sys

from learningAgent import learningAgent

VECTOR_SIZE = 60

class LearningGame():
  def __init__(self, phoneme_file):
    self.map = []
    self.objects = []
    self.word_list = []
    self.buildWordList(phoneme_file)
    self.createObjects(60)
    self.createLearners(2)

  def buildWordList(self, phoneme_file):
    phonemes = open(phoneme_file)
    for line in phonemes:
      phoneme = line.split()
      word = phoneme[0]
      phoneme_values = phoneme[4:]
      phoneme_rep = numpy.zeros((len(phoneme_values)))
      for i in range(len(phoneme_values)):
        phoneme_rep[i] = float(phoneme_values[i])
      self.word_list.append((word, phoneme_rep))
        
  def createLearners(self, num_players):
    phoneme_size = len(self.word_list[0][1])
    self.learners = [learningAgent(VECTOR_SIZE, phoneme_size, self)
                     for i in range(num_players)]

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
      context, word = speaker.generateUtterance()
      #print "speaker says: ", word, context
      hearer.receiveUtterance(word, context)

    for learner in self.learners:
      learner.printMap()

    converge_count = 0
    for obj in self.objects:
      meanings = set()
      for learner in self.learners:
        suc, _, word = learner.produceWord(obj, False)
        if suc:
          meanings.add(word[0])
        else:
          meanings.add("")
      print meanings
      if len(meanings) == 1 and "" not in meanings: 
        converge_count += 1
    print "number of converging mappings %f" %(converge_count /
                                               float(len(self.objects)))

if __name__ == "__main__":
  game = LearningGame(sys.argv[2])
  game.playGame(int(sys.argv[1]))
