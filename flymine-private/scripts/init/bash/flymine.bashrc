# This script will run the appropriate tasks for the group

# Make files non-world readable by default
umask 0007

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

# Set up the prompt
case $TERM in
    *term | rxvt )
	PS1="\u@\h:\w\$ \[\033]0;\u@\h:\w\007\]"
        if [ "$COLORTERM" = "gnome-terminal" ]
        then
	    PS1="\u@\h:\w\$ "
        fi
        ;;
    *)
	PS1="\u@\h:\w\$ " ;;
esac

# Set up the shell (part 2)

set -o nounset

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

if [ "${SVNTREE:-unset}" = "unset" ]; then
    if [ "${HOME:+set}" = "set" ] && [ -d $HOME/svn/dev ]; then
	SVNTREE=$HOME/svn/dev; export SVNTREE
    fi
fi

if [ "${INTERMINE:-unset}" = "unset" ]; then
    if [ "${SVNTREE:+set}" = "set" ] && [ -d $SVNTREE/intermine ]; then
	INTERMINE=$SVNTREE/intermine; export INTERMINE
    fi
fi

if [ "${FLYMINE:-unset}" = "unset" ]; then
    if [ "${SVNTREE:+set}" = "set" ] && [ -d $SVNTREE/flymine ]; then
	FLYMINE=$SVNTREE/flymine; export FLYMINE
    fi
fi

if [ "${FLYMINE_PRIVATE:-unset}" = "unset" ]; then
    if [ "${SVNTREE:+set}" = "set" ] && [ -d $SVNTREE/flymine-private ]; then
	FLYMINE_PRIVATE=$SVNTREE/flymine-private; export FLYMINE_PRIVATE
    fi
fi

# Location that all logs will be written to
if [ "${LOG:-unset}" = "unset" ]; then
	LOG=/shared/log; export LOG
fi

alias log='cd $LOG'

# Aliases for working with the private CVS tree

if [ "${FLYMINE_PRIVATE:+set}" = "set" ]; then
    alias doc='cd $FLYMINE_PRIVATE/doc'
    alias scr='cd $FLYMINE_PRIVATE/scripts'
fi

# Aliases for working with the public CVS tree

if [ "${FLYMINE:+set}" = "set" ]; then
    alias fly='cd $FLYMINE'
    alias flysrc='cd $FLYMINE/src/org/flymine'
    alias flydoc='cd $FLYMINE/doc'
fi

# Options for using ANT with FlyMine
if [ "${ANT_OPTS:+set}" = "set" ]; then
    ANT_OPTS=$ANT_OPTS\ -XX:MaxPermSize=256M; export ANT_OPTS
else
    ANT_OPTS=-XX:MaxPermSize=256M; export ANT_OPTS
fi
