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

package com.sun.enterprise.config.serverbeans.customvalidators;

import com.sun.enterprise.config.serverbeans.ConnectorConnectionPool;
import com.sun.enterprise.config.serverbeans.JdbcConnectionPool;
import com.sun.enterprise.config.serverbeans.ResourcePool;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Implementation for Connection Pool validation.
 * Following validations are done :
 * - Validation of datasource/driver classnames when resource type is not null 
 * - Max pool size to be always higher than steady pool size
 * - Check if statement wrapping is on when certain features are enabled.
 * 
 * @author Shalini M
 */
public class ConnectionPoolValidator
    implements ConstraintValidator<ConnectionPoolConstraint, ResourcePool> {
    
    protected ConnectionPoolErrorMessages poolFaults;
    
    public void initialize(final ConnectionPoolConstraint constraint) {
        this.poolFaults = constraint.value();
    }

    @Override
    public boolean isValid(final ResourcePool pool,
        final ConstraintValidatorContext constraintValidatorContext) {

        if(poolFaults == ConnectionPoolErrorMessages.MAX_STEADY_INVALID) {
            if(pool instanceof JdbcConnectionPool) {
                JdbcConnectionPool jdbcPool = (JdbcConnectionPool) pool;
                if (Integer.parseInt(jdbcPool.getMaxPoolSize()) <
                        (Integer.parseInt(jdbcPool.getSteadyPoolSize()))) {
                    //max pool size fault
                    return false;
                }
            } else if(pool instanceof ConnectorConnectionPool) {
                ConnectorConnectionPool connPool = (ConnectorConnectionPool) pool;
                if (Integer.parseInt(connPool.getMaxPoolSize()) <
                        (Integer.parseInt(connPool.getSteadyPoolSize()))) {
                    //max pool size fault
                    return false;
                }                
            }
        }
        
        if(poolFaults == ConnectionPoolErrorMessages.STMT_WRAPPING_DISABLED) {
            if(pool instanceof JdbcConnectionPool) {
                JdbcConnectionPool jdbcPool = (JdbcConnectionPool) pool;
                if (jdbcPool.getSqlTraceListeners() != null) {
                    if (!Boolean.valueOf(jdbcPool.getWrapJdbcObjects())) {
                        return false;
                    }
                }
                if (Integer.valueOf(jdbcPool.getStatementCacheSize()) != 0) {
                    if (!Boolean.valueOf(jdbcPool.getWrapJdbcObjects())) {
                        return false;
                    }
                }
                if (Integer.valueOf(jdbcPool.getStatementLeakTimeoutInSeconds()) != 0) {
                    if (!Boolean.valueOf(jdbcPool.getWrapJdbcObjects())) {
                        return false;
                    }
                }
                if (Boolean.valueOf(jdbcPool.getStatementLeakReclaim())) {
                    if (!Boolean.valueOf(jdbcPool.getWrapJdbcObjects())) {
                        return false;
                    }
                }
            }
        }
        
        if (poolFaults == ConnectionPoolErrorMessages.RES_TYPE_MANDATORY) {
            if (pool instanceof JdbcConnectionPool) {
                JdbcConnectionPool jdbcPool = (JdbcConnectionPool) pool;
                String resType = jdbcPool.getResType();
                String dsClassName = jdbcPool.getDatasourceClassname();
                String driverClassName = jdbcPool.getDriverClassname();
                if (resType == null) {
                    //One of datasource/driver classnames must be provided.
                    if ((dsClassName == null || dsClassName.equals("")) &&
                            (driverClassName == null || driverClassName.equals(""))) {
                        return false;
                    } else {
                        //Check if both are provided and if so, return false
                        if (dsClassName != null && driverClassName != null) {
                            return false;
                        }
                    }
                } else if (resType.equals("javax.sql.DataSource") ||
                        resType.equals("javax.sql.ConnectionPoolDataSource") ||
                        resType.equals("javax.sql.XADataSource")) {
                    //Then datasourceclassname cannot be empty
                    if (dsClassName == null || dsClassName.equals("")) {
                        return false;
                    }
                } else if (resType.equals("java.sql.Driver")) {
                    //Then driver classname cannot be empty
                    if (driverClassName == null || driverClassName.equals("")) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}





