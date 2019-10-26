
package com.vzome.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
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

        public AlgebraicNumber length;

        public String orbit;

        public int orientation;
        
        @JsonInclude( JsonInclude.Include.NON_NULL )
        public String color;
    }
    
    public static class Panel
    {
        public int[] vertices;
        
        public String color;
    }

}
