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

/** field -> name of the column, null for a field the user chose not to import */
export type SavedMapping = Record<string, string | null>;

/** The names of the mapped columns, so the mapping fits files with other column orders. */
export function toSavedMapping(
  fields: string[],
  headers: string[],
  mapping: Record<string, number>,
): SavedMapping {
  return Object.fromEntries(
    fields.map((field) => [field, headers[mapping[field]] ?? null]),
  );
}

/**
 * Proposes a column for every field: the column of the saved mapping if the file
 * has it, a guess by name for fields the saved mapping does not know.
 *
 * @return field -> index of the column, and whether the saved mapping was used
 */
export function restoreMapping(
  fields: string[],
  headers: string[],
  saved?: SavedMapping,
) {
  if (!saved) {
    return { mapping: guessMapping(fields, headers), restored: false };
  }

  const mapping: Record<string, number> = {};
  let restored = false;
  fields.forEach((field) => {
    const name = saved[field];
    const index = name ? headers.indexOf(name) : -1;
    if (index >= 0) {
      mapping[field] = index;
      restored = true;
    }
  });
  // a file of another export, the saved mapping does not help
  if (!restored) return { mapping: guessMapping(fields, headers), restored };

  const used = new Set(Object.values(mapping));
  const unknown = fields.filter((field) => !(field in saved));
  const guessed = guessMapping(unknown, headers);
  Object.entries(guessed).forEach(([field, index]) => {
    if (!used.has(index)) mapping[field] = index;
  });

  return { mapping, restored };
}

const storageKey = (type: string) => `csvMapping.${type}`;

/** The mapping of the last import of this entry type in this browser. */
export function loadMapping(type: string): SavedMapping | undefined {
  try {
    const value = localStorage.getItem(storageKey(type));
    return value ? (JSON.parse(value) as SavedMapping) : undefined;
  } catch {
    return undefined;
  }
}

export function saveMapping(type: string, mapping: SavedMapping) {
  try {
    localStorage.setItem(storageKey(type), JSON.stringify(mapping));
  } catch {
    // without storage the mapping is just not remembered
  }
}
