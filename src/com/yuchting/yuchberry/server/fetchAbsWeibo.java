/**
 *  Dear developer:
 *  
 *   If you want to modify this file of project and re-publish this please visit:
 *  
 *     http://code.google.com/p/yuchberry/wiki/Project_files_header
 *     
 *   to check your responsibility and my humble proposal. Thanks!
 *   
 *  -- 
 *  Yuchs' Developer    
 *  
 *  
 *  
 *  
 *  尊敬的开发者：
 *   
 *    如果你想要修改这个项目中的文件，同时重新发布项目程序，请访问一下：
 *    
 *      http://code.google.com/p/yuchberry/wiki/Project_files_header
 *      
 *    了解你的责任，还有我卑微的建议。 谢谢！
 *   
 *  -- 
 *  语盒开发者
 *  
 */
package com.yuchting.yuchberry.server;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.dom4j.Element;

import twitter4j.internal.org.json.JSONObject;

public abstract class fetchAbsWeibo extends fetchAccount{
	
	static public boolean		sm_debug = false;
	
	byte[] m_headImageBuffer	= new byte[1024 * 10];
	
	String	m_headImageDir		= null;
		
	String	m_prefix			= null;
	
	String	m_accountName 		= null;
	
	String	m_accessToken		= null;
	String	m_secretToken		= null;

	final class fetchWeiboData{
		long					m_fromIndex = -1;
		long					m_fromIndex2 = -1; // this var for QQ out/in box
		long					m_fromIndex3 = -1;// this var for QQ out/in box timestamp
		long					m_fromIndex4 = -1;// this var for QQ out/in box timestamp
		
		Vector<fetchWeibo>		m_historyList = null;
		Vector<fetchWeibo>		m_weiboList = new Vector<fetchWeibo>();
		int						m_sum		= -1;
		int						m_counter	= -1;
		Vector<fetchWeibo>		m_WeiboComfirm	= new Vector<fetchWeibo>();
	}
	
	fetchWeiboData				m_timeline 		= new fetchWeiboData();
	fetchWeiboData				m_directMessage	= new fetchWeiboData();
	fetchWeiboData				m_atMeMessage	= new fetchWeiboData();
	fetchWeiboData				m_commentMessage= new fetchWeiboData();
	
	boolean					m_timelineHandleRefresh = false;
	boolean					m_directHandleRefresh 	= false;
	boolean					m_atMeHandleRefresh 	= false;
	boolean					m_commentHandleRefresh	= false;
		
	//! the time of weibo check folder call
	int							m_weiboDelayTimer = -1;
	
	//! check number 
	int		m_maxCheckFolderNum = 0;
	int		m_currRemainCheckFolderNum = 0;
	
	//! get rid of message weibo if first pushing
	boolean					m_firstPush				= true;
			
	public fetchAbsWeibo(fetchMgr _mainMgr){
		super(_mainMgr);
	}
	
	/**
	 * get the directory prefix of this account 
	 */
	public String GetAccountPrefix(){
		return m_prefix;
	}

	/**
	 * initialize the account to sign in 
	 * 
	 * @param _elem		: the xml element for read the attribute 
	 */
	public void InitAccount(Element _elem)throws Exception{
		m_accountName			= fetchAccount.ReadStringAttr(_elem,"account");
		String t_accessToken			= fetchAccount.ReadStringAttr(_elem,"accessToken");
		String t_secretToken			= fetchAccount.ReadStringAttr(_elem,"secretToken");
		
		setAccessTokens(t_accessToken, t_secretToken);
		
		m_timeline.m_sum		= fetchAccount.ReadIntegerAttr(_elem,"timelineSum");
		if(m_timeline.m_sum < 0){
			m_timeline.m_sum = Math.abs(m_timeline.m_sum);
			m_timelineHandleRefresh = true;
		}
		
		m_directMessage.m_sum	= fetchAccount.ReadIntegerAttr(_elem,"directMessageSum");
		if(m_directMessage.m_sum < 0){
			m_directMessage.m_sum = Math.abs(m_directMessage.m_sum);
			m_directHandleRefresh = true;
		}
		
		m_atMeMessage.m_sum		= fetchAccount.ReadIntegerAttr(_elem,"atMeSum");
		if(m_atMeMessage.m_sum < 0){
			m_atMeMessage.m_sum = Math.abs(m_atMeMessage.m_sum);
			m_atMeHandleRefresh = true;
		}
		
		m_commentMessage.m_sum	= fetchAccount.ReadIntegerAttr(_elem,"commentSum");
		if(m_commentMessage.m_sum < 0){
			m_commentMessage.m_sum = Math.abs(m_commentMessage.m_sum);
			m_commentHandleRefresh = true;
		}
				
		m_prefix				= m_accountName + "/";
		
		// create the account directory
		//
		File t_file = new File(m_mainMgr.GetPrefixString() + m_prefix);
		if(!t_file.exists()){
			t_file.mkdir();
		}
		
		m_headImageDir			= m_mainMgr.GetPrefixString() + m_prefix + "WeiboHeadImage/";
		t_file = new File(m_headImageDir);
		if(!t_file.exists()){
			t_file.mkdir();
		}
	}
		
	public String GetHeadImageFilename(final String _id){
		return GetHeadImageDir() + _id + ".png";
	}
	
