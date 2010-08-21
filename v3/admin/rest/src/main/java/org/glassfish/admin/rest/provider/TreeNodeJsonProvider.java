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

package org.glassfish.admin.rest.provider;

import org.glassfish.admin.rest.Constants;
import org.glassfish.external.statistics.Statistic;
import org.glassfish.external.statistics.Stats;
import org.glassfish.flashlight.datatree.TreeNode;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.glassfish.admin.rest.provider.ProviderUtil.*;

// TODO: Convert this to org.codehaus.jettison
/**
 * @author Rajeshwar Patil
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class TreeNodeJsonProvider extends BaseProvider<List<TreeNode>> {

    public TreeNodeJsonProvider() {
        super(List.class, MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType) {
        if ("java.util.List<org.glassfish.flashlight.datatree.TreeNode>".equals(genericType.toString())) {
            return mediaType.isCompatible(supportedMediaType);
        }
        return false;
    }

    @Override
    public String getContent(List<TreeNode> proxy) {
        String result;
        String indent = Constants.INDENT;
        result ="{" ;
        result = result + "\n\n" + indent;

        result = result + quote(KEY_ENTITY) + ":{";
        //display hint if module monitoring levels are OFF.
        if ((proxy.isEmpty()) && (uriInfo.getPath().equalsIgnoreCase("domain"))) {
            result = result + getHint(uriInfo, MediaType.APPLICATION_JSON);
        }
        result = result + getAttributes(proxy, indent + Constants.INDENT);
        result = result + "},";

        result = result + "\n\n" + indent;
        result = result + quote(KEY_CHILD_RESOURCES);
        result = result + ":[";
        result = result + getResourcesLinks(proxy, indent + Constants.INDENT);
        result = result + "\n" + indent + "]";

        result = result + "\n\n" + "}";
        return result;
    }


    private String getAttributes(List<TreeNode> nodeList, String indent) {
        String result ="";
        for (TreeNode node : nodeList) {
            //process only the leaf nodes, if any
            if (!node.hasChildNodes()) {
                //getValue() on leaf node will return one of the following -
                //Statistic object, String object or the object for primitive type
                result = result +
                        jsonForNodeValue(node.getName(), node.getValue(), indent);
            }
        }

        int endIndex = result.length() - 1;
        if (endIndex > 0) result = result.substring(0, endIndex );
        return result;
    }


    private String getResourcesLinks(List<TreeNode> nodeList, String indent) {
        String result = "";
        String elementName;
        for (TreeNode node: nodeList) {
            //process only the non-leaf nodes, if any
            if (node.hasChildNodes()) {
                try {
                    elementName = node.getName();
                    result = result + "\n" + indent;
                    result = result + quote(getElementLink(uriInfo, elementName));
                    result = result + ",";
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        int endIndex = result.length() - 1;
        if (endIndex > 0) result = result.substring(0, endIndex);
        return result;
    }


    private String jsonForNodeValue(String name, Object value, String indent) {
        String result = "";
        if (value == null) return result;

        try {
            if (value instanceof Statistic) {
                Statistic statisticObject = (Statistic)value;
                result = result + getStatisticRepresentation(statisticObject);
                result = "\n" + indent + quote(name) + ":" + "{" + result + "}";
                result = result + ",";
                return result;
            } else if (value instanceof Stats) {
                String statResult;
                for (Statistic statistic: ((Stats)value).getStatistics()) {
                    statResult = getStatisticRepresentation(statistic);
                    if (!statResult.equals("")) {
                        statResult = "\n" + indent + indent +
                                quote(statistic.getName()) + ":" + "{" + statResult + "}";
                        result = result + statResult;
                        result = result + ",";
                        statResult ="";
                    }
                }

                int endIndex = result.length() - 1;
                if (endIndex > 0) result = result.substring(0, endIndex);

                result = "\n" + indent + quote(name) + ":" + "{" + result + "}";
                result = result + ",";
                return result;
            }
        } catch (Exception exception) {
            //log exception message as warning
        }

        result = " " + quote(name) + ":" + jsonValue(value);
        result = result + ",";
        return result;
    }


    private String getStatisticRepresentation(Statistic statistic)
            throws IllegalAccessException, InvocationTargetException {
        String result ="";
        //Swithching to getStatistic(Statistic) method i.e Gettting the attribute
        //map provided by monitoring infrastructure instead of introspecting
        Map map = getStatistic(statistic);
        Set<String> attributes = map.keySet();
        Object attributeValue;
        for (String attributeName: attributes) {
            attributeValue = map.get(attributeName);
            result = result + " " + quote(attributeName) + ":" + jsonValue(attributeValue);
            result = result + ",";
        }

        int endIndex = result.length() - 1;
        if (endIndex > 0) result = result.substring(0, endIndex);

        return result;
    }
}
