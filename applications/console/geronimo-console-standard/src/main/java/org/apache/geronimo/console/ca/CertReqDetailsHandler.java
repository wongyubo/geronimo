/**
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.console.ca;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.console.MultiPageModel;
import org.apache.geronimo.management.geronimo.CertificationAuthority;

/**
 * Handler for CSR details screen.
 *
 * @version $Rev$ $Date$
 */
public class CertReqDetailsHandler extends BaseCAHandler {
    private final static Log log = LogFactory.getLog(CertReqDetailsHandler.class);
    public CertReqDetailsHandler() {
        super(CERT_REQ_DETAILS_MODE, "/WEB-INF/view/ca/certReqDetails.jsp");
    }

    public String actionBeforeView(ActionRequest request, ActionResponse response, MultiPageModel model) throws PortletException, IOException {
        String[] params = {ERROR_MSG, INFO_MSG, "algorithm", "sNo", "validFrom", "validTo", "pkcs10certreq", "subject", "publickey", "requestId"};
        for(int i = 0; i < params.length; ++i) {
            String value = request.getParameter(params[i]);
            if(value != null) response.setRenderParameter(params[i], value);
        }
        String sNo = request.getParameter("sNo");
        if(sNo == null) {
            // Freshly loading the certificate request details screen
            CertificationAuthority ca = getCertificationAuthority(request);
            try {
                sNo = ca.getNextSerialNumber().toString();
                response.setRenderParameter("sNo", sNo);
            } catch (Exception e) {
                log.error("Unable to get next serial number from CA.", e);
                response.setRenderParameter(ERROR_MSG, e.toString());
            }
        }
        return getMode();
    }

    public void renderView(RenderRequest request, RenderResponse response, MultiPageModel model) throws PortletException, IOException {
        String[] params = {ERROR_MSG, INFO_MSG, "subject", "publickey", "sNo", "validFrom", "validTo", "algorithm", "pkcs10certreq", "requestId"};
        for(int i = 0; i < params.length; ++i) {
            Object value = request.getParameter(params[i]);
            if(value != null) request.setAttribute(params[i], value);
        }
    }

    public String actionAfterView(ActionRequest request, ActionResponse response, MultiPageModel model) throws PortletException, IOException {
        String errorMsg = null;

        try {
            // Validate the Serial Number
            String sNo = request.getParameter("sNo");
            new BigInteger(sNo.trim());
            
            // Validate the from and to dates
            String validFrom = request.getParameter("validFrom");
            String validTo = request.getParameter("validTo");
            DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
            // Check if the from date format is MM/DD/YYYY
            Date validFromDate = df.parse(validFrom);
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(validFromDate);
            String mmddyyyy = (calendar.get(Calendar.MONTH) < 9 ? "0":"") + (calendar.get(Calendar.MONTH)+1);
            mmddyyyy += "/"+(calendar.get(Calendar.DAY_OF_MONTH) < 10 ? "0":"") + (calendar.get(Calendar.DAY_OF_MONTH));
            mmddyyyy += "/"+calendar.get(Calendar.YEAR);
            if(!mmddyyyy.equals(validFrom)) {
                throw new Exception("validFrom must be a date in MM/DD/YYYY format.");
            }
            // Check if the to date format is MM/DD/YYYY
            Date validToDate = df.parse(validTo);
            calendar.setTime(validToDate);
            mmddyyyy = (calendar.get(Calendar.MONTH) < 9 ? "0":"") + (calendar.get(Calendar.MONTH)+1);
            mmddyyyy += "/"+(calendar.get(Calendar.DAY_OF_MONTH) < 10 ? "0":"") + (calendar.get(Calendar.DAY_OF_MONTH));
            mmddyyyy += "/"+calendar.get(Calendar.YEAR);
            if(!mmddyyyy.equals(validTo)) {
                throw new Exception("validTo must be a date in MM/DD/YYYY format.");
            }
            // Check if the from date is before the to date
            if(validFromDate.after(validToDate)) {
                throw new Exception("Validity: From date '"+validFrom+"' is before the To date '"+validTo+"'.");
            }
            
            // Go to client certificate confirmation page
            return CONFIRM_CLIENT_CERT_MODE+BEFORE_ACTION;
        } catch(Exception e) {
            errorMsg = e.toString();
            log.error("Errors in user input while processing a CSR.", e);
        }
        
        if(errorMsg != null) response.setRenderParameter(ERROR_MSG, errorMsg);
        return getMode()+BEFORE_ACTION;
    }
}
