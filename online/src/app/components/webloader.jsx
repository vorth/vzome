
import React, { useState } from 'react'
import TextField from '@mui/material/TextField';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import Button from '@mui/material/Button';

export default ({ show, setShow, openDesign }) =>
{
  const [url, setUrl] = useState( '' )

  const handleCancel = () =>{
    setShow( false )
  }
  const handleOpen = () =>{
    setShow( false )
    openDesign( url )
  }
  const handleChange = (event) => setUrl( event.target.value )

  return (
    <Dialog open={show} onClose={handleCancel} aria-labelledby="form-dialog-title" maxWidth='lg' fullWidth={true}>
      <DialogTitle id="form-dialog-title">Load a Remote vZome Design</DialogTitle>
      <DialogContent>
        <DialogContentText>
          The URL must be open to public access.
        </DialogContentText>
        <TextField onChange={handleChange}
          autoFocus
          margin="dense"
          id="name"
          label="vZome design URL"
          type="url"
          fullWidth
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={handleCancel} color="secondary">
          Cancel
        </Button>
        <Button onClick={handleOpen} color="primary">
          Open
        </Button>
      </DialogActions>
    </Dialog>
  )
} 


