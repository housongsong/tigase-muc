/*
 * Tigase Jabber/XMPP Multi-User Chat Component
 * Copyright (C) 2008 "Bartosz M. Małkowski" <bartosz.malkowski@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.muc.modules;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import tigase.criteria.Criteria;
import tigase.criteria.ElementCriteria;
import tigase.muc.ElementWriter;
import tigase.muc.MucConfig;
import tigase.muc.exceptions.MUCException;
import tigase.muc.repository.IMucRepository;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;

/**
 * @author bmalkow
 * 
 */
public class UniqueRoomNameModule extends AbstractModule {

	private final static String CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

	private static final Criteria CRIT = ElementCriteria.nameType("iq", "get").add(
			ElementCriteria.name("unique", "http://jabber.org/protocol/muc#unique"));

	private SecureRandom random;

	public UniqueRoomNameModule(MucConfig config, ElementWriter writer, IMucRepository mucRepository) {
		super(config, writer, mucRepository);
		try {
			this.random = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			this.random = new SecureRandom();
		}
	}

	private String generateName(int len) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < len; i++) {
			int a = random.nextInt(CHARS.length());
			sb.append(CHARS.charAt(a));
		}
		return sb.toString();
	}

	@Override
	public String[] getFeatures() {
		return null;
	}

	@Override
	public Criteria getModuleCriteria() {
		return CRIT;
	}

	@Override
	public void process(Packet element) throws MUCException {
		try {
			JID jid = JID.jidInstance(element.getAttribute("to"));
			if (jid.getResource() != null) {
				throw new MUCException(Authorization.BAD_REQUEST);
			}
			final String host = jid.getDomain();

			String newRoomName;
			do {
				newRoomName = generateName(30);
			} while (repository.isRoomIdExists(newRoomName + "@" + host));

			Element unique = new Element("unique", new String[] { "xmlns" },
					new String[] { "http://jabber.org/protocol/muc#unique" });
			unique.setCData(newRoomName);
			writer.write(element.okResult(unique, 0));
		} catch (MUCException e1) {
			throw e1;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
