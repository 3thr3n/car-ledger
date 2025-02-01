import { Box, CircularProgress, Grid2, Typography } from '@mui/material';
import useBillPagination from '@/hooks/useBillPagination';
import {
  createRef,
  forwardRef,
  Fragment,
  useEffect,
  useImperativeHandle,
  useState,
} from 'react';
import { BillPojo } from '@/generated';
import { useScrollNeeded } from '@/hooks/useScrollNeeded';
import BillGridGroup from './BillGridGroup';

export interface BillGridListProps {
  carId: number;
}

export interface BillGridRef {
  refresh: () => void;
}

const defaultPageSize = 30;
const defaultPagination = { page: 0, pageSize: defaultPageSize };

const BillGridList = forwardRef<BillGridRef, BillGridListProps>(
  (props, ref) => {
    const { data, setPagination } = useBillPagination(
      props.carId,
      defaultPagination,
    );
    const [gridData, setGridData] = useState<BillPojo[]>([]);
    const [loading, setLoading] = useState(false);

    const gridRef = createRef<HTMLDivElement>();
    const scrollNeeded = useScrollNeeded(gridRef);

    useImperativeHandle(ref, () => ({
      refresh() {
        setPagination(defaultPagination);
        setGridData([]);
      },
    }));

    function renderComponent() {
      const groupedByYearUnsorted = Object.groupBy(
        gridData,
        ({ day }) => day?.split('-')[0] ?? '1970',
      );

      return Object.entries(groupedByYearUnsorted)
        .sort()
        .reverse()
        .map(([key, val]) => {
          if (val) {
            return <BillGridGroup bills={val} year={key} key={key} />;
          }
        });
    }

    useEffect(() => {
      if (!data?.data) return;
      setGridData([...gridData, ...(data?.data ?? [])]);
      setLoading(false);
    }, [data]);

    useEffect(() => {
      if (!scrollNeeded || gridData.length == data?.total) return;
      loadData(data?.page ?? 0);
    }, [scrollNeeded]);

    useEffect(() => {
      return () => {
        console.log('Cleanup');
        setGridData([]);
        setLoading(false);
      };
    }, []);

    const loadData = (page: number) => {
      setLoading(true);
      setPagination({ page: page, pageSize: defaultPageSize });
    };

    function loadingComponent() {
      if (!loading) return;

      return (
        <Box display="flex" justifyContent={'center'} alignItems={'center'}>
          <CircularProgress size={24} />
          <Typography ml={2} variant="body1" fontSize={24}>
            Loading...
          </Typography>
        </Box>
      );
    }

    return (
      <Fragment>
        <Grid2
          ref={gridRef}
          container
          spacing={2}
          mt={6}
          px={2}
          columns={{ xl: 14, md: 8, sm: 6, xs: 4 }}
          sx={{
            overflow: 'auto',
          }}
        >
          {renderComponent()}
        </Grid2>
        {loadingComponent()}
      </Fragment>
    );
  },
);

export default BillGridList;
