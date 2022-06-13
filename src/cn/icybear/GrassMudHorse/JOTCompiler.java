package cn.icybear.GrassMudHorse;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;

import javax.script.ScriptException;

/**
 * JOT（Just Out of Time）编译器，用于解析，验证草泥马语文件是否合法。
 * 另外本类的main方法可以吧草泥马语文件编译成伪汇编代码，跟适宜阅读。
 *
 * @author Bearice
 *
 */
public class JOTCompiler {

    Reader reader;
    String fileName = "<Unknown>";
    int lineNum = 1;
    int colNum = 1;
    int lastCodeLine;
    int lastCodeCol;

    public String getFileName() {
	return fileName;
    }

    public void setFileName(String fileName) {
	this.fileName = fileName;
    }

    public JOTCompiler(Reader reader) {
	this.reader = reader;
    }

    public GMHCodeSection compile() throws IOException, ScriptException {
	LinkedList<OpCode> codes = new LinkedList<OpCode>();
	LinkedList<BigInteger> args = new LinkedList<BigInteger>();
	HashMap<BigInteger, Integer> labels = new HashMap<BigInteger, Integer>();
	LinkedList<Integer> lineNums = new LinkedList<Integer>();
	LinkedList<Integer> colNums = new LinkedList<Integer>();
	LinkedList<Integer> labelCheckPoints = new LinkedList<Integer>();
	OpCode code;
	while ((code = readCode()) != OpCode.X_EOF) {
	    BigInteger arg = null;
	    switch (code) {
	    case F_CALL:
	    case F_JMP:
	    case F_JNEG:
	    case F_JZ:
		labelCheckPoints.add(codes.size());
		arg = readUnsignedArg();
		break;
	    case F_MARK:
		arg = readUnsignedArg();
		if (labels.containsKey(arg)) {
		    int i = labels.get(arg);
		    throwScriptException(lastCodeLine, lastCodeCol, fileName,
			    "Duplicated label found: [%s] against defined at line %d, column %d", arg, lineNums.get(i),
			    colNums.get(i));
		} else
		    labels.put(arg, codes.size());
		break;
	    default:
		if (code.needArg)
		    arg = readArg();
	    }
	    codes.add(code);
	    args.add(arg);
	    lineNums.add(lastCodeLine);
	    colNums.add(lastCodeCol);
	}
	GMHCodeSection ret = new GMHCodeSection();
	ret.codes = codes.toArray(new OpCode[0]);
	ret.args = args.toArray(new BigInteger[0]);
	ret.colNums = colNums.toArray(new Integer[0]);
	ret.lineNums = lineNums.toArray(new Integer[0]);
	ret.labels = labels;
	ret.fileName = fileName;
	for (int idx : labelCheckPoints) {
	    if (!labels.containsKey(ret.args[idx]))
		throwScriptException(ret.lineNums[idx], ret.colNums[idx], fileName, "Missing label: %s", ret.args[idx]);
	    else
		ret.args[idx] = BigInteger.valueOf(labels.get(ret.args[idx]));
	}
	return ret;
    }

    public int decompileNext() throws IOException, ScriptException {
	System.out.printf("%d:%d ", lineNum, colNum);
	OpCode code = readCode();
	BigInteger arg = null;
	switch (code) {
	case F_CALL:
	case F_JMP:
	case F_JNEG:
	case F_JZ:
	case F_MARK:
	    arg = readUnsignedArg();
	    break;
	default:
	    if (code.needArg)
		arg = readArg();
	}
	System.out.printf("%n%s", code);
	if (arg != null) {
	    System.out.print(" " + arg);
	}
	System.out.println();
	return code == OpCode.X_EOF ? -1 : 0;
    }

    private BigInteger readUnsignedArg() throws ScriptException, IOException {
	BigInteger ret = null;
	int val;
	while ((val = readNext()) != '马') {
	    if (ret == null)
		ret = BigInteger.ZERO;
	    switch (val) {
	    case '草':
		ret = ret.shiftLeft(1);
		break;
	    case '泥':
		ret = ret.shiftLeft(1).add(BigInteger.ONE);
		break;
	    default:
		throwUnexpectedChar(val, createExpected('草', '泥', '马'));
	    }
	}
	if (ret == null) {
	    throwUnexpectedChar(val, createExpected('草', '泥'));
	}
	return ret;
    }

