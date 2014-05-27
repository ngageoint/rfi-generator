package helpers;

// Generic type helpers...
public class TypeHelpers {
    //TODO: Replace me w/ apache commons
    public static Boolean inList(String [] list, String fn) {
        for ( String s: list) {
            if ( s.contains(fn)) {
                return true;
            }
        }
        
        return false;
    }
    
    // Easy casting for reference types
    public static <T> T as(Class<T> t, Object o) {
        return t.isInstance(o) ? t.cast(o): null;
    }
}
