package helpers;

import java.lang.reflect.*;

import flexjson.*;
import flexjson.factories.*;

// Used only in serialization for explicitly ignoring properties
public class IgnoreObjectFactory implements ObjectFactory {
    public Object instantiate(ObjectBinder context,
        Object value,
        Type targetType,
        Class targetClass)
    {
        return null;
    }
}
