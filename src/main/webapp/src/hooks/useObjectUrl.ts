import { useEffect, useState } from 'react';

/**
 * A blob URL of the file for previews, revoked when the file changes or the
 * component unmounts. Created in the effect, so the double effect run of the
 * strict mode does not leave a revoked URL behind.
 */
export default function useObjectUrl(file: File): string | undefined {
  const [url, setUrl] = useState<string>();

  useEffect(() => {
    const objectUrl = URL.createObjectURL(file);
    setUrl(objectUrl);
    return () => URL.revokeObjectURL(objectUrl);
  }, [file]);

  return url;
}
