export class BackendError extends Error {
  status: number;
  message: string;
  /** the answer of the server, e.g. the details of a conflict (409) */
  body?: unknown;

  constructor(status: number, message?: string, body?: unknown) {
    super();
    this.status = status;
    this.message = message ?? '';
    this.body = body;
  }
}

/**
 * @return the message of the backend, e.g. a validation error, otherwise the
 *   fallback: other errors may contain technical details
 */
export function errorMessage(error: unknown, fallback: string): string {
  return error instanceof BackendError && error.message
    ? error.message
    : fallback;
}