	public String GetHeadImageFilename_l(final String _id){
		return GetHeadImageDir() + _id + "_l.png";
	}
	
	/**
	 * get the account name (Email address)
	 */
	public String GetAccountName(){
		return m_accountName;
	}
	
	public String toString(){
		return GetAccountName();
	}
	
	/**
	 * check the folder to find the news to push
	 */
	public synchronized void CheckFolder()throws Exception{
		
		if(!m_mainMgr.isWeiboEnabled()){
			return;
		}
		
		try{
			
			int t_maxTime = (3600 / 100) / m_mainMgr.GetPushInterval() + 1;
			if(m_weiboDelayTimer == -1 || m_weiboDelayTimer >= t_maxTime){

				synchronized(this){
					m_weiboDelayTimer = 0;
				}
								
				m_currRemainCheckFolderNum -= 4;
				if(m_currRemainCheckFolderNum > 0){
										
					// timeline handle refresh adjudge
					//
					if(!m_timelineHandleRefresh || m_timeline.m_counter == -1){
						CheckTimeline();			
					}else{
						m_currRemainCheckFolderNum--;
					}
					
					
					// at me handle refresh adjudge
					//
					if(!m_atMeHandleRefresh || m_atMeMessage.m_counter == -1){
						CheckAtMeMessage();
					}else{
						m_currRemainCheckFolderNum--;
					}
					
					// comment handle refresh adjudge
					//
					if(!m_commentHandleRefresh || m_commentMessage.m_counter == -1){
						CheckCommentMeMessage();
					}else{
						m_currRemainCheckFolderNum--;
					}
					
					// direct message handle refresh adjudge
					//
					if(!m_directHandleRefresh || m_directMessage.m_counter == -1){
						CheckDirectMessage();
					}else{
						m_currRemainCheckFolderNum--;
					}
					
				}else{
										
					ResetCheckFolderLimit();
				}
			}else{
				
				synchronized(this){
					m_weiboDelayTimer++;
				}
			}
			
		}catch(Exception e){
			
			try{
				ResetCheckFolderLimit();
			}catch(Exception ex){
				m_mainMgr.m_logger.LogOut(GetAccountName() + " ResetCheckFolderLimit Error:" + e.getMessage());
			}
			
			
			m_mainMgr.m_logger.LogOut(GetAccountName() + " current limit:" + 
							m_currRemainCheckFolderNum + "/" + m_maxCheckFolderNum + 
							" Error:" + e.getMessage());
			
			// sleep for a while
			//
			Thread.sleep(2000);
		}
	}
	
	/**
	 * destroy the session connection
	 */
	public void DestroySession(){
		
	}	
	
	
	
	/**
	 * push the message to client
	 */
	public synchronized void PushMsg(sendReceive _sendReceive)throws Exception{
				
		PrepareRepushUnconfirmMsg_impl(m_timeline);
		PrepareRepushUnconfirmMsg_impl(m_directMessage);
		PrepareRepushUnconfirmMsg_impl(m_atMeMessage);
		PrepareRepushUnconfirmMsg_impl(m_commentMessage);
		
		PushMsg_impl(m_timeline,_sendReceive);
		PushMsg_impl(m_directMessage,_sendReceive);
		PushMsg_impl(m_atMeMessage,_sendReceive);
		PushMsg_impl(m_commentMessage,_sendReceive);
		
		m_firstPush = false;
	}
	
	protected void PrepareRepushUnconfirmMsg_impl(fetchWeiboData _weiboList){
		
		boolean t_repush = true;
		
		long t_currTime = System.currentTimeMillis();
		
		for(int i = 0;i < _weiboList.m_WeiboComfirm.size();i++){
			
			fetchWeibo confirmOne = _weiboList.m_WeiboComfirm.get(i);
			
			t_repush = true;
			for(fetchWeibo existOne : _weiboList.m_weiboList){
				
				if(confirmOne.GetId() == existOne.GetId()){
					t_repush = false;
				}
			}
			
			if(t_repush){
				
				final int t_maxTimes = 5;
				
				if(Math.abs(t_currTime - confirmOne.m_sendConfirmTime) >= (5 * 60 * 1000) ){
					
					if(confirmOne.m_sendConfirmCount++ < t_maxTimes){
						
						confirmOne.m_sendConfirmTime = t_currTime;
						
						_weiboList.m_weiboList.add(confirmOne);
						
						if(sm_debug){
							m_mainMgr.m_logger.LogOut("Weibo Account<" + GetAccountName() + 
									"> prepare Weibo<" + confirmOne.GetId() + "> send again...");	
						}
						
						
					}else{
						
						if(sm_debug){
							m_mainMgr.m_logger.LogOut("Weibo Account<" + GetAccountName() + 
									"> prepare Weibo<" + confirmOne.GetId() + "> sent " + t_maxTimes + " Times , give up.");	
						}
						
					}
					
					_weiboList.m_WeiboComfirm.remove(confirmOne);
					
					i--;
				}

			}
		}
		
	}
	
