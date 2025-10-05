#!/bin/bash
# Exit if any command fails
set -e

# Delete facts.n3 if it exists
if [ -f "facts.n3" ]; then
  rm -f facts.n3
fi

# Run CLIPS with start.clp
clips -f2 ./start.clp
