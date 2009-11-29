#!/bin/bash

CORPUS=WaCKy-cleaned-one-line-per.txt
#CORPUS=gos/corpora/Wacky/WaCKy-first-100k-lines.txt
CORPNAME=wacky
TSIZE=2000000
WORDS="-F wacky-test-unigrams-bigrams.txt -C top-150-wacky-bigrams.txt"
MAP=random-wacky-bigram.map
#CORPUS=tasa-one-doc-per-line-cleaned.txt
#CORPNAME=tasa
#TSIZE=39600
#WORDS=""
#MAP=replacement.map
SAVEDVECTORS=""
DPERM="-p edu.ucla.sspace.index.DefaultPermutationFunction"
WPERM="-p edu.ucla.sspace.index.WindowedPermutationFunction"

function h_run() {
SIZE="-l $1"
COUNT="-c $2"
THRESH="-h .$3"
WINDOW="-s $4,$4"
EXTRA=$5 
if [ "$SAVEDVECTORS" == "" ]
then
  LOADSAVE="-S /tmp/first-order-hermit.vectors"
  SAVEDVECTORS="yes"
else
  LOADSAVE="-L /tmp/first-order-hermit.vectors"
fi

java -server -Xmx8g edu.ucla.sspace.mains.HermitMain -d $CORPUS \
    -D $TSIZE -v $LOADSAVE $WORDS $SIZE $COUNT $THRESH $SECOND $EXTRA \
    -m $MAP /tmp 2> hermit.log > hermit.out

OUT_NAME=hermit_out/${CORPNAME}_$1_$2_$3_$4_$6
echo $OUT_NAME
sort hermit.out > $OUT_NAME.raw_results
./hermit_result.py hermit.out > $OUT_NAME.results
}

rm -rf hermit_out
mkdir hermit_out

h_run 50000 10 35 30 "$DPERM" "_p"
h_run 50000 10 35 30

#for i in $(seq 15 1 30)
#do
#  h_run 50000 10 85 30 "$WPERM -X $i" "_wp_${i}_"
#done