	protected void PushMsg_impl(fetchWeiboData _weiboList,sendReceive _sendReceive)throws Exception{
		
		if(_weiboList.m_counter == -1 && _weiboList.m_weiboList.isEmpty()){
			return;
		}
		
		if(m_firstPush){
			m_mainMgr.m_logger.LogOut(GetAccountName() + " FirstWeibo push, remove <" + _weiboList.m_weiboList.size() + ">Weibo");
			_weiboList.m_weiboList.clear();
			return;
		}
		
		ByteArrayOutputStream t_output = new ByteArrayOutputStream();
		
		long t_currTime = System.currentTimeMillis();
		
		if(_weiboList.m_counter == -1 // client send the refresh cmd to refresh or the first call
		|| (_weiboList.m_weiboList.size() + _weiboList.m_counter >= _weiboList.m_sum) ){
			
			
			int t_weiboNum = _weiboList.m_weiboList.size();
			
			while(!_weiboList.m_weiboList.isEmpty()){				
				
				// send the fetchWeibo
				//
				fetchWeibo t_weibo = (fetchWeibo)_weiboList.m_weiboList.get(_weiboList.m_weiboList.size() - 1); 
				
				t_output.write(msg_head.msgWeibo);
				t_weibo.OutputWeibo(t_output,m_mainMgr.GetConnectClientVersion());
				
				m_mainMgr.SendData(t_output,false);
							
				// add the confirm list
				//
				_weiboList.m_weiboList.remove(t_weibo);
				_weiboList.m_WeiboComfirm.add(t_weibo);
				
				t_weibo.m_sendConfirmTime = t_currTime;
				
				// statistics
				//
				m_stat_weiboRecvB += t_output.size();
				
				t_output.reset();
			}			
	
			m_stat_weiboRecv += t_weiboNum;
			
			if(t_weiboNum != 0){
				m_mainMgr.m_logger.LogOut(GetAccountName() + " Pushed <" + t_weiboNum + ">Weibo");
			}			
			
			synchronized(this){
				_weiboList.m_counter = 0;
			}
			
		}else{
			synchronized(this){
				_weiboList.m_counter++;
			}
		}
		
	}
	
	public String GetHeadImageDir(){
		return m_headImageDir;
	}
		
	protected abstract void ResetCheckFolderLimit()throws Exception;
	protected abstract void CheckTimeline()throws Exception;
	protected abstract void CheckDirectMessage()throws Exception;
	protected abstract void CheckAtMeMessage()throws Exception;
	protected abstract void CheckCommentMeMessage()throws Exception;
	
	static byte[] sm_operateWeiboFailed = null;
	static{
		ByteArrayOutputStream t_os = new ByteArrayOutputStream();
		t_os.write(msg_head.msgNote);
		try{
			sendReceive.WriteString(t_os,"Weibo operating failed, please check server log for detail.",false);
		}catch(Exception e){}
		
		sm_operateWeiboFailed = t_os.toByteArray();
	}
	
	
	/**
	 * network package process function
	 * 
	 * @return boolean		: has been processed?
	 */
	public boolean ProcessNetworkPackage(byte[] _package)throws Exception{
		ByteArrayInputStream in = new ByteArrayInputStream(_package);
		
		boolean t_processed = false;
		
		final int t_head = in.read();
		switch(t_head){
			case msg_head.msgWeibo:
				t_processed = ProcessWeiboUpdate(in);
				break;
			case msg_head.msgWeiboConfirm:
				t_processed = ProcessWeiboConfirmed(in);
				break;
			case msg_head.msgWeiboHeadImage:
				t_processed = ProcessWeiboHeadImage(in);
				break;
			case msg_head.msgWeiboFavorite:
				t_processed = ProcessWeiboFavorite(in);
				break;
			case msg_head.msgWeiboFollowUser:
				t_processed =  ProcessWeiboFollowUser(in);
				break;
			case msg_head.msgWeiboRefresh:
				ProcessWeiboRefresh();
				break;
			case msg_head.msgWeiboDelete:
				t_processed =  ProcessWeiboDelete(in);
				break;
			case msg_head.msgWeiboUser:
				t_processed = ProcessWeiboUser(in);
				break;
			case msg_head.msgWeiboUnfollowUser :
				t_processed = ProcessWeiboUnfollowUser(in);
				break;
			case msg_head.msgWeiboRemark:
				t_processed = ProcessWeiboRemarkName(in);
				break;
			case msg_head.msgWeiboRewteet:
				t_processed = ProcessWeiboRetweet(in);
				break;
		}
		
		return t_processed;
	}
	
	protected boolean ProcessWeiboRetweet(ByteArrayInputStream in){
		return false;
	}
	
	protected boolean ProcessWeiboUser(ByteArrayInputStream in){
		int t_style = in.read();
		if(t_style == GetCurrWeiboStyle()){
			try{
				fetchWeiboUser t_user = getWeiboUser(sendReceive.ReadString(in));
				ByteArrayOutputStream t_os = new ByteArrayOutputStream();
			
				t_os.write(msg_head.msgWeiboUser);
				t_user.OutputData(t_os,m_mainMgr.GetConnectClientVersion());
				
				m_mainMgr.SendData(t_os.toByteArray(), true);
				
			}catch(Exception e){
				m_mainMgr.m_logger.PrinterException(e);
			}
			
			return true; 
		}
		
		return false;
	}
	
	protected synchronized void ProcessWeiboRefresh(){
		
		m_mainMgr.m_logger.LogOut(GetAccountName() + " Refresh Weibo All List");
		
		m_timeline.m_counter 		= -1;
		m_directMessage.m_counter 	= -1;
		m_atMeMessage.m_counter 	= -1;
		m_commentMessage.m_counter	= -1;
		
		m_weiboDelayTimer			= -1;
	}
	
