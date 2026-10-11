import { Avatar } from '@mui/material';

const hues = [172, 28, 217, 271, 330, 145, 45, 195];

/**
 * Stable colored monogram for a car
 */
export default function CarAvatar({
  id,
  name,
  size = 32,
}: {
  id?: number;
  name?: string;
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
      sx={{
        width: size,
        height: size,
        fontSize: size * 0.4,
        fontWeight: 700,
        borderRadius: size / 4,
        color: `hsl(${hue} 70% 32%)`,
        backgroundColor: `hsl(${hue} 70% 88%)`,
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
