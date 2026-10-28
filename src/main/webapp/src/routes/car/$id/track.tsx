import { createFileRoute } from '@tanstack/react-router';
import { TrackSearch } from '@/pages/car/TrackPage';

const isApiDate = (value: unknown): value is string =>
  typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value);

/** the start of a drive, as the API lists it */
const isTime = (value: unknown): value is string =>
  typeof value === 'string' &&
  /^\d{4}-\d{2}-\d{2}T[\d:.]+(Z|[+-][\d:]+)$/.test(value);

/** the period and the selected drives, so a reload or a link keeps them */
export const Route = createFileRoute('/car/$id/track')({
  validateSearch: (search: Record<string, unknown>): TrackSearch => {
    const drives = Array.isArray(search.drives)
      ? search.drives.filter(isTime).slice(0, 200)
      : [];
    return {
      from: isApiDate(search.from) ? search.from : undefined,
      to: isApiDate(search.to) ? search.to : undefined,
      drives: drives.length ? drives : undefined,
    };
  },
});
