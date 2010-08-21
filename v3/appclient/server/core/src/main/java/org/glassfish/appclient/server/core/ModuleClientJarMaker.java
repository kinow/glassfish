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

package org.glassfish.appclient.server.core;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.enterprise.deploy.shared.ModuleType;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.archivist.ApplicationArchivist;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.WritableArchive;
import com.sun.enterprise.deployment.RootDeploymentDescriptor;
import com.sun.enterprise.deployment.util.ModuleDescriptor;
import com.sun.enterprise.deployment.util.XModuleType;
import com.sun.enterprise.util.zip.ZipItem;

/**
 * This class is responsible for creating an appclient jar file that
 * will be used by the appclient container to run the appclients for
 * the deployed application.
 *
 * @author deployment dev team
 */
class ModuleClientJarMaker implements ClientJarMaker {

    protected Properties props;

    /**
     * Default constructor for this stateless object
     * @param props are the implementation properties (if any)
     */
    public ModuleClientJarMaker(Properties props) {
        this.props = props;
    }
    
    /**
     * creates the appclient container jar file
     * @param descriptor is the loaded module's deployment descriptor
     * @param source is the abstract archive for the source module deployed
     * @param target is the abstract archive for the desired appclient container jar file
     * @param stubs are the stubs generated by the deployment codegen
     * @param props is a properties collection to pass implementation parameters
     *
     * @throws IOException when the jar file creation fail
     */
    public void create(RootDeploymentDescriptor descriptor, ReadableArchive source,
        WritableArchive target,ZipItem[] stubs, Properties props)
        throws IOException {
        create(descriptor, source, null, target, stubs, props);
    }

    /**
     * creates the appclient container jar file
     * @param descriptor is the loaded module's deployment descriptor
     * @param source is the abstract archive for the source module deployed
     * @param source2 is the abstract archive for the generated xml directory
     * @param target is the abstract archive for the desired appclient container jar file
     * @param stubs are the stubs generated by the deployment codegen
     * @param props is a properties collection to pass implementation parameters
     *
     * @throws IOException when the jar file creation fail
     */
    public void create(RootDeploymentDescriptor descriptor, ReadableArchive source,
        ReadableArchive source2, WritableArchive target,ZipItem[] stubs, 
        Properties props) throws IOException {
        
        // in all cases we copy the stubs file in the target archive
        ClientJarMakerUtils.populateStubs(target, stubs);

        ReadableArchive appclientSource = null;
        ReadableArchive appclientSource2 = null;

        // abstract out the one and only appclient content from ear file
        if (descriptor.isApplication()) {

            // copy and expand library files here
            // @@@ need to clean up to handle manifest override problem
            // @@@ for multiple library jar files
            List<String> libraries = ClientJarMakerUtils.getLibraryEntries(
                    Application.class.cast(descriptor), source);

            for (String entryName : libraries) {
                ReadableArchive subSource = null;
                try {
                    subSource = source.getSubArchive(entryName);
                    for (Enumeration subEntries = subSource.entries();
                                subEntries.hasMoreElements();) {
                        String subEntryName = 
                                String.class.cast(subEntries.nextElement());
                        ClientJarMakerUtils.copy(
                                subSource, target, subEntryName);
                    }
                } finally {
                    if (subSource != null) {
                        subSource.close();
                    }
                }
            }

            //there should only be one appclient in this ear file
            for (ModuleDescriptor md : 
                Application.class.cast(descriptor).getModules()) {
                if (md.getModuleType().equals(XModuleType.CAR)) {
                    appclientSource = source.getSubArchive(md.getArchiveUri());
                    if (source2 != null) {
                        appclientSource2 = 
                            source2.getSubArchive(md.getArchiveUri());
                    }
                    break;
                }
            }
        } else {
            appclientSource = source;
            appclientSource2 = source2;
        }

        //copy over all content of the appclient
        if (appclientSource != null) {
            ClientJarMakerUtils.populateModuleJar(
                appclientSource, appclientSource2, target);
        } else {
            //this is a workaround because otherwise the appclient jar could
            //be empty which causes problems when closing the archive (there is
            //a requirement on having at least one entry in the archive before
            //closing)
            ClientJarMakerUtils.copyDeploymentDescriptors(
                new ApplicationArchivist(), source, source2, target);
        }

        // for backward compatibility, we need to include the content
        // of the ejb module as well, since many clients currently are
        // packaged without their ejb dependencies.  We will copy the
        // .class entries only.  We copy them here _after_ the appclient
        // classes have been copied so that if any duplicates exist, 
        // the class in the original appclient jar prevails.
        if (descriptor.isApplication()) {
            for (ModuleDescriptor md : 
                Application.class.cast(descriptor).getModules()) {
                if (md.getModuleType().equals(XModuleType.EJB)) {
                    ReadableArchive subSource = 
                        source.getSubArchive(md.getArchiveUri());
                    for (Enumeration e = subSource.entries();e.hasMoreElements();) {
                        String entryName = String.class.cast(e.nextElement());
                        if (!entryName.endsWith(".class")) {
                            continue;
                        }
                        try {
                            ClientJarMakerUtils.copy(subSource, target, entryName);
                        } catch(IOException ioe) {
                            // duplicate, we ignore
                        } finally {
                            if (subSource != null) {
                                subSource.close();
                            }
                        }
                    }
                }
            }
        } 
    }
}