    private BigInteger readArg() throws ScriptException, IOException {

	// int count = 1;
	BigInteger sign = BigInteger.ZERO;
	int val = readNext();
	switch (val) {
	case '草':
	    sign = BigInteger.ONE;
	    break;
	case '泥':
	    sign = BigInteger.ONE.negate();
	    break;
	default:
	    throwUnexpectedChar(val, new int[] { '草', '泥' });
	}
	BigInteger ret = BigInteger.ZERO;
	while ((val = readNext()) != '马') {
	    switch (val) {
	    case '草':
		ret = ret.shiftLeft(1);
		break;
	    case '泥':
		ret = ret.shiftLeft(1).add(BigInteger.ONE);
		break;
	    default:
		throwUnexpectedChar(val, createExpected('草', '泥', '马'));
	    }
	}
	return ret.multiply(sign);
    }

    private OpCode readCode() throws IOException, ScriptException {
	lastCodeLine = lineNum;
	lastCodeCol = colNum;
	int imp = readNext();
	int[] expected = createExpected();
	outer: switch (imp) {
	case -1:
	    return OpCode.X_EOF;
	case '蟹':
	    // End
	    return OpCode.F_END;
	case '草':
	    // Stack
	    imp = readNext();
	    switch (imp) {
	    case '草':
		// Push
		return OpCode.S_PUSH;
	    case '泥':
		imp = readNext();
		switch (imp) {
		case '草':
		    // Copy
		    return OpCode.S_COPY;
		case '马':
		    // Slide
		    return OpCode.S_SLIDE;
		}
		expected = createExpected('草', '马');
		break outer;
	    case '马':
		imp = readNext();
		switch (imp) {
		case '草':
		    // Dup
		    return OpCode.S_DUP;
		case '泥':
		    // Swap
		    return OpCode.S_SWAP;
		case '马':
		    // Pop
		    return OpCode.S_POP;
		}
	    }
	    expected = createExpected('草', '泥', '马');
	    break outer;
	case '马':
	    // Flow Control
	    imp = readNext();
	    switch (imp) {
	    case '草':
		imp = readNext();
		switch (imp) {
		case '草':
		    // Mark
		    return OpCode.F_MARK;
		case '泥':
		    // Call
		    return OpCode.F_CALL;
		case '马':
		    // Jump
		    return OpCode.F_JMP;
		}
		expected = createExpected('草', '泥', '马');
		break outer;
	    case '泥':
		imp = readNext();
		switch (imp) {
		case '草':
		    // Jump if Zero
		    return OpCode.F_JZ;
		case '泥':
		    // Jump if Negative
		    return OpCode.F_JNEG;
		case '马':
		    // Return
		    return OpCode.F_RET;
		}
		expected = createExpected('草', '泥', '马');
		break outer;
	    case '马':
		imp = readNext();
		switch (imp) {
		case '马':
		    // End
		    return OpCode.F_END;
		}
		expected = createExpected('马');
		break outer;
	    }
	    expected = createExpected('草', '泥', '马');
	    break outer;
	case '泥':
	    imp = readNext();
	    switch (imp) {
	    case '草':
		// Arithmetic
		imp = readNext();
		switch (imp) {
		case '草':
		    imp = readNext();
		    switch (imp) {
		    case '草':
			// Addition
			return OpCode.A_ADD;
		    case '泥':
			// Subtraction
			return OpCode.A_SUB;
		    case '马':
			// Multiplication
			return OpCode.A_MUL;
		    }
		    expected = createExpected('草', '泥', '马');
		    break outer;
		case '泥':
		    imp = readNext();
		    switch (imp) {
		    case '草':
			// Division
			return OpCode.A_DIV;
		    case '泥':
			// Modulo
			return OpCode.A_MOD;
		    }
		    expected = createExpected('草', '泥');
		    break outer;
		}
		expected = createExpected('草', '泥');
		break outer;
	    case '泥':
		// Heap access
		imp = readNext();
		switch (imp) {
		case '草':
		    // Store
		    return OpCode.H_PUT;
		case '泥':
		    // Retrieve
		    return OpCode.H_GET;
		}
		expected = createExpected('草', '泥');
		break outer;
	    case '马':
		// IO Control
		imp = readNext();
		switch (imp) {
		case '草':
		    imp = readNext();
		    switch (imp) {
		    case '草':
			// Output the character at the top of the stack
			return OpCode.O_CHR;
		    case '泥':
			// Output the number at the top of the stack
			return OpCode.O_INT;
		    }
		    expected = createExpected('草', '泥');
		    break outer;
		case '泥':
		    imp = readNext();
		    switch (imp) {
		    case '草':
			// Read a character and place it in the location given
			// by the top of the stack
			return OpCode.I_CHR;
		    case '泥':
			// Read a number and place it in the location given by
			// the top of the stack
			return OpCode.I_INT;
		    }
		    expected = createExpected('草', '泥');
		    break outer;
		}
		expected = createExpected('草', '泥');
		break outer;
	    }
	    expected = createExpected('草', '泥', '马', '蟹', -1);
	}
	throwUnexpectedChar(imp, expected);
	return null;
    }

