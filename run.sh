mkdir -p cls && \
javac -Xlint -Xdiags:verbose -g src/**/*.java -d cls/ && \
java -cp cls Desktop $1
