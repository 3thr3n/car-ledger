import { Button, Stack } from '@mui/material';
import { useRouter } from '@tanstack/react-router';
import { useTranslation } from 'react-i18next';

export default function FormActions({
  pending,
  onSaveAgain,
  editing,
}: {
  pending: boolean;
  /** called before the form submits via "save and add another" */
  onSaveAgain: (again: boolean) => void;
  /** an existing bill is edited, there is no "add another" */
  editing?: boolean;
}) {
  const { t } = useTranslation();
  const router = useRouter();

  return (
    <Stack
      direction={{ xs: 'column-reverse', sm: 'row' }}
      spacing={1}
      justifyContent="flex-end"
      sx={{ pt: 1 }}
    >
      <Button color="inherit" onClick={() => router.history.back()}>
        {t('button.cancel')}
      </Button>
      {!editing && (
        <Button
          type="submit"
          variant="outlined"
          disabled={pending}
          onClick={() => onSaveAgain(true)}
        >
          {t('bill.add.saveAgain')}
        </Button>
      )}
      <Button
        type="submit"
        variant="contained"
        disabled={pending}
        onClick={() => onSaveAgain(false)}
      >
        {t('button.save')}
      </Button>
    </Stack>
  );
}
