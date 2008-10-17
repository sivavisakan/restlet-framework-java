/**
 * Copyright 2005-2008 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of the following open
 * source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 (the "Licenses"). You can
 * select the license that you prefer but you may not use this file except in
 * compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.gnu.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.sun.com/cddl/cddl.html
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royaltee free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package org.restlet;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

/**
 * Router of calls from Server connectors to Restlets. The attached Restlets are
 * typically Applications.<br>
 * <br>
 * A virtual host is defined along three properties:
 * <ul>
 * <li>request's {@link Request#getHostRef()}: the URI of the host that received
 * the request. Note that the same IP address can correspond to multiple domain
 * names and therefore receive request with different "hostRef" URIs.</li>
 * <li>request's {@link Request#getResourceRef()}: the URI of the target
 * resource of the request. If this reference is relative, then it is based on
 * the "hostRef", otherwise it is maintained as received. This difference is
 * useful for resources identified by URNs or for Web proxies or Web caches.</li>
 * <li>response's {@link Response#getServerInfo()}: the information about the
 * server connector receiving the requests such as it IP address and port
 * number.</li>
 * </ul>
 * When creating a new instance, you can define Java regular expressions (
 * {@link java.util.regex.Pattern}) that must match the domain name, port,
 * scheme for references or IP address and port number for server information.
 * The default values match everything.
 * 
 * Concurrency note: instances of this class or its subclasses can be invoked by
 * several threads at the same time and therefore must be thread-safe. You
 * should be especially careful when storing state in member variables.
 * 
 * @see java.util.regex.Pattern
 * @see <a href="http://en.wikipedia.org/wiki/Virtual_hosting">Wikipedia -
 *      Virtual Hosting</a>
 * @see <a href="http://httpd.apache.org/docs/2.2/vhosts/">Apache - Virtual
 *      Hosting</a>
 * @author Jerome Louvel
 */
public class VirtualHost extends Router {
    private static final ThreadLocal<Integer> CURRENT = new ThreadLocal<Integer>();

    /**
     * Returns the virtual host code associated to the current thread.
     * 
     * This variable is stored internally as a thread local variable and updated
     * each time a call is routed by a virtual host.
     * 
     * @return The current context.
     */
    public static Integer getCurrent() {
        return CURRENT.get();
    }

    /**
     * Returns the IP address of a given domain name.
     * 
     * @param domain
     *            The domain name.
     * @return The IP address.
     */
    public static String getIpAddress(String domain) {
        String result = null;

        try {
            result = InetAddress.getByName(domain).getHostAddress();
        } catch (UnknownHostException e) {
        }

        return result;
    }

    /**
     * Returns the local host IP address.
     * 
     * @return The local host IP address.
     */
    public static String getLocalHostAddress() {
        String result = null;

        try {
            result = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
        }

        return result;
    }

    /**
     * Returns the local host name.
     * 
     * @return The local host name.
     */
    public static String getLocalHostName() {
        String result = null;

        try {
            result = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
        }

        return result;
    }

    /**
     * Sets the virtual host code associated with the current thread.
     * 
     * @param code
     *            The thread's virtual host code.
     */
    public static void setCurrent(Integer code) {
        CURRENT.set(code);
    }

    /** The hostRef host domain pattern to match. */
    private volatile String hostDomain;

    /** The hostRef host port pattern to match. */
    private volatile String hostPort;

    /** The hostRef scheme pattern to match. */
    private volatile String hostScheme;

    /** The display name. */
    private volatile String name;

    /** The resourceRef host domain pattern to match. */
    private volatile String resourceDomain;

    /** The resourceRef host port pattern to match. */
    private volatile String resourcePort;

    /** The resourceRef scheme pattern to match. */
    private volatile String resourceScheme;

    /** The listening server address pattern to match. */
    private volatile String serverAddress;

    /** The listening server port pattern to match. */
    private volatile String serverPort;

    /** The parent component's context. */
    private volatile Context parentContext;

    /**
     * Constructor. Note that usage of this constructor is not recommended as
     * the Router won't have a proper context set. In general you will prefer to
     * use the other constructor and pass it the parent component's context.
     */
    public VirtualHost() {
        this(null);
    }

