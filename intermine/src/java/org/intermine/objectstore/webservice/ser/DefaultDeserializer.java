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

import org.apache.axis.encoding.DeserializerImpl;

/**
 * This the deserializer (xml to object translator) for all objects used in objectstore calls
 * (except lists...which will eventually be handled by axis)
 *
 * @author Mark Woodbridge
 */
public class DefaultDeserializer extends DeserializerImpl
{
//     public SOAPHandler onStartChild(String namespace,
//                                     String localName,
//                                     String prefix,
//                                     Attributes attributes,
//                                     DeserializationContext context)
//         throws SAXException {
//         if (log.isDebugEnabled()) {
//             log.debug("Enter: VectorDeserializer::onStartChild()");
//         }
        
//         if (attributes == null)
//             throw new SAXException(Messages.getMessage("noType01"));

//         // If the xsi:nil attribute, set the value to null and return since
//         // there is nothing to deserialize.
//         if (context.isNil(attributes)) {
//             setChildValue(null, new Integer(curIndex++));
//             return null;
//         }

//         // Get the type
//         QName itemType = context.getTypeFromAttributes(namespace,
//                                                        localName,
//                                                        attributes);
//         // Get the deserializer
//         Deserializer dSer = null;
//         if (itemType != null) {
//            dSer = context.getDeserializerForType(itemType);
//         }
//         if (dSer == null) {
//             dSer = new DeserializerImpl();
//         }

//         // When the value is deserialized, inform us.
//         // Need to pass the index because multi-ref stuff may 
//         // result in the values being deserialized in a different order.
//         dSer.registerValueTarget(new DeserializerTarget(this, new Integer(curIndex)));
//         curIndex++;

//         if (log.isDebugEnabled()) {
//             log.debug("Exit: VectorDeserializer::onStartChild()");
//         }
        
//         // Let the framework know that we aren't complete until this guy
//         // is complete.
//         addChildDeserializer(dSer);
        
//         return (SOAPHandler)dSer;
//     }

//    public void setChildValue(Object value, Object hint) throws SAXException {
//         if (log.isDebugEnabled()) {
//             log.debug(Messages.getMessage("gotValue00", "VectorDeserializer", "" + value));
//         }
//         int offset = ((Integer)hint).intValue();
//         Vector v = (Vector)this.value;
        
//         // If the vector is too small, grow it 
//         if (offset >= v.size()) {
//             v.setSize(offset+1);
//         }
//         v.setElementAt(value, offset);
//     }
}
