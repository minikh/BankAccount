#!/bin/sh

cd /opt/money-transfer/
./gradlew clean build --info

tar -xvf /opt/money-transfer/build/distributions/money-transfer-0.0.1.tar -C /
/money-transfer-0.0.1/bin/money-transfer
