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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.ReferenceList;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.restlet.util.Engine;

/**
 * Finder mapping a directory of local resources. Those resources have
 * representations accessed by the file system or the class loaders.<br>
 * <br>
 * An automatic content negotiation mechanism (similar to the one in Apache HTTP
 * server) is used to select the best representation of a resource based on the
 * available variants and on the client capabilities and preferences.<br>
 * <br>
 * The directory can be used in read-only or modifiable mode. In the latter
 * case, you just need to set the "modifiable" property to true. The currently
 * supported methods are PUT and DELETE.<br>
 * <br>
 * When no index is available in a given directory, a representation can be
 * automatically generated by the
 * {@link #getIndexRepresentation(Variant, ReferenceList)} method, unless the
 * "listingAllowed" property is turned off. You can even customize the way the
 * index entries are sorted by using the {@link #setComparator(Comparator)}
 * method. The default sorting uses the friendly Alphanum algorithm based on
 * David Koelle's <a href="http://www.davekoelle.com/alphanum.html">original
 * idea</a>, using a different and faster implementation contributed by Rob
 * Heittman.<br>
 * <br>
 * Concurrency note: instances of this class or its subclasses can be invoked by
 * several threads at the same time and therefore must be thread-safe. You
 * should be especially careful when storing state in member variables.
 * 
 * @see <a
 *      href="http://www.restlet.org/documentation/1.1/tutorial#part06">Tutorial
 *      : Serving static files</a>
 * @author Jerome Louvel
 */
public class Directory extends Finder {

    /**
     * Allows to sort the list of references set by the resource.
     * 
     */
    private class AlphabeticalComparator implements Comparator<Reference>,
            Serializable {
        private static final long serialVersionUID = 1L;

        public int compare(Reference rep0, Reference rep1) {
            final boolean bRep0Null = (rep0.getIdentifier() == null);
            final boolean bRep1Null = (rep1.getIdentifier() == null);

            if (bRep0Null && bRep1Null) {
                return 0;
            }
            if (bRep0Null) {
                return -1;
            }
            if (bRep1Null) {
                return 1;
            }
            return compare(rep0.toString(false, false), rep1.toString(false,
                    false));
        }

        public int compare(final String uri0, final String uri1) {
            return uri0.compareTo(uri1);
        }
    }

    /**
     * Optimized public-domain implementation of a Java alphanumeric sort.
     * <p>
     * 
     * This implementation uses a single comparison pass over the characters in
     * a CharSequence, and returns as soon as a differing character is found,
     * unless the difference occurs in a series of numeric characters, in which
     * case that series is followed to its end. Numeric series of equal length
     * are compared numerically, that is, according to the most significant
     * (leftmost) differing digit. Series of unequal length are compared by
     * their length.
     * <p>
     * 
     * This implementation appears to be 2-5 times faster than alphanumeric
     * comparators based based on substring analysis, with a lighter memory
     * footprint.
     * <p>
     * 
     * This alphanumeric comparator has approximately 20%-50% the performance of
     * the lexical String.compareTo() operation. Character sequences without
     * numeric data are compared more quickly.
     * <p>
     * 
     * Dedicated to the public domain by the original author:
     * http://creativecommons.org/licenses/publicdomain/
     * 
     * @author Rob Heittman, <a href="http://www.solertium.com">Solertium
     *         Corporation</a>
     */
    private class AlphaNumericComparator extends AlphabeticalComparator {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(final String uri0, final String uri1) {
            int ptr = 0;
            int msd = 0;
            int diff = 0;
            char a, b;

            final int llength = uri0.length();
            final int rlength = uri1.length();
            final int min;

            if (rlength < llength) {
                min = rlength;
            } else {
                min = llength;
            }

            boolean rAtEnd, rHasNoMoreDigits;

            while (ptr < min) {
                a = uri0.charAt(ptr);
                b = uri1.charAt(ptr);
                diff = a - b;
                if ((a > '9') || (b > '9') || (a < '0') || (b < '0')) {
                    if (diff != 0) {
                        return diff;
                    }
                    msd = 0;
                } else {
                    if (msd == 0) {
                        msd = diff;
                    }
                    rAtEnd = rlength - ptr < 2;
                    if (llength - ptr < 2) {
                        if (rAtEnd) {
                            return msd;
                        }
                        return -1;
                    }
                    if (rAtEnd) {
                        return 1;
                    }
                    rHasNoMoreDigits = isNotDigit(uri1.charAt(ptr + 1));
                    if (isNotDigit(uri0.charAt(ptr + 1))) {
                        if (rHasNoMoreDigits && (msd != 0)) {
                            return msd;
                        }
                        if (!rHasNoMoreDigits) {
                            return -1;
                        }
                    } else {
                        if (rHasNoMoreDigits) {
                            return 1;
                        }
                    }
                }
                ptr++;
            }
            return llength - rlength;
        }

