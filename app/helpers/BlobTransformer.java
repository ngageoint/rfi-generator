package helpers;

import flexjson.*;
import flexjson.transformer.*;
import org.joda.time.*;
import org.joda.time.format.*;

import play.db.jpa.*;

import java.io.*;

import org.apache.commons.io.*;
import org.apache.commons.codec.binary.Base64;

// Only used for serializing attachments (attachements are not currently enabled)
public class BlobTransformer extends AbstractTransformer {
    public BlobTransformer() {
    }
    
    public void transform(Object object) {
        boolean setContext = false;
 
        TypeContext typeContext = getContext().peekTypeContext();
 
        //Write comma before starting to write field name if this
        //isn't first property that is being transformed
        if (!typeContext.isFirst())
            getContext().writeComma();
 
        typeContext.setFirst(false);

        getContext().writeName(typeContext.getPropertyName());
        Blob b = (Blob)object;
        try {
            String s = FileUtils.readFileToString(b.getFile());
            getContext().writeQuoted(new String(Base64.encodeBase64(s.getBytes())));
        }
        catch (IOException e) {
            getContext().write("null");
        }
        
 
        if (setContext) {
            getContext().writeCloseObject();
        }
    }
    
    @Override
    public Boolean isInline() {
        return Boolean.TRUE;
    }
}
