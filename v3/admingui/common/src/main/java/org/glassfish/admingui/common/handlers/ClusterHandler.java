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

/*
 * ClusterHandler.java
 *
 * Created on July 1,2010  9:32 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
/**
 *
 * @author anilam
 */
package org.glassfish.admingui.common.handlers;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;

import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import java.util.Map;
import java.util.List;
import org.glassfish.admingui.common.util.GuiUtil;

public class ClusterHandler {

    /** Creates a new instance of InstanceHandler */
    public ClusterHandler() {
    }
    
    /**
     * This method takes in a list of instances with status, which is the output of list-instances
     * and count the # of instance that is running and non running.
     * @param handlerCtx
     */
    @Handler(id = "gf.getClusterStatusSummary",
        input = {
            @HandlerInput(name = "listInstancePropsMap", type = Map.class, required = true)
        },
        output = {
            @HandlerOutput(name = "numRunning", type = String.class),
            @HandlerOutput(name = "numNotRunning", type = String.class),
            @HandlerOutput(name = "status", type = String.class)
        })
    public static void getClusterStatusSummary(HandlerContext handlerCtx) {
        Map propsMap = (Map) handlerCtx.getInputValue("listInstancePropsMap");
        int running=0;
        int notRunning=0;
        try{

            for (Iterator it=propsMap.values().iterator(); it.hasNext(); ) {
                Object value = it.next();
                if (value.toString().equals(RUNNING)){
                    running++;
                }else{
                    notRunning++;
                }
            }

            handlerCtx.setOutputValue( "numRunning" , GuiUtil.getMessage(CLUSTER_RESOURCE_NAME, "cluster.number.instance.running", new String[]{""+running}));
            handlerCtx.setOutputValue( "numNotRunning" , GuiUtil.getMessage(CLUSTER_RESOURCE_NAME, "cluster.number.instance.notRunning", new String[]{""+notRunning}));
        }catch(Exception ex){
            //Log exception ?
             handlerCtx.setOutputValue("status", GuiUtil.getMessage(CLUSTER_RESOURCE_NAME, "cluster.status.unknown"));
         }
     }


    @Handler(id = "gf.saveInstanceWeight",
        input = {
            @HandlerInput(name = "rows", type = List.class, required = true)})
    public static void saveInstanceWeight(HandlerContext handlerCtx) {
        List<Map> rows =  (List<Map>) handlerCtx.getInputValue("rows");
        List errorInstances = new ArrayList();
        Map response = null;

        String prefix = GuiUtil.getSessionValue("REST_URL") + "/servers/server/";
        for (Map oneRow : rows) {
            String instanceName = (String) oneRow.get("Name");
            String endpoint = GuiUtil.getSessionValue("REST_URL") + "/servers/server/instanceName" ;
            Map attrsMap = new HashMap();
            attrsMap.put("lbWeight", oneRow.get("LbWeight"));
            try{
                response = RestApiHandlers.restRequest( prefix+instanceName , attrsMap, "post" , null);
            }catch (Exception ex){
                GuiUtil.getLogger().severe("Error in saveInstanceWeight ; \nendpoint = " + prefix + instanceName + "attrsMap=" + attrsMap);
                response = null;
            }
            if (response ==null){
                errorInstances.add(instanceName);
            }
        }
        if (errorInstances.size() > 0){
            String details = GuiUtil.getMessage(CLUSTER_RESOURCE_NAME, "instance.error.updateWeight" , new String[]{""+errorInstances});
            GuiUtil.handleError(handlerCtx, details);
        }
     }


