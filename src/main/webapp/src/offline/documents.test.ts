import { describe, expect, it, vi } from 'vitest';
import { Attachment, DocumentKind } from '@/generated';
import {
  dropDocuments,
  inPriority,
  keepDocuments,
  keptDocuments,
  MAX_OFFLINE_BYTES,
} from './documents';
import { memoryStore } from './store';

/** the part of the Cache Storage that is used */
function fakeCache() {
  const files = new Map<string, Response>();
  return {
    files,
    cache: {
      match: async (url: string) => files.get(url),
      put: async (url: string, response: Response) => {
        files.set(url, response);
      },
      delete: async (url: string) => files.delete(url),
    } as unknown as Cache,
  };
}

const document = (id: number, kind: DocumentKind, size = 1000): Attachment => ({
  id,
  kind,
  size,
  fileName: `${id}.pdf`,
});
const url = (each: Attachment) => `/content/${each.id}`;

describe('documents kept for offline', () => {
  it('keeps the papers first and skips what is beyond the limit', async () => {
    const store = memoryStore();
    const { cache, files } = fakeCache();
    const load = vi.fn(async () => new Response('%PDF'));

    const kept = await keepDocuments(
      store,
      cache,
      1,
      inPriority(
        [
          document(1, DocumentKind.PHOTO, MAX_OFFLINE_BYTES / 2),
          document(2, DocumentKind.APPROVAL, MAX_OFFLINE_BYTES / 2),
          document(3, DocumentKind.INSPECTION, 1000),
        ],
        [],
      ),
      url,
      load,
    );

    expect(kept.urls).toEqual(['/content/2', '/content/3']);
    expect(kept.skipped).toEqual(['1.pdf']);
    expect([...files.keys()]).toEqual(['/content/2', '/content/3']);
    expect(await keptDocuments(store, 1)).toEqual(kept);
  });

  it('puts the report of the HU after the papers, before the photos', () => {
    const report: Attachment = { id: 9, size: 1000, fileName: 'hu.pdf' };

    const ordered = inPriority(
      [
        document(1, DocumentKind.OTHER),
        document(2, DocumentKind.PHOTO),
        document(3, DocumentKind.INSPECTION),
        document(4, DocumentKind.APPROVAL),
      ],
      [report],
    );

    expect(ordered.map((each) => each.id)).toEqual([4, 3, 9, 2, 1]);
  });

  it('loads only new documents and drops the old ones', async () => {
    const store = memoryStore();
    const { cache, files } = fakeCache();
    const load = vi.fn(async () => new Response('%PDF'));
    await keepDocuments(
      store,
      cache,
      1,
      [document(1, DocumentKind.APPROVAL), document(2, DocumentKind.PHOTO)],
      url,
      load,
    );
    load.mockClear();

    // the photo was deleted, an Abnahme added
    await keepDocuments(
      store,
      cache,
      1,
      [
        document(1, DocumentKind.APPROVAL),
        document(3, DocumentKind.INSPECTION),
      ],
      url,
      load,
    );

    expect(load).toHaveBeenCalledExactlyOnceWith('/content/3');
    expect([...files.keys()].sort()).toEqual(['/content/1', '/content/3']);
  });

  it('keeps nothing of a document that cannot be loaded', async () => {
    const store = memoryStore();
    const { cache, files } = fakeCache();
    const load = async () => new Response('', { status: 404 });

    await expect(
      keepDocuments(
        store,
        cache,
        1,
        [document(1, DocumentKind.APPROVAL)],
        url,
        load,
      ),
    ).rejects.toThrow('/content/1: 404');
    expect(files.size).toBe(0);
    expect(await keptDocuments(store, 1)).toBeUndefined();
  });

  it('drops the documents of one car', async () => {
    const store = memoryStore();
    const { cache, files } = fakeCache();
    const load = async () => new Response('%PDF');
    await keepDocuments(
      store,
      cache,
      1,
      [document(1, DocumentKind.APPROVAL)],
      url,
      load,
    );
    await keepDocuments(
      store,
      cache,
      2,
      [document(2, DocumentKind.APPROVAL)],
      url,
      load,
    );

    await dropDocuments(store, cache, 1);

    expect([...files.keys()]).toEqual(['/content/2']);
    expect(await keptDocuments(store, 1)).toBeUndefined();
    expect(await keptDocuments(store, 2)).toBeDefined();
  });
});
