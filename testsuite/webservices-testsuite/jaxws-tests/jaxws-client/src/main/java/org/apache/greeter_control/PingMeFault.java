/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.greeter_control;

import javax.xml.ws.WebFault;
import org.apache.greeter_control.types.FaultDetail;


@WebFault(name = "faultDetail", targetNamespace = "http://apache.org/greeter_control/types")
public class PingMeFault
    extends Exception
{

    /**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private FaultDetail faultInfo;

    /**
     * 
     * @param faultInfo
     * @param message
     */
    public PingMeFault(String message, FaultDetail faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param faultInfo
     * @param message
     * @param cause
     */
    public PingMeFault(String message, FaultDetail faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: org.apache.greeter_control.types.FaultDetail
     */
    public FaultDetail getFaultInfo() {
        return faultInfo;
    }

}
