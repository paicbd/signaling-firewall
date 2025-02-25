#!/bin/bash
trap 'kill -TERM $PID' TERM INT
#rm -rf *.xml
java -Xms6g -Xmx6g -cp firewall-VERSION.jar:libs/* -Dlog4j.configuration=file:../conf/log4j.xml -Dorg.restcomm.sctp.bufferSize=50000000 -DmainConfig.path=../conf com.paic.firewall.SignalingFirewall &
PID=$!
wait $PID
trap - TERM INT
wait $PID
EXIT_STATUS=$?

