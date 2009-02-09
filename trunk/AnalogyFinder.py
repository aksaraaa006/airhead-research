#!/usr/bin/python

from nltk.corpus import wordnet as wn
import os.path
import sys
import getopt
import nltk    

def generateSynthesizedTriplets(tripletFile, outFile):
    infile = open(tripletFile,"r")
    outputWriter = open(outFile, "w");

    while infile:
        line = infile.readline()
        triplet = line.split("|")

        # extract the n-v-n from the file, the 4th column is the context in
        # which it occurs
        subject = triplet[0]
        verb = triplet[1]
        object = triplet[2]
        context = triplet[3]
        
        # perform two modifications:
        # 1 - check for compounds, and if they exist reduce to last word
        #     NOTE: this is a bad hack and needs to be removed
        # 2 - perform a morpholgical transformation on the word to remove any
        #     pluralization for nouns or conjugation for verbs
        subMorph = wn.morphy(subject.split(" ")[-1])
        # special case for "was", as morphy shortens this to 
        verbMorph = ("is" if (verb == "was") else wn.morphy(verb.split(" ")[-1]))
        objMorph = wn.morphy(object.split(" ")[-1])
        
        # before we replace the word, ensure that we didn't get a NoneType back
        # from morphy
        if subMorph is not None:
            #print "new subj %s -> %s" % (subject, subMorph)
            subject = subMorph
        if verbMorph is not None:
            #print "new verb %s -> %s" % (verb, verbMorph)
            verb = verbMorph            
        if objMorph is not None:
            #print "new obj  %s -> %s" % (object, objMorph)
            object = objMorph            

        # generate a list of possible sense for each word based on the context
        # in which it appears
        subjectSenses = disambiguateWordSense(subject, wn.NOUN, context)
        verbSenses = disambiguateWordSense(verb, wn.VERB, context)
        objectSenses = disambiguateWordSense(object, wn.NOUN, context)
        
        # lists of alternate words that could be used to replace the originals
        subjectAlternates = set()
        verbAlternates = set()
        objectAlternates = set()
        
        subjectAlternates.add(subject)
        verbAlternates.add(verb)
        objectAlternates.add(object)

        # For the subject, create a list based on the lemmas associated with the
        # each possible word sense
        for sense in subjectSenses:
            for lemma in sense.lemmas:
                subjectAlternates.add(toString(lemma))
            for seealso in sense.also_sees():
                subjectAlternates.add(toString(seealso));
         
        # For the verbs, we do more work to generate alternates based on:
        # 1 - lemmas (synonyms)
        # 2 - hypernyms
        # 3 - hyponyms
        # 4 - coordinate terms (sibling terms (hypernyms of the hyponym))
        for sense in verbSenses:
            for lemma in sense.lemmas:
                verbAlternates.add(toString(lemma))

            for hyper in sense.hypernyms():
                verbAlternates.add(toString(hyper))
                # find all the siblings
                for sibling in hyper.hyponyms():
                    # skip the original word sense
                    if (sibling != sense):
                        verbAlternates.add(toString(sibling))
                
            for hypo in sense.hyponyms():
                verbAlternates.add(toString(hypo))

        for sense in objectSenses:
            for lemma in sense.lemmas:
                objectAlternates.add(toString(lemma))
            for seealso in sense.also_sees():
                objectAlternates.add(seealso);        

        if (len(subjectAlternates) == 1 and
            len(verbAlternates) == 1 and
            len(objectAlternates) == 1):
            print "no alternates for \"%s\" \"%s\" \"%s\"" % (subject, verb, object)
            continue
                 
        if None:
            print "subject (%s): %s" % (subject, subjectAlternates)
            print "verb (%s): %s" % (verb, verbAlternates)
            print "object (%s): %s" % (object, objectAlternates)
            print "printing alterates...\n"
        
        for s in subjectAlternates:
            for v in verbAlternates:
                for o in objectAlternates:
                    outputWriter.write(s + "|" + v + "|" + o + "\n")
            outputWriter.write("\n")

    outputWriter.close()

# For now, return a singleton list of the first word synset according to the
# part of speech.  This function should really return a list of possible synsets
# depending on the context in which the word is used.
def disambiguateWordSense(word, pos, context):
    #print word + " " + pos
    try:
        if pos == wn.VERB:
            stemmed = wn.morphy(word)
            if stemmed is None:
                stemmed = word
            return [wn.synset(stemmed + "." + pos + ".01")]
        else:
            return [wn.synset(word + "." + pos + ".01")]
    except nltk.corpus.reader.wordnet.WordNetError:
        #print "no lemma for " + word
        return []
    except ValueError:
        return []

def toString(synset): 
    return synset.name.split(".")[0].replace("_"," ")

def main(argv=None):
    if argv is None:
        argv = sys.argv
        
    if len(argv) != 3:
        usage();
        return None

    tripletFile = argv[1]
    outputFile = argv[2]
    generateSynthesizedTriplets(tripletFile, outputFile)

def usage():
    print "usage: AnalogyFinder <triplet file> <output file>"
    return

if __name__ == "__main__":
    sys.exit(main())
