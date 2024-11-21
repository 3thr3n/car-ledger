import {Card, CardContent, Grid2, SxProps, Theme} from '@mui/material';
import React, {MouseEventHandler} from 'react';

export function CarGrid(props: {
    size?: number,
    height?: number,
    children: React.ReactNode,
    sx?: SxProps<Theme>,
    click: MouseEventHandler<HTMLDivElement>
}) {
    return (
        <Grid2
            size={props.size ?? 4}
            height={props.height}
            justifyContent="center"
            display="flex"
            maxWidth={400}
        >
            <Card
                onClick={props.click}
                variant="elevation"
                sx={{
                    ...props.sx,
                    height: props.height,
                    maxWidth: 500,
                    width: '100%',
                    cursor: 'pointer'
                }}
            >
                {props.children}
            </Card>
        </Grid2>
    );
}

export function CarGridContent(props: {
    children: React.ReactNode
}) {
    return (
        <CardContent sx={{
            alignItems: 'center',
            justifyContent: 'center',
            display: 'flex',
            flexDirection: 'column',
            position: 'relative'
        }}>
            {props.children}
        </CardContent>
    );
}