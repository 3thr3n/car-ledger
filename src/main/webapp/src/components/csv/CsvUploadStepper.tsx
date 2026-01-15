import {
  Box,
  Button,
  LinearProgress,
  Step,
  StepContent,
  StepLabel,
  Stepper,
  Typography,
} from '@mui/material';
import { CsvUploadButton } from '@/components/csv/CsvUploadButton';
import { useState } from 'react';
import CsvUploadOptions from '@/components/csv/CsvUploadOptions';
import { CsvOrder } from '@/generated';
import { useMutation } from '@tanstack/react-query';
import { localClient } from '@/utils/QueryClient';
import { importCsvMutation } from '@/generated/@tanstack/react-query.gen';
import useCsvStore from '@/store/CsvStore';
import { toast } from 'react-toastify';
import { BackendError } from '@/utils/BackendError';
import { useTranslation } from 'react-i18next';

export interface CsvUploadStepperProps {
  close: () => void;
}

export function OptionBox(props: {
  headers: string[];
  title: string;
  defaultColumn?: number;
  onOptionChanged: (value: number) => void;
}) {
  return (
    <Box display="flex" alignItems="center" mb={1}>
      <Typography variant="body1" width={120}>
        {props.title}
      </Typography>
      <CsvUploadOptions
        headers={props.headers}
        defaultColumn={props.defaultColumn}
        onOptionChanged={props.onOptionChanged}
      />
    </Box>
  );
}

export default function CsvUploadStepper(props: CsvUploadStepperProps) {
  const { t } = useTranslation();

  const carId = useCsvStore((state) => state.carId);
  const markImported = useCsvStore((state) => state.markImported);

  const [activeStep, setActiveStep] = useState(0);
  const [headers, setHeaders] = useState<string[]>([]);
  const [csvOrder, setCsvOrder] = useState<CsvOrder>({
    date: 0,
    unit: 1,
    pricePerUnit: 2,
    distance: 3,
    estimate: 4,
  });
  const [csvFile, setCsvFile] = useState<File | undefined>(undefined);

  const [loading, setLoading] = useState(0);
  const [loadingColor, setLoadingColor] = useState<
    'primary' | 'error' | 'success'
  >('primary');

  const { mutate } = useMutation({
    ...importCsvMutation({
      client: localClient,
    }),
    onSuccess: () => {
      toast.info('Import successful');
      setLoadingColor('success');
      setLoading(100);
      markImported();
      setTimeout(closeDialog, 200);
    },
    onError: (error: BackendError) => {
      console.warn('Non successful response', error.status);
      setLoadingColor('error');
      toast.error('Import failed!');
    },
  });

  const updateCsvOrder = (newOrder: CsvOrder) => {
    setCsvOrder({
      ...csvOrder,
      ...newOrder,
    });
  };

  const onParseComplete = (headers: string[], file: File) => {
    setHeaders(headers);
    setCsvFile(file);
    setActiveStep(1);
  };

  const nextStep = () => {
    setActiveStep(2);
    if (!carId) {
      toast.error(
        'For some reason the id of car was not set, no import possible!',
      );
      return;
    }

    mutate({
      body: {
        order: csvOrder,
        file: csvFile,
      },
      path: {
        carId: carId,
      },
      query: {
        skipHeader: true,
      },
    });
  };

  const closeDialog = () => {
    props.close();
  };

  return (
    <Stepper orientation="vertical" activeStep={activeStep}>
      <Step>
        <StepLabel>{t('app.car.fuel.stepper.upload')}</StepLabel>
        <StepContent>
          <Box display={'flex'} justifyContent={'center'}>
            <CsvUploadButton onParseComplete={onParseComplete} />
          </Box>
        </StepContent>
      </Step>
      <Step>
        <StepLabel>{t('app.car.fuel.stepper.configureOrder')}</StepLabel>
        <StepContent>
          <OptionBox
            headers={headers}
            title={t('app.car.fuel.table.date')}
            onOptionChanged={(value) => updateCsvOrder({ date: value })}
          />
          <OptionBox
            headers={headers}
            title={t('app.car.fuel.table.unit')}
            defaultColumn={1}
            onOptionChanged={(value) => updateCsvOrder({ unit: value })}
          />
          <OptionBox
            headers={headers}
            title={t('app.car.fuel.table.pricePerUnit')}
            defaultColumn={2}
            onOptionChanged={(value) => updateCsvOrder({ pricePerUnit: value })}
          />
          <OptionBox
            headers={headers}
            title={t('app.car.fuel.table.distance')}
            defaultColumn={3}
            onOptionChanged={(value) => updateCsvOrder({ distance: value })}
          />
          <OptionBox
            headers={headers}
            title={t('app.car.fuel.table.estimateConsumption')}
            defaultColumn={4}
            onOptionChanged={(value) => updateCsvOrder({ estimate: value })}
          />

          <Box display={'flex'}>
            <Box flexGrow={1} />
            <Button onClick={nextStep} variant={'outlined'}>
              {t('app.button.continue')}
            </Button>
          </Box>
        </StepContent>
      </Step>
      <Step>
        <StepLabel>{t('app.car.fuel.stepper.importData')}</StepLabel>
        <StepContent>
          <LinearProgress
            color={loadingColor}
            variant={'determinate'}
            value={loading}
          />
          <Box display={'flex'} mt={2}>
            <Box flexGrow={1} />
            <Button onClick={closeDialog} variant={'outlined'}>
              {t('app.button.done')}
            </Button>
          </Box>
        </StepContent>
      </Step>
    </Stepper>
  );
}
