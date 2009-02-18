#!/usr/bin/python
from math import sqrt
from nltk.corpus import wordnet as wn
import os.path
import sys
import getopt
import nltk    
import pickle

NUM_SYNSETS=10

def findParents(listOfAnalogousPairs):

    synsetToFirstValsDominated = dict()
    synsetToSecondValsDominated = dict()

    numPairs = float(len(listOfAnalogousPairs))
    roots = set()

    print "marking parents"

    # for each of the synsets, mark all of the parents on the path to the root
    # node has having dominated that synset.  This is equivalent to incrementing
    # each parent's count of how many synsets it dominates
    for (a, aSyn), (b, bSyn) in listOfAnalogousPairs:
        try:
            i = wn.synset(aSyn)
            j = wn.synset(bSyn)
            incrementParents(i, synsetToFirstValsDominated)
            incrementParents(j, synsetToSecondValsDominated)
            for r in i.root_hypernyms():
                roots.add(r)
            for r in j.root_hypernyms():
                roots.add(r)
        except ValueError:
            continue

    print "calculating information gain"

    # compute the entropy of each parent, weighting it by the depth of the node
    synsetToFirstInformationGain = computeInformationGain(synsetToFirstValsDominated, 
                                                          roots, numPairs)
    synsetToSecondtInformationGain = computeInformationGain(synsetToSecondValsDominated, 
                                                            roots, numPairs)
    
    aIG = synsetToFirstInformationGain.items()
    bIG = synsetToSecondtInformationGain.items()

    # sort according to the information gain
    aIG.sort(cmp)
    bIG.sort(cmp)

    for i in range(min(len(bIG), min(len(aIG), NUM_SYNSETS))):
        print "%s (%.2f) : %s (%.2f)" % (aIG[i][0].name, aIG[i][1], 
                                         bIG[i][0].name, bIG[i][1])
        #print aIG[i]
        #print bIG[i]


def cmp(x, y):
    if y[1] > x[1]:
        return 1
    elif y[1] < x[1]:
        return -1
    else:
        return 0

def computeInformationGain(synsetToValuesDominated, roots, totalPairs):

    synsetToInformationGain = dict()

    children = []
    for r in roots:
        if r in synsetToValuesDominated:
            children.append(r)
    
    alreadySeen = set()

    for child in children:
        if child in alreadySeen:
            continue
        alreadySeen.add(child)
        
        depth = getDepth(child)

        # weight the information gain by the dept of te synset in the tree
        synsetToInformationGain[child] = ((synsetToValuesDominated[child] / totalPairs)
                                          * (sqrt(depth)))
        
        hyponyms = child.hyponyms()
        if hyponyms == None or len(hyponyms) == 0:
            continue

        for hyp in hyponyms:
            # if we haven't already calcuated its information gain and if the
            # synset actually dominates at least one pair
            if hyp not in alreadySeen and hyp in synsetToValuesDominated:
                children.append(hyp)

    return synsetToInformationGain

    

def incrementParents(synset, synsetToValuesDominated):

    parents = []
    parents.append(synset)
    alreadySeen = set()

    for parent in parents:
        if parent in alreadySeen:
            continue

        if parent not in synsetToValuesDominated:
            synsetToValuesDominated[parent] = 1
        else:
            val = synsetToValuesDominated[parent]
            synsetToValuesDominated[parent] = val + 1

        # add in all the hypernyms (parents) we haven't already processed
        hypernyms = parent.hypernyms()
        
        # if this parent was a root node, keep toing
        if hypernyms == None or len(hypernyms) == 0:
            continue
        # otherwise add all of the parent's parents that haven't already been
        # seen
        else:
            for hyp in hypernyms:
                if hyp not in alreadySeen:
                    parents.append(hyp)
    return

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


def cmp2(x, y):
    l1, p1 = x
    l2, p2 = y
    return len(l1) - len(l2)

def main(argv=None):
    if argv is None:
        argv = sys.argv
        
    if len(argv) != 2:
        usage()
        return None

    print "Loading lists"
    infile = open(argv[1]);
    listOfLists = pickle.load(infile);

    print "sorting lists by size"
    listOfLists.sort(cmp2)


    for list, parent in listOfLists:
        if len(list) <= 5 or len(list) > 50:
            #print "skipping empty list"
            continue
        print "finding parents for:"
        print list
        findParents(list)

def usage():
    print "usage: <list of lists pickle file>"
    return

if __name__ == "__main__":
    sys.exit(main())
