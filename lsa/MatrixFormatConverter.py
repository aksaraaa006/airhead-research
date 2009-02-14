#!/usr/bin/python

import os.path
import sys
import getopt

def main(argv=None):
    if argv is None:
        argv = sys.argv
        
    if len(argv) != 3:
        usage();
        return None

    output = open(argv[2], "w")
    
    # determine the dimensions and number of non-zero elements to the matrix
    nonZeroVals = 0
    cols = 0
    rows = 0
    input = open(argv[1])

    curCol = 0
    rowsOfCurCol = []

    # lines are formated "col row val"
    for line in input:
        triple = line.split("\t")
        nonZeroVals += 1
        # subtract 1 from row and column when moving from matlab 1-indexed
        # matrices to SVDLIBC index 0-indexed matrices.
        row = int(triple[1].strip()) - 1
        col = int(triple[0].strip()) - 1
        val = float(triple[2].strip())
        
        # print "col %d, row %d, val %d" % (col, row, val)

        if row > rows:
            rows = row
        if col > cols:
            cols = col

        if col != curCol:     
            
            # output all the data for the current column
            output.write("%d\n" % (len(rowsOfCurCol)))

            for row, val in rowsOfCurCol:
                output.write("%s %s\n" % (row, val))
            
            # ensure that we've covered all the row in between the now current
            # row and the last one we saw in case we skippsed some rows
            for skipped in range(curCol + 1, col):
                output.write("0\n")

            # last reset the current column and empty the list
            curCol = col
            rowsOfCurCol = []

        rowsOfCurCol.append((row, val))
            

    output.close()
    print "ADD THIS TO FILE AS HEADER:"
    print "%s %s %s" % (rows + 1, cols + 1, nonZeroVals)    

def usage():
    print "usage: <input term-doc-matrix> <SVDLIBC-formatted output file>"
    return

if __name__ == "__main__":
    sys.exit(main())
