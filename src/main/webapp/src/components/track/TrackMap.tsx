import { Box } from '@mui/material';
import {
  AttributionControl,
  ExpressionSpecification,
  GeoJSONSource,
  IControl,
  LngLatBounds,
  Map as MapLibreMap,
  MapLayerMouseEvent,
  Marker,
  NavigationControl,
  Popup,
  setWorkerUrl,
} from 'maplibre-gl';
import type { FeatureCollection } from 'geojson';
import 'maplibre-gl/dist/maplibre-gl.css';
// the worker as a file of this origin: the CSP allows no workers from blob:
import workerUrl from 'maplibre-gl/dist/maplibre-gl-worker.mjs?url';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Place, Track, TrackFrequency } from '@/generated';
import { useFormat } from '@/utils/format';
import { speedColor, trackBands, trackEnds } from './speedScale';
import { decodeTrack, MapPoint, nearestPoint } from './trackData';
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
  /** the places the car stops at often, as markers */
  places?: Place[];
  onPlaceClick?: (place: Place) => void;
  /** a drive of the track drawn wider underneath, e.g. hovered in the list */
  highlight?: { start: string; end: string };
  /** a click on the track, with the time of the point there */
  onTrackClick?: (ts: string) => void;
  height?: number;
  /** takes the height left in its column instead, e.g. in fullscreen */
  fill?: boolean;
  /** with it, a button under the zoom buttons switches to fullscreen */
  onFullscreen?: () => void;
  fullscreen?: boolean;
}

/**
 * The button for the fullscreen, with the icons of MapLibre like its other
 * controls
 */
class FullscreenToggle implements IControl {
  private readonly container = document.createElement('div');
  private readonly button = document.createElement('button');

  constructor(onClick: () => void) {
    this.container.className = 'maplibregl-ctrl maplibregl-ctrl-group';
    this.button.type = 'button';
    const icon = document.createElement('span');
    icon.className = 'maplibregl-ctrl-icon';
    icon.setAttribute('aria-hidden', 'true');
    this.button.append(icon);
    this.button.addEventListener('click', onClick);
    this.container.append(this.button);
  }

  onAdd() {
    return this.container;
  }

  onRemove() {
    this.container.remove();
  }

  show(fullscreen: boolean, label: string) {
    this.button.className = fullscreen
      ? 'maplibregl-ctrl-shrink'
      : 'maplibregl-ctrl-fullscreen';
    this.button.title = label;
    this.button.setAttribute('aria-label', label);
    this.button.setAttribute('aria-pressed', String(fullscreen));
  }
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
function showTrack(map: MapLibreMap, segments: MapPoint[][]) {
  (map.getSource('track') as GeoJSONSource).setData(trackBands(segments));
  (map.getSource('ends') as GeoJSONSource).setData(trackEnds(segments));
  const bounds = new LngLatBounds();
  segments.flat().forEach((point) => bounds.extend([point.lon, point.lat]));
  if (!bounds.isEmpty()) {
    map.fitBounds(bounds, { padding: 40, maxZoom: 16, duration: 0 });
  }
}

/** the points of the track from start to end, as lines */
function highlightLines(
  segments: MapPoint[][],
  highlight?: { start: string; end: string },
): FeatureCollection {
  if (!highlight) return EMPTY;
  const start = Date.parse(highlight.start);
  const end = Date.parse(highlight.end);
  return {
    type: 'FeatureCollection',
    features: segments
      .map((segment) =>
        segment.filter((point) => {
          const ts = Date.parse(point.ts);
          return ts >= start && ts <= end;
        }),
      )
      .filter((points) => points.length > 1)
      .map((points) => ({
        type: 'Feature',
        properties: {},
        geometry: {
          type: 'LineString',
          coordinates: points.map((point) => [point.lon, point.lat]),
        },
      })),
  };
}

/** of the white outline under a line, wider on each side */
const casingWidth = (
  width: number | ExpressionSpecification,
): ExpressionSpecification => ['+', width, 3];

/**
 * The line of the source with a white outline, and a wider invisible line, so
 * it is easy to hit with a finger: the layers <source>-casing, <source>-line
 * and <source>-hit
 */
function addLineLayers(
  map: MapLibreMap,
  source: string,
  color: ExpressionSpecification,
  width: number | ExpressionSpecification,
) {
  const layout = { 'line-cap': 'round', 'line-join': 'round' } as const;
  map.addLayer({
    id: `${source}-casing`,
    type: 'line',
    source,
    layout,
    paint: { 'line-color': '#ffffff', 'line-width': casingWidth(width) },
  });
  map.addLayer({
    id: `${source}-line`,
    type: 'line',
    source,
    layout,
    paint: { 'line-color': color, 'line-width': width },
  });
  map.addLayer({
    id: `${source}-hit`,
    type: 'line',
    source,
    paint: { 'line-color': '#000000', 'line-width': 20, 'line-opacity': 0 },
  });
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
    map.setPaintProperty('frequency-casing', 'line-width', casingWidth(width));
  }
  for (const layer of SPEED_LAYERS) {
    map.setLayoutProperty(layer, 'visibility', frequency ? 'none' : 'visible');
  }
  for (const layer of FREQUENCY_LAYERS) {
    map.setLayoutProperty(layer, 'visibility', frequency ? 'visible' : 'none');
  }
}

