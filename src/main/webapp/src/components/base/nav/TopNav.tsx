import { Button, Stack } from '@mui/material';
import { useTranslation } from 'react-i18next';

const TopNav = ({
  navigate,
  currentPathName,
  navItems,
}: {
  navigate: (path: string) => void;
  currentPathName: string;
  navItems: { labelKey: string; path: string }[];
}) => {
  const { t } = useTranslation();

  return (
    <Stack direction="row" spacing={1}>
      {navItems.map(({ labelKey, path }) => {
        const isActive = currentPathName.startsWith(path);
        return (
          <Button
            key={path}
            variant="text"
            color={isActive ? 'primary' : 'inherit'}
            sx={{
              borderBottom: isActive ? '2px solid' : '2px solid transparent',
              borderColor: isActive ? 'primary.main' : 'transparent',
              borderRadius: 0,
              fontWeight: isActive ? 600 : 400,
              '&:hover': { backgroundColor: 'action.hover' },
            }}
            onClick={() => navigate(path)}
          >
            {t(labelKey)}
          </Button>
        );
      })}
    </Stack>
  );
};

export default TopNav;
