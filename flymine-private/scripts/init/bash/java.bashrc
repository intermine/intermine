if [ "${JDK:-unset}" = "unset" ]; then
    JDK=/software/arch/jdk; export JDK
fi

JAVA_HOME=$JDK; export JAVA_HOME

#if [ "${JDBC_HOME:-unset}" = "unset" ]; then
#    JDBC_HOME=/software/noarch/jConnect; export JDBC_HOME
#fi

prepend PATH $JDK/bin
prepend MANPATH $JDK/man

#append CLASSPATH $JDBC_HOME/classes/jconn2.jar
#append CLASSPATH /software/noarch/ecs/ecs.jar
#append CLASSPATH /software/noarch/local/lib/java
append CLASSPATH /software/noarch/biojava/biojava.jar
append CLASSPATH /software/noarch/jaxb/lib/jaxb-rt-1.0-ea.jar
append CLASSPATH /software/noarch/mage/MAGE-2002-02-22.jar
append CLASSPATH .

append JIKESPATH $JDK/jre/lib/rt.jar

for jar in `find $JDK/jre/lib/ext -name "*.jar"`; do
    append JIKESPATH $jar
done 

append JIKESPATH $CLASSPATH

alias javac="jikes -depend"

if [ "${ANT_HOME:-unset}" = "unset" ]; then
    ANT_HOME=/software/noarch/ant; export ANT_HOME
fi
