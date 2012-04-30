#!/bin/bash

# Hector style check
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

  # Naming conventions for getters and setters
  # Currently problematic with overriden methods like override def setUp() for Caliper.

: <<'END'
  if egrep -q "def\s+?get(.+?)(\s|\:|=)" $1
  then
    egrep -C 1 --color=auto "def\s+?get(.+?)(\s|\:|=)" $1
    echo "ERROR: Prefixed getter in $1. A getter like \"def getCount: Int = ...\" should be named \"def count: Int = ...\"."
    exit 1
  fi

  if egrep -q "def\s+?set(.+?)\(" $1
  then
    egrep -C 1 --color=auto "def\s+?set(.+?)\(" $1
    echo "ERROR: Prefixed setter in $1. A setter like \"def setCount(value: Int) { ... }\" should be named \"def count_=(value: Int) { ... }\"."
    exit 1
  fi
END

: <<'END'
  if egrep -q "def\s+?(.+?)\((.+?)\)[^\:]" $1
  then
    egrep -C 1 --color=auto "def\s+?(.+?)\((.+?)\)[^\:]" $1
    echo "ERROR: Explicit return-type is missing. 1"
    exit 1
  fi

  if egrep -q "def\s+?(.+)[^\:]\s+?=" $1
  then
    egrep -C 1 --color=auto "def\s+?(.+)[^\:]\s+?=" $1
    echo "ERROR: Explicit return-type is missing. 2"
    exit 1
  fi
END

  # File must end in new line.

  if [ "`tail -c 1 $1`" != "" ]
  then
    echo "ERROR: File $1 does not end with \\n."
    exit 1
  fi
}

#
# This is what you could use to check all files in the repo.
#
# for file in `find . -iname *.scala -type f` ; do
#   check_style $file
# done
#

#
# Git pre-commit hook stuff behaviour
#

if git rev-parse --verify HEAD >/dev/null 2>&1
then
        against=HEAD
else
        # Initial commit: diff against an empty tree object
        against=4b825dc642cb6eb9a060e54bf8d69288fbee4904
fi

files=$(git diff-index --name-status --cached $against | grep -v ^D | cut -c3-)

if [ "$files" != "" ]
then
  for file in $files
  do
    if [[ "$file" =~ [.]scala$ ]]
    then
      check_style $file
    fi
  done
fi