	static byte[] sm_followOkPrompt = null;
	static {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		os.write(msg_head.msgWeiboPrompt);
		try{
			sendReceive.WriteString(os,"follow user OK!",false);
			sm_followOkPrompt = os.toByteArray();
		}catch(Exception e){}
	}
	
	static byte[] sm_followOkPrompt_zh = null;
	static {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		os.write(msg_head.msgWeiboPrompt);
		try{
			sendReceive.WriteString(os,"成功关注此人！",false);
			sm_followOkPrompt_zh = os.toByteArray();
		}catch(Exception e){}
	}
	
	static byte[] sm_unfollowOkPrompt = null;
	static {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		os.write(msg_head.msgWeiboPrompt);
		try{
			sendReceive.WriteString(os,"unfollow user OK!",false);
			sm_unfollowOkPrompt = os.toByteArray();
		}catch(Exception e){}
	}
	
	static byte[] sm_unfollowOkPrompt_zh = null;
	static {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		os.write(msg_head.msgWeiboPrompt);
		try{
			sendReceive.WriteString(os,"成功取消关注此人！",false);
			sm_unfollowOkPrompt_zh = os.toByteArray();
		}catch(Exception e){}
	}
	
	static byte[] sm_updateOkPrompt = null;
	static {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		os.write(msg_head.msgWeiboPrompt);
		try{
			sendReceive.WriteString(os,"update Weibo OK!",false);
			sm_updateOkPrompt = os.toByteArray();
		}catch(Exception e){}
	}
	
	static byte[] sm_updateOkPrompt_zh = null;
	static {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		os.write(msg_head.msgWeiboPrompt);
		try{
			sendReceive.WriteString(os,"发布Weibo成功！",false);
			sm_updateOkPrompt_zh = os.toByteArray();
		}catch(Exception e){}
	}
	
	
	static byte[] sm_friendRemarkOkPrompt = null;
	static {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		os.write(msg_head.msgWeiboPrompt);
		try{
			sendReceive.WriteString(os,"Update friend remark OK!",false);
			sm_friendRemarkOkPrompt = os.toByteArray();
		}catch(Exception e){}
	}
	
	static byte[] sm_friendRemarkOkPrompt_zh = null;
	static {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		os.write(msg_head.msgWeiboPrompt);
		try{
			sendReceive.WriteString(os,"更新好友备注成功成功！",false);
			sm_friendRemarkOkPrompt_zh = os.toByteArray();
		}catch(Exception e){}
	}	
	
	
	private byte[] getUpdateOKData(){
		if(m_mainMgr.GetClientLanguage() == fetchMgr.CLIENT_LANG_ZH_S){
			return sm_updateOkPrompt_zh;
		}else{
			return sm_updateOkPrompt;
		}
	}
	
	private byte[] getFollowOKData(){
		if(m_mainMgr.GetClientLanguage() == fetchMgr.CLIENT_LANG_ZH_S){
			return sm_followOkPrompt_zh;
		}else{
			return sm_followOkPrompt;
		}
	}
	
	private byte[] getUnfollowOKData(){
		if(m_mainMgr.GetClientLanguage() == fetchMgr.CLIENT_LANG_ZH_S){
			return sm_unfollowOkPrompt_zh;
		}else{
			return sm_unfollowOkPrompt;
		}
	}
	
	private byte[] getUpdateFriendRemarkOKData(){
		if(m_mainMgr.GetClientLanguage() == fetchMgr.CLIENT_LANG_ZH_S){
			return sm_friendRemarkOkPrompt_zh;
		}else{
			return sm_friendRemarkOkPrompt;
		}
	}
	
	protected boolean ProcessWeiboDelete(ByteArrayInputStream in)throws Exception{
		int t_style = in.read();
				
		if(t_style == GetCurrWeiboStyle()){
			
			long t_long = sendReceive.ReadLong(in);
			boolean t_isComment = false;
			
			if(m_mainMgr.GetConnectClientVersion() >= 8){
				t_isComment = sendReceive.ReadBoolean(in);
			}
			
			try{
				DeleteWeibo(t_long,t_isComment);
			}catch(Exception e){
				m_mainMgr.m_logger.LogOut("Delete Weibo Error:" + e.getMessage());
			}			
			
			return true;
		}
		
		return false;
	}
	
