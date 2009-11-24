#!/bin/bash

#CORPUS=/argos/corpora/Wacky/WaCKy-cleaned-one-line-per.txt
#CORPUS=/argos/corpora/Wacky/WaCKy-first-100k-lines.txt
#CORPNAME=wacky
#WORDS="-F /argos/corpora/Wacky/wacky-test-unigrams-bigrams.txt -C top-100-wacky-bigrams.txt"
#MAP=random-wacky-bigram.map
CORPUS=tasa-one-doc-per-line-cleaned.txt
CORPNAME=tasa
WORDS=""
MAP=replacement.map
SAVEDVECTORS=""

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
    -v $LOADSAVE $WORDS $SIZE $COUNT $THRESH $SECOND $EXTRA \
    -m $MAP /tmp 2> hermit.log > hermit.out

OUT_NAME=hermit_out/${CORPNAME}_$1_$2_$3_$4_$5
echo $OUT_NAME
sort hermit.out > $OUT_NAME.raw_results
./hermit_result.py hermit.out > $OUT_NAME.results
}

mkdir hermit_out

for i in $(seq 05 5 95)
do
  h_run 50000 10 $i 05 
done
