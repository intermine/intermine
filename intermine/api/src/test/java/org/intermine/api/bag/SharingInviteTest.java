package org.intermine.api.bag;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.api.InterMineAPITestCase;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManager;
import org.intermine.api.tag.TagNames;
import org.intermine.metadata.FieldDescriptor;

public class SharingInviteTest extends InterMineAPITestCase {

	private Profile superUser;
	private Profile bob;
	private Profile alice;
	private InterMineBag bobBag1;
	private InterMineBag bobBag2;
	private InterMineBag globalBag;
	private InterMineBag aliceBag1;
	private TagManager tagManager;
	private SharedBagManager sbm;
	private InterMineBag bobBag3;
	private InterMineBag bobBag4;
	private InterMineBag bobBag5;

	public SharingInviteTest(String arg) {
		super(arg);
	}
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		ProfileManager pm = im.getProfileManager();
		tagManager = pm.getTagManager();
		sbm = SharedBagManager.getInstance(pm);
		superUser = pm.getSuperuserProfile();
		bob = new Profile(
			pm, "bob", 101, "bob_pass",
			new HashMap(), new HashMap(), new HashMap(), true, false);
        pm.createProfile(bob);
        alice = new Profile(
        	pm, "alice", 101, "alice_pass",
        	new HashMap(), new HashMap(), new HashMap(), true, false);
        pm.createProfile(alice);
        setUpBagsAndTags();
	}
	
	private void setUpBagsAndTags() throws Exception {
		Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();
		globalBag = superUser.createBag("globalBag", "Department", "", classKeys);
	    bobBag1 = bob.createBag("bobBag1", "Company", "", classKeys);
        bobBag2 = bob.createBag("bobBag2", "Address", "", classKeys);
        bobBag3 = bob.createBag("bobBag3", "Address", "", classKeys);
        bobBag4 = bob.createBag("bobBag4", "Address", "", classKeys);
        bobBag5 = bob.createBag("bobBag5", "Address", "", classKeys);
        aliceBag1 = alice.createBag("aliceBag1", "Employee", "", classKeys);

        if (tagManager == null) {
            throw new NullPointerException("oops - tag-manager not set");
        }
        tagManager.addTag(TagNames.IM_PUBLIC, globalBag, superUser);
	}
	
	public void testInitialState() {
		assertFalse(im.getBagManager().getBags(alice).containsKey("bobBag1"));
		assertFalse(im.getBagManager().getBags(bob).containsKey("aliceBag1"));
		assertTrue(im.getBagManager().getBags(alice).containsKey("globalBag"));
		assertTrue(im.getBagManager().getBags(alice).containsKey("aliceBag1"));
		assertTrue(im.getBagManager().getBags(bob).containsKey("globalBag"));
	
	}
	
	public void testCreateInvite() throws Exception {
		SharingInvite invite = SharedBagManager.inviteToShare(bobBag1, "alice-email");
		Collection<SharingInvite> bobsInvites = SharingInvite.getInvites(im, bob);
		assertTrue(bobsInvites.contains(invite));
	}
	
	public void testAcceptInvite() throws Exception {
		assertFalse(im.getBagManager().getBags(alice).containsKey("bobBag1"));
		SharingInvite invite = SharedBagManager.inviteToShare(bobBag1, "alice-email");
		sbm.acceptInvitation(invite, alice);
		assertTrue(im.getBagManager().getBags(alice).containsKey("bobBag1"));
	}
	
	public void testAcceptInvite2() throws Exception {
		long start = System.currentTimeMillis();
		assertFalse(im.getBagManager().getBags(alice).containsKey("bobBag1"));
		SharingInvite invite = SharedBagManager.inviteToShare(bobBag1, "alice-email");
		SharingInvite restored = SharingInvite.getByToken(im, invite.getToken());
		assertTrue(invite.equals(restored));
		sbm.acceptInvitation(restored, alice);
		assertTrue(im.getBagManager().getBags(alice).containsKey("bobBag1"));
		long end = System.currentTimeMillis();
		System.out.printf("Time in test: %.3fsec\n", Float.valueOf(end - start) / Float.valueOf(1000.0f));
	}
	
	public void testAcceptMany() throws Exception {
		long start = System.currentTimeMillis();

		InterMineBag[] bags = new InterMineBag[]{bobBag1, bobBag2, bobBag3, bobBag4, bobBag5};
		for (InterMineBag bag: bags) {
			SharingInvite invite = SharedBagManager.inviteToShare(bag, "alice-email");
			sbm.acceptInvitation(invite, alice);
		}
		Map<String, InterMineBag> aliceBags = im.getBagManager().getBags(alice); 
		for (InterMineBag bag: bags) {
			assertTrue(aliceBags.containsKey(bag.getName()));
		}
		long end = System.currentTimeMillis();
		System.out.printf("Time in test many: %.3fsec\n", Float.valueOf(end - start) / Float.valueOf(1000.0f));
	}

}
