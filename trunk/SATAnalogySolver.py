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
    return questions

def loadListFromFile(listFile):
    if True:
        handle = open(listFile)
        return pickle.load(handle)
    else:
        # TODO: load from text file
        return []

def buildInverseIndices(listOfListOfAnalogousPairs):

    # these will map words to the lists that contain them.  We use two lists
    # here to speed up indexing based on either word in the related pair
    firstInvIndex = dict()
    secondInvIndex = dict()
    
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
    for listOfPairs in listOfListOfAnalgousPairs:
        for (a, aSyn), (b, bSyn) in listOfPairs:
            # update the set of list indicies for the first element
            if firstInvIndex.has_key(a):
                lists = firstInvIndex[a]
                lists.add(listIndex);
            else:
                lists = set(listIndex);
                firstInvIndex[a] = lists
                
            # then for the second element
            if secondInvIndex.has_key(b):
                lists = secondInvIndex[b]
                lists.add(listIndex);
            else:
                lists = set(listIndex);
                secondInvIndex[b] = lists
                
        listIndex += 1
                
    return (firstInvIndex, secondInvIndex)

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
                         listOfListOfAnalogousPairs):

    optionIndexToList = dict()

    selectedOption = -1
    
    for index in listIndicesWithExampleAnalogy:
        listWithAnalogy = listOfListOfAnalogousPairs[index]
        
        optionIndex = 0
        for c, d in options:
            if c in firstInvIndex and d in secondInvIndex:
                listIndices = set()
                listIndices.copy(firstInvIndex[c])
                listIndices.intersection(secondInvIndex[d])
                
                # add whatever list indices were found to the mapping from
                # option to list that supports the option as being correct
                if optionIndex in optionIndexToList:
                    optionIndexToList[optionIndex].add(listIndices)
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
        for option, list in optionIndexToList:
            if len(list) > largestListSize:
                largestListSize = len(list)
                opt = option
        selectedOption = opt
        
    # if we didn't find any options, then consider looking at the hypernyms of
    # the synsets in the list.
    elif CONSIDER_HYPERNYMS:

        curDepth = 0
        for index in listIndicesWithExampleAnalogy:
            listWithAnalogy = listOfListOfAnalogousPairs[index]
            
            optionIndex = 0
            closestMatch = sys.maxint # really large value

            for c, d in options:

                # generate a list of all the synset combinations for c and d
                pairs = generateSynsetPairs(c,d)
                for e, f in pairs:
                    # compute the distance between each pair and all the options
                    # in the liste
                    for a, aSyn, b, bSyn in listWithAnalogy:
                        # check that we actually have a synset listed for the
                        # word
                        if aSyn == "None" or bSyn == "None":
                            continue
                        try:
                            dist1 = e.shortestPathDist(wn.synset(aSyn))
                            dist2 = f.shortestPathDist(wn.synset(bSyn))
                            # check that there was a path for both synsets
                            if dist1 == -1 and dist2 == -1:
                                continue                            
                            dist = dist1 + dist2

                            # if the distance between these the generated synset
                            # pair and some synset pair in the list is less than
                            # any we've seen before, then we should select
                            # whatever option we are currently on
                            if dist < closestMatch:
                                selectedOption = optionIndex
                                closestMatch = dist

                        # WordNet will throw a value error for rare cases where
                        # the name contains extra '.'s, e.g. Ph.D. so proctect
                        # against this case
                        except ValueError:
                            continue
                        
                


    return selectedOption



# For two words, c and d, generate all of their synsets and return a list of
# tuples that results from the pair-wise combination of each synset list
def generateSynsetPairs(c,d):
    cSyns = wn.synsets(c)
    dSyns = wn.sysnets(d)
    pairs = []
    for e in cSyns:
        for f in dSyns:
            pairs.append((e,f))
    return pairs


def answerSATQuestion(SATQuestion, listOfListOfAnalogousPairs, 
                      firstInvIndex, secondInvIndex):

    examplePair = SATQuestion[0];
    print "Question %s:%s" % (source)
    options = SATQuestion[1];
    for option in options:
        print "\t%s:%s" % (option)
    correctIndex = SATQuestion[2]

    listIndicesWithExampleAnalogy = set()

    # first see if we can find the example pair in any of the lists
    if examplePair[0] in firstInvIndex and examplePair[1] in secondInvIndex:
        listIndices = set()
        listIndices.copy(fistInvIndex[examplePair[0]])
        listIndices.intersection(secondInvIndex[examplePair[1]])
        listIndicesWithExampleAnalogy.add(listIndices)
            
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
                                              listOfListOfAnalogousPairs):        

        # if we didn't find the options in any of the lists, consider expanding
        # the options using their synonyms
        elif selectedOption == -1 and EXPAND_OPTION_PAIRS:
            print "Unable to find options in selected list(s), so expanding"
            expandedOptions, expandedToOriginal = expandList(options)
            expandedOptionSelection = selectOptionFromList(expandedOptions, 
                                                           listIndicesWithExampleAnalogy, 
                                                           listOfListOfAnalogousPairs):   
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
                listIndices = set()
                listIndices.copy(fistInvIndex[a])
                listIndices.intersection(secondInvIndex[b])
                listIndicesWithExampleAnalogy.add(listIndices)
        
        # look at all the lists that contain the example pair and see if any of
        # those lists contain one of the options.  Select the option based on
        # the criteria defined in the function.
        selectedOption = selectOptionFromList(options, 
                                              listIndicesWithExampleAnalogy, 
                                              listOfListOfAnalogousPairs):        

        # if we didn't find the options in any of the lists, consider expanding
        # the options using their synonyms
        elif selectedOption == -1 and EXPAND_OPTION_PAIRS:
            print "Unable to find options in selected list(s), so expanding"
            expandedOptions, expandedToOriginal = expandList(options)
            expandedOptionSelection = selectOptionFromList(expandedOptions, 
                                                           listIndicesWithExampleAnalogy, 
                                                           listOfListOfAnalogousPairs):   
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
    listOfListOfAnalogousPairs = loadListFromFile(listFile)

    # build the inverse indicies from each word in a tuple to any list that
    # contains it
    firstInvIndex, secondInvIndex = buildInverseIndices(listOfListOfAnalogousPairs)

    # now try to answer each question, keeping track of how many we get correct
    correctAnswers = 0
    exampleNotFoundAnswers = 0
    for question in listOfQuestions:
        result = answerQuestion(question, listOfListOfAnalogousPairs, 
                                firstInvIndex, secondInvIndex):
        if result == CORRECT:
            correctAnswers += 1
        elif result == NOT_FOUND:
            exampleNotFoundAnswers += 1
        print "Answered %d/%d correct" % (len(correct), len(listOfQuestions))

    # print out the summary results
    print "Finally: answered %d/%d correct, skipped %d" % (len(correct), 
                                                           len(listOfQuestions) - exampleNotFoundAnswers,
                                                           exampleNotFoundAnswers)


        

def usage():
    print "usage: <SAT questions file> <analogous-pairs pickle>"
    return

if __name__ == "__main__":
    sys.exit(main())
