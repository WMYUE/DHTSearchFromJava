package com.konka.dhtsearch.db.mongodb.orm1;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.konka.dhtsearch.db.DbUtil;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * @author 耳东 (cgp@0731life.com)
 */
public class MongodbUtil {
	protected final DB db;

	public MongodbUtil(DB db) {
		this.db = db;
	}

	/**
	 * 保存对象到db
	 * 
	 * @param object
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public void save(Object object) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		DBCollection collection = getCollectionName(object.getClass());
		DBObject dbobject = ObjectToDBObject(object);
		collection.insert(dbobject);
		DBCursor dbCursor = collection.find();
		 System.out.println(dbobject);
		for (; dbCursor.hasNext();) {
			DBObject dbObject2 = dbCursor.next();
//			if (dbObject2.containsField("lists")) {
				System.out.println("是集合吗=" + dbObject2);
//			}
		}
	}

	/**
	 * 获取DBCollection
	 * 
	 * @param clazz
	 * @return
	 */
	private DBCollection getCollectionName(Class<?> clazz) {
		MongoCollection collectionAnno = clazz.getAnnotation(MongoCollection.class);
		String collectionName;
		if (collectionAnno != null && collectionAnno.value().trim().length() != 0) {
			collectionName = collectionAnno.value();
		} else {
			collectionName = clazz.getName().replace(".", "_");// mongodb不支持'.'的名称
		}
		DBCollection collection = db.getCollection(collectionName);

		return collection;
	}

	/**
	 * 根据fiele获取字段名称
	 * 
	 * @param f
	 * @return
	 */
	private String getAnnotationFieldName(Field f) {
		MongoField fieldAnno = f.getAnnotation(MongoField.class);
		if (fieldAnno == null || fieldAnno.value().trim().length() == 0) {
			return f.getName();
		}
		return fieldAnno.value().trim();
	}

	/**
	 * 获取所有的字段
	 * 
	 * @param clazz
	 * @return
	 */
	private List<Field> getAllFields(Class<?> clazz) {
		List<Field> list = new ArrayList<Field>();
		list.addAll(Arrays.asList(clazz.getDeclaredFields()));
		// System.out.println(clazz);
		return list;
	}

	/**
	 * 将值转成db支持的类型
	 * 
	 * @param object
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	@SuppressWarnings("unchecked")
	private Object valueSerial(Object object) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (object.getClass().getAnnotation(MongoCollection.class) != null) {
			DBObject dbObject = ObjectToDBObject(object);
			return dbObject;
		}
		if (checkValidType(object.getClass())) {
			return object;
		}
		if (object instanceof List) {
			List<Object> list = (List<Object>) object;
			BasicDBList basicDBList = new BasicDBList();
			for (Object object2 : list) {
				Object dbobject = valueSerial(object2);
				basicDBList.add(dbobject);
			}
			return basicDBList;
		}
		return null;
	}

	/**
	 * 普通对象转成DbObject
	 * 
	 * @param object
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	private DBObject ObjectToDBObject(Object object) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Class<? extends Object> clazz = object.getClass();
		List<Field> fields = getAllFields(clazz);
		DBObject dbobject = new BasicDBObject();
		for (Field f : fields) {
			// MongoField mongoField = f.getAnnotation(MongoField.class);
			Method get = DbUtil.getFieldGetMethod(clazz, f);
			Object fieldobj = get.invoke(object);
			// System.out.println(fieldobj);
			// System.out.println(mongoField);
			if (fieldobj == null) {
				continue;
			}
			fieldobj = valueSerial(fieldobj);
			if (fieldobj == null) {
				continue;
			}
			String name = getAnnotationFieldName(f);
			dbobject.put(name, fieldobj);
		}
		return dbobject;
	}

	private boolean checkValidType(Class<?> clazz) {
		if (clazz.equals(Integer.class) || clazz.equals(Double.class) || //
				clazz.equals(Float.class) || clazz.equals(Boolean.class) || //
				clazz.equals(String.class) || clazz.equals(Date.class) || //
				clazz.equals(Long.class)) {
			return true;
		}
		return false;
	}

	// ----- load -----
	public <T> List<T> loadAll(Class<T> clazz) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SecurityException, IllegalArgumentException, NoSuchMethodException, InvocationTargetException {
		DBCollection collection = getCollectionName(clazz);
		List<T> objList = new ArrayList<T>();
		DBCursor curr = collection.find();
		while (curr.hasNext()) {
			// new object of the clazz
			// DBObject dbo = curr.next();
			// ObjectId id = (ObjectId) dbo.get("_id");
			T o = null;

			// if (depickled.containsKey(id)) {
			// o = (T) depickled.get(id);
			// } else {
			// o = (T) loadOne(clazz, dbo);
			// }
			objList.add(o);
		}
		return objList;
	}

	public <T> List<T> findByWhere(Class<T> clazz, int limit) throws Exception {
		DBCollection collection = getCollectionName(clazz);

		List<T> objList = new ArrayList<T>();
		// DBCursor curr = collection.find();
		DBCursor curr = collection.find(new BasicDBObject("name", "project1")).limit(limit);
		while (curr.hasNext()) {
			DBObject dbo = curr.next();
			T o = loadOne(clazz, dbo);
			objList.add(o);
		}
		return objList;
	}

	private <T> T loadOne(Class<T> clazz, DBObject dbo) throws Exception {
		T o = clazz.newInstance();
		List<Field> fields = getAllFields(o.getClass());
		for (Field f : fields) {
			String name = getAnnotationFieldName(f);// key
			Object value = dbo.get(name);//
			if (value != null) {
				// System.out.println("=="+f);
				Type type = f.getGenericType();
				value = valueDeserial(type, value);// 解析得到真实值
			}
			Method set = DbUtil.getFieldSetMethod(clazz, f);
			set.invoke(o, value);

		}
		return o;
	}

	private Object valueDeserial(Type type, Object object) throws Exception {
		Class<?> clazz = object.getClass();

		if (clazz == BasicDBList.class) {
			BasicDBList basicDBList = (BasicDBList) object;
			ArrayList<Object> arrayList = new ArrayList<>();
			Type type1 = ((ParameterizedType) type).getActualTypeArguments()[0];
			for (Object objectInList : basicDBList) {
				Class<?> objectInListTypeOfClass = Class.forName(type1.toString().replace("class ", "").trim());
				arrayList.add(valueDeserial(objectInListTypeOfClass, objectInList));
			}
			return arrayList;
		}
		if (checkValidType(clazz)) {//如果是基本类型，直接返回
			return object;
		}
		if (clazz == BasicDBObject.class) {
			DBObject basicDBObject = (BasicDBObject) object;
			Class<?> clazzq = Class.forName(type.toString().replace("class ", "").trim());
			return loadOne(clazzq, basicDBObject);
		}
		return object;
	}
}
