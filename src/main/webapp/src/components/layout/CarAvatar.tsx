import { Avatar } from '@mui/material';
import { baseUrl } from '@/utils/QueryClient';

const hues = [172, 28, 217, 271, 330, 145, 45, 195];

/** address of the picture, a new picture has a new id and so a new URL */
const pictureUrl = (carId: number, pictureId: number) =>
  `${baseUrl}/api/car/my/${carId}/picture?v=${pictureId}`;

/**
 * The own picture of a car, without one (or if it cannot be loaded) a stable
 * colored monogram
 */
export default function CarAvatar({
  id,
  name,
  pictureId,
  size = 32,
}: {
  id?: number;
  name?: string;
  pictureId?: number;
  size?: number;
}) {
  const hue = hues[(id ?? 0) % hues.length];
  const initials =
    (name ?? '?')
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join('') || '?';

  return (
    <Avatar
      variant="rounded"
      src={
        // the API sends null without a picture
        id != null && pictureId != null ? pictureUrl(id, pictureId) : undefined
      }
      alt={pictureId != null ? name : undefined}
      sx={{
        width: size,
        height: size,
        fontSize: size * 0.4,
        fontWeight: 700,
        borderRadius: size / 4,
        // darker text and a lighter ground than before: each hue reaches
        // 4.5:1, at 32% and 88% the teal, green and yellow did not
        color: `hsl(${hue} 70% 27%)`,
        backgroundColor: `hsl(${hue} 70% 90%)`,
        '.dark &': {
          color: `hsl(${hue} 80% 80%)`,
          backgroundColor: `hsl(${hue} 45% 22%)`,
        },
      }}
    >
      {initials}
    </Avatar>
  );
}
