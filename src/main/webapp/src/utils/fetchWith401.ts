import { redirect } from "@tanstack/react-router";

let redirecting = false;

export const fetchWith401: typeof fetch = async (input, init) => {
  const res = await fetch(input, {
    ...init,
    credentials: 'include',
  });

  if (res.status === 401 && !redirecting) {
    redirecting = true;
    setTimeout(() => (redirecting = false), 1000); // reset after 1s
    redirect({to: '/login'})
  }

  return res;
};