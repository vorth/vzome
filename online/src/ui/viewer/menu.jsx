
import React from 'react';
import { useSelector } from 'react-redux';

import IconButton from '@material-ui/core/IconButton';
import Menu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import MenuRoundedIcon from '@material-ui/icons/MenuRounded';
import Link from '@material-ui/core/Link';

// from https://www.bitdegree.org/learn/javascript-download
const download = source =>
{
  const { name, text } = source;
  const blob = new Blob( [ text ], { type : 'application/xml' } );
  const element = document.createElement( 'a' )
  const blobURI = URL.createObjectURL( blob )
  element.setAttribute( 'href', blobURI )
  element.setAttribute( 'download', `${name}` )
  element.style.display = 'none'
  document.body.appendChild( element )
  element.click()
  document.body.removeChild( element )
}

export const ActionMenu = () =>
{
  const [ anchorEl, setAnchorEl ] = React.useState(null);
  const source = useSelector( state => state.source );

  const showMenu = (event) =>
  {
    setAnchorEl( event.currentTarget );
  };

  const handleDownload = () =>
  {
    handleClose();
    download( source );
  };

  const handleClose = () =>
  {
    setAnchorEl( null );
  };

  return (
    <div>
      <IconButton color="inherit" aria-label="menu" aria-controls="action-menu" aria-haspopup="true"
          style={ { position: 'absolute', top: '5px', right: '5px' } }
          onClick={showMenu} >
        <MenuRoundedIcon fontSize='medium'/>
      </IconButton>
      <Menu
        id="action-menu"
        anchorEl={anchorEl}
        keepMounted
        open={Boolean(anchorEl)}
        onClose={handleClose}
      >
        <MenuItem onClick={handleDownload}>Download</MenuItem>
        <MenuItem component={Link} href='https://vzome.com/app' target="_blank" rel="noopener">
          Open in vZome Online
        </MenuItem>
        <MenuItem onClick={handleClose}>About</MenuItem>
      </Menu>
    </div>
  );
}
