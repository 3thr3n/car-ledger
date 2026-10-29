import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  LinearProgress,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import DownloadRounded from '@mui/icons-material/DownloadRounded';
import ExpandMoreRounded from '@mui/icons-material/ExpandMoreRounded';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { Device, FirmwareManifest, getFirmwareFile } from '@/generated';
import {
  getFirmwareManifestOptions,
  renewTrackerTokenMutation,
} from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import {
  configImage,
  hasErrors,
  initialSettings,
  RESERVED,
  settingsSchema,
  SettingsErrors,
  storedSettings,
  TrackerSettings,
  validateSettings,
} from './configImage';
import {
  FlashError,
  FlashMode,
  flashTracker,
  FlashStep,
  LayoutProblem,
  Monitor,
  readSettings,
} from './flasher';
import { SettingsField } from './SettingsFields';
import { useErrorText } from './useSettingsTexts';

export interface TrackerFlashDialogProps {
  open: boolean;
  carId: number;
  tracker: Device;
  /** right after creating the tracker, else a new one can be issued */
  token?: string;
  onClose: () => void;
}

/**
 * Flashes the firmware and its settings onto an ESP32 over USB with Web
 * Serial, no other tool needed. What it offers and the form of the settings
 * come from the manifest of the firmware (docs/TRACKER_FIRMWARE.md).
 */
export default function TrackerFlashDialog({
  open,
  onClose,
  ...props
}: TrackerFlashDialogProps) {
  return (
    <Dialog open={open} maxWidth="sm" fullWidth>
      {/* the WLAN passwords are gone after closing */}
      {open && <FlashForm {...props} onClose={onClose} />}
    </Dialog>
  );
}

type State =
  | { phase: 'form' }
  | { phase: 'flashing'; step: FlashStep; percent: number }
  | { phase: 'done'; mode: FlashMode }
  | { phase: 'error'; message: string };

/** what the dialog offers, by the capability of the manifest */
const ACTIONS: [string, FlashMode][] = [
  ['install', 'install'],
  ['update-firmware', 'firmware'],
  ['settings', 'settings'],
];

/** a layout to confirm before the firmware only is written */
interface LayoutQuestion {
  problem: LayoutProblem;
  answer: (write: boolean) => void;
}

const origin = () =>
  typeof window === 'undefined' ? '' : window.location.origin;

type FormProps = Omit<TrackerFlashDialogProps, 'open'>;

