# This script will run the appropriate tasks for the group

umask 0027

# for TASK in production sybase java lims perl; do
for TASK in java cvs perl; do
    file=`pathfinder INIT bash/${TASK}.bashrc`
    if [ "$file" ]; then
	. $file
    fi
done

# Set up the shell

shopt -s cdspell
shopt -s checkwinsize
shopt -s cmdhist
shopt -s histappend
set -o nounset

# Set up the prompt
case $TERM in
    *term | rxvt )
	PS1="\u@\h:\w\$ \[\033]0;\u@\h:\w\007\]" ;;
    *)
	PS1="\u@\h:\w\$ " ;;
esac

case $machine in
Linux)
    if [ -r ~/.ls_colors ]; then
	eval `dircolors -b ~/.ls_colors`
    else
	eval `dircolors -b`
    fi
    alias ls='ls -F --color=auto'
    ;;
SunOS)
    ;;
*)
    ;;
esac

#LESSOPEN="|lesspipe.sh %s"; export LESSOPEN
EDITOR=emacs; export EDITOR
VISUAL="emacs -nw"; export VISUAL
XTERMMOUSE=no; export XTERMMOUSE
LESSCHARSET=latin1; export LESSCHARSET

if [ "${HOME:+set}" = "set" ]; then
    alias rmallbak="find $HOME/. \( -name .snapshot -prune \) -o \( -name '*~' -o -name '.*~' -o -name '*#' \) -print -exec rm {} \;"
fi
