import {Box, Link, Typography} from "@mui/material";

export default function Copyright() {
    return (
        <Box
            sx={{
                pb: 1
            }}
        >
            <Typography
                variant="body2"
                align="center"
                sx={{
                    color: 'text.secondary',
                }}
            >
                {'Copyright ©'}
                <Link href="https://www.codeflowwizardry.de">3thr3n</Link>
                {' '}
                {new Date().getFullYear()}.
            </Typography>
        </Box>
    )
}