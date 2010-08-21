/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Configs;
import java.beans.PropertyVetoException;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.Cluster;
import org.glassfish.api.admin.RuntimeType;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.Transaction;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Adjusts each configuration in the domain to use secure admin.
 *
 * @author Tim Quinn
 */
@Service(name = "enable-secure-admin")
@Scoped(PerLookup.class)
@I18n("enable.secure.admin.command")
@Cluster(RuntimeType.ALL)
public class EnableSecureAdminCommand implements AdminCommand {

    @Param(optional = true, defaultValue="s1as")
    public String adminalias;

    @Param(optional = true, defaultValue="glassfish-instance")
    public String instancealias;

    @Inject
    private Configs configs;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        try {
            ConfigSupport.apply(new SingleConfigCode() {
                @Override
                public Object run(ConfigBeanProxy param) throws PropertyVetoException, TransactionFailure {
                    // get the transaction
                    Transaction t = Transaction.getTransaction(param);
                    if (t!=null) {

                        for (Config c : configs.getConfig()) {
                            report.getTopMessagePart().addChild().setMessage(c.getName());
                        }
                    }
                    return Boolean.TRUE;
                }
            }, configs);
            report.setMessage("Not yet functional");
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (TransactionFailure ex) {
            report.failure(context.getLogger(), Strings.get("enable.secure.admin.errenable"), ex);
        }
    }

}
