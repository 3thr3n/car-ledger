import { RefObject, useEffect, useState } from 'react';

const offset = 50;

/**
 * Checks the `ref` if the scrollHeight is reached and updates the scrollNeeded property
 * @param ref
 */
export function useScrollNearBottom(ref: RefObject<HTMLElement | null>) {
  const [isNearBottom, setIsNearBottom] = useState(false);

  useEffect(() => {
    const container = ref?.current;
    if (!container) return;

    const handleScroll = () => {
      const { scrollTop, scrollHeight, clientHeight } = container;
      const distanceFromBottom = scrollHeight - scrollTop - clientHeight;
      setIsNearBottom(distanceFromBottom < offset);
    };

    container.addEventListener('scroll', handleScroll);
    handleScroll(); // Check initial state

    return () => container.removeEventListener('scroll', handleScroll);
  }, [ref]);

  return isNearBottom;
}
