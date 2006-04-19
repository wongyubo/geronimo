/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.axis.builder;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.wsdl.Binding;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.OperationType;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import junit.framework.TestCase;
import org.apache.axis.constants.Style;
import org.apache.axis.soap.SOAPConstants;
import org.apache.geronimo.axis.builder.bookquote.BookQuote;
import org.apache.geronimo.axis.builder.bookquote.BookQuoteService;
import org.apache.geronimo.axis.builder.interop.InteropLab;
import org.apache.geronimo.axis.builder.interop.InteropTestPortType;
import org.apache.geronimo.axis.builder.mock.MockPort;
import org.apache.geronimo.axis.builder.mock.MockService;
import org.apache.geronimo.axis.client.OperationInfo;
import org.apache.geronimo.axis.client.AxisServiceReference;
import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.deployment.DeploymentContext;
import org.apache.geronimo.deployment.util.UnpackedJarFile;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.j2ee.deployment.EJBModule;
import org.apache.geronimo.j2ee.deployment.Module;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.kernel.Jsr77Naming;
import org.apache.geronimo.kernel.config.ConfigurationModuleType;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.kernel.repository.ArtifactManager;
import org.apache.geronimo.kernel.repository.DefaultArtifactManager;
import org.apache.geronimo.kernel.repository.ArtifactResolver;
import org.apache.geronimo.kernel.repository.DefaultArtifactResolver;
import org.apache.geronimo.xbeans.geronimo.naming.GerServiceRefType;
import org.apache.geronimo.xbeans.j2ee.JavaWsdlMappingDocument;
import org.apache.geronimo.xbeans.j2ee.JavaWsdlMappingType;
import org.apache.geronimo.xbeans.j2ee.PackageMappingType;

/**
 * @version $Rev:385232 $ $Date$
 */
public class ServiceReferenceTest extends TestCase {
    private static final File basedir = new File(System.getProperty("basedir", System.getProperty("user.dir")));

    public final static String NAMESPACE = "http://geronimo.apache.org/axis/mock";
    private File tmpbasedir;
    private Environment environment = new Environment();
    private Artifact configID = new Artifact("group", "test", "1", "car");
    private DeploymentContext context;
    private ClassLoader isolatedCl = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
    private final String operationName = "doMockOperation";
    private final File wsdlDir = new File(basedir, "src/test-resources/interop");
    private final File wsdlFile = new File(wsdlDir, "interop.wsdl");
    private List handlerInfos = new ArrayList();
    private GerServiceRefType gerServiceRefType = null;

    private Module module;

    private boolean runExternalWSTest;

    protected void setUp() throws Exception {
        super.setUp();
        tmpbasedir = File.createTempFile("car", "tmp");
        tmpbasedir.delete();
        tmpbasedir.mkdirs();
        environment.setConfigId(configID);
        Jsr77Naming naming = new Jsr77Naming();
        ArtifactManager artifactManager = new DefaultArtifactManager();
        ArtifactResolver artifactResolver = new DefaultArtifactResolver(artifactManager, Collections.EMPTY_SET, null);
        context = new DeploymentContext(tmpbasedir, null, environment, ConfigurationModuleType.CAR, naming, Collections.EMPTY_SET, Collections.EMPTY_SET, artifactResolver);

        File moduleLocation = new File(tmpbasedir, "ejb");
        moduleLocation.mkdirs();
        AbstractName moduleName = naming.createRootName(configID, "testejb", NameFactory.EJB_MODULE);
        module = new EJBModule(true, moduleName, environment, new UnpackedJarFile(moduleLocation), "ejb", null, null, null);

        runExternalWSTest = System.getProperty("geronimo.run.external.webservicetest", "false").equals("true");
    }

