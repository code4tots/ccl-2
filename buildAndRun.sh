[ "$#" -eq 1 ] || { echo "usage: xccl <ccl_script>"; exit 1; }
root="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
mkdir -p "$root"/cls && \
javac -Xlint -Xdiags:verbose -g "$root"/src/**/*.java -d "$root"/cls/ && \
java -cp "$root"/cls Desktop "$root" $1