/**
 * A marker of a place: a circle with its visits, the name below it. A button,
 * so it is reached by the keyboard and named for screen readers.
 */
function placeElement(place: Place, label: string, onClick: () => void) {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'place-marker';
  button.title = label;
  button.setAttribute('aria-label', label);
  button.dataset.named = place.name ? 'true' : 'false';
  const circle = document.createElement('span');
  circle.className = 'place-marker-circle';
  circle.textContent = `${place.visits}`;
  button.append(circle);
  if (place.name) {
    const name = document.createElement('span');
    name.className = 'place-marker-name';
    name.textContent = place.name;
    button.append(name);
  }
  button.addEventListener('click', (event) => {
    // not a click on the map, which closes the popup
    event.stopPropagation();
    onClick();
  });
  return button;
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
  places,
  onPlaceClick,
  highlight,
  onTrackClick,
  height = 480,
  fill = false,
  onFullscreen,
  fullscreen = false,
}: TrackMapProps) {
  const { t } = useTranslation();
  const format = useFormat();
  const container = useRef<HTMLDivElement>(null);
  /** set once the style and the layers are loaded */
  const map = useRef<MapLibreMap>(null);
  // the server sends it compact
  const segments = useMemo(() => decodeTrack(track), [track]);
  const segmentsRef = useRef(segments);
  segmentsRef.current = segments;
  const frequencyRef = useRef(frequency);
  frequencyRef.current = frequency;
  // the handlers of the map are registered once, they read the current format
  const formatRef = useRef(format);
  formatRef.current = format;
  const tRef = useRef(t);
  tRef.current = t;
  const onPlaceClickRef = useRef(onPlaceClick);
  onPlaceClickRef.current = onPlaceClick;
  const onTrackClickRef = useRef(onTrackClick);
  onTrackClickRef.current = onTrackClick;
  const highlightRef = useRef(highlight);
  highlightRef.current = highlight;
  const markers = useRef<Marker[]>([]);
  const onFullscreenRef = useRef(onFullscreen);
  onFullscreenRef.current = onFullscreen;
  const fullscreenRef = useRef(fullscreen);
  fullscreenRef.current = fullscreen;
  /** set when the map has the button for the fullscreen */
  const fullscreenToggle = useRef<FullscreenToggle>(null);
  /** set when the map is ready, so the markers of the places follow */
  const [ready, setReady] = useState(false);

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
    if (onFullscreenRef.current) {
      fullscreenToggle.current = new FullscreenToggle(() =>
        onFullscreenRef.current?.(),
      );
      instance.addControl(fullscreenToggle.current, 'top-right');
      fullscreenToggle.current.show(
        fullscreenRef.current,
        tRef.current(
          fullscreenRef.current
            ? 'track.fullscreen.leave'
            : 'track.fullscreen.enter',
        ),
      );
    }
    const popup = new Popup({
      closeButton: false,
      closeOnClick: false,
      offset: 10,
    });

    instance.on('load', () => {
      instance.addSource('track', { type: 'geojson', data: EMPTY });
      instance.addSource('ends', { type: 'geojson', data: EMPTY });
      instance.addSource('frequency', { type: 'geojson', data: EMPTY });
      instance.addSource('highlight', { type: 'geojson', data: EMPTY });
      // underneath the speeds and the frequency
      instance.addLayer({
        id: 'highlight-line',
        type: 'line',
        source: 'highlight',
        layout: { 'line-cap': 'round', 'line-join': 'round' },
        paint: {
          'line-color': '#22d3ee',
          'line-width': 16,
          'line-opacity': 0.7,
        },
      });
      addLineLayers(instance, 'track', speedColor, 4);
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
      addLineLayers(
        instance,
        'frequency',
        frequencyColor(0),
        frequencyWidth(0),
      );
      map.current = instance;
      showTrack(instance, segmentsRef.current);
      showFrequency(instance, frequencyRef.current);
      (instance.getSource('highlight') as GeoJSONSource).setData(
        highlightLines(segmentsRef.current, highlightRef.current),
      );
      setReady(true);
    });

    /**
     * The point under the pointer: a line of the track has many, an end is
     * one
     */
    const pointAt = (event: MapLayerMouseEvent) => {
      const feature = event.features?.[0];
      if (!feature) return undefined;
      const properties = feature.properties as {
        seg?: number;
        from?: number;
        to?: number;
        ts?: string;
        speed?: number | null;
      };
      if (properties.seg === undefined) {
        return properties.ts
          ? { ts: properties.ts, speed: properties.speed ?? null }
          : undefined;
      }
      const segment = segmentsRef.current[properties.seg];
      return segment
        ? nearestPoint(
            segment,
            properties.from ?? 0,
            properties.to ?? segment.length - 1,
            event.lngLat.lng,
            event.lngLat.lat,
          )
        : undefined;
    };
    const show = (event: MapLayerMouseEvent) => {
      const point = pointAt(event);
      if (!point) return;
      const { ts, speed } = point;
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
    // picks the drive of the line
    instance.on('click', 'track-hit', (event) => {
      const point = pointAt(event);
      if (point) onTrackClickRef.current?.(point.ts);
    });
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
      fullscreenToggle.current = null;
      setReady(false);
    };
  }, [styleUrl]);

  useEffect(() => {
    const instance = map.current;
    if (!ready || !instance) return;
    markers.current = (places ?? []).map((place) => {
      const visits =
        place.visits === 0
          ? t('track.places.visits', { count: 0 })
          : t('track.places.visits', {
              count: place.visits,
              date: format.dateTime(place.lastVisit),
            });
      const label = t('track.places.label', {
        name: place.name ?? t('track.places.unnamed'),
        visits,
      });
      return new Marker({
        element: placeElement(place, label, () =>
          onPlaceClickRef.current?.(place),
        ),
        anchor: 'top',
        offset: [0, -14],
      })
        .setLngLat([place.lon, place.lat])
        .addTo(instance);
    });
    return () => {
      markers.current.forEach((marker) => marker.remove());
      markers.current = [];
    };
  }, [ready, places, t, format]);

  useEffect(() => {
    if (map.current) showTrack(map.current, segments);
  }, [segments]);

  useEffect(() => {
    fullscreenToggle.current?.show(
      fullscreen,
      t(fullscreen ? 'track.fullscreen.leave' : 'track.fullscreen.enter'),
    );
  }, [fullscreen, t]);

  useEffect(() => {
    if (map.current) showFrequency(map.current, frequency);
  }, [frequency]);

  useEffect(() => {
    (map.current?.getSource('highlight') as GeoJSONSource | undefined)?.setData(
      highlightLines(segments, highlight),
    );
  }, [segments, highlight]);

  return (
    <Box
      data-testid="track-map"
      sx={{
        ...(fill ? { flex: 1, minHeight: 320 } : { height }),
        borderRadius: 3,
        overflow: 'hidden',
        '& .place-marker': (theme) => ({
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 0.25,
          p: 0,
          border: 0,
          background: 'none',
          cursor: 'pointer',
          font: 'inherit',
          '&:focus-visible .place-marker-circle': {
            outline: `2px solid ${theme.vars.palette.text.primary}`,
            outlineOffset: 2,
          },
        }),
        '& .place-marker-circle': (theme) => ({
          ...theme.typography.caption,
          display: 'grid',
          placeItems: 'center',
          minWidth: 28,
          height: 28,
          px: 0.5,
          borderRadius: 14,
          border: '2px solid #ffffff',
          boxShadow: 2,
          fontWeight: 700,
          color: '#ffffff',
          bgcolor: 'text.secondary',
        }),
        '& .place-marker[data-named="true"] .place-marker-circle': {
          bgcolor: 'primary.main',
        },
        '& .place-marker-name': (theme) => ({
          ...theme.typography.caption,
          px: 0.75,
          borderRadius: 1,
          bgcolor: 'background.paper',
          color: 'text.primary',
          boxShadow: 1,
          whiteSpace: 'nowrap',
        }),
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
    >
      {/* MapLibre's own classes on it, React would replace them with a new sx */}
      <div ref={container} style={{ height: '100%' }} />
    </Box>
  );
}
