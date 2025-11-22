import { getStatsMinimalOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { ExpandMore } from '@mui/icons-material';
import {
  Accordion,
  AccordionActions,
  AccordionDetails,
  AccordionSummary,
  Box,
  Fab,
  Grid,
  Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import HighLowCard from './HighLowCard';
import MainStatCard from './MainStatCard';
import { toast } from 'react-toastify';

export interface MinimalStatisticsProps {
  carId: number;
  expandend: boolean;
  setExpanded: (expand: boolean) => void;
}

export function MinimalStatistics(props: MinimalStatisticsProps) {
  const { data } = useQuery({
    ...getStatsMinimalOptions({
      client: localClient,
      path: {
        carId: props.carId,
      },
    }),
    retry: false,
  });

  const onClickMore = () => {
    toast.info('Will be implemented later');
  };

  return (
    <Box width={'100%'} mb={2} mt={6} px={2}>
      <Accordion
        className="background-card"
        expanded={props.expandend}
        onChange={() => props.setExpanded(!props.expandend)}
      >
        <AccordionSummary expandIcon={<ExpandMore />}>
          <Typography component="span">
            Statisitics for car id {props.carId}
          </Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Grid
            container
            spacing={2}
            columns={{ xs: 3, sm: 6, md: 9, lg: 12, xl: 18 }}
            sx={{
              justifyContent: 'center',
              alignItems: 'center',
            }}
          >
            <MainStatCard
              title="Total billed"
              total={data?.total}
              postfix="€"
            />
            <MainStatCard
              title="Average fuel consumtion"
              total={data?.avgFuelConsumption}
              postfix="L/100km"
            />
            <MainStatCard
              title="Average distance"
              total={data?.avgDistance}
              postfix="km"
            />
            <HighLowCard
              highLow={data?.minMaxFuelConsumption}
              title="Fuel used"
              lowIsGood={true}
              postfix="L/100km"
            />
          </Grid>
        </AccordionDetails>
        <AccordionActions>
          <Fab
            variant="extended"
            color="primary"
            size="small"
            sx={{
              mr: 2,
              mb: 1,
              padding: 2,
            }}
            onClick={onClickMore}
          >
            More
          </Fab>
        </AccordionActions>
      </Accordion>
    </Box>
  );
}
