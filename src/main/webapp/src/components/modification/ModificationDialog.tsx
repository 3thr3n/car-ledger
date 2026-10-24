import {
  Autocomplete,
  Box,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers';
import { useMutation } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { SubmitEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { ApprovalType, Modification, ModificationInput } from '@/generated';
import {
  addModificationMutation,
  updateModificationMutation,
} from '@/generated/@tanstack/react-query.gen';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import { toApiDate, useFormat } from '@/utils/format';
import { odometerToApi, odometerToField } from '@/utils/units';
import NumberField from '@/components/bill/NumberField';
import { hasApproval, needsInspection, TESTING_BODIES } from './approval';

export interface ModificationDialogProps {
  open: boolean;
  carId: number;
  /** current odometer of the car */
  odometer?: number;
  /** edits this one, adds a new one without */
  modification?: Modification;
  onClose: () => void;
}

export default function ModificationDialog({
  open,
  onClose,
  ...props
}: ModificationDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      {/* keyed, so the form starts fresh for every modification */}
      {open && (
        <ModificationForm
          key={props.modification?.id ?? 'new'}
          onClose={onClose}
          {...props}
        />
      )}
    </Dialog>
  );
}

const day = (value?: string) => (value ? dayjs(value) : null);
const valid = (value: Dayjs | null) => !!value?.isValid();

/**
 * The fields that don't apply to the approval are hidden and not saved, e.g.
 * no Abnahme for an ABE
 */
function ModificationForm({
  carId,
  odometer,
  modification,
  onClose,
}: Omit<ModificationDialogProps, 'open'>) {
  const { t } = useTranslation();
  const format = useFormat();
  const { units } = format;
  const [part, setPart] = useState(modification?.part ?? '');
  const [manufacturer, setManufacturer] = useState(
    modification?.manufacturer ?? '',
  );
  const [approvalType, setApprovalType] = useState(
    modification?.approvalType ?? ApprovalType.PARTS_REPORT,
  );
  const [approvalNumber, setApprovalNumber] = useState(
    modification?.approvalNumber ?? '',
  );
  const [conditions, setConditions] = useState(modification?.conditions ?? '');
  const [inspectionDate, setInspectionDate] = useState(
    day(modification?.inspectionDate),
  );
  const [testingBody, setTestingBody] = useState(
    modification?.testingBody ?? '',
  );
  const [registered, setRegistered] = useState(
    modification?.registered ?? false,
  );
  const [installedDate, setInstalledDate] = useState(
    modification ? day(modification.installedDate) : dayjs(),
  );
  const [installedOdometer, setInstalledOdometer] = useState(
    odometerToField(
      units,
      modification ? modification.installedOdometer : odometer,
    ),
  );
  const [removedDate, setRemovedDate] = useState(
    day(modification?.removedDate),
  );
  const [removedOdometer, setRemovedOdometer] = useState(
    odometerToField(units, modification?.removedOdometer),
  );
  const [notes, setNotes] = useState(modification?.notes ?? '');
  // only a removed one shows when, removing is a separate action
  const removed = !!modification && !modification.installed;

  const onSuccess = async () => {
    toast.success(
      t(modification ? 'modification.updated' : 'modification.added'),
    );
    onClose();
    await invalidateCar(carId);
  };
  const onError = (error: Error) =>
    toast.error(errorMessage(error, t('errors.backend')));
  const add = useMutation({
    ...addModificationMutation({ client: localClient }),
    onSuccess,
    onError,
  });
  const update = useMutation({
    ...updateModificationMutation({ client: localClient }),
    onSuccess,
    onError,
  });

  const approval = hasApproval(approvalType);
  const inspection = needsInspection(approvalType);
  const errors = {
    removedDate:
      removed && !valid(removedDate)
        ? t('modification.errors.removedDate')
        : valid(removedDate) &&
            valid(installedDate) &&
            removedDate!.isBefore(installedDate, 'day')
          ? t('modification.errors.removedBeforeInstalled')
          : undefined,
    removedOdometer:
      removedOdometer !== undefined &&
      installedOdometer !== undefined &&
      removedOdometer < installedOdometer
        ? t('modification.errors.removedBelowInstalled')
        : undefined,
  };
  const invalid = !part.trim() || Object.values(errors).some(Boolean);

  const submit = (event: SubmitEvent) => {
    event.preventDefault();
    if (invalid) return;
    const body: ModificationInput = {
      part: part.trim(),
      manufacturer: manufacturer.trim() || undefined,
      approvalType,
      approvalNumber: (approval && approvalNumber.trim()) || undefined,
      conditions: (approval && conditions.trim()) || undefined,
      inspectionDate:
        inspection && valid(inspectionDate)
          ? toApiDate(inspectionDate)
          : undefined,
      testingBody: (inspection && testingBody.trim()) || undefined,
      registered: inspection && registered,
      installedDate: valid(installedDate)
        ? toApiDate(installedDate)
        : undefined,
      installedOdometer: odometerToApi(units, installedOdometer),
      removedDate: removed ? toApiDate(removedDate) : undefined,
      removedOdometer: removed
        ? odometerToApi(units, removedOdometer)
        : undefined,
      notes: notes.trim() || undefined,
      // not changed here, kept as it is
      billId: modification?.billId,
    };
    if (modification) {
      update.mutate({ path: { carId, id: modification.id }, body });
    } else {
      add.mutate({ path: { carId }, body });
    }
  };

  return (
    <Box component="form" onSubmit={submit} noValidate>
      <DialogTitle>
        {t(modification ? 'modification.edit.title' : 'modification.add.title')}
      </DialogTitle>
      <DialogContent>
        <Stack spacing={2.5} sx={{ pt: 1 }}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <TextField
              label={t('modification.fields.part')}
              value={part}
              onChange={(e) => setPart(e.target.value)}
              required
              fullWidth
              autoFocus
              placeholder={t('modification.fields.partExample')}
              slotProps={{ htmlInput: { maxLength: 100 } }}
            />
            <TextField
              label={t('modification.fields.manufacturer')}
              value={manufacturer}
              onChange={(e) => setManufacturer(e.target.value)}
              fullWidth
              slotProps={{ htmlInput: { maxLength: 100 } }}
            />
          </Stack>

          <Box>
            <Typography variant="subtitle2" sx={{ mb: 1.5 }}>
              {t('modification.sections.approval')}
            </Typography>
            <Stack spacing={2.5}>
              <TextField
                select
                label={t('modification.fields.approvalType')}
                value={approvalType}
                onChange={(e) =>
                  setApprovalType(e.target.value as ApprovalType)
                }
                helperText={t(`modification.approvalHints.${approvalType}`)}
              >
                {Object.values(ApprovalType).map((type) => (
                  <MenuItem key={type} value={type}>
                    {t(`modification.approvalTypes.${type}`)}
                  </MenuItem>
                ))}
              </TextField>
              {approval && (
                <>
                  <TextField
                    label={t('modification.fields.approvalNumber')}
                    value={approvalNumber}
                    onChange={(e) => setApprovalNumber(e.target.value)}
                    helperText={t('modification.approvalNumberHint')}
                    slotProps={{ htmlInput: { maxLength: 50 } }}
                  />
                  <TextField
                    label={t('modification.fields.conditions')}
                    value={conditions}
                    onChange={(e) => setConditions(e.target.value)}
                    helperText={t('modification.conditionsHint')}
                    multiline
                    minRows={2}
                    slotProps={{ htmlInput: { maxLength: 2000 } }}
                  />
                </>
              )}
            </Stack>
          </Box>

          {inspection && (
            <Box>
              <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
                {t('modification.sections.inspection')}
              </Typography>
              <Typography
                variant="body2"
                sx={{ color: 'text.secondary', mb: 1.5 }}
              >
                {t('modification.inspectionHint')}
              </Typography>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <DatePicker
                  label={t('modification.fields.inspectionDate')}
                  value={inspectionDate}
                  onChange={setInspectionDate}
                  disableFuture
                  slotProps={{
                    textField: { fullWidth: true },
                    field: { clearable: true },
                  }}
                />
                <Autocomplete
                  freeSolo
                  fullWidth
                  options={TESTING_BODIES}
                  inputValue={testingBody}
                  onInputChange={(_, value) => setTestingBody(value)}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label={t('modification.fields.testingBody')}
                      slotProps={{
                        ...params.slotProps,
                        htmlInput: {
                          ...params.slotProps.htmlInput,
                          maxLength: 100,
                        },
                      }}
                    />
                  )}
                />
              </Stack>
              <FormControlLabel
                sx={{ mt: 1 }}
                control={
                  <Checkbox
                    checked={registered}
                    onChange={(e) => setRegistered(e.target.checked)}
                  />
                }
                label={t('modification.fields.registered')}
              />
            </Box>
          )}

          <Box>
            <Typography variant="subtitle2" sx={{ mb: 1.5 }}>
              {t('modification.sections.installed')}
            </Typography>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <DatePicker
                label={t('modification.fields.date')}
                value={installedDate}
                onChange={setInstalledDate}
                slotProps={{
                  textField: { fullWidth: true },
                  field: { clearable: true },
                }}
              />
              <NumberField
                label={t('modification.fields.odometer')}
                value={installedOdometer}
                onChange={setInstalledOdometer}
                decimalScale={0}
                suffix={` ${units.distance.label}`}
                max={10_000_000}
              />
            </Stack>
          </Box>

          {removed && (
            <Box>
              <Typography variant="subtitle2" sx={{ mb: 1.5 }}>
                {t('modification.sections.removed')}
              </Typography>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <DatePicker
                  label={t('modification.fields.date')}
                  value={removedDate}
                  onChange={setRemovedDate}
                  slotProps={{
                    textField: {
                      fullWidth: true,
                      error: !!errors.removedDate,
                      helperText: errors.removedDate,
                    },
                  }}
                />
                <NumberField
                  label={t('modification.fields.odometer')}
                  value={removedOdometer}
                  onChange={setRemovedOdometer}
                  decimalScale={0}
                  suffix={` ${units.distance.label}`}
                  max={10_000_000}
                  error={errors.removedOdometer}
                />
              </Stack>
            </Box>
          )}

          <TextField
            label={t('modification.fields.notes')}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            multiline
            minRows={2}
            slotProps={{ htmlInput: { maxLength: 1000 } }}
          />
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit">
          {t('button.cancel')}
        </Button>
        <Button
          type="submit"
          variant="contained"
          disabled={invalid || add.isPending || update.isPending}
        >
          {t('button.save')}
        </Button>
      </DialogActions>
    </Box>
  );
}
