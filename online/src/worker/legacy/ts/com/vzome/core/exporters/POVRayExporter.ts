/* Generated from Java with JSweet 3.2.0-SNAPSHOT - http://www.jsweet.org */
namespace com.vzome.core.exporters {
    /**
     * Renders out to POV-Ray using #declare statements to reuse geometry.
     * @author vorth
     * @class
     * @extends com.vzome.core.exporters.DocumentExporter
     */
    export class POVRayExporter extends com.vzome.core.exporters.DocumentExporter {
            /**
             * Orders RenderedManifestations by content, for reproducible export.  Every component
             * is derived from what the manifestation IS, never from its identity (guid), so two
             * runs -- or two platforms -- computing the same geometry produce the same order.
             * Without this, the enclosing HashSet's iteration order varies, which reorders the
             * "object" lines AND changes the lazily generated "trans0"/"trans1" names, so
             * exporting one design twice produced two different .pov files.
             * Keep in sync with the Java POVRayExporter.compareForExport.
             */
            public static compareForExport(a: com.vzome.core.render.RenderedManifestation, b: com.vzome.core.render.RenderedManifestation): number {
                const nullSafe = (x: string, y: string): number => {
                    if (x == null) return y == null ? 0 : -1;
                    if (y == null) return 1;
                    return x < y ? -1 : (x > y ? 1 : 0);
                };
                let comparison: number = nullSafe(a.getShapeKey(), b.getShapeKey());
                if (comparison !== 0) return comparison;
                const ao = a.getOrientation(), bo = b.getOrientation();
                comparison = nullSafe(ao == null ? null : ao.toString(), bo == null ? null : bo.toString());
                if (comparison !== 0) return comparison;
                const al = a.getLocationAV(), bl = b.getLocationAV();
                if (al != null && bl != null) {
                    comparison = al.compareTo(bl);
                    if (comparison !== 0) return comparison;
                } else if (al !== bl) {
                    return al == null ? -1 : 1;
                }
                const ac = a.getColor(), bc = b.getColor();
                return nullSafe(ac == null ? null : ac.toString(), bc == null ? null : bc.toString());
            }

        static FORMAT: java.text.NumberFormat; public static FORMAT_$LI$(): java.text.NumberFormat { if (POVRayExporter.FORMAT == null) { POVRayExporter.FORMAT = java.text.NumberFormat.getNumberInstance(java.util.Locale.US); }  return POVRayExporter.FORMAT; }

        static PREAMBLE_FILE: string = "com/vzome/core/exporters/povray/preamble.pov";

        public mapViewToWorld(view: com.vzome.core.viewing.CameraIntf, vector: com.vzome.core.math.RealVector) {
        }

        /**
         * 
         * @return {boolean}
         */
        public needsManifestations(): boolean {
            return false;
        }

