import { useMutation, useQuery } from '@tanstack/react-query';
import { ModificationState } from '@/generated';
import {
  getModificationDocumentsOptions,
  getModificationsOptions,
} from '@/generated/@tanstack/react-query.gen';
import queryClient, { localClient } from '@/utils/QueryClient';
import { offlineStore } from '@/offline';
import {
  canKeepDocuments,
  dropDocuments,
  DOCUMENTS_CACHE,
  keepDocuments,
  keptDocuments,
} from '@/offline/documents';
import { contentUrl } from '@/components/bill/attachments';

export const installedOptions = (carId: number) =>
  getModificationsOptions({
    client: localClient,
    path: { carId },
    query: { state: ModificationState.INSTALLED },
  });

/**
 * Whether the documents of the installed parts of the car are kept on the
 * device, and when they were saved. Saving loads the lists of the documents as
 * well, so they show without a connection.
 */
export default function useOfflineDocuments(carId: number) {
  const queryKey = ['offlineDocuments', carId];
  const { data: kept } = useQuery({
    queryKey,
    queryFn: async () => (await keptDocuments(offlineStore, carId)) ?? null,
    // read from the device, also without a connection
    networkMode: 'always',
    enabled: canKeepDocuments(),
  });

  const save = useMutation({
    mutationFn: async () => {
      // the list of the page, not loaded again: it starts the saving
      const modifications = await queryClient.ensureQueryData(
        installedOptions(carId),
      );
      const documents = [];
      for (const modification of modifications) {
        if (modification.documents === 0) continue;
        documents.push(
          ...(await queryClient.fetchQuery(
            getModificationDocumentsOptions({
              client: localClient,
              path: { carId, modificationId: modification.id },
            }),
          )),
        );
      }
      return keepDocuments(
        offlineStore,
        await caches.open(DOCUMENTS_CACHE),
        carId,
        documents,
        (document) => contentUrl(carId, document.id!),
        (url) => fetch(url, { credentials: 'include' }),
      );
    },
    onSuccess: (saved) => queryClient.setQueryData(queryKey, saved),
  });

  const drop = useMutation({
    mutationFn: async () =>
      dropDocuments(offlineStore, await caches.open(DOCUMENTS_CACHE), carId),
    onSuccess: () => queryClient.setQueryData(queryKey, null),
  });

  return { available: canKeepDocuments(), kept, save, drop };
}

/**
 * The kept file of a document as a page of its own, while the device is
 * offline
 *
 * @param tab opened on the click, a later one would be blocked as a popup
 * @return false if it isn't kept, the tab is closed then
 */
export async function openKeptDocument(url: string, tab: Window | null) {
  const response = canKeepDocuments()
    ? await caches.match(url, { cacheName: DOCUMENTS_CACHE })
    : undefined;
  if (!response) {
    tab?.close();
    return false;
  }
  const file = URL.createObjectURL(await response.blob());
  if (tab) {
    tab.location.href = file;
  } else {
    window.open(file, '_blank');
  }
  return true;
}
