import { Box } from '@mui/material';

/**
 * The colors of the HU stickers, a cycle of six years: 2021 green, 2022
 * orange, 2023 blue, 2024 yellow, 2025 brown, 2026 pink
 */
const COLORS = [
  '#3c9a48',
  '#f08a24',
  '#2c6cc4',
  '#f2cf2a',
  '#8b5a36',
  '#f19bbf',
];

/** the color of the sticker of the year */
const stickerColor = (year: number) => COLORS[(((year - 2021) % 6) + 6) % 6];

/**
 * The HU sticker of the number plate: the color of the year, its last two
 * digits in the middle and the months around them, the month it is due at
 * the top
 *
 * @param month 1 to 12
 */
export default function HuSticker({
  year,
  month,
  label,
  size = 132,
}: {
  year: number;
  month: number;
  /** for screen readers, e.g. "HU due in November 2026" */
  label: string;
  size?: number;
}) {
  const r = 50;
  // the due month at the top, the others clockwise after it
  const months = Array.from({ length: 12 }, (_, index) => {
    const value = ((month - 1 + index) % 12) + 1;
    const angle = (index / 12) * 2 * Math.PI;
    return {
      value,
      x: 60 + Math.sin(angle) * 38,
      y: 60 - Math.cos(angle) * 38,
    };
  });

  return (
    <Box
      component="svg"
      role="img"
      aria-label={label}
      viewBox="0 0 120 120"
      sx={{
        width: size,
        height: size,
        flexShrink: 0,
        // like a sticker on the plate, a little lifted
        filter: 'drop-shadow(0 2px 4px rgba(0 0 0 / 0.25))',
      }}
    >
      <circle cx="60" cy="60" r={r + 8} fill={stickerColor(year)} />
      <circle
        cx="60"
        cy="60"
        r={r + 8}
        fill="none"
        stroke="rgba(0 0 0 / 0.35)"
        strokeWidth="1"
      />
      {/* the mark above the due month */}
      <path d="M60 4 l5 8 h-10 z" fill="#111" />
      {months.map(({ value, x, y }) => (
        <text
          key={value}
          x={x}
          y={y}
          textAnchor="middle"
          dominantBaseline="central"
          fontSize={value === month ? 13 : 10}
          fontWeight={value === month ? 800 : 600}
          fill="#111"
          fontFamily="Arial, Helvetica, sans-serif"
        >
          {value}
        </text>
      ))}
      <circle cx="60" cy="60" r="21" fill="rgba(255 255 255 / 0.35)" />
      <text
        x="60"
        y="61"
        textAnchor="middle"
        dominantBaseline="central"
        fontSize="22"
        fontWeight="800"
        fill="#111"
        fontFamily="Arial, Helvetica, sans-serif"
      >
        {String(year % 100).padStart(2, '0')}
      </text>
    </Box>
  );
}
