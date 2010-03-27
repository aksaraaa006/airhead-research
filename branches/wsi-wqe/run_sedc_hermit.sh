#!/bin/bash

corpusDir=/fomor/corpora/senseEval07
baseDir=/fomor/sedc07/$1
CORPUS=$corpusDir/dependency_senseEval.xml
gsKey=$corpusDir/key/keys/senseinduction_test.key
INDEX=""
VALUES=""

indexPrefix=$baseDir/senseEval-dependency-hermit
senseEvalUnSup=$corpusDir/key/scripts/unsup_eval.pl
semEvalUnSup="java -jar $corpusDir/unsupvmeasure.jar"

jserv="java -Xmx8g -server"

function run() {
  outDir=$baseDir/$1
  if [ ! -e $outDir ]
  then
    mkdir -p $outDir
  fi

  if [[ "$VALUES" -eq "" ]]
  then
    INDEX="-S $indexPrefix"
    VALUES="yes"
  else
    INDEX="-L $indexPrefix"
    VALUES="yes"
  fi

  systemKey=$outDir/senseval-dep-waiting.key
  testKey=$corpusDir/key/keys/senseinduction_test.key
  nounKey=$corpusDir/key/keys/senseinduction_test_nouns.key
  verbKey=$corpusDir/key/keys/senseinduction_test_verbs.key
  trainKey=$corpusDir/key/keys/senseinduction_train.key

  $jserv $5 edu.ucla.sspace.mains.DependencyWaitingSenseEvalHermitMain \
       -d $CORPUS -l 5000 -c 15 -G $2 -s $3 $4 $INDEX \
       -v $outDir/hermit.sspace -t 4 2> hermit.log 

  echo "Running the senseval tester"
  java -Xmx8g -server edu.ucla.sspace.evaluation.SenseEvalDependencyTester \
       -m $indexPrefix.index -p $indexPrefix.permutation \
       -s $outDir/hermit.sspace -S $CORPUS $systemKey -w $3

  echo "Evaluating FlyingHermit with SenseEval07"
  senseEvalName=$outDir/senseval-dep-waiting
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

clustering="edu.ucla.sspace.clustering"
gap="$clustering.GapStatistic"
auto="$clustering.AutomaticStopClustering"
spectral="$clustering.SpectralClustering"

RPERM="-p edu.ucla.sspace.dependency.RelationPermutationFunction"
RSPERM="-p edu.ucla.sspace.dependency.RelationSumPermutationFunction"

run small_window/relPerm/gap "$gap" 1 "-P $RPERM"
run medium_window/relPerm/gap "$gap" 3 "-P $RPERM"
run large_window/relPerm/gap "$gap" 5 "-P $RPERM"
run max_window/relPerm/gap "$gap" 100 "-P $RPERM"

run small_window/relSumPerm/gap "$gap" 1 "-P $RSPERM"
run medium_window/relSumPerm/gap "$gap" 3 "-P $RSPERM"
run large_window/relSumPerm/gap "$gap" 5 "-P $RSPERM"
run max_window/relSumPerm/gap "$gap" 100 "-P $RSPERM"

run small_window/perm/gap "$gap" 1 "-P"
run medium_window/perm/gap "$gap" 3 "-P"
run large_window/perm/gap "$gap" 5 "-P"
run max_window/perm/gap "$gap" 100 "-P"

run small_window/nop/gap "$gap" 1 ""
run medium_window/nop/gap "$gap" 3 ""
run large_window/nop/gap "$gap" 5 ""
run max_window/nop/gap "$gap" 100 ""
