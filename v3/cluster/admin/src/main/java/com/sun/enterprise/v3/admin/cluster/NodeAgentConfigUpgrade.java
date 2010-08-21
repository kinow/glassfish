/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.admin.cluster;

import java.beans.PropertyVetoException;
import java.util.*;
import java.util.logging.*;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.NodeAgent;
import com.sun.enterprise.config.serverbeans.NodeAgents;
import com.sun.enterprise.config.serverbeans.JmxConnector;
import org.glassfish.api.admin.config.ConfigurationUpgrade;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.*;


/**
 * Change the node-agent element to use V3 mechanism 
 * @author Carla Mott
 */

@Service
@Scoped(PerLookup.class)
public class NodeAgentConfigUpgrade implements ConfigurationUpgrade, PostConstruct {
    @Inject
    Domain domain;

    @Inject
    Servers servers;

    public void postConstruct() {

        final NodeAgents nodeAgents = domain.getNodeAgents();
        if (nodeAgents == null)
            return;

        final List<NodeAgent> agList= nodeAgents.getNodeAgent();
        if (agList.size() == 0)
            return;
        try {
            ConfigSupport.apply(new SingleConfigCode<Domain>() {
                public Object run(Domain d) throws PropertyVetoException, TransactionFailure {

                    Nodes nodes=d.createChild(Nodes.class);
                    Transaction t = Transaction.getTransaction(d);
                    if (t==null)
                        return null;

                    for( NodeAgent na: agList){
                        String host=null;
                        Node node = nodes.createChild(Node.class);

                        node.setName(na.getName());
                        node.setType("CONFIG");
                        JmxConnector jc = na.getJmxConnector();
                        if (jc != null){
                            List<Property> agentProp =jc.getProperty();  //get the properties and see if host name is specified
                            for ( Property p : agentProp)  {
                                String name = p.getName();
                                if (name.equals("client-hostname")) {
                                    node.setNodeHost(p.getValue()); //create the node with a host name
                                    node.setInstallDir("${com.sun.aas.installRoot}");                                    
                                }
                            }
                        }
                        nodes.getNode().add(node);
                    }
                    d.setNodes(nodes);

                    List<Server> serverList=servers.getServer();
                    if (serverList.size() <= 0)
                        return null;

                    for (Server s: serverList){
                        t.enroll(s);
                        s.setNode(s.getNodeAgentRef());
//                        s.setNodeAgentRef(null);
                    }
                    //remove the node-agent element
//                    d.getNodeAgents().getNodeAgent().clear();
                    d.setNodeAgents(null);
                    return null;
                }
        
        }, domain);
        } catch (Exception e) {
            Logger.getAnonymousLogger().log(Level.SEVERE,
                "Failure while upgrading node-agent from V2 to V3", e);
            throw new RuntimeException(e);
        }

    }

}




