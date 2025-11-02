import { NavigateOptions } from '@tanstack/router-core';
import { Container, Typography } from '@mui/material';
import CarGridList from '@/components/carGrid/CarGridList';
import { useQuery } from '@tanstack/react-query';
import { getMyCarsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { LoadingCarGrid } from '@/components/carGrid/LoadingCarGrid';
import React from 'react';
import ErrorPage from '@/pages/ErrorPage';
import { BackendError } from '@/utils/BackendError';

export interface CarListPageProperties {
  navigate: (path: NavigateOptions) => void;
}

export default function CarListPage({ navigate }: CarListPageProperties) {
  const { data, isError, isLoading, error } = useQuery({
    ...getMyCarsOptions({
      client: localClient,
    }),
  });

  if (isError && error instanceof BackendError) {
    if (error.status == 401) {
      return <ErrorPage error="Please login, before accessing this page" />;
    }

    return (
      <ErrorPage error="Backend not responding. Please try again later!" />
    );
  }

  function renderComponents() {
    if (isLoading) {
      return <LoadingCarGrid />;
    }
    if (data) {
      return <CarGridList navigate={navigate} data={data} />;
    }
    return <></>;
  }

  return (
    <Container>
      <Typography variant="h4" gutterBottom>
        My Cars
      </Typography>
      <Typography variant="body1" color="text.secondary" gutterBottom>
        View, add, and manage your vehicles.
      </Typography>
      {renderComponents()}
    </Container>
  );
}
