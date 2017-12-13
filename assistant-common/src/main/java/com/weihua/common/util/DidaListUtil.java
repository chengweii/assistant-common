package com.weihua.common.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.base.Strings;
import com.google.gson.reflect.TypeToken;
import com.weihua.common.util.DateUtil.DateFormatType;

public class DidaListUtil {

	private static Logger LOGGER = Logger.getLogger(DidaListUtil.class);
	private static String username;
	private static String password;

	public static void init(String username, String password) {
		DidaListUtil.username = username;
		DidaListUtil.password = password;
	}

	public static void main(String[] args) throws Exception {
		List<Task> taskList = DidaListUtil.getTaskListFromDida365(TaskType.CURRENT_SCHEDULE, TaskStatus.UNFINISH);
		LOGGER.info(taskList);
	}

	private static String tokenHolder = null;
	private static Date tokenHoldTime = new Date();
	private static final String LOGIN_URL = "https://dida365.com/api/v2/user/signon?wc=true&remember=true";

	private static void login() {
		Date currentDate = new Date();
		try {
			if (tokenHolder == null || DateUtil.getDateDiff(currentDate, tokenHoldTime) > 600) {
				LoginInfo loginInfo = new LoginInfo();
				loginInfo.username = decode(username, decodeKey);
				loginInfo.password = decode(password, decodeKey);

				Map<String, String> headers = new HashMap<String, String>();
				headers.put("content-type", "application/json");
				headers.put("doOutput", "true");
				String loginResult = HttpUtil.post(LOGIN_URL, GsonUtil.toJson(loginInfo), headers);

				Map<String, String> map = GsonUtil.getMapFromJson(loginResult);
				tokenHolder = map.get("token");
				tokenHoldTime = currentDate;
			}
		} catch (Exception e) {
			LOGGER.error("login didalist failed.", e);
		}
	}

	private static final String GET_PROJECT_URL = "https://api.dida365.com/api/v2/projects";

	private static List<Map<String, Object>> getProjectList() throws Exception {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("cookie", "t=" + tokenHolder);
		String taskResult = HttpUtil.get(GET_PROJECT_URL, "", headers);
		List<Map<String, Object>> projectList = GsonUtil.<ArrayList<Map<String, Object>>> getEntityFromJson(taskResult,
				new TypeToken<ArrayList<Map<String, Object>>>() {
				});
		return projectList;
	}

	private static final String GET_TASK_URL = "https://api.dida365.com/api/v2/project/{project_id}/tasks/?from=&to={end_time}&limit={limit_count}";

	private static List<Task> getTaskList(String projectId, String endTime, String limitCount, TaskStatus taskStatus)
			throws Exception {
		if (Strings.isNullOrEmpty(projectId) || Strings.isNullOrEmpty(endTime) || Strings.isNullOrEmpty(limitCount)) {
			LOGGER.error("projectId,endTime,limitCount be required.");
			return null;
		}
		String url = GET_TASK_URL.replace("{project_id}", projectId).replace("{end_time}", endTime)
				.replace("{limit_count}", limitCount).replace(" ", "%20");
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("cookie", "t=" + tokenHolder);

		String taskContent = HttpUtil.get(url, "", headers);
		List<Task> taskList = GsonUtil.<ArrayList<Task>> getEntityFromJson(taskContent,
				new TypeToken<ArrayList<Task>>() {
				});
		if (!CollectionUtil.isEmpty(taskList) && taskStatus != null) {
			List<Task> filterTaskList = new ArrayList<Task>();
			for (Task task : taskList) {
				if (String.valueOf(taskStatus.ordinal()).equals(task.status)) {
					filterTaskList.add(task);
				}
			}
			return filterTaskList;
		}

		return taskList;
	}

	public static List<Task> getTaskListFromDida365(TaskType taskType, TaskStatus taskStatus) throws Exception {
		login();
		List<Map<String, Object>> projectList = getProjectList();
		if (!CollectionUtil.isEmpty(projectList)) {
			String projectId = null;
			for (Map<String, Object> map : projectList) {
				if (map.get("name").equals(taskType.getValue())) {
					projectId = String.valueOf(map.get("id"));
					break;
				}
			}
			String endTime = DateUtil.getDateFormat(new Date(), DateFormatType.YYYY_MM_DD_HH_MM_SS);
			List<Task> taskList = getTaskList(projectId, endTime, "100",
					taskStatus == null ? TaskStatus.UNFINISH : taskStatus);
			if (!CollectionUtil.isEmpty(taskList)) {
				LOGGER.info("Query task size:" + taskList.size());
				return taskList;
			}
		}
		return null;
	}

	public static enum TaskType {
		CURRENT_SCHEDULE("CurrentSchedule", "当前日程"), MY_WORKS("MyWorks", "我的工作"), FUTURE_TRIFLES("FutureTrifles",
				"待办琐事"), MY_DAILY("MyDaily", "我的日常");

		private TaskType(String code, String value) {
			this.code = code;
			this.value = value;
		}

		private String code;
		private String value;

		public String getCode() {
			return code;
		}

		public String getValue() {
			return value;
		}

		public static TaskType fromCode(String code) {
			for (TaskType entity : TaskType.values()) {
				if (entity.getCode().equals(code)) {
					return entity;
				}
			}
			return null;
		}

		public static TaskType fromValue(String value) {
			for (TaskType entity : TaskType.values()) {
				if (entity.getValue().equals(value)) {
					return entity;
				}
			}
			return null;
		}
	}

	public static enum TaskStatus {
		UNFINISH, DOING, FINISHED;
	}

	public static void initDidaListUtil(String uname, String pwd) {
		username = uname;
		password = pwd;
	}

	private static final String decodeKey = "huawei";

	private static String encode(String s, String key) {
		String str = "";
		int ch;
		if (key.length() == 0) {
			return s;
		} else if (!s.equals(null)) {
			for (int i = 0, j = 0; i < s.length(); i++, j++) {
				if (j > key.length() - 1) {
					j = j % key.length();
				}
				ch = s.codePointAt(i) + key.codePointAt(j);
				if (ch > 65535) {
					ch = ch % 65535;// ch - 33 = (ch - 33) % 95 ;
				}
				str += (char) ch;
			}
		}
		return str;

	}

	private static String decode(String s, String key) {
		String str = "";
		int ch;
		if (key.length() == 0) {
			return s;
		} else if (!s.equals(key)) {
			for (int i = 0, j = 0; i < s.length(); i++, j++) {
				if (j > key.length() - 1) {
					j = j % key.length();
				}
				ch = (s.codePointAt(i) + 65535 - key.codePointAt(j));
				if (ch > 65535) {
					ch = ch % 65535;// ch - 33 = (ch - 33) % 95 ;
				}
				str += (char) ch;
			}
		}
		return str;
	}

	private static class LoginInfo {
		public String username;
		public String password;
	}

	public static class Task {
		public String id;
		public String deleted;
		public String createdTime;
		public String completedTime;
		public String startDate;
		public String priority;
		public String title;
		public String content;
		public String status;
		public List<Item> items;

		public static class Item {
			public String id;
			public String title;
			public String status;
		}
	}
}
