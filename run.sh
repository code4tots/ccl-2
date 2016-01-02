mkdir -p cls && \
javac -Xlint -Xdiags:verbose src/*.java src/*/*.java -d cls/ && \
java -cp cls Desktop $1
rm -rf cls
