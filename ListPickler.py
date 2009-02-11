import os.path
import sys
import getopt
import pickle

def loadListOfLists(listFileName):
    infile = open(listFileName)

    curList = []
    listOfLists = []
    while infile:
        line = infile.readline();
        if len(line) == 0:
            break
        # blank line indicates start of new list
        elif line.isspace():
            listOfLists.append(curList)
            curList = []
            continue
        elif line[0] == "#":
            continue

        pair = line.strip().split("\t")
        print pair
        curList.append(pair)
    return listOfLists

def main(argv=None):
    if argv is None:
        argv = sys.argv
        
    if len(argv) != 3:
        usage();
        return None

    print "loading lists"
    listOfListsOfTuples = loadListOfLists(argv[1])
    print "writing tuples"
    outfile = open(argv[2], "w")
    pickle.dump(listOfListsOfTuples, outfile)

def usage():
    print "usage: <list file to pickle> <output file>"
    return

if __name__ == "__main__":
    sys.exit(main())
