#!/usr/bin/python

import getopt
import numpy
import random
import pylab
import sys

from learningAgent import learningAgent

VECTOR_SIZE = 60

class LearningGame():
  def __init__(self, phoneme_file, use_attention, use_context, out_dir):
    self.map = []
    self.objects = []
    self.object_contexts = {}
    self.word_list = []
    self.buildWordList(phoneme_file)
    self.createObjects(60)
    self.createLearners(2, use_attention, use_context)
    self.t = 0
    self.last_pick = 0
    self.time_axis = []
    self.pick_count = []
    self.time_first_seen = {}
    self.out_dir = out_dir + "/"

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
        
  def createLearners(self, num_players, use_attention, use_context):
    phoneme_size = len(self.word_list[0][1])
    self.learners = [learningAgent(VECTOR_SIZE, phoneme_size, self,
                                   use_attention, use_context)
                     for i in range(num_players)]

  def createObjects(self, num_objects):
    for i in range(num_objects):
      new_object = [0 for i in range(VECTOR_SIZE)]
      new_object[0] = random.random()
      new_object[1] = random.random()
      new_object[2] = random.random()
      random.shuffle(new_object)
      self.objects.append(numpy.array(new_object))
    object_indexes = [i for i in range(len(self.objects))]
    for obj_index in range(len(self.objects)):
      possible_contexts = []
      for i in range(4):
        context_indexes = random.sample(object_indexes, 4)
        if obj_index in context_indexes:
          context_indexes.remove(obj_index)
        possible_contexts.append(context_indexes)
      self.object_contexts[obj_index] = possible_contexts
  
  def getContext(self, context_size):
    obj_index = random.randint(0, len(self.objects)-1)
    possible_contexts = self.object_contexts[obj_index]
    context_index = random.randint(0, len(possible_contexts)-1)
    context = [self.objects[i] for i in possible_contexts[context_index]]
    object = self.objects[obj_index]
    return [object] + context

  def addPickCount(self):
    self.time_axis.append(self.t)
    self.pick_count.append(self.t - self.last_pick)
    self.last_pick = self.t

  def pickNewWord(self):
    self.addPickCount()
    word_index = random.randint(0, len(self.word_list) - 1)
    if self.word_list[word_index][0] not in self.time_first_seen:
      self.time_first_seen[self.word_list[word_index][0]] = self.t
    return self.word_list[word_index]

  def playGame(self, num_times):
    for i in range(num_times):
      [speaker, hearer] = random.sample(self.learners, 2)
      context, word = speaker.generateUtterance()
      #print "speaker says: ", word, context
      hearer.receiveUtterance(word, context)
      self.t += 1

    self.addPickCount()
    pylab.clf()
    pylab.xlabel("time")
    pylab.ylabel("time since last word picked")
    pylab.plot(self.time_axis, self.pick_count, 'b')
    pylab.axis([0, self.t, 0, max(self.pick_count)])
    pylab.savefig(self.out_dir + "word_pick.png")

    count = 0
    for learner in self.learners:
      learner.printMap(out_dir, count)
      count += 1

    converge_count = 0
    syn_dict = {}
    for obj in self.objects:
      meanings = set()
      for learner in self.learners:
        _, word = learner.produceWord(obj, False)
        if word:
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
    intro_axis = [self.time_first_seen[key] for key in syn_dict]
    print syn_dict
    print syn_counts
    pylab.clf()
    locs, labels = pylab.xticks(syn_axis, syn_dict.keys())
    pylab.axis([0, max(syn_counts), 0, len(syn_counts)])
    pylab.setp(labels, 'rotation', 'vertical')
    pylab.xlabel("words used")
    pylab.ylabel("polysemy count")
    pylab.plot(syn_axis, syn_counts)
    pylab.savefig(self.out_dir + "homonymy_count.png")

    pylab.clf()
    locs, labels = pylab.xticks(syn_axis, syn_dict.keys())
    pylab.axis([0, max(syn_counts), 0, len(intro_axis)])
    pylab.setp(labels, 'rotation', 'vertical')
    pylab.xlabel("words used")
    pylab.ylabel("time first seen")
    pylab.plot(syn_axis, intro_axis)
    pylab.savefig(self.out_dir + "word_intro.png")

def usage():
  print "usage: ./learningGame.py [options] phoneme_file output_directory"
  print "  options:"
  print "   -c     : use context in learning games"
  print "   -a     : use attention when learning"
  print "   -n NUM : set the number of games to play"

if __name__ == "__main__":
  opts, args = getopt.getopt(sys.argv[1:], 'can:')
  use_context = False
  use_attention = False
  num_games = 1000
  out_dir = "."
  for option, value in opts:
    if option == '-c':
      use_context = True
    if option == '-a':
      use_attention = True
    if option == '-n':
      num_games = int(value)
  if len(args) != 2:
    usage()
    sys.exit(1)
  out_dir = args[1]
  game = LearningGame(args[0], use_attention, use_context, out_dir)
  game.playGame(num_games)
