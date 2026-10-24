import { Box, IconButton, Tooltip, Typography } from '@mui/material';
import CloseRounded from '@mui/icons-material/CloseRounded';
import PictureAsPdfRounded from '@mui/icons-material/PictureAsPdfRounded';
import { MouseEvent } from 'react';
import { useTranslation } from 'react-i18next';

export const TILE = 88;

/** a file as thumbnail that opens it in a new tab, with a button to remove it */
export default function AttachmentTile({
  name,
  pdf,
  url,
  onRemove,
  onOpen,
}: {
  name: string;
  pdf: boolean;
  /** undefined while the preview of a picked file is prepared */
  url?: string;
  onRemove: () => void;
  /** before the link opens the file, e.g. to open a kept copy offline */
  onOpen?: (event: MouseEvent<HTMLAnchorElement>) => void;
}) {
  const { t } = useTranslation();

  return (
    <Box sx={{ position: 'relative', width: TILE, height: TILE }}>
      <Tooltip title={name}>
        <Box
          component="a"
          href={url}
          target="_blank"
          rel="noopener"
          onClick={onOpen}
          aria-label={t('attachments.open', { name })}
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            width: '100%',
            height: '100%',
            borderRadius: 2,
            overflow: 'hidden',
            border: 1,
            borderColor: 'divider',
            bgcolor: 'action.hover',
            color: 'text.secondary',
            textDecoration: 'none',
          }}
        >
          {pdf ? (
            <>
              <PictureAsPdfRounded color="error" />
              <Typography
                variant="caption"
                noWrap
                sx={{ maxWidth: '100%', px: 0.75 }}
              >
                {name}
              </Typography>
            </>
          ) : (
            <Box
              component="img"
              src={url}
              alt={name}
              loading="lazy"
              sx={{ width: '100%', height: '100%', objectFit: 'cover' }}
            />
          )}
        </Box>
      </Tooltip>
      <IconButton
        size="small"
        onClick={onRemove}
        aria-label={t('attachments.remove', { name })}
        sx={{
          position: 'absolute',
          top: 2,
          right: 2,
          p: 0.25,
          bgcolor: 'background.paper',
          boxShadow: 1,
          '&:hover': { bgcolor: 'background.paper' },
        }}
      >
        <CloseRounded sx={{ fontSize: 16 }} />
      </IconButton>
    </Box>
  );
}
