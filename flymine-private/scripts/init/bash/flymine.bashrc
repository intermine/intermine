# This script will run the appropriate tasks for the group

# Make files non-world readable by default
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

# Set up some defaults. These can be overridden in user profiles.

if [ "${EDITOR:-unset}" = "unset" ]; then
    EDITOR=emacs; export EDITOR
fi

if [ "${VISUAL:-unset}" = "unset" ]; then
    VISUAL="emacs -nw"; export VISUAL
fi

#XTERMMOUSE=no; export XTERMMOUSE
#LESSCHARSET=latin1; export LESSCHARSET

if [ "${HOME:+set}" = "set" ]; then
    alias rmallbak="find $HOME/. \( -name .snapshot -prune \) -o \( -name '*~' -o -name '.*~' -o -name '*#' \) -print -exec rm {} \;"
fi

if [ "${CVSTREE:-unset}" = "unset" ]; then
    if [ "${HOME:+set}" = "set" ] && [ -d $HOME/cvs ]; then
	CVSTREE=$HOME/cvs; export CVSTREE
    fi
fi

if [ "${FLYMINE:-unset}" = "unset" ]; then
    if [ "${CVSTREE:+set}" = "set" ] && [ -d $CVSTREE/flymine ]; then
	FLYMINE=$CVSTREE/flymine; export FLYMINE
    fi
fi

if [ "${FLYMINE_PRIVATE:-unset}" = "unset" ]; then
    if [ "${CVSTREE:+set}" = "set" ] && [ -d $CVSTREE/flymine-private ]; then
	FLYMINE_PRIVATE=$CVSTREE/flymine-private; export FLYMINE_PRIVATE
    fi
fi

# Location that all logs will be written to
if [ "${LOG:-unset}" = "unset" ]; then
    if [ -w /shared/log ]; then
	LOG=/shared/log; export LOG
    else
	if [ "${HOME:+set}" = "set" ] && [ -d $HOME/log ]; then
	    LOG=${HOME}/log; export LOG
	fi
    fi
fi

# Aliases for working with the private CVS tree

if [ "${FLYMINE_PRIVATE:+set}" = "set" ]; then
    alias doc='cd $FLYMINE_PRIVATE/doc'
    alias scr='cd $FLYMINE_PRIVATE/scripts'
fi

