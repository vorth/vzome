
package com.vzome.core.model;

import com.vzome.core.algebra.AlgebraicNumber;
import com.vzome.core.algebra.AlgebraicVector;

public class IndexedModel
{
    public String field;
    public String symmetry;
    public AlgebraicVector[] vertices;
    public int[] balls;
    public Strut[] struts;
    public Panel[] panels;
    
    public static class Strut
    {
        public int start;
        public int end;
        public String orbit;
        public AlgebraicNumber length;
        public int orientation;
        public String color;
    }
    
    public static class Panel
    {
        public int[] vertices;
        public String color;
    }

}
