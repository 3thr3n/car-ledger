import { Link, Stack, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { getLegalLinksOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';

/**
 * The Impressum and the privacy policy of the operator, nothing on an
 * installation without them. Public: also before the login.
 */
export default function LegalLinks() {
  const { t } = useTranslation();
  const { data } = useQuery({
    ...getLegalLinksOptions({ client: localClient }),
    // set up with the installation, a new value comes with the next start
    staleTime: Infinity,
  });

  const links = [
    { key: 'imprint', url: data?.imprintUrl },
    { key: 'privacy', url: data?.privacyUrl },
  ].filter((link): link is { key: string; url: string } => !!link.url);
  if (links.length === 0) {
    return null;
  }

  return (
    <Stack
      component="nav"
      direction="row"
      aria-label={t('legal.label')}
      sx={{ columnGap: 2, rowGap: 0.5, flexWrap: 'wrap' }}
      data-testid="legal-links"
    >
      {links.map((link) => (
        <Typography key={link.key} variant="caption">
          <Link
            href={link.url}
            target="_blank"
            rel="noreferrer"
            color="text.secondary"
          >
            {t(`legal.${link.key}`)}
          </Link>
        </Typography>
      ))}
    </Stack>
  );
}
