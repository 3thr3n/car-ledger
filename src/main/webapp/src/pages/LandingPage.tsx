import {
  Box,
  Card,
  CardContent,
  Container,
  Grid,
  Typography,
} from '@mui/material';
import CarLedgerSubPageHeader from '@/components/CarLedgerSubPageHeader';

export default function LandingPage() {
  return (
    <Box>
      <Box component="section" sx={{ pt: 6 }}>
        <Container>
          <Typography variant="h5" fontWeight={600} gutterBottom>
            Collect fuel receipts. See your true cost per kilometer.
          </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
            Upload or enter fuel bills and CarLedger organizes them, shows
            trends and helps you plan.
          </Typography>
        </Container>
      </Box>

      <Box id="features" sx={{ py: 5 }}>
        <Container>
          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 4 }}>
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardContent>
                  <CarLedgerSubPageHeader
                    title="Organized Fuel Records"
                    isCardHeader
                  />
                  <Typography variant="body2" color="text.secondary">
                    Store date, liters, price, odometer and receipt image.
                    Search and filter by car or date range.
                  </Typography>
                </CardContent>
              </Card>
            </Grid>

            <Grid size={{ xs: 12, md: 4 }}>
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardContent>
                  <CarLedgerSubPageHeader title="Cost Insights" isCardHeader />
                  <Typography variant="body2" color="text.secondary">
                    Automatic cost-per-km calculations and monthly spending
                    summaries so you spot trends fast.
                  </Typography>
                </CardContent>
              </Card>
            </Grid>

            <Grid size={{ xs: 12, md: 4 }}>
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardContent>
                  <CarLedgerSubPageHeader title="Multiple Cars" isCardHeader />
                  <Typography variant="body2" color="text.secondary">
                    Add one or many cars and keep each vehicle's history
                    separate for accurate comparisons.
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </Container>
      </Box>

      <Box
        id="future"
        sx={{
          py: 5,
          background: (theme) =>
            `linear-gradient(90deg, ${theme.palette.secondary.dark}, transparent)`,
        }}
      >
        <Container>
          <CarLedgerSubPageHeader
            title="Planned — Maintenance & Costs"
            isCardHeader
          />
          <Typography variant="body2" color="text.secondary">
            Soon you'll be able to add maintenance entries (one-off, monthly,
            yearly) per car — oil changes, tyres, inspections — and the app will
            combine them with fuel to show a complete ownership cost.
          </Typography>
        </Container>
      </Box>

      <Box id="get-started" sx={{ py: 5 }}>
        <Container>
          <CarLedgerSubPageHeader title="Get started" isCardHeader />
          <Box component="ol" sx={{ pl: 3, mb: 2 }}>
            <Typography component="li">
              Create a free account (coming soon).
            </Typography>
            <Typography component="li">
              Add your car(s) and upload fuel bills.
            </Typography>
            <Typography component="li">
              Explore spending reports and export data.
            </Typography>
          </Box>
          <Typography variant="body2" color="text.secondary">
            This project is an early-stage SPA built with React + TypeScript and
            will integrate with a Quarkus backend for secure data storage.
          </Typography>
        </Container>
      </Box>
    </Box>
  );
}
