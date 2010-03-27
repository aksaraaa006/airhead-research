#!/bin/bash

CORPUS=/fomor/corpora/tasa/tasa-dependency-parsed.txt
outDir=/fomor/sspaces
INDEX=""
VALUES=""

function run() {
  indexPrefix=$outDir/dri-static
  if [[ "$VALUES" -eq "" ]]
  then
    INDEX="-S $indexPrefix"
    VALUES="yes"
  else
    INDEX="-L $indexPrefix"
    VALUES="yes"
  fi

  echo "Running DRI with "
  java -Xmx8g -server edu.ucla.sspace.mains.DependencyRandomIndexingMain \
       -d $CORPUS -l 10000 -s $1 $2 $INDEX \
       -v $outDir/dependency-random-indexing-$3.sspace 2> hermit.log 
}

#run 15 45
function grid_search() {
run 1 "$1" "small-window-$2"
run 5 "$1" "medium-window-$2"
run 10 "$1" "large-window-$2"
run 100 "$1" "max-window-$2"
}

RPERM="edu.ucla.sspace.dependency.RelationPermutationFunction"
RSPERM="edu.ucla.sspace.dependency.RelationSumPermutationFunction"

grid_search " " " "
#grid_search "-P" "length-perm"
grid_search "-P -p $RPERM"  "rel-perm"
grid_search "-P -p $RSPERM" "rel-sum-perm"
