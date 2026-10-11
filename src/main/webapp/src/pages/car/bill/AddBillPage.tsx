import { Box, ButtonBase, Card, Grid, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { BillType } from '@/generated';
import { billTypeList } from '@/utils/billTypes';
import PageHeader from '@/components/ui/PageHeader';
import FuelForm from '@/components/bill/FuelForm';
import MaintenanceForm from '@/components/bill/MaintenanceForm';
import MiscellaneousForm from '@/components/bill/MiscellaneousForm';
import RecurringForm from '@/components/bill/RecurringForm';

export interface AddBillPageProps {
  carId: number;
  type: BillType;
  onTypeChange: (type: BillType) => void;
}

export default function AddBillPage({
  carId,
  type,
  onTypeChange,
}: AddBillPageProps) {
  const { t } = useTranslation();

  return (
    <Box id="AddBillPage" sx={{ maxWidth: 860 }}>
      <PageHeader
        title={t('bill.add.title')}
        subtitle={t('bill.add.subtitle')}
      />

      <Grid container spacing={1.5} sx={{ mb: 3 }} role="radiogroup">
        {billTypeList.map((meta) => {
          const selected = meta.type === type;
          return (
            <Grid key={meta.type} size={{ xs: 6, sm: 3 }}>
              <ButtonBase
                role="radio"
                aria-checked={selected}
                onClick={() => onTypeChange(meta.type)}
                sx={(theme) => ({
                  width: '100%',
                  justifyContent: 'flex-start',
                  p: 1.5,
                  borderRadius: 3,
                  border: 1.5,
                  borderColor: selected
                    ? theme.vars.palette[meta.color].main
                    : theme.vars.palette.divider,
                  backgroundColor: selected
                    ? `rgba(${theme.vars.palette[meta.color].mainChannel} / 0.08)`
                    : theme.vars.palette.background.paper,
                  transition: 'border-color .15s, background-color .15s',
                })}
              >
                <Stack direction="row" spacing={1.25} alignItems="center">
                  <Box
                    sx={(theme) => ({
                      display: 'grid',
                      placeItems: 'center',
                      width: 30,
                      height: 30,
                      borderRadius: 2,
                      color: theme.vars.palette[meta.color].main,
                      backgroundColor: `rgba(${theme.vars.palette[meta.color].mainChannel} / 0.14)`,
                    })}
                  >
                    {meta.icon}
                  </Box>
                  <Typography fontWeight={600}>
                    {t(`bill.types.${meta.type}`)}
                  </Typography>
                </Stack>
              </ButtonBase>
            </Grid>
          );
        })}
      </Grid>

      <Card sx={{ p: { xs: 2, sm: 3 } }}>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2.5 }}>
          {t(`bill.add.describe.${type}`)}
        </Typography>
        {/* keyed, so switching the type starts with a fresh form */}
        {type === BillType.FUEL && <FuelForm key={type} carId={carId} />}
        {type === BillType.MAINTENANCE && (
          <MaintenanceForm key={type} carId={carId} />
        )}
        {type === BillType.MISCELLANEOUS && (
          <MiscellaneousForm key={type} carId={carId} />
        )}
        {type === BillType.RECURRING && (
          <RecurringForm key={type} carId={carId} />
        )}
      </Card>
    </Box>
  );
}