    private int[] createExpected(int... arg) {
	return arg;
    }

    static boolean readEcho = false;

    int readNext() throws IOException {
	boolean lastRiver = false;
	while (true) {
	    int ret = reader.read();
	    colNum++;
	    switch (ret) {
	    case '草':
	    case '泥':
	    case '马':
		if (readEcho)
		    System.out.print((char) ret);
	    case -1:
		return ret;
	    case '蟹':
		if (lastRiver) {
		    if (readEcho)
			System.out.print((char) ret);
		    return ret;
		} else
		    continue;
	    case '河':
		if (readEcho)
		    System.out.print((char) ret);
		lastRiver = true;
		continue;
	    case '\n':
		lineNum++;
		colNum = 1;
	    }
	    lastRiver = false;
	}
    }

    protected void throwUnexpectedChar(int ch, int... arg) throws ScriptException {
	StringBuilder exp = new StringBuilder();
	if (arg.length != 0) {
	    exp.append(" Expecting: ");
	    for (int c : arg) {
		exp.append('<');
		switch (c) {
		case '\n':
		    exp.append("EOL");
		    break;
		case -1:
		    exp.append("EOF");
		    break;
		default:
		    exp.append((char) c);
		}
		exp.append('>');
		exp.append(' ');
	    }
	}
	if (ch == -1) {
	    throwScriptException("Unexpected char: <EOF>%s", exp);
	} else if (ch == '\n') {
	    throwScriptException("Unexpected char: <EOL>%s", exp);
	} else {
	    throwScriptException("Unexpected char: <%c>%s", ch, exp);
	}
    }

    protected void throwScriptException(int line, int col, String file, String fmt, Object... arg)
	    throws ScriptException {
	throw new ScriptException(String.format(fmt, arg), file, line, col);
    }

    protected void throwScriptException(String fmt, Object... arg) throws ScriptException {
	throw new ScriptException(String.format(fmt, arg), fileName, lineNum, colNum);
    }

    /**
     * @param args
     * @throws IOException
     * @throws ScriptException
     */
    public static void main(String[] args) throws IOException, ScriptException {
	if (args.length < 1) {
	    System.out.println("Usage: JOTCompiler <gmh_file> [output]");
	    return;
	}
	String file = args[0];
	if (args.length >= 2) {
	    System.setOut(new PrintStream(new FileOutputStream(args[1])));
	}
	readEcho = true;
	BufferedReader reader = new BufferedReader(new FileReader(file));
	JOTCompiler compiler = new JOTCompiler(reader);
	compiler.setFileName(file);
	while (compiler.decompileNext() != -1) {
	}
    }
}
