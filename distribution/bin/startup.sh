PRG="$0"
PRG_DIR=$(dirname "$PRG")
[ -z "$STRATO_HOME" ] && STRATO_HOME=$(cd "$PRG_DIR/.." > /dev/null || exit 1; pwd)

CONFIG_DIR="$STRATO_HOME/config/application.yaml"

JVM_OPTIONS="-Xms256M -Xmx8192M"

java -jar "$STRATO_HOME"/jars/standalone-server.jar "$JVM_OPTIONS" --spring.config.location="$CONFIG_DIR"