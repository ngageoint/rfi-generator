package controllers;

import play.*;
import play.mvc.Router;

import java.util.*;

import helpers.*;

import controllers.deadbolt.DeadboltHandler;
import controllers.deadbolt.ExternalizedRestrictionsAccessor;
import controllers.deadbolt.RestrictedResourcesHandler;
import models.User;
import models.deadbolt.*;
import play.mvc.Controller;

/**
* @author Steve Chaloner (steve@objectify.be)
*/

// Nabbaed and modified this example from Steve Chaloner
public class MyDeadboltHandler extends Controller implements DeadboltHandler
{
    public void beforeRoleCheck()
    {
        // Note that if you provide your own implementation of Secure's Security class you would refer to that instead
        if (!Master.isConnected())
        {
            redirect(ConfigurationHelpers.GetSSORedirectURL());
        }
    }

    public RoleHolder getRoleHolder()
    {
        String userName = Master.connected();
        User usr = User.getByUserName(userName);
        return usr;
    }

    public void onAccessFailure(String controllerClassName)
    {
       forbidden();
    }

    public ExternalizedRestrictionsAccessor getExternalizedRestrictionsAccessor()
    {
        return new ExternalizedRestrictionsAccessor()
        {
            public ExternalizedRestrictions getExternalizedRestrictions(String name)
            {
                return null;
            }
        };
    }

    public RestrictedResourcesHandler getRestrictedResourcesHandler()
    {
        return null;
    }
}
