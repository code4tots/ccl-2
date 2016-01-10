[ "$#" -eq 1 ] || { echo "usage: ccl <ccl_script>"; exit 1; }
root="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
java -cp "$root"/cls Desktop "$root" "$1"
