#!/usr/bin/python

from nltk.corpus import wordnet as wn
import os.path
import sys
import getopt
import nltk    
import pickle

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
    

def answerSATQuestion(SATQuestion, listOfListOfAnalogousPairs):

    source = SATQuestion[0];
    print "Question %s:%s" % (source)
    options = SATQuestion[1];
    for option in options:
        print "\t%s:%s" % (option)
    correctIndex = SATQuestion[2];

    listIndex = 0
    listOfScoreAndList = []
    listToHighestSimilarity = dict()

    # find the list of tuples which maximizes the similarity to the source pair
    for list in listOfListOfAnalogousPairs:
        highestSimilarity = 0
        for a, b in list:
            similarity = (getWordSimilarity(source[0],a)
                          + getWordSimilarity(source[1],b))
            if (similarity >= highestSimilarity):
                highestSimilarity = similarity
        listToHighestSimilarity[listIndex] = highestSimilarity
        listOfScoreAndList.append((highestSimilarity, list))
    
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

        print "SELECTING: %s:%s::%s:%s" % (source[0], source[1], 
                                           (mostSimilar[1])[0],
                                           (mostSimilar[1])[1])
        
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
        
        avgDescDepth = float(depthSum) / float(leaves)if leaves > 0 else depth
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


def main(argv=None):
    if argv is None:
        argv = sys.argv
        
    if len(argv) != 3:
        usage();
        return None

    # parse and load all of the SAT questions into a list of tuples
    listOfQuestions = loadQuestions(argv[1])
    
    # unpickle the list of list of pairs of analogous words.
    fileHandle = open(argv[2]);
    listOfListOfAnalogousPairs = pickle.load(fileHandle)

    # compute the answers
    correct = 0
    for question in listOfQuestions:
        if answerSATQuestion(question, listOfListOfAnalogousPairs):
            correct += 1
    print "Answered %d/%d correct" % (correct, len(listOfQuestions))


def usage():
    print "usage: <SAT questions file> <analogous-pairs pickle>"
    return

if __name__ == "__main__":
    sys.exit(main())
