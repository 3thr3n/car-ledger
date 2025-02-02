import { Grid2, Typography } from '@mui/material';
import carPlaceholder from '@/assets/car_placeholder.png';
import React, { useEffect } from 'react';
import { CarGrid, CarGridContent } from './CarGrid';
import { LoadingCarGrid } from './LoadingCarGrid';
import { useQuery } from '@tanstack/react-query';
import { localClient } from '@/utils/QueryClient';
import { getMyCarsOptions } from '@/generated/@tanstack/react-query.gen';
import { useNavigate } from '@tanstack/react-router';
import NewCar from './NewCar';
import CarGridMenu from './CarGridMenu';
import useCarStore from '@/store/CarStore';

export default function CarGridList() {
  const navigate = useNavigate();
  const setCarSize = useCarStore((state) => state.setCarSize);

  const { data, isError, isLoading, refetch } = useQuery({
    ...getMyCarsOptions({
      client: localClient,
    }),
  });

  useEffect(() => {
    if (data) {
      setCarSize(data.length);
    }
  }, [data, setCarSize]);

  const handleOpenCar = async (id: number) => {
    await navigate({
      to: '/bill/$id',
      params: {
        id: `${id}`,
      },
    });
  };

  function GridItem(props: {
    id: number;
    description: string;
    handleOpenCar: (id: number) => void;
  }) {
    return (
      <CarGrid click={() => props.handleOpenCar(props.id)}>
        <CarGridContent>
          <CarGridMenu
            id={props.id}
            sx={{
              position: 'relative',
            }}
          />
          <img
            src={carPlaceholder}
            alt="car"
            width={200}
            height={200}
            style={{
              maxHeight: '200px',
              width: '100%',
              objectFit: 'contain',
            }}
          />
          <Typography>{props.description}</Typography>
        </CarGridContent>
      </CarGrid>
    );
  }

  function renderComponent() {
    if (isLoading) {
      return <LoadingCarGrid />;
    } else if (isError) {
      return <Typography>Backend not responding</Typography>;
    } else {
      const gridItems = data?.map((car) => (
        <GridItem
          key={car.id}
          id={car.id ?? -1}
          description={car.description ?? ''}
          handleOpenCar={handleOpenCar}
        />
      ));
      return <React.Fragment>{gridItems}</React.Fragment>;
    }
  }

  return (
    <React.Fragment>
      <NewCar refetch={refetch} />
      <Grid2
        container
        spacing={2}
        columns={{ xl: 16, md: 12, sm: 8, xs: 4 }}
        justifyContent="center"
        mt={6}
        sx={{
          width: '100%',
          overflow: 'auto',
        }}
      >
        {renderComponent()}
      </Grid2>
    </React.Fragment>
  );
}
