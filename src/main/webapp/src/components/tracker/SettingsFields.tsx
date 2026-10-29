import {
  Box,
  Button,
  FormControlLabel,
  FormHelperText,
  IconButton,
  InputAdornment,
  Stack,
  Switch,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import AddRounded from '@mui/icons-material/AddRounded';
import DeleteOutlineRounded from '@mui/icons-material/DeleteOutlineRounded';
import VisibilityOffRounded from '@mui/icons-material/VisibilityOffRounded';
import VisibilityRounded from '@mui/icons-material/VisibilityRounded';
import { ChangeEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  emptyItem,
  Field,
  isList,
  Item,
  ListField,
  ScalarField,
  SettingsErrors,
  Value,
} from './configImage';
import { useErrorText, useTexts } from './useSettingsTexts';

interface FieldProps {
  name: string;
  field: Field;
  value: Value | Item[];
  required: boolean;
  errors?: SettingsErrors;
  onChange: (value: Value | Item[]) => void;
}

/** a setting of the schema of the firmware */
export function SettingsField(props: FieldProps) {
  const { field, value, onChange } = props;
  return isList(field) ? (
    <ListInput
      {...props}
      field={field}
      items={value as Item[]}
      onChange={onChange}
    />
  ) : (
    <ScalarInput
      {...props}
      path={props.name}
      field={field}
      value={value as Value}
      onChange={onChange}
    />
  );
}

function ListInput({
  name,
  field,
  items,
  errors,
  onChange,
}: Omit<FieldProps, 'field' | 'value' | 'onChange'> & {
  field: ListField;
  items: Item[];
  onChange: (items: Item[]) => void;
}) {
  const { t } = useTranslation();
  const texts = useTexts();
  const errorText = useErrorText(errors);
  const { title, description } = texts(field, name);
  const item = texts(field.items, name).title;
  const required = new Set(field.items.required ?? []);
  const error = errorText(name);

  const change = (index: number, itemName: string, value: Value) =>
    onChange(
      items.map((it, i) => (i === index ? { ...it, [itemName]: value } : it)),
    );

  return (
    <Stack spacing={1.5} data-testid={`flash-list-${name}`}>
      <Box>
        <Typography sx={{ fontWeight: 600 }}>{title}</Typography>
        {description && (
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {description}
          </Typography>
        )}
        {error && <FormHelperText error>{error}</FormHelperText>}
      </Box>
      {items.map((it, index) => {
        const label = `${item} ${index + 1}`;
        return (
          <Stack
            key={index}
            role="group"
            aria-label={label}
            direction={{ xs: 'column', sm: 'row' }}
            spacing={1}
            sx={{ alignItems: { sm: 'flex-start' } }}
            data-testid="flash-item"
          >
            {Object.entries(field.items.properties).map(
              ([itemName, itemField]) => (
                <ScalarInput
                  key={itemName}
                  name={itemName}
                  path={`${name}.${index}.${itemName}`}
                  field={itemField}
                  value={it[itemName]}
                  required={required.has(itemName)}
                  errors={errors}
                  onChange={(value) => change(index, itemName, value)}
                />
              ),
            )}
            {items.length > (field.minItems ?? 0) && (
              <Tooltip title={t('tracker.flash.removeItem', { item: label })}>
                <IconButton
                  aria-label={t('tracker.flash.removeItem', { item: label })}
                  onClick={() => onChange(items.filter((_, i) => i !== index))}
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
      })}
      {(field.maxItems === undefined || items.length < field.maxItems) && (
        <Box>
          <Button
            size="small"
            startIcon={<AddRounded />}
            onClick={() => onChange([...items, emptyItem(field.items)])}
          >
            {t('tracker.flash.addItem', { item })}
          </Button>
        </Box>
      )}
    </Stack>
  );
}

function ScalarInput({
  name,
  path,
  field,
  value,
  required,
  errors,
  onChange,
}: {
  name: string;
  path: string;
  field: ScalarField;
  value: Value | undefined;
  required: boolean;
  errors?: SettingsErrors;
  onChange: (value: Value) => void;
}) {
  const { t } = useTranslation();
  const texts = useTexts();
  const error = useErrorText(errors)(path);
  const [visible, setVisible] = useState(false);
  const { title, description } = texts(field, name);

  if (field.type === 'boolean') {
    return (
      <Box>
        <FormControlLabel
          control={
            <Switch
              checked={value === true}
              onChange={(e) => onChange(e.target.checked)}
            />
          }
          label={title}
        />
        {description && <FormHelperText>{description}</FormHelperText>}
      </Box>
    );
  }

  const text = typeof value === 'string' ? value : '';
  const common = {
    label: title,
    value: text,
    onChange: (e: ChangeEvent<HTMLInputElement>) => onChange(e.target.value),
    error: !!error,
    helperText: error ?? description,
    required,
    fullWidth: true,
  };

  if (field.type === 'integer' || field.type === 'number') {
    return (
      <TextField
        {...common}
        slotProps={{
          htmlInput: {
            inputMode: field.type === 'integer' ? 'numeric' : 'decimal',
          },
        }}
      />
    );
  }

  if (field.format === 'pem') {
    return (
      <TextField
        {...common}
        placeholder="-----BEGIN CERTIFICATE-----"
        multiline
        minRows={3}
        maxRows={10}
        slotProps={{
          input: { sx: { fontFamily: 'monospace', fontSize: 12 } },
        }}
      />
    );
  }

  if (field.format === 'password') {
    return (
      <TextField
        {...common}
        type={visible ? 'text' : 'password'}
        autoComplete="new-password"
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
    );
  }

  return (
    <TextField
      {...common}
      type={field.format === 'uri' ? 'url' : 'text'}
      autoComplete="off"
    />
  );
}
