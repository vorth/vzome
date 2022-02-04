import React from 'react';
import { makeStyles } from '@mui/styles';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';

import { AboutDialog } from './about.jsx';
import { OpenMenu } from './folder.jsx';
import { VZomeLogo } from './logo.jsx';

const useStyles = makeStyles((theme) => ({
  root: {
    zIndex: theme.zIndex.drawer + 1,
  },
  title: {
    marginLeft: theme.spacing(2),
    flexGrow: 1,
  },
  open: {
    marginRight: theme.spacing(5),
  },
}))

export const VZomeAppBar = ( { oneDesign } ) =>
{
  const classes = useStyles()

  return (
    <div id="appbar" className={classes.root}>
      <AppBar position="static">
        <Toolbar>
          <VZomeLogo/>
          <Typography variant="h5" className={classes.title}>
            vZome <Box component="span" fontStyle="oblique">Online</Box>
          </Typography>
          { !oneDesign && <OpenMenu className={classes.open} /> }
          <AboutDialog/>
        </Toolbar>
      </AppBar>
    </div>
  )
}
