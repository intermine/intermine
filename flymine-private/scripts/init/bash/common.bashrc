# This script will:
# 1. Define useful functions
# 2. Set a default PATH and LD_LIBRARY_PATH
# 3. Set INIT, SRC, DOC and LOG if appropriate
# 4. Identify the groups to which the user belongs and run the
#    appropriate startup scripts

addpath () {
  eval value=\"\$$1\"
  case "$value" in
    *:$2:*|*:$2|$2:*|$2)
      result="$value"
      ;;
    "")
      result="$2"
      ;;
    *)
      case "$3" in
        p*)
          result="$2:${value}"
          ;;
        *)
          result="${value}:$2"
          ;;
      esac
      ;;
  esac
  eval $1=$result
  eval export $1
  unset result value
}

# convenience routine which appends a string to a path.
append () {
  addpath "$1" "$2" append
}

# convenience routine which prepends a string to a path.
prepend () {
  addpath "$1" "$2" prepend
}

pathfinder() {
  perl -e 'foreach(split(/:/,$ENV{$ARGV[0]})) { if (-f "$_/$ARGV[1]")
    {print "$_/$ARGV[1]\n"; last}}' $1 $2
}

machine=`uname`
host=`hostname`

# Trust the system path for the main binaries

prepend PATH /usr/local/bin
prepend PATH /software/noarch/bin
prepend PATH /software/arch/bin
prepend PATH /software/arch/postgresql/bin
prepend PATH /software/noarch/local/bin
prepend PATH /software/arch/local/bin

prepend MANPATH /software/noarch/man
prepend MANPATH /software/arch/man

# Linux needs an extra colon in order to search the system path
if [ "$machine" = Linux ]; then
    prepend MANPATH :
fi

if [ "${HOME:+set}" = "set" ]; then
    prepend PATH $HOME/bin
    prepend MANPATH $HOME/man
    if [ -d $HOME/$machine/bin ]; then
	prepend PATH $HOME/$machine/bin
    fi
fi

append LD_LIBRARY_PATH /software/arch/lib
append LD_LIBRARY_PATH /software/arch/local/lib

append INIT /software/noarch/local/init

########################################################
# Run the initialisation scripts for each group that   #
# the user belongs, if such a script exists            #
########################################################

for GROUP in `groups`; do
  file=`pathfinder INIT bash/${GROUP}.bashrc`
  if [ "$file" ]; then
    . $file
  fi
done
