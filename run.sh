mkdir -p cls && \
javac -Xlint -Xdiags:verbose src/*/*.java -d cls/ && \
java -cp cls Desktop $1
