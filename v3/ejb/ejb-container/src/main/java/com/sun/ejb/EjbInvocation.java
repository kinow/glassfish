
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
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
package com.sun.ejb;

//XXX: import javax.xml.rpc.handler.MessageContext;
/* HARRY : JACC Changes */

import com.sun.ejb.containers.BaseContainer;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.ejb.containers.EJBLocalRemoteObject;
import com.sun.ejb.containers.EjbFutureTask;
import com.sun.ejb.containers.EJBContextImpl;
import org.glassfish.api.invocation.ComponentInvocation;
import com.sun.enterprise.transaction.spi.TransactionOperationsManager;

import javax.ejb.EJBContext;
import javax.ejb.Timer;
import javax.interceptor.InvocationContext;
import com.sun.ejb.containers.interceptors.InterceptorUtil;
import javax.transaction.Transaction;
import javax.xml.rpc.handler.MessageContext;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.WebServiceContext;
import java.lang.reflect.Method;
import java.rmi.UnmarshalException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.sun.ejb.containers.interceptors.InterceptorManager;

/**
 * The EjbInvocation object contains state associated with an invocation
 * on an EJB or EJBHome (local/remote). It is usually created by generated code
 * in *ObjectImpl and *HomeImpl classes. It is passed as a parameter to
 * Container.preInvoke() * and postInvoke(), which are called by the
 * EJB(Local)Object/EJB(Local)Home before and after an invocation.
 */

