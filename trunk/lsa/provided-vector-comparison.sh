#!/bin/bash

dir=`pwd`

#for i in `echo "025 050 060 070 080 090 450 500"` 
#for i in `echo "005 010 015 020 025 050 075 085 100 125 150 175 200"`
#for i in `echo "300 225 250 275 325 350 375 400 425 450 475 500"`
for dir in `ls -d *-dims`
do
  echo "processing $dir dimensions"
  mkdir "$dir/provided-vector-comparison" "$dir/provided-vector-comparison-similar-list"
  for f in `cat ../provided-term-vectors/provided-vector-list.txt ` ; do cp $dir/terms/$f $dir/provided-vector-comparison ; done 
  java -server -cp .. TermComparator "$PWD/$dir/provided-vector-comparison" "$PWD/$dir/provided-vector-comparison-similar-list" &
done

