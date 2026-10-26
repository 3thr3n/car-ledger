import { Alert, Button, Stack } from '@mui/material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import useOpenInvitations from '@/hooks/useOpenInvitations';
import { RouterButton } from '@/components/ui/links';

const DISMISSED = 'dismissedInvitations';

function dismissedIds(): number[] {
  try {
    const ids: unknown = JSON.parse(localStorage.getItem(DISMISSED) ?? '[]');
    return Array.isArray(ids) ? ids.filter(Number.isInteger) : [];
  } catch {
    return [];
  }
}

function dismiss(ids: number[]) {
  try {
    localStorage.setItem(DISMISSED, JSON.stringify(ids));
  } catch {
    // storage blocked: the note comes back on the next start
  }
}

/**
 * Tells on the dashboard that others invited the user to their cars, which
 * the garage accepts or declines. Hidden on this device after "Later" until a
 * new invitation comes; the badge of the garage stays.
 */
export default function InvitationNote() {
  const { t } = useTranslation();
  const invitations = useOpenInvitations();
  const [dismissed, setDismissed] = useState(dismissedIds);

  const shown = invitations.filter(
    (invitation) => !dismissed.includes(invitation.id),
  );
  if (shown.length === 0) return null;

  const later = () => {
    // only the open ones, the answered ones don't come back
    const ids = invitations.map((invitation) => invitation.id);
    dismiss(ids);
    setDismissed(ids);
  };

  return (
    <Alert
      severity="info"
      sx={{ mb: 2 }}
      data-testid="invitation-note"
      action={
        <Stack direction="row" spacing={1}>
          <Button color="inherit" size="small" onClick={later}>
            {t('access.note.later')}
          </Button>
          <RouterButton to="/car" variant="contained" size="small">
            {t('access.note.answer')}
          </RouterButton>
        </Stack>
      }
    >
      {shown.length === 1
        ? t('access.note.one', {
            owner: shown[0].owner,
            car: shown[0].car,
            role: t(`car.roles.${shown[0].role}`),
          })
        : t('access.note.other', { count: shown.length })}
    </Alert>
  );
}
