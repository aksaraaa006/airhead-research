#!/usr/bin/python

from nltk.corpus import wordnet as wn
import os.path
import sys
import getopt
import nltk    
import pickle

# question responses
CORRECT = 0
INCORRECT = 1
NOT_FOUND = 2

# whether to expand the various lists when looking for analogies
EXPAND_OPTION_PAIRS = False
EXPAND_EXAMPLE_PAIR = False
CONSIDER_HYPERNYMS = True

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

    print "loaded %d questions" % (len(questions))
    return questions

def loadListFromFile(listFile):
    if True:
        handle = open(listFile)
        listOfLists = pickle.load(handle)
        listSum = 0;
        for list, parent in listOfLists:
            listSum += len(list)
        print "loaded %d lists of avg length %d" % (len(listOfLists), 
                                                    listSum / len(listOfLists))
        return listOfLists
    else:
        # TODO: load from text file
        return []

def buildInverseIndices(listOfListOfAnalogousPairs):

    # these will map words to the lists that contain them.  We use two lists
    # here to speed up indexing based on either word in the related pair
    firstInvIndex = dict()
    firstSynsetInvIndex = dict()
    secondInvIndex = dict()
    secondSynsetInvIndex = dict()
    
    # Iterater through each list, mapping each word to the set of lists in which
    # it occurs.  We need to use a set of lists instead of a lists of lists to
    # ensture that duplciates are removed.  Otherwise, we will later incur a
    # heavy computational overhead for processing multiple lists that actually
    # identical.
    #
    # Note that because sets cannot contain mutable objects such as lists, we
    # are required to map a word to the set of indices of the lists in which the
    # word occurs.
    listIndex = 0
    for listOfPairs, parents in listOfListOfAnalogousPairs:
        for (a, aSyn), (b, bSyn) in listOfPairs:
            # update the set of list indicies for the first element
            if firstInvIndex.has_key(a):
                lists = firstInvIndex[a]
                lists.add(listIndex);
            else:
                lists = set();
                lists.add(listIndex)
                firstInvIndex[a] = lists
                                
            # then for the second element
            if secondInvIndex.has_key(b):
                lists = secondInvIndex[b]
                lists.add(listIndex);
            else:
                lists = set();
                lists.add(listIndex)
                secondInvIndex[b] = lists
                
            # then for the synsets
            if firstSynsetInvIndex.has_key(aSyn):
                lists = firstSynsetInvIndex[aSyn]
                lists.add(listIndex);
            else:
                lists = set();
                lists.add(listIndex)
                firstSynsetInvIndex[aSyn] = lists
                                
            # then for the second element
            if secondSynsetInvIndex.has_key(bSyn):
                lists = secondSynsetInvIndex[bSyn]
                lists.add(listIndex);
            else:
                lists = set();
                lists.add(listIndex)
                secondSynsetInvIndex[bSyn] = lists

                
        listIndex += 1
                
    return (firstInvIndex, firstSynsetInvIndex, 
            secondInvIndex, secondSynsetInvIndex)

# Given some list of pairs (a,b), returns a list at least as large whose pairs
# are generated using semantically related words to the original pairs.
def expandList(listOfPairs):

    expanded = []
    first = set()
    second = set()
    for a, b in listOfPairs:
        first.add(a)
        second.add(b)
        # generate the synonyms, hypernyms and hyponyms of a and b, placing them
        # in the appropriate set
        
        
    # create the list by pair-wise combination of all the expanded terms
    for f in first:
        for s in second:
            expanded.append((f,s))
    
    return expanded

