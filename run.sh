mkdir -p cls && \
javac -Xlint src/*.java -d cls/ && \
java -cp cls SimpleDesktop $1 src/*.ccl
