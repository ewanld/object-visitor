package objectvisitor;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;

import com.github.ewanld.objectvisitor.ObjectVisitor;
import com.github.ewanld.objectvisitor.VisitEvent;

public class Json5Dumper extends ObjectVisitor {
	private final Writer writer;
	private static final Pattern unquotedKeyPattern = Pattern.compile("[A-Za-z0-9_]+");
	private static final String[] REPLACEMENT_CHARS;
	static {
		REPLACEMENT_CHARS = new String[128];

		for (int i = 0; i <= 0x1f; i++) {
			REPLACEMENT_CHARS[i] = String.format("\\u%04x", i);
		}
		REPLACEMENT_CHARS['"'] = "\\\"";
		REPLACEMENT_CHARS['\\'] = "\\\\";
		REPLACEMENT_CHARS['\t'] = "\\t";
		REPLACEMENT_CHARS['\b'] = "\\b";
		REPLACEMENT_CHARS['\n'] = "\\\n";
		REPLACEMENT_CHARS['\r'] = "\\r";
		REPLACEMENT_CHARS['\f'] = "\\f";
	}

	public Json5Dumper(Writer writer) throws IOException {
		this.writer = writer;
	}

	public void writeln(String s) throws IOException {
		writeIndent();
		writer.write(s);
		writer.write("\n");
	}

	private void writeIndent() throws IOException {
		final String indentString = repeatString(" ", getNestingLevel() * 4);
		writer.write(indentString);
	}

	public void write(String s) throws IOException {
		writeIndent();
		writer.write(s);
	}

	public static String repeatString(String what, int howmany) {
		final StringBuilder out = new StringBuilder(what.length() * howmany);
		for (int i = 0; i < howmany; i++)
			out.append(what);
		return out.toString();
	}

	@Override
	public void visitNull() throws Exception {
		writer.write("null");
	}

	@Override
	public void visitBoolean(Boolean o) throws Exception {
		writer.write(o ? "true" : "false");
	}

	@Override
	public void visitLong(Long o) throws Exception {
		writer.write(o.toString());
	}

	@Override
	public void visitInteger(Integer o) throws Exception {
		writer.write(o.toString());
	}

	@Override
	public void visitShort(Short o) throws Exception {
		writer.write(o.toString());
	}

	@Override
	public void visitByte(Byte o) throws Exception {
		writer.write(o.toString());
	}

	@Override
	public void visitChar(Character o) throws Exception {
		visitString(o.toString());
	}

	@Override
	public void visitFloat(Float o) throws Exception {
		writer.write(o.toString());
	}

	@Override
	public void visitDouble(Double o) throws Exception {
		writer.write(o.toString());
	}

	@Override
	public void visitKey(Object property, KeyType type, Object parent) throws Exception {
		final String propertyName = property == null ? "" : property.toString();
		final boolean unquoted = unquotedKeyPattern.matcher(propertyName).matches();
		if (!unquoted) writer.write("\"");
		writer.write(propertyName);
		if (!unquoted) writer.write("\"");
		writer.write(": ");
	}

	@Override
	public void onIterableEvent(VisitEvent event, Iterable<?> iterable) throws Exception {
		if (event == VisitEvent.ENTER) {
			writer.write("[\n");
		} else if (event == VisitEvent.LEAVE) {
			writeIndent();
			writer.write("]");
		} else if (event == VisitEvent.BEFORE_CHILD) {
			writeIndent();
		} else if (event == VisitEvent.AFTER_CHILD) {
			writer.write(",\n");
		}
	}

	@Override
	public void onKeyValueObjectEvent(VisitEvent event, KeyValueObjectType type, Object object) throws Exception {
		if (event == VisitEvent.ENTER) {
			writer.write("{\n");
		} else if (event == VisitEvent.LEAVE) {
			writeIndent();
			writer.write("}");
		} else if (event == VisitEvent.BEFORE_CHILD) {
			writeIndent();
		} else if (event == VisitEvent.AFTER_CHILD) {
			writer.write(",\n");
		}
	}

	@Override
	public void visitString(String value) throws IOException {
		writer.write("\"");
		final int length = value.length();
		int last = 0;
		for (int i = 0; i < length; i++) {
			final char c = value.charAt(i);
			String replacement;
			if (c < 128) {
				replacement = REPLACEMENT_CHARS[c];
				if (replacement == null) {
					continue;
				}
			} else {
				continue;
			}
			if (last < i) {
				writer.write(value, last, i - last);
			}
			writer.write(replacement);
			last = i + 1;
		}
		if (last < length) {
			writer.write(value, last, length - last);
		}
		writer.write("\"");
	}

	public Writer getWriter() {
		return writer;
	}

	@Override
	public void visitEnum(Enum<?> e) throws Exception {
		visitString(e.toString());
	}
}
