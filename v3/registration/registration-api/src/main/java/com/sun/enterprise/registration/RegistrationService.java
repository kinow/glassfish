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

package com.sun.enterprise.registration;

import java.util.List;
import java.util.Locale;
import java.net.ConnectException;
import java.net.UnknownHostException;
//import java.util.Map;

public interface RegistrationService {
    
    public boolean isRegistrationEnabled();

    /**
     * registers using the specified registration account
     * 
     * @param account the RegistrationAccount
     * @throws RegistrationException for errors in registering
     * 
     */    
    public void register(RegistrationAccount account) 
        throws RegistrationException, ConnectException, UnknownHostException;
    
    /**
     * creates a new registration account
     * 
     * @param account the RegistrationAccount
     * @throws RegistrationException for errors in creating the account
     * 
     */    
    public void createRegistrationAccount(RegistrationAccount account) 
            throws RegistrationException;
    public List<String> getAvailableCountries();
    public List getAvailableCountries(Locale locale);
    public RegistrationStatus getRegistrationStatus() throws RegistrationException;
    public RegistrationReminder getRegistrationReminder() throws RegistrationException;
    public void setRegistrationReminder(RegistrationReminder reminder) throws RegistrationException;
    public String getPasswordHelpURL();
    public boolean isRegistrationAccountValid(RegistrationAccount account)
            throws RegistrationException, UnknownHostException, ConnectException;

    public List<RegistrationDescriptor> getRegistrationDescriptors() throws RegistrationException;
    public List<RegistrationDescriptor> getRegistrationDescriptors(String productURN) throws RegistrationException;
    public List<RegistrationDescriptor> getRegistrationDescriptors(RegistrationDescriptor.RegistrationStatus status) throws RegistrationException;
    
    enum RegistrationStatus {
        REGISTERED, 
        NOT_REGISTERED,
    }
    
    enum RegistrationReminder {
        ASK_FOR_REGISTRATION,
        DONT_ASK_FOR_REGISTRATION,
        REMIND_LATER                
    }
}
