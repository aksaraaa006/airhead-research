#!/bin/bash

corpusDir=/fomor/corpora/senseEval07
baseDir=$1
CORPUS=$corpusDir/dependency_senseEval.xml
gsKey=$corpusDir/key/keys/senseinduction_test.key
INDEX=""
VALUES=""

senseEvalUnSup=$corpusDir/key/scripts/unsup_eval.pl
semEvalUnSup="java -jar $corpusDir/unsupvmeasure.jar"


function run() {
  indexPrefix=$outDir/senseEval-dependency-hermit
  if [[ "$VALUES" -eq "" ]]
  then
    INDEX="-S $indexPrefix"
    VALUES="yes"
  else
    INDEX="-L $indexPrefix"
    VALUES="yes"
  fi

  systemKey=$outDir/senseval-$1p-$2h.key
  testKey=$corpusDir/key/keys/senseinduction_test.key
  nounKey=$corpusDir/key/keys/senseinduction_test_nouns.key
  verbKey=$corpusDir/key/keys/senseinduction_test_verbs.key
  trainKey=$corpusDir/key/keys/senseinduction_train.key

  echo "Running FlyingHermit with $1 clusters and .$2 threshold."
  java -Xmx8g -server edu.ucla.sspace.mains.DependencySenseEvalFlyingHermitMain \
       -d $CORPUS -l 10000 -c $1 -h .$2 -s $3 $4 $INDEX \
       -v $outDir/hermit.sspace 2> hermit.log 

  echo "Running the senseval tester"
  java -Xmx8g -server edu.ucla.sspace.evaluation.SenseEvalDependencyTester \
       -m $indexPrefix.index -p $indexPrefix.permutation \
       -s $outDir/hermit.sspace -S $CORPUS $systemKey -w $3

  echo "Evaluating FlyingHermit with SenseEval07"
  senseEvalName=$outDir/senseval-$1p-$2h
  evaluate 2> /dev/null
}

function evaluate() {
  $senseEvalUnSup -m fscore $systemKey $testKey > $senseEvalName.fscore
  $senseEvalUnSup -m fscore -p n $systemKey $testKey > $senseEvalName.noun_fscore
  $senseEvalUnSup -m fscore -p v $systemKey $testKey > $senseEvalName.verb_fscore
  $senseEvalUnSup -m purity $systemKey $testKey > $senseEvalName.purity
  $senseEvalUnSup -m entropy $systemKey $testKey > $senseEvalName.entropy

  pushd $corpusDir/key/scripts
  ./sup_eval.sh $systemKey /tmp $trainKey $testKey > $senseEvalName.sup_recall
  popd

  echo "Evaluating FlyingHermit with SemEval10"
  $semEvalUnSup $systemKey $testKey | tail -n 1 > $senseEvalName.vmeasure
}

#run 15 45
function grid_search() {

outDir=$baseDir/$1
if [ ! -e $outDir ]
then
  mkdir -p $outDir
fi
windowSize=$2
#threshold=30
for threshold in $(seq 05 5 95)
do
  run 15 $threshold $windowSize $3
done
}

RPERM="edu.ucla.sspace.dependency.RelationPermutationFunction"
RSPERM="edu.ucla.sspace.dependency.RelationSumPermutationFunction"

# No permutation runs
grid_search nop/max_window 100
grid_search nop/small_window 1
grid_search nop/medium_window 3
grid_search nop/large_window 5

# Relation based permutation runs
grid_search relPerm/max_window 100 "-P -p $RPERM"
grid_search relPerm/small_window 1 "-P -p $RPERM"
grid_search relPerm/medium_window 3 "-P -p $RPERM"
grid_search relPerm/large_window 5 "-P -p $RPERM"

# Relation sum based permutation runs
grid_search relSumPerm/max_window 100 "-P -p $RSPERM"
grid_search relSumPerm/small_window 1 "-P -p $RSPERM"
grid_search relSumPerm/medium_window 3 "-P -p $RSPERM"
grid_search relSumPerm/large_window 5 "-P -p $RSPERM"

# Length based Permutation runs
grid_search lengthPerm/max_window 100 -P
grid_search lengthPerm/small_window 1 -P
grid_search lengthPerm/medium_window 3 -P
grid_search lengthPerm/large_window 5 -P
