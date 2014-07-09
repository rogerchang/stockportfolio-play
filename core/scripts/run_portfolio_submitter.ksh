#!/bin/ksh

HOST=$1
PORT=$2
CLUSTER_NAME=$3

cd ..

echo 'Starting portfolio submitter ...'

sbt "run-main com.sungard.poc.pricingengine.actors.main.cluster.PortfolioSubmitterMain $HOST $PORT $CLUSTER_NAME"


