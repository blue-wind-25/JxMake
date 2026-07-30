#!/bin/sh
#
# Common Node.js runtime environment.
#

NODE_HOME=/opt/node-v24.14.0-linux-x64

export LD_LIBRARY_PATH=/opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib:/opt/isl-0.16.1/lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}

export NODE_PATH="$NODE_HOME/lib/node_modules:$HOME/mynpm/node_modules${NODE_PATH:+:$NODE_PATH}"

export PATH="$NODE_HOME/bin:$HOME/mynpm/bin:$PATH"

NODE="$NODE_HOME/bin/node"
NPM="$NODE_HOME/bin/npm"
export NODE NPM
