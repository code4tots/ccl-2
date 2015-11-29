python src/ccl.py gensrc/ src/*.ccl && \
mkdir -p cls && \
javac gensrc/*.java src/*.java -d cls/ && \
java -cp cls CclModuleSample