    @Handler(id = "gf.clusterAction",
        input = {
            @HandlerInput(name = "rows", type = List.class, required = true),
            @HandlerInput(name = "action", type = String.class, required = true),
            @HandlerInput(name = "extraInfo", type = Object.class) })
    public static void clusterAction(HandlerContext handlerCtx) {
        String action = (String) handlerCtx.getInputValue("action");
        List<Map> rows =  (List<Map>) handlerCtx.getInputValue("rows");
        List errorClusters = new ArrayList();
        Map response = null;
        String prefix = GuiUtil.getSessionValue("REST_URL") + "/clusters/cluster/";

        for (Map oneRow : rows) {
            String clusterName = (String) oneRow.get("name");

            boolean error = false;
            if (action.equals("delete-cluster")){
                //need to delete the clustered instance first
                Map clusterInstanceMap = (Map)handlerCtx.getInputValue("extraInfo");
                List<String> instanceNameList = (List) clusterInstanceMap.get(clusterName);
                for(String instanceName : instanceNameList){
                    response = deleteInstance(instanceName, null);
                    if (response == null){
                        errorClusters.add(clusterName);
                        error = true;
                        break;
                    }
                }
                if (error){
                    continue;
                }
            }
            try{
                response = RestApiHandlers.restRequest( prefix + clusterName + "/" + action, null, "post" ,null);
            }catch (Exception ex){
                GuiUtil.getLogger().severe("Error in clusterAction ; \nendpoint = " + prefix + clusterName + "/" + action + ", attrMap = null" );
                response = null;
            }
            if (response == null) {
                errorClusters.add(clusterName);
            }
        }
        if (errorClusters.size() > 0){
            String details = GuiUtil.getMessage(CLUSTER_RESOURCE_NAME, "cluster.error."+"action" , new String[]{""+errorClusters});
            GuiUtil.handleError(handlerCtx, details);
        }

     }


