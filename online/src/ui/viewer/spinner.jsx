
import React from 'react'

import Backdrop from '@mui/material/Backdrop';
import CircularProgress from '@mui/material/CircularProgress';

export const Spinner = ( { visible, message } ) =>
{
  return (
    <Backdrop open={visible}
        style={ { color: '#fff', position: 'absolute', zIndex: -100 } }>
      <CircularProgress color="inherit" />
      {/* <Typography variant="h2">{message}</Typography> */}
    </Backdrop>
  );
}
