#!/usr/bin/python

import os.path
import sys

def printWords(SATfile):    
    infile = open(SATfile, "r")
    # print "loading %s" % (SATfile)
    words = set()

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

        # write the question out as a tuple of the source analogy, a list of
        # tuples of target analogies,and which index contains the correct option
        words.add(source[0])
        words.add(source[1])
        words.add(opt1[0])
        words.add(opt1[1])
        words.add(opt2[0])
        words.add(opt2[1])
        words.add(opt3[0])
        words.add(opt3[1])
        words.add(opt4[0])
        words.add(opt4[1])
        words.add(opt5[0])
        words.add(opt5[1])

    # print
    for word in words:
        print word

def main(argv=None):
    if argv is None:
        argv = sys.argv
        
    if len(argv) != 2:
        usage()
        return None

    printWords(argv[1])

def usage():
    print "usage: <SAT questions file>"
    return

if __name__ == "__main__":
    sys.exit(main())
