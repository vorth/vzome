
import React from 'react'
import { connect } from 'react-redux'
import { commandTriggered } from '../commands/index.js'
import * as designs from '../bundles/designs.js'
import { ActionCreators as UndoActionCreators } from 'redux-undo'
import IconButton from '@mui/material/IconButton';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import Divider from '@mui/material/Divider';

const ITEM_HEIGHT = 48;

const EditMenu = ({ visible, edits, doEdit, canUndo, canRedo, doUndo, doRedo }) =>
{
  const [anchorEl, setAnchorEl] = React.useState(null);
  const open = Boolean(anchorEl);

  const handleClick = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };
  const h4_c = { groupName: "H4", renderGroupName: "H4", index: 12, edgesToRender: 15 }
  if ( visible )
    return (
      <div>
        <IconButton id="editmenu"
          aria-label="more"
          aria-controls="long-menu"
          aria-haspopup="true"
          onClick={handleClick}
        >
          <MoreVertIcon />
        </IconButton>
        <Menu id="long-menu"
          anchorEl={anchorEl} keepMounted
          open={open} onClose={handleClose}
          PaperProps={{
            style: {
              maxHeight: ITEM_HEIGHT * 20,
              width: '30ch',
            },
          }}
        >
          <Divider />
          <MenuItem disabled={!canUndo} onClick={doUndo}>Undo</MenuItem>
          <MenuItem disabled={!canRedo} onClick={doRedo}>Redo</MenuItem>
          <Divider />
          <MenuItem onClick={ e => doEdit( 'allSelected' ) }>Select All</MenuItem>
          <MenuItem onClick={ e => doEdit( 'allDeselected' ) }>Deselect All</MenuItem>
          <MenuItem onClick={ e => doEdit( 'centroid' ) }>Centroid 1</MenuItem>
          <MenuItem onClick={ e => doEdit( 'shortred' ) }>Short Red 0</MenuItem>
          <MenuItem onClick={ e => doEdit( 'ShowPoint', { mode: 'origin' } ) }>Show Origin</MenuItem>
          <MenuItem onClick={ e => doEdit( 'Delete' ) }>Delete</MenuItem>
          <Divider />
          <MenuItem onClick={ e => doEdit( 'Polytope4d', h4_c ) }>H4 Polytope</MenuItem>
          <Divider />
          { edits.map( edit =>
            <MenuItem key={edit} onClick={ e => doEdit( edit ) } >
              {edit}
            </MenuItem>
          ) }
        </Menu>
      </div>
    )
  else
    return null
} 

const select = ( state ) =>
{
  const { models, commands } = state
  const history = models && designs.selectCurrentDesign( state ).history
  return {
    canUndo: history && history.past.length > 0,
    canRedo: history && history.future.length > 0,
    visible: !!commands,
    edits: commands && Object.getOwnPropertyNames( commands )
  }
}

const boundEventActions = {
  doEdit : commandTriggered,
  doUndo : UndoActionCreators.undo,
  doRedo : UndoActionCreators.redo,
}

export default connect( select, boundEventActions )( EditMenu )
