package authenticationProviders;

import play.*;
/*
* Singleton-pattern authentication factory - all hand-rolled...
*/
public class AuthFactory  {
    public static IAuthenticationProvider inst = null;

    public static IAuthenticationProvider instance() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if ( inst != null) {
            return inst;
        }
        else {
            String cls = Play.configuration.get("authClass").toString();
            inst = (IAuthenticationProvider)Class.forName(cls).newInstance();
            Logger.info("Auth setup %s", cls);
            return inst;
        }
    }
}
