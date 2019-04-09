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

import org.apache.commons.lang3.tuple.Pair;

import edu.iu.dsc.tws.comms.api.DataPacker;
import edu.iu.dsc.tws.comms.api.KeyPacker;
import edu.iu.dsc.tws.comms.api.MessageType;
import edu.iu.dsc.tws.comms.dfw.DataBuffer;
import edu.iu.dsc.tws.comms.dfw.InMessage;
import edu.iu.dsc.tws.comms.dfw.io.SerializeState;

@SuppressWarnings("ReturnValueIgnored")
public interface PrimitivePacker<T> extends KeyPacker<T>, DataPacker<T> {

  MessageType<T> getMessageType();

  BiFunction<ByteBuffer, T, ByteBuffer> getByteBufferAppendFunction();

  BiFunction<ByteBuffer, Integer, T> getByteBufferReadFunction();

  @Override
  default boolean writeKeyToBuffer(T key, ByteBuffer targetBuffer, SerializeState state) {
    int unitDataSize = this.getMessageType().getUnitSizeInBytes();
    if (targetBuffer.remaining() > unitDataSize) {
      this.getByteBufferAppendFunction().apply(targetBuffer, key);
      state.setTotalBytes(state.getTotalBytes() + unitDataSize);
      state.setCurrentHeaderLength(state.getCurrentHeaderLength() + unitDataSize);
      state.setKeySize(unitDataSize);
      return true;
    }
    return false;
  }

  @Override
  default boolean writeDataToBuffer(T data, ByteBuffer targetBuffer, SerializeState state) {
    int unitDataSize = this.getMessageType().getUnitSizeInBytes();
    if (targetBuffer.remaining() > unitDataSize) {
      this.getByteBufferAppendFunction().apply(targetBuffer, data);
      state.setTotalBytes(state.getTotalBytes() + unitDataSize);
      //since it's a single value.
      state.setData(null);
      state.setBytesCopied(0);
      return true;
    }
    return false;
  }

  @Override
  default Pair<Integer, Integer> getKeyLength(InMessage message,
                                              DataBuffer buffer, int location) {
    return Pair.of(this.getMessageType().getUnitSizeInBytes(), 0);
  }

  @Override
  default int packKey(T key, SerializeState state) {
    return this.packData(key, state);
  }

  @Override
  default int packData(T data, SerializeState state) {
    return this.getMessageType().getUnitSizeInBytes();
  }

  @Override
  default int readDataFromBuffer(InMessage currentMessage, int currentLocation,
                                 DataBuffer buffers, int currentObjectLength) {
    ByteBuffer byteBuffer = buffers.getByteBuffer();
    int remaining = buffers.getSize() - currentLocation;
    if (remaining >= this.getMessageType().getUnitSizeInBytes()) {
      T val = this.getByteBufferReadFunction().apply(byteBuffer, currentLocation);
      currentMessage.setDeserializingKey(val);
      return this.getMessageType().getUnitSizeInBytes();
    } else {
      return 0;
    }
  }

  @Override
  default int readKeyFromBuffer(InMessage currentMessage, int currentLocation,
                                DataBuffer buffer, int currentObjectLength) {
    return this.readDataFromBuffer(
        currentMessage,
        currentLocation,
        buffer,
        currentObjectLength
    );
  }

  @Override
  default boolean isKeyHeaderRequired() {
    //will be false for primitive
    return false;
  }

  @Override
  default T initializeUnPackKeyObject(int size) {
    //will be null for primitive
    return null;
  }

  @Override
  default T initializeUnPackDataObject(int length) {
    //will be null for primitive
    return null;
  }
}
