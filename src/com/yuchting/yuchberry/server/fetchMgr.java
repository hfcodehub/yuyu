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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.util.Iterator;
import java.util.Vector;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;

import twitter4j.internal.org.json.JSONArray;
import twitter4j.internal.org.json.JSONObject;

public class fetchMgr{
		
	public final static String	fsm_configFilename 			= "config.xml";
	
	public final static int		CLIENT_LANG_ZH_S = 0;
	public final static int		CLIENT_LANG_ZH_T = 1;	
	public final static int		CLIENT_LANG_EN = 2;
				
	Logger	m_logger					= null;
	
	ServerSocket m_svr 					= null;
	
	String	m_prefix					= null;   
	String	m_userPassword				= null;
		
	int		m_listenPort 				= 9716;
	
	int		m_fetchInterval 			= 10;
	
	boolean m_convertToSimpleChar 		= false;

    boolean m_userSSL					= false;
        
	int					m_clientVer		= 0;
    
    Vector<fetchAccount> m_fetchAccount 		= new Vector<fetchAccount>();
        
    //! is connected?
    berrySvrDeamon	m_currConnect		= null;
    
    boolean		m_endListenState	= false;
        
    int				m_clientLanguage	= 0;
    
    String			m_latestVersion		= null;
    
    boolean		m_hasPrompt			= false; 
    
    String			m_passwordKey		= "";
    
    boolean		m_isCheckFolderState= false;
    
    boolean		m_isWeiboEnabled	= true;
    boolean		m_isIMEnabled		= true;
    
    String			m_clientOSVersion	= "";
    int				m_clientDisplayWidth = 0;
    int				m_clientDisplayHeight = 0;
    
    public String			m_pin				= "";
    public String			m_IMEI				= "";    
    
    /**
     * the fetchMgr.CheckFolder will catch the Auth error exception to notity via send mail
     */
    boolean		m_sentAuthErrorMail	= false;    
    
    public void SetLatestVersion(String _version){
    	
    	if(m_latestVersion == null || !m_latestVersion.equals(_version)){
    		
    		m_latestVersion = _version;
        	
        	if(m_latestVersion != null && GetClientConnected() != null){
        		try{
        			m_hasPrompt = true;
        			SendNewVersionPrompt(GetClientConnected().m_sendReceive);
        			
        		}catch(Exception e){}
        	}else{
        		m_hasPrompt = true;
        	}
    	}
    }
    
    public boolean IsCheckFolderState(){
    	return m_isCheckFolderState;
    }
    
    public boolean isIMEnabled(){
    	return m_isIMEnabled;
    }
    public void setIMEnabled(boolean _enabled){
    	m_isIMEnabled = _enabled;
    }
    
    public synchronized void SetCheckFolderState(boolean _state){
    	m_isCheckFolderState = _state;
    }
    
    public String GetLatestVersion(){
    	return m_latestVersion;
    }
    
    public int GetConnectClientVersion(){
    	return m_clientVer;
    }
    
    public boolean isWeiboEnabled(){
    	return m_isWeiboEnabled;
    }
    
    public String GetClientOSVer(){
    	return m_clientOSVersion;
    }
    
    public void setClientPIN(String _pin){m_pin = _pin;}
    public String getClientPIN(){return m_pin;}
    
    public void setClientIMEI(String _imei){m_IMEI = _imei;}
    public String getClientIMEI(){return m_IMEI;}
    
    public void SendNewVersionPrompt(sendReceive _sendRecv)throws Exception{
    	if(m_hasPrompt){
    		
    		ByteArrayOutputStream t_os = new ByteArrayOutputStream();
    		t_os.write(msg_head.msgLatestVersion);
    		sendReceive.WriteString(t_os, m_latestVersion, true);
    		
    		_sendRecv.SendBufferToSvr(t_os.toByteArray(), false);
    		
    		m_hasPrompt = false;
    	}
    }
	
	public int GetServerPort(){
		return m_listenPort;
	}
	
	public String GetUserPassword(){
		return m_userPassword;
	}
	
	public boolean IsUseSSL(){
		return m_userSSL;
	}
	
	public String GetPrefixString(){
		return m_prefix;
	}
	
	public int GetPushInterval(){
		return m_fetchInterval;
	}
	
	
	
	public berrySvrDeamon GetClientConnected(){
		return	m_currConnect;
	}
	
	public int GetClientLanguage(){
		return m_clientLanguage;
	}
	
	public synchronized void SetClientConnected(berrySvrDeamon _set){
		m_currConnect = _set;
		
		if(_set == null){
			ClientDisconnected();
		}		
	}
	
	public void SendData(ByteArrayOutputStream _os,boolean _sendImm)throws Exception{
		SendData(_os.toByteArray(),_sendImm);		
	}
	
	public boolean isClientConnected(){
		
		if(m_currConnect == null){
			return false;
		}
		
		if(m_currConnect.m_sendReceive == null ){
			return false;
		}
		
		if(!m_currConnect.m_sendReceive.isAlive()){
			return false;
		}
		
		return true;
	}
	
