#!/bin/bash

cgmesPath=$1
outputDir=$2
version=001

caseDateIso=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
caseDate=$(date -d $caseDateIso +"%Y%m%dT%H%MZ")

tmpDir=$(mktemp -d)
cd $tmpDir

cp $cgmesPath $tmpDir
zipFile=$(basename $1)
caseName=${zipFile%%.zip}
unzip *.zip >> /dev/null
for xmlFile in $(ls *.xml); do
    sed -i -e "s/<md:Model.scenarioTime>2017-10-02T09:30:00Z<\/md:Model.scenarioTime>/<md:Model.scenarioTime>$caseDateIso<\/md:Model.scenarioTime>/g" $xmlFile
    sed -i -e "s/<md:Model.version>.*<\/md:Model.version>/<md:Model.version>$version<\/md:Model.version>/g" $xmlFile
    sed -i -e "s/<md:Model.created>.*<\/md:Model.created>/<md:Model.created>$caseDateIso<\/md:Model.created>/g" $xmlFile
done
rm $zipFile
newZipFile=${caseDate}_${zipFile#*_}
zip -j $newZipFile * >> /dev/null
rm -f $outputDir/${newZipFile}
mv ${newZipFile} $outputDir

rm -rf $tmpDir
