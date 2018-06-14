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
package edu.iu.dsc.tws.comms.shuffle;

import java.util.List;

public class OpenFilePart {
  private List<KeyValue> keyValues;

  private int readOffSet;

  private int fileSize;

  private String fileName;

  private int keyValueIndex = 0;

  public OpenFilePart(List<KeyValue> keyValues, int readOffSet, int fileSize, String fileName) {
    this.keyValues = keyValues;
    this.readOffSet = readOffSet;
    this.fileSize = fileSize;
    this.fileName = fileName;
  }

  public int getReadOffSet() {
    return readOffSet;
  }

  public int getFileSize() {
    return fileSize;
  }

  public String getFileName() {
    return fileName;
  }

  public KeyValue next() {
    KeyValue kv = keyValues.get(keyValueIndex);
    keyValueIndex++;
    return kv;
  }

  public boolean hasNext() {
    return keyValueIndex < keyValues.size();
  }
}