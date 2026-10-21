import { Box } from '@mui/material';
import {
  AttributionControl,
  GeoJSONSource,
  LngLatBounds,
  Map as MapLibreMap,
  MapLayerMouseEvent,
  NavigationControl,
  Popup,
  setWorkerUrl,
} from 'maplibre-gl';
import type { FeatureCollection } from 'geojson';
import 'maplibre-gl/dist/maplibre-gl.css';
// the worker as a file of this origin: the CSP allows no workers from blob:
import workerUrl from 'maplibre-gl/dist/maplibre-gl-worker.mjs?url';
import { useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Track } from '@/generated';
import { useFormat } from '@/utils/format';
import { speedColor, trackEnds, trackPieces } from './speedScale';

setWorkerUrl(workerUrl);

export interface TrackMapProps {
  styleUrl: string;
  track: Track;
  height?: number;
}

const EMPTY: FeatureCollection = {
  type: 'FeatureCollection',
  features: [],
};

/** draws the track and zooms to it */
function showTrack(map: MapLibreMap, track: Track) {
  (map.getSource('track') as GeoJSONSource).setData(
    trackPieces(track.segments),
  );
  (map.getSource('ends') as GeoJSONSource).setData(trackEnds(track.segments));
  const bounds = new LngLatBounds();
  track.segments
    .flat()
    .forEach((point) => bounds.extend([point.lon, point.lat]));
  if (!bounds.isEmpty()) {
    map.fitBounds(bounds, { padding: 40, maxZoom: 16, duration: 0 });
  }
}

/**
 * The segments of a track on the map, each piece colored by its speed, with
 * the start and the end of every segment. Hovering or tapping the track shows
 * the time and the speed there.
 */
export default function TrackMap({
  styleUrl,
  track,
  height = 480,
}: TrackMapProps) {
  const { t } = useTranslation();
  const format = useFormat();
  const container = useRef<HTMLDivElement>(null);
  /** set once the style and the layers are loaded */
  const map = useRef<MapLibreMap>(null);
  const trackRef = useRef(track);
  trackRef.current = track;
  // the handlers of the map are registered once, they read the current format
  const formatRef = useRef(format);
  formatRef.current = format;
  const tRef = useRef(t);
  tRef.current = t;

  useEffect(() => {
    const instance = new MapLibreMap({
      container: container.current!,
      style: styleUrl,
      attributionControl: false,
      center: [10.4, 51.1],
      zoom: 5,
    });
    instance.addControl(new AttributionControl({ compact: true }));
    instance.addControl(
      new NavigationControl({ showCompass: false }),
      'top-right',
    );
    const popup = new Popup({
      closeButton: false,
      closeOnClick: false,
      offset: 10,
    });

    instance.on('load', () => {
      instance.addSource('track', { type: 'geojson', data: EMPTY });
      instance.addSource('ends', { type: 'geojson', data: EMPTY });
      instance.addLayer({
        id: 'track-casing',
        type: 'line',
        source: 'track',
        layout: { 'line-cap': 'round', 'line-join': 'round' },
        paint: { 'line-color': '#ffffff', 'line-width': 7 },
      });
      instance.addLayer({
        id: 'track-line',
        type: 'line',
        source: 'track',
        layout: { 'line-cap': 'round', 'line-join': 'round' },
        paint: { 'line-color': speedColor, 'line-width': 4 },
      });
      // wider than the line, so it is easy to hit with a finger
      instance.addLayer({
        id: 'track-hit',
        type: 'line',
        source: 'track',
        paint: { 'line-color': '#000000', 'line-width': 20, 'line-opacity': 0 },
      });
      instance.addLayer({
        id: 'track-ends',
        type: 'circle',
        source: 'ends',
        paint: {
          'circle-radius': 6,
          'circle-color': [
            'match',
            ['get', 'end'],
            'start',
            '#ffffff',
            '#0d366b',
          ],
          'circle-stroke-width': 2,
          'circle-stroke-color': [
            'match',
            ['get', 'end'],
            'start',
            '#0d366b',
            '#ffffff',
          ],
        },
      });
      map.current = instance;
      showTrack(instance, trackRef.current);
    });

    const show = (event: MapLayerMouseEvent) => {
      const feature = event.features?.[0];
      if (!feature) return;
      const { ts, speed } = feature.properties as {
        ts: string;
        speed: number | null;
      };
      const { units } = formatRef.current;
      const speedText =
        typeof speed === 'number'
          ? `${Math.round(units.distance.fromKm(speed))} ${units.distance.label}/h`
          : tRef.current('track.noSpeed');
      popup
        .setLngLat(event.lngLat)
        .setText(`${formatRef.current.dateTime(ts)} · ${speedText}`)
        .addTo(instance);
    };
    for (const layer of ['track-hit', 'track-ends']) {
      instance.on('mousemove', layer, (event) => {
        instance.getCanvas().style.cursor = 'pointer';
        show(event);
      });
      instance.on('mouseleave', layer, () => {
        instance.getCanvas().style.cursor = '';
        popup.remove();
      });
      // touch: tap the track, tap elsewhere to close
      instance.on('click', layer, show);
    }
    instance.on('click', (event) => {
      const hit = instance.queryRenderedFeatures(event.point, {
        layers: ['track-hit', 'track-ends'],
      });
      if (!hit.length) popup.remove();
    });

    return () => {
      popup.remove();
      instance.remove();
      map.current = null;
    };
  }, [styleUrl]);

  useEffect(() => {
    if (map.current) showTrack(map.current, track);
  }, [track]);

  return (
    <Box
      ref={container}
      data-testid="track-map"
      sx={{
        height,
        borderRadius: 3,
        overflow: 'hidden',
        // the popup in the colors of the app
        '& .maplibregl-popup-content': (theme) => ({
          ...theme.typography.body2,
          bgcolor: 'background.paper',
          color: 'text.primary',
          whiteSpace: 'nowrap',
          py: 0.75,
          px: 1.25,
        }),
      }}
    />
  );
}
