import {
  Box,
  FormControlLabel,
  Radio,
  RadioGroup,
  Typography,
  useColorScheme,
} from '@mui/material';
import CheckRounded from '@mui/icons-material/CheckRounded';
import { useId } from 'react';
import { useTranslation } from 'react-i18next';
import { Accent, Account } from '@/generated';
import { chooseAccent, useChosenAccent } from '@/hooks/useAccent';
import { ACCENTS, fonts } from '@/theme';
import Section from '@/components/ui/Section';
import { visuallyHidden } from '@/components/ui/visuallyHidden';

type Mode = 'light' | 'dark' | 'system';

/** the colors of the two looks, fixed: a preview shows the other look too */
const looks = {
  light: {
    ground: '#f3f4f6',
    panel: '#ffffff',
    line: '#d9dce1',
    ink: '#2b2f36',
    muted: '#5b6472',
    figure: '#2b2f36',
  },
  dark: {
    ground: '#0e1620',
    panel: '#17212d',
    line: '#263445',
    ink: '#e6edf3',
    muted: '#93a1b2',
    figure: '#f5a524',
  },
} as const;

/** a small panel in a look: the total, a plate and a dial in the accent */
function Miniature({
  look,
  accent,
}: {
  look: 'light' | 'dark';
  accent: Accent;
}) {
  const colors = looks[look];
  const font = fonts[look];
  const { main } = ACCENTS[accent][look];
  return (
    <Box
      aria-hidden
      sx={{
        height: '100%',
        boxSizing: 'border-box',
        p: { xs: 0.75, sm: 1.5 },
        bgcolor: colors.ground,
        color: colors.ink,
        fontFamily: font.text,
      }}
    >
      <Box
        sx={{
          height: '100%',
          boxSizing: 'border-box',
          p: { xs: 1, sm: 1.5 },
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          bgcolor: colors.panel,
          borderRadius: look === 'dark' ? '14px' : '7px',
          border: look === 'dark' ? 'none' : `1px solid ${colors.line}`,
          ...(look === 'dark' && {
            background: `radial-gradient(120% 100% at 30% 0%, #1c2836 0%, ${colors.panel} 70%)`,
            boxShadow: `inset 0 0 0 1px ${colors.line}`,
          }),
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
          <Box
            component="span"
            sx={{
              px: 0.6,
              py: 0.1,
              fontFamily: font.display,
              fontWeight: 700,
              fontSize: 9,
              letterSpacing: '0.05em',
              whiteSpace: 'nowrap',
              border: `1px solid ${look === 'dark' ? colors.muted : colors.ink}`,
              borderRadius: '3px',
              bgcolor: look === 'dark' ? 'transparent' : '#fff',
            }}
          >
            M-OC 2019
          </Box>
          <Box
            component="span"
            sx={{
              display: { xs: 'none', sm: 'inline' },
              fontSize: 10,
              color: colors.muted,
            }}
          >
            Octavia
          </Box>
        </Box>
        <Box
          sx={{
            display: 'flex',
            alignItems: 'flex-end',
            justifyContent: 'space-between',
          }}
        >
          <Box>
            <Box
              sx={{
                fontFamily: font.display,
                fontWeight: 700,
                fontSize: { xs: 17, sm: 24 },
                lineHeight: 1,
                color: colors.figure,
              }}
            >
              €2,589
            </Box>
          </Box>
          <Box
            component="svg"
            viewBox="0 0 46 28"
            sx={{ width: { xs: 28, sm: 46 }, height: 'auto', flexShrink: 0 }}
          >
            <path
              d="M5 25a18 18 0 0136 0"
              fill="none"
              stroke={colors.line}
              strokeWidth="5"
              strokeLinecap="round"
            />
            <path
              d="M5 25a18 18 0 0124-17"
              fill="none"
              stroke={main}
              strokeWidth="5"
              strokeLinecap="round"
            />
          </Box>
        </Box>
        <Box sx={{ height: 6, borderRadius: 3, bgcolor: main, width: '55%' }} />
      </Box>
    </Box>
  );
}

/**
 * The look of the app: light (road signs), dark (instrument cluster) or the one
 * of the device, and the accent color. Both are shown at once; the look stays
 * on this device, the accent is saved with the account, without a connection
 * once it is back.
 */
export default function AppearanceSection({ account }: { account: Account }) {
  const { t } = useTranslation();
  const { mode, systemMode, setMode } = useColorScheme();
  const scheme = (mode === 'system' ? systemMode : mode) ?? 'light';
  const chosen = useChosenAccent(account);
  const name = useId();

  const swatch = (accent: Accent, checked: boolean) => {
    const { main, contrastText } = ACCENTS[accent][scheme];
    return (
      <Box
        sx={{
          width: 32,
          height: 32,
          borderRadius: '50%',
          bgcolor: main,
          color: contrastText,
          display: 'grid',
          placeItems: 'center',
          // a ring around the chosen one, in the color of the text
          boxShadow: checked
            ? (theme) =>
                `0 0 0 2px ${theme.vars.palette.background.paper}, 0 0 0 4px ${theme.vars.palette.text.primary}`
            : 'none',
        }}
      >
        {checked && <CheckRounded fontSize="small" />}
      </Box>
    );
  };

  const modes: { value: Mode; preview: React.ReactNode }[] = [
    { value: 'light', preview: <Miniature look="light" accent={chosen} /> },
    { value: 'dark', preview: <Miniature look="dark" accent={chosen} /> },
    {
      value: 'system',
      // both looks, split along the diagonal
      preview: (
        <Box sx={{ position: 'relative', height: '100%' }}>
          <Miniature look="light" accent={chosen} />
          <Box
            sx={{
              position: 'absolute',
              inset: 0,
              clipPath: 'polygon(100% 0, 100% 100%, 0 100%)',
            }}
          >
            <Miniature look="dark" accent={chosen} />
          </Box>
        </Box>
      ),
    },
  ];

  return (
    <Section
      id="look"
      title={t('settings.sections.look')}
      subtitle={t('settings.appearance.lookText')}
      sx={{ mb: 3 }}
    >
      <Box data-testid="appearance">
        <Box
          role="radiogroup"
          aria-label={t('settings.appearance.look')}
          sx={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
            gap: { xs: 1.25, sm: 2 },
          }}
        >
          {modes.map((option) => {
            const checked = (mode ?? 'system') === option.value;
            return (
              <Box
                component="label"
                key={option.value}
                sx={(theme) => ({
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 1,
                  cursor: 'pointer',
                  '& .preview': {
                    height: { xs: 84, sm: 112 },
                    borderRadius: '12px',
                    overflow: 'hidden',
                    outline: checked
                      ? `2px solid ${theme.vars.palette.text.primary}`
                      : `1px solid ${theme.vars.palette.divider}`,
                    outlineOffset: checked ? 2 : 0,
                  },
                  '&:has(input:focus-visible) .preview': {
                    outline: `3px solid ${theme.vars.palette.primary.main}`,
                    outlineOffset: 2,
                  },
                })}
              >
                <input
                  type="radio"
                  name={name}
                  value={option.value}
                  checked={checked}
                  onChange={() => setMode(option.value)}
                  style={visuallyHidden as React.CSSProperties}
                />
                <span className="preview">{option.preview}</span>
                <Box
                  component="span"
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 0.75,
                    fontWeight: checked ? 600 : 400,
                  }}
                >
                  {checked && <CheckRounded fontSize="small" aria-hidden />}
                  {t(`settings.appearance.modes.${option.value}`)}
                </Box>
              </Box>
            );
          })}
        </Box>

        <Typography id="accent-label" sx={{ fontWeight: 600, mt: 3, mb: 0.5 }}>
          {t('settings.appearance.accent')}
        </Typography>
        <Typography variant="body2" sx={{ color: 'text.secondary', mb: 1.5 }}>
          {t('settings.appearance.text')}
        </Typography>
        <RadioGroup
          row
          aria-labelledby="accent-label"
          value={chosen}
          onChange={(_, value) => chooseAccent(value as Accent)}
          sx={{ gap: 1 }}
        >
          {Object.values(Accent).map((accent) => (
            <FormControlLabel
              key={accent}
              value={accent}
              label={t(`settings.appearance.accents.${accent}`)}
              control={
                <Radio
                  icon={swatch(accent, false)}
                  checkedIcon={swatch(accent, true)}
                />
              }
              sx={{ mr: 1.5 }}
            />
          ))}
        </RadioGroup>
      </Box>
    </Section>
  );
}
