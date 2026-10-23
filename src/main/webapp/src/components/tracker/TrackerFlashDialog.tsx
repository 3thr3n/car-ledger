import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  InputAdornment,
  LinearProgress,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import AddRounded from '@mui/icons-material/AddRounded';
import DeleteOutlineRounded from '@mui/icons-material/DeleteOutlineRounded';
import ExpandMoreRounded from '@mui/icons-material/ExpandMoreRounded';
import VisibilityOffRounded from '@mui/icons-material/VisibilityOffRounded';
import VisibilityRounded from '@mui/icons-material/VisibilityRounded';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { Device, getFirmwareFile } from '@/generated';
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
  SettingsError,
  SettingsErrors,
  TrackerSettings,
  validateSettings,
} from './configImage';
import { FlashError, flashTracker, FlashStep, Monitor } from './flasher';

export interface TrackerFlashDialogProps {
  open: boolean;
  carId: number;
  tracker: Device;
  /** right after creating the tracker, else a new one can be issued */
  token?: string;
  onClose: () => void;
}

/**
 * Flashes the firmware and the settings (WLAN, URL, token) onto an ESP32 over
 * USB with Web Serial, no other tool needed
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
  | { phase: 'done' }
  | { phase: 'error'; message: string };

const origin = () =>
  typeof window === 'undefined' ? '' : window.location.origin;

function FlashForm({
  carId,
  tracker,
  token: createdToken,
  onClose,
}: Omit<TrackerFlashDialogProps, 'open'>) {
  const { t } = useTranslation();
  const [settings, setSettings] = useState<TrackerSettings>({
    wifi: [{ ssid: '', pass: '' }],
    url: `${origin()}/api/device/points`,
    token: createdToken ?? '',
    ca: '',
  });
  const [touched, setTouched] = useState(false);
  const [renewing, setRenewing] = useState(false);
  const [state, setState] = useState<State>({ phase: 'form' });
  const [output, setOutput] = useState('');
  const monitor = useRef<Monitor>(undefined);
  const outputBox = useRef<HTMLPreElement>(null);

  const { data: manifest, error: manifestError } = useQuery(
    getFirmwareManifestOptions({ client: localClient }),
  );

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

  const errors: SettingsErrors | undefined =
    manifest && validateSettings(settings, manifest.config);
  const shown = touched ? errors : undefined;
  const busy = state.phase === 'flashing';

  const setNetwork = (index: number, field: 'ssid' | 'pass', value: string) =>
    setSettings((s) => ({
      ...s,
      wifi: s.wifi.map((n, i) => (i === index ? { ...n, [field]: value } : n)),
    }));

  const start = async (install: boolean) => {
    setTouched(true);
    if (!manifest || !errors || hasErrors(errors)) return;

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
        install,
        config: configImage(settings, manifest.config),
        download: async (path) => {
          const { data } = await getFirmwareFile({
            client: localClient,
            path: { file: path },
            throwOnError: true,
          });
          return new Uint8Array(await (data as Blob).arrayBuffer());
        },
        onStep: (step) =>
          setState((s) => ({
            phase: 'flashing',
            step,
            percent: s.phase === 'flashing' ? s.percent : 0,
          })),
        onProgress: (percent) =>
          setState((s) => (s.phase === 'flashing' ? { ...s, percent } : s)),
        onOutput: (text) => setOutput((o) => (o + text).slice(-20_000)),
      });
      setState({ phase: 'done' });
    } catch (e) {
      setState({
        phase: 'error',
        message:
          e instanceof FlashError
            ? t(`tracker.flash.errors.${e.code}`, { detail: e.detail })
            : t('tracker.flash.errors.write', { detail: String(e) }),
      });
    }
  };

  const close = async () => {
    await monitor.current?.stop();
    monitor.current = undefined;
    onClose();
  };

  const error = (
    key: SettingsError | undefined,
    values?: Record<string, unknown>,
  ) => (key ? t(`tracker.flash.errors.${key}`, values) : undefined);

  return (
    <>
      <DialogTitle>
        {t('tracker.flash.title', { name: tracker.name })}
      </DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {manifestError && (
            <Alert severity="error">{t('tracker.flash.noManifest')}</Alert>
          )}
          {state.phase === 'form' && (
            <Typography variant="body2" sx={{ color: 'text.secondary' }}>
              {t('tracker.flash.text')}
            </Typography>
          )}

          {(state.phase === 'form' || state.phase === 'error') && (
            <>
              <Typography sx={{ fontWeight: 600 }}>
                {t('tracker.flash.wifi')}
              </Typography>
              {settings.wifi.map((network, index) => (
                <NetworkRow
                  key={index}
                  index={index}
                  ssid={network.ssid}
                  pass={network.pass}
                  ssidError={error(shown?.networks[index]?.ssid)}
                  passError={error(shown?.networks[index]?.pass)}
                  onChange={(field, value) => setNetwork(index, field, value)}
                  onRemove={
                    settings.wifi.length > 1
                      ? () =>
                          setSettings((s) => ({
                            ...s,
                            wifi: s.wifi.filter((_, i) => i !== index),
                          }))
                      : undefined
                  }
                />
              ))}
              <Box>
                <Button
                  size="small"
                  startIcon={<AddRounded />}
                  onClick={() =>
                    setSettings((s) => ({
                      ...s,
                      wifi: [...s.wifi, { ssid: '', pass: '' }],
                    }))
                  }
                >
                  {t('tracker.flash.addNetwork')}
                </Button>
              </Box>

              <TextField
                label={t('tracker.flash.url')}
                value={settings.url}
                onChange={(e) =>
                  setSettings((s) => ({ ...s, url: e.target.value }))
                }
                error={!!shown?.url}
                helperText={
                  error(shown?.url) ??
                  (settings.url.trim().startsWith('http://')
                    ? t('tracker.flash.httpWarning')
                    : t('tracker.flash.urlHelp'))
                }
                required
              />

              <Stack
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
                    error(shown?.token) ??
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

              <Accordion
                disableGutters
                variant="outlined"
                defaultExpanded={!!settings.ca}
              >
                <AccordionSummary expandIcon={<ExpandMoreRounded />}>
                  {t('tracker.flash.advanced')}
                </AccordionSummary>
                <AccordionDetails>
                  <TextField
                    label={t('tracker.flash.ca')}
                    value={settings.ca}
                    onChange={(e) =>
                      setSettings((s) => ({ ...s, ca: e.target.value }))
                    }
                    error={!!shown?.ca}
                    helperText={error(shown?.ca) ?? t('tracker.flash.caHelp')}
                    placeholder="-----BEGIN CERTIFICATE-----"
                    multiline
                    minRows={3}
                    maxRows={10}
                    slotProps={{
                      input: { sx: { fontFamily: 'monospace', fontSize: 12 } },
                    }}
                    fullWidth
                  />
                </AccordionDetails>
              </Accordion>

              {shown?.size && (
                <Alert severity="error">
                  {error(shown.size, { max: manifest?.config.maxLength })}
                </Alert>
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
            <Alert severity="success">{t('tracker.flash.done')}</Alert>
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
            <Button
              variant="outlined"
              onClick={() => start(false)}
              disabled={busy || !manifest}
            >
              {t('tracker.flash.settingsOnly')}
            </Button>
            <Button
              variant="contained"
              onClick={() => start(true)}
              disabled={busy || !manifest}
            >
              {t('tracker.flash.install', { version: manifest?.version })}
            </Button>
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
    </>
  );
}

function NetworkRow({
  index,
  ssid,
  pass,
  ssidError,
  passError,
  onChange,
  onRemove,
}: {
  index: number;
  ssid: string;
  pass: string;
  ssidError?: string;
  passError?: string;
  onChange: (field: 'ssid' | 'pass', value: string) => void;
  onRemove?: () => void;
}) {
  const { t } = useTranslation();
  const [visible, setVisible] = useState(false);

  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      spacing={1}
      sx={{ alignItems: { sm: 'flex-start' } }}
      data-testid="flash-network"
    >
      <TextField
        label={t('tracker.flash.ssid', { number: index + 1 })}
        value={ssid}
        onChange={(e) => onChange('ssid', e.target.value)}
        error={!!ssidError}
        helperText={ssidError}
        autoComplete="off"
        required
        fullWidth
      />
      <TextField
        label={t('tracker.flash.pass')}
        value={pass}
        type={visible ? 'text' : 'password'}
        onChange={(e) => onChange('pass', e.target.value)}
        error={!!passError}
        helperText={passError ?? t('tracker.flash.passHelp')}
        autoComplete="new-password"
        fullWidth
        slotProps={{
          input: {
            endAdornment: (
              <InputAdornment position="end">
                <IconButton
                  aria-label={t(
                    visible
                      ? 'tracker.flash.hidePass'
                      : 'tracker.flash.showPass',
                  )}
                  onClick={() => setVisible((v) => !v)}
                  edge="end"
                >
                  {visible ? <VisibilityOffRounded /> : <VisibilityRounded />}
                </IconButton>
              </InputAdornment>
            ),
          },
        }}
      />
      {onRemove && (
        <Tooltip title={t('tracker.flash.removeNetwork')}>
          <IconButton
            aria-label={t('tracker.flash.removeNetwork')}
            onClick={onRemove}
            sx={{
              alignSelf: { xs: 'flex-end', sm: 'flex-start' },
              mt: { sm: 1 },
            }}
          >
            <DeleteOutlineRounded />
          </IconButton>
        </Tooltip>
      )}
    </Stack>
  );
}
