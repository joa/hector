#!/bin/bash

#
# This is supposed to be a Git commit hook once it is
# finished. The check_style function must simply be 
# called for each file in the changeset.
#

function check_style {
  # No tab characters allowed.
  #

  if grep -q $'\t' $1
  then
    grep -C 1 --color=auto $'\t' $1
    echo "File $1 contains tab character."
    exit 1
  fi

  # Correct would be \r\n but \r should not occurr.
  #

  if grep -q $'\r' $1
  then
    grep -C 1 --color=auto $'\r' $1
    echo "ERROR: File $1 contains Windows line endings."
    exit 1
  fi

  # Do not allow => character sequence
  #

  if grep -q "=>" $1
  then
    grep -C 1 --color=auto "=>" $1
    echo "ERROR: File $1 contains \"=>\" instead of \"⇒\"."
    exit 1
  fi

  # Do not allow <- character sequence.
  # 

  if grep -q "<-" $1
  then
    grep -C 1 --color=auto "<-" $1
    echo "ERROR: File $1 contains \"<-\" instead of \"←\"."
    exit 1
  fi

  # File must end in new line.

  if [ "`tail -c 1 $1`" != "" ]
  then
    echo "ERROR: File $1 does not end with \\n."
    exit 1
  fi
}

for file in `find . -iname *.scala -type f` ; do
  check_style $file
done
