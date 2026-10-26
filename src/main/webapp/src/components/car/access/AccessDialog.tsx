import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers';
import { useMutation } from '@tanstack/react-query';
import { Dayjs } from 'dayjs';
import { SubmitEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { CarAccessEntry, CarRole } from '@/generated';
import {
  changeCarAccessMutation,
  getCarAccessQueryKey,
  inviteToCarMutation,
} from '@/generated/@tanstack/react-query.gen';
import queryClient, { localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import { firstDay, fromDay, lastDay, untilDay } from './period';

/** the roles the owner gives, the owner is the user of the car */
const ROLES = [CarRole.EDITOR, CarRole.VIEWER, CarRole.DRIVER] as const;
/** same as CarInvitationInput.MAX_ACCOUNT of the backend */
const MAX_ACCOUNT = 320;

export interface AccessDialogProps {
  open: boolean;
  carId: number;
  /** the access to change, a new invitation without */
  access?: CarAccessEntry;
  onClose: () => void;
}

/**
 * Invites an account to the car by its user name or e-mail address, or
 * changes the role and the period of an access
 */
export default function AccessDialog({
  open,
  onClose,
  ...props
}: AccessDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      {/* keyed, so the form starts from the access */}
      {open && (
        <AccessForm
          key={props.access?.id ?? 'new'}
          {...props}
          onClose={onClose}
        />
      )}
    </Dialog>
  );
}

function AccessForm({
  carId,
  access,
  onClose,
}: Omit<AccessDialogProps, 'open'>) {
  const { t } = useTranslation();
  const [account, setAccount] = useState('');
  const [role, setRole] = useState<CarRole>(access?.role ?? CarRole.DRIVER);
  const [from, setFrom] = useState<Dayjs | null>(
    access ? firstDay(access) : null,
  );
  const [until, setUntil] = useState<Dayjs | null>(
    access ? lastDay(access) : null,
  );
  const [touched, setTouched] = useState(false);

  const accountError = access
    ? undefined
    : !account.trim()
      ? t('access.errors.account')
      : account.trim().length > MAX_ACCOUNT
        ? t('access.errors.accountLength', { max: MAX_ACCOUNT })
        : undefined;
  const untilError =
    from && until && until.isBefore(from, 'day')
      ? t('access.errors.period')
      : undefined;

  const done = async (message: string) => {
    toast.success(message);
    await queryClient.invalidateQueries({
      queryKey: getCarAccessQueryKey({ path: { carId } }),
    });
    onClose();
  };
  const onError = (e: unknown) =>
    toast.error(errorMessage(e, t('errors.backend')));
  const invite = useMutation({
    ...inviteToCarMutation({ client: localClient }),
    onSuccess: () => done(t('access.invited')),
    onError,
  });
  const change = useMutation({
    ...changeCarAccessMutation({ client: localClient }),
    onSuccess: () => done(t('access.changed')),
    onError,
  });

  const submit = (event: SubmitEvent) => {
    event.preventDefault();
    setTouched(true);
    if (accountError || untilError) return;
    const period = {
      role,
      validFrom: fromDay(from),
      validUntil: untilDay(until),
    };
    if (access) {
      change.mutate({ path: { carId, id: access.id }, body: period });
    } else {
      invite.mutate({
        path: { carId },
        body: { account: account.trim(), ...period },
      });
    }
  };

  return (
    <form onSubmit={submit} noValidate>
      <DialogTitle>
        {access
          ? t('access.changeTitle', { name: access.account ?? access.invitee })
          : t('access.inviteTitle')}
      </DialogTitle>
      <DialogContent>
        <Stack spacing={2.5} sx={{ pt: 1 }}>
          {!access && (
            <TextField
              label={t('access.fields.account')}
              value={account}
              onChange={(event) => setAccount(event.target.value)}
              error={touched && !!accountError}
              helperText={
                (touched && accountError) || t('access.fields.accountHelp')
              }
              autoComplete="off"
              autoFocus
              fullWidth
            />
          )}
          <TextField
            select
            label={t('access.fields.role')}
            value={role}
            onChange={(event) => setRole(event.target.value as CarRole)}
            helperText={t(`access.roleHints.${role}`)}
            fullWidth
          >
            {ROLES.map((each) => (
              <MenuItem key={each} value={each}>
                {t(`car.roles.${each}`)}
              </MenuItem>
            ))}
          </TextField>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <DatePicker
              label={t('access.fields.from')}
              value={from}
              onChange={setFrom}
              slotProps={{
                textField: {
                  fullWidth: true,
                  helperText: t('access.fields.fromHelp'),
                },
                field: { clearable: true },
              }}
            />
            <DatePicker
              label={t('access.fields.until')}
              value={until}
              onChange={setUntil}
              minDate={from ?? undefined}
              disablePast={!access}
              slotProps={{
                textField: {
                  fullWidth: true,
                  error: !!untilError,
                  helperText: untilError ?? t('access.fields.untilHelp'),
                },
                field: { clearable: true },
              }}
            />
          </Stack>
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose}>{t('button.cancel')}</Button>
        <Button
          type="submit"
          variant="contained"
          disabled={invite.isPending || change.isPending}
        >
          {access ? t('button.save') : t('access.invite')}
        </Button>
      </DialogActions>
    </form>
  );
}
