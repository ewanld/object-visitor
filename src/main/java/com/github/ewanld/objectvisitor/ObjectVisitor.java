package com.github.ewanld.objectvisitor;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;

import com.github.ewanld.objectvisitor.internal.ArrayUtil;
import com.github.ewanld.objectvisitor.internal.ObjectVisitorUtil;

/**
 * A class for traversing Java objects recursively.
 * <p>
 * The following objects may be traversed:
 * <ul>
 * <li>Primitive types
 * <li>{@code String} instances
 * <li>Arrays (recursively)
 * <li>{@code Iterable} instances (recursively)
 * <li>{@code Map} instances (recursively)
 * <li>Other {@code Object} instances (recursively, using reflection to access the attributes).
 * </ul>
 * Implementors should extend this class and override the {@code visitXXX methods}. Then, clients may call the
 * {@link #visitRecursively(Object)} method to visit any object.
 * <p>
 * {@code ObjectVisitor} is built around the following abstractions:
 * <ul>
 * <li>A <b>KeyValueObject</b> represents either a {@code Map}, or a Java object with fields or getters. A
 * {@code KeyValueObject} is made of <i>keys</i>.
 * <li>A <b>key</b> represents either:
 * <ul>
 * <li>An object field
 * <li>An object getter
 * <li>A key from a {@code Map}.
 * </ul>
 * </ul>
 * Arrays are automatically converted to {@link Iterable}, and boxed if necessary, to present a uniform interface
 * between arrays and Iterables.
 */
public abstract class ObjectVisitor {
	private final Map<Class<Object>, Function<Object, Object>> typeAdapters = new HashMap<>();
	private int nestingLevel = 0;

	// filter options
	private boolean transientFieldsIncluded;
	private boolean staticFieldsIncluded;
	private boolean nullsIncluded;
	private Predicate<Field> fieldInclusionPredicate;
	private Predicate<Class<?>> classInclusionPredicate;

	// sort options
	private boolean fieldsSorted = true;
	private boolean mapEntriesSorted = true;
	private boolean setsSorted = true;

	// cycle-related options
	private boolean detectCycles = true;
	private final List<Object> parentObjects = new ArrayList<>();
	private Function<Object, Object> alreadyVisitedReplacementFunction = null;

	public enum KeyValueObjectType {
		MAP, OBJECT
	}

	public enum KeyType {
		MAP_KEY, OBJECT_FIELD, OBJECT_GETTER
	}

	public abstract void visitBoolean(Boolean o) throws Exception;

	public abstract void visitLong(Long o) throws Exception;

	public abstract void visitInteger(Integer o) throws Exception;

	public abstract void visitShort(Short o) throws Exception;

	public abstract void visitByte(Byte o) throws Exception;

	public abstract void visitFloat(Float o) throws Exception;

	public abstract void visitDouble(Double o) throws Exception;

	public abstract void visitChar(Character o) throws Exception;

	public abstract void visitString(String s) throws Exception;

	public abstract void visitNull() throws Exception;

	@SuppressWarnings("unchecked")
	public <T> void addTypeAdapter(Class<T> _class, Function<? super T, Object> adapter) {
		typeAdapters.put((Class<Object>) _class, (Function<Object, Object>) adapter);
	}

	public void addBuiltinTypeAdapters() {
		addTypeAdapter(Date.class, Object::toString);
		addTypeAdapter(Instant.class, Object::toString);
		addTypeAdapter(SimpleDateFormat.class, SimpleDateFormat::toPattern);
	}

	public Map<Object, Object> mapOf(Object key, Object value) {
		final HashMap<Object, Object> res = new HashMap<>();
		res.put(key, value);
		return res;
	}

	private Function<Object, Object> findTypeAdapter(Object o) {
		@SuppressWarnings("unchecked") final Class<Object> _class = (Class<Object>) o.getClass();

		// fast path
		final Function<Object, Object> function = typeAdapters.get(_class);
		if (function != null) return function;

		// slow path
		for (final Map.Entry<Class<Object>, Function<Object, Object>> e : typeAdapters.entrySet()) {
			if (e.getKey().isInstance(o)) {
				typeAdapters.put(_class, e.getValue());
				return e.getValue();
			}
		}
		typeAdapters.put(_class, null);
		return null;
	}

	public void visitKey(Object key, KeyType type, Object parent) throws Exception {

	}

	protected void onIterableEvent(VisitEvent event, Iterable<?> list) throws Exception {

	}

	protected final void onIterableEvent(VisitEvent event) throws Exception {
		onIterableEvent(event, null);
	}

