package com.github.ewanld.objectvisitor.internal;

public class ArrayUtil {
	public static Character[] boxArray(char[] array) {
		if (array == null) return null;
		final Character[] result = new Character[array.length];
		for (int i = 0; i < array.length; i++) {
			result[i] = Character.valueOf(array[i]);
		}
		return result;
	}

	public static Long[] boxArray(long[] array) {
		if (array == null) return null;
		final Long[] result = new Long[array.length];
		for (int i = 0; i < array.length; i++) {
			result[i] = Long.valueOf(array[i]);
		}
		return result;
	}

	public static Integer[] boxArray(int[] array) {
		if (array == null) return null;
		final Integer[] result = new Integer[array.length];
		for (int i = 0; i < array.length; i++) {
			result[i] = Integer.valueOf(array[i]);
		}
		return result;
	}

	public static Short[] boxArray(short[] array) {
		if (array == null) return null;
		final Short[] result = new Short[array.length];
		for (int i = 0; i < array.length; i++) {
			result[i] = Short.valueOf(array[i]);
		}
		return result;
	}

	public static Byte[] boxArray(byte[] array) {
		if (array == null) return null;
		final Byte[] result = new Byte[array.length];
		for (int i = 0; i < array.length; i++) {
			result[i] = Byte.valueOf(array[i]);
		}
		return result;
	}

	public static Double[] boxArray(double[] array) {
		if (array == null) return null;
		final Double[] result = new Double[array.length];
		for (int i = 0; i < array.length; i++) {
			result[i] = Double.valueOf(array[i]);
		}
		return result;
	}

	public static Float[] boxArray(float[] array) {
		if (array == null) return null;
		final Float[] result = new Float[array.length];
		for (int i = 0; i < array.length; i++) {
			result[i] = Float.valueOf(array[i]);
		}
		return result;
	}

	public static Boolean[] boxArray(boolean[] array) {
		if (array == null) return null;
		final Boolean[] result = new Boolean[array.length];
		for (int i = 0; i < array.length; i++) {
			result[i] = (array[i] ? Boolean.TRUE : Boolean.FALSE);
		}
		return result;
	}
}
