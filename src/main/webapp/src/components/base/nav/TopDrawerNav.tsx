import { List, ListItemButton, ListItemText } from '@mui/material';
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