	protected void onKeyValueObjectEvent(VisitEvent event, KeyValueObjectType type, Object object) throws Exception {

	}

	protected final void onKeyValueObjectEvent(VisitEvent event, KeyValueObjectType type) throws Exception {
		onKeyValueObjectEvent(event, null);
	}

	private <T> Set<T> maybeSortSet(Set<T> set) {
		if (!setsSorted) return set;
		if (set instanceof SortedSet) return set;
		try {
			final TreeSet<T> res = new TreeSet<>(set);
			return res;
		} catch (final ClassCastException e) {
			return set;
		}
	}

	private boolean containsReference(Collection<?> collection, Object object) {
		return collection.stream().filter(o -> o == object).findFirst().isPresent();

	}

	public final void visitRecursively(Object o) throws Exception {
		if (o == null) {
			visitNull();
			return;
		}
		final Class<?> _class = o.getClass();

		if (o instanceof Boolean) {
			visitBoolean((Boolean) o);
		} else if (o instanceof Long) {
			visitLong((Long) o);
		} else if (o instanceof Integer) {
			visitInteger((Integer) o);
		} else if (o instanceof Short) {
			visitShort((Short) o);
		} else if (o instanceof Byte) {
			visitByte((Byte) o);
		} else if (o instanceof Character) {
			visitChar((Character) o);
		} else if (o instanceof Double) {
			visitDouble((Double) o);
		} else if (o instanceof Float) {
			visitFloat((Float) o);
		} else if (o instanceof Void) {
			visitNull();

		} else if (o instanceof String) {
			visitString((String) o);

		} else if (o instanceof Set) {
			final Set<?> set = maybeSortSet((Set<?>) o);
			visitIterable(set);

		} else if (o instanceof Iterable) {
			visitIterable((Iterable<?>) o);

		} else if (o instanceof Map) {
			visitMap((Map<?, ?>) o);

		} else if (o instanceof boolean[]) {
			visitIterable(Arrays.asList(ArrayUtil.boxArray((boolean[]) o)));
		} else if (o instanceof long[]) {
			visitIterable(Arrays.asList(ArrayUtil.boxArray((long[]) o)));
		} else if (o instanceof int[]) {
			visitIterable(Arrays.asList(ArrayUtil.boxArray((int[]) o)));
		} else if (o instanceof short[]) {
			visitIterable(Arrays.asList(ArrayUtil.boxArray((short[]) o)));
		} else if (o instanceof byte[]) {
			visitIterable(Arrays.asList(ArrayUtil.boxArray((byte[]) o)));
		} else if (o instanceof char[]) {
			visitIterable(Arrays.asList(ArrayUtil.boxArray((char[]) o)));
		} else if (o instanceof double[]) {
			visitIterable(Arrays.asList(ArrayUtil.boxArray((double[]) o)));
		} else if (o instanceof float[]) {
			visitIterable(Arrays.asList(ArrayUtil.boxArray((float[]) o)));

		} else if (_class.isArray()) {
			final Object[] objs = (Object[]) o;
			visitIterable(Arrays.asList(objs));

		} else {
			visitObject(o);
		}
	}

	/**
	 * Returns whether or not to append the given <code>Field</code>.
	 * <ul>
	 * <li>Transient fields are appended only if {@link #isVisitTransients()} returns <code>true</code>.
	 * <li>Static fields are appended only if {@link #isAppendStatics()} returns <code>true</code>.
	 * <li>Inner class fields are not appened.</li>
	 * </ul>
	 * @param field
	 *        The Field to test.
	 * @return Whether or not to append the given <code>Field</code>.
	 */
	private boolean isFieldAccepted(Field field) {
		if (field.getName().indexOf('$') != -1) {
			// Reject field from inner class.
			return false;
		}
		if (Modifier.isTransient(field.getModifiers()) && !transientFieldsIncluded) {
			// Reject transient fields.
			return false;
		}
		if (Modifier.isStatic(field.getModifiers()) && !staticFieldsIncluded) {
			// Reject static fields.
			return false;
		}
		if (fieldInclusionPredicate != null && !fieldInclusionPredicate.test(field)) {
			// Reject fields from the getExcludeFieldNames list.
			return false;
		}
		return true;
	}

