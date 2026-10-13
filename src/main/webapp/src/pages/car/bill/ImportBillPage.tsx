import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  Divider,
  List,
  ListItem,
  ListItemText,
  MenuItem,
  Stack,
  Step,
  StepContent,
  StepLabel,
  Stepper,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import UploadFileRounded from '@mui/icons-material/UploadFileRounded';
import CheckCircleRounded from '@mui/icons-material/CheckCircleRounded';
import { useMutation, useQuery } from '@tanstack/react-query';
import Papa from 'papaparse';
import { ChangeEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { BillType, CsvImportResult, importCsv } from '@/generated';
import { getCsvFieldsOptions } from '@/generated/@tanstack/react-query.gen';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import { billTypeList, billTypes } from '@/utils/billTypes';
import useVatCountry from '@/hooks/useVatCountry';
import {
  loadMapping,
  restoreMapping,
  saveMapping,
  toSavedMapping,
} from '@/utils/csv';
import PageHeader from '@/components/ui/PageHeader';
import VatCountrySelect from '@/components/bill/VatCountrySelect';
import { RouterButton } from '@/components/ui/links';

type ImportType = BillType.FUEL | BillType.MAINTENANCE | BillType.MISCELLANEOUS;

const importTypes = billTypeList.filter((meta) => meta.importable);

export interface ImportBillPageProps {
  carId: number;
  initialType?: BillType;
}

export default function ImportBillPage({
  carId,
  initialType,
}: ImportBillPageProps) {
  const { t } = useTranslation();
  const vat = useVatCountry();

  const [step, setStep] = useState(0);
  const [type, setType] = useState<ImportType>(
    initialType && billTypes[initialType].importable
      ? (initialType as ImportType)
      : BillType.FUEL,
  );
  const [file, setFile] = useState<File>();
  const [headers, setHeaders] = useState<string[]>([]);
  const [mapping, setMapping] = useState<Record<string, number>>({});
  const [restored, setRestored] = useState(false);
  const [result, setResult] = useState<CsvImportResult>();

  const { data: fields } = useQuery(
    getCsvFieldsOptions({
      client: localClient,
      path: { carId },
      query: { importType: type },
    }),
  );
  const allFields = [...(fields?.all ?? [])].sort();
  const required = fields?.required ?? [];
  const missing = required.filter((field) => mapping[field] === undefined);

  const mutation = useMutation({
    mutationFn: async () => {
      const { data } = await importCsv({
        client: localClient,
        path: { carId },
        query: { skipHeader: true },
        body: {
          file,
          billType: type,
          order: JSON.stringify(mapping),
          vat: vat.vatRate,
        },
        throwOnError: true,
      });
      return data ?? {};
    },
    onSuccess: async (data) => {
      // the next import of this type starts with this mapping
      saveMapping(type, toSavedMapping(allFields, headers, mapping));
      setResult(data);
      setStep(4);
      await invalidateCar(carId);
    },
    onError: (error) => {
      toast.error(errorMessage(error, t('import.failed')));
    },
  });

  const onFile = (event: ChangeEvent<HTMLInputElement>) => {
    const selected = event.target.files?.[0];
    event.target.value = '';
    if (!selected) return;

    Papa.parse<Record<string, string>>(selected, {
      header: true,
      preview: 1,
      skipEmptyLines: true,
      complete: (parsed) => {
        const columns = parsed.meta.fields ?? [];
        if (columns.length === 0) {
          toast.warn(t('import.noHeader'));
          return;
        }
        setFile(selected);
        setHeaders(columns);
        const proposal = restoreMapping(allFields, columns, loadMapping(type));
        setMapping(proposal.mapping);
        setRestored(proposal.restored);
        setStep(2);
      },
    });
  };

  const restart = () => {
    setStep(0);
    setFile(undefined);
    setHeaders([]);
    setMapping({});
    setRestored(false);
    setResult(undefined);
  };

  const back = (
    <Button color="inherit" onClick={() => setStep((s) => s - 1)}>
      {t('button.back')}
    </Button>
  );

  return (
    <Box id="ImportBillPage" sx={{ maxWidth: 760 }}>
      <PageHeader title={t('import.title')} subtitle={t('import.subtitle')} />

      <Card sx={{ p: { xs: 2, sm: 3 } }}>
        <Stepper activeStep={step} orientation="vertical">
          <Step>
            <StepLabel>{t('import.steps.type')}</StepLabel>
            <StepContent>
              <Typography
                variant="body2"
                sx={{
                  color: 'text.secondary',
                  mb: 2,
                }}
              >
                {t('import.typeHint')}
              </Typography>
              <ToggleButtonGroup
                exclusive
                value={type}
                onChange={(_, value) => value && setType(value)}
                sx={{ mb: 2, flexWrap: 'wrap' }}
              >
                {importTypes.map((meta) => (
                  <ToggleButton
                    key={meta.type}
                    value={meta.type}
                    sx={{ gap: 1, px: 2 }}
                  >
                    {meta.icon}
                    {t(`bill.types.${meta.type}`)}
                  </ToggleButton>
                ))}
              </ToggleButtonGroup>
              <Box>
                <Button variant="contained" onClick={() => setStep(1)}>
                  {t('button.continue')}
                </Button>
              </Box>
            </StepContent>
          </Step>

          <Step>
            <StepLabel
              optional={
                file && <Typography variant="caption">{file.name}</Typography>
              }
            >
              {t('import.steps.file')}
            </StepLabel>
            <StepContent>
              <Typography
                variant="body2"
                sx={{
                  color: 'text.secondary',
                  mb: 1,
                }}
              >
                {t('import.fileHint')}
              </Typography>
              {allFields.length > 0 && (
                <Stack
                  direction="row"
                  spacing={0.75}
                  useFlexGap
                  sx={{
                    flexWrap: 'wrap',
                    mb: 2,
                  }}
                >
                  {allFields.map((field) => (
                    <Chip
                      key={field}
                      size="small"
                      variant="outlined"
                      label={`${t(`bill.fields.${field}`, { defaultValue: field })}${required.includes(field) ? ' *' : ''}`}
                    />
                  ))}
                </Stack>
              )}
              <Stack direction="row" spacing={1}>
                {back}
                <Button
                  variant="contained"
                  component="label"
                  startIcon={<UploadFileRounded />}
                >
                  {t('import.chooseFile')}
                  <input
                    hidden
                    type="file"
                    accept=".csv,text/csv"
                    onChange={onFile}
                  />
                </Button>
              </Stack>
            </StepContent>
          </Step>

          <Step>
            <StepLabel>{t('import.steps.mapping')}</StepLabel>
            <StepContent>
              <Typography
                variant="body2"
                sx={{
                  color: 'text.secondary',
                  mb: 2,
                }}
              >
                {t('import.mappingHint', { count: headers.length })}
              </Typography>
              {restored && (
                <Alert severity="info" variant="outlined" sx={{ mb: 2 }}>
                  {t('import.mappingRestored')}
                </Alert>
              )}
              <Stack spacing={1.5} sx={{ mb: 2 }}>
                {allFields.map((field) => (
                  <TextField
                    key={field}
                    select
                    size="small"
                    label={t(`bill.fields.${field}`, { defaultValue: field })}
                    required={required.includes(field)}
                    value={mapping[field] ?? -1}
                    onChange={(e) => {
                      const index = Number(e.target.value);
                      setMapping((current) => {
                        const next = { ...current };
                        if (index < 0) delete next[field];
                        else next[field] = index;
                        return next;
                      });
                    }}
                  >
                    <MenuItem value={-1}>
                      <em>{t('import.notMapped')}</em>
                    </MenuItem>
                    {headers.map((header, index) => (
                      <MenuItem key={`${header}-${index}`} value={index}>
                        {header}
                      </MenuItem>
                    ))}
                  </TextField>
                ))}
              </Stack>
              {missing.length > 0 && (
                <Alert severity="warning" variant="outlined" sx={{ mb: 2 }}>
                  {t('import.missingRequired', {
                    fields: missing
                      .map((field) =>
                        t(`bill.fields.${field}`, { defaultValue: field }),
                      )
                      .join(', '),
                  })}
                </Alert>
              )}
              <Stack direction="row" spacing={1}>
                {back}
                <Button
                  variant="contained"
                  disabled={missing.length > 0}
                  onClick={() => setStep(3)}
                >
                  {t('button.continue')}
                </Button>
              </Stack>
            </StepContent>
          </Step>

          <Step>
            <StepLabel>{t('import.steps.vat')}</StepLabel>
            <StepContent>
              <Box sx={{ maxWidth: 420, mb: 2, pt: 1 }}>
                <VatCountrySelect
                  value={vat.country}
                  onChange={vat.setCountry}
                />
              </Box>
              <Stack direction="row" spacing={1}>
                {back}
                <Button
                  variant="contained"
                  disabled={mutation.isPending || vat.vatRate === undefined}
                  onClick={() => mutation.mutate()}
                >
                  {t('import.start')}
                </Button>
              </Stack>
            </StepContent>
          </Step>

          <Step>
            <StepLabel>{t('import.steps.result')}</StepLabel>
            <StepContent>
              {result && <ImportSummary result={result} />}
              <Stack direction="row" spacing={1} sx={{ mt: 2 }}>
                <RouterButton
                  to={billTypes[type].listRoute}
                  params={{ id: `${carId}` }}
                  variant="contained"
                >
                  {t('import.showEntries')}
                </RouterButton>
                <Button color="inherit" onClick={restart}>
                  {t('import.again')}
                </Button>
              </Stack>
            </StepContent>
          </Step>
        </Stepper>
      </Card>
    </Box>
  );
}

function ImportSummary({ result }: { result: CsvImportResult }) {
  const { t } = useTranslation();
  const problems = [
    ...(result.errors ?? []).map((row) => ({
      ...row,
      severity: 'error' as const,
    })),
    ...(result.skipped ?? []).map((row) => ({
      ...row,
      severity: 'skipped' as const,
    })),
  ].sort((a, b) => (a.rowNumber ?? 0) - (b.rowNumber ?? 0));

  return (
    <Box>
      <Stack
        direction="row"
        spacing={1}
        sx={{
          alignItems: 'center',
          mb: 1.5,
        }}
      >
        <CheckCircleRounded color="success" />
        <Typography
          sx={{
            fontWeight: 600,
          }}
        >
          {t('import.result.success', { count: result.successCount ?? 0 })}
        </Typography>
      </Stack>
      <Stack direction="row" spacing={1} sx={{ mb: problems.length ? 2 : 0 }}>
        <Chip
          size="small"
          label={t('import.result.skipped', {
            count: result.skippedCount ?? 0,
          })}
        />
        <Chip
          size="small"
          color={result.errorCount ? 'error' : 'default'}
          label={t('import.result.errors', { count: result.errorCount ?? 0 })}
        />
      </Stack>
      {problems.length > 0 && (
        <Card variant="outlined" sx={{ maxHeight: 260, overflowY: 'auto' }}>
          <List dense disablePadding>
            {problems.map((row, index) => (
              <Box key={`${row.severity}-${row.rowNumber}-${index}`}>
                {index > 0 && <Divider />}
                <ListItem>
                  <ListItemText
                    primary={t('import.result.row', { row: row.rowNumber })}
                    secondary={row.message}
                    slotProps={{
                      primary: {
                        color:
                          row.severity === 'error' ? 'error' : 'text.secondary',
                        sx: { fontWeight: 600 },
                      },
                    }}
                  />
                </ListItem>
              </Box>
            ))}
          </List>
        </Card>
      )}
    </Box>
  );
}
