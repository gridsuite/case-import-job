#!/bin/bash
echo "Import new case"

importDate=$(date +"%Y%m%d_%H%M")
echo "Import date = $importDate"

fileName=$(basename -- "cases/LF.xml")
fileName="${fileName%.*}"
echo "Filename = $fileName"

uniqueFileName="${importDate}_SN5_D80.xml"
echo "uniqueFileName = $uniqueFileName"

fileToImport="LF.xml"
cp cases/$fileToImport ~/opde/$uniqueFileName

caseDate=$(date +"%Y-%m-%dT%T.000+01:00")
echo "Case date = $caseDate"


xsltproc --stringparam dateReplacement "$caseDate" changedate.xsl ~/opde/$uniqueFileName > ~/opde/"$uniqueFileName.tmp"
rm ~/opde/$uniqueFileName
mv ~/opde/"$uniqueFileName.tmp" ~/opde/$uniqueFileName
