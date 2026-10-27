import {
  Box,
  Button,
  Collapse,
  Divider,
  IconButton,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import AttachFileRounded from '@mui/icons-material/AttachFileRounded';
import DeleteOutlineRounded from '@mui/icons-material/DeleteOutlineRounded';
import EditOutlined from '@mui/icons-material/EditOutlined';
import EventRepeatRounded from '@mui/icons-material/EventRepeatRounded';
import ExpandMoreRounded from '@mui/icons-material/ExpandMoreRounded';
import dayjs from 'dayjs';
import { Fragment, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { TechnicalInspection } from '@/generated';
import { useFormat } from '@/utils/format';
import InspectionFiles from './InspectionFiles';
import InspectionResultChip from './InspectionResultChip';

export interface InspectionListProps {
  carId: number;
  inspections: TechnicalInspection[];
  onEdit: (inspection: TechnicalInspection) => void;
  onDelete: (inspection: TechnicalInspection) => void;
  /** without the actions, for a viewer */
  readOnly?: boolean;
}

/** the HUs, the latest first, the defects and the report below each one */
export default function InspectionList({
  carId,
  inspections,
  onEdit,
  onDelete,
  readOnly = false,
}: InspectionListProps) {
  const { t } = useTranslation();
  const format = useFormat();
  const [open, setOpen] = useState<number>();

  return (
    <Stack>
      {inspections.map((inspection, index) => {
        const expanded = open === inspection.id;
        const details = [
          inspection.testingBody,
          inspection.odometer != null && format.distance(inspection.odometer),
        ]
          .filter(Boolean)
          .join(' · ');

        return (
          <Fragment key={inspection.id}>
            {index > 0 && <Divider />}
            <Box data-testid="inspection" sx={{ py: 1.5 }}>
              <Stack
                direction="row"
                spacing={1.5}
                sx={{ alignItems: 'flex-start' }}
              >
                <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                  <Stack
                    direction="row"
                    spacing={1}
                    useFlexGap
                    sx={{ alignItems: 'center', flexWrap: 'wrap', mb: 0.5 }}
                  >
                    <Typography sx={{ fontWeight: 600 }}>
                      {format.date(inspection.date)}
                    </Typography>
                    <InspectionResultChip result={inspection.result} />
                  </Stack>
                  {details && (
                    <Typography
                      variant="body2"
                      sx={{ color: 'text.secondary' }}
                    >
                      {details}
                    </Typography>
                  )}
                  {inspection.nextDue && (
                    <Stack
                      direction="row"
                      spacing={0.75}
                      sx={{
                        alignItems: 'center',
                        color: 'text.secondary',
                        mt: 0.25,
                        '& svg': { fontSize: 16 },
                      }}
                    >
                      <EventRepeatRounded />
                      <Typography variant="body2" sx={{ color: 'inherit' }}>
                        {t('inspection.nextDue', {
                          month: new Intl.DateTimeFormat(format.locale, {
                            month: 'long',
                            year: 'numeric',
                          }).format(dayjs(inspection.nextDue).toDate()),
                        })}
                      </Typography>
                    </Stack>
                  )}
                </Box>
                {!readOnly && (
                  <Stack direction="row" sx={{ flexShrink: 0 }}>
                    <Tooltip title={t('button.edit')}>
                      <IconButton
                        size="small"
                        aria-label={t('button.edit')}
                        onClick={() => onEdit(inspection)}
                      >
                        <EditOutlined fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title={t('button.delete')}>
                      <IconButton
                        size="small"
                        aria-label={t('button.delete')}
                        onClick={() => onDelete(inspection)}
                      >
                        <DeleteOutlineRounded fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </Stack>
                )}
              </Stack>

              <Button
                size="small"
                color="inherit"
                onClick={() => setOpen(expanded ? undefined : inspection.id)}
                aria-expanded={expanded}
                endIcon={
                  <ExpandMoreRounded
                    sx={{
                      transform: expanded ? 'rotate(180deg)' : undefined,
                      transition: 'transform 150ms',
                    }}
                  />
                }
                sx={{ mt: 0.5, ml: -0.75, color: 'text.secondary' }}
              >
                {t('inspection.details')}
                {inspection.documents > 0 && (
                  <Stack
                    component="span"
                    direction="row"
                    spacing={0.25}
                    sx={{
                      alignItems: 'center',
                      ml: 1,
                      '& svg': { fontSize: 15 },
                    }}
                  >
                    <AttachFileRounded />
                    <span>{inspection.documents}</span>
                  </Stack>
                )}
              </Button>
              <Collapse in={expanded} unmountOnExit>
                <Stack
                  spacing={2}
                  sx={{
                    mt: 1,
                    p: 2,
                    borderRadius: 2,
                    bgcolor: 'action.hover',
                  }}
                >
                  {inspection.defects && (
                    <Box>
                      <Typography
                        variant="caption"
                        sx={{ color: 'text.secondary' }}
                      >
                        {t('inspection.fields.defects')}
                      </Typography>
                      <Typography
                        variant="body2"
                        sx={{ whiteSpace: 'pre-line' }}
                      >
                        {inspection.defects}
                      </Typography>
                    </Box>
                  )}
                  <InspectionFiles
                    carId={carId}
                    inspectionId={inspection.id}
                    readOnly={readOnly}
                  />
                </Stack>
              </Collapse>
            </Box>
          </Fragment>
        );
      })}
    </Stack>
  );
}
