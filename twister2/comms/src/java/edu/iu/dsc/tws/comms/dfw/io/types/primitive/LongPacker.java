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
package edu.iu.dsc.tws.comms.dfw.io.types.primitive;

import java.nio.ByteBuffer;
import java.util.function.BiFunction;

import edu.iu.dsc.tws.comms.api.MessageType;

public final class LongPacker implements PrimitivePacker<Long> {

  private static volatile LongPacker instance;

  private LongPacker() {

  }

  public static LongPacker getInstance() {
    if (instance == null) {
      instance = new LongPacker();
    }
    return instance;
  }

  @Override
  public MessageType<Long> getMessageType() {
    return MessageType.LONG;
  }

  @Override
  public BiFunction<ByteBuffer, Long, ByteBuffer> getByteBufferAppendFunction() {
    return ByteBuffer::putLong;
  }

  @Override
  public BiFunction<ByteBuffer, Integer, Long> getByteBufferReadFunction() {
    return ByteBuffer::getLong;
  }
}
