/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.enterprise.v3.server;

import org.glassfish.hk2.classmodel.reflect.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.*;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.container.Sniffer;
import org.glassfish.api.container.CompositeSniffer;
import org.glassfish.internal.deployment.SnifferManager;
import com.sun.enterprise.util.LocalStringManagerImpl;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.util.*;
import java.net.URI;

import com.sun.enterprise.module.impl.ClassLoaderProxy;

/**
 * Provide convenience methods to deal with {@link Sniffer}s in the system.
 *
 * @author Kohsuke Kawaguchi
 */
@Service
public class SnifferManagerImpl implements SnifferManager {
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(SnifferManagerImpl.class);

    @Inject
    protected Habitat habitat;

    /**
     * Returns all the presently registered sniffers
     *
     * @return Collection (possibly empty but never null) of Sniffer
     */
    public Collection<Sniffer> getSniffers() {
        // this is a little bit of a hack, sniffers are now ordered by their names
        // which is useful since connector is before ejb which is before web so if
        // a standalone module happens to implement the three types of components,
        // they will be naturally ordered correctly. We might want to revisit this
        // later and formalize the ordering of sniffers. The hard thing as usual
        // is that sniffers are highly pluggable so you never know which sniffers
        // set you are working with depending on the distribution
        List<Sniffer> sniffers = new ArrayList<Sniffer>();
        sniffers.addAll(habitat.getAllByContract(Sniffer.class));
        Collections.sort(sniffers, new Comparator<Sniffer>() {
            public int compare(Sniffer o1, Sniffer o2) {
                return o1.getModuleType().compareTo(o2.getModuleType());
            }
        });            

        return sniffers;
    }

    /**
     * Returns all the presently registered composite sniffers
     *
     * @return Collection (possibly empty but never null) of Sniffer
     */
    public Collection<CompositeSniffer> getCompositeSniffers() {
        return habitat.getAllByContract(CompositeSniffer.class);
    }

    /**
     * Check if there's any {@link Sniffer} installed at all.
     */
    public final boolean hasNoSniffers() {
        return getSniffers().isEmpty();
    }

    public Sniffer getSniffer(String appType) {
        assert appType!=null;
        for (Sniffer sniffer :  getSniffers()) {
            if (appType.equalsIgnoreCase(sniffer.getModuleType())) {
                return sniffer;
            }
        }
        return null;
    }

    /**
     * Returns a collection of sniffers that recognized some parts of the
     * passed archive as components their container handle.
     *
     * If no sniffer recognize the passed archive, an empty collection is
     * returned.
     *
     * @param context the deployment context
     * @return possibly empty collection of sniffers that handle the passed
     * archive.
     */
    public Collection<Sniffer> getSniffers(DeploymentContext context) {

        // it is important to keep an ordered sequence here to keep sniffers
        // in their natural order.
        List<Sniffer> nonCompositeSniffers = new ArrayList<Sniffer>();
        for (Sniffer sniffer : getSniffers()) {
            if (!(sniffer instanceof CompositeSniffer))
                nonCompositeSniffers.add(sniffer);
        }

        // scan for registered annotations and retrieve applicable sniffers
        List<Sniffer> appSniffers = this.getApplicableSniffers(context, nonCompositeSniffers,  true);
        
        // call handles method of the sniffers
        for (Sniffer sniffer : nonCompositeSniffers) {
            if ( !appSniffers.contains(sniffer) &&
                sniffer.handles(context.getSource(), context.getClassLoader() )) {
                appSniffers.add(sniffer);
            }
        }
        return appSniffers;
    }

    public boolean canBeIsolated(Sniffer sniffer) {
        // quick and dirty to isolate OSGi container, this avoid clashes between
        // java ee and OSGi fighting to deploy applications.
        // we may need a more generic way of doing this, maybe by adding an API to Sniffer
        return sniffer.getModuleType().equalsIgnoreCase("osgi");
    }

    /**
     * Returns a collection of composite sniffers that recognized some parts of
     * the passed archive as components their container handle.
     *
     * If no sniffer recognize the passed archive, an empty collection is
     * returned.
     *
     * @param context deployment context
     * @return possibly empty collection of sniffers that handle the passed
     * archive.
     */
    public Collection<CompositeSniffer> getCompositeSniffers(DeploymentContext context) {
        // it is important to keep an ordered sequence here to keep sniffers
        // in their natural order.

        List<CompositeSniffer> appSniffers =
                getApplicableSniffers(context, getCompositeSniffers(), false);

        // call handles method of the sniffers
        for (CompositeSniffer sniffer : getCompositeSniffers()) {
            if (!appSniffers.contains(sniffer) && 
                sniffer.handles(context)) {
                appSniffers.add(sniffer);
            }
        }
        return appSniffers;
    }

    public <T extends Sniffer> List<T> getApplicableSniffers(DeploymentContext context, Collection<T> sniffers, boolean checkPath) {
        if (sniffers==null || sniffers.isEmpty()) {
            return Collections.emptyList();
        }
        Types types = context.getModuleMetaData(Types.class);
        List<URI> uris = new ArrayList<URI>();
        uris.add(context.getSource().getURI());
        try {
            File f = new File(context.getSource().getURI());
            if (f.exists() && f.isDirectory()) {
                // we automatically add web-inf/classes to acceptable loading uri to handle
                // specificities of war files. Potentially we could check that f ends with "WAR" 
                uris.add(new URI(context.getSource().getURI().toString()+"WEB-INF/classes/"));
            }
        } catch (URISyntaxException e) {
            // ignore.
        }

        List<T> result = new ArrayList<T>();
        for (T sniffer : sniffers) {
            Class<? extends Annotation>[] annotations = sniffer.getAnnotationTypes();
            if (annotations==null) continue;
            for (Class<? extends Annotation> annotationType : annotations)  {
                Type type = types.getBy(annotationType.getName());
                if (type instanceof AnnotationType) {
                    Collection<AnnotatedElement> elements = ((AnnotationType) type).allAnnotatedTypes();
                    for (AnnotatedElement element : elements) {
                        if (checkPath) {
                            Type t = (element instanceof Member?((Member) element).getDeclaringType():(Type) element);
                            if (t.wasDefinedIn(uris)) {
                                result.add(sniffer);
                                break;
                            }
                        } else {
                            result.add(sniffer);
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    public void validateSniffers(Collection<? extends Sniffer> snifferCol, DeploymentContext context) {
        for (Sniffer sniffer : snifferCol) {
            String[] incompatTypes = sniffer.getIncompatibleSnifferTypes();
            if (incompatTypes==null)
                return;
            for (String type : incompatTypes) { 
                for (Sniffer sniffer2 : snifferCol) {
                    if (sniffer2.getModuleType().equals(type)) {
                        throw new IllegalArgumentException(
                            localStrings.getLocalString(
                            "invalidarchivepackaging",
                            "Invalid archive packaging {2}",
                            sniffer.getModuleType(), type,
                            context.getSourceDir().getPath()));
                    }
                }
            }
        }
    }
}
