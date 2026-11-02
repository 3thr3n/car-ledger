import {
  Box,
  Button,
  Collapse,
  Divider,
  IconButton,
  Stack,
  Theme,
  Tooltip,
  Typography,
} from '@mui/material';
import AttachFileRounded from '@mui/icons-material/AttachFileRounded';
import DeleteOutlineRounded from '@mui/icons-material/DeleteOutlineRounded';
import EditOutlined from '@mui/icons-material/EditOutlined';
import EventRounded from '@mui/icons-material/EventRounded';
import ExpandMoreRounded from '@mui/icons-material/ExpandMoreRounded';
import LogoutRounded from '@mui/icons-material/LogoutRounded';
import TaskAltRounded from '@mui/icons-material/TaskAltRounded';
import ReportProblemRounded from '@mui/icons-material/ReportProblemRounded';
import InfoOutlined from '@mui/icons-material/InfoOutlined';
import { Fragment, ReactNode, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Modification } from '@/generated';
import { CLUSTER_AMBER, displayFont } from '@/theme';
import { useFormat } from '@/utils/format';
import ModificationDocuments from './ModificationDocuments';
import { billText, needsInspection, registrationOpen } from './approval';

export interface ModificationListProps {
  carId: number;
  modifications: Modification[];
  onEdit: (modification: Modification) => void;
  onRemove: (modification: Modification) => void;
  onDelete: (modification: Modification) => void;
  /** without the actions, for a viewer */
  readOnly?: boolean;
}

function Detail({ label, children }: { label: string; children: ReactNode }) {
  return (
    <Box>
      <Typography variant="caption" sx={{ color: 'text.secondary' }}>
        {label}
      </Typography>
      <Typography variant="body2" sx={{ whiteSpace: 'pre-line' }}>
        {children}
      </Typography>
    </Box>
  );
}

type Papers = 'complete' | 'inspection' | 'registration' | 'removed';

/** what is still open for the papers of a part */
const papersOf = (modification: Modification): Papers =>
  !modification.installed
    ? 'removed'
    : modification.inspectionOpen
      ? 'inspection'
      : registrationOpen(modification)
        ? 'registration'
        : 'complete';

/** the light of the papers: amber for the Abnahme, blue for the entry */
const lampSx = (papers: Papers) => (theme: Theme) =>
  ({
    complete: {
      color: '#15803d',
      ...theme.applyStyles('dark', { color: '#4ade80' }),
    },
    inspection: {
      color: theme.vars.palette.warning.main,
      ...theme.applyStyles('dark', { color: CLUSTER_AMBER }),
    },
    registration: { color: theme.vars.palette.info.main },
    removed: { color: theme.vars.palette.text.secondary },
  })[papers];

/**
 * The modifications with their approval and what is still open; the details
 * with the conditions and the documents open below each one
 */