	protected boolean ProcessWeiboUpdate(ByteArrayInputStream in)throws Exception{
		
		int t_byte = in.available();
		
		int t_style = in.read();
		int t_type = in.read();	
		
		String t_text = sendReceive.ReadString(in);
		t_text = t_text.replace("＠", "@");
		
		GPSInfo t_gpsInfo = null;
		
		try{
			
			switch(t_type){
			case fetchWeibo.SEND_NEW_UPDATE_TYPE:
							
				if(in.read() != 0){
					t_gpsInfo = new GPSInfo();
					t_gpsInfo.InputData(in);
					
					// statistics
					//
					m_mainMgr.addGPSInfo(t_gpsInfo);
				}
				
				byte[] t_fileBuffer = null;
				String	t_fileType	= null;
				
				if(m_mainMgr.GetConnectClientVersion() >= 13){
					try{
						// find the uploaded file
						//
						int attach = sendReceive.ReadInt(in);
						int t_fileTypeVal = in.read();
												
						sendWeiboConfirm(attach);
						
						if(m_mainMgr.GetConnectClientVersion() >= 17){
							// find whether update in this account
							//
							String t_updateIdList = sendReceive.ReadString(in);
							
							if(t_updateIdList.length() != 0){
								String t_currAccountId = Long.toString(getCurrAccountId());
								
								if(t_updateIdList.indexOf(t_currAccountId) == -1){
									// don't update in this weibo account
									//
									m_mainMgr.m_logger.LogOut(GetAccountName() + " client[id " + t_currAccountId + "] can't update to this account.");
									return false;
								}
							}
						}						
						
						File t_attachFile = new File(m_mainMgr.GetPrefixString() + attach + "_0.satt");
						
						if(t_attachFile.exists() && !t_attachFile.isDirectory()){
							FileInputStream t_file= new FileInputStream(t_attachFile);
							try{
								
								t_fileBuffer = new byte[(int)t_attachFile.length()];
								sendReceive.ForceReadByte(t_file, t_fileBuffer, t_fileBuffer.length);
								
								switch(t_fileTypeVal){				
								case fetchWeibo.IMAGE_TYPE_GIF:
									t_fileType = "image/gif";
									break;
								case fetchWeibo.IMAGE_TYPE_JPG:
									t_fileType = "image/jpeg";
									break;
								case fetchWeibo.IMAGE_TYPE_PNG:
									t_fileType = "image/png";
									break;
								case fetchWeibo.IMAGE_TYPE_BMP:
									t_fileType = "image/jpeg";
									t_fileBuffer = convertBMPImage(t_fileBuffer);
									break;					
								}
																
							}finally{
								t_file.close();
							}
						}
					}catch(Exception e){
						m_mainMgr.m_logger.PrinterException(e);
					}
				}			
				
				UpdateStatus(t_text,t_gpsInfo,t_fileBuffer,t_fileType);
				
				if(t_fileBuffer == null){
					m_mainMgr.m_logger.LogOut(GetAccountName() + " update new weibo ");
				}else{
					m_mainMgr.m_logger.LogOut(GetAccountName() + " update new weibo with file:"+ t_fileBuffer.length +"B type:"+t_fileType);
				}				
				
				
				m_mainMgr.SendData(getUpdateOKData(), false);
				
				ProcessWeiboRefresh();
				
				//statistics
				//
				synchronized (this) {
					m_stat_weiboSend++;
					m_stat_weiboSendB += t_byte;
				}				
				
				break;
			case fetchWeibo.SEND_FORWARD_TYPE:
			case fetchWeibo.SEND_REPLY_TYPE:
				
				boolean t_public_fw = (in.read() == 1) && (t_type == fetchWeibo.SEND_FORWARD_TYPE);
				
				if(t_style == GetCurrWeiboStyle()  // same style
				|| t_public_fw){	// public forward
					
					long t_orgWeiboId = sendReceive.ReadLong(in);
					long t_commentWeiboId = 0;
					
					if(m_mainMgr.GetConnectClientVersion() >= 7){
						t_commentWeiboId = sendReceive.ReadLong(in);
					}
					
					if(in.read() != 0){
						t_gpsInfo = new GPSInfo();
						t_gpsInfo.InputData(in);
					}
					
					int t_forwardStyle = in.read();
					
					if(t_forwardStyle == 2){
						// just forward this weibo append text
						//
						String t_appendText = sendReceive.ReadString(in);
						
						if(t_style != GetCurrWeiboStyle() && !t_appendText.isEmpty()){							
							t_text += t_appendText;							
							if(t_text.length() > 140){
								t_text = t_text.substring(0,140);
							}
						}					
					}
					
					if(m_mainMgr.GetConnectClientVersion() >= 13){
						// find the uploaded file
						//
						sendWeiboConfirm(sendReceive.ReadInt(in));
					}
					
					if(t_type == fetchWeibo.SEND_FORWARD_TYPE){

						m_mainMgr.m_logger.LogOut(GetAccountName() + "comment weibo " + t_orgWeiboId + " updateStyle:" + t_forwardStyle);
						
						UpdateComment(t_style,t_text,t_orgWeiboId,t_gpsInfo,t_forwardStyle);
						
					}else{	
						
						m_mainMgr.m_logger.LogOut(GetAccountName() + " reply weibo " + t_commentWeiboId + " orgWeibo " + t_orgWeiboId + " updateStyle:" + t_forwardStyle);
						
						UpdateReply(t_text,t_commentWeiboId,t_orgWeiboId,t_gpsInfo,t_forwardStyle != 0);
						
					}
					
					m_mainMgr.SendData(getUpdateOKData(), false);
					
					if(t_forwardStyle != 0){
						ProcessWeiboRefresh();
					}
					
					//statistics
					//
					synchronized (this) {
						m_stat_weiboSend++;
						m_stat_weiboSendB += t_byte;
					}					
					
					// public the forward comment/forward
					// return false to give another weibo to process if public forward
					//
					return !t_public_fw;
				}
				
				break;
				
			case fetchWeibo.SEND_DIRECT_MSG_TYPE:
				
				if(t_style == GetCurrWeiboStyle()){

					String t_screenName = sendReceive.ReadString(in);
					if(m_mainMgr.GetConnectClientVersion() >= 13){
						// find the uploaded file
						//						
						sendWeiboConfirm(sendReceive.ReadInt(in));						
					}
					
					
					sendDirectMsg(t_screenName,t_text);				
					
					m_mainMgr.SendData(getUpdateOKData(), false);
					
					ProcessWeiboRefresh();
					
					//statistics
					//
					synchronized (this) {
						m_stat_weiboSend++;
						m_stat_weiboSendB += t_byte;
					}					
					
					return true;	
				}
				
				break;
			}
			
		}catch(Exception e){
			m_mainMgr.m_logger.LogOut(GetAccountName() + " Exception:" + e.getMessage());
			m_mainMgr.m_logger.PrinterException(e);
			
			m_mainMgr.SendData(sm_operateWeiboFailed, false);
		}
		
		return false;
	}
	