        protected boolean isNotDigit(final char x) {
            return (x > '9') || (x < '0');
        }

    }

    /** The reference comparator to sort index pages. */
    private volatile Comparator<Reference> comparator;

    /** Indicates if the subdirectories are deeply accessible (true by default). */
    private volatile boolean deeplyAccessible;

    /** The index name, without extensions (ex: "index" or "home"). */
    private volatile String indexName;

    /**
     * Indicates if the display of directory listings is allowed when no index
     * file is found.
     */
    private volatile boolean listingAllowed;

    /**
     * Indicates if modifications to local resources are allowed (false by
     * default).
     */
    private volatile boolean modifiable;

    /** Indicates if the best content is automatically negotiated. */
    private volatile boolean negotiateContent;

    /** The absolute root reference (file, clap URI). */
    private volatile Reference rootRef;

    /**
     * Constructor.
     * 
     * @param context
     *            The context.
     * @param rootLocalReference
     *            The root URI.
     */
    public Directory(Context context, Reference rootLocalReference) {
        super(context);

        // First, let's normalize the root reference to prevent any issue with
        // relative paths inside the reference leading to listing issues.
        final String rootIdentifier = rootLocalReference.getTargetRef()
                .getIdentifier();

        if (rootIdentifier.endsWith("/")) {
            this.rootRef = new Reference(rootIdentifier);
        } else {
            // We don't take the risk of exposing directory "file:///C:/AA"
            // if only "file:///C:/A" was intended
            this.rootRef = new Reference(rootIdentifier + "/");
        }

        this.comparator = new AlphaNumericComparator();
        this.deeplyAccessible = true;
        this.indexName = "index";
        this.listingAllowed = false;
        this.modifiable = false;
        this.negotiateContent = true;
    }

    /**
     * Constructor.
     * 
     * @param context
     *            The context.
     * @param rootUri
     *            The absolute root URI. <br>
     * <br>
     *            If you serve files from the file system, use file:// URIs and
     *            make sure that you register a FILE connector with your parent
     *            Component. On Windows, make sure that you add enough slash
     *            characters at the beginning, for example: file:///c:/dir/file<br>
     * <br>
     *            If you serve files from a class loader, use clap:// URIs and
     *            make sure that you register a CLAP connector with your parent
     *            Component.<br>
     * <br>
     */
    public Directory(Context context, String rootUri) {
        this(context, new Reference(rootUri));
    }

    /**
     * Finds the target handler if available.
     * 
     * @param request
     *            The request to filter.
     * @param response
     *            The response to filter.
     * @return The target handler if available or null.
     */
    @Override
    public Handler findTarget(Request request, Response response) {
        try {
            return Engine.getInstance().createDirectoryResource(this, request,
                    response);
        } catch (IOException ioe) {
            getLogger().log(Level.WARNING,
                    "Unable to find the directory's resource", ioe);
            return null;
        }
    }

    /**
     * Returns the reference comparator used to sort index pages. The default
     * implementation used a friendly alphanum sorting.
     * 
     * @return The reference comparator.
     * @see #setAlphaNumComparator()
     */
    public Comparator<Reference> getComparator() {
        return this.comparator;
    }

    /**
     * Returns the index name, without extensions. Returns "index" by default.
     * 
     * @return The index name.
     */
    public String getIndexName() {
        return this.indexName;
    }

    /**
     * Returns an actual index representation for a given variant.
     * 
     * @param variant
     *            The selected variant.
     * @param indexContent
     *            The directory index to represent.
     * @return The actual index representation.
     */
    public Representation getIndexRepresentation(Variant variant,
            ReferenceList indexContent) {
        Representation result = null;
        if (variant.getMediaType().equals(MediaType.TEXT_HTML)) {
            result = indexContent.getWebRepresentation();
        } else if (variant.getMediaType().equals(MediaType.TEXT_URI_LIST)) {
            result = indexContent.getTextRepresentation();
        }
        return result;
    }