public class EjbInvocation
    extends ComponentInvocation
    implements InvocationContext, TransactionOperationsManager, Cloneable,
         org.glassfish.ejb.api.EJBInvocation, InterceptorManager.AroundInvokeContext
{
  

    public ComponentContext context;
    
    EjbInvocation(String compEnvId, Container container) {
        super.componentId = compEnvId;
        super.container = container;
        super.setComponentInvocationType(ComponentInvocation.ComponentInvocationType.EJB_INVOCATION);
    }

    /**
     * The EJBObject/EJBLocalObject which created this EjbInvocation object.
     * This identifies the target bean.
     */
    public EJBLocalRemoteObject ejbObject;
    
    /**
     * Local flag: true if this invocation was through the 2.x (or earlier)
     * Local client view, the 3.x local client view or a no-interface client view.
     */
    public boolean isLocal=false;

    /**
     * True if this invocation was made through the 2.x (or earlier) Remote
     * client view or the 3.x remote client view. 
     */
    public boolean isRemote=false;
    
    /**
     * InvocationInfo object caches information about the current method
     */
    public InvocationInfo invocationInfo;
    
    /**
     * True if this invocation was made through a local business interface or
     * bean local view or a remote business interface.
     */
    public boolean isBusinessInterface;

    /**
     * true if this is a web service invocation
     */
    public boolean isWebService=false;

    /**
     * true if this is an ejb timeout method invocation
     */
    public boolean isTimerCallback=false;
    
    /**
     * true if this is a message-driven bean invocation
     */
    public boolean isMessageDriven=false;
    
    /**
     * true if this is an invocation on the home object
     * this is required for jacc.
     */
    public boolean isHome=false;

    /** 
     * Home, Remote, LocalHome, Local, WebService, or business interface
     * through which a synchronous ejb invocation was made.
     */
    public Class clientInterface;
    
    /**
     * Method to be invoked. This is a method of the EJB's local/remote
     * component interface for invocations on EJB(Local)Objects,
     * or of the local/remote Home interface
     * for invocations on the EJBHome.
     * Set by the EJB(Local)Object/EJB(Local)Home before calling
     * Container.preInvoke().
     */
    public java.lang.reflect.Method method;
    
    /**
     * The EJB instance to be invoked.
     * Set by Container and used by EJBObject/EJBHome.
     */
    public Object ejb;

    /**
     * This reflects any exception that has occurred during this invocation,
     * including preInvoke, bean method execution, and postInvoke.
     */
    public Throwable exception;

    /**
     * Set to any exception directly thrown from bean method invocation,
     * which could be either an application exception or a runtime exception.
     * This is set *in addition to* the this.exception field.  Some container
     * processing logic, e.g. @Remove, depends specifically on whether a
     * bean method threw an exception.  
     */
    public Throwable exceptionFromBeanMethod;
    
    
    /**
     * The client's transaction if any.
     * Set by the Container during preInvoke() and used by the Container
     * during postInvoke().
     */
    public Transaction clientTx;
    
    /**
     * The EJBContext object of the bean instance being invoked.
     * Set by the Container during preInvoke() and used by the Container
     * during postInvoke().
     */
    // Moved to com/sun/enterprise/ComponentInvocation
    // public ComponentContext context;
    
    /**
     * The transaction attribute of the bean method. Set in generated
     * EJBObject/Home/LocalObject/LocalHome class.
     */
    public int transactionAttribute;
    
    /**
     * The security attribute of the bean method. Set in generated
     * EJBObject/Home/LocalObject/LocalHome class.
     */
    public int securityPermissions;
    
    
    /**
     * Used by MessageBeanContainer.  true if container started
     * a transaction for this invocation.
     */
    public boolean containerStartsTx;
    
    /**
     * Used by MessageBeanContainer to keep track of the context class
     * loader that was active before message delivery began.
     */
    public ClassLoader originalContextClassLoader;
    
    /**
     * Used for web service invocations to hold SOAP message context.
     * EJBs can access message context through SessionContext.
     */
	/* HARRY: JACC Related Changes */
     public MessageContext messageContext;
    
    /**
     * Used for JACC PolicyContextHandlers. The handler can query the container
     * back for parameters on the ejb. This is set during the method invocation
     * and is not available for preInvoke calls.
     */
    public Object[] methodParams;

    public Timer timer;

    /**
     * Result of txManager.getStatus() performed at the beginning of
     * BaseContainer.preInvoke() and valid up until preinvokeTx().
     * txManager.getStatus() accesses a thread-local which is an 
     * expensive operation.  Storing status in the invocation makes it
     * easier for some of the other early pre-invoke operations to
     * re-use it.  
     */
    private Integer preInvokeTxStatus;

    /**
     * Tells if a CMP2.x bean was found in the Tx cache. Applicable
     * only for CMP2.x beans
     */
    public boolean foundInTxCache = false;

    /**
     * Tells if a fast path can be taken for a business method
     * invocation.
     */
    public boolean useFastPath = false;
  
    private java.util.concurrent.locks.Lock cmcLock;

    private boolean doTxProcessingInPostInvoke;

    private long invId;

    private boolean yetToSubmitStatus = true;

    private EjbFutureTask asyncFuture;

    private boolean wasCancelCalled = false;

    // True if lock is currently held for this invocation
    private boolean holdingSFSBSerializedLock = false;

    public EjbFutureTask getEjbFutureTask() {
        return asyncFuture;
    }

    public void setEjbFutureTask(EjbFutureTask future) {
        asyncFuture = future;
    }

    public void setWasCancelCalled(boolean flag) {
        wasCancelCalled = flag;
    }

    public boolean getWasCancelCalled() {
        return wasCancelCalled;
    }

    public long getInvId() {
        return invId;
    }

    public void setInvId(long invId) {
        this.invId = invId;
    }

    public boolean mustInvokeAsynchronously() {
        return invocationInfo.isAsynchronous() && yetToSubmitStatus;
    }

    public void clearYetToSubmitStatus() {
        yetToSubmitStatus = false;
    }

    public boolean getDoTxProcessingInPostInvoke() {
        return doTxProcessingInPostInvoke;
    }

    public void setDoTxProcessingInPostInvoke(boolean doTxProcessingInPostInvoke) {
        this.doTxProcessingInPostInvoke = doTxProcessingInPostInvoke;
    }

    public EjbInvocation clone() {
        EjbInvocation newInv = (EjbInvocation) super.clone();

        newInv.ejb = null;
        newInv.exception = null;
        newInv.exceptionFromBeanMethod = null;
        newInv.clientTx = null;
        newInv.preInvokeTxStatus = null;
        newInv.originalContextClassLoader = null;
        
        return newInv;
    }

    /**
     * Used by JACC implementation to get an enterprise bean
     * instance for the EnterpriseBean policy handler.  The jacc
     * implementation should use this method rather than directly
     * accessing the ejb field.
     */
    public Object getJaccEjb() {
        Object bean = null;
        if( container != null ) {
            bean = ((Container) container).getJaccEjb(this);
        }
        return bean;
    }
    
    /**
     * This method returns the method interface constant for this EjbInvocation.
     */
    public String getMethodInterface() {
        if (isWebService) {
            return MethodDescriptor.EJB_WEB_SERVICE;
        } else if (isMessageDriven) {
            return MethodDescriptor.EJB_BEAN;
        } else if (isLocal) {
            return (isHome) ? MethodDescriptor.EJB_LOCALHOME :
                    MethodDescriptor.EJB_LOCAL;
        } else {
            return (isHome) ? MethodDescriptor.EJB_HOME :
                    MethodDescriptor.EJB_REMOTE;
        }
    }

    /**
     * Returns CachedPermission associated with this invocation, or
     * null if not available.
     */
    public Object getCachedPermission() {
        return (invocationInfo != null) ? invocationInfo.cachedPermission :
            null;
    }

    /**
     * @return Returns the ejbCtx.
     */
    public EJBContext getEJBContext() {
        return (EJBContext) this.context;
    }

    public Integer getPreInvokeTxStatus() {
        return preInvokeTxStatus;
    }
    
    public void setPreInvokeTxStatus(Integer txStatus) {
        // Can be null, which means preInvokeTxStatus is no longer applicable.
        preInvokeTxStatus = txStatus;
    }

    public java.util.concurrent.locks.Lock getCMCLock() {
        return cmcLock;
    }

    public void setCMCLock(java.util.concurrent.locks.Lock l) {
        cmcLock = l;
    }

    public boolean holdingSFSBSerializedLock() {
        return this.holdingSFSBSerializedLock;
    }

    public void setHoldingSFSBSerializedLock(boolean flag) {
        holdingSFSBSerializedLock = flag;
    }

    @Override
    public Object getTransactionOperationsManager() {
        return this;
    }

    //Implementation of TransactionOperationsManager methods
    
    /**
     * Called by the UserTransaction implementation to verify 
     * access to the UserTransaction methods.
     */
    public boolean userTransactionMethodsAllowed() {
        return ((Container) container).userTransactionMethodsAllowed(this);
    }

    /**
     * Called by the UserTransaction when transaction is started.
     */
    public void doAfterUtxBegin() {
        ((Container) container).doAfterBegin(this);
    }

    //Implementation of InvocationContext methods
    
    private int interceptorIndex;

    public Method   beanMethod;

    // Only set for web service invocations.
    private WebServiceContext webServiceContext;

    // Only set for EJB JAXWS
    //FIXME: private Message message = null;
    private Object message;

    private SOAPMessage soapMessage = null;

    private Map      contextData;

    public InterceptorManager.InterceptorChain getInterceptorChain() {
        return (invocationInfo == null)
            ? null : invocationInfo.interceptorChain;
    }

    /**
     * @return Returns the bean instance.
     */
    public Object getTarget() {
        return this.ejb;
    }
 
    /**
     * @return Returns the timer instance.
     */
    public Object getTimer() {
        return timer;
    }
 
    
    /**
     * @return For AroundInvoke/AroundTimeout methods, returns the bean class 
     *         method being invoked.  For lifecycle callback methods, 
     *         returns null.
     */
    public Method getMethod() {
        return getBeanMethod();
    }
    public Method getBeanMethod() {
        return this.beanMethod;
    }

    /**
     * @return Returns the parameters that will be used to invoke
     * the business method.  If setParameters has been called, 
     * getParameters() returns the values to which the parameters 
     * have been set.
     */
    public Object[] getParameters() {
        return this.methodParams;
    }
    
    /**
     * Set the parameters that will be used to invoke the business method.
     *
     */
    public void setParameters(Object[] params) {
        InterceptorUtil.checkSetParameters(params, getMethod());
        this.methodParams = params;
    }
    
    //The following method is not part of InvocationContext interface
    //  but needed for JAXWS message context propagation
    public void setContextData(WebServiceContext context) {
        this.webServiceContext = context;
    }
    
    /**
     * @return Returns the contextMetaData.
     */
    public Map<String, Object> getContextData() {
        if (this.contextData == null) {
            if (webServiceContext != null)
                this.contextData = webServiceContext.getMessageContext();
            else
                this.contextData = new HashMap<String, Object>();
        }
        return contextData;
    }

    /**
     * This is for EJB JAXWS only.
     * @param message  an unconsumed message
     */
    public <T> void setMessage(T message) {
        this.message = message;
    }

    /**
     * This is for EJB JAXWS only.
     * @return the JAXWS message
     */
    public Object getMessage() {
        return this.message;
    }
    
    /**
     * This is for EJB JAXWS only.
     */
    public SOAPMessage getSOAPMessage() {
        if (message != null && soapMessage == null) {
            try {
                //FIXME: soapMessage = message.readAsSOAPMessage();
                soapMessage = (SOAPMessage) message;
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
            //message consumed, set it to null
            message = null;
        }
        return soapMessage;
    }

    /* (non-Javadoc)
     * @see javax.interceptor.InvocationContext#proceed()
     */
    public Object proceed()
        throws Exception
    {
        try {
            //TODO: Internal error if getInterceptorChain() is null
            interceptorIndex++;
            return getInterceptorChain().invokeNext(interceptorIndex, this);
        } catch (Exception ex) {
            throw ex;
        } catch (Throwable th) {
            throw new Exception(th);
        } finally {
            interceptorIndex--;
        }
    }

    /**
     * Print most useful fields.  Don't do all of them (yet) since there
     * are a large number. 
     * @return
     */
    public String toString() {

        StringBuffer sbuf = new StringBuffer();
        sbuf.append("EjbInvocation  ");
        sbuf.append("componentId="+getComponentId());
        sbuf.append(",isLocal="+isLocal);
        sbuf.append(",isRemote="+isRemote);
        sbuf.append(",isBusinessInterface="+isBusinessInterface);
        sbuf.append(",isWebService="+isWebService);
        sbuf.append(",isMessageDriven="+isMessageDriven);
        sbuf.append(",isHome="+isHome);
        sbuf.append(",clientInterface="+clientInterface);
        sbuf.append(",method="+method);
        sbuf.append(",ejb="+ejb);
        sbuf.append(",exception="+exception);
        sbuf.append(",exceptionFromBeanMethod="+exceptionFromBeanMethod);
        sbuf.append(",invId="+invId);
        sbuf.append(",wasCancelCalled="+wasCancelCalled);
        sbuf.append(",yetToSubmitStatus="+yetToSubmitStatus);

        return sbuf.toString();
    }

    // Implementation of AroundInvokeContext
    public Object[] getInterceptorInstances() {
        return  ((EJBContextImpl)context).getInterceptorInstances();
    }

    public  Object invokeBeanMethod() throws Throwable {
        return ((BaseContainer) container).invokeBeanMethod(this);
    }

    /*********************************************************/


    
    public com.sun.enterprise.security.SecurityManager getEjbSecurityManager() {
        return ((BaseContainer)container).getSecurityManager();
    }

    public boolean isAWebService() {
        return this.isWebService;
    }

    public Object[] getMethodParams() {
        return this.methodParams;
    }

    public boolean authorizeWebService(Method m) throws Exception {
        Exception ie = null;
        if (isAWebService()) {
		try {
		    this.method = m;
		    if (!((com.sun.ejb.Container)container).authorize(this)) {
			ie = new Exception
			    ("Client not authorized for invocation of method {" + method + "}");       
		    } else {
			// Record the method on which the successful
			// authorization check was performed. 
                        //TODO:V3 the method is not currently available, waiting for inputs from Bhakti
			//inv.setWebServiceMethod(eInv.method);
		    }
		} catch(Exception e) {
		    String errorMsg = "Error unmarshalling method {" + method + "} for ejb "; 
		    ie = new UnmarshalException(errorMsg); 
		    ie.initCause(e);
		} 
		if ( ie != null ) {
		    exception = ie;
		    throw ie;
		} 

	    } else {
                //TODO:V3 the method is not currently available, waiting for inputs from Bhakti
		//inv.setWebServiceMethod(null);
	    }
             return true;
	}            
       
    }
    