	protected void sendWeiboConfirm(int _hashCode)throws Exception{
		ByteArrayOutputStream t_os = new ByteArrayOutputStream();
		t_os.write(msg_head.msgWeiboConfirm);
		sendReceive.WriteInt(t_os,_hashCode);
		
		m_mainMgr.SendData(t_os, true);		
	}

	public abstract void setAccessTokens(String _accessToken,String _secretToken);
	protected abstract int GetCurrWeiboStyle(); 
	public abstract void UpdateStatus(String _text,GPSInfo _info,byte[] _filePic,String _fileType)throws Exception;
	protected abstract void UpdateComment(int _style,String _text,long _commentWeiboId,
											GPSInfo _info,int _updateStyle)throws Exception;
	
	protected abstract void UpdateReply(String _text,long _commentWeiboId,long _orgWeiboId,
											GPSInfo _info,boolean _updateTimeline)throws Exception;
	
	protected abstract void FavoriteWeibo(long _id)throws Exception;
	public abstract void FollowUser(String _screenName)throws Exception;
	public abstract void FollowUser(long _id)throws Exception;
	protected abstract void UnfollowUser(String _screenName)throws Exception;
	protected abstract void DeleteWeibo(long _id,boolean _isComment)throws Exception;
	protected abstract void sendDirectMsg(String _screenName,String _text)throws Exception;
	
	protected abstract void setFriendRemark(String _id,String _remark)throws Exception;
	
	/**
	 * instance of fetchAbsWeibo realize to add a WeiboAccount class to
	 * parameter list  
	 * 
	 * @param	_accList		list vector to add
	 */
	protected abstract void addWeiboAccountList(Vector<WeiboAccount> _accList);
	
	/**
	 * get the current account id
	 * @return
	 */
	protected abstract long getCurrAccountId();
	
	/**
	 * get the fetchWeiboUser data by name
	 * @param _name
	 * @return
	 * @throws Exception
	 */
	protected abstract fetchWeiboUser getWeiboUser(String _name)throws Exception;
	
	protected boolean ProcessWeiboConfirmed(ByteArrayInputStream in)throws Exception{
		
		if(in.read() == GetCurrWeiboStyle()){

			final long t_id = sendReceive.ReadLong(in);

			if(ProcessWeiboConfirmed_imple(m_timeline,t_id)){
				return true;
			}
			if(ProcessWeiboConfirmed_imple(m_directMessage,t_id)){
				return true;
			}
			if(ProcessWeiboConfirmed_imple(m_atMeMessage,t_id)){
				return true;
			}
			if(ProcessWeiboConfirmed_imple(m_commentMessage,t_id)){
				return true;
			}
		}	
		
		return false;
	}
	
	protected boolean ProcessWeiboConfirmed_imple(fetchWeiboData _weiboList,final long _id){
		boolean t_found = false;
		for(fetchWeibo confirmOne : _weiboList.m_WeiboComfirm){
			
			if(confirmOne.GetId() == _id){
				t_found = true;
				_weiboList.m_WeiboComfirm.remove(confirmOne);
				
				if(sm_debug){
					m_mainMgr.m_logger.LogOut("Weibo Account<" + GetAccountName() + "> confirm Weibo<" + _id + ">");
				}
				
				break;
			}
		}
		
		return t_found;
	}
	
	protected boolean ProcessWeiboHeadImage(ByteArrayInputStream in)throws Exception{
		
		int t_WeiboStyle = in.read();
		
		if(t_WeiboStyle == GetCurrWeiboStyle()){
			
			String t_id = null;
			if(t_WeiboStyle == fetchWeibo.QQ_WEIBO_STYLE){
				t_id = sendReceive.ReadString(in);
			}else{
				t_id = Long.toString(sendReceive.ReadLong(in));
			}
			
			boolean t_largeSize = false;
			if(m_mainMgr.GetConnectClientVersion() >= 7){
				t_largeSize = sendReceive.ReadBoolean(in);
			}
			
			File t_file = new File(t_largeSize?GetHeadImageFilename_l(t_id):GetHeadImageFilename(t_id));
			if(t_file.exists()){
				
				ByteArrayOutputStream t_os = new ByteArrayOutputStream();
				t_os.write(msg_head.msgWeiboHeadImage);
				t_os.write(GetCurrWeiboStyle());
				
				if(m_mainMgr.GetConnectClientVersion() >= 7){
					sendReceive.WriteBoolean(t_os,t_largeSize);
				}
				
				if(t_WeiboStyle == fetchWeibo.QQ_WEIBO_STYLE){
					sendReceive.WriteString(t_os,t_id,false);
				}else{
					sendReceive.WriteLong(t_os,Long.valueOf(t_id).longValue());
				}				
				
				BufferedInputStream t_read = new BufferedInputStream(new FileInputStream(t_file));
				int size = 0;
		        while((size = t_read.read(m_headImageBuffer))!= -1){
		        	t_os.write(m_headImageBuffer,0,size);
		        }	        
		        t_read.close();
		        
		        m_mainMgr.SendData(t_os, false);
		        
		        if(sm_debug){
		        	m_mainMgr.m_logger.LogOut(GetAccountName() + " send weibo head image " + t_id);
		        }
		        
		        return true;
			}			
		}
		
		return false;
	}
	
