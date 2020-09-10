#!/bin/bash

cgmesPath=$1
outputDir=$2
version=1

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
    # we only deal with SV, TP, SSH and EQ profiles
    if [[ "$xmlFile" =~ ^.*_(SV|TP|EQ|SSH)_[0-9]+[.]xml$ ]] 
    then
      sed -i -e "s/<md:Model.scenarioTime>.*<\/md:Model.scenarioTime>/<md:Model.scenarioTime>$caseDateIso<\/md:Model.scenarioTime>/g" $xmlFile
      sed -i -e "s/<md:Model.version>.*<\/md:Model.version>/<md:Model.version>$version<\/md:Model.version>/g" $xmlFile
      zipFile=${xmlFile%%.xml}
      newZipFile=${caseDate}_${zipFile#*Z_}
      # for EQ profile, deal with optional businessProcess part in file name (two underscores after dateTime needed in that case)
      if [[ "$newZipFile" =~ ^.*_EQ_[0-9]+.*$ && ! "$newZipFile" =~ ^.*Z__.*_EQ_.*$ ]]
      then
        newZipFile=${newZipFile%Z_*}Z__${newZipFile#*Z_}
      fi
      # add version padding with zeros (3 characters needed)
      newZipFile=${newZipFile%*_[0-9]}_`printf "%03d" ${newZipFile##*_}` 
      # rebuild the zip with update profile and using a new naming
      zip -j $newZipFile $xmlFile >> /dev/null
      # copy the new CGMES to output directory
      rm -f $outputDir/${newZipFile}.zip
      mv ${newZipFile}.zip $outputDir
    fi
done

cd ..

# delete the working directory
rm -rf $tmpDir
