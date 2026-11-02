import { Box, ButtonBase, Card, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { can } from '@/utils/carRole';
import { BillType } from '@/generated';
import { getMyCarOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { useFormat } from '@/utils/format';
import { billTypesFor, categoryInk } from '@/utils/billTypes';
import PageHeader from '@/components/ui/PageHeader';
import FuelForm from '@/components/bill/FuelForm';
import ChargingForm from '@/components/bill/ChargingForm';
import MaintenanceForm from '@/components/bill/MaintenanceForm';
import MiscellaneousForm from '@/components/bill/MiscellaneousForm';
import RecurringForm from '@/components/bill/RecurringForm';
import { useQueued } from '@/offline/useQueued';

export interface AddBillPageProps {
  carId: number;
  /** the first type of the car without one */
  type?: BillType;
  /** id of a bill of the offline queue to correct */
  queued?: string;
  onTypeChange: (type: BillType) => void;
}

export default function AddBillPage({
  carId,
  type: requestedType,
  queued: queuedId,
  onTypeChange,
}: AddBillPageProps) {
  const { t } = useTranslation();
  const { units } = useFormat();
  // loaded by the layout of the car already
  const { data: car } = useQuery(
    getMyCarOptions({ client: localClient, path: { id: carId } }),
  );
  // an electric car is not refueled, a petrol car not charged
  // a driver adds refuels and charges only
  const types = billTypesFor(car?.energyType, !!car && !can(car, 'READ'));
  const type = types.some((meta) => meta.type === requestedType)
    ? requestedType!
    : types[0].type;
  // gone when it was sent or discarded meanwhile
  const queued = useQueued(carId, type).find((entry) => entry.id === queuedId);

  return (
    <Box id="AddBillPage" sx={{ maxWidth: 860 }}>
      <PageHeader title={t('bill.add.title')} />

      {/* one row of the kinds: the form starts high on a phone */}
      <Box
        role="radiogroup"
        aria-label={t('bill.fields.type')}
        sx={{
          display: 'flex',
          gap: 1,
          flexWrap: { xs: 'nowrap', sm: 'wrap' },
          overflowX: 'auto',
          scrollbarWidth: 'none',
          // the focus ring of the first one isn't cut off
          p: '3px',
          m: '-3px',
          mb: 2.5,
        }}
      >
        {types.map((meta) => {
          const selected = meta.type === type;
          return (
            <ButtonBase
              key={meta.type}
              role="radio"
              aria-checked={selected}
              onClick={() => onTypeChange(meta.type)}
              sx={(theme) => ({
                flexShrink: 0,
                gap: 1,
                minHeight: 44,
                pl: 1.25,
                pr: 2,
                borderRadius: 22,
                border: 1,
                borderColor: selected
                  ? theme.vars.palette[meta.color].main
                  : theme.vars.palette.divider,
                bgcolor: selected
                  ? `rgba(${theme.vars.palette[meta.color].mainChannel} / 0.12)`
                  : theme.vars.palette.background.paper,
                fontWeight: 600,
                transition: 'border-color .15s, background-color .15s',
                '&:focus-visible': {
                  outline: `2px solid ${theme.vars.palette.primary.main}`,
                  outlineOffset: 2,
                },
                '& .kind': {
                  display: 'flex',
                  ...categoryInk(theme, meta.color),
                },
              })}
            >
              <span className="kind" aria-hidden>
                {meta.icon}
              </span>
              {t(`bill.types.${meta.type}`)}
            </ButtonBase>
          );
        })}
      </Box>

      <Card sx={{ p: { xs: 2, sm: 3 } }}>
        <Typography
          variant="body2"
          sx={{
            color: 'text.secondary',
            mb: 2.5,
          }}
        >
          {t(`bill.add.describe.${type}`, {
            // only the refuels name the volume
            context: type === BillType.FUEL ? units.volume.context : undefined,
          })}
        </Typography>
        {/* keyed, so switching the type starts with a fresh form */}
        {type === BillType.FUEL && (
          <FuelForm key={type + queued?.id} carId={carId} queued={queued} />
        )}
        {type === BillType.CHARGING && (
          <ChargingForm key={type + queued?.id} carId={carId} queued={queued} />
        )}
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
