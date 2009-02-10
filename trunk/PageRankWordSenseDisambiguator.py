#!/usr/bin/python

from nltk.corpus import wordnet as wn
import networkx as nx # for graph
import os.path
import sys
import getopt
import nltk    

DAMPENING_FACTOR = .85

def disambiguate(listOfNVNTuples):
    synsetGraph = createGraph(listOfNVNTuples)
    ranks = computePageRank(synsetGraph)

    # check to see if any of the vertices in the graph have a rank greater than
    # the dampening factor
    uninformativeConvergence = True
    for rank in ranks.values():
        if rank != (1 - DAMPENING_FACTOR):
            uninformativeConvergence = False
            break
        
    if uninformativeConvergence:
        print "all tuples converged to the dampening factor"
    else:
        for sense in synsetGraph:
            print "%s -> %f" % (sense, ranks[sense])
    # missing step of determining which to use?

# Creates a networkx graph where nodes are the Synset objects and edges indicate
# a semantic relation between the two sysnsets
def createGraph(listOfNVNTuples):
    synsetGraph = nx.Graph()
    print "creating graph for %d triples" % (len(listOfNVNTuples))
    # print listOfNVNTuples
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
    # NOTE: this is an n^2 algorithm, and could probably be improved
    alreadyCompared = set()
    for sense in synsetGraph:
        # add in the identity set because we don't need to compare that
        alreadyCompared.add((sense, sense)) 
        hypernyms = sense.hyponyms()

        for other in synsetGraph:
            if (sense, other) in alreadyCompared:
                continue
            # add in the tuples indicating that this pair-wise comparison has
            # already been examined
            alreadyCompared.add((sense, other))
            alreadyCompared.add((other, sense))

            # check for all the different WordNet relations
            # and the coordinate relation (i.e. sibling, share a hypernym)
            if other in sense.hyponyms():
                synsetGraph.add_edge(sense, other)
            elif other in hypernyms:
                synsetGraph.add_edge(sense, other)
            elif other in sense.also_sees():
                synsetGraph.add_edge(sense, other)
            elif other in sense.instance_hypernyms():
                synsetGraph.add_edge(sense, other)
            elif other in sense.instance_hyponyms():
                synsetGraph.add_edge(sense, other)
            elif other in sense.member_holonyms():
                synsetGraph.add_edge(sense, other)
            elif other in sense.substance_holonyms():
                synsetGraph.add_edge(sense, other)
            elif other in sense.part_holonyms():
                synsetGraph.add_edge(sense, other)
            elif other in sense.member_meronyms():
                synsetGraph.add_edge(sense, other)
            elif other in sense.substance_meronyms():
                synsetGraph.add_edge(sense, other)
            elif other in sense.part_meronyms():
                synsetGraph.add_edge(sense, other)
            elif other in sense.attributes():
                synsetGraph.add_edge(sense, other)
            elif other in sense.entailments():
                synsetGraph.add_edge(sense, other)
            elif other in sense.causes():
                synsetGraph.add_edge(sense, other)
            elif other in sense.verb_groups():
                synsetGraph.add_edge(sense, other)
            elif other in sense.similar_tos():
                synsetGraph.add_edge(sense, other)
            else: #coordinate term check
                for hyper in hypernyms:
                    if (other in hyper.hyponyms()):
                        synsetGraph.add_edge(sense, other)
                        break

    print "added %d edges" % (synsetGraph.number_of_edges())
    return synsetGraph

# adds the synset as a vertex in the provided networkx graph
def addSynsetsToGraph(synsets, graph):
    for synset in synsets:
        graph.add_node(synset)
        
# computes the PageRank algorithm on the provided networkx graph and returns a
# dict mapping each of the vertices to its rank
def computePageRank(synsetGraph):
    ranks = dict()
    epsilon = .0001
    error = 1.
    # give each sense a nomimal score of 1
    for sense in synsetGraph:
        ranks[sense] = 1
        
    d = DAMPENING_FACTOR
    
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

    return ranks

# returns a mapping of verb to the tuples that use that verb, and a sorted list
# of verbs in decreasing order of number of tuples.
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

    # sort the list so that the verbs with the most triples come first
    sortedVerbTuples = []
    for verb, triples in verbToTriples.iteritems():
        sortedVerbTuples.append((verb, len(triples)))
    sortedVerbTuples.sort(revTupleComp)
    
    sortedVerbs = []
    for (verb, n)  in sortedVerbTuples:
        sortedVerbs.append(verb)
    
#    print "loaded %d verbs, in decreasing order of # of tuples: %s" % (len(sortedVerbs), sortedVerbs)
    
        
    return verbToTriples, sortedVerbs

def revTupleComp(x, y):
    i = x[1]
    j = y[1]
    if i < j:
        return 1
    elif i == j:
        return 0
    else:
        return -1
    
def main(argv=None):
    if argv is None:
        argv = sys.argv
        
    if len(argv) != 2:
        usage();
        return None

    tripletFile = argv[1]
    verbToTriplets, sortedVerbs = parseFile(tripletFile)    

    for verb in sortedVerbs:
        disambiguate(verbToTriplets[verb])

def usage():
    print "usage: <triplet file>"
    return

if __name__ == "__main__":
    sys.exit(main())
