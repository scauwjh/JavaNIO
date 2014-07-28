package com.java.nio.beans;

import java.io.Serializable;
import java.util.Date;

public class DataPacket implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private Long id;
	
	private String content;
	
	private Date sendTime;
	
	private String toUser;
	
	private String fromUser;

	
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}

	public Date getSendTime() {
		return sendTime;
	}
	public void setSendTime(Date sendTime) {
		this.sendTime = sendTime;
	}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getToUser() {
		return toUser;
	}
	public void setToUser(String toUser) {
		this.toUser = toUser;
	}
	public String getFromUser() {
		return fromUser;
	}
	public void setFromUser(String fromUser) {
		this.fromUser = fromUser;
	}
	

}
