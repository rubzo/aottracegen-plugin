#!/bin/bash

BAKSMALI_JAR=/home/skyle/bin/smali_extra/baksmali-1.4.2-dev.jar

if [ ! -e "$BAKSMALI_JAR" ]; then
	echo "Cannot find baksmali.jar - needed in plugin creation."
	exit 1
fi

find . -iname "*.java" | xargs javac -cp $BAKSMALI_JAR 
find . -iname "*.class" | xargs jar -cf aottracegen-plugin.jar 
echo "Created aottracegen-plugin.jar"
