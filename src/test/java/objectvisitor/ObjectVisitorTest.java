package objectvisitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.GregorianCalendar;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.github.ewanld.objectvisitor.ObjectVisitor;

/**
 * JUnit tests for the class {@link ObjectVisitor}.
 */
public class ObjectVisitorTest {
	private final ObjectVisitor visitor;
	private static final File last = new File(
			ObjectVisitorTest.class.getClassLoader().getResource("TestObjectVisitor-last.txt").getFile());
	private static final File ref = new File(
			ObjectVisitorTest.class.getClassLoader().getResource("TestObjectVisitor-ref.txt").getFile());
	private Writer writer;

	public ObjectVisitorTest() throws IOException {
		writer = new BufferedWriter(new FileWriter(last));

		visitor = new Json5Dumper(writer);

		// visit options
		visitor.setNullsIncluded(true);
		visitor.setTransientFieldsIncluded(false);
		visitor.setStaticFieldsIncluded(false);

		// sorting options
		visitor.setSetsSorted(false);
		visitor.setKeysSorted(false);

		// inclusion options
		visitor.setFieldInclusionPredicate(f -> {
			if (f.getName().startsWith("mylong")) return false;
			return true;
		});
		visitor.setClassInclusionPredicate(c -> {
			return true;
		});
		visitor.setAlreadyVisitedReplacementFunction(o -> "<skipped>");
		visitor.addBuiltinTypeAdapters();
	}

	public void tearDown() throws IOException {
		writer.close();
	}

	public void standardOptions() throws Exception {
		// test null
		write(null);

		// test booleans
		write(true);
		write(false);

		// test lists of booleans
		write(Arrays.asList());
		write(Arrays.asList(true));
		write(Arrays.asList(true, false));
		write(Arrays.asList(true, null));

		write(Arrays.asList(Boolean.TRUE));
		write(Arrays.asList(Boolean.TRUE, Boolean.FALSE));

		// test integers
		write(1);
		write(1l);

		// test lists of integers
		write(Arrays.asList(1, 2));
		write(Arrays.asList(3l, 4l));
		write(Arrays.asList(5l, null));
		write(Arrays.asList(6l, 7l));

		// test arrays of integers
		final int[] arr = { 8, 9 };
		write(arr);

		final int[] arr2 = {};
		write(arr2);

		final int[] arr3 = null;
		write(arr3);

		final long[] arr4 = { 10l, 11l };
		write(arr4);

		final short[] arr5 = { 12, 13 };
		write(arr5);

		final byte[] arr6 = { 14, 15 };
		write(arr6);

		final int[][] arr7 = { { 16, 17 }, { 18 }, null };
		write(arr7);

		// test strings
		write("");
		write("s");
		write(" ");

		write("line1\nline2");

		write("line1\nline2\nline3");

		write("a\tb");
		write("a	b");

		write("a\\b");
		write("a\"b");
		write("a'b");
		write("a\\\"b");

		// test objects
		write(new Class1());

		// test cycle detection
		final RecursiveClass recursiveClass = new RecursiveClass(true, null);
		final RecursiveClass recursiveClass_child = new RecursiveClass(false, recursiveClass);
		recursiveClass.setOther(recursiveClass_child);
		write(recursiveClass);

		// test builtin type adapters
		write(new GregorianCalendar(2010, 2, 10).getTime());
		write(new java.sql.Date(new GregorianCalendar(2010, 2, 10).getTimeInMillis()));
		write(new SimpleDateFormat("YYYY-MM-dd").toPattern());
		write(new GregorianCalendar(2010, 2, 10).getTime().toInstant());

		writer.flush();
	}

	@Test
	public void testAll() throws Exception {
		standardOptions();
		assert FileUtils.contentEquals(last, ref);
	}

	public void write(Object o) throws Exception {
		visitor.visitRecursively(o);
		writer.write("\n\n");
	}

	public static class Class1 {
		private final boolean bool1 = true;

		public boolean isBool1() {
			return bool1;
		}
	}

	public static class RecursiveClass {
		private final boolean bool1;
		private RecursiveClass other;

		public RecursiveClass(boolean bool1, RecursiveClass other) {
			this.bool1 = bool1;
			this.other = other;
		}

		public void setOther(RecursiveClass other) {
			this.other = other;
		}

		public boolean isBool1() {
			return bool1;
		}

		public RecursiveClass getOther() {
			return other;
		}

	}
}
