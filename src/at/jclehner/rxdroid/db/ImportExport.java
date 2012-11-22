package at.jclehner.rxdroid.db;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import at.caspase.rxdroid.Fraction;
import at.jclehner.androidutils.Reflect;
import at.jclehner.rxdroid.util.CollectionUtils;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.DatabaseTableConfig;

public final class ImportExport
{
	private static final String TAG = ImportExport.class.getName();

	public interface JsonPersister<T>
	{
		public static final String NULL = JsonPersister.class.getName() + ".NULL";

		public String toJsonString(T object) throws JSONException;
		public T fromJsonString(String data) throws JSONException;

		public String nullString();
	}

	public abstract class AbsJsonPersister<T> implements JsonPersister<T>
	{
		@Override
		public String nullString() {
			return null;
		}
	}

	@SuppressWarnings("rawtypes")
	private static HashMap<Class<?>, JsonPersister> sPersisters =
			new HashMap<Class<?>, JsonPersister>();

	static
	{
		sPersisters.put(Fraction.class, new JsonPersisters.FractionPersister());
		sPersisters.put(Date.class, new JsonPersisters.DatePersister());
	}

	public static <T> void register(Class<? extends T> clazz, JsonPersister<T> persister) {
		sPersisters.put(clazz, persister);
	}

	public static JSONObject tableToJsonObject(Class<?> clazz, Collection<?> entries) throws JSONException
	{
		final JSONArray array = new JSONArray();

		for(Object t : entries)
			array.put(entryToJsonObject(t));

		return new JSONObject().put(DatabaseTableConfig.extractTableName(clazz), array);
	}

	public static JSONObject entryToJsonObject(Object object) throws JSONException
	{
		final Class<?> clazz = object.getClass();
		final JSONObject json = new JSONObject();

		Log.d(TAG, "entryToJsonObject");

		for(Field f : clazz.getDeclaredFields())
		{
			Annotation a = f.getAnnotation(DatabaseField.class);
			if(a == null)
				continue;

			final Class<?> type = !isForeignField(f, a) ? f.getType() : Long.class;
			final String name = getColumnName(f, a);
			final Object value = Reflect.getFieldValue(f, object);

			Log.d(TAG, "  " + name + "=" + value);

			if(isJsonable(type))
				json.put(name, value);
			else if(isFloat(type))
				json.put(name, (Double) value);
			else
				json.put(name, toJsonStringInternal(value, type));

			Log.d(TAG, "  " + name + ": " + json.get(name));
		}

		return json;
	}

	public static void entryFromJsonObject(JSONObject json, Object outObject) throws JSONException
	{
		final Class<?> clazz = outObject.getClass();

		for(Field f : clazz.getFields())
		{
			Annotation a = f.getAnnotation(DatabaseField.class);
			if(a == null)
				continue;

			final Class<?> type = !isForeignField(f, a) ? f.getType() : long.class;
			final String name = getColumnName(f, a);
			final Object value;

			if(isJsonable(type))
				value = json.get(name);
			else if(isFloat(type))
				value = (float) json.getDouble(name);
			else
				value = fromJsonStringInternal(json.getString(name), clazz);

			try
			{
				f.set(outObject, value);
			}
			catch(IllegalArgumentException e)
			{
				throw new JSONException(e.getMessage());
			}
			catch(IllegalAccessException e)
			{
				throw new JSONException(e.getMessage());
			}
		}
	}

	public static JsonPersister<?> getPersister(Class<?> clazz) throws JSONException
	{
		final JsonPersister<?> persister = sPersisters.get(clazz);
		if(persister == null)
			throw new JSONException("No registered persister for " + clazz.getName());

		return persister;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static String toJsonStringInternal(Object object, Class<?> clazz) throws JSONException
	{
		Log.d(TAG, "toJsonStringInternal(" + object + ", " + clazz + ")");

		final JsonPersister persister = getPersister(clazz);
		final String string = object != null ? persister.toJsonString(object) : persister.nullString();

		Log.d(TAG, "  string=" + string);

		return string;
	}

	private static Object fromJsonStringInternal(String string, Class<?> clazz) throws JSONException
	{
//		final String[] data = string.split("|");
//		if(data.length != 2)
//			throw new JSONException("Invalid data");
//		final Class<?> clazz = Class.forName(data[1]);

		if(string == null || string.length() == 0)
			return null;

		final JsonPersister<?> persister = getPersister(clazz);
		return persister.fromJsonString(string);
	}

	private static String getColumnName(Field f, Annotation a)
	{
		final DatabaseFieldConfig dfc = new DatabaseFieldConfig(
				f.getName(),
				(String) Reflect.getAnnotationParameter(a, "columnName"),
				(DataType) Reflect.getAnnotationParameter(a, "dataType"),
				(String) Reflect.getAnnotationParameter(a, "defaultValue"),
				(Integer) Reflect.getAnnotationParameter(a, "width"),
				(Boolean) Reflect.getAnnotationParameter(a, "canBeNull"),
				(Boolean) Reflect.getAnnotationParameter(a, "id"),
				(Boolean) Reflect.getAnnotationParameter(a, "generatedId"),
				(String) Reflect.getAnnotationParameter(a, "generatedIdSequence"),
				(Boolean) Reflect.getAnnotationParameter(a, "foreign"),
				null, //Reflect.getAnnotationParameter(a, "foreignTableConfig"),
				(Boolean) Reflect.getAnnotationParameter(a, "useGetSet"),
				null, //(Enum<?>) Reflect.getAnnotationParameter(a, "unknownEnumValue"),
				(Boolean) Reflect.getAnnotationParameter(a, "throwIfNull"),
				(String) Reflect.getAnnotationParameter(a, "format"),
				(Boolean) Reflect.getAnnotationParameter(a, "unique"),
				(String) Reflect.getAnnotationParameter(a, "indexName"),
				(String) Reflect.getAnnotationParameter(a, "uniqueIndexName"),
				false, //(Boolean) Reflect.getAnnotationParameter(a, "autoRefresh"),
				(Integer) Reflect.getAnnotationParameter(a, "maxForeignAutoRefreshLevel"),
				0 //(Integer) Reflect.getAnnotationParameter(a, "maxForeignCollectionLevel")
		);

		return dfc.getColumnName();


//		String columnName = Reflect.getAnnotationParameter(a, "columnName");
//		if(columnName == null || columnName.length() == 0 || DatabaseField.DEFAULT_STRING.equals(columnName))
//			columnName = new DatabaseFieldConfig(fieldName, columnName);
//		else if(isForeignField(f, a))
//			return f.getName() + "_id";
//
//		return columnName;
	}

	private static boolean isForeignField(Field f, Annotation a) {
		return Reflect.getAnnotationParameter(a, "foreign");
	}

	private static final Class<?>[] JSONABLE = {
		boolean.class, double.class, int.class, long.class,
		Boolean.class, Double.class, Integer.class, Long.class,
		String.class
	};

	private static boolean isJsonable(Class<?> clazz) {
		return CollectionUtils.indexOfByReference(clazz, JSONABLE) != -1;
	}

	private static boolean isFloat(Class<?> clazz) {
		return clazz == float.class || clazz == Float.class;
	}

	private ImportExport() {}
}
