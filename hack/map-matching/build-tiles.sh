#!/usr/bin/env bash
# Builds the routing tiles of Valhalla for the map matching on this machine and packs them into an image, which the
# chart map-matching mounts as its tiles (tiles.volume): the server only serves them, without a download or a build.
# See docs/GPS_TRACKER.md#map-matching.
#
#   hack/map-matching/build-tiles.sh <image> [extract URL ...]
#
#   hack/map-matching/build-tiles.sh git.example.com/me/car-ledger-tiles:2026-11
#   PUSH=true hack/map-matching/build-tiles.sh git.example.com/me/car-ledger-tiles:2026-11 \
#     https://download.geofabrik.de/europe/germany/bayern-latest.osm.pbf
#
# Without extracts: Germany, Austria and Switzerland of Geofabrik, like the chart. Environment:
#   WORK_DIR  where the extracts and the tiles are built, about 4× the size of the extracts, deleted afterwards
#             (./map-matching-tiles)
#   THREADS   threads of the build (all cores)
#   MEMORY    memory limit of the build, e.g. 16g (none)
#   PUSH      true: push the image afterwards (log in to the registry first, e.g. docker login git.example.com)
#   ENGINE    docker or podman (the one found)
set -euo pipefail

image=${1:?"usage: $0 <image> [extract URL ...]"}
shift
extracts=("$@")
if [ ${#extracts[@]} -eq 0 ]; then
  extracts=(
    https://download.geofabrik.de/europe/germany-latest.osm.pbf
    https://download.geofabrik.de/europe/austria-latest.osm.pbf
    https://download.geofabrik.de/europe/switzerland-latest.osm.pbf
  )
fi

here=$(cd "$(dirname "$0")" && pwd)
# the version of Valhalla the chart serves the tiles with
version=$(sed -n 's/^appVersion: *"\(.*\)"/\1/p' "$here/../../helm/map-matching/Chart.yaml")
valhalla="ghcr.io/valhalla/valhalla-scripted:$version"
engine=${ENGINE:-$(command -v docker >/dev/null && echo docker || echo podman)}
work=$(mkdir -p "${WORK_DIR:-map-matching-tiles}" && cd "${WORK_DIR:-map-matching-tiles}" && pwd)
build="$work/build"
package="$work/image"

# the files of the build belong to the user of the image (59999), only it deletes them
clean() {
  if [ -d "$build" ]; then
    "$engine" run --rm --user 59999:1001 -v "$build:/custom_files" --entrypoint sh "$valhalla" \
      -c 'rm -rf /custom_files/* /custom_files/.[!.]*'
    rmdir "$build"
  fi
}

# a new build every time: newer extracts under the same name would keep the old tiles
clean
rm -rf "$package"
mkdir -p "$build" "$package"
chmod 777 "$build"

echo "Building the tiles of ${extracts[*]} with Valhalla $version in $build"
"$engine" run --rm \
  --user 59999:1001 --read-only --tmpfs /tmp:rw,exec,mode=1777 --cap-drop ALL --security-opt no-new-privileges \
  ${MEMORY:+--memory "$MEMORY" --memory-swap "$MEMORY"} \
  -v "$build:/custom_files" \
  -e tile_urls="${extracts[*]}" \
  -e server_threads="${THREADS:-$(nproc)}" \
  -e use_tiles_ignore_pbf=False \
  -e build_tar=True \
  -e serve_tiles=False \
  "$valhalla"

if [ ! -s "$build/valhalla_tiles.tar" ]; then
  echo "The build has no valhalla_tiles.tar, see its log above" >&2
  exit 1
fi

# the config of the service: the tiles of the tarball under /tiles, where the chart mounts the image; nothing else
# (no traffic, transit or elevation, the admins and time zones are part of the tiles)
"$engine" run --rm --entrypoint valhalla_build_config "$valhalla" \
  --mjolnir-tile-extract /tiles/valhalla_tiles.tar \
  --mjolnir-tile-dir /tiles/valhalla_tiles \
  --mjolnir-traffic-extract "" \
  --mjolnir-transit-dir "" \
  --mjolnir-admin "" \
  --mjolnir-timezone "" \
  --additional-data-elevation "" \
  >"$package/valhalla.json"
cp "$build/valhalla_tiles.tar" "$package/"
chmod 644 "$package"/*

cat >"$package/Containerfile" <<EOF
# only files, mounted by the chart map-matching as an image volume
FROM scratch
COPY valhalla_tiles.tar valhalla.json /
LABEL org.opencontainers.image.title="CarLedger map matching tiles" \\
      org.opencontainers.image.description="Valhalla $version tiles of ${extracts[*]}" \\
      org.opencontainers.image.licenses="ODbL-1.0" \\
      org.opencontainers.image.created="$(date -u +%Y-%m-%dT%H:%M:%SZ)" \\
      de.codeflowwizardry.carledger.valhalla="$version"
EOF
"$engine" build -t "$image" -f "$package/Containerfile" "$package"
echo "Image $image: $(du -h "$package/valhalla_tiles.tar" | cut -f1) of tiles"

if [ "${PUSH:-false}" = true ]; then
  "$engine" push "$image"
fi
# the extracts, the files of the build and the copy of the tiles aren't needed anymore, the image has them
clean
rm -rf "$package"
