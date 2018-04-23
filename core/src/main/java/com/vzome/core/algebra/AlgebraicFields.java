package com.vzome.core.algebra;

public class AlgebraicFields {
    private AlgebraicFields() {}
    
    /**
     * 
     * @param field
     * @param subFieldName
     * @return true, only if the named subField is of equal or lower order than field
     * and all of the coefficients of the named fubField are equal to 
     * the coefficients of field in corresponding positions.
     * <br>
     * For example, if field is a SnubDodecField, and {@code PentagonField.FIELD_NAME.equals(subFieldName)},
     * then this method will return true because {@code PentagonField.getOrder() <= SnubDodecField.getOrder()}
     * and both have phi as their second coefficient.
     * Note that all AlgebraicFields will have 1 as their first coefficient.
     * <br>
     * The significant implication of returning true is that any AlgebraicNumber of the subField 
     * will evaluate to the same double value as an AlgebraicNumber created by the field, given the same factors.
     * <br>
     * When this method returns true, then...
     * <br>
     * {@code 
        new PentagonField(). createAlgebraicNumber( 2, 3 ).evaluate() ==
        new SnubDodecField().createAlgebraicNumber( 2, 3 ).evaluate();
        }
     * <br>
     * Some of the specialty fields that are in the works will have some common coefficients such as phi, 
     * but will not necessarily have them in the same order or position. 
     * In that case, this method will return false when they are compared.
     * <br>
     * TODO: I eventually intend to provide a FieldMapper that will facilitate similar functionality 
     * for the VefParser in those cases where the only difference is the position of the coefficients, not their value.      
     * <br>
     * Initially, this method was designed to allow the SnubDodecField to parse PentagonField based VEF data
     * without the need to mix AlgebraicNumbers generated by different fields within the same model.
     * This method will be of particular use when using existing VEF trackballs in models of upcoming specialty fields.  
     */
    public static boolean haveSameInitialCoefficients(AlgebraicField field, String subFieldName)
    {
        if(field.getName().equals(subFieldName)) {
            return true;
        }

        double[] fieldCoeff = getCoefficients(field.getName());
        double[] subFieldCoeff = getCoefficients(subFieldName);
        
        if(fieldCoeff.length < subFieldCoeff.length) {
            return false;
        }
        
        int i = 0;
        for(double coefficient : subFieldCoeff) {
            // subtract and compare the difference to zero 
            // instead of comparing floating point numbers directly
            if(fieldCoeff[i++] - coefficient != 0d) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 
     * @param fieldName the common name of the desired AlgebraicField.
     * Note that this is not the class name, but rather the value returned by {@Code getName()}.
     * For example, use "golden" to retrieve the coefficients of a PentagonField.
     * @return an array containing the coefficients of the named field. 
     * The length of the array will be equal to the order of the field.
     * If fieldName does not specify the name of a supported AlgebraicField, 
     * then the length of the array will be 0.
     */
    public static double[] getCoefficients(String fieldName) {
        double[] coefficients = new double[] {};
        switch( fieldName ) {
        case PentagonField.FIELD_NAME:
            coefficients = PentagonField.getFieldCoefficients();
            break;
            
        case RootTwoField.FIELD_NAME:
            coefficients = RootTwoField.getFieldCoefficients();
            break;
            
        case RootThreeField.FIELD_NAME:
            coefficients = RootThreeField.getFieldCoefficients();
            break;
            
        case HeptagonField.FIELD_NAME:
            coefficients = HeptagonField.getFieldCoefficients();
            break;
            
        case SnubDodecField.FIELD_NAME:
            coefficients = SnubDodecField.getFieldCoefficients();
            break;

        default:
            break;
        }
        return coefficients;
    }

}