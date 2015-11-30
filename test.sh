python src/ccl.py gensrc/ src/*.ccl && \
mkdir -p cls && \
javac -Xlint gensrc/*.java src/*.java -d cls/ && \
java -cp cls CclModuleTest
