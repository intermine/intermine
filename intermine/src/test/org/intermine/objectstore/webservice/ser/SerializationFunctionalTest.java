package org.flymine.objectstore.webservice.ser;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.axis.Constants;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.encoding.DeserializationContext;
import org.apache.axis.encoding.DeserializationContextImpl;
import org.apache.axis.encoding.SerializationContext;
import org.apache.axis.encoding.SerializationContextImpl;
import org.apache.axis.encoding.TypeMapping;
import org.apache.axis.encoding.TypeMappingRegistry;
import org.apache.axis.message.RPCElement;
import org.apache.axis.message.RPCParam;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.server.AxisServer;
import org.xml.sax.InputSource;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.ArrayList;

import org.flymine.util.TypeUtil;

import org.flymine.objectstore.query.fql.FqlQuery;
import org.flymine.metadata.Model;
import org.flymine.objectstore.query.ResultsInfo;
import org.flymine.model.testmodel.Address;
import org.flymine.model.testmodel.Company;
import org.flymine.model.testmodel.Department;

import junit.framework.TestCase;

public class SerializationFunctionalTest extends TestCase
{
    public void testEncoding() throws Exception {
        MessageContext msgContext = new MessageContext(new AxisServer());
        SOAPEnvelope msg = new SOAPEnvelope();

        List args = args();
        List params = new ArrayList();
        for (int i = 0; i < args.size(); i++) {
            params.add(new RPCParam("", "arg" + i, args.get(i)));
        }

        RPCElement body = new RPCElement("urn:myNamespace", "method1", params.toArray());
        msg.addBodyElement(body);
        
        Writer stringWriter = new StringWriter();
        SerializationContext context = new SerializationContextImpl(stringWriter, msgContext);
            
        TypeMappingRegistry reg = context.getTypeMappingRegistry();
        TypeMapping tm = (TypeMapping) reg.getTypeMapping(Constants.URI_SOAP11_ENC);
        if (tm == null) {
            tm = (TypeMapping) reg.createTypeMapping();
            reg.register(Constants.URI_DEFAULT_SOAP_ENC, tm);
        }
        SerializationUtil.registerDefaultMappings(tm);
        SerializationUtil.registerMappings(tm, Model.getInstanceByName("testmodel"));
        msg.output(context);
            
        String msgString = stringWriter.toString();
        Reader reader = new StringReader(msgString);
            
        DeserializationContext dser = new DeserializationContextImpl(
                                                                     new InputSource(reader),
                                                                     msgContext, Message.REQUEST);
        dser.parse();
        SOAPEnvelope env = dser.getEnvelope();
            
        RPCElement rpcElem = (RPCElement) env.getFirstBody();
        for (int i = 0; i < args.size(); i++) {
            assertEquals(args.get(i), rpcElem.getParam("arg" + i).getValue());
        }
    }

    static List args() throws Exception {
        List args = new ArrayList();

        List l = new ArrayList();
        l.add("one");
        l.add("two");
        args.add(l);

        ResultsInfo ri = new ResultsInfo();
        TypeUtil.setFieldValue(ri, "rows", new Integer(4));
        TypeUtil.setFieldValue(ri, "start", new Long(2));
        TypeUtil.setFieldValue(ri, "complete", new Long(3));
        TypeUtil.setFieldValue(ri, "min", new Integer(1));
        TypeUtil.setFieldValue(ri, "max", new Integer(5));
        args.add(ri);

        FqlQuery q = new FqlQuery("select c from Company as c", null);
        q.setParameters(l);
        args.add(q);

        ProxyBean b = new ProxyBean("Company", q, new Integer(42));
        args.add(b);
        
        Model m = Model.getInstanceByName("testmodel");
        args.add(m);

        Address a1 = new Address();
        a1.setAddress("a1");
        Company c1 = new Company();
        c1.setName("c1");
        c1.setVatNumber(101);
        c1.setAddress(a1);
        Department d1 = new Department();
        d1.setName("d1");
        d1.setCompany(c1);
        Department d2 = new Department();
        d2.setName("d2");
        d2.setCompany(c1);
        c1.getDepartments().add(d1);
        c1.getDepartments().add(d2);
        args.add(c1);
         
        return args;
    }
}
