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

import static org.glassfish.admin.rest.Util.*;
import static org.glassfish.admin.rest.provider.ProviderUtil.*;

/**
 * @author Rajeshwar Patil
 */
@Provider
@Produces(MediaType.TEXT_HTML)
public class TreeNodeHtmlProvider extends BaseProvider<List<TreeNode>> {

    public TreeNodeHtmlProvider() {
        super(List.class, MediaType.TEXT_HTML_TYPE);
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
        String result = getHtmlHeader();
        result = result + "<h1>" + upperCaseFirstLetter((decode(getName(uriInfo.getPath(), '/')))) + "</h1>" + "<hr>";

        //display hint if module monitoring levels are OFF.
        if ((proxy.isEmpty()) && (uriInfo.getPath().equalsIgnoreCase("domain"))) {
            result = result + getHint(uriInfo, MediaType.TEXT_HTML);
        }
        String attributes = getAttributes(proxy);
        result = getHtmlForComponent(attributes, "Attributes", result);

        String childResourceLinks = getResourcesLinks(proxy);
        result = getHtmlForComponent(childResourceLinks, KEY_CHILD_RESOURCES, result);

        result = result + "</html></body>";
        return result;
    }

    private String getAttributes(List<TreeNode> nodeList) {
        String result = "";
        for (TreeNode node : nodeList) {
            //process only the leaf nodes, if any
            if (!node.hasChildNodes()) {
                //getValue() on leaf node will return one of the following -
                //Statistic object, String object or the object for primitive type
                result = result + htmlForNode(node.getName(), node.getValue());
            }
        }

        return result;
    }

    private String getResourcesLinks(List<TreeNode> nodeList) {
        String result = "";
        String elementName;
        for (TreeNode node : nodeList) { //for each element
            //process only the non-leaf nodes, if any
            if (node.hasChildNodes()) {
                try {
                    elementName = node.getName();
                    //replace all \. with . from the node name
                    elementName = elementName.replaceAll("\\\\.", "\\.");
                    result = result + "<a href=\"" + getElementLink(uriInfo, elementName) + "\">";
                    result = result + elementName;
                    result = result + "</a>";
                    result = result + "<br>";
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    private String htmlForNode(String name, Object value) {
        String result = "";
        if (value == null) {
            return result;
        }

        try {
            if (value instanceof Statistic) {
                Statistic statisticObject = (Statistic) value;
                result = result + getStatisticRepresentation(statisticObject);

                if (!result.equals("")) {
                    result = "<h3>" + name + "</h3>"
                            + "<div><dl>" + result + "</dl></div>";
                }

                result = result + "<br class=\"separator\">";
                return result;
            } else if (value instanceof Stats) {
                String statResult;
                boolean firstEntry = true;
                String lineSpacing = "";
                for (Statistic statistic : ((Stats) value).getStatistics()) {
                    statResult = getStatisticRepresentation(statistic);
                    if (!statResult.equals("")) {
                        if (!firstEntry) {
                            lineSpacing = "<br><br>";
                        }
                        statResult = "<dt>" + lineSpacing + "<b>"
                                + statistic.getName() + "</b></dt><br>" + statResult + "<br>";
                        firstEntry = false;
                    }
                    result = result + statResult;
                    statResult = "";
                }

                if (!result.equals("")) {
                    result = "<h3>" + name + "</h3>"
                            + "<div><dl>" + result + "</dl></div>";
                }

                result = result + "<br class=\"separator\">";
                return result;
            }
        } catch (Exception exception) {
            //log exception message as warning
        }

        //for html output, string value of the object should suffice,
        //irrespective of the type of object
        result = result + "<dt><label for=\"" + name + "\">" + name + ":&nbsp;" + "</label></dt>";
        result = result + "<dd>" + value.toString() + "</dd>";
        result = "<div><dl>" + result + "</dl></div>";

        return result;
    }

    private String getStatisticRepresentation(Statistic statistic)
            throws IllegalAccessException, InvocationTargetException {
        String result = "";
        //Swithching to getStatistic(Statistic) method i.e Gettting the attribute
        //map provided by monitoring infrastructure instead of introspecting
        Map<String, Object> map = getStatistic(statistic);
        Set<String> attributes = map.keySet();
        Object attributeValue;
        for (String attributeName : attributes) {
            attributeValue = map.get(attributeName);
            //for html output, string value of the object should suffice,
            //irrespective of the type of object
            result = result + "<dt><label for=\"" + attributeName + "\">"
                    + attributeName + ":&nbsp;" + "</label></dt>";
            result = result + "<dd>" + attributeValue.toString() + "</dd>";
        }
        return result;
    }
}
