import { Avatar, Box } from '@mui/material';
import { baseUrl } from '@/utils/QueryClient';
import { displayFont } from '@/theme';

const hues = [172, 28, 217, 271, 330, 145, 45, 195];

/** address of the picture, a new picture has a new id and so a new URL */
const pictureUrl = (carId: number, pictureId: number) =>
  `${baseUrl}/api/car/my/${carId}/picture?v=${pictureId}`;

const initialsOf = (name?: string) =>
  (name ?? '?')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('') || '?';

export interface CarPlateProps {
  /** the stable color of the car, the band on the left */
  id?: number;
  /** the text of the plate */
  text: string;
  /** the height in px */
  size?: number;
  /** the initials instead of a registration: decoration next to the name */
  decorative?: boolean;
}

/**
 * A number plate: the registration of a car, or its initials when it has
 * none. The band on the left keeps the color of the car, so several cars stay
 * apart.
 */
export function CarPlate({ id, text, size = 28, decorative }: CarPlateProps) {
  const hue = hues[(id ?? 0) % hues.length];
  return (
    <Box
      component="span"
      aria-hidden={decorative || undefined}
      sx={(theme) => ({
        display: 'inline-flex',
        alignItems: 'stretch',
        flexShrink: 0,
        boxSizing: 'border-box',
        height: size,
        maxWidth: size * 4.5,
        overflow: 'hidden',
        borderRadius: `${Math.max(3, size / 7)}px`,
        border: `${size >= 40 ? 2 : 1.5}px solid ${theme.vars.palette.text.primary}`,
        bgcolor: 'background.paper',
        color: 'text.primary',
        fontFamily: displayFont,
        fontWeight: 700,
        fontSize: Math.max(10, Math.round(size * 0.42)),
        letterSpacing: '0.05em',
        lineHeight: 1,
        whiteSpace: 'nowrap',
        // dark: the plate in the light of the cluster, an outline on the panel
        ...theme.applyStyles('dark', {
          bgcolor: 'transparent',
          borderColor: theme.vars.palette.text.secondary,
        }),
      })}
    >
      <Box
        component="span"
        sx={(theme) => ({
          width: Math.max(3, Math.round(size / 7)),
          flexShrink: 0,
          bgcolor: `hsl(${hue} 70% 40%)`,
          ...theme.applyStyles('dark', { bgcolor: `hsl(${hue} 70% 60%)` }),
        })}
      />
      <Box
        component="span"
        sx={{
          display: 'flex',
          alignItems: 'center',
          minWidth: 0,
          px: `${Math.max(4, Math.round(size / 4))}px`,
          // the figures of the display faces sit a little high
          pt: '0.1em',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
        }}
      >
        {text.toUpperCase()}
      </Box>
    </Box>
  );
}

/**
 * The own picture of a car; without one (or if it cannot be loaded) its number
 * plate, or its initials on a plate when it has no registration
 */
export default function CarAvatar({
  id,
  name,
  licensePlate,
  pictureId,
  size = 32,
}: {
  id?: number;
  name?: string;
  licensePlate?: string;
  pictureId?: number;
  size?: number;
}) {
  // the API sends null without a picture
  if (id != null && pictureId != null) {
    return (
      <Avatar
        variant="rounded"
        src={pictureUrl(id, pictureId)}
        alt={name}
        sx={{
          width: size,
          height: size,
          borderRadius: size / 4,
          fontSize: size * 0.4,
          fontWeight: 700,
        }}
      >
        {/* a picture that cannot be loaded */}
        {initialsOf(name)}
      </Avatar>
    );
  }
  return (
    <CarPlate
      id={id}
      text={licensePlate || initialsOf(name)}
      size={size}
      decorative={!licensePlate}
    />
  );
}
