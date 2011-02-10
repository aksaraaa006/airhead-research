#!/bin/bash
# To RUN:
# 1) Change CORP_PATH to correct location of SemEval2010
# 2) run with ./run_psd.sh /path/to/SemEvalResults/psd_results n
#    where n is the segment number to use as the test set.

bdir=$1
mkdir -p $bdir/res

CORP_PATH=/argos/corpora/WaCkypedia/word-contexts
CLUST=edu.ucla.sspace.clustering
MAIN=edu.ucla.sspace.mains

LIMIT=4

j="java -Xmx8g -server "

run() {
  corp=""
  # Tac on the extra corpora segments, if specified.
  for i in $(seq 0 $LIMIT)
  do
    [ $i -eq $1 ] && continue
    corp=$CORP_PATH/$keyWord.$pos.$i,$CORP_PATH/$confounder.$pos.$i,$corp
  done

  clustAlg=$2
  clustName=$3
  extraArgs=$4

  outputName=semeval-2010-$algName-$clustName-$keyWord-$confounder-$pos-$1
  echo $bdir/$outputName-train.log
  echo "Running $algName-$clustName-$1"
  $j $extArgs $MAIN.$alg -d $corp -P $keyMap -W $window -c $numClust -v \
     $clustAlg -S $bdir/$outputName.basis $extraArgs \
     $bdir/$outputName-train.sspace \
     2> $bdir/$outputName-train.log \
     > $bdir/res/$outputName-train.counts

  corp=$CORP_PATH/$keyWord.noun.$testSeg,$CORP_PATH/$confounder.noun.$testSeg
  $j $extArgs $MAIN.$alg -d $corp -P $keyMap -W $window -c $numClust -v \
     -L $bdir/$outputName.basis $extraArgs -e $bdir/$outputName-train.sspace \
     $bdir/$outputName-test.sspace \
     2> $bdir/$outputName-test.log \
     > $bdir/res/$outputName-test.counts
}

run_all() {
  alg=DVWCWordsiMain
  algOpts=""
  window=25
  numClust=15

  algName="dv-wc-wordsi-25"
  run $3 "$CTYPE $CLUST.$1" $2 ""
}

processWordList() {
pos=$2
keyMap=pseudoword-test-key-map.txt
for confounderPair in `cat $1`
do
  keyWord=`echo $confounderPair | cut -d "-" -f 1`
  confounder=`echo $confounderPair | cut -d "-" -f 2`

  echo "$keyWord $confounder"
  echo "$keyWord $keyWord$confounder" > $keyMap
  echo "$confounder $keyWord$confounder" >> $keyMap

  for testSeg in $(seq 0 $LIMIT)
  do
    CTYPE=-s
    run_all StreamingKMeans stkm $testSeg
    CTYPE=-b
    run_all ClusteringByCommittee cbc $testSeg
    run_all CKVWSpectralClustering06 sc06 $testSeg
    #run_all GapStatistic gs-kmeans $testSeg
  done
done
}

processWordList psd-noun-word-list.txt noun
processWordList psd-verb-word-list.txt verb