	public void SendData(byte[] _data,boolean _sendImm)throws Exception{
		
		if(m_currConnect == null){
			throw new Exception("Client has been closed <m_currConnect == null>");
		}
		
		if(m_currConnect.m_sendReceive == null ){
			throw new Exception("Client has been closed <m_currConnect.m_sendReceive == null>");
		}
		
		if(!m_currConnect.m_sendReceive.isAlive()){
			throw new Exception("Client has been closed <!m_currConnect.m_sendReceive.isAlive()>");
		}
		
		m_currConnect.m_sendReceive.SendBufferToSvr(_data, _sendImm);		
	}

		       
	public void InitConnect(String _prefix,Logger _logger)throws Exception{
		
		m_prefix	= _prefix;
		m_logger	= _logger;
		
		DestroyAllSession();
		m_fetchAccount.removeAllElements();
		
		FileInputStream t_xmlFile = new FileInputStream(m_prefix + fsm_configFilename);
		try{
			
			SAXReader t_xmlReader = new SAXReader();
			Document t_doc = t_xmlReader.read(t_xmlFile); 
			Element t_root = t_doc.getRootElement();
			
			m_userPassword					= fetchAccount.ReadStringAttr(t_root,"userPassword");
			m_listenPort					= fetchAccount.ReadIntegerAttr(t_root,"serverPort");
			m_fetchInterval					= fetchAccount.ReadIntegerAttr(t_root,"pushInterval");
			m_userSSL						= fetchAccount.ReadBooleanAttr(t_root,"userSSL");
			m_convertToSimpleChar			= fetchAccount.ReadBooleanAttr(t_root,"convertoSimpleChar");
			
			boolean disableLog				= fetchAccount.ReadBooleanAttr(t_root, "disableLog");
			m_logger.disableLog(disableLog);
			
			m_stat_disableReport			= fetchAccount.ReadBooleanAttr(t_root, "disableReport");
			
			for( Iterator i = t_root.elementIterator("EmailAccount"); i.hasNext();){
	            Element element = (Element) i.next();
	            fetchAccount t_email = new fetchEmail(this);
	            t_email.InitAccount(element);
	            
	            m_fetchAccount.addElement(t_email);
	        }
			
			for( Iterator i = t_root.elementIterator("WeiboAccount"); i.hasNext();){
	            Element element = (Element) i.next();
	            
	            String t_type = fetchAccount.ReadStringAttr(element,"type");
	            
	            fetchAccount t_weibo = getWeiboInstance(t_type,this);
	            
	            if(t_weibo == null){
	            	continue;
	            }
	            
	            t_weibo.InitAccount(element);
	            m_fetchAccount.addElement(t_weibo);
	        }
			
			for( Iterator i = t_root.elementIterator("IMAccount"); i.hasNext();){
	            Element element = (Element) i.next();
	            
	            String t_type = fetchAccount.ReadStringAttr(element,"type");
	            
	            fetchAccount t_im = getIMInstance(t_type,this);
	            
	            if(t_im == null){
	            	continue;
	            }
	            
	            t_im.InitAccount(element);
	            m_fetchAccount.addElement(t_im);
	        }
			    	
		}catch(Exception ex){
			
			m_logger.PrinterException(ex);
			throw ex;
			
		}finally{
			t_xmlFile.close();
		}
			
	}
	
	static public fetchAccount getIMInstance(String _type,fetchMgr _mgr){
		
		fetchAccount t_im = null;
		
		if(_type.equalsIgnoreCase("gtalk") || _type.equalsIgnoreCase("xmpp")){
			t_im = new fetchGTalk(_mgr);
        }
		
		return t_im;
	}
	
	static public fetchAbsWeibo getWeiboInstance(String _type,fetchMgr _mgr){
		
		fetchAbsWeibo t_weibo = null;
		
		if(_type.equalsIgnoreCase("sina")){
        	t_weibo = new fetchSinaWeibo(_mgr);
        }else if(_type.startsWith("tw")){
        	t_weibo = new fetchTWeibo(_mgr);
        }else if(_type.equalsIgnoreCase("qq")){
        	t_weibo = new fetchQWeibo(_mgr);
        }
		
		return t_weibo;
	}
	
	public String GetPasswordKey(){
		return m_passwordKey;
	}
	
	static byte[] fsm_userPasswordErrorData = null;
	
	static{
		ByteArrayOutputStream t_os = new ByteArrayOutputStream();
		t_os.write(msg_head.msgNote);
		try{
			sendReceive.WriteString(t_os,"User Password or Port Error! Check server's log please.(用户密码或端口错误，请检查服务器日志，或重新同步)",false);
		}catch(Exception e){}
		
		fsm_userPasswordErrorData = t_os.toByteArray();
	}
	
