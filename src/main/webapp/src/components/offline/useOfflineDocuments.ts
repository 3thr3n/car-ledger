import { useMutation, useQuery } from '@tanstack/react-query';
import { MouseEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { ModificationState } from '@/generated';
import {
  getModificationDocumentsOptions,
  getModificationsOptions,
  getTechnicalInspectionFilesOptions,
  getTechnicalInspectionsOptions,
} from '@/generated/@tanstack/react-query.gen';
import queryClient, { localClient } from '@/utils/QueryClient';
import { offlineStore } from '@/offline';
import {
  canKeepDocuments,
  dropDocuments,
  DOCUMENTS_CACHE,
  inPriority,
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

export const inspectionsOptions = (carId: number) =>
  getTechnicalInspectionsOptions({ client: localClient, path: { carId } });

/**
 * Whether the documents of the installed parts and the report of the latest HU
 * of the car are kept on the device, and when they were saved. Saving loads
 * the lists of the files as well, so they show without a connection.
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
      // the lists of the pages, not loaded again: they start the saving
      const modifications = await queryClient.ensureQueryData(
        installedOptions(carId),
      );
      const [latest] = await queryClient.ensureQueryData(
        inspectionsOptions(carId),
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
      const reports =
        latest && latest.documents > 0
          ? await queryClient.fetchQuery(
              getTechnicalInspectionFilesOptions({
                client: localClient,
                path: { carId, inspectionId: latest.id },
              }),
            )
          : [];
      return keepDocuments(
        offlineStore,
        await caches.open(DOCUMENTS_CACHE),
        carId,
        inPriority(documents, reports),
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
 * @return a handler of the link of a file: without a connection it opens the
 *         kept copy, or says that there is none
 */
export function useOpenKept() {
  const { t } = useTranslation();
  return (url: string) => (event: MouseEvent<HTMLAnchorElement>) => {
    if (navigator.onLine) return;
    event.preventDefault();
    void openKeptDocument(url, window.open('', '_blank')).then(
      (opened) => opened || toast.info(t('modification.offline.notKept')),
    );
  };
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
