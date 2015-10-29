package org.hao.puzzle54;

public class SkinEntity {
	private String code;
	private String assetsFullPath;

	@Override
	public boolean equals(Object obj) {
		if (this==obj) return true;
		if (!(obj instanceof SkinEntity)) return false;
		final SkinEntity other = (SkinEntity)obj;
		return (other.getCode() != null && other.getCode().equals(this.getCode()));
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getAssetsFullPath() {
		return assetsFullPath;
	}
	public void setAssetsFullPath(String assetsFullPath) {
		this.assetsFullPath = assetsFullPath;
	}
}