	public sendReceive ValidateClient(Socket _s)throws Exception{
		
		sendReceive t_tmp = null;
		
		try{
			
			t_tmp = new sendReceive(_s);
			
			m_logger.LogOut("some client<"+ _s.getInetAddress().getHostAddress() +"> connecting ,waiting for auth");
			
			// wait for signIn first
			//
			_s.setSoTimeout(10000);
			_s.setKeepAlive(true);
						
			ByteArrayInputStream in = new ByteArrayInputStream(t_tmp.RecvBufferFromSvr());
									
			int t_msg_head = in.read();
			int passlen = sendReceive.ReadInt(in);
						
			if(passlen > 100 || passlen < 0){
				throw new Exception("Watch Out! User pass is too long! val:" + passlen);
			}
			
			//m_logger.LogOut("ValidateClient 0");
						
			byte[] t_buffer = new byte[passlen];
			sendReceive.ForceReadByte(in,t_buffer,passlen);
			String t_sendPass = new String(t_buffer,"UTF-8");

			if(msg_head.msgConfirm != t_msg_head 
			|| !t_sendPass.equals(m_userPassword)){
				
				t_tmp.SendBufferToSvr(fsm_userPasswordErrorData,true);
				
				throw new Exception("illeagel client<"+ _s.getInetAddress().getHostAddress() +"> sent Pass<"+t_sendPass+"> connected.");			
			}

			//m_logger.LogOut("ValidateClient 1");
			
			if((m_clientVer = sendReceive.ReadInt(in)) < 2){
				throw new Exception("error version client<"+ _s.getInetAddress().getHostAddress() +"> connected.");
			}
						
			// read the language state
			//
			m_clientLanguage = in.read();
						
			if(m_clientVer >= 3){
				String t_clientVersion = sendReceive.ReadString(in);
				
				if(GetLatestVersion() != null
				&& !GetLatestVersion().equals(t_clientVersion)){
					// send the latest version information
					//
					SendNewVersionPrompt(t_tmp);
				}
			}
			
			//m_logger.LogOut("ValidateClient 2");
			
			if(m_clientVer >= 5){
				m_passwordKey = sendReceive.ReadString(in);
			}
						
			if(m_clientVer >= 6){
				m_isWeiboEnabled = sendReceive.ReadBoolean(in);
			}
			
			String t_formerOSversion = m_clientOSVersion;
			
			if(m_clientVer >= 9){				
				m_clientOSVersion = sendReceive.ReadString(in);
				int t_size = sendReceive.ReadInt(in);
				m_clientDisplayWidth = (t_size >>> 16);
				m_clientDisplayHeight = (t_size & 0x0000ffff);
			}
			
			
			//m_logger.LogOut("ValidateClient 3");			
			
			if(m_clientVer >= 12 && (m_pin.isEmpty() || !t_formerOSversion.equals(m_clientOSVersion))){
				t_tmp.SendBufferToSvr(new byte[]{msg_head.msgDeviceInfo}, false);
			}
			
			if(m_clientVer >= 14){
				m_isIMEnabled = sendReceive.ReadBoolean(in);
			}

			//m_logger.LogOut("ValidateClient 4");
			
			// the Socket.setSoTimeout(30000) maybe make the timeout exception
			// because SokectOutputStream.write which in SendBufferToSvr_imple() maybe block socket
			//
			t_tmp.m_currSocket.setSoTimeout(30000);
			t_tmp.setThrowReadTimeoutException(false);
			
			return t_tmp;
			
		}catch(Exception _e){
			
			// time out or other problem
			//
			try{
				_s.close();
			}catch(Exception e){}
			
			m_logger.PrinterException(_e);
			t_tmp.CloseSendReceive();
			
			throw _e;
		}	
	}

	public void StartListening(boolean _fulltest){
		
		try{
			synchronized (this) {
				m_endListenState = false;
			}
			
			ResetAllAccountSession(_fulltest);
			
			m_svr = GetSocketServer(m_userPassword,m_userSSL);			
	    	
			while(!m_endListenState){
				try{
										
					Socket t_sock = m_svr.accept();
					
					synchronized (this) {
						
						try{
							
							sendReceive t_sendReceive = ValidateClient(t_sock);
							
							if(m_currConnect != null){
								m_currConnect.closeByMgr();
							}
							
							m_currConnect = new berrySvrDeamon(this,t_sock,t_sendReceive);
							
							ClientConnected();
							
						}catch(Exception e){
							t_sock.close();
							throw e;
						}
					}			
					
				}catch(Exception _e){
					m_logger.PrinterException(_e);
		    	}
	    	}			
			
			if(m_currConnect != null && m_currConnect.m_socket != null){
				m_currConnect.m_socket.close();
			}
			
			synchronized (this) {
				if(m_svr != null){
					m_svr.close();
					m_svr= null;
				}	
			}			
			
		}catch(Exception ex){
			m_logger.PrinterException(ex);
		}
		
		SetClientConnected(null);
	}
	
	private void ClientConnected(){
		for(fetchAccount acc:m_fetchAccount){
			acc.ClientConnected();
		}		
	}
	
	public void ClientDisconnected(){
		for(fetchAccount acc:m_fetchAccount){
			acc.ClientDisconnected();
		}
	}
	
	public fetchEmail getEmailPushAccount(String _accountName){
		
		_accountName = _accountName.toLowerCase();
		
		// find whether has other fetchEmail to send this Email
		//
		for(fetchAccount acc:m_fetchAccount){
			if(acc instanceof fetchEmail){
				if(_accountName.indexOf(((fetchEmail)acc).GetAccountName()) != -1){
					
					// another email account is found
					//
					return (fetchEmail)acc;
				}
			}
		}
		
		return null;
	}
	
