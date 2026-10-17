/** the key of the server (base64url) as the browser wants it */
export function keyBytes(base64url: string) {
  const base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
  const binary = atob(
    base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '='),
  );
  return Uint8Array.from(binary, (char) => char.charCodeAt(0));
}

/**
 * Whether the subscription of the browser was made with the key of the server.
 * Undefined when the browser does not tell its key (not every browser fills
 * PushSubscription.options.applicationServerKey): then the subscription is
 * kept, a wrong one is removed by the server once the push service rejects it.
 */
export function keyMatches(
  subscribedKey: ArrayBuffer | null | undefined,
  serverKey: string,
): boolean | undefined {
  if (!subscribedKey || subscribedKey.byteLength === 0) return undefined;
  const bytes = new Uint8Array(subscribedKey);
  const key = keyBytes(serverKey);
  return (
    bytes.length === key.length && bytes.every((byte, i) => byte === key[i])
  );
}

const ENDPOINT = 'pushEndpoint';

/**
 * The subscription this browser registered last. A browser may lose its
 * subscription or get a new one: the old one is then removed from the server,
 * so the device is not counted twice.
 */
export function rememberedEndpoint(): string | null {
  try {
    return localStorage.getItem(ENDPOINT);
  } catch {
    return null;
  }
}

export function rememberEndpoint(endpoint: string | null) {
  try {
    if (endpoint) localStorage.setItem(ENDPOINT, endpoint);
    else localStorage.removeItem(ENDPOINT);
  } catch {
    // storage blocked: an old subscription stays until its push service rejects it
  }
}
