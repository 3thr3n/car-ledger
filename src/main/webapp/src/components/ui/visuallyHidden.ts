/**
 * read by screen readers only, e.g. the name of a column of buttons; at the left
 * edge, so even a wide content doesn't widen the page
 */
export const visuallyHidden = {
  position: 'absolute',
  left: 0,
  width: 1,
  height: 1,
  margin: '-1px',
  padding: 0,
  border: 0,
  overflow: 'hidden',
  clip: 'rect(0 0 0 0)',
  whiteSpace: 'nowrap',
} as const;
