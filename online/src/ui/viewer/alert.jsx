import React from 'react'
import { useDispatch, useSelector } from 'react-redux';

import { Alert, AlertTitle } from '@mui/lab';
import Backdrop from '@mui/material/Backdrop';
import { makeStyles } from '@mui/styles'

const useStyles = makeStyles((theme) => ({
  backdrop: {
    zIndex: theme.zIndex.drawer - 1,
    position: "absolute",
    color: '#fff',
  },
}));

export const ErrorAlert = () =>
{
  const classes = useStyles();
  const report = useDispatch();
  const message = useSelector( state => state.problem );

  const dismissed = () => report( { type: 'ALERT_DISMISSED' } );

  return (
    <Backdrop className={classes.backdrop} open={!!message}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}>
      <Alert variant='filled' severity="error" onClose={dismissed}>
        <AlertTitle>There's a problem</AlertTitle>
        {message}
      </Alert>
    </Backdrop>
  )
}

