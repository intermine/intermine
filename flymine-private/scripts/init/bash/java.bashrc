if [ "${JDK:-unset}" = "unset" ]; then
    JDK=/software/arch/jdk; export JDK
fi

JAVA_HOME=$JDK; export JAVA_HOME

if [ "${JDBC_HOME:-unset}" = "unset" ]; then
    JDBC_HOME=/software/noarch/mm.mysql; export JDBC_HOME
fi


addjarstopath () {
    for jar in $2/*.jar; do
	append "$1" $jar
    done
}

prepend PATH $JDK/bin
prepend MANPATH $JDK/man

append CLASSPATH /software/noarch/junit/junit.jar



# Jikes needs the main java runtime libraries in the CLASSPATH
append JIKESPATH $JDK/jre/lib/rt.jar
addjarstopath JIKESPATH $JDK/jre/lib/ext
append JIKESPATH $CLASSPATH

if [ "${ANT_HOME:-unset}" = "unset" ]; then
    ANT_HOME=/software/noarch/ant; export ANT_HOME
fi

# Aliases
alias javac="jikes -depend"

addjarstopath ARGOUMLCLASSPATH /software/noarch/argouml/
alias argouml="java -classpath \$ARGOUMLCLASSPATH:\$CLASSPATH -jar /software/noarch/argouml/argouml.jar"
