PORT=$1
SCRIPT=$2
arduino-cli upload -p $PORT -b arduino:avr:uno $SCRIPT
