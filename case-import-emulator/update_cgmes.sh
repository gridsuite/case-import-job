#!/bin/bash

cgmesPath=$1
country=$2
outputDir=$3
version=001

# generate a new date for the CGMES
caseDateIso=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
caseDate=$(date -d $caseDateIso +"%Y%m%dT%H%MZ")

# create a working directory
tmpDir=$(mktemp -d)
cd $tmpDir

# copy the CGMES to the working directory
cp $cgmesPath $tmpDir

# unzip the CGMES
zipFile=$(basename $1)
unzip *.zip >> /dev/null

# for each of the profile replace scenarioTime and version
caseName=${zipFile%%.zip}
cd $caseName
for xmlFile in $(ls *.xml); do
    sed -i -e "s/<md:Model.scenarioTime>.*<\/md:Model.scenarioTime>/<md:Model.scenarioTime>$caseDateIso<\/md:Model.scenarioTime>/g" $xmlFile
    sed -i -e "s/<md:Model.version>.*<\/md:Model.version>/<md:Model.version>$version<\/md:Model.version>/g" $xmlFile
done

# rebuild the zip with update profiles and using a new naming
cd ..
rm $zipFile
newZipFile=${caseDate}_1D_${country}_${version}
zip -j $newZipFile $caseName/* >> /dev/null

# copy the new CGMES to output directory
rm -f $outputDir/${newZipFile}.zip
mv ${newZipFile}.zip $outputDir

# delete the working directory
rm -rf $tmpDir
