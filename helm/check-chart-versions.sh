#!/usr/bin/env bash
# The charts keycloak and authentik are published by a release in the version of their Chart.yaml, a version
# only once (see .github/workflows/build-backend.yml). A chart that changed since the last release needs a new
# version, otherwise the change would not be published.
set -euo pipefail
cd "$(dirname "$0")/.."

# The newest release without this commit, also on the build of a tag (whose own tag is left out). Not git describe:
# a release is tagged on main, so develop does not contain its tag.
last=$(git tag --list --sort=-v:refname | grep -E '^[0-9]+\.[0-9]+\.[0-9]+$' \
  | grep -vxF -f <(git tag --points-at HEAD; echo) | head -n 1 || true)
if [ -z "$last" ]; then
  echo "No release before this commit, nothing to compare"
  exit 0
fi

failed=0
for chart in keycloak authentik; do
  current=$(sed -n 's/^version: *//p' "helm/$chart/Chart.yaml")
  released=$(git show "$last:helm/$chart/Chart.yaml" 2>/dev/null | sed -n 's/^version: *//p' || true)
  if [ -z "$released" ]; then
    echo "helm/$chart: new since $last, version $current"
  elif [ "$current" != "$released" ]; then
    echo "helm/$chart: $released in $last, now $current"
  # the tests and .helmignore (which keeps them out) are not part of the published chart
  elif ! git diff --quiet "$last" -- "helm/$chart" ":!helm/$chart/tests" ":!helm/$chart/.helmignore"; then
    echo "::error file=helm/$chart/Chart.yaml::helm/$chart changed since $last, raise its version $current"
    failed=1
  else
    echo "helm/$chart: unchanged since $last, version $current"
  fi
done
exit "$failed"
