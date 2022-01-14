
import React from 'react';
import { withStyles } from '@mui/styles'
import Dialog from '@mui/material/Dialog'
import MuiDialogTitle from '@mui/material/DialogTitle'
import MuiDialogContent from '@mui/material/DialogContent'
import IconButton from '@mui/material/IconButton'
import CloseIcon from '@mui/icons-material/Close'
import Typography from '@mui/material/Typography'
import InfoRoundedIcon from '@mui/icons-material/InfoRounded'
import Link from '@mui/material/Link'
import Tooltip from '@mui/material/Tooltip'

const styles = (theme) => ({
  root: {
    margin: 0,
    padding: theme.spacing(2),
  },
  closeButton: {
    position: 'absolute',
    right: theme.spacing(1),
    top: theme.spacing(1),
    color: theme.palette.grey[500],
  },
})

const DialogTitle = withStyles(styles)((props) => {
  const { children, classes, onClose, ...other } = props;
  return (
    <MuiDialogTitle disableTypography className={classes.root} {...other}>
      <Typography variant="h6">{children}</Typography>
      {onClose ? (
        <IconButton aria-label="close" className={classes.closeButton} onClick={onClose}>
          <CloseIcon />
        </IconButton>
      ) : null}
    </MuiDialogTitle>
  )
})

const DialogContent = withStyles((theme) => ({
  root: {
    padding: theme.spacing(2),
  },
}))(MuiDialogContent)

export const AboutDialog = () =>
{
  const [open, setOpen] = React.useState(false);

  const handleClickOpen = () => {
    setOpen(true)
  }
  const handleClose = () => {
    setOpen(false)
  }

  return (
    <>
      <Tooltip title="About vZome Online" aria-label="about">
        <IconButton color="inherit" aria-label="about" onClick={handleClickOpen}>
          <InfoRoundedIcon fontSize="large"/>
        </IconButton>
      </Tooltip>
      <Dialog onClose={handleClose} aria-labelledby="customized-dialog-title" open={open}>
        <DialogTitle id="customized-dialog-title" onClose={handleClose}>
          About vZome Online
        </DialogTitle>
        <DialogContent dividers>
          <Typography gutterBottom>
            vZome Online is the world's first in-browser modeling tool
            for <Link target="_blank" href="https://zometool.com" rel="noopener" >Zometool</Link>
            ... or it will be soon.
          </Typography>
          <Typography gutterBottom>
            Right now, you can load and view existing vZome designs, created using
            the <Link target="_blank" rel="noopener" href="https://vzome.com/home/index/vzome-7/">vZome desktop app</Link>.
            Click on the folder icon try out some of the built-in designs, or load one of your own!
          </Typography>
          <Typography gutterBottom>
            At the moment, you cannot modify designs or create new designs.  I'm working to complete those features
            (and all the other features of desktop vZome)
            as soon as possible;
            my top priority so far has been to support loading existing designs.
            If you want to stay informed about my progress, follow vZome
            on <Link target="_blank" rel="noopener" href="https://www.facebook.com/vZome">Facebook</Link> or <Link target="_blank" rel="noopener" href="https://twitter.com/vZome">Twitter</Link>.
          </Typography>
          <Typography gutterBottom>
            Unfortunately, some vZome designs will not load successfully.
            If you have a vZome design that does not load
            here, <Link target="_blank" rel="noopener" href="mailto:info@vzome.com">send me the vZome file</Link>,
            and I can prioritize the necessary fixes.
          </Typography>
        </DialogContent>
        {/* <DialogActions>
          <Button autoFocus onClick={handleClose} color="primary">
            Download vZome Desktop App
          </Button>
        </DialogActions> */}
      </Dialog>
    </>
  )
}
