package org.hao.puzzle54;

public class PicsPackageEntity {
	private long id;
	private String name;
	private String code;
	private String icon;
	private int iconVersion;
	private String iconState;
	private long icongSize;
	private String iconLastModified;
	private String zip;
	private int zipVersion;
	private long zipSize;
	private String zipLastModified;
	private String state;
	private int downloadPercent;
	private int updateIndex;

	@Override
	public boolean equals(Object obj) {
		 if (this==obj) return true;
		 if (!(obj instanceof PicsPackageEntity)) return false;
		 final PicsPackageEntity other=(PicsPackageEntity)obj;
        return other.getCode().equals(this.getCode());
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public int getIconVersion() {
		return iconVersion;
	}

	public void setIconVersion(int iconVersion) {
		this.iconVersion = iconVersion;
	}

	public String getIconState() {
		return iconState;
	}

	public void setIconState(String iconState) {
		this.iconState = iconState;
	}

	public long getIcongSize() {
		return icongSize;
	}

	public void setIcongSize(long icongSize) {
		this.icongSize = icongSize;
	}

	public String getIconLastModified() {
		return iconLastModified;
	}

	public void setIconLastModified(String iconLastModified) {
		this.iconLastModified = iconLastModified;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public String getZipLastModified() {
		return zipLastModified;
	}

	public void setZipLastModified(String zipLastModified) {
		this.zipLastModified = zipLastModified;
	}

	public int getZipVersion() {
		return zipVersion;
	}

	public void setZipVersion(int zipVersion) {
		this.zipVersion = zipVersion;
	}

	public long getZipSize() {
		return zipSize;
	}

	public void setZipSize(long zipSize) {
		this.zipSize = zipSize;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public int getDownloadPercent() {
		return downloadPercent;
	}

	public void setDownloadPercent(int downloadPercent) {
		this.downloadPercent = downloadPercent;
	}
	
	public int getUpdateIndex() {
		return updateIndex;
	}

	public void setUpdateIndex(int updateIndex) {
		this.updateIndex = updateIndex;
	}

	public static final class PackageStates {
		public static final String INSTALLED = "installed";
		public static final String SCHEDULED = "scheduled";
		public static final String PAUSING = "pausing";
		public static final String DOWNLOADING = "downloading";
		public static final String NOTINSTALL = "notinstall";
		public static final String OLDVERSION = "oldversion";
		public static final String INNER = "inner"; // It's a temporary, does not save to DB.
	}
	public static final class IconStates {
		public static final String OLDVERSION = "oldversion";
		public static final String DOWNLOADED = "downloaded";
	}

}
