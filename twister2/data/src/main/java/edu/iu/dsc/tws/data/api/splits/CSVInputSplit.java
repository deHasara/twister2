//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
package edu.iu.dsc.tws.data.api.splits;

import edu.iu.dsc.tws.api.data.Path;

public abstract class CSVInputSplit<OT> extends DelimitedInputSplit<String> {

  public CSVInputSplit(int num, Path file, long start, long length, String[] hosts) {
    super(num, file, start, length, hosts);
  }


}


//
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.Reader;
//import java.util.List;
//import java.util.logging.Logger;
//
//import com.opencsv.CSVParser;
//import com.opencsv.CSVParserBuilder;
//import com.opencsv.CSVReader;
//import com.opencsv.CSVReaderBuilder;
//
//import edu.iu.dsc.tws.api.config.Config;
//import edu.iu.dsc.tws.api.data.Path;
//import edu.iu.dsc.tws.api.exceptions.Twister2RuntimeException;
//import edu.iu.dsc.tws.data.api.formatters.FileInputPartitioner;
////import edu.iu.dsc.tws.data.utils.DataObjectConstants;
//
//public class CSVInputSplit extends FileInputSplit<Object> {
//
//  private static final Logger LOG = Logger.getLogger(CSVInputSplit.class.getName());
//
//  private static final int DEFAULT_READ_BUFFER_SIZE = 1024 * 1024;
//
//  public static final String DEFAULT_LINE_DELIMITER = "\n";
//  public static final String DEFAULT_FIELD_DELIMITER = ",";
//
//  private int bufferSize = -1;
//  protected transient int recordLength;
//
//  private transient int readPos;
//  private transient int limit;
//  private transient int commentCount;
//  private transient int invalidLineCount;
//
//  private long offset;
//
//  private transient byte[] readBuffer;
//  private transient byte[] wrapBuffer;
//  private transient byte[] currBuffer;
//
//  protected byte[] commentPrefix = null;
//  protected transient Object[] parsedValues;
//
//  private boolean end;
//
//  private boolean lineDelimiterIsLinebreak = false;
//
//  private boolean[] fieldIncluded;
//  private byte[] delimiter;
//  private String delimiterString;
//  private String charsetName;
//  private Object charset;
//  private boolean overLimit;
//  private boolean lenient;
//  private byte[] fieldDelim;
//  private transient FieldParser<?>[] fieldParsers;
//
//  private static final byte BACKSLASH = 92;
//  private static final Class<?>[] EMPTY_TYPES = new Class<?>[0];
//  private Class<?>[] fieldTypes = EMPTY_TYPES;
//
//  public CSVInputSplit(int num, Path file, long start, long length, String[] hosts) {
//    super(num, file, start, length, hosts);
//  }
//
//  public void setRecordLength(int recordLen) {
//    if (recordLen <= 0) {
//      throw new IllegalArgumentException("RecordLength must be larger than 0");
//    }
//    this.recordLength = recordLen;
//    if (this.bufferSize % recordLen != 0) {
//      int bufferFactor = 1;
//      if (this.bufferSize > 0) {
//        bufferFactor = bufferSize / recordLen;
//      } else {
//        bufferFactor = DEFAULT_READ_BUFFER_SIZE / recordLen;
//      }
//      if (bufferFactor >= 1) {
//        setBufferSize(recordLen * bufferFactor);
//      } else {
//        setBufferSize(recordLen * 8);
//      }
//    }
//  }
//
//  public int getBufferSize() {
//    return bufferSize;
//  }
//
//  public void setBufferSize(int buffSize) {
//    if (buffSize < 2) {
//      throw new IllegalArgumentException("Buffer size must be at least 2.");
//    }
//    this.bufferSize = buffSize;
//  }
//
//  @Override
//  public void configure(Config parameters) {
//    super.configure(parameters);
//    //int dataSize = Integer.parseInt(String.valueOf(parameters.get(DataObjectConstants.DSIZE)));
//    int dataSize = 122;
//    int recordLen = dataSize * Short.BYTES;
//    if (recordLen > 0) {
//      setRecordLength(recordLen);
//    }
//  }
//
//  @Override
//  public boolean reachedEnd() {
//    return this.end;
//  }
//
//  private void initBuffers() {
//    this.bufferSize = this.bufferSize <= 0 ? DEFAULT_READ_BUFFER_SIZE : this.bufferSize;
//    if (this.bufferSize % this.recordLength != 0) {
//      throw new IllegalArgumentException("Buffer size must be a multiple of the record length");
//    }
//
//    if (this.readBuffer == null || this.readBuffer.length != this.bufferSize) {
//      this.readBuffer = new byte[this.bufferSize];
//    }
//    if (this.wrapBuffer == null || this.wrapBuffer.length < 256) {
//      this.wrapBuffer = new byte[256];
//    }
//
//    this.readPos = 0;
//    this.limit = 0;
//    this.overLimit = false;
//    this.end = false;
//  }
//
//  /**
//   * To open the input split
//   * @throws IOException
//   */
//  public void open() throws IOException {
//    super.open();
//
//    CSVParser csvParser = new CSVParserBuilder()
//        .withSeparator(',')
//        .withIgnoreQuotations(true)
//        .build();
//
//    Reader reader = new FileReader("/home/kannan/opencsvexamples/input/ex.csv");
//    CSVReader csvReader = new CSVReaderBuilder(reader)
//        .withSkipLines(0) //'1' it will skip the header
//        .withCSVParser(csvParser)
//        .build();
//
//    List<String[]> list;
//    try {
//      list = csvReader.readAll();
//      LOG.info("%%% List Values:%%%" + list.size());
//      for (String[] first : list) {
//        for (String s : first) {
//          LOG.info("Person Values:" + s);
//        }
//      }
//      reader.close();
//      csvReader.close();
//    } catch (Exception ex) {
//      ex.printStackTrace();
//    }
//
//    /*initBuffers();
//    long recordMod = this.splitStart % this.recordLength;
//    if (recordMod != 0) {
//      this.offset = this.splitStart + this.recordLength - recordMod;
//      if (this.offset > this.splitStart + this.splitLength) {
//        this.end = true;
//      }
//    } else {
//      this.offset = splitStart;
//    }
//    if (this.splitStart != 0) {
//      this.stream.seek(offset);
//    }
//    fillBuffer(0);*/
//  }
//
//  public void open(Config cfg) throws IOException {
//    super.open(cfg);
//    this.configure(cfg);
//   /* initBuffers();
//    long recordMod = this.splitStart % this.recordLength;
//    if (recordMod != 0) {
//      this.offset = this.splitStart + this.recordLength - recordMod;
//      if (this.offset > this.splitStart + this.splitLength) {
//        this.end = true;
//      }
//    } else {
//      this.offset = splitStart;
//    }
//    if (this.splitStart != 0) {
//      this.stream.seek(offset);
//    }
//    fillBuffer(0);*/
//  }
//
//  private boolean fillBuffer(int fillOffset) throws IOException {
//    int maxReadLength = this.readBuffer.length - fillOffset;
//    if (this.splitLength == FileInputPartitioner.READ_WHOLE_SPLIT_FLAG) {
//      int read = this.stream.read(this.readBuffer, fillOffset, maxReadLength);
//      if (read == -1) {
//        this.stream.close();
//        this.stream = null;
//        return false;
//      } else {
//        this.readPos = fillOffset;
//        this.limit = read;
//        return true;
//      }
//    }
//
//    int toRead;
//    if (this.splitLength > 0) {
//      toRead = this.splitLength > maxReadLength ? maxReadLength : (int) this.splitLength;
//    } else {
//      toRead = maxReadLength;
//      this.overLimit = true;
//      return false;
//    }
//
//    int read = this.stream.read(this.readBuffer, fillOffset, toRead);
//    if (read == -1) {
//      this.stream.close();
//      this.stream = null;
//      return false;
//    } else {
//      this.splitLength -= read;
//      this.readPos = fillOffset;
//      this.limit = read + fillOffset;
//      return true;
//    }
//  }
//
//  @Override
//  public Object nextRecord(Object reuse) {
//    Object returnRecord;
//    do {
//      returnRecord = nextRecord(reuse);
//    } while (returnRecord == null && !reachedEnd());
//    return returnRecord;
//  }
//
//  public Object readRecord(Object reuse, byte[] bytes, int readoffset, int numBytes) {
//
//    //CSVParser csvParser = new CSVParserBuilder()
//    if (this.lineDelimiterIsLinebreak && numBytes > 0 && bytes[readoffset
//    + numBytes - 1] == '\r') {
//      //reduce the number of bytes so that the Carriage return is not taken as data
//      //numBytes--;
//    }
//
//    if (commentPrefix != null && commentPrefix.length <= numBytes) {
//      //check record for comments
//      boolean isComment = true;
//      for (int i = 0; i < commentPrefix.length; i++) {
//        if (commentPrefix[i] != bytes[readoffset + i]) {
//          isComment = false;
//          break;
//        }
//      }
//      if (isComment) {
//        this.commentCount++;
//        return null;
//      }
//    }
//
//    if (parseRecord(parsedValues, bytes, readoffset, numBytes)) {
//      return fillRecord(reuse, parsedValues);
//    } else {
//      this.invalidLineCount++;
//      return null;
//    }
//  }
//
//  protected Object fillRecord(Object reuse, Object[] parsedvalues) {
//    return null;
//  }
//
//  //TODO: Modify this part.
//  protected boolean parseRecord(Object[] holders, byte[] bytes, int recordOffset, int numBytes)
//      throws Twister2RuntimeException {
//
//    boolean[] fieldincluded = this.fieldIncluded;
//    int startPos = recordOffset;
//    final int parselimit = recordOffset + numBytes;
//
//    for (int field = 0, output = 0; field < fieldincluded.length; field++) {
//      if (startPos > parselimit || (startPos == parselimit && field != fieldincluded.length - 1))
//      {
//        if (lenient) {
//          return false;
//        } else {
//          throw new Twister2RuntimeException("Row too short: " + new String(bytes, recordOffset,
//              numBytes, getCharset()));
//        }
//      }
//
//      if (fieldincluded[field]) {
//        // parse field
//        @SuppressWarnings("unchecked")
//        FieldParser<Object> parser = (FieldParser<Object>) this.fieldParsers[output];
//        Object reuse = holders[output];
//        startPos = parser.resetErrorStateAndParse(bytes, startPos, parselimit,
//            this.fieldDelim, reuse);
//        holders[output] = parser.getLastResult();
//
//        // check parse result
//        if (startPos < 0) {
//          if (lenient) {
//            return false;
//          } else {
//            String lineAsString = new String(bytes, recordOffset, numBytes, getCharset());
//            throw new Twister2RuntimeException("Line could not be parsed: '" + lineAsString
//            + "'\n"
//                + "in file: " + currentSplit.getPath());
//          }
//        } else if (startPos == parselimit
//            && field != fieldincluded.length - 1
//            && !FieldParser.endsWithDelimiter(bytes, startPos - 1, fieldDelim)) {
//          if (lenient) {
//            return false;
//          } else {
//            throw new Twister2RuntimeException("Row too short: " + new String(bytes,
//                recordOffset, numBytes));
//          }
//        }
//        output++;
//      } else {
//        // skip field
//        startPos = skipFields(bytes, startPos, parselimit, this.fieldDelim);
//        if (startPos < 0) {
//          if (!lenient) {
//            String lineAsString = new String(bytes, recordOffset, numBytes, getCharset());
//            throw new Twister2RuntimeException("Line could not be parsed: '" + lineAsString
//            + "'\n"
//                + "in file: " + currentSplit.getPath());
//          } else {
//            return false;
//          }
//        } else if (startPos == parselimit
//            && field != fieldincluded.length - 1
//            && !FieldParser.endsWithDelimiter(bytes, startPos - 1, fieldDelim)) {
//          if (lenient) {
//            return false;
//          } else {
//            throw new Twister2RuntimeException("Row too short: " + new String(bytes,
//                recordOffset, numBytes));
//          }
//        }
//      }
//    }
//    return true;
//  }
//
//  //TODO: Modify this part.
//  protected int skipFields(byte[] bytes, int startPos, int skipLimit, byte[] delim) {
//    int i = startPos;
//    final int delimLimit = skipLimit - delim.length + 1;
//
//    boolean quotedStringParsing = false;
//    byte quoteCharacter = 0;
//    if (quotedStringParsing && bytes[i] == quoteCharacter) {
//      i++;
//      while (i < skipLimit && (bytes[i] != quoteCharacter || bytes[i - 1] == BACKSLASH)) {
//        i++;
//      }
//      i++;
//
//      if (i == skipLimit) {
//        return skipLimit;
//      } else if (i < delimLimit && FieldParser.delimiterNext(bytes, i, delim)) {
//        return i + delim.length;
//      } else {
//        return -1;
//      }
//    } else {
//      while (i < delimLimit && !FieldParser.delimiterNext(bytes, i, delim)) {
//        i++;
//      }
//
//      if (i >= delimLimit) {
//        return skipLimit;
//      } else {
//        return i + delim.length;
//      }
//    }
//  }
//}