	public final void visitObject(Object o) throws Exception {
		final Function<Object, Object> typeAdapter = findTypeAdapter(o);
		if (typeAdapter != null) {
			visitRecursively(typeAdapter.apply(o));
			return;
		}

		final Class<?> _class = o.getClass();
		final Field[] fields = _class.getDeclaredFields();
		AccessibleObject.setAccessible(fields, true);
		if (fieldsSorted) Arrays.sort(fields, Comparator.comparing(field -> field.getName().toLowerCase()));
		nestingLevel++;
		onKeyValueObjectEvent(VisitEvent.ENTER, KeyValueObjectType.OBJECT, o);
		boolean first = true;
		for (int i = 0; i < fields.length; i++) {
			final Field field = fields[i];
			if (!isFieldAccepted(field)) continue;
			final String key = field.getName();
			// Warning: Field.get(Object) creates wrappers objects for primitive types.
			Object child = ObjectVisitorUtil.getQuietly(() -> field.get(o));
			if (!isObjectAccepted(child)) continue;
			if (detectCycles) {
				if (containsReference(parentObjects, child)) {
					if (alreadyVisitedReplacementFunction == null)
						continue;
					else child = alreadyVisitedReplacementFunction.apply(child);
				}
				parentObjects.add(child);
			}
			onKeyValueObjectEvent(VisitEvent.BEFORE_CHILD, KeyValueObjectType.OBJECT, o);
			if (!first) onKeyValueObjectEvent(VisitEvent.INBETWEEN_CHILDREN, KeyValueObjectType.OBJECT, o);
			first = false;
			visitKey(key, KeyType.OBJECT_FIELD, o);
			visitRecursively(child);
			onKeyValueObjectEvent(VisitEvent.AFTER_CHILD, KeyValueObjectType.OBJECT, o);
			if (detectCycles) {
				final Object popped = parentObjects.remove(parentObjects.size() - 1);
				assert popped == child;
			}
		}
		nestingLevel--;
		onKeyValueObjectEvent(VisitEvent.LEAVE, KeyValueObjectType.OBJECT, o);
	}

	private static <T> TreeSet<T> newTreeSet(Collection<? extends T> collection, Comparator<? super T> comparator) {
		final TreeSet<T> res = new TreeSet<>(comparator);
		res.addAll(collection);
		return res;
	}

	public final void visitMap(Map<?, ?> map) throws Exception {
		nestingLevel++;
		onKeyValueObjectEvent(VisitEvent.ENTER, KeyValueObjectType.MAP, map);
		boolean first = true;
		@SuppressWarnings("unchecked") final Comparator<Object> comparator = (a, b) -> {
			if (a instanceof Comparable && b instanceof Comparable) {
				return Objects.compare((Comparable<Object>) a, (Comparable<Object>) b, Comparator.naturalOrder());
			} else {
				return Objects.compare(a.toString(), b.toString(), Comparator.naturalOrder());
			}
		};
		final Set<?> keySet = mapEntriesSorted ? newTreeSet(map.keySet(), comparator) : map.keySet();
		for (final Object key : keySet) {
			Object child = map.get(key);
			if (!isObjectAccepted(child)) continue;
			if (detectCycles) {
				if (containsReference(parentObjects, child)) {
					if (alreadyVisitedReplacementFunction == null)
						continue;
					else child = alreadyVisitedReplacementFunction.apply(child);
				}
				parentObjects.add(child);
			}
			onKeyValueObjectEvent(VisitEvent.BEFORE_CHILD, KeyValueObjectType.MAP, map);
			if (!first) onKeyValueObjectEvent(VisitEvent.INBETWEEN_CHILDREN, KeyValueObjectType.MAP, map);
			first = false;
			visitKey(key, KeyType.MAP_KEY, map);
			visitRecursively(child);
			onKeyValueObjectEvent(VisitEvent.AFTER_CHILD, KeyValueObjectType.MAP, map);
			if (detectCycles) {
				final Object popped = parentObjects.remove(parentObjects.size() - 1);
				assert popped == child;
			}
		}
		nestingLevel--;
		onKeyValueObjectEvent(VisitEvent.LEAVE, KeyValueObjectType.MAP, map);
	}

	public final void visitIterable(Iterable<?> iterable) throws Exception {
		nestingLevel++;
		onIterableEvent(VisitEvent.ENTER, iterable);
		boolean first = true;
		for (final Object child : iterable) {
			if (!isObjectAccepted(child)) continue;
			onIterableEvent(VisitEvent.BEFORE_CHILD, iterable);
			if (!first) onIterableEvent(VisitEvent.INBETWEEN_CHILDREN, iterable);
			first = false;
			visitRecursively(child);
			onIterableEvent(VisitEvent.AFTER_CHILD, iterable);
		}
		nestingLevel--;
		onIterableEvent(VisitEvent.LEAVE, iterable);
	}

