#!/usr/bin/python

from nltk.corpus import wordnet as wn
import os.path
import sys
import getopt
import nltk    
import pickle

import thread
import Queue

# Loads the SAT questions from a file and returns a list of tuples of the form
# ((source pair), (list of possible target pair tuples), <int of which tuple in
# the list is correct>
def loadQuestions(SATfile):    
    infile = open(SATfile, "r")
    print "loading %s" % (SATfile)
    questions = []

    while infile:
        line = infile.readline();
        if len(line) == 0:
            break
        elif line.isspace():
            continue
        elif line[0] == "#":
            continue

        # print "header: %s" % (line)

        # questions are 7 lines long including header
    
        source = infile.readline().strip().split("\t")
        opt1 = infile.readline().strip().split("\t")
        opt2 = infile.readline().strip().split("\t")
        opt3 = infile.readline().strip().split("\t")
        opt4 = infile.readline().strip().split("\t")
        opt5 = infile.readline().strip().split("\t")
        correct = infile.readline().strip()
        correctIndex = -1
        if correct == "a":
            correctIndex = 0
        elif correct == "b":
            correctIndex = 1
        elif correct == "c":
            correctIndex = 2
        elif correct == "d":
            correctIndex = 3
        else: # "e"
            correctIndex = 4

        # write the question out as a tuple of the source analogy, a list of
        # tuples of target analogies,and which index contains the correct option
        questions.append(((source[0], source[1]),
                          ((opt1[0], opt1[1]),
                           (opt2[0], opt2[1]),
                           (opt3[0], opt3[1]),
                           (opt4[0], opt4[1]),
                           (opt5[0], opt5[1])),
                          correctIndex))
    return questions
    
def getSimilarityOfMostSimilarPair(listOfAnalogousPairs, sourcePair):
    highestSimilarity = 0
    for a, b in listOfAnalogousPairs:
        similarity = (getWordSimilarity(sourcePair[0],a) +
                      getWordSimilarity(sourcePair[1],b))
        if (similarity >= highestSimilarity):
            highestSimilarity = similarity
    return highestSimilarity

def getAverageSimilarity(listOfAnalogousPairs, sourcePair):
    similaritySum = 0.0
    for a, b in listOfAnalogousPairs:
        similaritySum += (getWordSimilarity(sourcePair[0],a) +
                          getWordSimilarity(sourcePair[1],b))
    return similaritySum / float(len(listOfAnalogousPairs))

def answerSATQuestion(SATQuestion, listOfListOfAnalogousPairs):

    source = SATQuestion[0];
    print "Question %s:%s" % (source)
    options = SATQuestion[1];
    for option in options:
        print "\t%s:%s" % (option)
    correctIndex = SATQuestion[2];

    listIndex = 0
    listOfScoreAndList = []

    # find the list of tuples which maximizes the similarity to the source pair
    for list in listOfListOfAnalogousPairs:
        similarity = getAverageSimilarity(list, source) 
        listOfScoreAndList.append((similarity, list))
    
    # sort with highest similarity first (inverse sort routine)
    listOfScoreAndList.sort(invComp)
    
    if len(listOfScoreAndList) == 0:
        print "could not find answer???"
        return False

    # In decreasing order of similarity to the source, use the list of analogous
    # items to look for the list whose similarity is closest to the options
    for score, list in listOfScoreAndList:
        
        mostSimilar = (0, ())
        for a, b in list:
            
            # look at each of the options and see which of them has the highest
            # similarity to this list of analogous pairs
            for c, d in options:
                similarity = (getWordSimilarity(a, c)
                              + getWordSimilarity(b, d))
                if (similarity > mostSimilar[0]):
                    mostSimilar = (similarity, (c, d))

        try:
            print "SELECTING: %s:%s::%s:%s" % (source[0], source[1], 
                                               (mostSimilar[1])[0],
                                               (mostSimilar[1])[1])
        except IndexError:
            print "BLARGH: fix this case"
            return false
        
        if ((mostSimilar[1])[0] == (options[correctIndex])[0] and 
            (mostSimilar[1])[1] == (options[correctIndex])[1]):
            print "CORRECT!"
            return True
        else:
            print "INCORRECT: %s:%s::%s:%s" % (source[0], source[1], 
                                               (options[correctIndex])[0],
                                               (options[correctIndex])[1])
            return False

