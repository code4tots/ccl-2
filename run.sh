mkdir -p cls && \
javac -Xlint src/*.java -d cls/ && \
java -cp cls Desktop $1
