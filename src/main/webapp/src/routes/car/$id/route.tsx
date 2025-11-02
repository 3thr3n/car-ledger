import { createFileRoute, redirect } from '@tanstack/react-router';

export const Route = createFileRoute('/car/$id')({
  beforeLoad: ({ params }) => {
    // ✅ Validate the car ID before any nested route loads
    const id = Number(params.id);

    if (Number.isNaN(id)) {
      throw redirect({ to: '..' });
    }

    // optionally, you could even fetch minimal car metadata here
    return { id };
  },
});
