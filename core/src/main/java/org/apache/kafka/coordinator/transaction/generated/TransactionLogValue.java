/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// THIS CODE IS AUTOMATICALLY GENERATED.  DO NOT EDIT.

package org.apache.kafka.coordinator.transaction.generated;

import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.apache.kafka.common.protocol.*;
import org.apache.kafka.common.protocol.Readable;
import org.apache.kafka.common.protocol.types.*;
import org.apache.kafka.common.utils.ByteUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.apache.kafka.common.protocol.types.Field.TaggedFieldsSection;


public class TransactionLogValue implements ApiMessage {
    long producerId;
    long previousProducerId;
    long nextProducerId;
    short producerEpoch;
    int transactionTimeoutMs;
    byte transactionStatus;
    List<PartitionsSchema> transactionPartitions;
    long transactionLastUpdateTimestampMs;
    long transactionStartTimestampMs;
    short clientTransactionVersion;
    private List<RawTaggedField> _unknownTaggedFields;
    
    public static final Schema SCHEMA_0 =
        new Schema(
            new Field("producer_id", Type.INT64, "Producer id in use by the transactional id."),
            new Field("producer_epoch", Type.INT16, "Epoch associated with the producer id."),
            new Field("transaction_timeout_ms", Type.INT32, "Transaction timeout in milliseconds."),
            new Field("transaction_status", Type.INT8, "TransactionState the transaction is in."),
            new Field("transaction_partitions", ArrayOf.nullable(PartitionsSchema.SCHEMA_0), "Partitions involved in the transaction."),
            new Field("transaction_last_update_timestamp_ms", Type.INT64, "Time the transaction was last updated."),
            new Field("transaction_start_timestamp_ms", Type.INT64, "Time the transaction was started.")
        );
    
    public static final Schema SCHEMA_1 =
        new Schema(
            new Field("producer_id", Type.INT64, "Producer id in use by the transactional id."),
            new Field("producer_epoch", Type.INT16, "Epoch associated with the producer id."),
            new Field("transaction_timeout_ms", Type.INT32, "Transaction timeout in milliseconds."),
            new Field("transaction_status", Type.INT8, "TransactionState the transaction is in."),
            new Field("transaction_partitions", CompactArrayOf.nullable(PartitionsSchema.SCHEMA_1), "Partitions involved in the transaction."),
            new Field("transaction_last_update_timestamp_ms", Type.INT64, "Time the transaction was last updated."),
            new Field("transaction_start_timestamp_ms", Type.INT64, "Time the transaction was started."),
            TaggedFieldsSection.of(
                0, new Field("previous_producer_id", Type.INT64, "Producer id used by the last committed transaction."),
                1, new Field("next_producer_id", Type.INT64, "Latest producer ID sent to the producer for the given transactional id."),
                2, new Field("client_transaction_version", Type.INT16, "The transaction version used by the client.")
            )
        );
    
    public static final Schema[] SCHEMAS = new Schema[] {
        SCHEMA_0,
        SCHEMA_1
    };
    
    public static final short LOWEST_SUPPORTED_VERSION = 0;
    public static final short HIGHEST_SUPPORTED_VERSION = 1;
    
    public TransactionLogValue(Readable _readable, short _version) {
        read(_readable, _version);
    }
    
    public TransactionLogValue() {
        this.producerId = 0L;
        this.previousProducerId = -1L;
        this.nextProducerId = -1L;
        this.producerEpoch = (short) 0;
        this.transactionTimeoutMs = 0;
        this.transactionStatus = (byte) 0;
        this.transactionPartitions = new ArrayList<PartitionsSchema>(0);
        this.transactionLastUpdateTimestampMs = 0L;
        this.transactionStartTimestampMs = 0L;
        this.clientTransactionVersion = (short) 0;
    }
    
    @Override
    public short apiKey() {
        return -1;
    }
    
    @Override
    public short lowestSupportedVersion() {
        return 0;
    }
    
    @Override
    public short highestSupportedVersion() {
        return 1;
    }
    
