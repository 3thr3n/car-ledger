import { RefObject, useEffect, useState } from 'react';

const offset = 50;

/**
 * Checks the `ref` if the scrollHeight is reached and updates the scrollNeeded property
 * @param ref
 */
export function useScrollNeeded(ref: RefObject<HTMLElement>) {
  const [scrollNeeded, setScrollNeeded] = useState(false);

  useEffect(() => {
    const container = ref.current;
    if (!container) return;

    const updateNeedScroll = () => {
      const result =
        container.scrollTop <
        container.scrollHeight - container.clientHeight - offset;
      setScrollNeeded(!result);
    };

    container.addEventListener('scroll', updateNeedScroll);
    updateNeedScroll();
    return () => container.removeEventListener('scroll', updateNeedScroll);
  }, [ref]);

  return scrollNeeded;
}
