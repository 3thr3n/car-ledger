import { Grid, Typography } from '@mui/material';
import carPlaceholder from '@/assets/car_placeholder.png';
import React, { useEffect } from 'react';
import { CarGrid, CarGridContent } from './CarGrid';
import NewCar from './NewCar';
import CarGridMenu from './CarGridMenu';
import useCarStore from '@/store/CarStore';
import { NavigateOptions } from '@tanstack/router-core';
import { CarPojo } from '@/generated';

export interface CarGridListProps {
  navigate: (path: NavigateOptions) => void;
  data: CarPojo[];
}

export default function CarGridList({ navigate, data }: CarGridListProps) {
  const setCarSize = useCarStore((state) => state.setCarSize);

  useEffect(() => {
    if (data) {
      setCarSize(data.length);
    }
  }, [data, setCarSize]);

  const handleOpenCar = (id: number) => {
    navigate({
      to: '/car/$id',
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
    return (
      <React.Fragment>
        {data?.map((car) => (
          <GridItem
            key={car.id}
            id={car.id ?? -1}
            description={car.description ?? ''}
            handleOpenCar={handleOpenCar}
          />
        ))}
      </React.Fragment>
    );
  }

  return (
    <React.Fragment>
      <Grid
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
        <NewCar />
      </Grid>
    </React.Fragment>
  );
}