	static byte[] sm_favorOkPrompt = null;
	static {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		os.write(msg_head.msgWeiboPrompt);
		try{
			sendReceive.WriteString(os,"Favorite OK!",false);
			sm_favorOkPrompt = os.toByteArray();
		}catch(Exception e){}
	}
	
	protected boolean ProcessWeiboFavorite(ByteArrayInputStream in)throws Exception{
		
		if(in.read() == GetCurrWeiboStyle()){
			FavoriteWeibo(sendReceive.ReadLong(in));
			m_mainMgr.SendData(sm_favorOkPrompt, true);
			return true;
		}
		
		return false;
	}
	protected boolean ProcessWeiboFollowUser(ByteArrayInputStream in)throws Exception{
		
		int t_style = in.read();
		if(t_style == GetCurrWeiboStyle()){
			
			String t_id = null;
			
			if(m_mainMgr.GetConnectClientVersion() >= 10){
				
				t_id = sendReceive.ReadString(in);
				
			}else{

				if(t_style == fetchWeibo.QQ_WEIBO_STYLE){
					t_id = sendReceive.ReadString(in);
				}else{
					t_id = Long.toString(sendReceive.ReadLong(in));
				}	
			}

			m_mainMgr.m_logger.LogOut(GetAccountName() + " Follow User " + t_id);
			
			try{
				
				FollowUser(t_id);
				m_mainMgr.SendData(getFollowOKData(), false);
				
			}catch(Exception e){
				m_mainMgr.m_logger.PrinterException(e);
			}			
			
			return true;
		}
		
		return false;
	}
	
	protected boolean ProcessWeiboUnfollowUser(ByteArrayInputStream in)throws Exception{
		
		int t_style = in.read();
		if(t_style == GetCurrWeiboStyle()){
			
			String t_id = null;
			
			if(m_mainMgr.GetConnectClientVersion() >= 10){
				
				t_id = sendReceive.ReadString(in);
				
			}else{

				if(t_style == fetchWeibo.QQ_WEIBO_STYLE){
					t_id = sendReceive.ReadString(in);
				}else{
					t_id = Long.toString(sendReceive.ReadLong(in));
				}	
			}

			m_mainMgr.m_logger.LogOut(GetAccountName() + " Unfollow User " + t_id);
			
			try{
				
				UnfollowUser(t_id);
				m_mainMgr.SendData(getUnfollowOKData(), false);
				
			}catch(Exception e){
				m_mainMgr.m_logger.PrinterException(e);
			}			
			
			return true;
		}
		
		return false;
	}
	
	protected boolean ProcessWeiboRemarkName(ByteArrayInputStream in)throws Exception{
		int t_style = in.read();
		if(t_style == GetCurrWeiboStyle()){
			String t_id 		= sendReceive.ReadString(in);
			String t_remark		= sendReceive.ReadString(in);
			
			try{
				setFriendRemark(t_id,t_remark);
				m_mainMgr.SendData(getUpdateFriendRemarkOKData(), false);
				
			}catch(Exception e){
				m_mainMgr.m_logger.PrinterException(e);
			}	
			
			return true;			
		}
		
		return false;
	}
		
