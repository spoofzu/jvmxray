package org.owasp.jvmxray.api;

import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;


public class NullPolicy extends Policy {

	private static PermissionCollection perms;
	
	public NullPolicy() {}
	
	  private boolean busy = false;
	
	  private static class AllPermissionsSingleton extends PermissionCollection
	    {
	        private static final long serialVersionUID = 1L;
	        private static final Vector<Permission> ALL_PERMISSIONS_VECTOR = new Vector<Permission>(Arrays.asList(new AllPermission()));

	        @Override
	        public void add(Permission permission)
	        {
	        }

	        @Override
	        public boolean implies(Permission permission)
	        {
	            return true;
	        }

	        @Override
	        public Enumeration<Permission> elements()
	        {
	            return ALL_PERMISSIONS_VECTOR.elements();
	        }

	        @Override
	        public boolean isReadOnly()
	        {
	            return false;
	        }
	    }

	    private static final AllPermissionsSingleton ALL_PERMISSIONS_SINGLETON = new AllPermissionsSingleton();

	    @Override
	    public synchronized PermissionCollection getPermissions(CodeSource codesource) {
//    		if( busy ) return ALL_PERMISSIONS_SINGLETON;
//    		busy = true;
//	    	System.out.println("NullPolicy.getPermissions(CodeSource)-->"+codesource.toString());
//    		busy = false;
	        return ALL_PERMISSIONS_SINGLETON;
	    }
	
	    @Override
	    public synchronized PermissionCollection getPermissions(ProtectionDomain domain) {
//    		if( busy ) return ALL_PERMISSIONS_SINGLETON;
//    		busy = true;
//    		System.out.println("NullPolicy.getPermissions(ProtectionDomain)-->"+domain.toString());
//    		busy = false;
	        return ALL_PERMISSIONS_SINGLETON;
	    }
	   

}
