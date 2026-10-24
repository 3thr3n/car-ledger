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
import { Track, TrackFrequency } from '@/generated';
import { useFormat } from '@/utils/format';
import { speedColor, trackEnds, trackPieces } from './speedScale';
import {
  frequencyColor,
  frequencyLines,
  frequencyWidth,
} from './frequencyScale';

setWorkerUrl(workerUrl);

export interface TrackMapProps {
  styleUrl: string;
  track: Track;
  /** shown instead of the speeds of the track */
  frequency?: TrackFrequency;
  height?: number;
}

const SPEED_LAYERS = ['track-casing', 'track-line', 'track-hit', 'track-ends'];
const FREQUENCY_LAYERS = [
  'frequency-casing',
  'frequency-line',
  'frequency-hit',
];

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

/** draws the passes instead of the speeds, or the speeds again without */
function showFrequency(map: MapLibreMap, frequency?: TrackFrequency) {
  (map.getSource('frequency') as GeoJSONSource).setData(
    frequency ? frequencyLines(frequency) : EMPTY,
  );
  if (frequency) {
    const color = frequencyColor(frequency.maxPasses);
    const width = frequencyWidth(frequency.maxPasses);
    map.setPaintProperty('frequency-line', 'line-color', color);
    map.setPaintProperty('frequency-line', 'line-width', width);
    map.setPaintProperty('frequency-casing', 'line-width', ['+', width, 3]);
  }
  for (const layer of SPEED_LAYERS) {
    map.setLayoutProperty(layer, 'visibility', frequency ? 'none' : 'visible');
  }
  for (const layer of FREQUENCY_LAYERS) {
    map.setLayoutProperty(layer, 'visibility', frequency ? 'visible' : 'none');
  }
}

/**
 * The segments of a track on the map, each piece colored by its speed, with
 * the start and the end of every segment. Hovering or tapping the track shows
 * the time and the speed there. With the frequency, the track is colored by
 * how often the car passed it instead, and shows that.
 */
export default function TrackMap({
  styleUrl,
  track,
  frequency,
  height = 480,
}: TrackMapProps) {
  const { t } = useTranslation();
  const format = useFormat();
  const container = useRef<HTMLDivElement>(null);
  /** set once the style and the layers are loaded */
  const map = useRef<MapLibreMap>(null);
  const trackRef = useRef(track);
  trackRef.current = track;
  const frequencyRef = useRef(frequency);
  frequencyRef.current = frequency;
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
      instance.addSource('frequency', { type: 'geojson', data: EMPTY });
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
            '#0d0887',
          ],
          'circle-stroke-width': 2,
          'circle-stroke-color': [
            'match',
            ['get', 'end'],
            'start',
            '#0d0887',
            '#ffffff',
          ],
        },
      });
      instance.addLayer({
        id: 'frequency-casing',
        type: 'line',
        source: 'frequency',
        layout: { 'line-cap': 'round', 'line-join': 'round' },
        paint: { 'line-color': '#ffffff' },
      });
      instance.addLayer({
        id: 'frequency-line',
        type: 'line',
        source: 'frequency',
        layout: { 'line-cap': 'round', 'line-join': 'round' },
      });
      instance.addLayer({
        id: 'frequency-hit',
        type: 'line',
        source: 'frequency',
        paint: { 'line-color': '#000000', 'line-width': 20, 'line-opacity': 0 },
      });
      map.current = instance;
      showTrack(instance, trackRef.current);
      showFrequency(instance, frequencyRef.current);
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
    const showPasses = (event: MapLayerMouseEvent) => {
      // the most frequent is drawn on top
      const feature = event.features?.reduce((top, other) =>
        other.properties.passes > top.properties.passes ? other : top,
      );
      if (!feature) return;
      const { passes, last } = feature.properties as {
        passes: number;
        last: string;
      };
      popup
        .setLngLat(event.lngLat)
        .setText(
          tRef.current('track.passes', {
            count: passes,
            date: formatRef.current.dateTime(last),
          }),
        )
        .addTo(instance);
    };
    for (const [layer, handler] of [
      ['track-hit', show],
      ['track-ends', show],
      ['frequency-hit', showPasses],
    ] as const) {
      instance.on('mousemove', layer, (event) => {
        instance.getCanvas().style.cursor = 'pointer';
        handler(event);
      });
      instance.on('mouseleave', layer, () => {
        instance.getCanvas().style.cursor = '';
        popup.remove();
      });
      // touch: tap the track, tap elsewhere to close
      instance.on('click', layer, handler);
    }
    instance.on('click', (event) => {
      const hit = instance.queryRenderedFeatures(event.point, {
        layers: ['track-hit', 'track-ends', 'frequency-hit'],
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

  useEffect(() => {
    if (map.current) showFrequency(map.current, frequency);
  }, [frequency]);

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
