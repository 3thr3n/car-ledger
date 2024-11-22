import { Button, styled } from '@mui/material';
import { CloudUpload } from '@mui/icons-material';

import Papa, { ParseResult } from 'papaparse';
import { toast } from 'react-toastify';

const VisuallyHiddenInput = styled('input')({
  clipPath: 'inset(50%)',
  height: 1,
  overflow: 'hidden',
  position: 'absolute',
  bottom: 0,
  left: 0,
  whiteSpace: 'nowrap',
  width: 1,
});

export interface CsvUploadProps {
  onParseComplete: (data: string[], file: File) => void;
}

export function CsvUploadButton(props: CsvUploadProps) {
  const handleUploadFile = (file?: File) => {
    if (!file) return;

    // @ts-expect-error stream or file, but it ignores that file is possible
    Papa.parse(file, {
      delimiter: ',',
      download: false,
      header: true,
      preview: 1,
      skipEmptyLines: true,
      complete(results: ParseResult<any>, file: File) {
        if (!results.meta.fields) {
          toast.warn('Could not find header column.');
          return;
        }
        props.onParseComplete(results.meta.fields, file);
      },
    });
  };

  return (
    <Button
      component="label"
      variant="outlined"
      tabIndex={-1}
      startIcon={<CloudUpload />}
    >
      Upload Files
      <VisuallyHiddenInput
        type="file"
        accept={'.csv'}
        onChange={(event) => handleUploadFile(event.target.files?.[0])}
      />
    </Button>
  );
}