	private boolean isObjectAccepted(Object o) {
		if (o == null) {
			return nullsIncluded;
		}
		final Class<?> _class = o.getClass();
		if (classInclusionPredicate != null && !classInclusionPredicate.test(_class)) return false;
		return true;
	}

	/**
	 * Get the current nesting level, starting from 0.
	 */
	protected int getNestingLevel() {
		return nestingLevel;
	}

	/**
	 * Specify whether {@code null} objects should be included/excluded.
	 * <p>
	 * For excluded objects, the parent key is excluded as well.
	 */
	public void setNullsIncluded(boolean nullsIncluded) {
		this.nullsIncluded = nullsIncluded;
	}

	/**
	 * Specify whether fields should be sorted by their field name.
	 */
	public void setFieldsSorted(boolean fieldsSorted) {
		this.fieldsSorted = fieldsSorted;
	}

	/**
	 * Specify whether map entries should be sorted according to their key.
	 * <p>
	 * When set to {@code true}, keys are sorted by their natural order if keys implement the {@link Comparable}
	 * interface, or according to their {@code toString} representation otherwise.
	 */
	public void setMapEntriesSorted(boolean mapEntriesSorted) {
		this.mapEntriesSorted = mapEntriesSorted;
	}

	/**
	 * Specify whether instances of {@link Set} should be sorted.
	 */
	public void setSetsSorted(boolean setsSorted) {
		this.setsSorted = setsSorted;
	}

	/**
	 * A custom predicate to include/exclude objects based on their parent field.
	 */
	public void setFieldInclusionPredicate(Predicate<Field> fieldInclusionPredicate) {
		this.fieldInclusionPredicate = fieldInclusionPredicate;
	}

	/**
	 * A custom predicate to include/exclude objects based on their class.
	 * <p>
	 * For excluded objects, the parent key is excluded as well.
	 */
	public void setClassInclusionPredicate(Predicate<Class<?>> classInclusionPredicate) {
		this.classInclusionPredicate = classInclusionPredicate;
	}

	/**
	 * Specify whether {@code transient} fields should be included/excluded.
	 */
	public void setTransientFieldsIncluded(boolean transientFieldsIncluded) {
		this.transientFieldsIncluded = transientFieldsIncluded;
	}

	/**
	 * Specify whether {@code static} fields should be included/excluded.
	 */
	public void setStaticFieldsIncluded(boolean staticFieldsIncluded) {
		this.staticFieldsIncluded = staticFieldsIncluded;
	}

	/**
	 * If true, the object graph is checked for cycles. Objects that have been already visited once are skipped along
	 * with the parent key (unless {@link {setAlreadyVisitedReplacementFunction} has been called).
	 * <p>
	 * Default value: {@code true}
	 */
	public void setDetectCycles(boolean detectCycles) {
		this.detectCycles = detectCycles;
	}

	/**
	 * A function to replace already visited objects with a custom one. The replaced object may be anything, even
	 * null.
	 * <p>
	 * This function has effect only when {@link #setDetectCycles(boolean)} is set to {@code true}.
	 * <p>
	 * If the specified function is not null, then the parent object key will be visited as well as the replaced
	 * object; otherwise, neither the parent key nor the object are visited.
	 */
	public void setAlreadyVisitedReplacementFunction(Function<Object, Object> alreadyVisitedReplacementFunction) {
		this.alreadyVisitedReplacementFunction = alreadyVisitedReplacementFunction;
	}

	public Map<Class<Object>, Function<Object, Object>> getTypeAdapters() {
		return typeAdapters;
	}

	public boolean isTransientFieldsIncluded() {
		return transientFieldsIncluded;
	}

	public boolean isStaticFieldsIncluded() {
		return staticFieldsIncluded;
	}

	public boolean isNullsIncluded() {
		return nullsIncluded;
	}

	public Predicate<Field> getFieldInclusionPredicate() {
		return fieldInclusionPredicate;
	}

	public Predicate<Class<?>> getClassInclusionPredicate() {
		return classInclusionPredicate;
	}

	public boolean isFieldsSorted() {
		return fieldsSorted;
	}

	public boolean isMapEntriesSorted() {
		return mapEntriesSorted;
	}

	public boolean isSetsSorted() {
		return setsSorted;
	}

	public boolean isDetectCycles() {
		return detectCycles;
	}

	public Function<Object, Object> getAlreadyVisitedReplacementFunction() {
		return alreadyVisitedReplacementFunction;
	}
}