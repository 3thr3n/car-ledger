import { Box } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { BillType } from '@/generated';
import { billTypes, categoryInk } from '@/utils/billTypes';

export default function TypeChip({ type }: { type?: BillType }) {
  const { t } = useTranslation();
  if (!type) return null;
  const meta = billTypes[type];

  return (
    <Box
      component="span"
      sx={(theme) => ({
        display: 'inline-flex',
        alignItems: 'center',
        gap: 0.75,
        px: 1,
        py: 0.25,
        borderRadius: 999,
        fontSize: 12.5,
        fontWeight: 600,
        lineHeight: 1.6,
        ...categoryInk(theme, meta.color),
        backgroundColor: `rgba(${theme.vars.palette[meta.color].mainChannel} / 0.12)`,
        '& svg': { fontSize: 14 },
      })}
    >
      {meta.icon}
      {t(`bill.types.${type}`)}
    </Box>
  );
}
