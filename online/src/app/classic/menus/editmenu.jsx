
import Button from "@suid/material/Button"
import Menu from "@suid/material/Menu"
import Divider from "@suid/material/Divider";
import { createSignal } from "solid-js";

import { createMenuAction } from "../components/menuaction.jsx";
import { subController } from "../controllers-solid.js";

export const EditMenu = ( props ) =>
{
  const [ anchorEl, setAnchorEl ] = createSignal( null );
  const open = () => Boolean( anchorEl() );
  const doClose = () => setAnchorEl( null );

  const EditAction = createMenuAction( props.controller, doClose );
  const undoRedoController = () => subController( props.controller, 'undoRedo' );
  const UndoRedoAction = createMenuAction( undoRedoController(), doClose );

  return (
    <div>
      <Button id="edit-menu-button"
        aria-controls={open() ? "edit-menu-menu" : undefined} aria-haspopup="true" aria-expanded={open() ? "true" : undefined}
        onClick={ (event) => setAnchorEl(event.currentTarget) }
      >
        Edit
      </Button>
      <Menu id="edit-menu-menu" MenuListProps={{ "aria-labelledby": "edit-menu-button" }}
        anchorEl={anchorEl()} open={open()} onClose={doClose}
      >
        <UndoRedoAction label="Undo"     action="undo"    mods="⌘" key="Z" />
        <UndoRedoAction label="Redo"     action="redo"    mods="⌘" key="Y" />
        <UndoRedoAction label="Undo All" action="undoAll" mods="⌥⌘" key="Z" />
        <UndoRedoAction label="Redo All" action="redoAll" mods="⌥⌘" key="Y" />

        <Divider />

        <EditAction label="Cut"    action="cut"    mods="⌘" key="X" disabled={true} />
        <EditAction label="Copy"   action="copy"   mods="⌘" key="C" disabled={true} />
        <EditAction label="Paste"  action="paste"  mods="⌘" key="V" disabled={true} />
        <EditAction label="Delete" action="Delete" code="Delete|Backspace" />

        <Divider />

        <EditAction label="Select All"       action="SelectAll"       mods="⌘" key="A" />
        <EditAction label="Select Neighbors" action="SelectNeighbors" mods="⌥⌘" key="A" />
        <EditAction label="Invert Selection" action="InvertSelection" />

        <Divider />

        <EditAction label="Group"   action="GroupSelection/group" mods="⌘" key="G" />
        <EditAction label="Ungroup" action="GroupSelection/ungroup" mods="⌥⌘" key="G" />

        <Divider />

        <EditAction label="Hide"            action="hideball"   mods="⌃" key="H" />
        <EditAction label="Show All Hidden" action="ShowHidden" mods="⌥⌃" key="H" />

      </Menu>
    </div>
  );
}
