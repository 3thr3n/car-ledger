import CarLedgerPage from '@/components/CarLedgerPage';
import {
  Box,
  Container,
  MenuItem,
  Select,
  SelectChangeEvent,
  Step,
  StepContent,
  StepLabel,
  Stepper,
} from '@mui/material';
import CarLedgerPageHeader from '@/components/CarLedgerPageHeader';
import { useState } from 'react';
import { CsvUploadButton } from '@/components/csv/CsvUploadButton';
import { useTranslation } from 'react-i18next';
import { BillType } from '@/generated';
import { useMutation, useQuery } from '@tanstack/react-query';
import {
  getCsvFieldsOptions,
  importCsvMutation,
} from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import CarLedgerButton from '@/components/CarLedgerButton';
import { CsvOptionBox } from '@/components/csv/CsvOptionBox';
import { toast } from 'react-toastify';
import { BackendError } from '@/utils/BackendError';
import countryVat from 'country-vat';
import CountrySelection from '@/components/car/bill/CountrySelection';
import { NavigateOptions } from '@tanstack/router-core';

export interface ImportBillPageProps {
  id: number;
  navigate: (opt: NavigateOptions) => void;
}

export function ImportBillPage(props: ImportBillPageProps) {
  const { t } = useTranslation();
  const [activeStep, setActiveStep] = useState(0);
  const [headers, setHeaders] = useState<string[]>([]);
  const [csvFile, setCsvFile] = useState<File | undefined>(undefined);
  const [countryCode, setCountryCode] = useState<string>('DE');
  const [selectedType, setSelectedType] = useState<BillType>(BillType.FUEL);
  const [order] = useState<Map<string, number>>(new Map());

  const { refetch: fetchFields, data: fields } = useQuery({
    ...getCsvFieldsOptions({
      client: localClient,
      query: {
        importType: selectedType,
      },
    }),
    enabled: false,
  });

  const { mutate } = useMutation({
    ...importCsvMutation({
      client: localClient,
    }),
    onSuccess: () => {
      toast.info('Import successful');
      props.navigate({ to: '/car/$id', params: { id: `${props.id}` } });
    },
    onError: (error: BackendError) => {
      console.warn('Non successful response', error.status);
      toast.error('Import failed!');
    },
  });

  const vatSelected = () => {
    if (countryCode === '') {
      toast.error('Please select your country');
      return;
    }
    nextStep();
  };

  const handleChange = (event: SelectChangeEvent<BillType>) => {
    const newHeaderIndex = event.target.value;
    setSelectedType(newHeaderIndex);
  };

  const onParseComplete = (data: string[], file: File) => {
    setHeaders(data);
    setCsvFile(file);
    nextStep();
  };

  const backStep = () => {
    if (activeStep == 0) return;
    setActiveStep(activeStep - 1);
  };

  const nextStep = () => {
    if (activeStep == 0) {
      fetchFields();
      setActiveStep(1);
    } else if (activeStep == 1 || activeStep == 2) {
      setActiveStep(activeStep + 1);
    } else if (activeStep == 3) {
      const stringifiedOrder = JSON.stringify(Object.fromEntries(order));
      mutate({
        body: {
          order: stringifiedOrder,
          file: csvFile,
          vat: countryVat(countryCode)! * 100,
          billType: selectedType,
        },
        path: {
          carId: props.id,
        },
        query: {
          skipHeader: true,
        },
      });
    }
  };

  return (
    <CarLedgerPage id="ImportBillPage">
      <Container maxWidth={'md'}>
        <CarLedgerPageHeader title={`Import CSV for car ID: ${props.id}`} />
        <Stepper orientation="vertical" activeStep={activeStep}>
          <Step>
            <StepLabel>Selection of import type</StepLabel>
            <StepContent>
              <Box
                display={'flex'}
                flexDirection={'column'}
                alignItems={'center'}
              >
                <Select
                  variant="outlined"
                  value={selectedType}
                  onChange={handleChange}
                  sx={{ mb: 2, width: 250 }}
                >
                  {Object.values(BillType).map((x) => (
                    <MenuItem key={x} value={x}>
                      {x}
                    </MenuItem>
                  ))}
                </Select>
                <Box display={'flex'}>
                  <CarLedgerButton onClick={nextStep}>
                    {t('app.button.continue')}
                  </CarLedgerButton>
                </Box>
              </Box>
            </StepContent>
          </Step>
          <Step>
            <StepLabel>VAT selection</StepLabel>
            <StepContent id="VatSelectionContent">
              <Box
                display={'flex'}
                flexDirection={'column'}
                alignItems={'center'}
              >
                <CountrySelection
                  value={countryCode}
                  onChange={setCountryCode}
                  sx={{
                    mb: 2,
                  }}
                />
                <Box display={'flex'}>
                  <CarLedgerButton onClick={backStep}>
                    {t('app.button.back')}
                  </CarLedgerButton>
                  <CarLedgerButton onClick={vatSelected}>
                    {t('app.button.continue')}
                  </CarLedgerButton>
                </Box>
              </Box>
            </StepContent>
          </Step>
          <Step>
            <StepLabel>{t('app.car.fuel.stepper.upload')}</StepLabel>
            <StepContent>
              <Box
                display={'flex'}
                flexDirection={'column'}
                alignItems={'center'}
              >
                <CsvUploadButton
                  sx={{ mb: 2 }}
                  onParseComplete={onParseComplete}
                />
                <CarLedgerButton onClick={backStep}>
                  {t('app.button.back')}
                </CarLedgerButton>
              </Box>
            </StepContent>
          </Step>
          <Step>
            <StepLabel>Mapping of keys to columns</StepLabel>
            <StepContent>
              <Box
                display={'flex'}
                flexDirection={'column'}
                alignItems={'center'}
              >
                {fields?.all?.sort().map((x) => (
                  <CsvOptionBox
                    key={x}
                    headers={headers}
                    mandatory={fields?.required?.includes(x)}
                    title={t(`app.car.common.${x}`)}
                    onOptionChanged={(value) => {
                      if (value != undefined) {
                        order.set(x, value);
                      } else {
                        order.delete(x);
                      }
                    }}
                  />
                ))}
                <Box display={'flex'}>
                  <CarLedgerButton onClick={backStep}>
                    {t('app.button.back')}
                  </CarLedgerButton>
                  <CarLedgerButton onClick={nextStep}>
                    {t('app.button.upload')}
                  </CarLedgerButton>
                </Box>
              </Box>
            </StepContent>
          </Step>
        </Stepper>
      </Container>
    </CarLedgerPage>
  );
}
