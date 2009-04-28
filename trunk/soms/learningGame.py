#!/usr/bin/python

import numpy
import random
import pylab
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
    self.t = 0
    self.last_pick = 0
    self.time_axis = []
    self.pick_count = []

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
  
  def pickNewWord(self):
    self.time_axis.append(self.t)
    self.pick_count.append(self.t - self.last_pick)
    self.last_pick = self.t
    word_index = random.randint(0, len(self.word_list) - 1)
    return self.word_list[word_index]

  def playGame(self, num_times):
    for i in range(num_times):
      [speaker, hearer] = random.sample(self.learners, 2)
      context, word = speaker.generateUtterance()
      #print "speaker says: ", word, context
      hearer.receiveUtterance(word, context)
      self.t += 1

    pylab.plot(self.time_axis, self.pick_count, 'b')
    pylab.show()

    for learner in self.learners:
      learner.printMap()

    converge_count = 0
    syn_dict = {}
    for obj in self.objects:
      meanings = set()
      for learner in self.learners:
        suc, _, word = learner.produceWord(obj, False)
        if suc:
          meanings.add(word[0])
          if word[0] in syn_dict:
            syn_dict[word[0]] += 1
          else:
            syn_dict[word[0]] = 1
        else:
          meanings.add("")
      if len(meanings) == 1 and "" not in meanings: 
        converge_count += 1
    print "number of converging mappings %f" %(converge_count /
                                               float(len(self.objects)))
    syn_counts = [syn_dict[key] for key in syn_dict]
    syn_axis = [i for i in range(len(syn_dict))]
    print syn_dict
    print syn_counts
    pylab.plot(syn_axis, syn_counts)
    locs, labels = pylab.xticks(syn_axis, syn_dict.keys())
    pylab.axis([0, max(syn_counts), 0, len(syn_counts)])
    pylab.setp(labels, 'rotation', 'vertical')
    pylab.show()

if __name__ == "__main__":
  game = LearningGame(sys.argv[2])
  game.playGame(int(sys.argv[1]))
