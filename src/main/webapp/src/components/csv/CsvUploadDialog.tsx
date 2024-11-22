import { Card, CardContent, Dialog } from '@mui/material';
import useCsvStore from '@/store/CsvStore';
import CsvUploadStepper from '@/components/csv/CsvUploadStepper';

export default function CsvUploadDialog() {
  const open = useCsvStore((state) => state.open);
  const closeDialog = useCsvStore((state) => state.closeDialog);

  const handleClose = () => {
    closeDialog();
  };

  return (
    <Dialog onClose={handleClose} open={open} maxWidth={'sm'}>
      <Card
        sx={{
          width: 400,
        }}
      >
        <CardContent>
          <CsvUploadStepper close={handleClose} />
        </CardContent>
      </Card>
    </Dialog>
  );
}
