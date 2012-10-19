package org.intermine.api.bag;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class InvitationHandler extends DefaultHandler {

    private static class UnresolvedInvite {
        String bag;
        String invitee;
        String token;
        Boolean accepted;
        Date acceptedAt;
        Date createdAt;
    }
    private final List<UnresolvedInvite> unresolved = new ArrayList<UnresolvedInvite>();

    private String currentName = null;
    private StringBuffer currentValue = null; 
    private UnresolvedInvite currentInvite = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if ("invitations".equals(qName)) {
            currentName = null;
            currentValue = null;
        } else if ("invite".equals(qName)) {
            currentInvite = new UnresolvedInvite();
            unresolved.add(currentInvite);
            currentName = null;
            currentValue = null;
        } else {
            currentName = qName;
            currentValue = new StringBuffer();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (currentValue != null) currentValue.append(ch, start, length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        if (currentName != null && currentValue != null) {
            String value = currentValue.toString().trim();
            if ("bag".equals(currentName)) {
                currentInvite.bag = value;
            } else if ("invitee".equals(currentName)) {
                currentInvite.invitee = value;
            } else if ("token".equals(currentName)) {
                currentInvite.token = value;
            } else if ("accepted".equals(currentName)) {
                currentInvite.accepted = (StringUtils.isBlank(value)) ? null : Boolean.parseBoolean(value);
            } else if ("acceptedAt".equals(currentName)) {
                currentInvite.acceptedAt = (StringUtils.isBlank(value)) ? null : new Date(Long.valueOf(value));
            } else if ("createdAt".equals(currentName)) {
                currentInvite.createdAt = (StringUtils.isBlank(value)) ? null : new Date(Long.valueOf(value));
            }
        }
        currentValue = null;
        currentName = null;
    }

    public void storeInvites(Profile inviter) throws SQLException {
        for (UnresolvedInvite unres: unresolved) {
            InterMineBag bag = inviter.getSavedBags().get(unres.bag);
            SharingInvite invite = new SharingInvite(
              bag, unres.invitee, unres.token, unres.createdAt,
              unres.acceptedAt, unres.accepted);
            invite.save();
        }
    }
}
