#!/bin/bash
echo "==Deposit new case file=="

importDate=$(date +"%Y%m%d_%H%M")

fileToImport="example.xml"

fileName=$(basename -- "cases/$fileToImport")
echo "[INFO] File to deposit: $fileName"
fileName="${fileName%.*}"

uniqueFileName="${importDate}_SN5_D80.xml"
echo "[INFO] File is deposit as: $uniqueFileName"

cp cases/$fileToImport ~/opde/$uniqueFileName

caseDate=$(date +"%Y-%m-%dT%T.000+01:00")

#Change the case date to the current date
xsltproc --stringparam dateReplacement "$caseDate" changedate.xsl ~/opde/$uniqueFileName > ~/opde/"$uniqueFileName.tmp"
rm ~/opde/$uniqueFileName
mv ~/opde/"$uniqueFileName.tmp" ~/opde/$uniqueFileName
echo "[INFO] $uniqueFileName deposit in ~/opde"
echo "==End deposit new case file=="

echo "==End remove old files=="
cd ~/opde
ls -tp | grep -v '/$' | tail -n +101 | xargs -d '\n' -r rm --
echo "==Remove old files=="