        /**
         * 
         * @param {java.io.File} povFile
         * @param {java.io.Writer} writer
         * @param {number} height
         * @param {number} width
         */
        public doExport(povFile: java.io.File, writer: java.io.Writer, height: number, width: number) {
            this.output = new java.io.PrintWriter(writer);
            const lookDir: com.vzome.core.math.RealVector = this.mScene.getLookDirectionRV();
            const upDir: com.vzome.core.math.RealVector = this.mScene.getUpDirectionRV();
            POVRayExporter.FORMAT_$LI$().setMaximumFractionDigits(8);
            this.output.println$();
            this.output.println$();
            this.output.println$java_lang_Object("#declare           look_dir = " + this.printTuple3d(lookDir) + ";");
            this.output.println$();
            this.output.println$java_lang_Object("#declare             up_dir = " + this.printTuple3d(upDir) + ";");
            this.output.println$();
            this.output.println$java_lang_Object("#declare viewpoint_distance = " + POVRayExporter.FORMAT_$LI$().format(this.mScene.getViewDistance()) + ";");
            this.output.println$();
            this.output.println$java_lang_Object("#declare      look_at_point = " + this.printTuple3d(this.mScene.getLookAtPointRV()) + ";");
            this.output.println$();
            this.output.println$java_lang_Object("#declare      field_of_view = " + POVRayExporter.FORMAT_$LI$().format(this.mScene.getFieldOfView()) + ";");
            this.output.println$();
            this.output.println$java_lang_Object("#declare      parallel_proj = " + (this.mScene.isPerspective() ? 0 : 1) + ";");
            this.output.println$();
            const preamble: string = com.vzome.xml.ResourceLoader.loadStringResource(POVRayExporter.PREAMBLE_FILE);
            this.output.println$java_lang_Object(preamble);
            this.output.println$();
            for(let i: number = 0; i < 3; i++) {{
                const color: com.vzome.core.construction.Color = this.mLights.getDirectionalLightColor(i);
                let rv: com.vzome.core.math.RealVector = this.mLights.getDirectionalLightVector(i);
                rv = this.mScene.mapViewToWorld(rv);
                this.output.print("light_source { -light_distance * " + this.printTuple3d(rv));
                this.output.print(" ");
                this.printColor(color);
                this.output.println$java_lang_Object(" * multiplier_light_" + (i + 1) + " }");
                this.output.println$();
            };}
            this.output.print("#declare ambient_color = ");
            this.printColor(this.mLights.getAmbientColor());
            this.output.println$java_lang_Object(";");
            this.output.println$();
            this.output.println$java_lang_Object("#default { texture { finish { phong 0.3 ambient multiplier_ambient * ambient_color diffuse 0.6 } } }");
            this.output.println$();
            this.output.print("background { ");
            this.printColor(this.mLights.getBackgroundColor());
            this.output.println$java_lang_Object(" }");
            this.output.println$();
            const instances: java.lang.StringBuffer = new java.lang.StringBuffer();
            const field: com.vzome.core.algebra.AlgebraicField = this.mModel.getField();
            const embedding: com.vzome.core.math.symmetry.Embedding = this.mModel.getEmbedding();
            let embeddingTransform: string = " ";
            if (!embedding.isTrivial()){
                embeddingTransform = " transform embedding ";
                this.output.print("#declare embedding = transform { matrix < ");
                for(let i: number = 0; i < 3; i++) {{
                    const columnSelect: com.vzome.core.algebra.AlgebraicVector = field.basisVector(3, i);
                    const columnI: com.vzome.core.math.RealVector = embedding.embedInR3(columnSelect);
                    this.output.print(columnI.x);
                    this.output.print(", ");
                    this.output.print(columnI.y);
                    this.output.print(", ");
                    this.output.print(columnI.z);
                    this.output.print(", ");
                };}
                this.output.println$java_lang_Object(" 0, 0, 0 > }");
                this.output.flush();
            }
            let numTransforms: number = 0;
            const shapes: java.util.HashSet<string> = <any>(new java.util.HashSet<any>());
            const transforms: java.util.Map<string, string> = <any>(new java.util.HashMap<any, any>());
            const colors: java.util.Map<string, string> = <any>(new java.util.HashMap<any, any>());
            const sorted: com.vzome.core.render.RenderedManifestation[] = [];
            for(let index=this.mModel.iterator();index.hasNext();) {
                sorted.push(index.next());
            }
            sorted.sort(POVRayExporter.compareForExport);
            for(const rm of sorted) {
                {
                    const shapeName: string = "S" + /* replaceAll */rm.getShapeKey().replace(new RegExp("[^A-Za-z0-9_]", 'g'),"_");
                    if (!shapes.contains(shapeName)){
                        shapes.add(shapeName);
                        this.exportShape(shapeName, rm.getShape());
                    }
                    const transform: com.vzome.core.algebra.AlgebraicMatrix = rm.getOrientation();
                    // Key by content, not object identity: Java dedups with a HashMap keyed on
                    // AlgebraicMatrix, whose equals/hashCode compare by value, but here the map
                    // compares references, so equal matrices each got their own "transN".
                    const transformKey: string = "" + transform;
                    let transformName: string = transforms.get(transformKey);
                    if (transformName == null){
                        transformName = "trans" + numTransforms++;
                        transforms.put(transformKey, transformName);
                        this.exportTransform(transformName, transform);
                    }
                    let color: com.vzome.core.construction.Color = rm.getColor();
                    if (color == null)color = com.vzome.core.construction.Color.WHITE_$LI$();
                    const colorKey: string = "" + color;
                    let colorName: string = colors.get(colorKey);
                    if (colorName == null){
                        colorName = this.nameColor(color);
                        colors.put(colorKey, colorName);
                        this.exportColor(colorName, color);
                    }
                    instances.append("object { " + shapeName + " transform " + transformName + " translate ");
                    instances.append("(<");
                    let loc: com.vzome.core.algebra.AlgebraicVector = rm.getLocationAV();
                    if (loc == null)loc = rm.getShape().getField().origin(3);
                    this.appendVector(loc, instances);
                    instances.append(">)");
                    instances.append(embeddingTransform + "transform anim texture { " + colorName + " } }");
                    instances.append(java.lang.System.getProperty("line.separator"));
                }
            }
            this.output.println$java_lang_Object(instances.toString());
            this.output.flush();
            if (povFile == null)return;
            let filename: string = povFile.getName();
            const index: number = filename.lastIndexOf(".pov");
            if (index > 0){
                filename = filename.substring(0, index);
            }
            const file: java.io.File = new java.io.File(povFile.getParentFile(), filename + ".ini");
            this.output = new java.io.PrintWriter(new java.io.FileWriter(file));
            this.output.println$java_lang_Object("+W" + 600);
            this.output.println$java_lang_Object("+H" + 600);
            this.output.println$java_lang_Object("+A");
            this.output.println$java_lang_Object("Input_File_Name=" + filename + ".pov");
            this.output.println$java_lang_Object("Output_File_Name=" + filename + ".png");
            this.output.close();
        }

