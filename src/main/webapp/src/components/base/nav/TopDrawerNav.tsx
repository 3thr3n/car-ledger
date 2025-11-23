import { List, ListItemButton, ListItemText } from '@mui/material';

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
    <List>
      {navItems.map(({ labelKey, path }) => {
        const isActive = currentPathName.startsWith(path);
        return (
          <ListItemButton
            key={labelKey}
            selected={isActive}
            onClick={() => {
              navigate(path);
            }}
          >
            <ListItemText primary={t(labelKey)} />
          </ListItemButton>
        );
      })}
    </List>
  );
};

export default TopNav;