    protected void tearDown() throws Exception {
        recursiveDelete(tmpbasedir);
        super.tearDown();
    }

/*
    public void testServiceProxy() throws Exception {
        //construct the SEI proxy
        Map portMap = new HashMap();
        MockSEIFactory factory = new MockSEIFactory();
        portMap.put("MockPort", factory);
        Map seiClassNameToFactoryMap = new HashMap();
        seiClassNameToFactoryMap.put(MockPort.class.getName(), factory);
        AxisBuilder builder = new AxisBuilder();
        Object service = builder.createServiceInterfaceProxy(MockService.class, portMap, seiClassNameToFactoryMap, context, module, isolatedCl);
        assertTrue(service instanceof MockService);
        MockService mockService = (MockService) service;
        MockPort mockPort = mockService.getMockPort();
        assertNotNull(mockPort);
    }
*/

/*
    public void testServiceEndpointProxy() throws Exception {
        AxisBuilder builder = new AxisBuilder();

        ServiceImpl serviceInstance = new ServiceImpl(null, null);

        URL location = new URL("http://geronimo.apache.org/ws");

        OperationInfo op = buildOperationInfoForMockOperation(builder);
        OperationInfo[] operationInfos = new OperationInfo[]{op};
        Class serviceEndpointClass = builder.enhanceServiceEndpointInterface(MockPort.class, context, module, isolatedCl);
        String portName = "foo";
        SEIFactory serviceInterfaceFactory = builder.createSEIFactory(null, portName, serviceEndpointClass, serviceInstance, Collections.EMPTY_LIST, location, operationInfos, handlerInfos, null, context, isolatedCl);
        assertNotNull(serviceInterfaceFactory);
        Remote serviceInterface = serviceInterfaceFactory.createServiceEndpoint();
        assertTrue(serviceInterface instanceof MockPort);
    }
*/

    public void testBuildOperationInfo() throws Exception {
        AxisBuilder builder = new AxisBuilder();
        OperationInfo operationInfo = buildOperationInfoForMockOperation(builder);
        assertNotNull(operationInfo);
    }

    public void testBuildFullServiceProxy() throws Exception {
        Definition definition = buildDefinition();
        SchemaInfoBuilder schemaInfoBuilder = new SchemaInfoBuilder(null, definition);
        JavaWsdlMappingType mapping = buildLightweightMappingType();
        QName serviceQName = new QName(NAMESPACE, "MockService");
        AxisBuilder builder = new AxisBuilder();
        Object reference = builder.createService(MockService.class, schemaInfoBuilder, mapping, serviceQName, SOAPConstants.SOAP11_CONSTANTS, handlerInfos, gerServiceRefType, context, module, isolatedCl);
        assertNotNull(reference);
        assertTrue(reference instanceof AxisServiceReference);
        AxisServiceReference claReference = (AxisServiceReference) reference;
        claReference.setClassLoader(isolatedCl);
        Object proxy = claReference.getContent();
        assertTrue(proxy instanceof MockService);
        MockPort mockPort = ((MockService) proxy).getMockPort();
        assertNotNull(mockPort);
    }

    public void testBuildBookQuoteProxy() throws Exception {
        File wsdl = new File(basedir, "src/test-resources/BookQuote.wsdl");
        WSDLFactory factory = WSDLFactory.newInstance();
        WSDLReader reader = factory.newWSDLReader();
        Definition definition = reader.readWSDL(wsdl.toURI().toString());
        SchemaInfoBuilder schemaInfoBuilder = new SchemaInfoBuilder(null, definition);
        File jaxrpcMapping = new File(basedir, "src/test-resources/BookQuote.xml");
        JavaWsdlMappingDocument mappingDocument = JavaWsdlMappingDocument.Factory.parse(jaxrpcMapping);
        JavaWsdlMappingType mapping = mappingDocument.getJavaWsdlMapping();
        QName serviceQName = new QName("http://www.Monson-Haefel.com/jwsbook/BookQuote", "BookQuoteService");
        AxisBuilder builder = new AxisBuilder();
        Object reference = builder.createService(BookQuoteService.class, schemaInfoBuilder, mapping, serviceQName, SOAPConstants.SOAP11_CONSTANTS, handlerInfos, gerServiceRefType, context, module, isolatedCl);
        assertNotNull(reference);
        assertTrue(reference instanceof AxisServiceReference);
        AxisServiceReference claReference = (AxisServiceReference) reference;
        claReference.setClassLoader(isolatedCl);
        Object proxy = claReference.getContent();
        assertTrue(proxy instanceof BookQuoteService);
        BookQuote bookQuote = ((BookQuoteService) proxy).getBookQuotePort();
        assertNotNull(bookQuote);
    }