    /**
     * Constructor. Accepts all incoming requests by default, use the set
     * methods to restrict the matchable patterns.
     * 
     * @param parentContext
     *            The parent component's context.
     */
    public VirtualHost(Context parentContext) {
        this(parentContext, ".*", ".*", ".*", ".*", ".*", ".*", ".*", ".*");
    }

    /**
     * Constructor.
     * 
     * @param parentContext
     *            The parent component's context.
     * @param hostDomain
     *            The hostRef host domain pattern to match.
     * @param hostPort
     *            The hostRef host port pattern to match.
     * @param hostScheme
     *            The hostRef scheme protocol pattern to match.
     * @param resourceDomain
     *            The resourceRef host domain pattern to match.
     * @param resourcePort
     *            The resourceRef host port pattern to match.
     * @param resourceScheme
     *            The resourceRef scheme protocol pattern to match.
     * @param serverAddress
     *            The listening server address pattern to match.
     * @param serverPort
     *            The listening server port pattern to match.
     */
    public VirtualHost(Context parentContext, String hostDomain,
            String hostPort, String hostScheme, String resourceDomain,
            String resourcePort, String resourceScheme, String serverAddress,
            String serverPort) {
        super((parentContext == null) ? null : parentContext
                .createChildContext());
        this.parentContext = parentContext;

        this.hostDomain = hostDomain;
        this.hostPort = hostPort;
        this.hostScheme = hostScheme;

        this.resourceDomain = resourceDomain;
        this.resourcePort = resourcePort;
        this.resourceScheme = resourceScheme;

        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    /**
     * Attaches a target Restlet to this router with an empty URI pattern. A new
     * route will be added routing to the target when any call is received.
     * 
     * In addition to super class behavior, this method will set the context of
     * the target if it is empty by creating a protected context via the
     * {@link Context#createChildContext()} method.
     * 
     * @param target
     *            The target Restlet to attach.
     * @return The created route.
     */
    @Override
    public Route attach(Restlet target) {
        if ((target.getContext() == null) && (this.parentContext != null)) {
            target.setContext(this.parentContext.createChildContext());
        }

        return super.attach(target);
    }

    /**
     * Attaches a target Restlet to this router based on a given URI pattern. A
     * new route will be added routing to the target when calls with a URI
     * matching the pattern will be received.
     * 
     * In addition to super class behavior, this method will set the context of
     * the target if it is empty by creating a protected context via the
     * {@link Context#createChildContext()} method.
     * 
     * @param uriPattern
     *            The URI pattern that must match the relative part of the
     *            resource URI.
     * @param target
     *            The target Restlet to attach.
     * @return The created route.
     */
    @Override
    public Route attach(String uriPattern, Restlet target) {
        if ((target.getContext() == null) && (this.parentContext != null)) {
            target.setContext(this.parentContext.createChildContext());
        }

        return super.attach(uriPattern, target);
    }

    /**
     * Attaches a Restlet to this router as the default target to invoke when no
     * route matches. It actually sets a default route that scores all calls to
     * 1.0.
     * 
     * In addition to super class behavior, this method will set the context of
     * the target if it is empty by creating a protected context via the
     * {@link Context#createChildContext()} method.
     * 
     * @param defaultTarget
     *            The Restlet to use as the default target.
     * @return The created route.
     */
    @Override
    public Route attachDefault(Restlet defaultTarget) {
        if ((defaultTarget.getContext() == null)
                && (this.parentContext != null)) {
            defaultTarget.setContext(this.parentContext.createChildContext());
        }

        return super.attachDefault(defaultTarget);
    }

    /**
     * Creates a new finder instance based on the "targetClass" property.
     * 
     * In addition to super class behavior, this method will set the context of
     * the finder by creating a protected context via the
     * {@link Context#createChildContext()} method.
     * 
     * @param targetClass
     *            The target Resource class to attach.
     * @return The new finder instance.
     */
    @Override
    protected Finder createFinder(Class<? extends Resource> targetClass) {
        Finder result = super.createFinder(targetClass);
        result.setContext(getContext().createChildContext());
        return result;
    }

    @Override
    protected Route createRoute(String uriPattern, Restlet target) {
        return new Route(this, uriPattern, target) {
            @Override
            protected int beforeHandle(Request request, Response response) {
                final int result = super.beforeHandle(request, response);

                // Set the request's root reference
                request.setRootRef(request.getResourceRef().getBaseRef());

                // Save the hash code of the current host
                setCurrent(VirtualHost.this.hashCode());

                return result;
            }
        };
    }

    /**
     * Returns the hostRef host domain to match. Uses patterns in
     * java.util.regex.
     * 
     * @return The hostRef host domain to match.
     */
    public String getHostDomain() {
        return this.hostDomain;
    }

    /**
     * Returns the hostRef host port to match. Uses patterns in java.util.regex.
     * 
     * @return The hostRef host port to match.
     */
    public String getHostPort() {
        return this.hostPort;
    }

    /**
     * Returns the hostRef scheme to match. Uses patterns in java.util.regex.
     * 
     * @return The hostRef scheme to match.
     */
    public String getHostScheme() {
        return this.hostScheme;
    }

    /**
     * Returns the display name.
     * 
     * @return The display name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the resourceRef host domain to match. Uses patterns in
     * java.util.regex.
     * 
     * @return The resourceRef host domain to match.
     */
    public String getResourceDomain() {
        return this.resourceDomain;
    }

    /**
     * Returns the resourceRef host port to match. Uses patterns in
     * java.util.regex.
     * 
     * @return The resourceRef host port to match.
     */
    public String getResourcePort() {
        return this.resourcePort;
    }

    /**
     * Returns the resourceRef scheme to match. Uses patterns in
     * java.util.regex.
     * 
     * @return The resourceRef scheme to match.
     */
    public String getResourceScheme() {
        return this.resourceScheme;
    }

    /**
     * Returns the listening server address. Uses patterns in java.util.regex.
     * 
     * @return The listening server address.
     */
    public String getServerAddress() {
        return this.serverAddress;
    }

    /**
     * Returns the listening server port. Uses patterns in java.util.regex.
     * 
     * @return The listening server port.
     */
    public String getServerPort() {
        return this.serverPort;
    }

    /**
     * Sets the hostRef host domain to match. Uses patterns in java.util.regex.
     * 
     * @param hostDomain
     *            The hostRef host domain to match.
     */
    public void setHostDomain(String hostDomain) {
        this.hostDomain = hostDomain;
    }

    /**
     * Sets the hostRef host port to match. Uses patterns in java.util.regex.
     * 
     * @param hostPort
     *            The hostRef host port to match.
     */
    public void setHostPort(String hostPort) {
        this.hostPort = hostPort;
    }

    /**
     * Sets the hostRef scheme to match. Uses patterns in java.util.regex.
     * 
     * @param hostScheme
     *            The hostRef scheme to match.
     */
    public void setHostScheme(String hostScheme) {
        this.hostScheme = hostScheme;
    }

    /**
     * Sets the display name.
     * 
     * @param name
     *            The display name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the resourceRef host domain to match. Uses patterns in
     * java.util.regex.
     * 
     * @param resourceDomain
     *            The resourceRef host domain to match.
     */
    public void setResourceDomain(String resourceDomain) {
        this.resourceDomain = resourceDomain;
    }

    /**
     * Sets the resourceRef host port to match. Uses patterns in
     * java.util.regex.
     * 
     * @param resourcePort
     *            The resourceRef host port to match.
     */
    public void setResourcePort(String resourcePort) {
        this.resourcePort = resourcePort;
    }

    /**
     * Sets the resourceRef scheme to match. Uses patterns in java.util.regex.
     * 
     * @param resourceScheme
     *            The resourceRef scheme to match.
     */
    public void setResourceScheme(String resourceScheme) {
        this.resourceScheme = resourceScheme;
    }

    /**
     * Sets the listening server address. Uses patterns in java.util.regex.
     * 
     * @param serverAddress
     *            The listening server address.
     */
    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    /**
     * Sets the listening server port. Uses patterns in java.util.regex.
     * 
     * @param serverPort
     *            The listening server port.
     */
    public void setServerPort(String serverPort) {
        this.serverPort = serverPort;
    }

}
