
import { Choices, Divider, Menu, MenuAction, createCheckboxItem } from "../../framework/menus.jsx";

import { controllerAction, controllerProperty } from "../../../workerClient/controllers-solid.js";
import { useWorkerClient } from "../../../workerClient/index.js";
import { useSymmetry } from "../classic.jsx";

export const SystemMenu = () =>
{
  const { rootController } = useWorkerClient();

  const { showOrbitsDialog, showShapesDialog } = useSymmetry();

  const symmetries = () => controllerProperty( rootController(), 'symmetryPerspectives', 'symmetryPerspectives', true );
  const currentSymm = () => controllerProperty( rootController(), 'symmetry' ); // TODO move to useSymmetry?
  const EditAction = createCheckboxItem( rootController() );
  const setSymmetry = system => {
    controllerAction( rootController(), `setSymmetry.${system}` );
  }

  return (
      <Menu label="System">
        <Choices label="Symmetry System" choices={symmetries()} choice={currentSymm()} setChoice={setSymmetry} />

        <Divider />
        
        <MenuAction label="Shapes..." onClick={ showShapesDialog } />

        <MenuAction label="Directions..." onClick={ showOrbitsDialog } />

        <EditAction label="Show Directions Graphically" action="toggleOrbitViews" disabled="true" checked="true" />
        <EditAction label="Show Strut Scales" action="toggleStrutScales" disabled="true" checked="true" />
        <EditAction label="Show Frame Labels" action="toggleFrameLabels" disabled="true" />
        <EditAction label="Show Panel Normals" action="toggleNormals" disabled="true" />

      </Menu>
  );
}
