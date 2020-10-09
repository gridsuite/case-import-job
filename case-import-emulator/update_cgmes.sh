#!/bin/bash

cgmesPath=$1
outputDir=$2
outputType="COMPLETE"
if [[ $# -eq 3 ]]
then
  outputType=$3
fi
version=001

# generate a new date for the CGMES
caseDateIso=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
caseDate=$(date -d $caseDateIso +"%Y%m%dT%H%MZ")

# create a working directory
tmpDir=$(mktemp -d)
pwdDir=$(pwd)
cd $tmpDir

# copy the zip input file to the working directory
cp $cgmesPath $tmpDir

# unzip the file
zipFile=$(basename $1)
caseName=${zipFile%%.zip}

unzip *.zip >> /dev/null

nbXml=$(ls -1q *.xml 2>/dev/null | wc -l)

if [[ "${nbXml}" -eq 1 ]]
then
  # individual zip profile file as input
  for xmlFile in $(ls *.xml); do
    sed -i -e "s/<md:Model.scenarioTime>.*<\/md:Model.scenarioTime>/<md:Model.scenarioTime>$caseDateIso<\/md:Model.scenarioTime>/g" $xmlFile
    sed -i -e "s/<md:Model.version>.*<\/md:Model.version>/<md:Model.version>$version<\/md:Model.version>/g" $xmlFile
    sed -i -e "s/<md:Model.created>.*<\/md:Model.created>/<md:Model.created>$caseDateIso<\/md:Model.created>/g" $xmlFile
  done
  rm $zipFile
  newZipFile=${caseDate}_${zipFile#*_}
  zip -j $newZipFile *.xml >> /dev/null
  rm -f $outputDir/${newZipFile}
  mv ${newZipFile} $outputDir
else
  # complete cgmes zip file as input
  if [[ -d $caseName ]]
  then
    cd $caseName
  fi

  if [[ "${outputType}" == "INDIVIDUAL" ]]
  then
    # generating individual profile zip files for SV, TP, SSH and EQ
    for xmlFile in $(ls *.xml); do
      # we only deal with SV, TP, SSH and EQ profiles
      if [[ "$xmlFile" =~ ^.*_(SV|TP|EQ|SSH)_[0-9]+[.]xml$ ]]
      then
        # for each of the profile replace scenarioTime, version and created
        sed -i -e "s/<md:Model.scenarioTime>.*<\/md:Model.scenarioTime>/<md:Model.scenarioTime>$caseDateIso<\/md:Model.scenarioTime>/g" $xmlFile
        sed -i -e "s/<md:Model.version>.*<\/md:Model.version>/<md:Model.version>$version<\/md:Model.version>/g" $xmlFile
        sed -i -e "s/<md:Model.created>.*<\/md:Model.created>/<md:Model.created>$caseDateIso<\/md:Model.created>/g" $xmlFile

        zipFile=${xmlFile%%.xml}
        newZipFile=${caseDate}_${zipFile#*Z_}

        # for EQ profile, deal with optional businessProcess part in file name (two underscores after dateTime needed in that case)
        if [[ "$newZipFile" =~ ^.*_EQ_[0-9]+.*$ ]]
        then
          nbUnderscore=$(grep -o "_" <<< "$newZipFile" | wc -l)
          if [[ "${nbUnderscore}" -eq 3 ]]
          then
            newZipFile=${newZipFile%Z_*}Z__${newZipFile#*Z_}
          fi
        fi

        # add version padding with zeros (3 characters), if needed
        if [[ "$newZipFile" =~ ^.*_[0-9]$ ]]
        then
          newZipFile=${newZipFile%*_[0-9]}_`printf "%03d" ${newZipFile##*_}`
        fi

        # rebuild the zip with update profile and using a new naming
        zip -j $newZipFile $xmlFile >> /dev/null
        rm -f $outputDir/${newZipFile}.zip
        mv ${newZipFile}.zip $outputDir
      fi
    done
  elif [[ "${outputType}" == "COMPLETE" ]]
  then
      # generating complete cgmes zip file
      for xmlFile in $(ls *.xml); do
        sed -i -e "s/<md:Model.scenarioTime>.*<\/md:Model.scenarioTime>/<md:Model.scenarioTime>$caseDateIso<\/md:Model.scenarioTime>/g" $xmlFile
        sed -i -e "s/<md:Model.version>.*<\/md:Model.version>/<md:Model.version>$version<\/md:Model.version>/g" $xmlFile
        sed -i -e "s/<md:Model.created>.*<\/md:Model.created>/<md:Model.created>$caseDateIso<\/md:Model.created>/g" $xmlFile
      done

      # rebuild the zip with update profiles and using a new naming
      cd $tmpDir
      rm $zipFile
      newZipFile=${caseDate}_${zipFile#*_}
      if [[ -d $caseName ]]
      then
        zip -j ${newZipFile%%.zip} $caseName/*.xml >> /dev/null
      else
        zip -j ${newZipFile%%.zip} *.xml >> /dev/null
      fi
      rm -f $outputDir/${newZipFile}
      mv ${newZipFile} $outputDir
  fi
fi

cd $pwdDir

# delete the working directory
rm -rf $tmpDir
