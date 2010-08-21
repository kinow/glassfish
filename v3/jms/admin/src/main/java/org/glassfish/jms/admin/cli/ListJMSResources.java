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

package org.glassfish.jms.admin.cli;

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.I18n;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.PerLookup;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.config.serverbeans.ConnectorConnectionPool;
import com.sun.enterprise.config.serverbeans.AdminObjectResource;
import com.sun.enterprise.util.LocalStringManagerImpl;

import java.util.ArrayList;
import org.glassfish.api.admin.Cluster;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.api.admin.RuntimeType;

/**
 * List Connector Resources command
 *
 */
@Service(name="list-jms-resources")
@Scoped(PerLookup.class)
@I18n("list.jms.resources")
@Cluster({RuntimeType.DAS})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.DOMAIN})

public class ListJMSResources implements AdminCommand {

    private static final String QUEUE = "javax.jms.Queue";
    private static final String TOPIC = "javax.jms.Topic";
    private static final String QUEUE_CF = "javax.jms.QueueConnectionFactory";
    private static final String TOPIC_CF = "javax.jms.TopicConnectionFactory";
    private static final String UNIFIED_CF = "javax.jms.ConnectionFactory";
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ListJMSResources.class);

    @Param(name="resType", optional=true)
    String resourceType;

    @Inject
    ConnectorConnectionPool[] connectionpools;

    @Inject
    AdminObjectResource[] resources;


    /**
        * Executes the command with the command parameters passed as Properties
        * where the keys are the paramter names and the values the parameter values
        *
        * @param context information
        */
       public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        ArrayList<String> list = new ArrayList();

        if(resourceType == null){
          try{
            //list all JMS resources
            for (AdminObjectResource r : resources) {
              if(QUEUE.equals(r.getResType()) || TOPIC.equals(r.getResType()))
                list.add(r.getJndiName());
            }

            for (ConnectorConnectionPool cp : connectionpools) {
               if(QUEUE_CF.equals(cp.getConnectionDefinitionName()) || TOPIC_CF.equals(cp.getConnectionDefinitionName())
                       || UNIFIED_CF.equals(cp.getConnectionDefinitionName()))
                    list.add(cp.getName());
            }
            if (list.isEmpty()) {
                final ActionReport.MessagePart part = report.getTopMessagePart().addChild();
                part.setMessage(localStrings.getLocalString("nothingToList",
                    "Nothing to list."));
            } else {
                for (String jndiName : list) {
                    final ActionReport.MessagePart part = report.getTopMessagePart().addChild();
                    part.setMessage(jndiName);
                }
            }
        } catch (Exception e) {
            report.setMessage(localStrings.getLocalString("list.jms.resources.fail",
                    "Unable to list JMS Resources") + " " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
      } else {
          if(resourceType.equals(TOPIC_CF) || resourceType.equals(QUEUE_CF) || resourceType.equals(UNIFIED_CF)){

            for (ConnectorConnectionPool cp : connectionpools) {
               if(resourceType.equals(cp.getConnectionDefinitionName()))
                    list.add(cp.getName());
            }
          }  else if (resourceType.equals(TOPIC) || resourceType.equals(QUEUE))
          {
                for (AdminObjectResource r : resources) {
                    if(resourceType.equals(r.getResType()))
                        list.add(r.getJndiName());
            }

          }

        }

        for (String jndiName : list) {
              final ActionReport.MessagePart part = report.getTopMessagePart().addChild();
              part.setMessage(jndiName);
        }

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);


  }
}