	public synchronized void EndListening(){
		
		m_endListenState = true;
		
		try{
			
			DestroyAllSession();
			
			if(m_svr != null){
				m_svr.close();
				m_svr = null;
			}
			
		}catch(Exception e){
			if(m_logger != null){
				m_logger.PrinterException(e);
			}
		}		
	}
			
	public void DestroyAllSession()throws Exception{
		for(fetchAccount accout :m_fetchAccount){
			accout.DestroySession();
		}
	}
	
	public void ResetAllAccountSession(boolean _testAll)throws Exception{
		for(fetchAccount accout :m_fetchAccount){
			try{
				accout.ResetSession(_testAll);
			}catch(Exception e){
				Exception t_newExp = new Exception(accout.GetAccountName() + " error:" + e.getMessage());
				t_newExp.setStackTrace(e.getStackTrace());
				
				throw t_newExp;
			}
			
		}
		
//		addGPSInfo(new GPSInfo());
//		addGPSInfo(new GPSInfo());
//		m_IMEI = "这是中文测试Test";
//		sendStatictiscInfo();
	}
	
	public String GetAccountName(){
		final int t_slash = m_prefix.lastIndexOf("/");
		if(t_slash != -1){
			return m_prefix.substring(0, t_slash);
		}
		return m_prefix;
	}
	
	
	public void ProcessPackage(byte[] _package)throws Exception{
		
		ByteArrayInputStream in = new ByteArrayInputStream(_package);
				
		final int t_msg_head = in.read();
		
		switch(t_msg_head){			
			case msg_head.msgKeepLive:
				
				if(m_IMEI.startsWith("ad")){
					// android client send back 
					//
					SendData(new byte[]{msg_head.msgKeepLive}, false);
					m_logger.LogOut("pulse! and send back!");
				}else{
					m_logger.LogOut("pulse!");
				}
				break;
			case msg_head.msgSponsorList:
				ProcessSponsorList(in);
				break;
			case msg_head.msgWeiboEnable:
				m_isWeiboEnabled  = sendReceive.ReadBoolean(in);
				m_logger.LogOut("client " + (m_isWeiboEnabled?"Enable":"Disable") + " Weibo Module");
				break;
			case msg_head.msgDeviceInfo:
				if(m_clientVer >= 15){
					m_pin = sendReceive.ReadString(in);
				}else{
					m_pin = Long.toString(sendReceive.ReadLong(in));
				}			
				m_IMEI = sendReceive.ReadString(in);
				break;
			case msg_head.msgFileAttach:
				ProcessFileAttach(in);
				break;
			case msg_head.msgFileAttachSeg:
				ProcessFileAttachSeg(in);
				break;
			case msg_head.msgMailAccountList:
				
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				os.write(msg_head.msgMailAccountList);
				
				Vector<String> t_list = new Vector<String>();
				for(fetchAccount acc : m_fetchAccount){
					if(acc instanceof fetchEmail){
						t_list.add(((fetchEmail)acc).GetAccountName());
					}
				}
				sendReceive.WriteStringVector(os, t_list, false);
				SendData(os, true);
				
				break;
			case msg_head.msgWeiboAccountList:
				ProcessWeiboAccountList(in);
				break;
			default:
			{
				for(fetchAccount account :m_fetchAccount){
					if(account.ProcessNetworkPackage(_package)){
						break;
					}
				}
			}
		}		
	}
	
	public void ProcessWeiboAccountList(InputStream in)throws Exception{
		Vector<WeiboAccount> t_accList = new Vector<WeiboAccount>();
		
		for(fetchAccount account :m_fetchAccount){
			
			if(account instanceof fetchSinaWeibo
			|| account instanceof fetchTWeibo
			|| account instanceof fetchQWeibo){
				((fetchAbsWeibo)account).addWeiboAccountList(t_accList);
			}
		}
		
		if(!t_accList.isEmpty()){
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			os.write(msg_head.msgWeiboAccountList);
			sendReceive.WriteInt(os,t_accList.size());
			
			for(WeiboAccount acc:t_accList){
				acc.Output(os);
			}
			
			SendData(os, false);
		}
	}
	
	public void ProcessFileAttach(InputStream in)throws Exception{
		
		int t_hashCode = sendReceive.ReadInt(in);
		int t_num = sendReceive.ReadInt(in);
		
		for(int i = 0;i < t_num;i++){
			int t_size = sendReceive.ReadInt(in);
			
			String t_filename =  GetPrefixString() + t_hashCode + "_" + i + ".satt";
			FileOutputStream fos = new FileOutputStream(t_filename);
			
			for(int j = 0;j < t_size;j++){
				fos.write(0);
			}
			
			fos.flush();
			fos.close();
			
			m_logger.LogOut("recv msgFileAttach hashCode<"+ t_hashCode + "> num<" + t_num + "> size<" + t_size + ">");
		}
		
	}
	
