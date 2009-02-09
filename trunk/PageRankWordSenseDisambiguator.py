#!/usr/bin/python

from nltk.corpus import wordnet as wn
import networkx as nx # for graph
import os.path
import sys
import getopt
import nltk    


def disambiguate(listOfNVNTuples):
    synsetGraph = createGraph(listOfNVNTuples)
    synsetGraph, ranks = computePageRank(synsetGraph)
    # missing step of determining which to use?
    for sense in synsetGraph:
        print "%s -> %f" % (sense, ranks[sense])

# Creates a networkx graph where nodes are the Synset objects and edges indicate
# a semantic relation between the two sysnsets
def createGraph(listOfNVNTuples):
    synsetGraph = nx.Graph()
    print "creating graph for %d triples" % (len(listOfNVNTuples))
    print listOfNVNTuples
    # add all possible synsets from each triplet as vertices in the graph
    for tuple in listOfNVNTuples:
        subject = tuple[0]
        verb = tuple[1]
        object = tuple[2]
        subjectSynsets = wn.synsets(subject, wn.NOUN);
        verbSynsets = wn.synsets(verb, wn.VERB);
        objectSynsets = wn.synsets(object, wn.NOUN);
                
        addSynsetsToGraph(subjectSynsets, synsetGraph)
        addSynsetsToGraph(verbSynsets, synsetGraph)
        addSynsetsToGraph(objectSynsets, synsetGraph)

    # once all the vertices are in place, determine any relationship between
    # them that might link them together
    #
    # NOTE: this is an n^2 algorith, and could probably be improved
    for sense in synsetGraph:
        for other in synsetGraph:
            if (sense == other):
                continue
            # check for 4 different relations
            # 1 - hyponym
            # 2 - hypernym
            # 3 - also_sees
            # 4 - coordinate (i.e. sibling)
            # REMINDER: we could also check for part-of, entails, etc.
            hypernyms = sense.hyponyms()
            if other in sense.hyponyms():
                synsetGraph.add_edge(sense, other)
            elif other in hypernyms:
                synsetGraph.add_edge(sense, other)
            elif other in sense.also_sees():
                synsetGraph.add_edge(sense, other)
            else:
                for hyper in hypernyms:
                    if (other in hyper.hyponyms()):
                        synsetGraph.add_edge(sense, other)
                        break

    print "added %d edges" % (synsetGraph.number_of_edges())
    return synsetGraph

def addSynsetsToGraph(synsets, graph):
    for synset in synsets:
        graph.add_node(synset)
        
def computePageRank(synsetGraph):
    ranks = dict()
    epsilon = .0001
    error = 1.
    # give each sense a nomimal score of 1
    for sense in synsetGraph:
        ranks[sense] = 1
        
    # d = dampening factor
    d = .85
    
    iteration = 0

    # compute the page rank
    while error > epsilon:
        iteration += 1
        print "iteration %d" % (iteration)
        error = 0.
        for sense in synsetGraph:
            neighbors = synsetGraph.neighbors(sense)
            oldRank = ranks[sense]
            rank = 0.
            for neighbor in neighbors:
                outDegree = len(synsetGraph.neighbors(neighbor))
                neighborRank = ranks[neighbor]
                rank += float(neighborRank) / float(outDegree)
            newRank =  (1 - d) + (d * rank);
            ranks[sense] = newRank
            error += abs(newRank - oldRank)

    return (synsetGraph, ranks)

def parseFile(tripletFile):
    infile = open(tripletFile,"r")

    verbToTriples = dict()
    
    while infile:
        line = infile.readline()

        triplet = line.split("|")
        if len(triplet) < 3:
            break

        subject = triplet[0]
        verb = triplet[1]
        object = triplet[2]      
        
        if verb not in verbToTriples:
            triples = []
            verbToTriples[verb] = triples
        triples = verbToTriples[verb]
        triples.append((subject, verb, object))
            
    return verbToTriples

def main(argv=None):
    if argv is None:
        argv = sys.argv
        
    if len(argv) != 2:
        usage();
        return None

    tripletFile = argv[1]
    verbToTriplets = parseFile(tripletFile)
    for verb in verbToTriplets:
        disambiguate(verbToTriplets[verb])

def usage():
    print "usage: <triplet file>"
    return

if __name__ == "__main__":
    sys.exit(main())
