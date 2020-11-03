#!/bin/bash

cgmesPath=$1
outputDir=$2
outputType="COMPLETE"
if [[ $# -eq 3 ]]
then
  outputType=$3
fi
version=001

# update uuid in xml profile
# $1 is the new uuid
# $2 is the xml file name
updateUuid() {
  sed -i -e "s/\(<md:FullModel[ ]*rdf:about=\"urn:uuid:\).*\(\">\)/\1$1\2/g" $2
}

# update scenario time, created and version in xml profile
# $1 is the case date in iso format
# $2 is the version
# $3 is the xml file name
updateTimeAndVersion()
{
  sed -i -e "s/<md:Model.scenarioTime>.*<\/md:Model.scenarioTime>/<md:Model.scenarioTime>$1<\/md:Model.scenarioTime>/g" $3
  sed -i -e "s/<md:Model.version>.*<\/md:Model.version>/<md:Model.version>$2<\/md:Model.version>/g" $3
  sed -i -e "s/<md:Model.created>.*<\/md:Model.created>/<md:Model.created>$1<\/md:Model.created>/g" $3
}

# update dependencies uuids in xml profile
# $1 is the old uuid
# $2 is the new uuid
# $3 is the xml file name
updateDependencies() {
  sed -i -e "s/\(<md:Model.DependentOn[ ]*rdf:resource=\"urn:uuid:\)$1\(.*\)/\1$2\2/g" $3
}


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
    newUuid=$(uuidgen)
    updateTimeAndVersion $caseDateIso $version $xmlFile
    updateUuid ${newUuid} $xmlFile
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

  for xmlFile in $(ls *.xml); do
    # memorizing old and new uuid, and setting new uuid for SV, TP, EQ, SSH profiles
    if [[ "$xmlFile" =~ ^.*_(SV)_[0-9]+[.]xml$ ]]
    then
      oldUuidSV=$(grep 'md:FullModel.*rdf:about=' $xmlFile | cut -d'"' -f 2 | cut -d':' -f 3)
      newUuidSV=$(uuidgen)
      sed -i -e "s/\(<md:FullModel[ ]*rdf:about=\"urn:uuid:\).*\(\">\)/\1${newUuidSV}\2/g" $xmlFile
    elif [[ "$xmlFile" =~ ^.*_(TP)_[0-9]+[.]xml$ ]]
    then
      oldUuidTP=$(grep 'md:FullModel.*rdf:about=' $xmlFile | cut -d'"' -f 2 | cut -d':' -f 3)
      newUuidTP=$(uuidgen)
      sed -i -e "s/\(<md:FullModel[ ]*rdf:about=\"urn:uuid:\).*\(\">\)/\1${newUuidTP}\2/g" $xmlFile
    elif [[ "$xmlFile" =~ ^.*_(EQ)_[0-9]+[.]xml$ ]]
    then
      oldUuidEQ=$(grep 'md:FullModel.*rdf:about=' $xmlFile | cut -d'"' -f 2 | cut -d':' -f 3)
      newUuidEQ=$(uuidgen)
      sed -i -e "s/\(<md:FullModel[ ]*rdf:about=\"urn:uuid:\).*\(\">\)/\1${newUuidEQ}\2/g" $xmlFile
    elif [[ "$xmlFile" =~ ^.*_(SSH)_[0-9]+[.]xml$ ]]
    then
      oldUuidSSH=$(grep 'md:FullModel.*rdf:about=' $xmlFile | cut -d'"' -f 2 | cut -d':' -f 3)
      newUuidSSH=$(uuidgen)
      sed -i -e "s/\(<md:FullModel[ ]*rdf:about=\"urn:uuid:\).*\(\">\)/\1${newUuidSSH}\2/g" $xmlFile
    fi
  done

  if [[ "${outputType}" == "INDIVIDUAL" ]]
  then
    # generating individual profile zip files for SV, TP, SSH and EQ
    for xmlFile in $(ls *.xml); do
      # we only deal with SV, TP, SSH and EQ profiles
      if [[ "$xmlFile" =~ ^.*_(SV|TP|EQ|SSH)_[0-9]+[.]xml$ ]]
      then
        # for each of the profile replace scenarioTime, version, created and dependencies with new uuids
        updateTimeAndVersion $caseDateIso $version $xmlFile
        updateDependencies ${oldUuidSV} ${newUuidSV} $xmlFile
        updateDependencies ${oldUuidTP} ${newUuidTP} $xmlFile
        updateDependencies ${oldUuidEQ} ${newUuidEQ} $xmlFile
        updateDependencies ${oldUuidSSH} ${newUuidSSH} $xmlFile

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
        # for each of the profile replace scenarioTime, version, created and dependencies with new uuids
        updateTimeAndVersion $caseDateIso $version $xmlFile
        updateDependencies ${oldUuidSV} ${newUuidSV} $xmlFile
        updateDependencies ${oldUuidTP} ${newUuidTP} $xmlFile
        updateDependencies ${oldUuidEQ} ${newUuidEQ} $xmlFile
        updateDependencies ${oldUuidSSH} ${newUuidSSH} $xmlFile
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