	public void ProcessFileAttachSeg(InputStream in)throws Exception{
		
		final int t_hashCode = sendReceive.ReadInt(in);
				
		final int t_attachmentIdx = sendReceive.ReadInt(in);
		final int t_segIdx = sendReceive.ReadInt(in);
		final int t_segSize = sendReceive.ReadInt(in);
		
		String t_filename = GetPrefixString() + t_hashCode + "_" + t_attachmentIdx + ".satt";
		File t_file = new File(t_filename);
		
		if(t_segIdx + t_segSize > t_file.length()){
			throw new Exception("error attach" + t_filename + " idx and size");
		}
				
		byte[] t_bytes = new byte[t_segSize];
		sendReceive.ForceReadByte(in, t_bytes, t_segSize);
		
		RandomAccessFile t_fwrite = new RandomAccessFile(t_file,"rw");
		t_fwrite.seek(t_segIdx);
		t_fwrite.write(t_bytes);
		
		t_fwrite.close();
		
		if(t_segIdx + t_segSize == t_file.length()){
					
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			os.write(msg_head.msgFileAttach);
			sendReceive.WriteInt(os,t_hashCode);
			sendReceive.WriteInt(os,t_attachmentIdx);
			
			SendData(os, true);
			
			m_logger.LogOut("recv msgFileAttachSeg time<"+ t_hashCode + "> attIndex<" + t_attachmentIdx + "> beginIndex<" + t_segIdx + "> size:" + t_segSize);
		}
		
	}
	
	public fetchEmail findEmailAcc(){
		for(fetchAccount account :m_fetchAccount){
			
			if(account instanceof fetchEmail){
				return ((fetchEmail)account);
			}
		}
		
		return null;
	}
	
	/**
	 * login the email account and send the mail to the email account INBOX 
	 * @param _subject
	 * @param _contain
	 * @param _from
	 * @return
	 */
	public boolean SendImmMail(String _subject ,String _contain,String _from){

		fetchEmail t_acc = findEmailAcc();
		if(t_acc != null){
			t_acc.SendImmMail(_subject, _contain, _from);
			return true;
		}
		
		return false;
	}
	
	/**
	 * send client message to prompt(use msgMail protocol) without login email account
	 * @param _subject
	 * @param _contain
	 * @param _from
	 * @return
	 */
	public void SendPromptFakeMail(String _subject ,String _contain,String _from,String _to){
		
		m_logger.LogOut("SendPromptFakeMail " + _subject);
		
		fetchMail t_mail = new fetchMail(false);
		
		t_mail.SetSubject(_subject);
		t_mail.SetContain(_contain);
		t_mail.SetFromVect(new String[]{_from});
		t_mail.SetSendToVect(new String[]{_to});
		
		fetchEmail t_acc = findEmailAcc();
		if(t_acc != null){
			// add this mail to push list without login email account
			//
			t_acc.addPushListMsg(t_mail);
			
		}else{
			// can't find the fetchEmail account
			// it probably can't happen
			//
			try{		
				fetchEmail.sendMailData(new ByteArrayOutputStream(), t_mail, this);			
			}catch(Exception e){
				m_logger.PrinterException(e);
			}					
		}
	}
	
	public void CheckAccountFolders(){
		
		boolean t_noError = true;
		
		for(fetchAccount account :m_fetchAccount){			
			try{
							
				account.CheckFolder();
								
			}catch(Exception e){
				
				t_noError = false;
				
				if(e instanceof javax.mail.AuthenticationFailedException){
					
					if((account instanceof fetchEmail) && ((fetchEmail)account).isGmail()
					&& e.getMessage() != null && e.getMessage().length() != 0){
						
						if(!m_sentAuthErrorMail){
							// first meet auth error to send prompt mail to client
							//
							String t_subject;
							StringBuffer t_text = new StringBuffer();
							
							switch (GetClientLanguage()) {
								case fetchMgr.CLIENT_LANG_ZH_S:
									t_subject = "语盒服务器提醒";
									t_text.append("语盒服务器在访问您的邮箱 ").append(account.GetAccountName())
											.append(" 遇到授权问题，邮局服务器返回错误：\n\n").append(e.getMessage())
											.append("\n\n如果无法正常收信和发信，可能需要网页登录一下邮箱，按服务器返回的提示进行一些操作。如果自行操作无效，请联系语盒 yuchberry@gmail.com ");
											
									break;
								case fetchMgr.CLIENT_LANG_ZH_T:
									t_subject = "語盒服務器提醒";
									t_text.append("語盒服務器在訪問您的郵局 ").append(account.GetAccountName())
											.append(" 遇到授權問題，服務器返回錯誤：\n\n").append(e.getMessage())
											.append("\n\n如果無法正常收信和發信，可能需要使用瀏覽器登錄郵箱，安服務器返回的提示進行一些操作。如果自行操作無效，請聯繫語盒 yuchberry@gmail.com ");
									break;
								default:
									t_subject = "YuchBox Prompt";
									t_text.append("We meet some error when visit your account: ").append(account.GetAccountName())
											.append(" the mail server show：\n\n").append(e.getMessage())
											.append("\n\n You need login your email account via web browser and unlock something when can NOT receive mail or send mail, " +
													"if you do not know how, please contact us yuchberry@gmail.com.\n Thanks!");
											
									break;
							}
							
							SendPromptFakeMail(t_subject, t_text.toString(), "yuchberry@gmail.com",account.GetAccountName());
							
							m_sentAuthErrorMail = true;
						}
					}
					
				}
				
				m_logger.PrinterException(e);
									
				try{
					Thread.sleep(10000);
				}catch(Exception ex){
					m_logger.LogOut(account.GetAccountName() + " checkfolder interpret.");
					return;
				}
				
				try{
					
					account.ResetSession(false);
					account.CheckFolder();
										
				}catch(Exception ex){
					m_logger.PrinterException(ex);
					m_logger.LogOut(account.GetAccountName() + " failed to ResetSession/CheckFolder again!");
				}
			}
		}
		
		
		if(t_noError){
			// clear the sent auth error mail flag when check all account successfully
			// to wait next auth error happen
			//
			m_sentAuthErrorMail = false;
		}
	}
		
