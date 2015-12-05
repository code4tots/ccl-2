mkdir -p cls && \
javac -Xlint src/*.java -d cls/ && \
java -cp cls Sanity src/"$1"
