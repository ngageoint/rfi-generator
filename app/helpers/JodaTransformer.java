package helpers;

import flexjson.*;
import flexjson.transformer.*;
import org.joda.time.*;
import org.joda.time.format.*;

// Not currently using any Joda date/time stuff, but was prior and this was a
// handy serializer for serializing human/machine readable dates/times.
public class JodaTransformer extends AbstractTransformer {
    public JodaTransformer() {
    }
    
    public void transform(Object object) {
        boolean setContext = false;

        TypeContext typeContext = getContext().peekTypeContext();

        //Write comma before starting to write field name if this
        //isn't first property that is being transformed
        if (!typeContext.isFirst()) {
            getContext().writeComma();
        }

        typeContext.setFirst(false);

        getContext().writeName(typeContext.getPropertyName());
        DateTime date = (DateTime)object;
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        getContext().write(String.valueOf(date.getMillis()));
        getContext().writeComma();
        getContext().writeName(typeContext.getPropertyName()+"_human");
        getContext().writeQuoted(String.valueOf(fmt.print(date)));
 
        if (setContext) {
            getContext().writeCloseObject();
        }
    }
    
    @Override
    public Boolean isInline() {
        return Boolean.TRUE;
    }
}
