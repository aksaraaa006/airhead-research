#!/bin/bash
# To RUN:
# 1) Change CORP_PATH to correct location of SemEval2010
# 2) run with 
#  ./run_single_psd_model.sh /path/to/SemEvalResults/psd_results confoudners.txt POS clustModel sspaceModel
# POS can be: NOUN VERB ADV ADJ
# clustModel can be: cbc stkm sc06 gs-kmeans
# sspaceModel can be: wordsi order pos rel
keyMap=$2
pos=$3
model=$4
sspace=$5

bdir=$1
mkdir -p $bdir/res

CORP_PATH=~/psd_contexts
CLUST=edu.ucla.sspace.clustering
MAIN=edu.ucla.sspace.mains
krackenPath=/wukung/SemEvalResults/

LIMIT=4

j="java -Xmx16g -server "

run() {
  corp=""
  # Tac on the extra corpora segments, if specified.
  for i in $(seq 0 $LIMIT)
  do
    [ $i -eq $1 ] && continue
    corp=$CORP_PATH/$keyWord.$pos.contexts.$i,$CORP_PATH/$confounder.$pos.contexts.$i,$corp
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

  corp=$CORP_PATH/$keyWord.$pos.contexts.$testSeg,$CORP_PATH/$confounder.$pos.contexts.$testSeg
  $j $extArgs $MAIN.$alg -d $corp -P $keyMap -W $window -c $numClust -v \
     -L $bdir/$outputName.basis $extraArgs -e $bdir/$outputName-train.sspace \
     $bdir/$outputName-test.sspace \
     2> $bdir/$outputName-test.log \
     > $bdir/res/$outputName-test.counts
}

run_all() {
  if [ "$sspace" == "wordsi" ]
  then
    alg=DVWCWordsiMain
    algOpts=""
    window=5
    numClust=15

    algName="dv-wc-wordsi"
    run $3 "$CTYPE $CLUST.$1" $2 ""
  elif [ "$sspace" == "order" ]
  then
    alg=DVWCWordsiMain
    algOpts=""
    window=5
    numClust=15

    algName="dv-wc-wordsi-order"
    run $3 "$CTYPE $CLUST.$1" $2 "-G edu.ucla.sspace.hal.GeometricWeighting"
  elif [ "$sspace" == "pos" ]
  then
    alg=DVWCWordsiMain
    algOpts=""
    window=5
    numClust=15

    algName="dv-wc-wordsi-pos"
    run $3 "$CTYPE $CLUST.$1" $2 "-O"
  elif [ "$sspace" == "rel" ]
  then
    alg=DVWordsiMain
    algOpts=""
    window=3
    numClust=15

    algName="dv-wordsi-rel"
    run $3 "$CTYPE $CLUST.$1" $2 "-B edu.ucla.sspace.dv.RelationBasedBasisMapping"
  fi
}

for testSeg in $(seq 0 $LIMIT)
do
  if [ "$model" == "stkm" ]
  then
    CTYPE=-s
    NAME="StreamingKMeans"
  elif [ "$model" == "cbc" ]
  then
    CTYPE=-b
    NAME=ClusteringByCommittee
  elif [ "$model" == "sc06" ]
    CTYPE=-b
    NAME=CKVWSpectralClustering06
  then
  elif [ "$model" == "gs-kmeans" ]
  then
    CTYPE=-b
    NAME=GapStatistic
  fi

  run_all GapStatistic gs-kmeans $testSeg
done
