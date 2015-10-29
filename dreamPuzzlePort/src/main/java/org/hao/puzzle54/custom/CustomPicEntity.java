package org.hao.puzzle54.custom;

import java.util.Date;

public class CustomPicEntity {
	private long id;
	private Date importDateTime;
	private String imageName;
	private String srcFilePath;
	
	@Override
	public boolean equals(Object obj) {
		 if (this==obj) return true;
		 if (!(obj instanceof CustomPicEntity)) return false;
		 final CustomPicEntity other=(CustomPicEntity)obj;
        return other.getId() == this.getId();
	}

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public Date getImportDateTime() {
		return importDateTime;
	}
	public void setImportDateTime(Date importDateTime) {
		this.importDateTime = importDateTime;
	}
	public String getImageName() {
		return imageName;
	}
	public void setImageName(String imageName) {
		this.imageName = imageName;
	}
	public String getSrcFilePath() {
		return srcFilePath;
	}
	public void setSrcFilePath(String srcFilePath) {
		this.srcFilePath = srcFilePath;
	}
}
