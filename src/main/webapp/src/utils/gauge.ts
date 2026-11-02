/** a scale of round numbers around the values, with some room at both ends */
export function scaleOf(values: number[], step: number) {
  const lowest = Math.min(...values);
  const highest = Math.max(...values);
  const room = Math.max(step, (highest - lowest) * 0.5);
  return {
    min: Math.max(0, Math.floor((lowest - room) / step) * step),
    max: Math.ceil((highest + room) / step) * step,
  };
}
