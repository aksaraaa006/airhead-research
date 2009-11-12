#!/bin/bash

CORPUS=/argos/corpora/Wacky/WaCKy-cleaned-one-line-per.txt
CORPNAME=wacky
WORDS=/argos/corpora/Wacky/wacky-unigrams-over-10-occurrances.txt
#CORPUS=/argos/corpora/tasa/tasa-one-doc-per-line-cleaned.txt
#CORPNAME=tasa
#TOKENS=

function h_run() {
SIZE=$1
COUNT=$2
THRESH=$3
if [ $# -eq 4 ]
then
  NAME=second
  SECOND=$4
else
  NAME=first
  SECOND=
fi

java -server -Xmx8g edu.ucla.sspace.mains.HermitMain -d $CORPUS \
    -o sparse_binary -v \
    -l $SIZE -p -m shutze_replacement.map -c $COUNT -h $THRESH $SECOND \
    -F $WORDS -C shutze_compounds /argos/sspaces 2> hermit.log > hermit.out

sort hermit.out > \
    hermit_out/${CORPNAME}${NAME}_order_${SIZE}_${COUNT}_${THRESH}.raw_results
./hermit_result.py hermit.out > \
    hermit_out/${CORPNAME}${NAME}_order_${SIZE}_${COUNT}_${THRESH}.results
}

#h_run 5000 2 .75 -O
#h_run 5000 2 .85 -O
#h_run 5000 2 .95 -O

#h_run 8000 2 .75 -O
#h_run 8000 2 .85 -O
#h_run 8000 2 .95 -O

h_run 5000 2 .75
h_run 5000 2 .85
h_run 5000 2 .95

h_run 8000 2 .75
h_run 8000 2 .85
h_run 8000 2 .95