    @Override
    public final void read(Readable _readable, short _version) {
        this.producerId = _readable.readLong();
        this.previousProducerId = -1L;
        this.nextProducerId = -1L;
        this.producerEpoch = _readable.readShort();
        this.transactionTimeoutMs = _readable.readInt();
        this.transactionStatus = _readable.readByte();
        {
            if (_version >= 1) {
                int arrayLength;
                arrayLength = _readable.readUnsignedVarint() - 1;
                if (arrayLength < 0) {
                    this.transactionPartitions = null;
                } else {
                    if (arrayLength > _readable.remaining()) {
                        throw new RuntimeException("Tried to allocate a collection of size " + arrayLength + ", but there are only " + _readable.remaining() + " bytes remaining.");
                    }
                    ArrayList<PartitionsSchema> newCollection = new ArrayList<>(arrayLength);
                    for (int i = 0; i < arrayLength; i++) {
                        newCollection.add(new PartitionsSchema(_readable, _version));
                    }
                    this.transactionPartitions = newCollection;
                }
            } else {
                int arrayLength;
                arrayLength = _readable.readInt();
                if (arrayLength < 0) {
                    this.transactionPartitions = null;
                } else {
                    if (arrayLength > _readable.remaining()) {
                        throw new RuntimeException("Tried to allocate a collection of size " + arrayLength + ", but there are only " + _readable.remaining() + " bytes remaining.");
                    }
                    ArrayList<PartitionsSchema> newCollection = new ArrayList<>(arrayLength);
                    for (int i = 0; i < arrayLength; i++) {
                        newCollection.add(new PartitionsSchema(_readable, _version));
                    }
                    this.transactionPartitions = newCollection;
                }
            }
        }
        this.transactionLastUpdateTimestampMs = _readable.readLong();
        this.transactionStartTimestampMs = _readable.readLong();
        this.clientTransactionVersion = (short) 0;
        this._unknownTaggedFields = null;
        if (_version >= 1) {
            int _numTaggedFields = _readable.readUnsignedVarint();
            for (int _i = 0; _i < _numTaggedFields; _i++) {
                int _tag = _readable.readUnsignedVarint();
                int _size = _readable.readUnsignedVarint();
                switch (_tag) {
                    case 0: {
                        this.previousProducerId = _readable.readLong();
                        break;
                    }
                    case 1: {
                        this.nextProducerId = _readable.readLong();
                        break;
                    }
                    case 2: {
                        this.clientTransactionVersion = _readable.readShort();
                        break;
                    }
                    default:
                        this._unknownTaggedFields = _readable.readUnknownTaggedField(this._unknownTaggedFields, _tag, _size);
                        break;
                }
            }
        }
    }
    
    @Override
    public void write(Writable _writable, ObjectSerializationCache _cache, short _version) {
        int _numTaggedFields = 0;
        _writable.writeLong(producerId);
        if (_version >= 1) {
            if (this.previousProducerId != -1L) {
                _numTaggedFields++;
            }
        } else {
            if (this.previousProducerId != -1L) {
                throw new UnsupportedVersionException("Attempted to write a non-default previousProducerId at version " + _version);
            }
        }
        if (_version >= 1) {
            if (this.nextProducerId != -1L) {
                _numTaggedFields++;
            }
        } else {
            if (this.nextProducerId != -1L) {
                throw new UnsupportedVersionException("Attempted to write a non-default nextProducerId at version " + _version);
            }
        }
        _writable.writeShort(producerEpoch);
        _writable.writeInt(transactionTimeoutMs);
        _writable.writeByte(transactionStatus);
        if (_version >= 1) {
            if (transactionPartitions == null) {
                _writable.writeUnsignedVarint(0);
            } else {
                _writable.writeUnsignedVarint(transactionPartitions.size() + 1);
                for (PartitionsSchema transactionPartitionsElement : transactionPartitions) {
                    transactionPartitionsElement.write(_writable, _cache, _version);
                }
            }
        } else {
            if (transactionPartitions == null) {
                _writable.writeInt(-1);
            } else {
                _writable.writeInt(transactionPartitions.size());
                for (PartitionsSchema transactionPartitionsElement : transactionPartitions) {
                    transactionPartitionsElement.write(_writable, _cache, _version);
                }
            }
        }
        _writable.writeLong(transactionLastUpdateTimestampMs);
        _writable.writeLong(transactionStartTimestampMs);
        if (_version >= 1) {
            if (this.clientTransactionVersion != (short) 0) {
                _numTaggedFields++;
            }
        } else {
            if (this.clientTransactionVersion != (short) 0) {
                throw new UnsupportedVersionException("Attempted to write a non-default clientTransactionVersion at version " + _version);
            }
        }
        RawTaggedFieldWriter _rawWriter = RawTaggedFieldWriter.forFields(_unknownTaggedFields);
        _numTaggedFields += _rawWriter.numFields();
        if (_version >= 1) {
            _writable.writeUnsignedVarint(_numTaggedFields);
            {
                if (this.previousProducerId != -1L) {
                    _writable.writeUnsignedVarint(0);
                    _writable.writeUnsignedVarint(8);
                    _writable.writeLong(previousProducerId);
                }
            }
            {
                if (this.nextProducerId != -1L) {
                    _writable.writeUnsignedVarint(1);
                    _writable.writeUnsignedVarint(8);
                    _writable.writeLong(nextProducerId);
                }
            }
            {
                if (this.clientTransactionVersion != (short) 0) {
                    _writable.writeUnsignedVarint(2);
                    _writable.writeUnsignedVarint(2);
                    _writable.writeShort(clientTransactionVersion);
                }
            }
            _rawWriter.writeRawTags(_writable, Integer.MAX_VALUE);
        } else {
            if (_numTaggedFields > 0) {
                throw new UnsupportedVersionException("Tagged fields were set, but version " + _version + " of this message does not support them.");
            }
        }
    }
    
