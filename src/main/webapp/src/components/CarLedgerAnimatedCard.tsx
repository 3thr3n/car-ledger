import { motion } from 'framer-motion';
import { ReactNode } from 'react';

export const CarLedgerAnimatedCard = ({
  index,
  children,
  maxWidth,
}: {
  index: number;
  children: ReactNode;
  maxWidth?: string | number;
}) => {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20, scale: 0.98 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{
        duration: 0.4,
        delay: index * 0.07, // stagger timing
        ease: 'easeOut',
      }}
      style={{ width: '100%', height: '100%', maxWidth }}
    >
      {children}
    </motion.div>
  );
};