def selectOptionFromList(options, listIndicesWithExampleAnalogy, 
                         firstInvIndex,
                         secondInvIndex,
                         listOfListOfAnalogousPairs):

    optionIndexToList = dict()

    selectedOption = -1
    
    for index in listIndicesWithExampleAnalogy:
        listWithAnalogy, parents = listOfListOfAnalogousPairs[index]
        
        optionIndex = 0
        for c, d in options:
            if c in firstInvIndex and d in secondInvIndex:
                listIndices = set(firstInvIndex[c])
                listIndices.intersection(secondInvIndex[d])
                
                # add whatever list indices were found to the mapping from
                # option to list that supports the option as being correct
                if optionIndex in optionIndexToList:
                    indices = optionIndexToList[optionIndex]
                    for index in listIndices:
                        indices.add(index)
                else:
                    optionIndexToList[optionIndex] = listIndices
            optionIndex += 1
                    
    # hopefully we found only one list or in thoses list we only found one
    # option.
    if len(optionIndexToList) == 1:
        selectedOption = optionIndexToList.keys()[0];                      
        
    # otherwise, if we have more than one option to chooose from, [insert
    # text here]
    elif len(optionIndexToList) > 1:
        print "EDGE: found key in multiple lists, and then value in multiple lists"
        # IDEA: choose the largest list (it has the most examples of the
        # relation)?
        largestListSize = -1
        opt = -1
        for option, list in optionIndexToList.items():
            if len(list) > largestListSize:
                largestListSize = len(list)
                opt = option
        selectedOption = opt
        
    # if we didn't find any options, then consider looking at the hypernyms of
    # the synsets in the list.
    elif CONSIDER_HYPERNYMS:
            
        optionIndex = 0
        closestMatch = sys.maxint # really large value

        for c, d in options:
            #print "searching for list that is most similar to %s:%s" % (c,d)

            # generate a list of all the synset combinations for c and d
            #print "generating alternate pairs"
            pairs = generateSynsetPairs(c,d)
            #print "created %d alternates" % (len(pairs))
            
            # look at each list that we determined might have the target analogy
            for index in listIndicesWithExampleAnalogy:

                listWithAnalogy, parents = listOfListOfAnalogousPairs[index]

                # for each alternate, look at the distance between it at all the
                # target lists.  If some list contains a pair whose distance the
                # closest we've seen to the alternate, then we should choose
                # that option
                for e, f in pairs:
                    dist = findClosestDistInList(e, f, listWithAnalogy)
                    if dist < closestMatch:
                        closestMatch = dist
                        selectedOption = optionIndex
                        
            # increment the option index to point to the option we are looking
            # at next
            optionIndex += 1
                        
             
    return selectedOption


#
# Returns the closest distance between (c,d) and each (a,b) pair in the provided
# list.  The distance function is determined by the wordnet
# sysnet.shortest_path_distance function
#
def findClosestDistInList(c, d, listWithAnalogy):
    
    closestDist = sys.maxint

    # compute the distance between each pair and all the options
    # in the liste
    for (a, aSyn), (b, bSyn) in listWithAnalogy:
        # check that we actually have a synset listed for the word
        if aSyn == "None" or bSyn == "None":
            continue
        try:
            dist1 = getShortestPathDistance(c, wn.synset(aSyn))
            dist2 = getShortestPathDistance(d, wn.synset(bSyn))
            # check that there was a path for both synsets
            if dist1 == -1 and dist2 == -1:
                continue                            
            dist = dist1 + dist2

            # if the distance between these the generated synset pair and some
            # synset pair in the list is less than any we've seen before, then
            # we should select whatever option we are currently on
            if dist < closestDist:
                closestDist = dist

                # WordNet will throw a value error for rare cases where
                # the name contains extra '.'s, e.g. Ph.D. so proctect
                # against this case
        except ValueError:
            continue
    return closestDist


SHORTEST_PATH_DISTANCE_CACHE = dict()
def getShortestPathDistance(synset1, synset2):
    if (synset1, synset2) not in SHORTEST_PATH_DISTANCE_CACHE:
        dist = synset1.shortest_path_distance(synset2)
        SHORTEST_PATH_DISTANCE_CACHE[(synset1, synset2)] = dist
        return dist

    return SHORTEST_PATH_DISTANCE_CACHE[(synset1, synset2)]