    @Override
    public void addSize(MessageSizeAccumulator _size, ObjectSerializationCache _cache, short _version) {
        int _numTaggedFields = 0;
        _size.addBytes(8);
        if (_version >= 1) {
            if (this.previousProducerId != -1L) {
                _numTaggedFields++;
                _size.addBytes(1);
                _size.addBytes(1);
                _size.addBytes(8);
            }
        }
        if (_version >= 1) {
            if (this.nextProducerId != -1L) {
                _numTaggedFields++;
                _size.addBytes(1);
                _size.addBytes(1);
                _size.addBytes(8);
            }
        }
        _size.addBytes(2);
        _size.addBytes(4);
        _size.addBytes(1);
        if (transactionPartitions == null) {
            if (_version >= 1) {
                _size.addBytes(1);
            } else {
                _size.addBytes(4);
            }
        } else {
            if (_version >= 1) {
                _size.addBytes(ByteUtils.sizeOfUnsignedVarint(transactionPartitions.size() + 1));
            } else {
                _size.addBytes(4);
            }
            for (PartitionsSchema transactionPartitionsElement : transactionPartitions) {
                transactionPartitionsElement.addSize(_size, _cache, _version);
            }
        }
        _size.addBytes(8);
        _size.addBytes(8);
        if (_version >= 1) {
            if (this.clientTransactionVersion != (short) 0) {
                _numTaggedFields++;
                _size.addBytes(1);
                _size.addBytes(1);
                _size.addBytes(2);
            }
        }
        if (_unknownTaggedFields != null) {
            _numTaggedFields += _unknownTaggedFields.size();
            for (RawTaggedField _field : _unknownTaggedFields) {
                _size.addBytes(ByteUtils.sizeOfUnsignedVarint(_field.tag()));
                _size.addBytes(ByteUtils.sizeOfUnsignedVarint(_field.size()));
                _size.addBytes(_field.size());
            }
        }
        if (_version >= 1) {
            _size.addBytes(ByteUtils.sizeOfUnsignedVarint(_numTaggedFields));
        } else {
            if (_numTaggedFields > 0) {
                throw new UnsupportedVersionException("Tagged fields were set, but version " + _version + " of this message does not support them.");
            }
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TransactionLogValue)) return false;
        TransactionLogValue other = (TransactionLogValue) obj;
        if (producerId != other.producerId) return false;
        if (previousProducerId != other.previousProducerId) return false;
        if (nextProducerId != other.nextProducerId) return false;
        if (producerEpoch != other.producerEpoch) return false;
        if (transactionTimeoutMs != other.transactionTimeoutMs) return false;
        if (transactionStatus != other.transactionStatus) return false;
        if (this.transactionPartitions == null) {
            if (other.transactionPartitions != null) return false;
        } else {
            if (!this.transactionPartitions.equals(other.transactionPartitions)) return false;
        }
        if (transactionLastUpdateTimestampMs != other.transactionLastUpdateTimestampMs) return false;
        if (transactionStartTimestampMs != other.transactionStartTimestampMs) return false;
        if (clientTransactionVersion != other.clientTransactionVersion) return false;
        return MessageUtil.compareRawTaggedFields(_unknownTaggedFields, other._unknownTaggedFields);
    }
    
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 31 * hashCode + ((int) (producerId >> 32) ^ (int) producerId);
        hashCode = 31 * hashCode + ((int) (previousProducerId >> 32) ^ (int) previousProducerId);
        hashCode = 31 * hashCode + ((int) (nextProducerId >> 32) ^ (int) nextProducerId);
        hashCode = 31 * hashCode + producerEpoch;
        hashCode = 31 * hashCode + transactionTimeoutMs;
        hashCode = 31 * hashCode + transactionStatus;
        hashCode = 31 * hashCode + (transactionPartitions == null ? 0 : transactionPartitions.hashCode());
        hashCode = 31 * hashCode + ((int) (transactionLastUpdateTimestampMs >> 32) ^ (int) transactionLastUpdateTimestampMs);
        hashCode = 31 * hashCode + ((int) (transactionStartTimestampMs >> 32) ^ (int) transactionStartTimestampMs);
        hashCode = 31 * hashCode + clientTransactionVersion;
        return hashCode;
    }
    
    @Override
    public TransactionLogValue duplicate() {
        TransactionLogValue _duplicate = new TransactionLogValue();
        _duplicate.producerId = producerId;
        _duplicate.previousProducerId = previousProducerId;
        _duplicate.nextProducerId = nextProducerId;
        _duplicate.producerEpoch = producerEpoch;
        _duplicate.transactionTimeoutMs = transactionTimeoutMs;
        _duplicate.transactionStatus = transactionStatus;
        if (transactionPartitions == null) {
            _duplicate.transactionPartitions = null;
        } else {
            ArrayList<PartitionsSchema> newTransactionPartitions = new ArrayList<PartitionsSchema>(transactionPartitions.size());
            for (PartitionsSchema _element : transactionPartitions) {
                newTransactionPartitions.add(_element.duplicate());
            }
            _duplicate.transactionPartitions = newTransactionPartitions;
        }
        _duplicate.transactionLastUpdateTimestampMs = transactionLastUpdateTimestampMs;
        _duplicate.transactionStartTimestampMs = transactionStartTimestampMs;
        _duplicate.clientTransactionVersion = clientTransactionVersion;
        return _duplicate;
    }
    
    @Override
    public String toString() {
        return "TransactionLogValue("
            + "producerId=" + producerId
            + ", previousProducerId=" + previousProducerId
            + ", nextProducerId=" + nextProducerId
            + ", producerEpoch=" + producerEpoch
            + ", transactionTimeoutMs=" + transactionTimeoutMs
            + ", transactionStatus=" + transactionStatus
            + ", transactionPartitions=" + ((transactionPartitions == null) ? "null" : MessageUtil.deepToString(transactionPartitions.iterator()))
            + ", transactionLastUpdateTimestampMs=" + transactionLastUpdateTimestampMs
            + ", transactionStartTimestampMs=" + transactionStartTimestampMs
            + ", clientTransactionVersion=" + clientTransactionVersion
            + ")";
    }
    
    public long producerId() {
        return this.producerId;
    }
    
    public long previousProducerId() {
        return this.previousProducerId;
    }
    
    public long nextProducerId() {
        return this.nextProducerId;
    }
    
    public short producerEpoch() {
        return this.producerEpoch;
    }
    
    public int transactionTimeoutMs() {
        return this.transactionTimeoutMs;
    }
    
    public byte transactionStatus() {
        return this.transactionStatus;
    }
    
    public List<PartitionsSchema> transactionPartitions() {
        return this.transactionPartitions;
    }
    
    public long transactionLastUpdateTimestampMs() {
        return this.transactionLastUpdateTimestampMs;
    }
    
    public long transactionStartTimestampMs() {
        return this.transactionStartTimestampMs;
    }
    
    public short clientTransactionVersion() {
        return this.clientTransactionVersion;
    }
    
    @Override
    public List<RawTaggedField> unknownTaggedFields() {
        if (_unknownTaggedFields == null) {
            _unknownTaggedFields = new ArrayList<>(0);
        }
        return _unknownTaggedFields;
    }
    
    public TransactionLogValue setProducerId(long v) {
        this.producerId = v;
        return this;
    }
    
    public TransactionLogValue setPreviousProducerId(long v) {
        this.previousProducerId = v;
        return this;
    }
    
    public TransactionLogValue setNextProducerId(long v) {
        this.nextProducerId = v;
        return this;
    }
    
    public TransactionLogValue setProducerEpoch(short v) {
        this.producerEpoch = v;
        return this;
    }
    
    public TransactionLogValue setTransactionTimeoutMs(int v) {
        this.transactionTimeoutMs = v;
        return this;
    }
    
    public TransactionLogValue setTransactionStatus(byte v) {
        this.transactionStatus = v;
        return this;
    }
    
    public TransactionLogValue setTransactionPartitions(List<PartitionsSchema> v) {
        this.transactionPartitions = v;
        return this;
    }
    
    public TransactionLogValue setTransactionLastUpdateTimestampMs(long v) {
        this.transactionLastUpdateTimestampMs = v;
        return this;
    }
    
    public TransactionLogValue setTransactionStartTimestampMs(long v) {
        this.transactionStartTimestampMs = v;
        return this;
    }
    
    public TransactionLogValue setClientTransactionVersion(short v) {
        this.clientTransactionVersion = v;
        return this;
    }
    
    public static class PartitionsSchema implements Message {
        String topic;
        List<Integer> partitionIds;
        private List<RawTaggedField> _unknownTaggedFields;
        
        public static final Schema SCHEMA_0 =
            new Schema(
                new Field("topic", Type.STRING, "Topic involved in the transaction."),
                new Field("partition_ids", new ArrayOf(Type.INT32), "Partition ids involved in the transaction.")
            );
        
        public static final Schema SCHEMA_1 =
            new Schema(
                new Field("topic", Type.COMPACT_STRING, "Topic involved in the transaction."),
                new Field("partition_ids", new CompactArrayOf(Type.INT32), "Partition ids involved in the transaction."),
                TaggedFieldsSection.of(
                )
            );
        
        public static final Schema[] SCHEMAS = new Schema[] {
            SCHEMA_0,
            SCHEMA_1
        };
        
        public static final short LOWEST_SUPPORTED_VERSION = 0;
        public static final short HIGHEST_SUPPORTED_VERSION = 1;
        
        public PartitionsSchema(Readable _readable, short _version) {
            read(_readable, _version);
        }
        
        public PartitionsSchema() {
            this.topic = "";
            this.partitionIds = new ArrayList<Integer>(0);
        }
        
        
        @Override
        public short lowestSupportedVersion() {
            return 0;
        }
        
        @Override
        public short highestSupportedVersion() {
            return 1;
        }
        
        @Override
        public final void read(Readable _readable, short _version) {
            if (_version > 1) {
                throw new UnsupportedVersionException("Can't read version " + _version + " of PartitionsSchema");
            }
            {
                int length;
                if (_version >= 1) {
                    length = _readable.readUnsignedVarint() - 1;
                } else {
                    length = _readable.readShort();
                }
                if (length < 0) {
                    throw new RuntimeException("non-nullable field topic was serialized as null");
                } else if (length > 0x7fff) {
                    throw new RuntimeException("string field topic had invalid length " + length);
                } else {
                    this.topic = _readable.readString(length);
                }
            }
            {
                int arrayLength;
                if (_version >= 1) {
                    arrayLength = _readable.readUnsignedVarint() - 1;
                } else {
                    arrayLength = _readable.readInt();
                }
                if (arrayLength < 0) {
                    throw new RuntimeException("non-nullable field partitionIds was serialized as null");
                } else {
                    if (arrayLength > _readable.remaining()) {
                        throw new RuntimeException("Tried to allocate a collection of size " + arrayLength + ", but there are only " + _readable.remaining() + " bytes remaining.");
                    }
                    ArrayList<Integer> newCollection = new ArrayList<>(arrayLength);
                    for (int i = 0; i < arrayLength; i++) {
                        newCollection.add(_readable.readInt());
                    }
                    this.partitionIds = newCollection;
                }
            }
            this._unknownTaggedFields = null;
            if (_version >= 1) {
                int _numTaggedFields = _readable.readUnsignedVarint();
                for (int _i = 0; _i < _numTaggedFields; _i++) {
                    int _tag = _readable.readUnsignedVarint();
                    int _size = _readable.readUnsignedVarint();
                    switch (_tag) {
                        default:
                            this._unknownTaggedFields = _readable.readUnknownTaggedField(this._unknownTaggedFields, _tag, _size);
                            break;
                    }
                }
            }
        }
        
        @Override
        public void write(Writable _writable, ObjectSerializationCache _cache, short _version) {
            int _numTaggedFields = 0;
            {
                byte[] _stringBytes = _cache.getSerializedValue(topic);
                if (_version >= 1) {
                    _writable.writeUnsignedVarint(_stringBytes.length + 1);
                } else {
                    _writable.writeShort((short) _stringBytes.length);
                }
                _writable.writeByteArray(_stringBytes);
            }
            if (_version >= 1) {
                _writable.writeUnsignedVarint(partitionIds.size() + 1);
            } else {
                _writable.writeInt(partitionIds.size());
            }
            for (Integer partitionIdsElement : partitionIds) {
                _writable.writeInt(partitionIdsElement);
            }
            RawTaggedFieldWriter _rawWriter = RawTaggedFieldWriter.forFields(_unknownTaggedFields);
            _numTaggedFields += _rawWriter.numFields();
            if (_version >= 1) {
                _writable.writeUnsignedVarint(_numTaggedFields);
                _rawWriter.writeRawTags(_writable, Integer.MAX_VALUE);
            } else {
                if (_numTaggedFields > 0) {
                    throw new UnsupportedVersionException("Tagged fields were set, but version " + _version + " of this message does not support them.");
                }
            }
        }
        
        @Override
        public void addSize(MessageSizeAccumulator _size, ObjectSerializationCache _cache, short _version) {
            int _numTaggedFields = 0;
            if (_version > 1) {
                throw new UnsupportedVersionException("Can't size version " + _version + " of PartitionsSchema");
            }
            {
                byte[] _stringBytes = topic.getBytes(StandardCharsets.UTF_8);
                if (_stringBytes.length > 0x7fff) {
                    throw new RuntimeException("'topic' field is too long to be serialized");
                }
                _cache.cacheSerializedValue(topic, _stringBytes);
                if (_version >= 1) {
                    _size.addBytes(_stringBytes.length + ByteUtils.sizeOfUnsignedVarint(_stringBytes.length + 1));
                } else {
                    _size.addBytes(_stringBytes.length + 2);
                }
            }
            {
                if (_version >= 1) {
                    _size.addBytes(ByteUtils.sizeOfUnsignedVarint(partitionIds.size() + 1));
                } else {
                    _size.addBytes(4);
                }
                _size.addBytes(partitionIds.size() * 4);
            }
            if (_unknownTaggedFields != null) {
                _numTaggedFields += _unknownTaggedFields.size();
                for (RawTaggedField _field : _unknownTaggedFields) {
                    _size.addBytes(ByteUtils.sizeOfUnsignedVarint(_field.tag()));
                    _size.addBytes(ByteUtils.sizeOfUnsignedVarint(_field.size()));
                    _size.addBytes(_field.size());
                }
            }
            if (_version >= 1) {
                _size.addBytes(ByteUtils.sizeOfUnsignedVarint(_numTaggedFields));
            } else {
                if (_numTaggedFields > 0) {
                    throw new UnsupportedVersionException("Tagged fields were set, but version " + _version + " of this message does not support them.");
                }
            }
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PartitionsSchema)) return false;
            PartitionsSchema other = (PartitionsSchema) obj;
            if (this.topic == null) {
                if (other.topic != null) return false;
            } else {
                if (!this.topic.equals(other.topic)) return false;
            }
            if (this.partitionIds == null) {
                if (other.partitionIds != null) return false;
            } else {
                if (!this.partitionIds.equals(other.partitionIds)) return false;
            }
            return MessageUtil.compareRawTaggedFields(_unknownTaggedFields, other._unknownTaggedFields);
        }
        
        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode = 31 * hashCode + (topic == null ? 0 : topic.hashCode());
            hashCode = 31 * hashCode + (partitionIds == null ? 0 : partitionIds.hashCode());
            return hashCode;
        }
        
        @Override
        public PartitionsSchema duplicate() {
            PartitionsSchema _duplicate = new PartitionsSchema();
            _duplicate.topic = topic;
            ArrayList<Integer> newPartitionIds = new ArrayList<Integer>(partitionIds.size());
            for (Integer _element : partitionIds) {
                newPartitionIds.add(_element);
            }
            _duplicate.partitionIds = newPartitionIds;
            return _duplicate;
        }
        
        @Override
        public String toString() {
            return "PartitionsSchema("
                + "topic=" + ((topic == null) ? "null" : "'" + topic.toString() + "'")
                + ", partitionIds=" + MessageUtil.deepToString(partitionIds.iterator())
                + ")";
        }
        
        public String topic() {
            return this.topic;
        }
        
        public List<Integer> partitionIds() {
            return this.partitionIds;
        }
        
        @Override
        public List<RawTaggedField> unknownTaggedFields() {
            if (_unknownTaggedFields == null) {
                _unknownTaggedFields = new ArrayList<>(0);
            }
            return _unknownTaggedFields;
        }
        
        public PartitionsSchema setTopic(String v) {
            this.topic = v;
            return this;
        }
        
        public PartitionsSchema setPartitionIds(List<Integer> v) {
            this.partitionIds = v;
            return this;
        }
    }
}
