package org.intermine.objectstore.webservice.ser;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

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
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.util.TypeUtil;
import org.xml.sax.InputSource;

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
        MappingUtil.registerDefaultMappings(tm);
        msg.output(context);
            
        String msgString = stringWriter.toString();
        System.out.println(msgString);
            
        DeserializationContext dser = new DeserializationContextImpl(
                                                                     new InputSource(new StringReader(msgString)),
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

        IqlQuery q = new IqlQuery("select c from Company as c", null);
        q.setParameters(l);
        args.add(q);

        Model m = Model.getInstanceByName("testmodel");
        args.add(m);
         
        return args;
    }
}
