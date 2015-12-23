mkdir -p cls && \
javac -Xlint src/*.java -d cls/ && \
java -cp cls DesktopSanity $1 src/*.ccl
