#!/bin/bash

KEY=all-english/EnglishLS.test.key
MAP=all-english/EnglishLS.sensemap
for i in `ls answers`
do
  echo $i
  ./scorer2 answers/${i} $KEY $MAP -g coarse
done
