# Add configuration for connecting with JMX tools
export JAVA_OPTS="$JAVA_OPTS \
    -Dcatalina.ext.dirs=$CATALINA_HOME/shared/lib:$CATALINA_HOME/common/lib \
    -Dcom.sun.management.jmxremote=true \
    -Dcom.sun.management.jmxremote.port=9090 \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Dcom.sun.management.jmxremote.authenticate=true \
    -Dcom.sun.management.jmxremote.password.file=$HOME/.jmx/remote.password \
    -Dcom.sun.management.jmxremote.access.file=$HOME/.jmx/remote.access \
    -Djava.rmi.server.hostname=localhost"