    //needs to have heavyweight mapping
    public void xtestBuildInteropProxy() throws Exception {
        WSDLFactory factory = WSDLFactory.newInstance();
        WSDLReader reader = factory.newWSDLReader();
        Definition definition = reader.readWSDL(wsdlFile.toURI().toString());
        SchemaInfoBuilder schemaInfoBuilder = new SchemaInfoBuilder(null, definition);
        File jaxrpcMapping = new File(basedir, "src/test-resources/interop/interop-jaxrpcmapping.xml");
        JavaWsdlMappingDocument mappingDocument = JavaWsdlMappingDocument.Factory.parse(jaxrpcMapping);
        JavaWsdlMappingType mapping = mappingDocument.getJavaWsdlMapping();
        QName serviceQName = new QName("http://tempuri.org/4s4c/1/3/wsdl/def/interopLab", "interopLab");
        AxisBuilder builder = new AxisBuilder();
        Object proxy = builder.createService(InteropLab.class, schemaInfoBuilder, mapping, serviceQName, SOAPConstants.SOAP11_CONSTANTS, handlerInfos, gerServiceRefType, context, module, isolatedCl);
        assertNotNull(proxy);
        assertTrue(proxy instanceof InteropLab);
        InteropTestPortType interopTestPort = ((InteropLab) proxy).getinteropTestPort();
        assertNotNull(interopTestPort);
        testInteropPort(interopTestPort);
    }

    private void testInteropPort(InteropTestPortType interopTestPort) throws java.rmi.RemoteException {
        if (runExternalWSTest) {
            System.out.println("Running external ws test");
            int result = interopTestPort.echoInteger(1);
            assertEquals(result, 1);
        } else {
            System.out.println("Skipping external ws test");
        }
    }

    public void testBuildComplexTypeMap() throws Exception {
        WSDLFactory factory = WSDLFactory.newInstance();
        WSDLReader reader = factory.newWSDLReader();
        Definition definition = reader.readWSDL(wsdlFile.toURI().toString());
        SchemaInfoBuilder schemaInfoBuilder = new SchemaInfoBuilder(null, definition);
        Map complexTypeMap = schemaInfoBuilder.getComplexTypesInWsdl();
        assertEquals(7, complexTypeMap.size());
    }


    private OperationInfo buildOperationInfoForMockOperation(AxisBuilder builder) throws NoSuchMethodException, DeploymentException, WSDLException {
        Class portClass = MockPort.class;
        Method method = portClass.getDeclaredMethod("doMockOperation", new Class[]{String.class});
        WSDLFactory factory = WSDLFactory.newInstance();
        Definition definition = factory.newDefinition();
        ExtensionRegistry extensionRegistry = factory.newPopulatedExtensionRegistry();
        BindingOperation bindingOperation = buildBindingOperation(definition, extensionRegistry);

        Style defaultStyle = Style.DOCUMENT;

        OperationInfo operationInfo = builder.buildOperationInfoLightweight(method, bindingOperation, defaultStyle, SOAPConstants.SOAP11_CONSTANTS);
        return operationInfo;
    }

