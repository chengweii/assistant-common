package com.weihua.common.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Throwables;

public class DateUtil {

	private static ThreadLocal<Map<DateFormatType, DateFormat>> dateFormatMap = new ThreadLocal<Map<DateFormatType, DateFormat>>() {
		@Override
		protected Map<DateFormatType, DateFormat> initialValue() {
			Map<DateFormatType, DateFormat> map = new HashMap<DateFormatType, DateFormat>();
			for (DateFormatType item : DateFormatType.values()) {
				map.put(item, new SimpleDateFormat(item.getValue()));
			}
			return map;
		}
	};

	public static void main(String[] args) throws Exception {
		System.out.println(getDateFormat(new Date(), DateFormatType.YYYY_MM_DD_HH_MM_SS));
	}

	public static String getDateFormat(Date date, DateFormatType dateFormatType) {
		if (dateFormatType == null)
			return null;
		DateFormat dateFormat = dateFormatMap.get().get(dateFormatType);
		return dateFormat.format(date);
	}

	public static String getDateFormat(Date date, String dateFormat) {
		DateFormat dateFormator = new SimpleDateFormat(dateFormat);
		return dateFormator.format(date);
	}

	public static Date getDateFormat(String date) {
		DateFormat dateFormat = dateFormatMap.get().get(DateFormatType.YYYY_MM_DD);
		try {
			return dateFormat.parse(date);
		} catch (ParseException e) {
			Throwables.propagate(e);
		}
		return null;
	}

	public static Date getDateTimeFormat(String dateTime) {
		DateFormat dateTimeFormat = dateFormatMap.get().get(DateFormatType.YYYY_MM_DD_HH_MM_SS);
		try {
			return dateTimeFormat.parse(dateTime);
		} catch (ParseException e) {
			Throwables.propagate(e);
		}
		return null;
	}

	public static Date getNowDate() {
		DateFormat dateFormat = dateFormatMap.get().get(DateFormatType.YYYY_MM_DD);
		try {
			return dateFormat.parse(dateFormat.format(new Date()));
		} catch (ParseException e) {
			Throwables.propagate(e);
		}
		return null;
	}

	public static Date getNowDateTime() {
		return new Date();
	}

	public static long getDateDiff(Date dateEnd, Date dateStart) {
		return dateEnd.getTime() - dateStart.getTime();
	}

	public static enum DateFormatType {
		YYYY_MM_DD("yyyy-MM-dd"), YYYY_MM_DD_HH_MM_SS("yyyy-MM-dd HH:mm:ss"), HH_MM_SS("HH:mm:ss"), HH_MM("HH:mm");
		private DateFormatType(String value) {
			this.value = value;
		}

		private String value;

		public String getValue() {
			return value;
		}
	}

	public static TimePeriod getTimePeriodByDate(Date date) {
		double hour = Double.valueOf(date.getHours()) + Double.valueOf(date.getMinutes()) / 60d;
		if (hour >= 5 && hour < 8) {
			return TimePeriod.MORNING;
		} else if (hour >= 8 && hour < 11) {
			return TimePeriod.BEFORENOON;
		} else if (hour >= 11 && hour < 13) {
			return TimePeriod.NOON;
		} else if (hour >= 13 && hour < 18) {
			return TimePeriod.AFTERNOON;
		} else if (hour >= 18 && hour < 21) {
			return TimePeriod.NIGHT;
		} else if (hour >= 21 || hour < 5) {
			return TimePeriod.DEEPNIGHT;
		}
		return null;
	}

	public static enum TimePeriod {

		/**
		 * 早晨
		 */
		MORNING("MORNING", "早晨"),

		/**
		 * 上午
		 */
		BEFORENOON("BEFORENOON", "上午"),

		/**
		 * 中午
		 */
		NOON("NOON", "中午"),

		/**
		 * 下午
		 */
		AFTERNOON("AFTERNOON", "下午"),

		/**
		 * 晚上
		 */
		NIGHT("NIGHT", "晚上"),

		/**
		 * 深夜
		 */
		DEEPNIGHT("DEEPNIGHT", "深夜");

		private TimePeriod(String code, String value) {
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

		public static TimePeriod fromCode(String code) {
			for (TimePeriod entity : TimePeriod.values()) {
				if (entity.getCode().equals(code)) {
					return entity;
				}
			}
			return null;
		}
	}

}