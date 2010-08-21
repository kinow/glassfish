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

package com.sun.enterprise.tools.upgrade.common;

import com.sun.enterprise.tools.upgrade.logging.LogService;
import com.sun.enterprise.util.i18n.StringManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Rewrote to not save the password except in the file. When a
 * password is set, it is saved in a temporary file. If a user
 * passes in an existing file location, will use that instead.
 *
 * For 3.1, we will change this to pass the password characters
 * into asadmin without the text file.
 */
public class DomainCredentials implements Credentials {

    private File passwordFile = null;

    private static final Logger logger = LogService.getLogger();

    private static final StringManager stringManager =
        StringManager.getManager(DomainCredentials.class);

    @Override
	public void setMasterPassword(char [] chars){
            try {
                passwordFile = File.createTempFile("ugpw", null);
                FileWriter writer = new FileWriter(passwordFile);
                writer.write("AS_ADMIN_MASTERPASSWORD=");
                writer.write(chars);
                writer.write("\n");
                writer.close();
            } catch (IOException ioe) {
                logger.severe(stringManager.getString(
                    "upgrade.common.general_exception") +
                    " " + ioe.getMessage());
            } finally {
                Arrays.fill(chars, ' ');
            }
	}

    @Override
    public String getPasswordFile() {
        if (passwordFile == null) {
            return null;
        }
        return passwordFile.getAbsolutePath();
    }

    @Override
    public void setPasswordFile(File file) {
        passwordFile = file;
    }
}
