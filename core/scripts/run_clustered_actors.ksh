#!/bin/ksh

HOST=$1
NUM_STOCK_DIRECTORY_ACTORS=$2
NUM_DATA_RETRIEVER_ACTORS=$3
NUM_STOCK_DIRECTORY_ROUTER_ACTORS=$4

cd ..

echo 'Starting stock directory actors ...'

sbt "run-main com.sungard.poc.pricingengine.actors.main.cluster.ClusteredStockDirectoryMain $HOST 2551 $HOST:2551" > "/var/tmp/directoryActors1" &
sleep 10

for i in {2..$NUM_STOCK_DIRECTORY_ACTORS}
do
    let akka_port=2551+$i-1
    sbt "run-main com.sungard.poc.pricingengine.actors.main.cluster.ClusteredStockDirectoryMain $HOST $akka_port $HOST:2551 $HOST:2552" > "/var/tmp/directoryActors$i" &
    sleep 10
done

sleep 10

echo 'Starting data retriever actors ...'

for i in {1..$NUM_DATA_RETRIEVER_ACTORS}
do
    let akka_port=2551+$i-1+$NUM_STOCK_DIRECTORY_ACTORS
    sbt "run-main com.sungard.poc.pricingengine.actors.main.cluster.ClusteredDataRetrieverMain $HOST $akka_port $HOST:2551 $HOST:2552" > "/var/tmp/dataRetrieverActors$i" &
    sleep 10
done

sleep 10

echo 'Starting stock directory router actors ...'

for i in {1..$NUM_STOCK_DIRECTORY_ROUTER_ACTORS}
do
    let akka_port=2551+$i-1+$NUM_STOCK_DIRECTORY_ACTORS+$NUM_DATA_RETRIEVER_ACTORS
    sbt "run-main com.sungard.poc.pricingengine.actors.main.cluster.ClusteredStockDirectoryRouterMain $HOST $akka_port $HOST:2551 $HOST:2552" > "/var/tmp/stockDirectoryRouterActors$i" &
    sleep 10
done

sleep 10

echo 'Starting stock portfolio valuation actor ...'

let STOCK_DIR_ROUTER_PORT_1=2551+$NUM_STOCK_DIRECTORY_ACTORS+$NUM_DATA_RETRIEVER_ACTORS
let STOCK_DIR_ROUTER_PORT_1=2552+$NUM_STOCK_DIRECTORY_ACTORS+$NUM_DATA_RETRIEVER_ACTORS


let akka_port=2551+$NUM_STOCK_DIRECTORY_ACTORS+$NUM_DATA_RETRIEVER_ACTORS+$NUM_STOCK_DIRECTORY_ROUTER_ACTORS
sbt "run-main com.sungard.poc.pricingengine.actors.main.cluster.ClusteredStockPortfolioValuationMain $HOST $akka_port ClusterSystem $HOST $STOCK_DIR_ROUTER_PORT_1 $HOST $STOCK_DIR_ROUTER_PORT_2" > "/var/tmp/stockPortfolioValuationActors" &

sleep 20

trap 'echo "Killing SBT processes"; pkill -f sbt' INT