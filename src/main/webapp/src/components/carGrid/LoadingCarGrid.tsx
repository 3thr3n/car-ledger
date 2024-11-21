import { Skeleton } from "@mui/material";
import { CarGrid, CarGridContent } from "./CarGrid";

export function LoadingCarGrid() {
  return (
    <CarGrid click={() => {}}>
      <CarGridContent>
        <Skeleton variant="rectangular" height={100} width='100%'/>
        <Skeleton variant="text" width='100%' sx={{ fontSize: '2rem' }} />
      </CarGridContent>
    </CarGrid>
  )
}