        nameColor(color: com.vzome.core.construction.Color): string {
            return "color_" + /* replace */color.toString().split(',').join('_');
        }

        /*private*/ printTuple3d(t: com.vzome.core.math.RealVector): string {
            const buf: java.lang.StringBuilder = new java.lang.StringBuilder("<");
            buf.append(POVRayExporter.FORMAT_$LI$().format(t.x));
            buf.append(",");
            buf.append(POVRayExporter.FORMAT_$LI$().format(t.y));
            buf.append(",");
            buf.append(POVRayExporter.FORMAT_$LI$().format(t.z));
            buf.append(">");
            return buf.toString();
        }

        exportColor(name: string, color: com.vzome.core.construction.Color) {
            this.output.print("#declare " + /* replace */name.split('.').join('_') + " = texture { pigment { ");
            this.printColor(color);
            this.output.println$java_lang_Object(" } };");
        }

        /*private*/ printColor(color: com.vzome.core.construction.Color) {
            const doAlpha: boolean = color.getAlpha() < 255;
            if (doAlpha)this.output.print("color rgbf <"); else this.output.print("color rgb <");
            const rgb: number[] = color.getRGBColorComponents([0, 0, 0, 0]);
            this.output.print(POVRayExporter.FORMAT_$LI$().format(rgb[0]) + ",");
            this.output.print(POVRayExporter.FORMAT_$LI$().format(rgb[1]) + ",");
            if (doAlpha){
                this.output.print(POVRayExporter.FORMAT_$LI$().format(rgb[2]) + ",");
                this.output.print(POVRayExporter.FORMAT_$LI$().format(rgb[3]));
            } else {
                this.output.print(POVRayExporter.FORMAT_$LI$().format(rgb[2]));
            }
            this.output.print(">");
        }

        appendVector(loc: com.vzome.core.algebra.AlgebraicVector, buf: java.lang.StringBuffer) {
            const vector: com.vzome.core.math.RealVector = loc.toRealVector();
            buf.append(POVRayExporter.FORMAT_$LI$().format(vector.x));
            buf.append(", ");
            buf.append(POVRayExporter.FORMAT_$LI$().format(vector.y));
            buf.append(", ");
            buf.append(POVRayExporter.FORMAT_$LI$().format(vector.z));
        }

        /*private*/ exportShape(shapeName: string, poly: com.vzome.core.math.Polyhedron) {
            this.output.print("#declare " + shapeName + " = ");
            const vertices: java.util.List<com.vzome.core.algebra.AlgebraicVector> = poly.getVertexList();
            this.output.println$java_lang_Object("mesh {");
            // Emit the triangles in a stable order; see the Java POVRayExporter for why (the
            // faces live in a HashSet, which iterates differently here than it does in Java).
            // Sorting the rendered text changes no geometry.
            const triangles: string[] = [];
            for(let index=poly.getTriangleFaces().iterator();index.hasNext();) {
                let face = index.next();
                {
                    const buf: java.lang.StringBuffer = new java.lang.StringBuffer();
                    buf.append("triangle {");
                    for(let loopIndex = 0; loopIndex < face.vertices.length; loopIndex++) {
                        let index = face.vertices[loopIndex];
                        {
                            const loc: com.vzome.core.algebra.AlgebraicVector = vertices.get(index);
                            buf.append("<");
                            this.appendVector(loc, buf);
                            buf.append(">");
                        }
                    }
                    buf.append("}");
                    triangles.push(buf.toString());
                }
            }
            triangles.sort();
            for(const triangle of triangles) {
                this.output.println$java_lang_Object(triangle);
            }
            this.output.println$java_lang_Object("}");
            this.output.flush();
        }

        /*private*/ exportTransform(name: string, transform: com.vzome.core.algebra.AlgebraicMatrix) {
            const field: com.vzome.core.algebra.AlgebraicField = this.mModel.getField();
            this.output.print("#declare " + name + " = transform { matrix < ");
            const buf: java.lang.StringBuffer = new java.lang.StringBuffer();
            for(let i: number = 0; i < 3; i++) {{
                const columnSelect: com.vzome.core.algebra.AlgebraicVector = field.basisVector(3, i);
                const columnI: com.vzome.core.algebra.AlgebraicVector = transform.timesColumn(columnSelect);
                this.appendVector(columnI, buf);
                buf.append(", ");
            };}
            this.output.print(buf);
            this.output.println$java_lang_Object(" 0, 0, 0 > }");
            this.output.flush();
        }

        /**
         * 
         * @return {string}
         */
        public getFileExtension(): string {
            return "pov";
        }

        constructor() {
            super();
        }
    }
    POVRayExporter["__class"] = "com.vzome.core.exporters.POVRayExporter";
    POVRayExporter["__interfaces"] = ["com.vzome.core.exporters.DocumentExporterIntf"];


}

