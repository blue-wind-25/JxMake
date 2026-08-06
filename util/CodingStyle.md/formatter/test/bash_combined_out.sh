#
# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT
#

if foo; then
echo hi
fi

find . | grep foo | sort

foo() {
    echo hi
}

case "$x" in
foo)
    echo foo
    ;;
bar)
    echo bar
    ;;
esac

echo "${arr[$((i + 1))]}"

# safety: pipe/arith inside string and comment must stay untouched
echo "a|b|c"
# find .|grep foo
echo $((1 + 2))

# safety: heredoc body left byte-identical (including unspaced pipe)
cat <<EOF
find .|grep foo
if bar
then
  echo x
fi
EOF

# safety: command substitution left opaque
echo $(echo a|b)
