const normalize = (name: string) =>
  name.toLowerCase().replace(/[^a-z0-9]/g, '');

/**
 * Proposes a column for every field, by comparing the names. Exact matches win,
 * longer field names are matched first (pricePerUnit before unit) and a column
 * is used only once.
 *
 * @return field -> index of the column
 */
export function guessMapping(fields: string[], headers: string[]) {
  const mapping: Record<string, number> = {};
  const columns = headers.map(normalize);
  const used = new Set<number>();

  const take = (field: string, matches: (column: string) => boolean) => {
    const index = columns.findIndex(
      (column, i) => !used.has(i) && matches(column),
    );
    if (index >= 0) {
      mapping[field] = index;
      used.add(index);
    }
  };

  const sorted = [...fields].sort((a, b) => b.length - a.length);
  sorted.forEach((field) =>
    take(field, (column) => column === normalize(field)),
  );
  sorted
    .filter((field) => mapping[field] === undefined)
    .forEach((field) =>
      take(field, (column) => column.includes(normalize(field))),
    );

  return mapping;
}
