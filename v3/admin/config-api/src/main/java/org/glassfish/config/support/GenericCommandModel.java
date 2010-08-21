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

package org.glassfish.config.support;

import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.Cluster;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.common.util.admin.CommandModelImpl;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.DomDocument;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;

public class GenericCommandModel extends CommandModel {

    final HashMap<String, ParamModel> params = new HashMap<String, ParamModel>();
    final String commandName;
    final Cluster cluster;

    public GenericCommandModel(Class<?> targetType, Cluster cluster, DomDocument document, String commandName, Class<?>... extraTypes) {
        this.commandName = commandName;
        this.cluster = cluster;
        if (targetType!=null && ConfigBeanProxy.class.isAssignableFrom(targetType)) {
            ConfigModel cm = document.buildModel(targetType);
            for (Method m : targetType.getMethods()) {
                ConfigModel.Property prop = cm.toProperty(m);
                if (prop == null) continue;
                String attributeName = prop.xmlName;
                if (m.isAnnotationPresent(Param.class)) {
                    Param p = m.getAnnotation(Param.class);
                    if (p.name() != null && !p.name().isEmpty()) {
                        params.put(p.name(), new ParamBasedModel(p.name(), p));
                    } else {
                        if (m.isAnnotationPresent(Attribute.class)) {
                            Attribute attr = m.getAnnotation(Attribute.class);
                            if (attr.value() != null && !attr.value().isEmpty()) {
                                params.put(attr.value(), new AttributeBasedModel(attr.value(), attr));
                            } else {
                                params.put(attributeName, new AttributeBasedModel(attributeName, attr));
                            }
                        }
                    }
                }
            }
        }

        if (extraTypes!=null) {
            for (Class extraType : extraTypes) {
                CommandModelImpl cm = new CommandModelImpl();
                cm.init(extraType);
                
                for (String paramName : cm.getParametersNames()) {
                    params.put(paramName, cm.getModelFor(paramName));
                }

            }
        }
    }

    public I18n getI18n() {
        return null;
    }

    public String getCommandName() {
        return commandName;
    }

    public ParamModel getModelFor(String paramName) {
        return params.get(paramName);
    }

    public Collection<String> getParametersNames() {
        return params.keySet();
    }

    @Override
    public Cluster getClusteringAttributes() {
        return cluster;
    }

    private final class ParamBasedModel extends ParamModel {
        final String name;
        final Param param;

        private ParamBasedModel(String name, Param param) {
            this.name = name;
            this.param = param;
        }

        public String getName() {
            return name;
        }

        public Param getParam() {
            return param;
        }

        public I18n getI18n() {
            return null;
        }

        public Class getType() {
            return String.class;
        }
    }

    private final class AttributeBasedModel extends ParamModel {
        final String name;
        final Attribute attr;

        private AttributeBasedModel(String name, Attribute attr) {
            this.name = name;
            this.attr = attr;
        }

        public String getName() {
            return name;
        }

        public I18n getI18n() {
            return null;
        }

        public Class getType() {
            return String.class;
        }

        public Param getParam() {
            return new Param() {

                public Class<? extends Annotation> annotationType() {
                    return Param.class;
                }

                public String name() {
                    return name;
                }

                public String acceptableValues() {
                    return null;
                }

                public boolean optional() {
                    return !attr.key();

                }

                public String shortName() {
                    return null;
                }

                public boolean primary() {
                    return attr.key();
                }

                public String defaultValue() {
                    return attr.defaultValue();
                }

                public boolean password() {
                    return false;
                }

                public char separator() {
                    return ',';
                }

                public boolean multiple() {
                    return false;
                }

                public boolean obsolete() {
                    return false;
                }

                public String alias() {
                    return "";
                }
            };
        }
    }
}
