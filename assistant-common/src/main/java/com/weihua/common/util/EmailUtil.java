package com.weihua.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.mail.search.AndTerm;
import javax.mail.search.FromStringTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;

import org.apache.log4j.Logger;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.sun.mail.pop3.POP3Message;

public class EmailUtil {

	private static Logger LOGGER = Logger.getLogger(EmailUtil.class);
	private static Properties props = System.getProperties();
	public static Map<String, String> configs = new HashMap<String, String>();

	static {
		configs.put(Constant.EMAIL_UTIL_DEFAULT_KEY_SMTP, "mail.smtp.host");
		configs.put(Constant.EMAIL_UTIL_DEFAULT_VALUE_SMTP, "smtp.163.com");
		configs.put(Constant.EMAIL_UTIL_DEFAULT_SEND_NICKNAME, "MyAssistant");
		configs.put(Constant.EMAIL_UTIL_DEFAULT_MAIL_HOST, "pop3.163.com");
		configs.put(Constant.EMAIL_UTIL_DEFAULT_MAIL_PORT, "110");
		configs.put(Constant.EMAIL_UTIL_DEFAULT_MAIL_TYPE, "pop3");
		configs.put(Constant.EMAIL_UTIL_DEFAULT_MAIL_AUTH, "true");
		configs.put(Constant.EMAIL_UTIL_DEFAULT_MAIL_ATTACH_PATH, "upload/recMail/");
		configs.put(Constant.EMAIL_UTIL_DEFAULT_LOG_SPERATOR, "\n");
		props.put("mail.smtp.auth", "true");
	}

	public static void init(String dataEmailUser, String dataEmailUserPwd) {
		configs.put(Constant.EMAIL_UTIL_DEFAULT_SEND_USER, dataEmailUser);
		configs.put(Constant.EMAIL_UTIL_DEFAULT_SEND_PWD, dataEmailUserPwd);
		configs.put(Constant.EMAIL_UTIL_DEFAULT_RECEIVE_USER, dataEmailUser);
	}

	public static void send(final SendInfo sendInfo) {
		OverFrequencyProtector.protect(OverFrequencyProtector.ProtectType.SEND);

		try {
			checkSendInfo(sendInfo);
			props.setProperty(sendInfo.getKeySmtp(), sendInfo.getValueSmtp());

			Session session = Session.getDefaultInstance(props, new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(sendInfo.getSendUname(), sendInfo.getSendPwd());
				}
			});

			session.setDebug(false);

			MimeMessage message = new MimeMessage(session);

			String nickName = javax.mail.internet.MimeUtility.encodeText(sendInfo.getSendNickName());
			InternetAddress from = new InternetAddress(nickName + " <" + sendInfo.getSendUser() + ">");
			message.setFrom(from);

			String receiveUser = sendInfo.getReceiveUser().replaceAll("[^0-9@a-zA-Z\\_\\.\\;]*", "");
			if (receiveUser.contains(";")) {
				String[] receiveUsers = receiveUser.split(";");
				InternetAddress[] to = new InternetAddress[receiveUsers.length];
				int i = 0;
				for (String item : receiveUsers) {
					to[i] = new InternetAddress(item);
					i++;
				}
				message.setRecipients(Message.RecipientType.TO, to);
			} else {
				InternetAddress to = new InternetAddress(receiveUser);
				message.setRecipient(Message.RecipientType.TO, to);
			}

