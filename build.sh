#!/bin/bash

BAKSMALI_JAR=~/Code/itrace-smali/baksmali/build/libs/baksmali-1.4.2-dev.jar

if [ ! -e "$BAKSMALI_JAR" ]; then
	echo "Cannot find baksmali.jar - needed in plugin creation."
	exit 1
fi

javac -cp $BAKSMALI_JAR eu/whrl/aottracegen/*.java eu/whrl/aottracegen/*/*.java
jar -cf aottracegen-plugin.jar eu/whrl/aottracegen/*.class eu/whrl/aottracegen/*/*.class
echo "Created aottracegen-plugin.jar"