# For two words, c and d, generate all of their synsets and return a list of
# tuples that results from the pair-wise combination of each synset list
def generateSynsetPairs(c,d):
    cSyns = wn.synsets(c)
    dSyns = wn.synsets(d)
    pairs = []
    for e in cSyns:
        for f in dSyns:
            pairs.append((e,f))
    return pairs


def answerSATQuestion(SATQuestion, listOfListOfAnalogousPairs, 
                      firstInvIndex, firstSynsetInvIndex, 
                      secondInvIndex, secondSynsetInvIndex):

    examplePair = SATQuestion[0];
    print "Question %s:%s" % (examplePair)
    options = SATQuestion[1];
    for option in options:
        print "\t%s:%s" % (option)
    correctIndex = SATQuestion[2]

    listIndicesWithExampleAnalogy = set()

    # first see if we can find the example pair in any of the lists
    if examplePair[0] in firstInvIndex and examplePair[1] in secondInvIndex:
        listIndices = set(firstInvIndex[examplePair[0]])
        listIndices.intersection(secondInvIndex[examplePair[1]])
        for list in listIndices:
            listIndicesWithExampleAnalogy.add(list)

    # If we didn't find the example pair, try searching for it using hypernym
    if len(listIndicesWithExampleAnalogy) == 0 and CONSIDER_HYPERNYMS and False:
        print "didn't find the example pair in any list, so searching using closest path distance"
        
        closestDist = sys.maxint
        listIndex = None

        # generated all the possible synsets for the example pair
        expandedExamples = generateSynsetPairs(examplePair[0], examplePair[1])
        #print "consider %d synset options for example" % (len(expandedExamples))

        for a, b in expandedExamples:

            listIndicesWithBothExpandedSynsets = set()
            
            # find lists in which both occur
            if a.name in firstSynsetInvIndex and b.name in secondSynsetInvIndex:
                listIndicesWithBothExpandedSynsets = set(firstSynsetInvIndex[a.name])
                listIndicesWithBothExpandedSynsets.intersection(secondSynsetInvIndex[b.name])
                
            #print "for x-example %s:%s, saw %d lists that had it" % (a.name, b.name,
            #                                                         len(listIndicesWithBothExpandedSynsets))
        

            curIndex = 0
            for index in listIndicesWithBothExpandedSynsets:

                list, parents = listOfListOfAnalogousPairs[index]
                dist = findClosestDistInList(a, b, list)
                if dist < closestDist:
                    closestDist = dist
                    listIndex = curIndex
                    if curIndex % 100 == 0:
                        print "\t list %d" % (curIndex)
                curIndex += 1

        # add whatever list had the closest match to the list with the selected
        # value
        listIndicesWithExampleAnalogy.add(listIndex)

            
    # default value of -1 indicates that we were unable to find an option
    selectedOption = -1


    # if we found the example pair in at least one list, then try to choose an
    # answer from the list of options
    if len(listIndicesWithExampleAnalogy) > 0:

        # look at all the lists that contain the example pair and see if any of
        # those lists contain one of the options.  Select the option based on
        # the criteria defined in the function.
        selectedOption = selectOptionFromList(options, 
                                              listIndicesWithExampleAnalogy, 
                                              firstInvIndex,
                                              secondInvIndex,
                                              listOfListOfAnalogousPairs)        

    # if we didn't find the options in any of the lists, consider expanding
    # the options using their synonyms
    elif selectedOption == -1 and EXPAND_OPTION_PAIRS:
        print "Unable to find options in selected list(s), so expanding"
        expandedOptions, expandedToOriginal = expandList(options)
        expandedOptionSelection = selectOptionFromList(expandedOptions, 
                                                       listIndicesWithExampleAnalogy, 
                                                       firstInvIndex,
                                                       secondInvIndex,
                                                       listOfListOfAnalogousPairs)
        # remap whatever option we really chose to the original option from
        # which it came
        if expandedOptionSelection >= 0:
            selectedOption = expandedToOriginal[expandedOptions[ExpandedOptionSelection]]


            
        

    # If we didn't find the example pair, we can try expanding it by using
    # synonyms of the words.
    elif EXPAND_EXAMPLE_PAIR:
        print "Unable to find example pair, so expanding"
        
        expandedExample, unusedMap = expandList((examplePair))
        for a, b in expandedExample:
            if a in firstInvIndex and b in secondInvIndex:
                listIndices = set(firstInvIndex[a])
                listIndices.intersection(secondInvIndex[b])
                for list in listIndices:
                    listIndicesWithExampleAnalogy.add(list)
        
        # look at all the lists that contain the example pair and see if any of
        # those lists contain one of the options.  Select the option based on
        # the criteria defined in the function.
        selectedOption = selectOptionFromList(options, 
                                              listIndicesWithExampleAnalogy, 
                                              firstInvIndex,
                                              secondInvIndex,
                                              listOfListOfAnalogousPairs)     

    # if we didn't find the options in any of the lists, consider expanding
    # the options using their synonyms
    elif selectedOption == -1 and EXPAND_OPTION_PAIRS:
        print "Unable to find options in selected list(s), so expanding"
        expandedOptions, expandedToOriginal = expandList(options)
        expandedOptionSelection = selectOptionFromList(expandedOptions, 
                                                       listIndicesWithExampleAnalogy, 
                                                       firstInvIndex,
                                                       secondInvIndex,
                                                       listOfListOfAnalogousPairs)
        # remap whatever option we really chose to the original option from
        # which it came
        if expandedOptionSelection >= 0:
            selectedOption = expandedToOriginal[expandedOptions[ExpandedOptionSelection]]
            
        

    # Finally, determine if we made a selection and whether it was correct
    if selectedOption >= 0:
        print "selected option %s:%s" % (options[selectedOption])
        if selectedOption == correctIndex:
            print "CORRECT!"
            return CORRECT
        else:
            print "INCORRECT; should be %s:%s" % (options[correctIndex])
            return INCORRECT

    # If selection wasn't set we are unable to find any list that contains both
    # the example and option (possibly including expansions), so return that we
    # couldn't find answer.  This counts against our recall, but not precision.
    else:
        print "Unable to find an answer"
        return NOT_FOUND


