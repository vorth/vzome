/* Generated from Java with JSweet 3.2.0-SNAPSHOT - http://www.jsweet.org */
namespace com.vzome.core.tools {
    export class MirrorToolFactory extends com.vzome.core.editor.AbstractToolFactory {
        public constructor(tools: com.vzome.core.editor.ToolsModel) {
            super(tools, null, com.vzome.core.tools.MirrorTool.ID, com.vzome.core.tools.MirrorTool.LABEL, com.vzome.core.tools.MirrorTool.TOOLTIP);
        }

        /**
         * 
         * @param {number} total
         * @param {number} balls
         * @param {number} struts
         * @param {number} panels
         * @return {boolean}
         */
        countsAreValid(total: number, balls: number, struts: number, panels: number): boolean {
            return (total === 2 && balls === 1 && struts === 1) || (total === 1 && panels === 1);
        }

        /**
         * 
         * @param {string} id
         * @return {com.vzome.core.editor.Tool}
         */
        public createToolInternal(id: string): com.vzome.core.editor.Tool {
            return new com.vzome.core.tools.MirrorTool(id, this.getToolsModel());
        }

        /**
         * 
         * @return {com.vzome.core.editor.Tool}
         */
        public createTool(): com.vzome.core.editor.Tool {
            const result: com.vzome.core.editor.Tool = super.createTool();
            result.setCopyColors(false);
            return result;
        }

        /**
         * 
         * @param {*} selection
         * @return {boolean}
         */
        bindParameters(selection: com.vzome.core.editor.api.Selection): boolean {
            return true;
        }
    }
    MirrorToolFactory["__class"] = "com.vzome.core.tools.MirrorToolFactory";
    MirrorToolFactory["__interfaces"] = ["com.vzome.core.editor.SelectionSummary.Listener","com.vzome.api.Tool.Factory"];


}