def invComp(x, y):
    if x < y:
        return 1
    elif x == y:
        return 0
    else:
        return -1


def findBestRelationForList(listOfAnalogousPairs):

    memberHolonymSimSum = 0.0
    substanceHolonymSimSum = 0.0
    partHolonymSimSum = 0.0

    memberMeronymSimSum = 0.0
    substanceMeronymSimSum = 0.0
    partMeronymSimSum = 0.0

    attributeSimSum = 0.0
    entailmentSimSum = 0.0
    causesSimSum = 0.0

    for a, b in listOfAnalogousPairs:
        synsets1 = wn.synsets(a, wn.NOUN)
        synsets2 = wn.synsets(b, wn.NOUN)
 
        # sums of the highest similarity score for each pair-wise synset
        # relationship search
        memberMerSim = 0
        substanceMerSim = 0
        partMerSim = 0

        memberHolSim = 0
        substanceHolSim = 0
        partHol = 0
        
        attrSim = 0
        entailSim = 0
        causesSim = 0

        # for the pair-wise comparison of synsets between two words, find the
        # relation (part-whole, meronym, cause, etc.) that minimizes the distance
        # between the two
        for s1 in synset1:

            # load all of comparison sysets
            memberMer = s1.member_meronyms()
            substanceMer = s1.substance_meronyms()
            partMer = s1.part_meronyms()
            
            memberHol = s1.member_holonyms()
            substanceHol = s1.substance_holonyms()
            partHolSim = s1.part_holonyms()

            attributes = s1.attributes()
            entailments = s1.entailments()
            causes = s1.causes()

            # See how similar the second synset is the related-set from the
            # first.  For example, if the relation ship is "cause", and we see
            # spark.causes() = {flame}, then any synset that is similar to
            # "flame" (e.g. fire) should have a close hypernym relation to it.
            #
            # Therefore, for each item in the sets related to the first,
            # calculate the scaled similarity between it.  The set that contains
            # the synset with the highest similarity could likely indicate which
            # type of relationship exists between the words (over all the
            # synsets)
            for s2 = synset2:

                for (s3 in memberMer):
                    sim = getScaledSimilarity(s3, s2)
                    if (sim > memberMerSim):
                        memberMerSim = sim
                for (s3 in substanceMer):
                    sim = getScaledSimilarity(s3, s2)
                    if (sim > substanceMerSim):
                        substanceMerSim = sim
                for (s3 in partMer):
                    sim = getScaledSimilarity(s3, s2)
                    if (sim > partMerSim):
                        partMerSim = sim

                for (s3 in memberHol):
                    sim = getScaledSimilarity(s3, s2)
                    if (sim > memberHolSim):
                        memberHolSim = sim
                for (s3 in substanceHol):
                    sim = getScaledSimilarity(s3, s2)
                    if (sim > substanceHolSim):
                        substanceHolSim = sim
                for (s3 in partHol):
                    sim = getScaledSimilarity(s3, s2)
                    if (sim > partHolSim):
                        partHolSim = sim

                for (s3 in attributes):
                    sim = getScaledSimilarity(s3, s2)
                    if (sim > attrSim):
                        attrSim = sim
                for (s3 in entailments):
                    sim = getScaledSimilarity(s3, s2)
                    if (sim > entailSim):
                        entailSim = sim
                for (s3 in causes):
                    sim = getScaledSimilarity(s3, s2)
                    if (sim > causesSim):
                        causesSim = sim
        
        # once all the pair-wise combinations for the each synset of the two
        # words have been computed add the highest similarity score to the
        # appropriate sum for the list.


        listOfSims = (memberMerSim, substanceMerSim, partMerSim,
                      memberHolSim, substanceHolSim, partHolSim,
                      attrSim, entailSim, causesSim)


        substanceMerSim
        partMerSim

        memberHolSim
        substanceHolSim
        partHolSim
        
        attrSim
        entailSim
        causesSim

        if isMax(memberHolonymSimSum, listOfSims):
            memberHolonymSimSum += memberHolSim
        elif isMax(substanceHolonymSimSum, listOfSims):
            substanceHolonymSimSum += substanceHolSim
        elif isMax(partHolonymSimSum, listOfSims):
            partHolonymSimSum += partHolSim

        elif isMax(memberMerSim, listOfSims):
            memberMeronymSimSum += memberMerSim
        elif isMax(substanceMerSim, listOfSims):
            substanceMeronymSimSum += substanceMerSim
        elif isMax(partMeronymSimSum, listOfSims):
            partMeronymSimSum += partMerSim

        elif isMax(attributeSimSum, listOfSims):
            attributeSimSum += attrSim
        elif isMax(entailmentSimSum, listOfSims):
            entailmentSimSum += entailSim
        else:
            causesSimSum += causesSim
    


