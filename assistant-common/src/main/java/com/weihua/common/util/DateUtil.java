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
}