export default function ModificationList({
  carId,
  modifications,
  onEdit,
  onRemove,
  onDelete,
  readOnly = false,
}: ModificationListProps) {
  const { t } = useTranslation();
  const format = useFormat();
  const [open, setOpen] = useState<number>();

  /** e.g. "12.04.2025 · 41,000 km" */
  const when = (date?: string, odometer?: number) =>
    [date && format.date(date), odometer != null && format.distance(odometer)]
      .filter(Boolean)
      .join(' · ');

  return (
    <Stack>
      {modifications.map((modification, index) => {
        const expanded = open === modification.id;
        const installed = when(
          modification.installedDate,
          modification.installedOdometer,
        );
        const removed = when(
          modification.removedDate,
          modification.removedOdometer,
        );
        const inspection = needsInspection(modification.approvalType);
        const papers = papersOf(modification);

        return (
          <Fragment key={modification.id}>
            {index > 0 && <Divider />}
            <Box data-testid="modification" sx={{ py: 1.5 }}>
              <Stack
                direction="row"
                spacing={1.5}
                sx={{ alignItems: 'flex-start' }}
              >
                {/* whether its papers would pass a roadside check */}
                <Box
                  aria-hidden
                  sx={[lampSx(papers), { display: 'flex', pt: 0.25 }]}
                >
                  {papers === 'complete' ? (
                    <TaskAltRounded />
                  ) : papers === 'inspection' ? (
                    <ReportProblemRounded />
                  ) : papers === 'registration' ? (
                    <InfoOutlined />
                  ) : (
                    <LogoutRounded />
                  )}
                </Box>
                <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                  <Typography sx={{ fontWeight: 600 }}>
                    {modification.part}
                    {modification.manufacturer && (
                      <Typography
                        component="span"
                        sx={{ color: 'text.secondary', fontWeight: 400 }}
                      >
                        {' '}
                        {modification.manufacturer}
                      </Typography>
                    )}
                  </Typography>
                  {modification.installed && (
                    <Typography
                      variant="body2"
                      sx={[lampSx(papers), { fontWeight: 600 }]}
                    >
                      {t(
                        papers === 'inspection'
                          ? 'modification.open.inspection'
                          : papers === 'registration'
                            ? 'modification.open.registration'
                            : 'modification.papersComplete',
                      )}
                    </Typography>
                  )}
                  {/* the approval like the marking stamped on the part */}
                  <Box
                    component="span"
                    className="tabular"
                    sx={{
                      display: 'inline-block',
                      maxWidth: '100%',
                      mt: 0.75,
                      px: 0.75,
                      py: 0.125,
                      border: 1.5,
                      borderColor: 'text.secondary',
                      borderRadius: '4px',
                      fontFamily: displayFont,
                      fontWeight: 650,
                      fontSize: '0.875rem',
                      letterSpacing: '0.04em',
                      color: 'text.secondary',
                    }}
                  >
                    {/* a break only between the type and the number */}
                    <Box component="span" sx={{ whiteSpace: 'nowrap' }}>
                      {t(
                        `modification.approvalTypes.${modification.approvalType}`,
                      )}
                    </Box>
                    {modification.approvalNumber && (
                      <>
                        {' · '}
                        <Box component="span" sx={{ whiteSpace: 'nowrap' }}>
                          {modification.approvalNumber}
                        </Box>
                      </>
                    )}
                  </Box>
                  <Stack spacing={0.25} sx={{ mt: 0.75 }}>
                    {installed && (
                      <Stack
                        direction="row"
                        spacing={0.75}
                        sx={{
                          alignItems: 'center',
                          color: 'text.secondary',
                          '& svg': { fontSize: 16 },
                        }}
                      >
                        <EventRounded />
                        <Typography variant="body2" sx={{ color: 'inherit' }}>
                          {t('modification.installedAt', { when: installed })}
                        </Typography>
                      </Stack>
                    )}
                    {!modification.installed && (
                      <Stack
                        direction="row"
                        spacing={0.75}
                        sx={{
                          alignItems: 'center',
                          color: 'text.secondary',
                          '& svg': { fontSize: 16 },
                        }}
                      >
                        <LogoutRounded />
                        <Typography variant="body2" sx={{ color: 'inherit' }}>
                          {t('modification.removedAt', { when: removed })}
                        </Typography>
                      </Stack>
                    )}
                  </Stack>
                </Box>
                {!readOnly && (
                  <Stack direction="row" sx={{ flexShrink: 0 }}>
                    {modification.installed && (
                      <Tooltip title={t('modification.remove.button')}>
                        <IconButton
                          size="small"
                          aria-label={t('modification.remove.button')}
                          onClick={() => onRemove(modification)}
                        >
                          <LogoutRounded fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                    <Tooltip title={t('button.edit')}>
                      <IconButton
                        size="small"
                        aria-label={t('button.edit')}
                        onClick={() => onEdit(modification)}
                      >
                        <EditOutlined fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title={t('button.delete')}>
                      <IconButton
                        size="small"
                        aria-label={t('button.delete')}
                        onClick={() => onDelete(modification)}
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
                onClick={() => setOpen(expanded ? undefined : modification.id)}
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
                {t('modification.details')}
                {modification.documents > 0 && (
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
                    <span>{modification.documents}</span>
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
                  {modification.conditions && (
                    <Detail label={t('modification.fields.conditions')}>
                      {modification.conditions}
                    </Detail>
                  )}
                  {inspection && (
                    <Detail label={t('modification.sections.inspection')}>
                      {modification.inspectionDate
                        ? [
                            format.date(modification.inspectionDate),
                            modification.testingBody,
                          ]
                            .filter(Boolean)
                            .join(' · ')
                        : t('modification.open.inspection')}
                      {'\n'}
                      {t(
                        modification.registered
                          ? 'modification.registered'
                          : 'modification.notRegistered',
                      )}
                    </Detail>
                  )}
                  {modification.bills.length > 0 && (
                    <Detail label={t('modification.fields.bills')}>
                      {modification.bills
                        .map((bill) => billText(bill, format, t))
                        .join('\n')}
                      {modification.bills.length > 1 &&
                        `\n${t('modification.billsTotal', {
                          total: format.money(
                            modification.bills.reduce(
                              (sum, bill) => sum + bill.total,
                              0,
                            ),
                          ),
                        })}`}
                    </Detail>
                  )}
                  {modification.notes && (
                    <Detail label={t('modification.fields.notes')}>
                      {modification.notes}
                    </Detail>
                  )}
                  <ModificationDocuments
                    carId={carId}
                    modificationId={modification.id}
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
