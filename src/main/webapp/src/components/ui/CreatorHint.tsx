import { Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import useMe from '@/hooks/useMe';

/**
 * Who made an entry or drove a trip, only when it wasn't the user: on a car of
 * one account it never shows
 *
 * @param name of the account, `null` once it was deleted, `undefined` where
 *   the entry has none (e.g. a recurring cost)
 */
export default function CreatorHint({ name }: { name?: string | null }) {
  const { t } = useTranslation();
  const { account } = useMe();
  if (name === undefined || (name !== null && name === account?.name)) {
    return null;
  }
  return (
    <Typography
      component="span"
      variant="caption"
      noWrap
      sx={{ ml: 0.75, color: 'text.secondary', fontWeight: 400 }}
    >
      {name === null ? t('creator.deleted') : t('creator.by', { name })}
    </Typography>
  );
}
