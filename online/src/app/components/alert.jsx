import React from 'react'
import { useDispatch, useSelector } from 'react-redux';

import { Alert, AlertTitle } from '@mui/lab';
import Snackbar from '@mui/material/Snackbar';


export const ErrorAlert = () =>
{
  const report = useDispatch();
  const message = useSelector( state => state.problem );

  const dismissed = () => report( { type: 'ALERT_DISMISSED' } );

  return (
    <Snackbar open={!!message} onClose={dismissed}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}>
      <Alert variant='filled' severity="error" onClose={dismissed}>
        <AlertTitle>There's a problem</AlertTitle>
        {message}
      </Alert>
    </Snackbar>
  )
}

