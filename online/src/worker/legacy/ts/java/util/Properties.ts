import { java, javaemul } from "../../../candies/j4ts-2.1.0-SNAPSHOT/bundle.js";

export class Properties {
    public getProperty(key: string): string {
        return "";
    }

    public load(inStream: java.io.InputStream) {
    }

    public isEmpty(): boolean {
        return true;
    }
}
Properties["__class"] = "java.util.Properties";
