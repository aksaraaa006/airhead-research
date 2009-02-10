#!/bin/bash

dir=`pwd`

#for i in `echo "025 050 060 070 080 090 450 500"` 
#for i in `echo "005 010 015 020 025 050 075 085 100 125 150 175 200"`
for i in `echo "300 225 250 275 325 350 375 400 425 450 475 500"`
do
  echo "processing $i dimensions"
  mkdir "$i-dims"
  mkdir "$i-dims/terms" "$i-dims/similar-term-lists"
  java -server -cp .. RowExtractor "termVectors$i.dat"  tasa-.indexToTerm.dat "$i-dims/terms"
#  java -server -cp .. TermFileRenamer tasa-.indexToTerm.dat "$i-dims/vectors" "$i-dims/terms"
  java -server -cp .. TermComparator "$i-dims/terms" "$i-dims/similar-term-lists"
done