
import React from 'react'

import Backdrop from '@mui/material/Backdrop';
import CircularProgress from '@mui/material/CircularProgress';
import { makeStyles } from '@mui/styles';

const useStyles = makeStyles((theme) => ({
  backdrop: {
    zIndex: theme.zIndex.drawer - 1,
    position: "absolute",
    color: '#fff',
  },
}));

export const Spinner = ( { visible, message } ) =>
{
  const classes = useStyles();
  return (
    <Backdrop className={classes.backdrop} open={visible}>
      <CircularProgress color="inherit" />
      {/* <Typography variant="h2">{message}</Typography> */}
    </Backdrop>
  );
}
