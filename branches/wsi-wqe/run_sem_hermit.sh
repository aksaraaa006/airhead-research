#!/bin/bash

corpusDir=/fomor/corpora/SemEval2010
outDir=$1
CORPUS=$corpusDir/semEvalTraining.txt,$corpusDir/noun_training_file.txt,$corpusDir/verb_training_file.txt,
INDEX=""
VALUES="yes"
baseDir=/fomor/semEvalTraining/$1

senseEvalUnSup=$corpusDir/trial-data/evaluation/unsupervised_evaluation/scripts/unsup_eval.pl
semEvalUnSup="java -jar $corpusDir/unsupvmeasure.jar"

function run() {
  indexPrefix=$baseDir/senseEval-hermit
  if [[ "$VALUES" -eq "" ]]
  then
    INDEX="-S $indexPrefix"
    VALUES="yes"
  else
    INDEX="-L $indexPrefix"
    VALUES="yes"
  fi

  systemKey=$outDir/senseval-gap.key
  testKey=$corpusDir/trial-data/evaluation/keys/allwords.key
  #nounKey=$corpusDir/key/keys/senseinduction_test_nouns.key
  #verbKey=$corpusDir/key/keys/senseinduction_test_verbs.key
  #trainKey=$corpusDir/key/keys/senseinduction_train.key

  echo "Running FlyingHermit with $1 clusters and .$2 threshold."
  java -Xmx8g -server edu.ucla.sspace.mains.SenseEvalFlyingHermitMain \
       -d $CORPUS -l 5000 -c $1 -h .$2 \
       $4 $INDEX -s $3 \
       -v $outDir/hermit-$1-$2${4}.sspace 2> hermit.log 

  echo "Running the semEval tester"
  java -Xmx8g -server edu.ucla.sspace.evaluation.SemEvalTester \
       -m $indexPrefix.index -p $indexPrefix.permutation \
       -s $outDir/hermit-$1-$2.sspace -w $3 \
       $systemKey $corpusDir/test_data/nouns/*.xml \
       $corpusDir/test_data/verbs/*.xml

  if [[ "$4" -eq "" ]]
  then
    testPerm=""
  else
    testPerm="-p $indexPrefix.permutation"
  fi

  java -Xmx8g -server edu.ucla.sspace.evaluation.SemEvalTester \
       -m $indexPrefix.index $testPerm \
       -s $outDir/hermit-$1-$2${4}.sspace -w $3 \
       $systemKey-$1-$2${4}.trial $corpusDir/trial-data/testing\ data/*.xml

  #echo "Evaluating FlyingHermit with SenseEval07"
  senseEvalName=$outDir/trial-senseval-$1-$2${4}
  #$semEvalUnSup $systemKey.trial $testKey | tail -n 1 > $senseEvalName.vmeasure
  evaluate $systemKey-$1-$2${4}.trial
}

function evaluate() {
  $senseEvalUnSup -m fscore $1 $testKey > $senseEvalName.fscore
  $senseEvalUnSup -m fscore -p n $1 $testKey > $senseEvalName.noun_fscore
  $senseEvalUnSup -m fscore -p v $1 $testKey > $senseEvalName.verb_fscore
  $senseEvalUnSup -m purity $1 $testKey > $senseEvalName.purity
  $senseEvalUnSup -m entropy $1 $testKey > $senseEvalName.entropy

  pushd $corpusDir/trial-data/evaluation/supervised_evaluation/scripts
  ./sup_eval.sh $1 /tmp $trainKey > $senseEvalName.sup_recall
  popd

  echo "Evaluating FlyingHermit with SemEval10"
  $semEvalUnSup $1 $testKey | tail -n 1 > $senseEvalName.vmeasure
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
for threshold in $(seq 05 10 95)
do
  run 15 $threshold $windowSize "$3"
done
}

#outDir=$baseDir
#if [ ! -e $outDir ]
#then
#  mkdir -p $outDir
#fi
#run 15 15 1 ""
# No permutation runs
#grid_search perm/small_window 1 -P
#grid_search perm/large_window 10 -P
#grid_search perm/max_window 100 -P
#grid_search nop/small_window 1
#grid_search nop/large_window 10 
#grid_search nop/max_window 100 

outDir=$baseDir
run 15 15 15 ""
run 15 75 1 ""

# Permutation runs
#grid_search nop/max_window 100
#grid_search nop/medium_window 5
#grid_search perm/max_window 100 -P
#grid_search perm/medium_window 5 -P
