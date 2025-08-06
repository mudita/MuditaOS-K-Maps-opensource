package net.osmand;

import java.io.IOException;
import java.io.OutputStream;

public interface StreamWriter {
   void write(OutputStream var1, IProgress var2) throws IOException;
}
