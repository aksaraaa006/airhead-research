#!/bin/bash

corpusDir=/fomor/corpora/senseEval07
outDir=$1
CORPUS=$corpusDir/dependency_senseEval.xml
gsKey=$corpusDir/key/keys/senseinduction_test.key
INDEX=""
VALUES=""

senseEvalUnSup=$corpusDir/key/scripts/unsup_eval.pl
semEvalUnSup="java -jar $corpusDir/unsupvmeasure.jar"

if [ ! -e $outDir ]
then
  mkdir -p $outDir
fi

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
  java -Xmx8g -server edu.ucla.sspace.mains.DependencySecondFlyingHermitMain \
       -d $CORPUS -l 5000 -c $1 -h .$2 -P $3 $INDEX \
       -v $outDir/hermit.sspace 2> hermit.log 

  echo "Running the senseval tester"
  java -Xmx8g -server edu.ucla.sspace.evaluation.SenseEvalDependencyTester \
       -m $indexPrefix.index -p $indexPrefix.permutation
       -s $outDir/hermit.sspace -S $CORPUS $systemKey

  echo "Evaluating FlyingHermit with SenseEval07"
  senseEvalName=$outDir/senseval-$1p-$2h
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

run 15 30
