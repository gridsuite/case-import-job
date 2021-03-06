#!/bin/bash
current_dir=$(dirname $(readlink -f $0))
cd $current_dir

echo "==Deposit new case file=="

importDate=$(date +"%Y%m%d_%H%M")

fileToImport="example.xml"

fileName=$(basename -- "cases/$fileToImport")
echo "[INFO] File to deposit: $fileName"
fileName="${fileName%.*}"

uniqueFileName="${importDate}_SN5_D80.xml"
echo "[INFO] File is deposit as: $uniqueFileName"

cp cases/$fileToImport cases/$uniqueFileName

#Change the case date to the current date
caseDate=$(date +"%Y-%m-%dT%T.000+01:00")
xsltproc --stringparam dateReplacement "$caseDate" changedate.xsl cases/$uniqueFileName > cases/"$uniqueFileName.tmp"
rm cases/$uniqueFileName
mv cases/"$uniqueFileName.tmp" ~/opde/$uniqueFileName

echo "[INFO] $uniqueFileName deposit in ~/opde"
echo "==End deposit new case file=="
printf "\n"
echo "==Remove old files=="
cd ~/opde	
if [ $? -eq 0 ]; then
    ls -tp | grep -v '/$' | tail -n +101 | xargs -d '\n' -r rm --
else
	echo $?
fi
echo "==End remove old files=="
