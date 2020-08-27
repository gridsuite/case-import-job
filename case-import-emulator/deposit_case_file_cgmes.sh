#!/bin/bash

echo "Generating new BE and NL CGMES"

# update BE and NL files with a new date and copy then to scanned directory
~/emulator/update_cgmes.sh ~/emulator/cases/MicroGridTestConfiguration_T4_BE_BB_Complete_v2.zip BE ~/opde
~/emulator/update_cgmes.sh ~/emulator/cases/MicroGridTestConfiguration_T4_NL_BB_Complete_v2.zip NL ~/opde

# clean scanned directory
cd ~/opde
if [ $? -eq 0 ]; then
    ls -tp | grep -v '/$' | tail -n +101 | xargs -d '\n' -r rm --
else
    echo $?
fi
