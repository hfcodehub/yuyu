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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.dom4j.Element;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smackx.packet.VCard;

import twitter4j.internal.org.json.JSONObject;


final class ChatData{		
	Chat			m_chatData;
	
	String			m_accountName;
	long			m_lastActiveTime;
	
	boolean			m_isYBClient;
	
	int				m_chatState 		= fetchChatMsg.CHAT_STATE_COMMON;
	int				m_chatState_sent	= fetchChatMsg.CHAT_STATE_COMMON;
	
	public ChatData(Chat _data,long _time){
		m_chatData = _data;
		m_accountName = convertAccount(_data.getParticipant());
		
		m_lastActiveTime = _time;
		m_isYBClient = _data.getParticipant().indexOf(fetchGTalk.fsm_ybClientSource) != -1;
	}
	
	static public String convertAccount(String _participant){
		
		int t_slash = _participant.indexOf('/');
		if(t_slash != -1){
			_participant = _participant.substring(0,t_slash).toLowerCase();
		}
		return _participant;
	}
}

final class ComposeStateMessage extends Packet{
	
	int m_composeState = 0;
	String m_to = null;
	
	public ComposeStateMessage(int _state,String _to){
		m_composeState = _state;
		m_to = _to;
	}
	
	public String toXML(){
		StringBuffer t_final = new StringBuffer();
		t_final.append("<message type=\"chat\" id=\"").append(nextID()).append("\" to=\"")
			 .append(m_to).append("\">");
		
		if(m_composeState == fetchChatMsg.CHAT_STATE_COMPOSING){
			t_final.append("<composing xmlns=\"").append(fetchGTalk.fsm_chatStateNamespace).append("\"/></message> ");
		}else{
			t_final.append("<paused xmlns=\"").append(fetchGTalk.fsm_chatStateNamespace).append("\"/></message> ");
		}
		
		return t_final.toString();
	}
	
}

final class ChatReadMessage extends Packet{
	
	String 	m_to = null;
	int		m_hashCode = 0;
	
	public ChatReadMessage(int _hashCode,String _to){
		m_hashCode = _hashCode;
		m_to = _to;
	}
	
	public String toXML(){
		StringBuffer t_final = new StringBuffer();
		t_final.append("<message type=\"chat\" id=\"").append(nextID()).append("\" to=\"")
			 .append(m_to).append("\">");
		
		t_final.append("<read xmlns=\"").append(fetchGTalk.fsm_chatStateNamespace).append("\"/>");
		t_final.append("<hashcode>").append(m_hashCode).append("</hashcode></message>");
				
		return t_final.toString();
	}
	
}