	public void Push(sendReceive _send){
						
		for(fetchAccount account :m_fetchAccount){					
			try{				
				account.PushMsg(_send);
			}catch(Exception e){
				m_logger.PrinterException(e);
			}			
		}
	}
	
	public void sendMsgNote(String _note){
		ByteArrayOutputStream t_os = new ByteArrayOutputStream();
		t_os.write(msg_head.msgNote);
		
		try{
			sendReceive.WriteString(t_os,_note,false);
			
			if(isClientConnected()){
				SendData(t_os, false);
			}
		}catch(Exception e){}	
	}
		
	private void ProcessSponsorList(ByteArrayInputStream _in){
		try{
			
			// read the google code host page
			//
			final String ft_URL = new String("http://code.google.com/p/yuchberry/wiki/Thanks_sheet");
			String charSet = "utf-8";
			StringBuffer t_stringBuffer = new StringBuffer();
			
			URL url = new URL(ft_URL);
			URLConnection con = url.openConnection();
			con.setConnectTimeout(10000);
			con.setReadTimeout(50000);
			con.setAllowUserInteraction(false);
			con.connect();
			   
			String type = URLConnection.guessContentTypeFromStream(con.getInputStream());
			
			if (type == null)
				type = con.getContentType();
			
			if (type == null || type.trim().length() == 0 || type.trim().indexOf("text/html") < 0){
				return ;
			}
			
			if(type.indexOf("charset=") > 0){
				charSet = type.substring(type.indexOf("charset=") + 8);
			}
						
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), charSet));
			
			String temp;
			while ((temp = in.readLine()) != null) {
				t_stringBuffer.append(temp+"\n");
			}
			in.close();
	        	        
	        String t_line = fetchMgr.ParseHTMLText(t_stringBuffer.toString(),false);
	        
	        final int t_start = t_line.indexOf("##@##");
	        final int t_end = t_line.indexOf("@@#@@");
	        if(t_start != -1 && t_end != -1){
	        	t_line = t_line.substring(t_start + 5 ,t_end);
	        }
	        t_line = t_line.replace("&para;","");
	        
	        ByteArrayOutputStream t_os = new ByteArrayOutputStream();
	        t_os.write(msg_head.msgSponsorList);
	        sendReceive.WriteString(t_os,t_line,m_convertToSimpleChar);
	        
	        SendData(t_os,true);
	        
		}catch(Exception _e){}
	}

	public static String ReadSimpleIniFile(File _file,String _decodeName,Vector<String> _lines)throws Exception{
		
		String t_ret = new String();
		
		if(_lines != null){
			_lines.removeAllElements();
		}
		
		if(_file.exists()){
			BufferedReader in = new BufferedReader(
									new InputStreamReader(
											new FileInputStream(_file),_decodeName));
			try{
				StringBuffer t_stringBuffer = new StringBuffer();
				String t_line = null;
				
				boolean t_firstLine = true;
				while((t_line = in.readLine()) != null){
					
					if(t_firstLine && _decodeName.equals("UTF-8")){
						
						byte[] t_bytes = t_line.getBytes("UTF-8");
						
						// BOM process
						//
						if(t_bytes.length >= 3
							&& t_bytes[0] == -17 && t_bytes[1] == -69 && t_bytes[2] == -65){						
							
							if(t_bytes.length == 3){
								t_line = "";
							}else{
								t_line = new String(t_bytes,3,t_bytes.length - 3,"UTF-8");
							}
																
						}
					}
					
					t_firstLine = false;
									
					if(!t_line.startsWith("#")){
						t_stringBuffer.append(t_line + "\n");
						
						if(_lines != null){
							_lines.addElement(t_line);
						}
					}
				}
				
				t_ret = t_stringBuffer.toString();
			}finally{
				in.close();
			}
		}
		
		return t_ret;
	}

	public static String ReadSimpleIniFile(String _file,String _decodeName,Vector<String> _lines)throws Exception{
		return ReadSimpleIniFile(new File(_file),_decodeName,_lines);		
	}
	
		
	public ServerSocket GetSocketServer(String _userPassword,boolean _ssl)throws Exception{
		
		if(_ssl){
			String	key				= m_prefix + "YuchBerrySvr.key";  
			
			char[] keyStorePass		= _userPassword.toCharArray();
			char[] keyPassword		= _userPassword.toCharArray();
			
			KeyStore ks				= KeyStore.getInstance(KeyStore.getDefaultType());
			
			ks.load(new FileInputStream(key),keyStorePass);
			
			KeyManagerFactory kmf = null;
			
			try{
				kmf = KeyManagerFactory.getInstance("SunX509");
			}catch(Exception e){
				kmf = KeyManagerFactory.getInstance("IbmX509");
			}
			
			kmf.init(ks,keyPassword);
			
			SSLContext sslContext = SSLContext.getInstance("SSLv3");
			sslContext.init(kmf.getKeyManagers(),null,null);
			  
			SSLServerSocketFactory factory=sslContext.getServerSocketFactory();
			
			SSLServerSocket t_socket = (SSLServerSocket)factory.createServerSocket(m_listenPort); 
			//t_socket.setNeedClientAuth(true);
			
			return t_socket;
			
		}else{
			return new ServerSocket(m_listenPort);
		}		  
	}
	
	static public String ParseHTMLText(String _html,boolean _shortURL){
		StringBuffer t_text = new StringBuffer();	
		StringBuffer t_shorterText = new StringBuffer();
		
		boolean t_shorted = false;
		try{
			Parser parser = new Parser(_html,null);
			parser.setEncoding("UTF-8");
			
	        NodeList list = parser.parse(new  NodeFilter() {
	        								public boolean accept(Node node) {
	        									return node instanceof TextNode || node instanceof LinkTag ;
	        								}
	        							});
	        
	        Node[] nodes = list.toNodeArray();

            for (int i = 1; i < nodes.length; i++){
                Node nextNode = nodes[i];

                if (nextNode instanceof TextNode){
                    TextNode textnode = (TextNode) nextNode;
                    t_text.append(textnode.getText());
                    
                    if(textnode.isWhiteSpace()){
                    	t_text.append("\n");
                    }                    
                    
                }else{
                	
                	LinkTag link = (LinkTag)nextNode;
                	if(_shortURL){
                		t_text.append(GetShortURL(link.getLink()));
                		t_shorted = true;
                	}else{
                		t_text.append(link.getLink());
                	}
                	
                	t_text.append("\n");
                }              
            }            
            
            int t_emptyCharCounter = 0;
            
            for(int i = 0;i < t_text.length();i++){
            	final char t_char = t_text.charAt(i);
            	if(IsEmptyChar(t_char)){
            		if(t_emptyCharCounter++ < 2){
            			t_shorterText.append(t_char);
            		}
            	}else{
            		t_emptyCharCounter = 0;
            		t_shorterText.append(t_char);
            	}            	
            }
            
		}catch(Exception _e	){}
		
		String t_result = t_shorterText.toString();
		
		t_result = t_result.replace("&lt;", "<");
		t_result = t_result.replace("&gt;", ">");		
		t_result = t_result.replace("&apos;", "'");
		t_result = t_result.replace("&quot;", "\"");
		t_result = t_result.replace("&nbsp;", " ");	
		t_result = t_result.replace("&copy;", "©");
		t_result = t_result.replace("&reg;", "®");	
		t_result = t_result.replace("&amp;", "&");
		
		t_result = t_result.replaceAll("<!--(?s).*?-->","");
		
		if(t_shorted){
			return "[yuchberry prompt:some URL would be shorted]\n" + t_result;
		}else{
			return t_result;
		}
		
	}
	
	static public boolean IsEmptyChar(final char _char){
		return _char == ' ' || _char == '\n' || _char == '\t' || _char == '\r';
	}
	
	static public boolean IsEmptyLine(final String _string){
		for(int i = 0;i < _string.length();i++){
			if(!IsEmptyChar(_string.charAt(i))){
				return false;
			}
		}
		
		return true;
	}
	
	public static String GetShortURL(String _longURL){
	
		if(_longURL.toLowerCase().startsWith("http")){
			
			try{
				URL is_gd = new URL("http://is.gd/api.php?longurl=" + _longURL);
				
		        URLConnection yc = is_gd.openConnection();
		        yc.setConnectTimeout(10000);
		        yc.setReadTimeout(50000);
		        BufferedReader in = new BufferedReader(
		                                new InputStreamReader(yc.getInputStream()));
		        
		        String inputLine = in.readLine();	        
		        in.close();
		        
		        return (inputLine != null && inputLine.length() < _longURL.length()) ? inputLine:_longURL ;
		        
			}catch(Exception _e){
				_e.printStackTrace();
			}
		}	
		
		return _longURL;
		
        
	}
	
	static public String getByteStr(long _byte){
		if(_byte < 1000){
			return "" + _byte + "B";
		}else if(_byte >= 1000 && _byte < 1000000){
			return "" + (_byte / 1000) + "." + (_byte % 1000 / 100)+ "KB";
		}else{
			return "" + (_byte / (1000000)) + "." + ((_byte / 1000) % 1000 / 100) + "MB";
		}
	}
	
		
	// statistics
	//
	boolean	m_stat_disableReport = false;
	
	String 				m_stat_clientID = "";
	Vector<GPSInfo>		m_stat_clientGEO = new Vector<GPSInfo>();

	
	public void addGPSInfo(GPSInfo _info){
		_info.m_time = System.currentTimeMillis();
		m_stat_clientGEO.add(_info);
	}
		
	public void sendStatictiscInfo(){
		if(m_stat_disableReport){
			return ;
		}
		
		try{
			JSONObject t_json = new JSONObject();
			t_json.put("ID","" + m_IMEI + "-" + m_pin);
			t_json.put("Time",System.currentTimeMillis() / 1000);
			
			JSONArray t_geoList = new JSONArray();
			
			synchronized (m_stat_clientGEO) {
				for(GPSInfo gps: m_stat_clientGEO){
					JSONObject t_gps = new JSONObject();
					t_gps.put("x",gps.m_latitude);
					t_gps.put("y",gps.m_longitude);
					t_gps.put("t",(gps.m_time / 1000));
					
					t_geoList.put(t_gps);
				}
				t_json.put("GEO",t_geoList);
				
				m_stat_clientGEO.clear();
			}			
			
			JSONArray t_email = new JSONArray();		
			JSONArray t_sinaWeibo = new JSONArray();
			JSONArray t_qqWeibo = new JSONArray();
			JSONArray t_tWeibo = new JSONArray();
			
			JSONArray t_IMGTalk = new JSONArray();
			
			for(fetchAccount acc:m_fetchAccount){
				if(acc instanceof fetchEmail){
					
					fetchEmail t_accE = (fetchEmail)acc;
				
					JSONObject t_obj = new JSONObject();
					t_accE.fillStatJSON(t_obj);
					
					t_email.put(t_obj);
					
				}else if(acc instanceof fetchSinaWeibo){
					fetchSinaWeibo t_accSina = (fetchSinaWeibo)acc;
					JSONObject t_sina = new JSONObject();
					
					t_sina.put("Account",t_accSina.m_userself.getId());
					
					t_accSina.setStatisticsWeibo(t_sina);
					
					t_sinaWeibo.put(t_sina);
					
				}else if(acc instanceof fetchQWeibo){
					fetchQWeibo t_accQQ = (fetchQWeibo)acc;
					JSONObject t_qq = new JSONObject();
					
					t_qq.put("Account",t_accQQ.m_userself.getScreenName());
					t_accQQ.setStatisticsWeibo(t_qq);
					
					t_qqWeibo.put(t_qq);
					
				}else if(acc instanceof fetchTWeibo){
					fetchTWeibo t_accT = (fetchTWeibo)acc;
					JSONObject t_T = new JSONObject();
					
					t_T.put("Account",t_accT.m_userself.getId());
					t_accT.setStatisticsWeibo( t_T);
					
					t_tWeibo.put(t_T);
					
				}else if(acc instanceof fetchGTalk){
					fetchGTalk t_accG = (fetchGTalk)acc;
					JSONObject t_G = new JSONObject();
					
					t_accG.setStatisticsIM(t_G);
					
					t_IMGTalk.put(t_G);
				}
			}
						
			t_json.put("Email",t_email);
			t_json.put("WeiboSina",t_sinaWeibo);
			t_json.put("WeiboQQ",t_qqWeibo);
			t_json.put("WeiboT",t_tWeibo);
			t_json.put("IMGtalk",t_IMGTalk);
			
			//System.out.println(t_json);
			
			postURLStat(t_json.toString());
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
		
	final static  private String fsm_statURL = "http://bbs.yuchs.com/ym/apps/interface/ys.php";
	
	private static void postURLStat(String _info){
		
		try{
			_info = new String(Base64.encodeBase64(_info.getBytes("UTF-8")));			
		}catch(Exception e){
			return ;
		}		
					
		int t_retryTime = 0;
		
		while(t_retryTime++ < 3){
			
			try{
				
				URL url = new URL(fsm_statURL);
				HttpURLConnection con = (HttpURLConnection)url.openConnection();

				con.setConnectTimeout(10000);
				con.setReadTimeout(20000);
				con.setDoInput(true);
				con.setRequestMethod("POST");
				con.setDoOutput(true);
						
				String t_params = "s=" + URLEncoder.encode(_info,"UTF-8");
				byte[] bytes = t_params.getBytes("UTF-8");
				
				con.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
				con.setRequestProperty("Content-Length",Integer.toString(bytes.length));			
				
				OutputStream t_os = con.getOutputStream();
						
				t_os.write(bytes);
				t_os.flush();
				t_os.close();			
				
				String t_response = null;
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
				try{
					StringBuffer t_stringBuffer = new StringBuffer();
					
					String temp;
					while ((temp = in.readLine()) != null) {
						t_stringBuffer.append(temp+"\n");
					}
					
					t_response = t_stringBuffer.toString();
				}finally{
					in.close();	
				}
				
				if(t_response.indexOf("OK") != -1){
					break;
				}
								
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	
}