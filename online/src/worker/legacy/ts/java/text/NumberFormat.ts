import { java, javaemul } from "../../../candies/j4ts-2.1.0-SNAPSHOT/bundle.js";

export class NumberFormat {
    public static getNumberInstance(us: java.util.Locale): NumberFormat {
        return new NumberFormat();
    }

    public static getInstance(): NumberFormat {
        return new NumberFormat();
    }

    public setMaximumFractionDigits(i: number) {
    }

    public setMinimumFractionDigits(i: number) {
    }

    public format(x: number): string {
        return /* toString */(''+(x));
    }

    public setGroupingUsed(newValue: boolean) {
    }
}
NumberFormat["__class"] = "java.text.NumberFormat";
