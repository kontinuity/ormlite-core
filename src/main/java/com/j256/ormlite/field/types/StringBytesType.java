package com.j256.ormlite.field.types;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.sql.SQLException;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.misc.SqlExceptionUtil;
import com.j256.ormlite.support.DatabaseResults;

/**
 * Type that persists a String as a byte array.
 * 
 * @author graywatson
 */
public class StringBytesType extends BaseDataType {

	private static final String DEFAULT_STRING_BYTES_CHARSET_NAME = "Unicode";

	private static final StringBytesType singleTon = new StringBytesType();

	public static StringBytesType getSingleton() {
		return singleTon;
	}

	private StringBytesType() {
		super(SqlType.BYTE_ARRAY, new Class<?>[0]);
	}

	@Override
	public Object parseDefaultString(FieldType fieldType, String defaultStr) throws SQLException {
		throw new SQLException("String bytes type cannot have default values");
	}

	@Override
	public Object resultToJava(FieldType fieldType, DatabaseResults results, int columnPos) throws SQLException {
		byte[] bytes = results.getBytes(columnPos);
		if (bytes == null) {
			return null;
		} else {
			return sqlArgToJava(fieldType, bytes, columnPos);
		}
	}

	@Override
	public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) throws SQLException {
		byte[] bytes = (byte[]) sqlArg;
		String charsetName = getCharsetName(fieldType);
		try {
			// NOTE: I can't use new String(bytes, Charset) because it was introduced in 1.6.
			return new String(bytes, charsetName);
		} catch (UnsupportedEncodingException e) {
			throw SqlExceptionUtil.create("Could not convert string with charset name: " + charsetName, e);
		}
	}

	@Override
	public Object javaToSqlArg(FieldType fieldType, Object javaObject) throws SQLException {
		String string = (String) javaObject;
		String charsetName = getCharsetName(fieldType);
		try {
			// NOTE: I can't use string.getBytes(Charset) because it was introduced in 1.6.
			return string.getBytes(charsetName);
		} catch (UnsupportedEncodingException e) {
			throw SqlExceptionUtil.create("Could not convert string with charset name: " + charsetName, e);
		}
	}

	@Override
	public boolean isValidForField(Field field) {
		// by default this is a noop
		return true;
	}

	@Override
	public boolean isAppropriateId() {
		return false;
	}

	@Override
	public boolean isSelectArgRequired() {
		return true;
	}

	private String getCharsetName(FieldType fieldType) {
		if (fieldType == null || fieldType.getFormat() == null) {
			return DEFAULT_STRING_BYTES_CHARSET_NAME;
		} else {
			return fieldType.getFormat();
		}
	}
}
