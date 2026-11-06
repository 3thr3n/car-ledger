import { Box } from '@mui/material';
import { visuallyHidden } from './visuallyHidden';

export interface ChartTableProps {
  /** what the chart shows */
  caption: string;
  /** the header of the column of the row labels, e.g. "Month" */
  rowHeader: string;
  /** the series */
  columns: string[];
  /** a label and the formatted values of each column, empty for none */
  rows: { label: string; values: (string | null)[] }[];
}

/**
 * The values of a chart as a table for screen readers, which can't read the
 * chart itself
 */
export default function ChartTable({
  caption,
  rowHeader,
  columns,
  rows,
}: ChartTableProps) {
  return (
    // in a hidden box: a table would grow to its content and widen the page
    <Box sx={visuallyHidden}>
      <table>
        <caption>{caption}</caption>
        <thead>
          <tr>
            <th scope="col">{rowHeader}</th>
            {columns.map((column) => (
              <th key={column} scope="col">
                {column}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.label}>
              <th scope="row">{row.label}</th>
              {row.values.map((value, index) => (
                <td key={columns[index]}>{value ?? '–'}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </Box>
  );
}
