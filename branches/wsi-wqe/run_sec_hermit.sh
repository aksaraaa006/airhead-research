#!/bin/bash

baseDir=/fomor/sec07/large_window/
corpusDir=/fomor/corpora/senseEval07
CORPUS=$corpusDir/cleaned_senseEval.txt
xmlCorpus=$corpusDir/test/English_sense_induction.xml
gsKey=$corpusDir/key/keys/senseinduction_test.key
INDEX=""
VALUES=""

jserv="java -Xmx8g -server"

senseEvalUnSup=$corpusDir/key/scripts/unsup_eval.pl
semEvalUnSup="java -jar $corpusDir/unsupvmeasure.jar"

indexPrefix=$baseDir/senseEval-waiting

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

  systemKey=$outDir/senseval-waiting.key
  testKey=$corpusDir/key/keys/senseinduction_test.key
  nounKey=$corpusDir/key/keys/senseinduction_test_nouns.key
  verbKey=$corpusDir/key/keys/senseinduction_test_verbs.key
  trainKey=$corpusDir/key/keys/senseinduction_train.key

  echo "Running FlyingHermit with $2 clusters and $1 clustering."
  $jserv $5 edu.ucla.sspace.mains.WaitingSenseEvalHermitMain \
       -d $CORPUS -l 5000 -c $2 -G $3 $4 $INDEX -w 10,10 \
       -v $outDir/hermit.sspace 2> hermit.log 

  echo "Running the senseval tester"
  $jserv edu.ucla.sspace.evaluation.SenseEvalTester \
       -m $indexPrefix.index -p $indexPrefix.permutation \
       -s $outDir/hermit.sspace -S $xmlCorpus -w 10 $systemKey

  echo "Evaluating FlyingHermit with SenseEval07"
  senseEvalName=$outDir/senseval-waiting
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

echo "Running without permutations"
run nop/gap 15 "$gap" ""

for alpha in $(seq 5 5 95)
do
  run nop/auto-pk1-$alpha 15 "$auto" "" \
     "-D$auto.clusteringMethod=PK1 -D$auto.pk1Threshold=$alpha"
done

run nop/auto-pk2 15 "$auto" "" "-D$auto.clusteringMethod=PK2"
run nop/auto-pk3 15 "$auto" "" "-D$auto.clusteringMethod=PK3"

for alpha in $(seq 5 5 95)
do
  run nop/spectral-$alpha 15 "$spectral" "" "-D$spectral.alpha=$alpha"
done

echo "Running with permutations"
run perm/gap 15 "$gap" "-P"

for alpha in $(seq 5 5 95)
do
  run perm/auto-pk1-$alpha 15 "$auto" "-P" \
     "-D$auto.clusteringMethod=PK1 -D$auto.pk1Threshold=$alpha"
done

run perm/auto-pk2 15 "$auto" "-P" "-D$auto.clusteringMethod=PK2"
run perm/auto-pk3 15 "$auto" "-P" "-D$auto.clusteringMethod=PK3"

for alpha in $(seq 5 5 95)
do
  run perm/spectral-$alpha 15 "$spectral" "-P" "-D$spectral.alpha=$alpha"
done
