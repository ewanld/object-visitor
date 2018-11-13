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
import org.junit.After;
import org.junit.Test;

import com.github.ewanld.objectvisitor.ObjectVisitor;

/**
 * JUnit tests for the class {@link ObjectVisitor}.
 */
public class ObjectVisitorTest {
	private final ObjectVisitor visitor;
	private final File last;
	private final File ref = new File(
			ObjectVisitorTest.class.getClassLoader().getResource("TestObjectVisitor-ref.txt").getFile());
	private Writer writer;

	public ObjectVisitorTest() throws IOException {
		last = File.createTempFile("TestObjectVisitor-", ".txt");
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

	@After
	public void tearDown() throws IOException {
		writer.close();
		last.delete();
	}

	public void standardOptions() throws Exception {
		visitNull();
		visitBoolean();
		visitListOfBooleans();
		visitInt();
		visitListOfInts();
		visitArrayOfInts();
		visitString();
		visit(new Class1());
		visitRecursiveObject();
		visitBuiltinTypes();
		writer.flush();
	}

	private void visitBuiltinTypes() throws Exception {
		// test builtin type adapters
		visit(new GregorianCalendar(2010, 2, 10).getTime());
		visit(new java.sql.Date(new GregorianCalendar(2010, 2, 10).getTimeInMillis()));
		visit(new SimpleDateFormat("YYYY-MM-dd").toPattern());
		visit(new GregorianCalendar(2010, 2, 10).getTime().toInstant());
	}

	private void visitRecursiveObject() throws Exception {
		// test cycle detection
		final RecursiveClass recursiveClass = new RecursiveClass(true, null);
		final RecursiveClass recursiveClass_child = new RecursiveClass(false, recursiveClass);
		recursiveClass.setOther(recursiveClass_child);
		visit(recursiveClass);
	}

	private void visitString() throws Exception {
		// test strings
		visit("");
		visit("s");
		visit(" ");

		visit("line1\nline2");

		visit("line1\nline2\nline3");

		visit("a\tb");
		visit("a	b");

		visit("a\\b");
		visit("a\"b");
		visit("a'b");
		visit("a\\\"b");
	}

	private void visitArrayOfInts() throws Exception {
		// test arrays of integers
		final int[] arr = { 8, 9 };
		visit(arr);

		final int[] arr2 = {};
		visit(arr2);

		final int[] arr3 = null;
		visit(arr3);

		final long[] arr4 = { 10l, 11l };
		visit(arr4);

		final short[] arr5 = { 12, 13 };
		visit(arr5);

		final byte[] arr6 = { 14, 15 };
		visit(arr6);

		final int[][] arr7 = { { 16, 17 }, { 18 }, null };
		visit(arr7);
	}

	private void visitListOfInts() throws Exception {
		// test lists of integers
		visit(Arrays.asList(1, 2));
		visit(Arrays.asList(3l, 4l));
		visit(Arrays.asList(5l, null));
		visit(Arrays.asList(6l, 7l));
	}

	private void visitInt() throws Exception {
		// test integers
		visit(1);
		visit(1l);
	}

	private void visitListOfBooleans() throws Exception {
		// test lists of booleans
		visit(Arrays.asList());
		visit(Arrays.asList(true));
		visit(Arrays.asList(true, false));
		visit(Arrays.asList(true, null));
		visit(Arrays.asList(Boolean.TRUE));
		visit(Arrays.asList(Boolean.TRUE, Boolean.FALSE));
	}

	private void visitBoolean() throws Exception {
		// test booleans
		visit(true);
		visit(false);
	}

	private void visitNull() throws Exception {
		// test null
		visit(null);
	}

	@Test
	public void testAll() throws Exception {
		standardOptions();
		assert FileUtils.contentEquals(last, ref);
	}

	public void visit(Object o) throws Exception {
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