	protected byte[] DownloadHeadImage(URL _url,String _id){
		
		try{
			
			File t_file = new File(GetHeadImageFilename_l(_id));
			if(t_file.exists()){
				
				ByteArrayOutputStream t_os = new ByteArrayOutputStream();
				
				BufferedInputStream t_read = new BufferedInputStream(new FileInputStream(t_file));
				try{
					int size = 0;
			        while((size = t_read.read(m_headImageBuffer))!= -1){
			        	t_os.write(m_headImageBuffer,0,size);
			        }        
			     	
				}finally{
					t_read.close();
					t_read = null;
				}			
		        	        
		        return t_os.toByteArray();
			}
			
			URLConnection t_connect = _url.openConnection();
			t_connect.setConnectTimeout(10000);
		    t_connect.setReadTimeout(50000);
	        BufferedInputStream t_read = new BufferedInputStream(t_connect.getInputStream()); 
	   		try{
	   			ByteArrayOutputStream fos = new ByteArrayOutputStream();
	   			int size = 0;
		        while((size = t_read.read(m_headImageBuffer))!= -1){
		        	fos.write(m_headImageBuffer,0,size);
		        }
		    
		        return fos.toByteArray();
		        
	   		}finally{
	   			t_read.close();
	   			t_read = null;
	   		}
	   		
		}catch(Exception e){
			
			return null;
		}
	}
	protected int StoreHeadImage(URL _url,String _id){
		
		if(_url == null){
			return -1;
		}
		
		long t_currentTime = System.currentTimeMillis();
		
		final String t_filename 		= GetHeadImageFilename(_id);
		final String t_filename_l		= GetHeadImageFilename_l(_id);
		
		int t_hashCode = -1;
		
		try{
			
			File t_file_l = new File(t_filename_l);
			File t_file = new File(t_filename);
			
			boolean t_forceDownload = false;
			if(t_file_l.exists()){
				// over the modified time to refresh these
				// 
				long t_modified = t_file_l.lastModified();
				
				t_forceDownload = Math.abs(t_currentTime - t_modified) > 3 * 24 * 3600000;
			}
			
			if(!t_file_l.exists() || !t_file.exists() || t_forceDownload){
				
				// local file is NOT exist and then download from the URL
				//				
		        URLConnection t_connect = _url.openConnection();
		        t_connect.setConnectTimeout(10000);
		        t_connect.setReadTimeout(50000);
		        
		        if(t_file_l.exists() 
		        	&& t_file.exists()
		        	&& t_file_l.length() == t_connect.getContentLength()){
		        	
		        	// if the both file is exist and the image length is not changed
		        	// don't download and set the modified time 
		        	//		        	
	        		t_file_l.setLastModified(t_currentTime);
	        		
		        }else{
		        	
		        	BufferedInputStream t_read = new BufferedInputStream(t_connect.getInputStream()); 
			   		try{
			   			FileOutputStream fos = new FileOutputStream(t_file_l);
			   			int size = 0;
				        try{
					        while((size = t_read.read(m_headImageBuffer))!= -1){
					        	fos.write(m_headImageBuffer,0,size);
					        }
				        }finally{
				        	fos.flush();
					        fos.close();
					        fos = null;
				        }
			   		}finally{
			   			t_read.close();
			   			t_read = null;
			   		}
			   		
			        // scale the image and store
			        //
			        writeHeadImage(ImageIO.read(t_file_l),"PNG",t_file_l,t_file);
		        }
		        	        
			}
			
			if(m_mainMgr.m_clientDisplayWidth <= 320){
				t_hashCode = (int)t_file.length();
			}else{
				t_hashCode = (int)t_file_l.length();
			}	
				        
		}catch(Exception e){
			m_mainMgr.m_logger.PrinterException(e);
		}
		
		return t_hashCode;
	}
	
	static public byte[] convertBMPImage(byte[] _bmpBuffer)throws Exception{
		
		BufferedImage orig = ImageIO.read(new ByteArrayInputStream(_bmpBuffer));
		ByteArrayOutputStream t_os = new ByteArrayOutputStream();
		
		ImageIO.write(orig,"JPG",t_os);
		
		return t_os.toByteArray();
	}

	
	// statistics
	//
	int		m_stat_weiboSend = 0;
	int		m_stat_weiboRecv = 0;
	int		m_stat_weiboSendB = 0;
	int		m_stat_weiboRecvB = 0;

	public void setStatisticsWeibo(JSONObject _json)throws Exception{
		
		_json.put("Send",m_stat_weiboSend );
		_json.put("Recv",m_stat_weiboRecv );
		_json.put("SendB",m_stat_weiboSendB / 1024);
		_json.put("RecvB",m_stat_weiboRecvB / 1024);
		
		synchronized (this) {
			m_stat_weiboSend = 0;
			m_stat_weiboRecv = 0;
			m_stat_weiboSendB = 0;
			m_stat_weiboRecvB = 0;	
		}			
	}
	
	public static String getSinaConsumerKey(){
		try{
			Class<?> clazz = Class.forName("com.yuchting.yuchberry.server.PrivateConfig");
			Method method = clazz.getMethod("getSinaConsumerKey",new Class[0]);
			return method.invoke(null, null).toString();
		}catch(Exception e){
			return "";
		}
	}
	public static String getSinaSecretKey(){
		try{
			Class<?> clazz = Class.forName("com.yuchting.yuchberry.server.PrivateConfig");
			Method method = clazz.getMethod("getSinaSecretKey",new Class[0]);
			return method.invoke(null, null).toString();
		}catch(Exception e){
			return "";
		}
	}
	
	public static String getQQConsumerKey(){
		try{
			Class<?> clazz = Class.forName("com.yuchting.yuchberry.server.PrivateConfig");
			Method method = clazz.getMethod("getQQConsumerKey",new Class[0]);
			return method.invoke(null, null).toString();
		}catch(Exception e){
			return "";
		}
	}
	public static String getQQSecretKey(){
		try{
			Class<?> clazz = Class.forName("com.yuchting.yuchberry.server.PrivateConfig");
			Method method = clazz.getMethod("getQQSecretKey",new Class[0]);
			return method.invoke(null, null).toString();
		}catch(Exception e){
			return "";
		}
	}
	
	public static String getTWConsumerKey(){
		try{
			Class<?> clazz = Class.forName("com.yuchting.yuchberry.server.PrivateConfig");
			Method method = clazz.getMethod("getTWConsumerKey",new Class[0]);
			return method.invoke(null, null).toString();
		}catch(Exception e){
			return "";
		}
	}
	public static String getTWSecretKey(){
		try{
			Class<?> clazz = Class.forName("com.yuchting.yuchberry.server.PrivateConfig");
			Method method = clazz.getMethod("getTWSecretKey",new Class[0]);
			return method.invoke(null, null).toString();
		}catch(Exception e){
			return "";
		}
	}
}
