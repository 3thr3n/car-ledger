import { Box, Card, MenuItem, Select, Stack, Typography } from '@mui/material';

export interface YearSelectionProps {
  years: number[];
  selectedYear: number;
  setSelectedYear: (toSelect: number) => void;
  isMobile?: boolean;
}

export default function YearSelection({
  years,
  selectedYear,
  setSelectedYear,
  isMobile,
}: YearSelectionProps) {
  if (isMobile) {
    return (
      <Select
        fullWidth
        value={selectedYear}
        onChange={(e) => setSelectedYear(Number(e.target.value))}
        sx={{ mb: 2 }}
      >
        <MenuItem value={-1}>All</MenuItem>
        {years.map((y) => (
          <MenuItem key={y} value={y}>
            {y}
          </MenuItem>
        ))}
      </Select>
    );
  }

  return (
    <Box sx={{ minWidth: 140 }}>
      <Typography variant="subtitle1" gutterBottom>
        Years
      </Typography>
      <Stack spacing={1}>
        <Card
          sx={{
            p: 1,
            textAlign: 'center',
            cursor: 'pointer',
            bgcolor: -1 === selectedYear ? 'primary.main' : 'background.paper',
            color: -1 === selectedYear ? 'white' : 'text.primary',
            transition: '0.2s',
          }}
          onClick={() => setSelectedYear(-1)}
        >
          <Typography variant="body2">All</Typography>
        </Card>
        {years.map((y) => (
          <Card
            key={y}
            sx={{
              p: 1,
              textAlign: 'center',
              cursor: 'pointer',
              bgcolor: y === selectedYear ? 'primary.main' : 'background.paper',
              color: y === selectedYear ? 'white' : 'text.primary',
              transition: '0.2s',
            }}
            onClick={() => setSelectedYear(y)}
          >
            <Typography variant="body2">{y}</Typography>
          </Card>
        ))}
      </Stack>
    </Box>
  );
}