def main(argv=None):
    if argv is None:
        argv = sys.argv
        
    if len(argv) != 3:
        usage();
        return None

    # parse and load all of the SAT questions into a list of tuples
    listOfQuestions = loadQuestions(argv[1])

    # unpickle the list of list of pairs of analogous words.   
    listOfListOfAnalogousPairs = loadListFromFile(argv[2])

    # build the inverse indicies from each word in a tuple to any list that
    # contains it
    firstInvIndex, firstSynsetInvIndex, secondInvIndex, secondSynsetInvIndex = buildInverseIndices(listOfListOfAnalogousPairs)

    # now try to answer each question, keeping track of how many we get correct
    correctAnswers = 0
    answeredQuestions = 0
    exampleNotFoundAnswers = 0
    for question in listOfQuestions:
        result = answerSATQuestion(question, listOfListOfAnalogousPairs, 
                                   firstInvIndex, firstSynsetInvIndex, 
                                   secondInvIndex, secondSynsetInvIndex)
        if result != NOT_FOUND:
            answeredQuestions += 1
        if result == CORRECT:
            correctAnswers += 1
        elif result == NOT_FOUND:
            exampleNotFoundAnswers += 1
        print "Answered %d/%d correct" % (correctAnswers, answeredQuestions)

    # print out the summary results
    print "Finally: answered %d/%d correct, skipped %d" % (correctAnswers, 
                                                           len(listOfQuestions) - exampleNotFoundAnswers,
                                                           exampleNotFoundAnswers)


        

def usage():
    print "usage: <SAT questions file> <analogous-pairs pickle>"
    return

if __name__ == "__main__":
    sys.exit(main())
