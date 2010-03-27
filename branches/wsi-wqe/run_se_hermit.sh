#!/bin/bash

corpusDir=/fomor/corpora/senseEval07
outDir=$1
CORPUS=$corpusDir/cleaned_senseEval.txt
gsKey=$corpusDir/key/keys/senseinduction_test.key
INDEX=""
VALUES=""
baseDir=$1

senseEvalUnSup=$corpusDir/key/scripts/unsup_eval.pl
semEvalUnSup="java -jar $corpusDir/unsupvmeasure.jar"


function run() {
  indexPrefix=$outDir/senseEval-hermit
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
  java -Xmx8g -server edu.ucla.sspace.mains.SenseEvalFlyingHermitMain \
       -d $CORPUS -l 5000 -c $1 -h .$2 -s $3 $4 $INDEX \
       -v $outDir/hermit.sspace 2> hermit.log 

  echo "Running the senseval tester"
  java -Xmx8g -server edu.ucla.sspace.evaluation.SenseEvalTester \
       -m $indexPrefix.index -p $indexPrefix.permutation \
       -s $outDir/hermit.sspace -S $corpusDir/test/English_sense_induction.xml \
       $systemKey -w $3

  echo "Evaluating FlyingHermit with SenseEval07"
  senseEvalName=$outDir/senseval-$1p-$2h
  evaluate
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
  run 15 $threshold $windowSize "$3"
done
}

# No permutation runs
grid_search nop/max_window 100
grid_search nop/small_window 1
grid_search nop/medium_window 5
grid_search nop/large_window 10 

# Permutation runs
grid_search perm/max_window 100 -P
grid_search perm/small_window 1 -P
grid_search perm/medium_window 5 -P
grid_search perm/large_window 10 -P