public class fetchGTalk extends fetchAccount implements RosterListener,
															ChatManagerListener,
															MessageListener{
		
	public final static String[] fsm_gtalkPhiz = 
	{
		":)",
		":-D",
		":-(",
		";-)",
		
		":P",
		"=-O",
		":kiss:",
		"8-)",
		
		":[",
		":'-(",
		":-/",
		"O:-)",
		
		":-X",
		":-$",
		":-!",
		">:o",
		
		">:-(",
		":yes:",
		":no:",
		":wait:",
		
		"@->--",
		":telephone:",
		":email:",
		":jabber:",
		
		":cake:",
		":heart:",
		":brokenheart",
		":music:",
		
		":beer:",
		":coffee:",
		":money:",
		":moon:",
		
		":sun:",
		":star:",
		":|",
		"\\m/",
		
	};
	
	public final static String[] fsm_weiboPhiz = 
	{
		"[呵呵]",
		"[嘻嘻]",
		"[失望]",
		"[太开心]",
		
		"[馋嘴]",
		"[抓狂]",
		"[爱你]",
		"[酷]",
		
		"[可爱]",
		"[泪]",
		"[懒得理你]",
		"[晕]",
		
		"[闭嘴]",
		"[嘘]",
		"[闭嘴]",
		"[怒骂]",
		
		"[怒]",
		"[good]",
		"[弱]",
		"[wait]",
		
		"[花]",
		"[电话]",
		"[邮件]",
		"[jabber]",
		
		"[蛋糕]",
		"[心]",
		"[伤心]",
		"[音乐]",
		
		"[干杯]",
		"[咖啡]",
		"[钱]",
		"[月亮]",
		
		"[太阳]",
		"[星星]",
		"[吃惊]",
		"[猪头]",
		
	};
	
	public final static String fsm_ybReadProperty = "YuchBerryRead";
	public final static String fsm_ybFile 		= "YuchBerryFile";
	public final static String fsm_ybFileType 	= "YuchBerryType";
	public final static String fsm_ybClientSource = "YuchBerry.info";
	public final static String fsm_chatStateNamespace = "http://jabber.org/protocol/chatstates";
	
	String	m_accountName 		= null;
	String	m_prefix			= null;
	String	m_headImageDir		= null;
	
	String	m_XMPPHost			= null;
	int		m_XMPPPort			= 5222;
	
	String m_password			= null;
	String m_cryptPassword		= null;
	
	XMPPConnection	m_mainConnection = null;
	
	Roster		m_roster		= null;
	ChatManager	m_chatManager	= null;
	
	Vector<fetchChatRoster>		m_chatRosterList 		= new Vector<fetchChatRoster>();
	Vector<fetchChatRoster>		m_changeChatRosterList 	= new Vector<fetchChatRoster>();
	Vector<ChatData>			m_chatList 				= new Vector<ChatData>();
	
	Vector<fetchChatMsg>		m_pushedChatMsgList 	= new Vector<fetchChatMsg>();
	Vector<fetchChatMsg>		m_markReadChatMsgList 	= new Vector<fetchChatMsg>();
	
	int[]						m_sentChatMsgList 		= new int[256];
	int							m_sentChatMsgListIdx 	= 0;
	
	private boolean findSentChatMsg(int _hashcode){
		for(int i = 0;i < m_sentChatMsgList.length;i++){
			if(m_sentChatMsgList[i] == _hashcode){
				return true;
			}
		}
		
		return false;
	}
	
	private void  appendSentChatMsg(int _hashcode) {
		if(findSentChatMsg(_hashcode)){
			return ;
		}
		
		m_sentChatMsgList[m_sentChatMsgListIdx++] = _hashcode;
		if(m_sentChatMsgListIdx >= m_sentChatMsgList.length){
			m_sentChatMsgListIdx = 0;
		}		
	}
	
	int		m_connectPresence	= -1;
	String	m_connectStatus		= null;
	
	public fetchGTalk(fetchMgr _mainMgr){
		super(_mainMgr);
	}
	
	public void InitAccount(Element _elem)throws Exception{
		
		String t_type	= fetchAccount.ReadStringAttr(_elem,"type").toLowerCase();
		
		m_accountName	= fetchAccount.ReadStringAttr(_elem,"account").toLowerCase();
		m_password		= fetchAccount.ReadStringAttr(_elem,"password");
		m_cryptPassword	= fetchAccount.ReadStringAttr(_elem,"cryptPassword");
		
		m_XMPPHost		= fetchAccount.ReadStringAttr(_elem,"xmppHost");
		m_XMPPPort		= fetchAccount.ReadIntegerAttr(_elem,"xmppPort");
				
		m_prefix				= m_mainMgr.GetPrefixString() + m_accountName + "["+t_type+"]/" ;
		m_headImageDir			= m_prefix;
		
		File t_file  = new File(GetAccountPrefix());
		if(!t_file.exists() || !t_file.isDirectory()){
			t_file.mkdir();
		}
		
		t_file  = new File(m_headImageDir);
		if(!t_file.exists() || !t_file.isDirectory()){
			t_file.mkdir();
		}
	}
	
	public String toString(){
		return m_accountName + "[gtalk]";
	}
	
	public String GetAccountName(){
		return m_accountName;
	}
	
	
	public String GetAccountPrefix(){
		return m_prefix;
	}
	
	public int getCurrChatStyle(){
		return fetchChatMsg.STYLE_GTALK;
	}
	
	public void DestroySession(){
		if(m_mainConnection != null){
			m_mainConnection.disconnect();
		}
		
		m_chatRosterList.removeAllElements();
		m_chatList.removeAllElements();
		m_changeChatRosterList.removeAllElements();
	}
	
	public void ResetSession(boolean _fullTest)throws Exception{
		
		DestroySession();
		
		if(m_mainConnection == null){
			// create the XMPP connection
			//
			String t_domain = "gmail.com";
			int t_index = 0;
			if((t_index = m_accountName.toLowerCase().indexOf("@")) != -1){
				t_domain = m_accountName.substring(t_index + 1);
			}
			
			// Create a connection to the jabber.org server on a specific port.
			//
			ConnectionConfiguration t_config;
			if(m_XMPPHost != null && m_XMPPHost.length() != 0){
				// manually xmpp server
				//
				t_config = new ConnectionConfiguration(m_XMPPHost,m_XMPPPort,t_domain);
			}else{
				t_config = new ConnectionConfiguration("talk.google.com",5222,t_domain);
			}
			
			
			if(t_domain.equals("gmail.com")){
				t_config.setSASLAuthenticationEnabled(false);
			}			
			
			m_mainConnection = new XMPPConnection(t_config);
		}		
				
		String decryptPass = decryptPassword(m_cryptPassword,m_password);
		if(decryptPass != null){
			m_password = decryptPass;
		}else{
			// haven't got the client password key
			//
			return ;
		}		
		
		m_mainConnection.connect();
				
		String t_account = null;
		int t_index = 0;
		if((t_index = m_accountName.toLowerCase().indexOf("@gmail.com")) != -1){
			t_account = m_accountName.substring(0,t_index);
		}else{
			t_account = m_accountName;
		}	
		
		m_mainConnection.login(t_account,m_password,fsm_ybClientSource + "-" + (new Random()).nextInt(1000));
		
		if(m_chatManager == null){
			m_chatManager = m_mainConnection.getChatManager();
			m_chatManager.addChatListener(this);
		}

		// retreive the new roster data
		// don't remain the former Roster(it will re-allocate by XMPPConnection.login)
		// please CAUTION!
		//
		m_roster = m_mainConnection.getRoster();
		m_roster.addRosterListener(this);
				
		synchronized (m_chatRosterList) {

			m_chatRosterList.removeAllElements();
			
			Collection<RosterEntry> t_rosterList = m_roster.getEntries();
			
			for(RosterEntry entry:t_rosterList){
				m_chatRosterList.add(convertRoster(entry));
			}
		}
		
		m_mainMgr.m_logger.LogOut(GetAccountPrefix() + " prepare OK! load " + m_chatRosterList.size() + " roster");
		
		ClientDisconnected();
	}
	

	/**
	 * yuchsbox client is connected to server
	 */
	public void ClientConnected(){
		
		if(m_mainConnection != null && m_mainConnection.isConnected()){
			
			Presence t_presence = null;
			
			if(m_connectPresence != -1 && m_connectStatus != null){
				t_presence = getPresence(m_connectPresence,m_connectStatus);
			}else{
				t_presence = new Presence(Presence.Type.available);
				t_presence.setMode(Presence.Mode.available);
			}
			
			try{
				m_mainConnection.sendPacket(t_presence);
			}catch(Exception e){
				m_mainMgr.m_logger.PrinterException(e);
			}
			
		}
	}
	
	/**
	 * client is disconnected from the server
	 */
	public void ClientDisconnected(){
		
		if(m_mainConnection != null && m_mainConnection.isConnected()){
			
			Presence t_presence = new Presence(Presence.Type.available);
			t_presence.setMode(Presence.Mode.xa);
			
			if(m_connectStatus != null){
				t_presence.setStatus(m_connectStatus);
			}
						
			try{	
				m_mainConnection.sendPacket(t_presence);
			}catch(Exception e){
				m_mainMgr.m_logger.PrinterException(e);
			}
		}
	}
	
	public void entriesAdded(Collection<String> addresses){
		
    	synchronized (m_chatRosterList) {
    		
    		addRosterListener_flag:
    		for(String acc:addresses){

    			for(fetchChatRoster roster : m_chatRosterList){
    				if(roster.getAccount().toLowerCase().equals(acc.toLowerCase())){
	    				continue addRosterListener_flag;
	    			}
	    		}
    			
    			try{
    				RosterEntry t_entry = m_roster.getEntry(acc);
        			
        			fetchChatRoster t_roster = convertRoster(t_entry);
        			m_chatRosterList.add(t_roster);
        			
        			m_changeChatRosterList.add(t_roster);    			
        			m_mainMgr.m_logger.LogOut(GetAccountPrefix() + " entriesAdded:" + acc);
    			}catch(Exception e){
    				m_mainMgr.m_logger.PrinterException(e);
    			}
    			
    		}
    	}
    	
    }
    public void entriesDeleted(Collection<String> addresses){
    	
    	synchronized (m_chatRosterList) {
    		for(String acc:addresses){
    			
    			for(fetchChatRoster roster : m_chatRosterList){
    				if(roster.getAccount().toLowerCase().equals(acc.toLowerCase())){
    					
	    				m_chatRosterList.remove(roster);	    				
	    				m_mainMgr.m_logger.LogOut(GetAccountPrefix() + " entriesDeleted:" + acc);
	    				
	    				break;
	    			}
	    		}
    		}
    		
    	}
    }
    
        
    public void entriesUpdated(Collection<String> addresses){
    	
    	synchronized (m_chatRosterList) {
    		for(String acc:addresses){
    			
    			for(fetchChatRoster roster : m_chatRosterList){
    				if(roster.getAccount().toLowerCase().equals(acc.toLowerCase())){
    					
    					try{
	    					fetchChatRoster t_updateRoster = convertRoster(m_roster.getEntry(acc));
		    				m_chatRosterList.remove(roster);
		    				
		    				m_chatRosterList.add(t_updateRoster);
		    				m_changeChatRosterList.add(t_updateRoster);
		    				
		    				m_mainMgr.m_logger.LogOut(GetAccountPrefix() + " entriesUpdated:" + acc);
    					}catch(Exception e){
    						m_mainMgr.m_logger.PrinterException(e);
    					}
	    				
	    				break;
	    			}
	    		}
    		}
    	}
    	
    }
    
    public void presenceChanged(Presence presence) {
    	
    	//That will return the presence value for the user with the highest priority and availability.
    	//
    	presence = m_roster.getPresence(presence.getFrom());
    	
    	String account = ChatData.convertAccount(presence.getFrom());
    	    	
    	//synchronized (m_chatRosterList) {
    		// don't sync m_chatRosterList because ResetSession->XMPPConnection.disconnect->...->presenceChanged
    		// CAN BE make DEAD-LOCK
    		//
    		//for(fetchChatRoster roster : m_chatRosterList){
    		//
    		//
    		for(int i = 0;i < m_chatRosterList.size();i++){
    			fetchChatRoster roster = (fetchChatRoster)m_chatRosterList.elementAt(i);
    			
    			if(roster.getAccount().toLowerCase().equals(account)){
    				boolean changed = setPresence(roster,presence);
    				
    				storeHeadImage(roster);
   				
    				if(changed){
    					
//    					m_mainMgr.m_logger.LogOut(GetAccountPrefix() + 
//								" presenceChanged:" + account + " Presence:" + presence);
    					
    					boolean t_hasBeenAdded = false;        				
    					
        				synchronized (m_changeChatRosterList) {
    						for(fetchChatRoster roster1:m_changeChatRosterList){
    							if(roster1 == roster){
    								t_hasBeenAdded = true;
    								break;
    							}
    						}
    						
    						if(!t_hasBeenAdded){
    							m_changeChatRosterList.add(roster);
    						}
    					}	
    				}
    				
    				break;
    			}
    		}
    	//}
    }
    
   
    
    public void chatCreated(Chat chat, boolean createdLocally){
    	        	        	
    	chat.addMessageListener(this);
    	
    	String t_acc = ChatData.convertAccount(chat.getParticipant());
        
        synchronized(m_chatList){
        	
			for(ChatData data:m_chatList){
				
				if(data.m_accountName.equals(t_acc)){
					
					data.m_isYBClient = chat.getParticipant().indexOf(fsm_ybClientSource) != -1;
					data.m_lastActiveTime = System.currentTimeMillis();
					
					return;
				}
			}
			
			m_chatList.add(new ChatData(chat, System.currentTimeMillis()));
		}            
        
    }
    
    long m_formerMessageTime;
    String m_formerMessage = "";
    
    public void processMessage(Chat chat, Message message){
        
    	int t_state = -1;   	
    	
		if(message.getBody() == null){

			Object t_read;
			
			if(message.getExtension("composing", fsm_chatStateNamespace) != null){
				
				// <message type='chat' id='purple296e7714' to='yuchberry@gmail.com'>
				//   <composing xmlns='http://jabber.org/protocol/chatstates'/>
				// </message>
				//			
				//System.out.println(chat.getParticipant() + " composing");
				
				t_state = fetchChatMsg.CHAT_STATE_COMPOSING;
				
			}else if(message.getExtension("paused", fsm_chatStateNamespace) != null){
				
				// <message type='chat' id='purple296e7715' to='yuchberry@gmail.com'>
				//   <paused xmlns='http://jabber.org/protocol/chatstates'/>
				// </message>
				//
				//System.out.println(chat.getParticipant() + " paused");
				
				t_state = fetchChatMsg.CHAT_STATE_COMMON;
			}else if((t_read = message.getProperty(fsm_ybReadProperty)) != null && t_read instanceof Integer){
				
				// get the property of YB read
				//
				Integer t_readHashCode = (Integer)t_read;
				
				sendChatMsgRead(ChatData.convertAccount(chat.getParticipant()),t_readHashCode.intValue());
			}
			
		}else{			
			fetchChatMsg msg = convertChat(chat, message);
			
			//get ride of duplicate message firstly
			//
			if(Math.abs(msg.getSendTime() - m_formerMessageTime) < 1000){
				if(msg.getMsg().equals(m_formerMessage)){
					return;
				}
			}
			
			m_formerMessageTime = msg.getSendTime();
			m_formerMessage = msg.getMsg();
						
			// send to client 
			//
			sendClientChatMsg(msg,true);			
						
			synchronized (m_pushedChatMsgList) {
				while(m_pushedChatMsgList.size() > 64){
					m_pushedChatMsgList.remove(0);
				}
				m_pushedChatMsgList.add(msg);
			}
						
			synchronized (m_markReadChatMsgList) {
				while(m_markReadChatMsgList.size() > 256){
					m_markReadChatMsgList.remove(0);
				}
				m_markReadChatMsgList.add(msg);	
			}			
			
			t_state = fetchChatMsg.CHAT_STATE_COMMON;
		}
		
		String t_acc  = ChatData.convertAccount(chat.getParticipant());
		
		synchronized(m_chatList){		
			
			for(ChatData data:m_chatList){
				if(data.m_accountName.equals(t_acc)){
					
					data.m_isYBClient = chat.getParticipant().indexOf(fsm_ybClientSource) != -1;
					data.m_lastActiveTime = System.currentTimeMillis();
					
					if(t_state != -1){
						data.m_chatState = t_state;
					}
					
					break;
				}
			}
		}
    }
    
    private fetchChatMsg convertChat(Chat _chat, Message message){
    	
    	fetchChatMsg t_msg = new fetchChatMsg();
    	
    	t_msg.setStyle(fetchChatMsg.STYLE_GTALK);
    	t_msg.setMsg(convertPhiz(message.getBody(),true));
    	t_msg.setSendTo(GetAccountName());
    	t_msg.setOwner(ChatData.convertAccount(_chat.getParticipant()));
    	t_msg.setSendTime(System.currentTimeMillis());
    	
    	t_msg.m_svrSentTime = t_msg.getSendTime();
    	
    	Object t_read = message.getProperty(fsm_ybReadProperty);
    	if(t_read != null && t_read instanceof Integer){
    		t_msg.setReadHashCode(((Integer)t_read).intValue());
    	}
    	
    	Object t_file = message.getProperty(fsm_ybFile);
    	if(t_file != null){
    		int t_type = ((Integer)message.getProperty(fsm_ybFileType)).intValue();
    		t_msg.setFileContent((byte[])t_file, t_type);
    	}
    	
    	return t_msg;
    }
    
    private String convertPhiz(String _org,boolean _gtalkToYB){
    	
    	if(_gtalkToYB){
    		for(int i = 0 ;i < fsm_gtalkPhiz.length;i++){
    			_org = _org.replace(fsm_gtalkPhiz[i], fsm_weiboPhiz[i]);
    		}
    	}else{
    		for(int i = 0 ;i < fsm_gtalkPhiz.length;i++){
    			_org = _org.replace(fsm_weiboPhiz[i],fsm_gtalkPhiz[i]);
    		}
    	}
    	
    	return _org;
    }
    
    private void sendChatMsgRead(String _account,int _hashCode){
    	
    	if(!m_mainMgr.isClientConnected()){
    		return ;
    	}
    	
    	try{
    		ByteArrayOutputStream os = new ByteArrayOutputStream();
    		os.write(msg_head.msgChatRead);
    		os.write(getCurrChatStyle());
    		
    		sendReceive.WriteString(os,_account,false);
    		sendReceive.WriteInt(os,_hashCode);
    		
    		m_mainMgr.SendData(os, true);

    	}catch(Exception e){
    		m_mainMgr.m_logger.PrinterException(e);
    	}
    }
    
    private boolean sendClientChatMsg(fetchChatMsg _msg,boolean _imm){
    	
    	if(!m_mainMgr.isClientConnected()){
    		return false;
    	}
    	
    	try{
    		
    		ByteArrayOutputStream os = new ByteArrayOutputStream();
    		os.write(msg_head.msgChat);
    		_msg.Output(os);
    		
    		m_mainMgr.SendData(os, _imm);
    		
    		// statistics 
    		//
    		synchronized (this) {
    			m_stat_IMRecv++;
        		m_stat_IMRecvB += os.size();
			}    		
    		    		
    		return true;
    		
    	}catch(Exception e){
    		m_mainMgr.m_logger.PrinterException(e);
    	}
    	
    	return false;
    	
    }
    
	private fetchChatRoster convertRoster(RosterEntry _entry){
		if(_entry == null){
			return null;
		}
		
		fetchChatRoster roster = new fetchChatRoster();		
		
		roster.m_smackRoster = _entry;
		
		roster.setStyle(fetchChatMsg.STYLE_GTALK);
		roster.setAccount(_entry.getUser().toLowerCase());
		roster.setOwnAccount(GetAccountName().toLowerCase());
		
		Collection<RosterGroup> t_groupList = _entry.getGroups();
		if(t_groupList == null || t_groupList.isEmpty()){
			roster.setGroup("Friends");
		}else{
			for(RosterGroup g:t_groupList){
				roster.setGroup(g.getName());
				break;
			}			
		}
		
		String t_name = _entry.getName();
		roster.setName(t_name == null?roster.getAccount():t_name);
						
		Presence t_presence = m_roster.getPresence(roster.getAccount());
		setPresence(roster, t_presence);
		
		return roster;
	}
	
	private Presence getPresence(int _presence,String _status){
		
		Presence t_sendStatus = new Presence(Presence.Type.available);
		t_sendStatus.setStatus(_status);
		
		switch(_presence){
		case fetchChatRoster.PRESENCE_AVAIL:
			t_sendStatus.setMode(Presence.Mode.available);
			break;
		case fetchChatRoster.PRESENCE_AWAY:
			t_sendStatus.setMode(Presence.Mode.away);
			break;
		case fetchChatRoster.PRESENCE_BUSY:
			t_sendStatus.setMode(Presence.Mode.dnd);
			break;
		case fetchChatRoster.PRESENCE_FAR_AWAY:
			t_sendStatus.setMode(Presence.Mode.xa);
			break;
		}
		
		return t_sendStatus;
	}
	
	private boolean setPresence(fetchChatRoster _roster,Presence _presence){
		
		int t_formerPresence = _roster.getPresence();
		String t_formerSource = _roster.getSource();
		String t_formerStatus = _roster.getStatus();
		
		_roster.setSource(_presence.getFrom());
		_roster.setStatus(_presence.getStatus());				
		
		if(_presence.isAvailable()){

			_roster.setPresence(fetchChatRoster.PRESENCE_AVAIL);
			
			if(_presence.getMode() == Presence.Mode.away){
				_roster.setPresence(fetchChatRoster.PRESENCE_AWAY);
			}else if(_presence.getMode() == Presence.Mode.dnd){
				_roster.setPresence(fetchChatRoster.PRESENCE_BUSY);
			}else if(_presence.getMode() == Presence.Mode.xa){
				_roster.setPresence(fetchChatRoster.PRESENCE_FAR_AWAY);
			}
			
		}else{
			_roster.setPresence(fetchChatRoster.PRESENCE_UNAVAIL);
		}
		
		if(t_formerPresence != _roster.getPresence()){
			return true;
		}
		
		if(t_formerSource != null  && !t_formerSource.equals(_roster.getSource())){
			
			// both are yuchbox type will be ignored 
			//
			if(t_formerSource.indexOf(fetchGTalk.fsm_ybClientSource) == -1 
			|| _roster.getSource().indexOf(fetchGTalk.fsm_ybClientSource) == -1){
				return true;
			}
		}
		
		if(t_formerStatus != null 
		&& _roster.getStatus() != null 
		&& !t_formerStatus.equals(_roster.getStatus())){
			return true;
		}
		
		return false;
	}
	
	private void storeHeadImage(fetchChatRoster _roster){
				
		File t_headImageFile_l = new File(m_headImageDir + _roster.getAccount() + "_l.jpg");
		File t_headImageFile = new File(m_headImageDir + _roster.getAccount() + ".jpg");
		
		long t_currentTime = System.currentTimeMillis();
		
		if(!t_headImageFile_l.exists() 
			|| !t_headImageFile.exists()
			|| Math.abs(t_currentTime - t_headImageFile_l.lastModified()) > 5 * 24 * 3600000){
			
			
			VCard vCard = new VCard();
			try{
				
				if(!m_mainConnection.isConnected() || !m_mainConnection.isAuthenticated()){
					return ;
					// CAN NOT reset session because some dead locked bug
					//
					//ResetSession(true);
				}
				
				vCard.load(m_mainConnection, _roster.getSource());
				
				if(vCard.getAvatar() != null){
					
					writeHeadImage(ImageIO.read(new ByteArrayInputStream(vCard.getAvatar())),
									getImageType(vCard.getAvatar()),
									t_headImageFile_l,t_headImageFile);
				}		
				
			}catch(Exception e){

				// many exception in this
				//
				
				//m_mainMgr.m_logger.PrinterException(e);	
			}
		}
				
		_roster.setHeadImageHashCode((int)t_headImageFile.length());
		_roster.setHeadImageHashCode_l((int)t_headImageFile_l.length());
	}
		
	public boolean ProcessNetworkPackage(byte[] _package)throws Exception{
		
		ByteArrayInputStream in = new ByteArrayInputStream(_package);
		
		boolean t_processed = false;
		
		final int t_head = in.read();
		switch(t_head){
			case msg_head.msgChat:
				t_processed = ProcessMsgChat(in);
				break;
			case msg_head.msgChatRosterList:
				t_processed = true;
				// fetch all IM fetchAccount roster to return
				//
				ProcessMsgRosterList();
				break;
			case msg_head.msgChatConfirm:
				t_processed = ProcessMsgChatConfirm(in);
				break;
			case msg_head.msgChatState:
				t_processed = ProcessMsgChatState(in);
				break;
			case msg_head.msgChatHeadImage:
				t_processed = ProcessChatHeadImage(in);
				break;
			case msg_head.msgChatPresence:
				ProcessChatPresence(in);
				break;
			case msg_head.msgChatEnable:
				
				m_mainMgr.setIMEnabled(sendReceive.ReadBoolean(in));
				m_mainMgr.m_logger.LogOut(GetAccountPrefix() + " client enable :"+m_mainMgr.isIMEnabled());
				
				if(!m_mainMgr.isIMEnabled()){
					DestroySession();
				}
				break;
			case msg_head.msgChatRead:
				t_processed = ProcessChatRead(in);
				break;
			case msg_head.msgChatAddRoster:
				t_processed = ProcessChatAddRoster(in);
				break;
			case msg_head.msgChatDelRoster:
				t_processed = ProcessChatDelRoster(in);
				break;
			case msg_head.msgChatAlias:
				t_processed = ProcessChatAlias(in);
				break;
		}
		
		return t_processed;
	}
	
	private boolean ProcessChatAlias(InputStream in)throws Exception {
		int t_style = in.read();
		if(t_style == getCurrChatStyle()){
			String t_ownAcccount = sendReceive.ReadString(in);
			
			if(t_ownAcccount.equals(GetAccountName())){
		
				String t_account = sendReceive.ReadString(in);
				String t_alias	= sendReceive.ReadString(in);
				
				synchronized (m_chatRosterList) {
					for(fetchChatRoster roster:m_chatRosterList){
						if(roster.getAccount().equals(t_account)){
							
							RosterPacket t_send = new RosterPacket();
							t_send.addRosterItem(new RosterPacket.Item(t_account,t_alias));
							t_send.setType(IQ.Type.SET);
							
							m_mainConnection.sendPacket(t_send);
							
							m_mainMgr.m_logger.LogOut(GetAccountPrefix() + 
									" change account<"+t_account+"> name<"+roster.getName()+">" + " to<"+t_alias+">");
							
							return true;
						}
					}		
				}
			}
		}
		
		return false;
	}
	
	private boolean ProcessChatDelRoster(InputStream in)throws Exception{
		int t_style = in.read();
		if(t_style == getCurrChatStyle()){
			String t_ownAcccount = sendReceive.ReadString(in);
						
			if(t_ownAcccount.equals(GetAccountName())){
								
				String t_account = sendReceive.ReadString(in);
				
				m_mainMgr.m_logger.LogOut(GetAccountPrefix() + " remove roster:" + t_account);
				
				synchronized (m_chatRosterList) {
					for(fetchChatRoster roster : m_chatRosterList){
						if(roster.getAccount().equals(t_account)){
							
							try{
								Roster	t_delRoster = m_mainConnection.getRoster();
								t_delRoster.removeEntry(roster.m_smackRoster);
							}catch(Exception e){
								m_mainMgr.m_logger.PrinterException(e);
							}
							
							m_chatRosterList.remove(roster);
							
							break;
						}
					}
				}
				
				synchronized (m_changeChatRosterList) {
					
					for(fetchChatRoster roster : m_changeChatRosterList){
						
						if(roster.getAccount().equals(t_account)){
							
							m_changeChatRosterList.remove(roster);
							
							break;
						}
					}
				}
				
				return true;
			}
		}
		return false;
	}
	
	private boolean ProcessChatAddRoster(InputStream in)throws Exception{
		int t_style = in.read();
		
		if(t_style == getCurrChatStyle()){
			
			String t_addr = sendReceive.ReadString(in).toLowerCase().replace("＠", "@");
			String t_name = sendReceive.ReadString(in);
			String t_group = sendReceive.ReadString(in);
			
			m_mainMgr.m_logger.LogOut(GetAccountPrefix() + " add roster:" + t_addr);
			
			try{
				Roster	t_newRoster = m_mainConnection.getRoster();
				t_newRoster.createEntry(t_addr, t_name,new String[]{ t_group});

				StringBuffer ret = new StringBuffer();
				switch (m_mainMgr.GetClientLanguage()) {
				case fetchMgr.CLIENT_LANG_ZH_S:
					ret.append("添加好友 ").append(t_name).append(" ").append(t_addr).append("成功！请等待他人同意之后自动刷新好友列表。");
					break;
				case fetchMgr.CLIENT_LANG_ZH_T:
					ret.append("添加好友 ").append(t_name).append(" ").append(t_addr).append("成功！請等待他人同意之後自動刷新好友列表。");
					break;
				default:
					ret.append("Add friend ").append(t_name).append(" ").append(t_addr).append("successfully! please wait the other agree and auto-refresh friend list.");
					break;
				}
				
				m_mainMgr.sendMsgNote(ret.toString());
				
			}catch(Exception e){
				m_mainMgr.m_logger.PrinterException(e);
			}			
			
			return true;
		}
		
		return false;
	}
	private boolean ProcessChatRead(InputStream in)throws Exception{
	
		int t_style = in.read();
		
		if(t_style == getCurrChatStyle()){
			int t_hashCode = sendReceive.ReadInt(in);
			
			synchronized (m_markReadChatMsgList) {
				for(fetchChatMsg msg:m_markReadChatMsgList){
					if(t_hashCode == msg.getReadHashCode()){
						
						if(m_mainConnection != null && m_mainConnection.isConnected()){
							
							synchronized (m_chatList) {
								for(ChatData data:m_chatList){
									if(data.m_accountName.equals(msg.getOwner())){
									
										Message t_msg = new Message();
										t_msg.setProperty(fsm_ybReadProperty,t_hashCode);
										
										data.m_chatData.sendMessage(t_msg);
										data.m_lastActiveTime = System.currentTimeMillis();
										
										break;
									}
								}
							}
							
						}
						
						m_markReadChatMsgList.remove(msg);
						return true;
					}
				}
			}			
		}
		
		return false;
	}
	
	private void ProcessChatPresence(InputStream in)throws Exception{
		
		m_connectPresence	= in.read();
		m_connectStatus		= sendReceive.ReadString(in);
		
		Presence t_sendStatus = getPresence(m_connectPresence,m_connectStatus);
		
		if(m_mainConnection != null && m_mainConnection.isConnected()){
			
			try{
				m_mainConnection.sendPacket(t_sendStatus);
			}catch(Exception e){
				m_mainMgr.m_logger.PrinterException(e);
			}
		}		
	}
	
	private boolean ProcessChatHeadImage(InputStream in)throws Exception{
		int t_style = in.read();
		
		if(t_style == getCurrChatStyle()){
			String t_imageID = sendReceive.ReadString(in);
			boolean t_largeImage = sendReceive.ReadBoolean(in);
			
			File t_headImageFile = new File(m_headImageDir + t_imageID + (t_largeImage?"_l.jpg":".jpg"));
			
			if(t_headImageFile.exists()){
				
				FileInputStream t_fileIn = new FileInputStream(t_headImageFile);
				byte[] t_fileData = null;
				try{
					t_fileData = new byte[(int)t_headImageFile.length()];
					sendReceive.ForceReadByte(t_fileIn, t_fileData, t_fileData.length);
				}finally{
					t_fileIn.close();
				}
				
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				os.write(msg_head.msgChatHeadImage);
				os.write(t_style);
				sendReceive.WriteBoolean(os, t_largeImage);
				sendReceive.WriteString(os,t_imageID,false);
				os.write(t_fileData);				
				
				m_mainMgr.SendData(os, false);
				
				m_mainMgr.m_logger.LogOut(GetAccountPrefix() + " send HeadImage:" + t_imageID);
				
				return true;
			}
		}
		
		return false;
	}
	
	private boolean ProcessMsgChatState(InputStream in)throws Exception{
		int t_style = in.read();
		if(t_style == getCurrChatStyle()){
			
			String t_ownAccount = sendReceive.ReadString(in);
			if(GetAccountName().equals(t_ownAccount)){
				
				String to = sendReceive.ReadString(in);
				
				int t_state = in.read();
								
				synchronized (m_chatList) {
					for(ChatData data:m_chatList){
						if(data.m_accountName.equals(to)){
							
							ComposeStateMessage t_msg = new ComposeStateMessage(t_state,data.m_chatData.getParticipant());
							
							m_mainConnection.sendPacket(t_msg);
							
							data.m_lastActiveTime = System.currentTimeMillis();
							
							break;
						}
					}
				}
			}
		}
		
		return false;
	}
	
	private void ProcessMsgRosterList()throws Exception{
		
		Vector<fetchChatRoster> t_rosterList = new Vector<fetchChatRoster>();
		
		for(fetchAccount t_acc: m_mainMgr.m_fetchAccount){
			if(t_acc instanceof fetchGTalk){
				((fetchGTalk)t_acc).loadRosterList(t_rosterList);
			}
		}
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		os.write(msg_head.msgChatRosterList);
		os.write(0);
		
		sendReceive.WriteInt(os,t_rosterList.size());
		
		for(fetchChatRoster roster:t_rosterList){
			roster.Outport(os,m_mainMgr.GetConnectClientVersion(),m_mainMgr.m_clientDisplayWidth);
		}
		
		m_mainMgr.m_logger.LogOut("IM send Roster " + t_rosterList.size());
		
		m_mainMgr.SendData(os, true);		
	}
	
	private void loadRosterList(Vector<fetchChatRoster> _rosterList){
		synchronized (m_chatRosterList) {
			for(fetchChatRoster roster: m_chatRosterList){
				_rosterList.add(roster);
			}
		}
	}
	
	private boolean ProcessMsgChatConfirm(InputStream in)throws Exception{
		int t_style = in.read();
				
		if(t_style == getCurrChatStyle()){
			int t_hashCode = sendReceive.ReadInt(in);
			
			synchronized (m_pushedChatMsgList) {
				
				for(fetchChatMsg msg:m_pushedChatMsgList){
					if(msg.hashCode() == t_hashCode){
						
						m_pushedChatMsgList.remove(msg);
						
						m_mainMgr.m_logger.LogOut(GetAccountPrefix() + " confirm " + t_hashCode);
						
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	private void sendMsgChatSentConfirm(long _sendTime)throws Exception{

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		os.write(msg_head.msgChatConfirm);
		sendReceive.WriteLong(os,_sendTime);
		
		m_mainMgr.SendData(os, true);
	}
	
	private boolean ProcessMsgChat(InputStream in)throws Exception{
		int t_byte = in.available();
		int t_style = in.read();
		
		if(t_style == getCurrChatStyle()){
			
			long	t_sendTime = sendReceive.ReadLong(in);
			String from = sendReceive.ReadString(in);
			
			if(from.equals(m_accountName)){

				String	to = sendReceive.ReadString(in);
				String	message = sendReceive.ReadString(in);
				int		hashcode = sendReceive.ReadInt(in);
				
				if(findSentChatMsg(hashcode)){
					sendMsgChatSentConfirm(t_sendTime);
					m_mainMgr.m_logger.LogOut(GetAccountPrefix() + " sent chat Msg :" + hashcode);
					return true;
				}
				
				int t_filelen = sendReceive.ReadInt(in);
				byte[] t_fileContent = null;
				int t_type = 0;
				boolean t_yuchClient = true;
				if(t_filelen != 0){
					if(m_mainMgr.GetConnectClientVersion() >= 19){
						t_yuchClient = sendReceive.ReadBoolean(in);
					}
					t_type = in.read();
					t_fileContent = new byte[t_filelen];
					sendReceive.ForceReadByte(in, t_fileContent, t_fileContent.length);
				}
				
				String convertText = convertPhiz(message,false);
				Message t_message = new Message();
				t_message.setProperty(fsm_ybReadProperty, hashcode);
				
				try{
					
					// check server whether is connect to GTalk server 
					// if NOT, reset to connect it
					//
					CheckFolder();
					
					ChatData t_chatData = searchChatData(to);
					
					if(t_chatData == null){
						
						// the fetchGTalk.chatCreated will add it to m_chatList
						// this function will call fetchGTalk.createChat to add the chatData to m_chatList;
						//
						m_chatManager.createChat(to, this);
					}
					
					t_chatData = searchChatData(to);
					
					if(t_chatData == null){
						throw new Exception("YuchBox fetchGtalk Internal Error: t_chatData == null");
					}
									
					if(t_fileContent != null){
						// has file content
						//
						if(m_mainMgr.GetConnectClientVersion() < 19){
							t_yuchClient = t_chatData.m_isYBClient;
						}
						
						if(t_yuchClient){
							t_message.setProperty(fsm_ybFile, t_fileContent);
							t_message.setProperty(fsm_ybFileType, t_type);
						}else{
							convertText += uploadResFile(t_fileContent, t_type);
						}
					}
					
					
					t_message.setBody(convertText);
					
					t_chatData.m_chatData.sendMessage(t_message);
					t_chatData.m_lastActiveTime = System.currentTimeMillis();					
					
					sendMsgChatSentConfirm(t_sendTime);					
					appendSentChatMsg(hashcode);
					
					//statistics
					//
					synchronized (this) {
						m_stat_IMSend++;
						m_stat_IMSendB += t_byte;
					}	
					
				}catch(Exception e){
					// send failed check the reason
					// don't throw the exception to close the connection 
					//
					m_mainMgr.m_logger.PrinterException(e);
				}							
				
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * search the chatData from the m_chatList
	 * @param _acc
	 * @return
	 */
	private ChatData searchChatData(String _acc){
		
		synchronized (m_chatList){			
			for(ChatData data:m_chatList){
				if(data.m_accountName.equals(_acc)){
					return data;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * upload the resource file(image or sound) to my file server
	 * @param _data
	 * @param _type
	 * @return
	 */
	private String uploadResFile(byte[] _data,int _type){
		
		try{
			
			String t_ext = _type==fetchChatMsg.FILE_TYPE_IMG?".jpg":".amr";
			
			HttpURLConnection httpUrlConnection = 
				(HttpURLConnection)new URL("http://d.yuchs.com/upload.php?fext="+t_ext+"&pass=" + fetchAbsWeibo.getSinaSecretKey()).openConnection();
			
			try{
				httpUrlConnection.setDoOutput(true);
		        httpUrlConnection.setRequestMethod("POST");

		        OutputStream os = httpUrlConnection.getOutputStream();
		        try{
		        	os.write(_data);
		        }finally{
		        	os.close();
		        }
		        
		        
		        BufferedReader in = new BufferedReader(new InputStreamReader(httpUrlConnection.getInputStream()));
		        try{
		        	return "\r\n" + in.readLine() + " (" + fetchMgr.getByteStr(_data.length) + ")";
		        }finally{
		        	in.close();   	
		        }
			}finally{
				httpUrlConnection.disconnect();
			}	        
	        
		}catch(Exception e){
			return "";
		}
		
	}
	
	public void CheckFolder()throws Exception{
		
		if(!m_mainMgr.isIMEnabled()){
			return;
		}
		
		if(m_mainConnection == null || !m_mainConnection.isConnected() || !m_mainConnection.isAuthenticated()){
			
			m_mainMgr.m_logger.LogOut(GetAccountPrefix() + " disconnected reset it.");
			
			ResetSession(true);
			
			ClientConnected();
		}		
	}
	
	public void PushMsg(sendReceive _sendReceive)throws Exception{

		if(!m_mainMgr.isIMEnabled() || !m_mainMgr.isClientConnected()){
			return;
		}
		
		if(m_connectPresence == -1 || m_connectStatus == null){
			m_mainMgr.SendData(new byte[]{msg_head.msgChatPresence}, false);
		}
		
		long t_currTime = System.currentTimeMillis();
		
		synchronized(m_chatList){
         	
			for(ChatData data : m_chatList){
				
				if(data.m_chatState != data.m_chatState_sent){
					data.m_chatState_sent = data.m_chatState;

					ByteArrayOutputStream os = new ByteArrayOutputStream();
					os.write(msg_head.msgChatState);
					os.write(getCurrChatStyle());
					os.write(data.m_chatState_sent);
					sendReceive.WriteString(os,GetAccountName(),false);
					sendReceive.WriteString(os,data.m_accountName,false);
					
					
					m_mainMgr.SendData(os, true);
				}
			}
		}
		
		synchronized (m_pushedChatMsgList) {
			
			for(int i = 0 ;i < m_pushedChatMsgList.size();i++){
				fetchChatMsg msg = m_pushedChatMsgList.elementAt(i);
				
				if(msg.m_svrSentTimes >= 2){
					
					m_mainMgr.m_logger.LogOut(GetAccountPrefix() + " sent msg 5 times give up!" + msg.hashCode());
					m_pushedChatMsgList.remove(i);
					
					i--;					
					continue;
				}
				
				if(Math.abs(msg.m_svrSentTime - t_currTime) > 2 * 60000) {
					
					if(sendClientChatMsg(msg,false)){
						
						msg.m_svrSentTime = t_currTime;
						
						m_mainMgr.m_logger.LogOut(GetAccountPrefix() + " sent msg again..." + msg.hashCode());
						
						msg.m_svrSentTimes++;
					}
				}
			}
		}
		
		synchronized (m_changeChatRosterList){
			for(fetchChatRoster roster:m_changeChatRosterList){
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				os.write(msg_head.msgChatRosterList);
				os.write(1);
				
				roster.Outport(os,m_mainMgr.GetConnectClientVersion(),m_mainMgr.m_clientDisplayWidth);
				
				m_mainMgr.SendData(os, false);
			}
			
			m_changeChatRosterList.removeAllElements();
		}
	}
	
	// statistics
	//
	int		m_stat_IMSend = 0;
	int		m_stat_IMRecv = 0;
	int		m_stat_IMSendB = 0;
	int		m_stat_IMRecvB = 0;
	
	public void setStatisticsIM(JSONObject _json)throws Exception{
		_json.put("Account",GetAccountName());
		_json.put("Send",m_stat_IMSend);
		_json.put("Recv",m_stat_IMRecv);
		_json.put("SendB",m_stat_IMSendB / 1024);
		_json.put("RecvB",m_stat_IMRecvB / 1024);
		
		synchronized (this) {
			m_stat_IMSend = 0;
			m_stat_IMRecv = 0;
			m_stat_IMSendB = 0;
			m_stat_IMRecvB = 0;
		}	
	}
	
	
	static boolean echo = false;
	public static void main(String[] _arg)throws Exception{
		try{
			echo = true;
			
			//Connection.DEBUG_ENABLED = true;
			
			fetchMgr t_manger = new fetchMgr();
			Logger t_logger = new Logger("");
			
			t_logger.EnabelSystemOut(true);
			t_manger.InitConnect("",t_logger);
			
			fetchGTalk t_talk = new fetchGTalk(t_manger);
			
			t_talk.m_accountName = "yuchdroid@gmail.com";
			t_talk.m_password = "hF8IBrCmBDQsKaWa";

			t_talk.m_cryptPassword = "";
						
			t_talk.ResetSession(true);
			t_talk.ResetSession(true);
			
			System.out.println("OK");
			
			Thread.sleep(500000);
			
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
		
}
