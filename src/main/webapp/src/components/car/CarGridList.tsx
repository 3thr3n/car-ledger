import { Grid, Typography } from '@mui/material';
import carPlaceholder from '@/assets/car_placeholder.svg';
import React, { useEffect } from 'react';
import { CarGrid } from './CarGrid';
import NewCar from './NewCar';
import useCarStore from '@/store/CarStore';
import { NavigateOptions } from '@tanstack/router-core';
import { Car } from '@/generated';
import { CarLedgerAnimatedCard } from '@/components/CarLedgerAnimatedCard';

export interface CarGridListProps {
  navigate: (path: NavigateOptions) => void;
  data: Car[];
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
    index?: number;
    description: string;
    handleOpenCar: (id: number) => void;
  }) {
    return (
      <CarGrid click={() => props.handleOpenCar(props.id)} index={props.index}>
        {/*<CarGridMenu*/}
        {/*  id={props.id}*/}
        {/*  sx={{*/}
        {/*    position: 'relative',*/}
        {/*  }}*/}
        {/*/>*/}
        <img
          src={carPlaceholder}
          alt="car"
          width={200}
          height={200}
          style={{
            maxHeight: '200px',
            width: '100%',
            objectFit: 'contain',
            filter:
              'blur(0.3em) invert(0.7) opacity(0.2) sepia(.88) hue-rotate(180deg)',
          }}
        />
        <Typography>{props.description}</Typography>
      </CarGrid>
    );
  }

  function renderComponent() {
    return (
      <React.Fragment>
        {data?.map((car, i) => (
          <CarLedgerAnimatedCard index={i} key={car.id} maxWidth={400}>
            <GridItem
              id={car.id ?? -1}
              index={i}
              description={car.name ?? ''}
              handleOpenCar={handleOpenCar}
            />
          </CarLedgerAnimatedCard>
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
        display="flex"
        pt={4}
        sx={{
          width: '100%',
          overflow: 'auto',
        }}
      >
        {renderComponent()}
        <NewCar index={data.length ?? 0} />
      </Grid>
    </React.Fragment>
  );
}
