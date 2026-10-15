#!/usr/bin/env bash
# The charts keycloak, authentik and n8n are published by a release in the version of their Chart.yaml, a version
# only once (see .github/workflows/build-backend.yml). A chart that changed since the last release needs a new
# version, otherwise the change would not be published.
set -euo pipefail
cd "$(dirname "$0")/.."

# the release before this commit, also on the build of a tag
if ! last=$(git describe --tags --abbrev=0 HEAD^ 2>/dev/null); then
  echo "No release before this commit, nothing to compare"
  exit 0
fi

failed=0
for chart in keycloak authentik n8n; do
  current=$(sed -n 's/^version: *//p' "helm/$chart/Chart.yaml")
  released=$(git show "$last:helm/$chart/Chart.yaml" 2>/dev/null | sed -n 's/^version: *//p' || true)
  if [ -z "$released" ]; then
    echo "helm/$chart: new since $last, version $current"
  elif [ "$current" != "$released" ]; then
    echo "helm/$chart: $released in $last, now $current"
  elif ! git diff --quiet "$last" -- "helm/$chart"; then
    echo "::error file=helm/$chart/Chart.yaml::helm/$chart changed since $last, raise its version $current"
    failed=1
  else
    echo "helm/$chart: unchanged since $last, version $current"
  fi
done
exit "$failed"
