#!/bin/bash
function check_style {
	# No tab characters allowed.
  #

	if grep -q -e $'\t' $1
	then
	  grep -e $'\t' $1
	  echo "File $1 contains tab character."
	  exit 1
  fi

  # Correct would be \r\n but \r should not occurr.
  #

  if grep -q -e $'\r' $1
  then
    grep -e $'\r' $1
    echo "ERROR: File $1 contains Windows line endings."
    exit 1
  fi

  # may not contain case (.+?) =>
  # may not contain for { (.+?) <-
  # must end with new line
}

for file in `find . -iname *.scala -type f` ; do
	check_style $file
done