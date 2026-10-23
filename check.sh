#!/usr/bin/env bash
# Compiles all java files, runs every case in cases.txt, and compares the
# output (stdout, stderr, and exit status) with the original program's,
# saved in expected/.
#
# Usage: ./check.sh

cd "$(dirname "$0")" || exit 1

if ! javac *.java; then
    echo "Compile failed; nothing checked."
    exit 1
fi

passed=0
total=0
while read -r name args; do
    [ -z "$name" ] && continue
    total=$((total + 1))
    # $args is unquoted on purpose so each word becomes its own argument
    actual=$(java Gradebook $args < /dev/null 2>&1; echo "exit status: $?")
    if diff -u --label "expected" --label "yours" "expected/$name.txt" <(printf '%s\n' "$actual") > diff.tmp; then
        echo "PASS  $name"
        passed=$((passed + 1))
    else
        echo "FAIL  $name   (java Gradebook $args)"
        sed 's/^/      /' diff.tmp
    fi
done < cases.txt
rm -f diff.tmp

echo
echo "$passed of $total cases match the original."
[ "$passed" -eq "$total" ]
