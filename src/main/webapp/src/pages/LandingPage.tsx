import {
  Box,
  Button,
  Card,
  CardContent,
  Container,
  Grid,
  Typography,
} from '@mui/material';

export default function LandingPage() {
  return (
    <Box
      sx={{
        bgcolor: 'background.default',
        color: 'text.primary',
      }}
    >
      <Box component="section" sx={{ py: 6 }}>
        <Container>
          <Typography variant="h5" fontWeight={600} gutterBottom>
            Collect fuel receipts. See your true cost per kilometer.
          </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
            Upload or enter fuel bills and CarLedger organizes them, shows
            trends and helps you plan.
          </Typography>
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
            <Button variant="contained" color="primary" href="#features">
              Learn more
            </Button>
            <Button variant="outlined" color="primary" href="#get-started">
              Get started
            </Button>
          </Box>
        </Container>
      </Box>

      <Box id="features" sx={{ py: 5, bgcolor: 'background.paper' }}>
        <Container>
          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 4 }}>
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Organized Fuel Records
                  </Typography>
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
                  <Typography variant="h6" gutterBottom>
                    Cost Insights
                  </Typography>
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
                  <Typography variant="h6" gutterBottom>
                    Multiple Cars
                  </Typography>
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
            `linear-gradient(180deg, transparent, ${theme.palette.primary.light}11)`,
        }}
      >
        <Container>
          <Typography variant="h6" gutterBottom>
            Planned — Maintenance & Costs
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Soon you'll be able to add maintenance entries (one-off, monthly,
            yearly) per car — oil changes, tyres, inspections — and the app will
            combine them with fuel to show a complete ownership cost.
          </Typography>
        </Container>
      </Box>

      <Box id="get-started" sx={{ py: 5 }}>
        <Container>
          <Typography variant="h6" gutterBottom>
            Get started
          </Typography>
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
