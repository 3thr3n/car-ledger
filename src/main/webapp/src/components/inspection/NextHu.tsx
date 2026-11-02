import { Box, Typography } from '@mui/material';
import dayjs from 'dayjs';
import { useTranslation } from 'react-i18next';
import { useFormat } from '@/utils/format';
import { CLUSTER_AMBER, displayFont } from '@/theme';
import HuSticker from './HuSticker';

/**
 * The next HU as its sticker on the plate, with the month and how far it is:
 * amber in its month and the one before, red once it is overdue
 *
 * @param nextDue the due month of the latest HU, any day of it
 */
export default function NextHu({ nextDue }: { nextDue: string }) {
  const { t } = useTranslation();
  const format = useFormat();
  const due = dayjs(nextDue).startOf('month');
  const months = due.diff(dayjs().startOf('month'), 'month');
  const month = new Intl.DateTimeFormat(format.locale, {
    month: 'long',
    year: 'numeric',
  }).format(due.toDate());

  return (
    <Box
      data-testid="next-hu"
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: { xs: 2, sm: 3 },
        pb: 2.5,
        mb: 1,
        borderBottom: 1,
        borderColor: 'divider',
      }}
    >
      <HuSticker
        year={due.year()}
        month={due.month() + 1}
        label={t('inspection.sticker', { month })}
        size={148}
      />
      <Box sx={{ minWidth: 0 }}>
        <Typography sx={{ color: 'text.secondary' }}>
          {t('inspection.fields.nextDue')}
        </Typography>
        <Typography
          sx={{
            fontFamily: displayFont,
            fontWeight: 700,
            fontSize: { xs: '1.75rem', sm: '2.25rem' },
            lineHeight: 1.1,
          }}
        >
          {month}
        </Typography>
        <Typography
          sx={(theme) => ({
            fontWeight: 600,
            color: 'text.secondary',
            ...(months <= 1 && {
              color: theme.vars.palette.warning.main,
              ...theme.applyStyles('dark', { color: CLUSTER_AMBER }),
            }),
            ...(months < 0 && { color: theme.vars.palette.error.main }),
          })}
        >
          {months < 0
            ? t('inspection.due.overdue', { count: -months })
            : months === 0
              ? t('inspection.due.thisMonth')
              : t('inspection.due.inMonths', { count: months })}
        </Typography>
      </Box>
    </Box>
  );
}
