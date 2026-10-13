export class BackendError extends Error {
  status: number;
  message: string;

  constructor(status: number, message?: string) {
    super();
    this.status = status;
    this.message = message ?? '';
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
