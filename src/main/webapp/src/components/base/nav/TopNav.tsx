import { Button, Stack } from '@mui/material';

const TopNav = ({
  t,
  navigate,
  currentPathName,
  navItems,
}: {
  t: any;
  navigate: (path: string) => void;
  currentPathName: string;
  navItems: { labelKey: string; path: string }[];
}) => {
  return (
    <Stack direction="row" spacing={2}>
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
