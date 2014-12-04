# Configure standard memory size and server options.
# Sets webapp memory to between 256-1024m, and permgen to 64-128m
# Allows garbage collection of the permanent generation.
export JAVA_OPTS="-server \
    -Xms256m -Xmx1024m \
    -XX:+CMSClassUnloadingEnabled -XX:+CMSPermGenSweepingEnabled \
    -XX:NewSize=128m -XX:MaxNewSize=256m \
    -XX:PermSize=64m -XX:MaxPermSize=128m \
    -Djava.awt.headless=true \
    -Dfile.encoding=UTF-8 \
    -Dorg.apache.el.parser.SKIP_IDENTIFIER_CHECK=true"