    /**
     * Returns the variant representations of a directory index. This method can
     * be subclassed in order to provide alternative representations.
     * 
     * By default it returns a simple HTML document and a textual URI list as
     * variants. Note that a new instance of the list is created for each call.
     * 
     * @param indexContent
     *            The list of references contained in the directory index.
     * @return The variant representations of a directory.
     */
    public List<Variant> getIndexVariants(ReferenceList indexContent) {
        final List<Variant> result = new ArrayList<Variant>();
        result.add(new Variant(MediaType.TEXT_HTML));
        result.add(new Variant(MediaType.TEXT_URI_LIST));
        return result;
    }

    /**
     * Returns the root URI from which the relative resource URIs will be lookep
     * up.
     * 
     * @return The root URI.
     */
    public Reference getRootRef() {
        return this.rootRef;
    }

    /**
     * Indicates if the subdirectories are deeply accessible (true by default).
     * 
     * @return True if the subdirectories are deeply accessible.
     */
    public boolean isDeeplyAccessible() {
        return this.deeplyAccessible;
    }

    /**
     * Indicates if the display of directory listings is allowed when no index
     * file is found.
     * 
     * @return True if the display of directory listings is allowed when no
     *         index file is found.
     */
    public boolean isListingAllowed() {
        return this.listingAllowed;
    }

    /**
     * Indicates if modifications to local resources (most likely files) are
     * allowed. Returns false by default.
     * 
     * @return True if modifications to local resources are allowed.
     */
    public boolean isModifiable() {
        return this.modifiable;
    }

    /**
     * Indicates if the best content is automatically negotiated. Default value
     * is true.
     * 
     * @return True if the best content is automatically negotiated.
     */
    public boolean isNegotiateContent() {
        return this.negotiateContent;
    }

    /**
     * Sets the reference comparator based on classic alphabetical order.
     * 
     * @see #setComparator(Comparator)
     */
    public void setAlphaComparator() {
        setComparator(new AlphabeticalComparator());
    }

    /**
     * Sets the reference comparator based on the more friendly "Alphanum
     * Algorithm" created by David Koelle. The internal implementation used is
     * based on an optimized public domain implementation provided by Rob
     * Heittman from the Solertium Corporation.
     * 
     * @see <a href="http://www.davekoelle.com/alphanum.html">The original
     *      Alphanum Algorithm from David Koelle</a>
     * @see #setComparator(Comparator)
     */
    public void setAlphaNumComparator() {
        setComparator(new AlphabeticalComparator());
    }

    /**
     * Sets the reference comparator used to sort index pages.
     * 
     * @param comparator
     *            The reference comparator.
     */
    public void setComparator(Comparator<Reference> comparator) {
        this.comparator = comparator;
    }

    /**
     * Indicates if the subdirectories are deeply accessible (true by default).
     * 
     * @param deeplyAccessible
     *            True if the subdirectories are deeply accessible.
     */
    public void setDeeplyAccessible(boolean deeplyAccessible) {
        this.deeplyAccessible = deeplyAccessible;
    }

    /**
     * Sets the index name, without extensions.
     * 
     * @param indexName
     *            The index name.
     */
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    /**
     * Indicates if the display of directory listings is allowed when no index
     * file is found.
     * 
     * @param listingAllowed
     *            True if the display of directory listings is allowed when no
     *            index file is found.
     */
    public void setListingAllowed(boolean listingAllowed) {
        this.listingAllowed = listingAllowed;
    }

    /**
     * Indicates if modifications to local resources are allowed.
     * 
     * @param modifiable
     *            True if modifications to local resources are allowed.
     */
    public void setModifiable(boolean modifiable) {
        this.modifiable = modifiable;
    }

    /**
     * Indicates if the best content is automatically negotiated. Default value
     * is true.
     * 
     * @param negotiateContent
     *            True if the best content is automatically negotiated.
     */
    public void setNegotiateContent(boolean negotiateContent) {
        this.negotiateContent = negotiateContent;
    }

    /**
     * Sets the root URI from which the relative resource URIs will be lookep
     * up.
     * 
     * @param rootRef
     *            The root URI.
     */
    public void setRootRef(Reference rootRef) {
        this.rootRef = rootRef;
    }

}
