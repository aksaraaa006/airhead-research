#!/usr/bin/python

import os.path
import sys
import getopt

def main(argv=None):
    if argv is None:
        argv = sys.argv
        
    if len(argv) != 2:
        usage();
        return None
    
    # determine the dimensions and number of non-zero elements to the matrix
    nonZeroVals = 0
    cols = 0
    rows = 0
    input = open(argv[1])
    colToRowVal = dict()

    # lines are formated "col row val"
    for line in input:
        triple = line.split("\t")
        nonZeroVals += 1
        row = int(triple[1].strip())
        col = int(triple[0].strip())
        val = float(triple[2].strip())
        if col not in colToRowVal:
            colToRowVal[col] = []
        rowAndVals = colToRowVal[col]
        rowAndVals.append((row, val))
        if row > rows:
            rows = row
        if col > cols:
            cols = col
    input.close()

    print "%s %s %s" % (rows, cols, nonZeroVals)
    
    # print out the column indicies in order
    for col in range(cols):
        if col in colToRowVal:
            rowAndVals = colToRowVal[col]
            print "%d" % (len(rowAndVals))
            for row, val in rowAndVals:
                print "%s %s" % (row, val)
        else:
            print "0"


def usage():
    print "usage: <input term-doc-matrix> <SVDLIBC-formatted output file>"
    return

if __name__ == "__main__":
    sys.exit(main())