    @Handler(id = "gf.instanceAction",
        input = {
            @HandlerInput(name = "rows", type = List.class, required = true),
            @HandlerInput(name = "action", type = String.class, required = true)})
    public static void instanceAction(HandlerContext handlerCtx) {
        String action = (String) handlerCtx.getInputValue("action");
        List<Map> rows =  (List<Map>) handlerCtx.getInputValue("rows");
        List errorInstances = new ArrayList();
        Map response = null;
        String prefix = GuiUtil.getSessionValue("REST_URL") + "/servers/server/";

        for (Map oneRow : rows) {
            String instanceName = (String) oneRow.get("name");
            if(action.equals("delete-instance")){
                response = deleteInstance(instanceName, (String) oneRow.get("node"));
            }else{
                try{
                       response = RestApiHandlers.restRequest(prefix + instanceName + "/" + action , null, "post" ,null);
                }catch (Exception ex){
                    GuiUtil.getLogger().severe("Error in instanceAction ; \nendpoint = " + prefix + instanceName + "/" + action  + "attrsMap=" + null);
                    response = null;
                }
            }
            if (response ==null){
                errorInstances.add(instanceName);
            }
        }
        if (errorInstances.size() > 0){
            String details = GuiUtil.getMessage(CLUSTER_RESOURCE_NAME, "instance.error."+"action" , new String[]{""+errorInstances});
            GuiUtil.handleError(handlerCtx, details);
        }else{
            if(action.equals("stop-instance")){
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
        }
     }


    @Handler(id = "gf.nodeAction",
        input = {
            @HandlerInput(name = "rows", type = List.class, required = true),
            @HandlerInput(name = "action", type = String.class, required = true),
            @HandlerInput(name = "nodeInstanceMap", type = Map.class)})
    public static void nodeAction(HandlerContext handlerCtx) {
        String action = (String) handlerCtx.getInputValue("action");
        Map nodeInstanceMap = (Map) handlerCtx.getInputValue("nodeInstanceMap");
        if (nodeInstanceMap == null){
            nodeInstanceMap=new HashMap();
        }
        List<Map> rows =  (List<Map>) handlerCtx.getInputValue("rows");
        List errorInstances = new ArrayList();
        Map response = null;
        String prefix = GuiUtil.getSessionValue("REST_URL") + "/nodes/node/";

        for (Map oneRow : rows) {
            int code = 500;
            String nodeName = (String) oneRow.get("name");
            List instancesList = (List)nodeInstanceMap.get(nodeName);
            if ( instancesList!= null && (instancesList.size()) != 0){
                GuiUtil.prepareAlert(handlerCtx, "error",  GuiUtil.getMessage("msg.Error"),
                        GuiUtil.getMessage(CLUSTER_RESOURCE_NAME, "nodes.instanceExistError", new String[]{ nodeInstanceMap.get(nodeName).toString(), nodeName}));
                return;
            }
            if(action.equals("delete-node")){
                try{
                       response = RestApiHandlers.restRequest(prefix + nodeName + "/" + action + ".json" , null, "post" ,null);
                }catch (Exception ex){
                    GuiUtil.getLogger().severe("Error in nodeAction ; \nendpoint = " + prefix + nodeName + "/" + action  + "attrsMap=" + null);
                    response = null;
                }
            }
            //TODO:  we may want to extract the exact error when issue# 12861 is fixed.
            if (response != null){
                code = (Integer) response.get("responseCode");
                if (code != 200 && code != 201){
                    Object body = response.get("responseBody");
                    errorInstances.add(body.toString());
                    break;
                }
            }else{
                errorInstances.add(nodeName);
                break;
            }
        }
        if (errorInstances.size() > 0){
            String details = GuiUtil.getMessage(CLUSTER_RESOURCE_NAME, "node.error.delete" , new String[]{""+errorInstances});
            GuiUtil.prepareAlert(handlerCtx, "error",  GuiUtil.getMessage("msg.Error"), details);
        }
     }



    @Handler(id = "gf.createClusterInstances",
        input = {
            @HandlerInput(name = "clusterName", type = String.class, required = true),
            @HandlerInput(name = "instanceRow", type = List.class, required = true)})
    public static void createClusterInstances(HandlerContext handlerCtx) {
        String clusterName = (String) handlerCtx.getInputValue("clusterName");
        List<Map> instanceRow =  (List<Map>) handlerCtx.getInputValue("instanceRow");
        Map attrsMap = new HashMap();
        Map response = null;
        String endpoint = GuiUtil.getSessionValue("REST_URL") + "/create-instance";
        for (Map oneInstance : instanceRow) {
            attrsMap.put("name", oneInstance.get("name"));
            attrsMap.put("cluster", clusterName);
            attrsMap.put("node", oneInstance.get("node"));
            //ignore for now till issue# 12646 is fixed
            //attrsMap.put("weight", oneInstance.get("weight"));
            try{
                response = RestApiHandlers.restRequest( endpoint , attrsMap, "post" ,null);
            }catch (Exception ex){
                GuiUtil.getLogger().severe("Error in createCluster ; \nendpoint = " + endpoint + "attrsMap=" + attrsMap);
            }
        }

    }


    /*
     * getDeploymentTargets takes in a list of cluster names, and an list of Properties that is returned from the
     * list-instances --standaloneonly=true.  Extract the instance name from this properties list.
     * The result list will include "server",  clusters and standalone instances,  suitable for deployment or create resources.
     *
     */
    @Handler(id = "gf.getDeploymentTargets",
        input = {
            @HandlerInput(name = "clusterList", type = List.class), // TODO: Should this be a map too?
            @HandlerInput(name = "listInstanceProps", type = Map.class)
        },
        output = {
            @HandlerOutput(name = "result", type = List.class)
        })
    public static void getDeploymentTargets(HandlerContext handlerCtx) {
        List<String> result = new ArrayList();
        result.add("server");
        try{
            List<String> clusterList = (List) handlerCtx.getInputValue("clusterList");
            if (clusterList != null){
                for(String oneCluster : clusterList){
                    result.add(oneCluster);
                }
            }

            Map props = (Map) handlerCtx.getInputValue("listInstanceProps");
            if (props != null) {
                result.addAll(props.keySet());
            }
         }catch(Exception ex){
             GuiUtil.getLogger().severe(ex.getLocalizedMessage());//"getDeploymentTargets failed.");
             //print stacktrace ??
         }
        handlerCtx.setOutputValue("result", result);
     }

    private static Map deleteInstance(String instanceName, String nodeName){
       /* if (GuiUtil.isEmpty(nodeName)){
            Map iMap = RestApiHandlers.getAttributesMap(GuiUtil.getSessionValue("REST_URL")+"/servers/server/" + instanceName);
            nodeName = (String) iMap.get("Node");
        }
        Map attrsMap = new HashMap();
        attrsMap.put("nodeagent", nodeName);
        */
        try{
            return  RestApiHandlers.restRequest( GuiUtil.getSessionValue("REST_URL") + "/servers/server/" + instanceName + "/delete-instance", null, "post" ,null);
        }catch(Exception ex){
            GuiUtil.getLogger().severe("Error in deleteInstance ; \nendpoint = " +
                            GuiUtil.getSessionValue("REST_URL") + "/servers/server/" + instanceName + "/delete-instance\n" +
                            "attrsMap=" + null);
            return null;
        }
    }


    public static String CLUSTER_RESOURCE_NAME = "org.glassfish.cluster.admingui.Strings";

    //The following is defined in v3/cluster/admin/src/main/java/..../cluster/Constants.java
    public static String RUNNING = "RUNNING";
    public static String NOT_RUNNING = "NOT_RUNNING";
    public static String PARTIALLY_RUNNING = "PARTIALLY_RUNNING";
}