def isMax(val, listOfVals):
    for v in listOfVals:
        if v > val:
            return False
    return True

def getWordSimilarity(word1, word2):
    # NB: I sure hope both of these words are nouns!
    synsets1 = wn.synsets(word1, wn.NOUN)
    synsets2 = wn.synsets(word2, wn.NOUN)
    
    highestSimilarity = 0
    for s1 in synsets1:
        for s2 in synsets2:
            similarity = getScaledSimilarityMeasure(s1, s2)
            if (similarity > highestSimilarity):
                highestSimilarity = similarity
            
    return highestSimilarity

# returns the normalized depth of the least common ancestor of the two synsets
# scaled by the depth of the synsets themselves
def getScaledSimilarityMeasure(synset1, synset2):
    similarity = getSimilarityMeasure(synset1, synset2)
    normDepth1 = getNormalizedDepth(synset1)
    normDepth2 = getNormalizedDepth(synset2)
    return (2.0 * similarity) / float(normDepth1 + normDepth2)

# returns the normalized depth of the least common ancestor of the two synsets
def getSimilarityMeasure(synset1, synset2):
    #print "getSimMeas(%s, %s)" % (synset1, synset2)
    ancestors = []
    try:
        ancestors = synset1.lowest_common_hypernyms(synset2)
    except ValueError:
        #print "ERROR: couldn't find common ancestor of %s and %s" % (synset1, synset2)
        # PANIC: just add in all the root hypernyms of each synset
        if True:
            return 0
        else:
            ancestors = set()
            for r in synset1.root_hypernyms():
                ancestors.add(r)
            for r in synset2.root_hypernyms():
                ancestors.add(r)

    # NOTE: what do we do if there are more than one?  For now, choose the
    # deepest ancestor in hopes that it is the cloests to the two synsets
    deepest = (0, ())
    for ancestor in ancestors:
        depth = getDepth(ancestor)
        if depth >= deepest[0]:
            deepest = (depth, ancestor)
    
        
    return getNormalizedDepth(deepest[1])

# Returns the depth of the synset divided by the average depth of its
# descendants
def getNormalizedDepth(synset):
    return float(getDepth(synset)) / float(getAverageDescendantDepth(synset))

