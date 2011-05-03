// brush: "bash-script" aliases: []

//	This file is part of the "jQuery.Syntax" project, and is licensed under the GNU AGPLv3.
//	Copyright 2010 Samuel Williams. All rights reserved.
//	See <jquery.syntax.js> for licensing details.

Syntax.register('bash-script', function(brush) {
	var keywords = ["break", "case", "continue", "do", "done", "elif", "else", "eq", "fi", "for", "function", "ge", "gt", "if", "in", "le", "lt", "ne", "return", "then", "until", "while"];
	
	brush.push(keywords, {klass: 'keyword'});
	
	var operators = ["&", "|", ">", "<", "="];
	
	brush.push(operators, {klass: 'operator'});
	
	var commands = ["wget", "alias", "apropos", "awk", "basename", "bash", "bc", "bg", "builtin", "bzip2", "cal", "cat", "cd", "cfdisk", "chgrp", "chmod", "chown", "chrootcksum", "clear", "cmp", "comm", "command", "cp", "cron", "crontab", "csplit", "cut", "date", "dc", "dd", "ddrescue", "declare", "df", "diff", "diff3", "dig", "dir", "dircolors", "dirname", "dirs", "du", "echo", "egrep", "eject", "enable", "env", "ethtool", "eval", "exec", "exit", "expand", "export", "expr", "false", "fdformat", "fdisk", "fg", "fgrep", "file", "find", "fmt", "fold", "format", "free", "fsck", "ftp", "gawk", "getopts", "grep", "groups", "gzip", "hash", "head", "history", "hostname", "id", "ifconfig", "import", "install", "join", "kill", "less", "let", "ln", "local", "locate", "logname", "logout", "look", "lpc", "lpr", "lprint", "lprintd", "lprintq", "lprm", "ls", "lsof", "make", "man", "mkdir", "mkfifo", "mkisofs", "mknod", "more", "mount", "mtools", "mv", "netstat", "nice", "nl", "nohup", "nslookup", "op", "open", "passwd", "paste", "pathchk", "ping", "popd", "pr", "printcap", "printenv", "printf", "ps", "pushd", "pwd", "quota", "quotacheck", "quotactl", "ram", "rcp", "read", "readonly", "remsync", "renice", "rm", "rmdir", "rsync", "scp", "screen", "sdiff", "sed", "select", "seq", "set", "sftp", "shift", "shopt", "shutdown", "sleep", "sort", "source", "split", "ssh", "strace", "su", "sudo", "sum", "symlink", "sync", "tail", "tar", "tee", "test", "time", "times", "top", "touch", "tr", "traceroute", "trap", "true", "tsort", "tty", "type", "ulimit", "umask", "umount", "unalias", "uname", "unexpand", "uniq", "units", "unset", "unshar", "useradd", "usermod", "users", "uudecode", "uuencode", "v", "vdir", "vi", "watch", "wc", "whereis", "which", "who", "whoami", "xargs", "yes", "git", "svn", "ruby", "gem", "rails"];
	
	var b = "[^\\B\\-\\w\\.]";
	var commandsPattern = "(" + b + ")(" + commands.join("|") + ")(?=" + b + ")";
	
	brush.push({
		pattern: new RegExp(commandsPattern, "g"),
		matches: Syntax.extractMatches({klass: 'function', index: 2})
	});
	
	brush.push({
		pattern: /\$\w+/g,
		klass: 'variable'
	})
	
	brush.push({pattern: /\s\-+\w+/g, klass: 'option'})

	brush.push(Syntax.lib.perlStyleComment);
	brush.push(jQuery.extend(Syntax.lib.singleQuotedString, {allow: '*'}));
	brush.push(jQuery.extend(Syntax.lib.doubleQuotedString, {allow: '*'}));

	brush.push(Syntax.lib.webLink);
});