function FlashForm(props: FormProps) {
  const { t } = useTranslation();
  const { data: manifest, error } = useQuery(
    getFirmwareManifestOptions({ client: localClient }),
  );

  if (manifest) return <Flasher {...props} manifest={manifest} />;
  return (
    <>
      <DialogTitle>
        {t('tracker.flash.title', { name: props.tracker.name })}
      </DialogTitle>
      <DialogContent>
        {error ? (
          <Alert severity="error">{t('tracker.flash.noManifest')}</Alert>
        ) : (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
            <CircularProgress aria-label={t('tracker.flash.loading')} />
          </Box>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button color="inherit" onClick={props.onClose}>
          {t('button.cancel')}
        </Button>
      </DialogActions>
    </>
  );
}

function Flasher({
  carId,
  tracker,
  token: createdToken,
  onClose,
  manifest,
}: FormProps & { manifest: FirmwareManifest }) {
  const { t } = useTranslation();
  const schema = settingsSchema(manifest.config);
  const [settings, setSettings] = useState<TrackerSettings>(() =>
    initialSettings(
      schema,
      `${origin()}/api/device/points`,
      createdToken ?? '',
    ),
  );
  const [touched, setTouched] = useState(false);
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const [renewing, setRenewing] = useState(false);
  const [state, setState] = useState<State>({ phase: 'form' });
  const [layout, setLayout] = useState<LayoutQuestion>();
  const [output, setOutput] = useState('');
  const monitor = useRef<Monitor>(undefined);
  const outputBox = useRef<HTMLPreElement>(null);

  const renew = useMutation({
    ...renewTrackerTokenMutation({ client: localClient }),
    onSuccess: (renewed) => {
      setSettings((s) => ({ ...s, token: renewed.token }));
      toast.success(t('tracker.flash.renewed'));
    },
    onError: (e) => toast.error(errorMessage(e, t('errors.backend'))),
  });

  // the monitor stops with the dialog
  useEffect(() => () => void monitor.current?.stop(), []);

  useEffect(() => {
    const box = outputBox.current;
    if (box) box.scrollTop = box.scrollHeight;
  }, [output]);

  const errors: SettingsErrors = validateSettings(settings, manifest.config);
  const shown = touched ? errors : undefined;
  const errorText = useErrorText(shown);
  const busy = state.phase === 'flashing';
  const actions = ACTIONS.filter(([capability]) =>
    manifest.capabilities.includes(capability),
  ).map(([, mode]) => mode);
  const canRead = manifest.capabilities.includes('read-settings');
  const required = new Set(schema.required ?? []);
  const fields = Object.entries(schema.properties);
  const advanced = fields.filter(
    ([name, field]) => !RESERVED.includes(name) && field.advanced,
  );

  const onStep = (step: FlashStep) =>
    setState((s) => ({
      phase: 'flashing',
      step,
      percent: s.phase === 'flashing' ? s.percent : 0,
    }));
  const onOutput = (text: string) =>
    setOutput((o) => (o + text).slice(-20_000));
  const failed = (e: unknown) => {
    if (e instanceof FlashError && e.code === 'cancelled') {
      // the tracker runs as before, the form stays for another try
      setState({ phase: 'form' });
      toast.info(t('tracker.flash.cancelled'));
      return;
    }
    setState({
      phase: 'error',
      message:
        e instanceof FlashError
          ? t(`tracker.flash.errors.${e.code}`, { detail: e.detail })
          : t('tracker.flash.errors.write', { detail: String(e) }),
    });
  };

  /** fills the form with the settings on the tracker */
  const read = async () => {
    let port: SerialPort;
    try {
      port = await navigator.serial.requestPort();
    } catch {
      return;
    }
    setOutput('');
    setState({ phase: 'flashing', step: 'connecting', percent: 0 });
    try {
      const stored = await readSettings(port, manifest, { onStep, onOutput });
      setState({ phase: 'form' });
      if (stored) {
        setSettings((current) =>
          storedSettings(schema, stored, current, createdToken),
        );
        toast.success(t('tracker.flash.readDone'));
      } else {
        toast.info(t('tracker.flash.readNone'));
      }
    } catch (e) {
      failed(e);
    }
  };

  const start = async (mode: FlashMode) => {
    // the firmware only keeps the settings on the tracker, no form needed
    setTouched(mode !== 'firmware');
    if (mode !== 'firmware' && hasErrors(errors)) {
      // show the errors folded away as well
      if (advanced.some(([name]) => hasErrorAt(errors, name))) {
        setAdvancedOpen(true);
      }
      return;
    }

    let port: SerialPort;
    try {
      // needs the click, before anything else is awaited
      port = await navigator.serial.requestPort();
    } catch {
      // no port chosen
      return;
    }

    setOutput('');
    setState({ phase: 'flashing', step: 'connecting', percent: 0 });
    try {
      monitor.current = await flashTracker(port, {
        manifest,
        mode,
        config:
          mode === 'firmware'
            ? undefined
            : configImage(settings, manifest.config),
        confirmLayout: (problem) =>
          new Promise((answer) => setLayout({ problem, answer })),
        download: async (path) => {
          const { data } = await getFirmwareFile({
            client: localClient,
            path: { file: path },
            throwOnError: true,
          });
          return new Uint8Array(await (data as Blob).arrayBuffer());
        },
        onStep,
        onProgress: (percent) =>
          setState((s) => (s.phase === 'flashing' ? { ...s, percent } : s)),
        onOutput,
      });
      setState({ phase: 'done', mode });
    } catch (e) {
      failed(e);
    }
  };

  const answerLayout = (write: boolean) => {
    layout?.answer(write);
    setLayout(undefined);
  };

  const close = async () => {
    await monitor.current?.stop();
    monitor.current = undefined;
    onClose();
  };

  const field = ([name, f]: (typeof fields)[number]) => {
    if (name === 'url') {
      return (
        <TextField
          key={name}
          label={t('tracker.flash.url')}
          value={settings.url}
          onChange={(e) => setSettings((s) => ({ ...s, url: e.target.value }))}
          error={!!shown?.url}
          helperText={
            errorText('url') ??
            (settings.url.trim().startsWith('http://')
              ? t('tracker.flash.httpWarning')
              : t('tracker.flash.urlHelp'))
          }
          required
        />
      );
    }
    if (name === 'token') {
      return (
        <Stack
          key={name}
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1}
          sx={{ alignItems: { sm: 'flex-start' } }}
        >
          <TextField
            label={t('tracker.token')}
            value={settings.token}
            onChange={(e) =>
              setSettings((s) => ({ ...s, token: e.target.value }))
            }
            error={!!shown?.token}
            helperText={
              errorText('token') ??
              (createdToken ? undefined : t('tracker.flash.tokenHelp'))
            }
            slotProps={{
              input: {
                readOnly: !!createdToken,
                sx: { fontFamily: 'monospace' },
              },
              htmlInput: { 'data-testid': 'flash-token' },
            }}
            required
            fullWidth
          />
          {!createdToken && (
            <Button
              variant="outlined"
              onClick={() => setRenewing(true)}
              disabled={renew.isPending}
              sx={{ flexShrink: 0, mt: { sm: 1 } }}
            >
              {t('tracker.flash.renew')}
            </Button>
          )}
        </Stack>
      );
    }
    return (
      <SettingsField
        key={name}
        name={name}
        field={f}
        value={settings.values[name]}
        required={required.has(name)}
        errors={shown}
        onChange={(value) =>
          setSettings((s) => ({
            ...s,
            values: { ...s.values, [name]: value },
          }))
        }
      />
    );
  };

  return (
    <>
      <DialogTitle>
        {t('tracker.flash.title', { name: tracker.name })}
      </DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {state.phase === 'form' && (
            <Typography variant="body2" sx={{ color: 'text.secondary' }}>
              {t('tracker.flash.text')}
            </Typography>
          )}
          {actions.length === 0 && (
            <Alert severity="warning">{t('tracker.flash.noActions')}</Alert>
          )}
          {canRead && state.phase === 'form' && (
            <Stack
              direction={{ xs: 'column', sm: 'row' }}
              spacing={1}
              sx={{ alignItems: { sm: 'center' } }}
            >
              <Typography
                variant="body2"
                sx={{ color: 'text.secondary', flexGrow: 1 }}
              >
                {t('tracker.flash.readHint')}
              </Typography>
              <Button
                variant="outlined"
                size="small"
                startIcon={<DownloadRounded />}
                onClick={read}
                sx={{ flexShrink: 0, alignSelf: { xs: 'flex-start' } }}
              >
                {t('tracker.flash.read')}
              </Button>
            </Stack>
          )}

          {(state.phase === 'form' || state.phase === 'error') && (
            <>
              {fields
                .filter(([name, f]) => RESERVED.includes(name) || !f.advanced)
                .map(field)}

              {advanced.length > 0 && (
                <Accordion
                  disableGutters
                  variant="outlined"
                  expanded={advancedOpen}
                  onChange={(_, expanded) => setAdvancedOpen(expanded)}
                >
                  <AccordionSummary expandIcon={<ExpandMoreRounded />}>
                    {t('tracker.flash.advanced')}
                  </AccordionSummary>
                  <AccordionDetails>
                    <Stack spacing={2}>{advanced.map(field)}</Stack>
                  </AccordionDetails>
                </Accordion>
              )}

              {shown?.size && (
                <Alert severity="error">{errorText('size')}</Alert>
              )}
            </>
          )}

          {state.phase === 'flashing' && (
            <Box>
              <Typography sx={{ mb: 1 }}>
                {t(`tracker.flash.steps.${state.step}`)}
              </Typography>
              <LinearProgress
                variant={
                  state.step === 'writing' ? 'determinate' : 'indeterminate'
                }
                value={state.percent}
                aria-label={t(`tracker.flash.steps.${state.step}`)}
              />
              <Typography
                variant="body2"
                sx={{ color: 'text.secondary', mt: 1 }}
              >
                {t('tracker.flash.keepConnected')}
              </Typography>
            </Box>
          )}
          {state.phase === 'done' && (
            <Alert severity="success">
              {t(
                state.mode === 'firmware'
                  ? 'tracker.flash.doneFirmware'
                  : 'tracker.flash.done',
              )}
            </Alert>
          )}
          {state.phase === 'error' && (
            <Alert severity="error" data-testid="flash-error">
              {state.message}
            </Alert>
          )}

          {output && (
            <Box>
              <Typography variant="body2" sx={{ fontWeight: 600, mb: 0.5 }}>
                {t('tracker.flash.output')}
              </Typography>
              <Box
                component="pre"
                ref={outputBox}
                data-testid="flash-output"
                sx={{
                  m: 0,
                  p: 1.5,
                  maxHeight: 220,
                  overflow: 'auto',
                  borderRadius: 1,
                  bgcolor: 'action.hover',
                  fontSize: 12,
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-all',
                }}
              >
                {output}
              </Box>
            </Box>
          )}
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2, flexWrap: 'wrap', gap: 1 }}>
        {state.phase === 'done' ? (
          <Button variant="contained" onClick={close}>
            {t('tracker.done')}
          </Button>
        ) : (
          <>
            <Button color="inherit" onClick={close} disabled={busy}>
              {t('button.cancel')}
            </Button>
            {actions.includes('settings') && (
              <Button
                variant={actions.includes('install') ? 'outlined' : 'contained'}
                onClick={() => start('settings')}
                disabled={busy}
              >
                {t('tracker.flash.settingsOnly')}
              </Button>
            )}
            {actions.includes('firmware') && (
              <Button
                variant="outlined"
                onClick={() => start('firmware')}
                disabled={busy}
              >
                {t('tracker.flash.firmwareOnly', { version: manifest.version })}
              </Button>
            )}
            {actions.includes('install') && (
              <Button
                variant="contained"
                onClick={() => start('install')}
                disabled={busy}
              >
                {t('tracker.flash.install', { version: manifest.version })}
              </Button>
            )}
          </>
        )}
      </DialogActions>
      <ConfirmDialog
        open={renewing}
        title={t('tracker.flash.renewTitle', { name: tracker.name })}
        text={t('tracker.flash.renewText')}
        confirmLabel={t('tracker.flash.renew')}
        onClose={() => setRenewing(false)}
        onConfirm={() => renew.mutate({ path: { carId, id: tracker.id } })}
      />
      <ConfirmDialog
        open={!!layout}
        title={t('tracker.flash.layoutTitle')}
        text={t(`tracker.flash.layout.${layout?.problem ?? 'unknown'}`)}
        confirmLabel={t('tracker.flash.layoutConfirm')}
        // the dialog closes before it confirms: the first answer counts
        onClose={() => queueMicrotask(() => answerLayout(false))}
        onConfirm={() => answerLayout(true)}
      />
    </>
  );
}

/** an error of the field or of an item of it */
const hasErrorAt = (errors: SettingsErrors, name: string) =>
  Object.keys(errors).some(
    (path) => path === name || path.startsWith(name + '.'),
  );
