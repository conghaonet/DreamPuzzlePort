package org.hao.puzzle54;

import java.util.Comparator;
import java.util.zip.ZipEntry;

public class ComparatorPicNameInZip implements Comparator<ZipEntry> {

	@Override
	public int compare(ZipEntry lhs, ZipEntry rhs) {
		String lname = lhs.getName();
		String rname = rhs.getName();
		return lname.compareTo(rname);
	}

}
