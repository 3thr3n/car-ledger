import {
  Button,
  CardActionArea,
  IconButton,
  Link,
  ListItemButton,
  Tab,
} from '@mui/material';
import { createLink } from '@tanstack/react-router';

/**
 * MUI components wired to the router, so they render real links (middle click, prefetch, ...)
 */
export const RouterButton = createLink(Button);
export const RouterIconButton = createLink(IconButton);
export const RouterLink = createLink(Link);
export const RouterListItemButton = createLink(ListItemButton);
export const RouterTab = createLink(Tab);
export const RouterCardActionArea = createLink(CardActionArea);