AVG_DESCENDENT_DEPTH_CACHE = dict()
# Returns the average depth of all leaf hyponyms under this synset
def getAverageDescendantDepth(synset):
    #print "looking for avg. descendent depth of %s" % (synset)
    
    # DEBUG: REMOVE ASAP
    if synset == wn.synset('entity.n.01'):
        return 9.396485
    if synset == wn.synset('abstraction.n.06'):
        return 8.695125
    if synset == wn.synset('physical_entity.n.01'):
        return 10.094137
    if synset == wn.synset('social_group.n.01'):
        return 8.550117
    if synset == wn.synset('psychological_feature.n.01'):
        return 9.650382
    if synset == wn.synset('whole.n.02'):
        return 10.663869
    if synset == wn.synset('group.n.01'):
        return 8.128212
    if synset == wn.synset('object.n.01'):
        return 10.490863
    if synset == wn.synset('organism.n.01'):
        return 11.179508
    
    if synset not in AVG_DESCENDENT_DEPTH_CACHE:    
        #print "(%s was not cached)" % (synset)
        depth = getDepth(synset)
        # find all of the leaf hyponyms under it and compute their depth
        hyponyms = []
        for s in synset.hyponyms():
            hyponyms.append(s)
        alreadySeen = set()
        depthSum = 0
        leaves = 0

        for hyponym in hyponyms:
            if hyponym in alreadySeen:
                continue
            alreadySeen.add(hyponym)
            nextLevel = hyponym.hyponyms()
            # if the synset doesn't have any hyponyms, it must be a leaf, so
            # compute its depth and it to the running sum
            if not nextLevel:
                depthSum += getDepth(hyponym)
                leaves += 1
            elif len(nextLevel) == 0:
                depthSum += getDepth(hyponym)
                leaves += 1
            # otherwise add all of the synset's hyponyms that haven't already
            # been seen
            else:
                for hyp in nextLevel:
                    if hyp not in alreadySeen:
                        hyponyms.append(hyp)
        
        avgDescDepth = float(depthSum) / float(leaves) if leaves > 0 else depth
#        if leaves > 1000:
#            print "%s: depth %f, leaves %d, avg desc. depth %f" % (synset, depth, leaves, avgDescDepth)
        AVG_DESCENDENT_DEPTH_CACHE[synset] = avgDescDepth
        
    return AVG_DESCENDENT_DEPTH_CACHE[synset]

DEPTH_CACHE = dict()
# Returns the depth of the synset from the root hypernym(s)
def getDepth(synset):
    #print "getting depth of %s" % (synset)
    if synset not in DEPTH_CACHE:
        hypernymPaths = synset.hypernym_paths()
        # NOTE: what to do when there are more than one paths?
        pathLenSum = 0
        for path in hypernymPaths:
            pathLenSum += len(path)
        avgDepth = float(pathLenSum) / len(hypernymPaths)
        DEPTH_CACHE[synset] = avgDepth
    depth = DEPTH_CACHE[synset]
    #print "depth(%s) = %f" % (synset, depth)
    return depth

correct = Queue.Queue()
questions = Queue.Queue()
NUM_THREADS = 1

def main(argv=None):
    if argv is None:
        argv = sys.argv
        
    if len(argv) != 3:
        usage();
        return None

    # parse and load all of the SAT questions into a list of tuples
    listOfQuestions = loadQuestions(argv[1])
    for q in listOfQuestions:
        questions.put(q, False)

    # unpickle the list of list of pairs of analogous words.
    fileHandle = open(argv[2]);
    listOfListOfAnalogousPairs = pickle.load(fileHandle)

    if NUM_THREADS > 1:
        for i in range(NUM_THREADS):
            t = thread.start_new_thread(answerQuestion, (listOfListOfAnalogousPairs, ()))
        
        questions.join()
        print "Answered %d/%d correct" % (len(correct), len(listOfQuestions))

    else:
        # compute the answers and calculate how many we got right
        correctAns = 0
        for question in listOfQuestions:
            if answerSATQuestion(question, listOfListOfAnalogousPairs):
                correctAns += 1
        print "Answered %d/%d correct" % (correctAns, len(listOfQuestions))


def answerQuestion(listOfListOfAnalogousPairs, dummy):
    while True:
        try:
            question = questions.get(False)        
            if answerSATQuestion(question, listOfListOfAnalogousPairs):
                correct.put(1)
        except Queue.Empty:
            return

        

def usage():
    print "usage: <SAT questions file> <analogous-pairs pickle>"
    return

if __name__ == "__main__":
    sys.exit(main())