			message.setSubject(sendInfo.getHeadName());
			String content = sendInfo.getSendHtml().toString();
			message.setContent(content, "text/html;charset=GBK");
			message.saveChanges();
			Transport transport = session.getTransport("smtp");
			transport.connect(sendInfo.getValueSmtp(), sendInfo.getSendUname(), sendInfo.getSendPwd());
			transport.sendMessage(message, message.getAllRecipients());
			transport.close();
			LOGGER.info("Message sent successfully:" + sendInfo.getHeadName());
		} catch (Exception e) {
			Throwables.propagate(e);
		}
	}

	private static void checkSendInfo(SendInfo sendInfo) {
		if (sendInfo.getKeySmtp() == null) {
			sendInfo.setKeySmtp(configs.get(Constant.EMAIL_UTIL_DEFAULT_KEY_SMTP));
		}
		if (sendInfo.getValueSmtp() == null) {
			sendInfo.setValueSmtp(configs.get(Constant.EMAIL_UTIL_DEFAULT_VALUE_SMTP));
		}
		if (sendInfo.getSendUser() == null) {
			sendInfo.setSendUser(configs.get(Constant.EMAIL_UTIL_DEFAULT_SEND_USER));
		}
		if (sendInfo.getSendUname() == null) {
			sendInfo.setSendUname(configs.get(Constant.EMAIL_UTIL_DEFAULT_SEND_USER).substring(0,
					configs.get(Constant.EMAIL_UTIL_DEFAULT_SEND_USER).lastIndexOf("@")));
		}
		if (sendInfo.getSendPwd() == null) {
			sendInfo.setSendPwd(configs.get(Constant.EMAIL_UTIL_DEFAULT_SEND_PWD));
		}
		if (sendInfo.getSendNickName() == null) {
			sendInfo.setSendNickName(configs.get(Constant.EMAIL_UTIL_DEFAULT_SEND_NICKNAME));
		}
		if (sendInfo.getReceiveUser() == null) {
			sendInfo.setReceiveUser(configs.get(Constant.EMAIL_UTIL_DEFAULT_RECEIVE_USER));
		}
	}

	public static class SendInfo {
		private String keySmtp;
		private String valueSmtp;
		private String sendUser;
		private String sendUname;
		private String sendPwd;
		private String sendNickName;
		private String receiveUser;
		private String headName;
		private String sendHtml;

		public String getKeySmtp() {
			return keySmtp;
		}

		public void setKeySmtp(String keySmtp) {
			this.keySmtp = keySmtp;
		}

		public String getValueSmtp() {
			return valueSmtp;
		}

		public void setValueSmtp(String valueSmtp) {
			this.valueSmtp = valueSmtp;
		}

		public String getSendUser() {
			return sendUser;
		}

		public void setSendUser(String sendUser) {
			this.sendUser = sendUser;
		}

		public String getSendUname() {
			return sendUname;
		}

		public void setSendUname(String sendUname) {
			this.sendUname = sendUname;
		}

		public String getSendPwd() {
			return sendPwd;
		}

		public void setSendPwd(String sendPwd) {
			this.sendPwd = sendPwd;
		}

		public String getSendNickName() {
			return sendNickName;
		}

		public void setSendNickName(String sendNickName) {
			this.sendNickName = sendNickName;
		}

		public String getReceiveUser() {
			return receiveUser;
		}

		public void setReceiveUser(String receiveUser) {
			this.receiveUser = receiveUser;
		}

		public String getHeadName() {
			return headName;
		}

		public void setHeadName(String headName) {
			this.headName = headName;
		}

		public String getSendHtml() {
			return sendHtml;
		}

		public void setSendHtml(String sendHtml) {
			this.sendHtml = sendHtml;
		}
	}

	private static String getFrom(Message message) throws Exception {
		InternetAddress[] address = (InternetAddress[]) ((MimeMessage) message).getFrom();
		String from = address[0].getAddress();
		if (from == null) {
			from = "";
		}
		return from;
	}

	private static String getSubject(Message message) throws Exception {
		String subject = "";
		if (((MimeMessage) message).getSubject() != null) {
			subject = MimeUtility.decodeText(((MimeMessage) message).getSubject());
		}
		return subject;
	}

	public static void getMailContent(Part part, StringBuffer bodytext) throws Exception {
		String contenttype = part.getContentType();
		int nameindex = contenttype.indexOf("name");
		boolean conname = false;
		if (nameindex != -1)
			conname = true;
		LOGGER.info("CONTENTTYPE: " + contenttype);
		if (part.isMimeType("text/plain") && !conname) {
			bodytext.append((String) part.getContent());
		} else if (part.isMimeType("text/html") && !conname) {
			bodytext.append((String) part.getContent());
		} else if (part.isMimeType("multipart/*")) {
			Multipart multipart = (Multipart) part.getContent();
			int counts = multipart.getCount();
			for (int i = 0; i < counts; i++) {
				getMailContent(multipart.getBodyPart(i), bodytext);
			}
		} else if (part.isMimeType("message/rfc822")) {
			getMailContent((Part) part.getContent(), bodytext);
		} else {
		}
	}

	private static boolean isContainAttach(Part part) throws Exception {
		boolean attachflag = false;
		if (part.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) part.getContent();
			for (int i = 0; i < mp.getCount(); i++) {
				BodyPart mpart = mp.getBodyPart(i);
				String disposition = mpart.getDisposition();
				if ((disposition != null)
						&& ((disposition.equals(Part.ATTACHMENT)) || (disposition.equals(Part.INLINE))))
					attachflag = true;
				else if (mpart.isMimeType("multipart/*")) {
					attachflag = isContainAttach((Part) mpart);
				} else {
					String contype = mpart.getContentType();
					if (contype.toLowerCase().indexOf("application") != -1)
						attachflag = true;
					if (contype.toLowerCase().indexOf("name") != -1)
						attachflag = true;
				}
			}
		} else if (part.isMimeType("message/rfc822")) {
			attachflag = isContainAttach((Part) part.getContent());
		}
		return attachflag;
	}

	private static void saveAttachMent(Part part, String filePath) throws Exception {
		String fileName = "";
		if (part.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) part.getContent();
			for (int i = 0; i < mp.getCount(); i++) {
				BodyPart mpart = mp.getBodyPart(i);
				String disposition = mpart.getDisposition();
				if ((disposition != null)
						&& ((disposition.equals(Part.ATTACHMENT)) || (disposition.equals(Part.INLINE)))) {
					fileName = mpart.getFileName();
					if (fileName != null) {
						fileName = MimeUtility.decodeText(fileName);
						saveFile(fileName, mpart.getInputStream(), filePath);
					}
				} else if (mpart.isMimeType("multipart/*")) {
					saveAttachMent(mpart, filePath);
				} else {
					fileName = mpart.getFileName();
					if (fileName != null) {
						fileName = MimeUtility.decodeText(fileName);
						saveFile(fileName, mpart.getInputStream(), filePath);
					}
				}
			}
		} else if (part.isMimeType("message/rfc822")) {
			saveAttachMent((Part) part.getContent(), filePath);
		}

	}

	private static void saveFile(String fileName, InputStream in, String filePath) throws Exception {
		File storefile = new File(filePath);
		if (!storefile.exists()) {
			storefile.mkdirs();
		}
		BufferedOutputStream bos = null;
		BufferedInputStream bis = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(filePath + "\\" + fileName));
			bis = new BufferedInputStream(in);
			int c;
			while ((c = bis.read()) != -1) {
				bos.write(c);
				bos.flush();
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (bos != null) {
				bos.close();
			}
			if (bis != null) {
				bis.close();
			}
		}
	}

	public static List<EmailInfo> receive(final ReceiveInfo receiveInfo) {
		OverFrequencyProtector.protect(OverFrequencyProtector.ProtectType.RECIEVE);

		List<EmailInfo> mailList = new ArrayList<EmailInfo>();

		URLName urln = null;
		Store receiveStore = null;
		Folder receiveFolder = null;
		try {
			checkReceiveInfo(receiveInfo);
			urln = new URLName(configs.get(Constant.EMAIL_UTIL_DEFAULT_MAIL_TYPE),
					configs.get(Constant.EMAIL_UTIL_DEFAULT_MAIL_HOST),
					Integer.valueOf(configs.get(Constant.EMAIL_UTIL_DEFAULT_MAIL_PORT)), null,
					receiveInfo.getUserName(), receiveInfo.getPassWord());

			Properties properties = System.getProperties();
			properties.put("mail.smtp.host", configs.get(Constant.EMAIL_UTIL_DEFAULT_MAIL_HOST));
			properties.put("mail.smtp.auth", configs.get(Constant.EMAIL_UTIL_DEFAULT_MAIL_AUTH));
			Session sessionMail = Session.getDefaultInstance(properties, new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(receiveInfo.getUserName(), receiveInfo.getPassWord());
				}
			});

			receiveStore = sessionMail.getStore(urln);
			receiveStore.connect();

			receiveFolder = receiveStore.getFolder("INBOX");
			receiveFolder.open(Folder.READ_WRITE);

			Message[] messages = null;

			if (!Strings.isNullOrEmpty(receiveInfo.getSenderFilter())) {
				SearchTerm st = new AndTerm(new FromStringTerm(receiveInfo.getSenderFilter()), new SubjectTerm(
						Strings.isNullOrEmpty(receiveInfo.getSubjectFilter()) ? "" : receiveInfo.getSubjectFilter()));
				messages = receiveFolder.search(st);
			} else {
				int count = receiveFolder.getMessageCount();
				messages = receiveFolder.getMessages(count, count);
			}

			if (messages != null && messages.length > 0) {

				LOGGER.info("Email count：" + messages.length);

				for (int i = 0; i < messages.length; i++) {
					EmailInfo entity = new EmailInfo();

					StringBuffer bodytext = new StringBuffer();
					getMailContent((Part) messages[i], bodytext);
					if (isContainAttach((Part) messages[i])) {
						saveAttachMent((Part) messages[i], configs.get(Constant.EMAIL_UTIL_DEFAULT_MAIL_ATTACH_PATH));
					}

					StringBuffer messageLog = new StringBuffer();
					messageLog.append("-----------------Email content start-----------------")
							.append(Constant.EMAIL_UTIL_DEFAULT_LOG_SPERATOR);
					String sender = getFrom(messages[i]);
					entity.setSender(sender);
					messageLog.append("sender:").append(sender).append(Constant.EMAIL_UTIL_DEFAULT_LOG_SPERATOR);
					String subject = getSubject(messages[i]);
					entity.setSubject(subject);
					messageLog.append("subject:").append(subject).append(Constant.EMAIL_UTIL_DEFAULT_LOG_SPERATOR);
					entity.setContent(bodytext.toString());
					// messageLog.append("content:").append(entity.getContent()).append(Config.LOG_SPERATOR);
					String sendTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
							.format(((MimeMessage) messages[i]).getSentDate());
					entity.setSendTime(sendTime);
					messageLog.append("send time:").append(sendTime).append(Constant.EMAIL_UTIL_DEFAULT_LOG_SPERATOR);
					boolean hasAttachment = isContainAttach((Part) messages[i]) ? true : false;
					entity.setHasAttachment(hasAttachment);
					messageLog.append("hasAttachment:").append(hasAttachment)
							.append(Constant.EMAIL_UTIL_DEFAULT_LOG_SPERATOR);
					messageLog.append("-----------------Email content end-----------------");

					LOGGER.info(messageLog);

					if (receiveInfo.isDelete()) {
						messages[i].setFlag(Flags.Flag.DELETED, true);
					}
					((POP3Message) messages[i]).invalidate(true);

					mailList.add(entity);
				}
			}

			receiveFolder.close(true);
		} catch (Exception e) {
			Throwables.propagate(e);
		} finally {
			if (receiveFolder != null && receiveFolder.isOpen()) {
				try {
					receiveFolder.close(true);
				} catch (MessagingException e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
			if (receiveStore.isConnected()) {
				try {
					receiveStore.close();
				} catch (MessagingException e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}

		return mailList;
	}

	private static void checkReceiveInfo(ReceiveInfo receiveInfo) {
		if (receiveInfo.getUserName() == null) {
			receiveInfo.setUserName(configs.get(Constant.EMAIL_UTIL_DEFAULT_SEND_USER));
			receiveInfo.setPassWord(configs.get(Constant.EMAIL_UTIL_DEFAULT_SEND_PWD));
			receiveInfo.setSenderFilter(configs.get(Constant.EMAIL_UTIL_DEFAULT_SEND_USER));
		}
	}

	public static class EmailInfo {
		private String sender;
		private String subject;
		private String content;
		private String sendTime;
		private boolean hasAttachment;

		public String getSender() {
			return sender;
		}

		public void setSender(String sender) {
			this.sender = sender;
		}

		public String getSubject() {
			return subject;
		}

		public void setSubject(String subject) {
			this.subject = subject;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public String getSendTime() {
			return sendTime;
		}

		public void setSendTime(String sendTime) {
			this.sendTime = sendTime;
		}

		public boolean isHasAttachment() {
			return hasAttachment;
		}

		public void setHasAttachment(boolean hasAttachment) {
			this.hasAttachment = hasAttachment;
		}
	}

	public static class ReceiveInfo {
		private String userName;
		private String passWord;
		private String senderFilter;
		private String subjectFilter;
		private boolean isDelete;
		private boolean isNew;

		public String getUserName() {
			return userName;
		}

		public void setUserName(String userName) {
			this.userName = userName;
		}

		public String getPassWord() {
			return passWord;
		}

		public void setPassWord(String passWord) {
			this.passWord = passWord;
		}

		public String getSenderFilter() {
			return senderFilter;
		}

		public void setSenderFilter(String senderFilter) {
			this.senderFilter = senderFilter;
		}

		public String getSubjectFilter() {
			return subjectFilter;
		}

		public void setSubjectFilter(String subjectFilter) {
			this.subjectFilter = subjectFilter;
		}

		public boolean isDelete() {
			return isDelete;
		}

		public void setDelete(boolean isDelete) {
			this.isDelete = isDelete;
		}

		public boolean isNew() {
			return isNew;
		}

		public void setNew(boolean isNew) {
			this.isNew = isNew;
		}
	}

	private static Lock sendLock = new ReentrantLock();
	private static Lock recieveLock = new ReentrantLock();;
	private static Date lastSendTime = null;
	private static Date lastRecieveTime = null;

	private static class OverFrequencyProtector {
		public static void protect(ProtectType protectType) {
			try {
				if (protectType == ProtectType.SEND) {
					long diffTime = protectType.getValue();
					if (lastSendTime != null) {
						Date now = new Date();
						diffTime = DateUtil.getDateDiff(now, lastSendTime);
					}
					if (sendLock.tryLock() && diffTime >= protectType.getValue()) {
						lastSendTime = new Date();
					} else {
						throw new RuntimeException("Over frequency protect");
					}
				} else if (protectType == ProtectType.RECIEVE) {
					long diffTime = protectType.getValue();
					if (lastRecieveTime != null) {
						Date now = new Date();
						diffTime = DateUtil.getDateDiff(now, lastRecieveTime);
					}
					if (recieveLock.tryLock() && diffTime >= protectType.getValue()) {
						lastRecieveTime = new Date();
					} else {
						throw new RuntimeException("Over frequency protect");
					}
				}
			} catch (Exception e) {
				Throwables.propagate(e);
			} finally {
				if (protectType == ProtectType.SEND) {
					sendLock.unlock();
				} else if (protectType == ProtectType.RECIEVE) {
					recieveLock.unlock();
				}
			}
		}

		public static enum ProtectType {
			SEND(5 * 1000), RECIEVE(1 * 60 * 1000);
			private ProtectType(Integer value) {
				this.value = value;
			}

			private Integer value;

			public Integer getValue() {
				return value;
			}
		}
	}

	public static void main(String[] args) throws InterruptedException {
		init("3333", "4444");

		/*
		 * SendInfo sendInfo = new SendInfo(); sendInfo.setHeadName("test");
		 * sendInfo.setSendHtml("test"); send(sendInfo);
		 */

		/*
		 * ReceiveInfo receiveInfo=new ReceiveInfo();
		 * receiveInfo.setDelete(true); receive(receiveInfo);
		 */
	}

}
