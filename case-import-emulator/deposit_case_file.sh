#!/bin/bash
echo "Import new case"

importDate=$(date +"%Y%m%d_%H%M")
echo "Import date = $importDate"
fileToImport="example.xml"

fileName=$(basename -- "cases/$fileToImport")
fileName="${fileName%.*}"
echo "Filename = $fileName"

uniqueFileName="${importDate}_SN5_D80.xml"
echo "uniqueFileName = $uniqueFileName"


cp cases/$fileToImport ~/opde/$uniqueFileName

caseDate=$(date +"%Y-%m-%dT%T.000+01:00")
echo "Case date = $caseDate"


xsltproc --stringparam dateReplacement "$caseDate" changedate.xsl ~/opde/$uniqueFileName > ~/opde/"$uniqueFileName.tmp"
rm ~/opde/$uniqueFileName
mv ~/opde/"$uniqueFileName.tmp" ~/opde/$uniqueFileName

#Clean old cases
filesCount=$(ls -1q ~/opde* | wc -l)
while [ $filesCount -gt 100 ] 
do
	#remove oldest file
	oldestFile=$(find ~/opde -type f | sort | head -n 1)
	rm $oldestFile
	filesCount=$[$filesCount - 1]
done