    private Definition buildDefinition() throws WSDLException {
        WSDLFactory factory = WSDLFactory.newInstance();
        Definition definition = factory.newDefinition();
        definition.setDocumentBaseURI("META-INF/wsdl/fake.wsdl");
        ExtensionRegistry extensionRegistry = factory.newPopulatedExtensionRegistry();
        BindingOperation bindingOperation = buildBindingOperation(definition, extensionRegistry);
        Binding binding = definition.createBinding();
        binding.setQName(new QName(NAMESPACE, "MockPortBinding"));
        //add soap:binding
        SOAPBinding soapBinding = (SOAPBinding) extensionRegistry.createExtension(Binding.class, new QName("http://schemas.xmlsoap.org/wsdl/soap/", "binding"));
        soapBinding.setTransportURI("http://schemas.xmlsoap.org/soap/http");
        soapBinding.setStyle("rpc");
        binding.addExtensibilityElement(soapBinding);
        binding.addBindingOperation(bindingOperation);
        PortType portType = definition.createPortType();
        portType.setQName(new QName(NAMESPACE, "MockPort"));
        portType.addOperation(bindingOperation.getOperation());
        binding.setPortType(portType);
        Port port = definition.createPort();
        port.setName("MockPort");
        //add soap:address
        SOAPAddress soapAddress = (SOAPAddress) extensionRegistry.createExtension(Port.class, new QName("http://schemas.xmlsoap.org/wsdl/soap/", "address"));
        soapAddress.setLocationURI("http://127.0.0.1:8080/foo");
        port.addExtensibilityElement(soapAddress);
        port.setBinding(binding);
        javax.wsdl.Service service = definition.createService();
        service.setQName(new QName(NAMESPACE, "MockService"));
        service.addPort(port);
        definition.addService(service);
        return definition;
    }

    private BindingOperation buildBindingOperation(Definition definition, ExtensionRegistry extensionRegistry) throws WSDLException {
        Operation operation = definition.createOperation();
        operation.setName(operationName);
        operation.setStyle(OperationType.REQUEST_RESPONSE);
        Input input = definition.createInput();
        Message inputMessage = definition.createMessage();
        Part inputPart = definition.createPart();
        inputPart.setName("string");
        inputPart.setTypeName(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        inputMessage.addPart(inputPart);
        operation.setInput(input);
        input.setMessage(inputMessage);
        Output output = definition.createOutput();
        Message outputMessage = definition.createMessage();
        operation.setOutput(output);
        output.setMessage(outputMessage);
        BindingOperation bindingOperation = definition.createBindingOperation();
        SOAPOperation soapOperation = (SOAPOperation) extensionRegistry.createExtension(BindingOperation.class, new QName("http://schemas.xmlsoap.org/wsdl/soap/", "operation"));
        soapOperation.setSoapActionURI("actionURI");
        soapOperation.setStyle("rpc");
        bindingOperation.addExtensibilityElement(soapOperation);
        bindingOperation.setOperation(operation);
        bindingOperation.setName(operation.getName());
        BindingInput bindingInput = definition.createBindingInput();
        SOAPBody inputBody = (SOAPBody) extensionRegistry.createExtension(BindingInput.class, new QName("http://schemas.xmlsoap.org/wsdl/soap/", "body"));
        inputBody.setUse("encoded");
        bindingInput.addExtensibilityElement(inputBody);
        bindingOperation.setBindingInput(bindingInput);
        BindingOutput bindingOutput = definition.createBindingOutput();
        bindingOutput.addExtensibilityElement(inputBody);
        bindingOperation.setBindingOutput(bindingOutput);
        return bindingOperation;
    }

    private JavaWsdlMappingType buildLightweightMappingType() {
        JavaWsdlMappingType mapping = JavaWsdlMappingType.Factory.newInstance();
        PackageMappingType packageMapping = mapping.addNewPackageMapping();
        packageMapping.addNewNamespaceURI().setStringValue(NAMESPACE);
        packageMapping.addNewPackageType().setStringValue("org.apache.geronimo.axis.builder.mock");
        return mapping;
    }


    private void recursiveDelete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                recursiveDelete(files[i]);
            }
        }
        file.delete();
    }

}
