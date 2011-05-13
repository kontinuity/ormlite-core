package com.j256.ormlite.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.j256.ormlite.field.ForeignCollectionField;

/**
 * Collection that is set on a field that as been marked with the {@link ForeignCollectionField} annotation when an
 * object is refreshed or queried (i.e. not created). Most of the methods here require a pass through the database.
 * Operations such as size() therefore should most likely not be used because of their expense. Chances are you only
 * want to use the {@link #iterator()}, {@link #toArray()}, and {@link #toArray(Object[])} methods.
 * 
 * <p>
 * <b>WARNING:</b> Most likely for(;;) loops should not be used here since we need to be careful about closing the
 * iterator.
 * </p>
 * 
 * @author graywatson
 */
public class LazyForeignCollection<T, ID> extends BaseForeignCollection<T, ID> implements ForeignCollection<T> {

	private CloseableIterator<T> lastIterator;

	public LazyForeignCollection(Dao<T, ID> dao, String fieldName, Object fieldValue) throws SQLException {
		super(dao, fieldName, fieldValue);
	}

	/**
	 * The iterator returned from a lazy collection keeps a connection open to the database as it iterates across the
	 * collection. You will need to call {@link CloseableIterator#close()} or go all the way through the loop to ensure
	 * that the connection has been closed. You can also call {@link #closeLastIterator()} on the collection itself
	 * which will close the last iterator returned. See the reentrant warning.
	 */
	public CloseableIterator<T> iterator() {
		try {
			return iteratorThrow();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * This is the same as {@link #iterator()} except it throws.
	 */
	public CloseableIterator<T> iteratorThrow() throws SQLException {
		lastIterator = dao.iterator(preparedQuery);
		return lastIterator;
	}

	public CloseableWrappedIterable<T> getWrappedIterable() {
		return new CloseableWrappedIterableImpl<T>(this);
	}

	public void closeLastIterator() throws SQLException {
		if (lastIterator != null) {
			lastIterator.close();
			lastIterator = null;
		}
	}

	public int size() {
		int sizeC = 0;
		CloseableIterator<T> iterator = iterator();
		try {
			while (iterator.hasNext()) {
				sizeC++;
			}
		} finally {
			try {
				iterator.close();
			} catch (SQLException e) {
				// ignored
			}
		}
		return sizeC;
	}

	public boolean isEmpty() {
		CloseableIterator<T> iterator = iterator();
		try {
			return !iterator.hasNext();
		} finally {
			try {
				iterator.close();
			} catch (SQLException e) {
				// ignored
			}
		}
	}

	public boolean contains(Object obj) {
		CloseableIterator<T> iterator = iterator();
		try {
			while (iterator.hasNext()) {
				if (iterator.next().equals(obj)) {
					return true;
				}
			}
			return false;
		} finally {
			try {
				iterator.close();
			} catch (SQLException e) {
				// ignored
			}
		}
	}

	public boolean containsAll(Collection<?> collection) {
		/*
		 * This is pretty inefficient (i.e. N^2) but I didn't want to suck all of the items into a Map because this
		 * could swallow all memory. So user beware.
		 */
		for (Object obj : collection) {
			if (!contains(obj)) {
				return false;
			}
		}
		return true;
	}

	public Object[] toArray() {
		List<T> items = new ArrayList<T>();
		CloseableIterator<T> iterator = iterator();
		try {
			while (iterator.hasNext()) {
				items.add(iterator.next());
			}
		} finally {
			try {
				iterator.close();
			} catch (SQLException e) {
				// ignored
			}
		}
		return items.toArray();
	}

	public <E> E[] toArray(E[] array) {
		List<E> items = null;
		int itemC = 0;
		CloseableIterator<T> iterator = iterator();
		try {
			while (iterator.hasNext()) {
				@SuppressWarnings("unchecked")
				E castData = (E) iterator.next();
				// are we exceeding our capacity in the array?
				if (itemC >= array.length) {
					if (items == null) {
						items = new ArrayList<E>();
						for (E arrayData : array) {
							items.add(arrayData);
						}
					}
					items.add(castData);
				} else {
					array[itemC] = castData;
				}
				itemC++;
			}
		} finally {
			try {
				iterator.close();
			} catch (SQLException e) {
				// ignored
			}
		}
		if (items == null) {
			if (itemC < array.length - 1) {
				array[itemC] = null;
			}
			return array;
		} else {
			@SuppressWarnings("unchecked")
			E[] castArray = (E[]) items.toArray();
			return castArray;
		}
	}

	/**
	 * This is just a call to {@link Object#equals(Object)}.
	 */
	@Override
	public boolean equals(Object other) {
		return super.equals(other);
	}

	/**
	 * This is just a call to {@link Object#hashCode()}.
	 */
	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
