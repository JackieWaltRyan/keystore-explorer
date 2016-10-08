/*
 * Copyright 2004 - 2013 Wayne Grant
 *           2013 - 2016 Kai Kramer
 *
 * This file is part of KeyStore Explorer.
 *
 * KeyStore Explorer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * KeyStore Explorer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with KeyStore Explorer.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.keystore_explorer.gui.dnchooser;

import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.style.BCStyle;

public class OidDisplayNameMapping {

	// TODO use resource bundle for display names
	private static final String DC = "Domain Component (DC):";
	private static final String SURNAME = "Surname (SURNAME):";
	private static final String GIVENNAME = "Given Name (GN):";
	private static final String SN = "Serial Number (SN):";
	private static final String E = "Email (E):";
	private static final String C = "Country (C):";
	private static final String ST = "State Name (ST):";
	private static final String L = "Locality Name (L):";
	private static final String O = "Organization Name (O):";
	private static final String OU = "Organization Unit (OU):";
	private static final String CN = "Common Name (CN):";

	private static Map<String, ASN1ObjectIdentifier> displayNameToOID = new HashMap<String, ASN1ObjectIdentifier>();
	static {
		displayNameToOID.put(CN, BCStyle.CN);
		displayNameToOID.put(OU, BCStyle.OU);
		displayNameToOID.put(O, BCStyle.O);
		displayNameToOID.put(L, BCStyle.L);
		displayNameToOID.put(ST, BCStyle.ST);
		displayNameToOID.put(C, BCStyle.C);
		displayNameToOID.put(E, BCStyle.E);

		displayNameToOID.put(SN, BCStyle.SN);
		displayNameToOID.put(GIVENNAME, BCStyle.GIVENNAME);
		displayNameToOID.put(SURNAME, BCStyle.SURNAME);
		displayNameToOID.put(DC, BCStyle.DC);
	}

	private static Map<String, String> oidToDisplayName = new HashMap<String, String>();
	static {
		oidToDisplayName.put(BCStyle.CN.getId(), CN);
		oidToDisplayName.put(BCStyle.OU.getId(), OU);
		oidToDisplayName.put(BCStyle.O.getId(), O);
		oidToDisplayName.put(BCStyle.L.getId(), L);
		oidToDisplayName.put(BCStyle.ST.getId(), ST);
		oidToDisplayName.put(BCStyle.C.getId(), C);
		oidToDisplayName.put(BCStyle.E.getId(), E);

		oidToDisplayName.put(BCStyle.SN.getId(), SN);
		oidToDisplayName.put(BCStyle.GIVENNAME.getId(), GIVENNAME);
		oidToDisplayName.put(BCStyle.SURNAME.getId(), SURNAME);
		oidToDisplayName.put(BCStyle.DC.getId(), DC);
	}

	public static String[] getDisplayNames() {
		return new String[]{ CN, OU, O, L, ST, C, E, SN, GIVENNAME, SURNAME, DC };
	}

	public static ASN1ObjectIdentifier getOidForDisplayName(String displayName) {
		return displayNameToOID.get(displayName);
	}

	public static String getDisplayNameForOid(String oid) {
		String displayName = oidToDisplayName.get(oid);
		if (displayName == null) {
			// simply use OID as display name for unknown OIDs
			displayName = oid;
		}
		return displayName;
	}
}
