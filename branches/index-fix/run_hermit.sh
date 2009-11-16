#!/bin/bash

#CORPUS=/argos/corpora/Wacky/WaCKy-cleaned-one-line-per.txt
#CORPNAME=wacky
#WORDS=/argos/corpora/Wacky/wacky-top-60k-unigrams.txt
CORPUS=/argos/corpora/tasa/tasa-one-doc-per-line-cleaned.txt
CORPNAME=tasa

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
    -l $SIZE -p -m replacement.map -c $COUNT -h $THRESH $SECOND \
    -C shutze_compounds /argos/sspaces 2> hermit.log > hermit.out

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

#h_run 5000 2 .75
#h_run 5000 2 .85
#h_run 5000 2 .95

#h_run 8000 20 .75 
#h_run 8000 20 .85
#h_run 8000 20 .95 

h_run 8000 20 .01
h_run 8000 20 .03
h_run 8000 20 .05
h_run 8000 20 .07
h_run 8000 20 .09
h_run 8000 20 .11
h_run 8000 20 .13
h_run 8000 20 .15 
