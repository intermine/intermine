if [ "${JDK:-unset}" = "unset" ]; then
    JDK=/software/arch/jdk; export JDK
fi

JAVA_HOME=$JDK; export JAVA_HOME

if [ "${JDBC_HOME:-unset}" = "unset" ]; then
    JDBC_HOME=/software/noarch/mm.mysql; export JDBC_HOME
fi


addjarstopath () {
    for jar in `find $2 -follow -maxdepth 1 -name "*.jar"`; do
	append "$1" $jar
    done
}

prepend PATH $JDK/bin
prepend MANPATH $JDK/man


addjarstopath CLASSPATH $JDBC_HOME
addjarstopath CLASSPATH /software/noarch/biojava/
addjarstopath CLASSPATH /software/noarch/jaxb/lib/
addjarstopath CLASSPATH /software/noarch/mage/
addjarstopath CLASSPATH /software/noarch/ojb/
addjarstopath CLASSPATH /software/noarch/junit/
addjarstopath CLASSPATH /software/noarch/castor/
addjarstopath CLASSPATH /software/noarch/argouml/
addjarstopath CLASSPATH /usr/share/java/

append CLASSPATH .

# Jikes needs the main java runtime libraries in the CLASSPATH
append JIKESPATH $JDK/jre/lib/rt.jar
addjarstopath JIKESPATH $JDK/jre/lib/ext
append JIKESPATH $CLASSPATH

if [ "${ANT_HOME:-unset}" = "unset" ]; then
    ANT_HOME=/software/noarch/ant; export ANT_HOME
fi

# Aliases
alias javac="jikes -depend"
alias argouml="java -jar /software/noarch/argouml/argouml.jar"
