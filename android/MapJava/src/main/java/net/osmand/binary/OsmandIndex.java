package net.osmand.binary;

import com.google.protobuf.AbstractParser;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.GeneratedMessageLite;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.LazyStringArrayList;
import com.google.protobuf.LazyStringList;
import com.google.protobuf.MessageLiteOrBuilder;
import com.google.protobuf.Parser;
import com.google.protobuf.UnmodifiableLazyStringList;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OsmandIndex {
   private OsmandIndex() {
   }

   public static void registerAllExtensions(ExtensionRegistryLite registry) {
   }

   public static final class AddressPart extends GeneratedMessageLite implements OsmandIndex.AddressPartOrBuilder {
      private static final OsmandIndex.AddressPart defaultInstance = new OsmandIndex.AddressPart(true);
      public static Parser<OsmandIndex.AddressPart> PARSER = new AbstractParser<OsmandIndex.AddressPart>() {
         public OsmandIndex.AddressPart parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return new OsmandIndex.AddressPart(input, extensionRegistry);
         }
      };
      private int bitField0_;
      public static final int SIZE_FIELD_NUMBER = 1;
      private long size_;
      public static final int OFFSET_FIELD_NUMBER = 2;
      private long offset_;
      public static final int NAME_FIELD_NUMBER = 3;
      private Object name_;
      public static final int NAMEEN_FIELD_NUMBER = 4;
      private Object nameEn_;
      public static final int INDEXNAMEOFFSET_FIELD_NUMBER = 5;
      private int indexNameOffset_;
      public static final int CITIES_FIELD_NUMBER = 8;
      private List<OsmandIndex.CityBlock> cities_;
      public static final int ADDITIONALTAGS_FIELD_NUMBER = 9;
      private LazyStringList additionalTags_;
      private byte memoizedIsInitialized = -1;
      private int memoizedSerializedSize = -1;
      private static final long serialVersionUID = 0L;

      private AddressPart(GeneratedMessageLite.Builder builder) {
         super(builder);
      }

      private AddressPart(boolean noInit) {
      }

      public static OsmandIndex.AddressPart getDefaultInstance() {
         return defaultInstance;
      }

      public OsmandIndex.AddressPart getDefaultInstanceForType() {
         return defaultInstance;
      }

      private AddressPart(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         this.initFields();
         int mutable_bitField0_ = 0;

         try {
            boolean done = false;

            while(!done) {
               int tag = input.readTag();
               switch(tag) {
                  case 0:
                     done = true;
                     break;
                  case 8:
                     this.bitField0_ |= 1;
                     this.size_ = input.readInt64();
                     break;
                  case 16:
                     this.bitField0_ |= 2;
                     this.offset_ = input.readInt64();
                     break;
                  case 26:
                     this.bitField0_ |= 4;
                     this.name_ = input.readBytes();
                     break;
                  case 34:
                     this.bitField0_ |= 8;
                     this.nameEn_ = input.readBytes();
                     break;
                  case 40:
                     this.bitField0_ |= 16;
                     this.indexNameOffset_ = input.readInt32();
                     break;
                  case 66:
                     if ((mutable_bitField0_ & 32) != 32) {
                        this.cities_ = new ArrayList<>();
                        mutable_bitField0_ |= 32;
                     }

                     this.cities_.add(input.readMessage(OsmandIndex.CityBlock.PARSER, extensionRegistry));
                     break;
                  case 74:
                     if ((mutable_bitField0_ & 64) != 64) {
                        this.additionalTags_ = new LazyStringArrayList();
                        mutable_bitField0_ |= 64;
                     }

                     this.additionalTags_.add(input.readBytes());
                     break;
                  default:
                     if (!this.parseUnknownField(input, extensionRegistry, tag)) {
                        done = true;
                     }
               }
            }
         } catch (InvalidProtocolBufferException var10) {
            throw var10.setUnfinishedMessage(this);
         } catch (IOException var11) {
            throw new InvalidProtocolBufferException(var11.getMessage()).setUnfinishedMessage(this);
         } finally {
            if ((mutable_bitField0_ & 32) == 32) {
               this.cities_ = Collections.unmodifiableList(this.cities_);
            }

            if ((mutable_bitField0_ & 64) == 64) {
               this.additionalTags_ = new UnmodifiableLazyStringList(this.additionalTags_);
            }

            this.makeExtensionsImmutable();
         }
      }

      @Override
      public Parser<OsmandIndex.AddressPart> getParserForType() {
         return PARSER;
      }

      @Override
      public boolean hasSize() {
         return (this.bitField0_ & 1) == 1;
      }

      @Override
      public long getSize() {
         return this.size_;
      }

      @Override
      public boolean hasOffset() {
         return (this.bitField0_ & 2) == 2;
      }

      @Override
      public long getOffset() {
         return this.offset_;
      }

      @Override
      public boolean hasName() {
         return (this.bitField0_ & 4) == 4;
      }

      @Override
      public String getName() {
         Object ref = this.name_;
         if (ref instanceof String) {
            return (String)ref;
         } else {
            ByteString bs = (ByteString)ref;
            String s = bs.toStringUtf8();
            if (bs.isValidUtf8()) {
               this.name_ = s;
            }

            return s;
         }
      }

      @Override
      public ByteString getNameBytes() {
         Object ref = this.name_;
         if (ref instanceof String) {
            ByteString b = ByteString.copyFromUtf8((String)ref);
            this.name_ = b;
            return b;
         } else {
            return (ByteString)ref;
         }
      }

      @Override
      public boolean hasNameEn() {
         return (this.bitField0_ & 8) == 8;
      }

      @Override
      public String getNameEn() {
         Object ref = this.nameEn_;
         if (ref instanceof String) {
            return (String)ref;
         } else {
            ByteString bs = (ByteString)ref;
            String s = bs.toStringUtf8();
            if (bs.isValidUtf8()) {
               this.nameEn_ = s;
            }

            return s;
         }
      }

      @Override
      public ByteString getNameEnBytes() {
         Object ref = this.nameEn_;
         if (ref instanceof String) {
            ByteString b = ByteString.copyFromUtf8((String)ref);
            this.nameEn_ = b;
            return b;
         } else {
            return (ByteString)ref;
         }
      }

      @Override
      public boolean hasIndexNameOffset() {
         return (this.bitField0_ & 16) == 16;
      }

      @Override
      public int getIndexNameOffset() {
         return this.indexNameOffset_;
      }

      @Override
      public List<OsmandIndex.CityBlock> getCitiesList() {
         return this.cities_;
      }

      public List<? extends OsmandIndex.CityBlockOrBuilder> getCitiesOrBuilderList() {
         return this.cities_;
      }

      @Override
      public int getCitiesCount() {
         return this.cities_.size();
      }

      @Override
      public OsmandIndex.CityBlock getCities(int index) {
         return this.cities_.get(index);
      }

      public OsmandIndex.CityBlockOrBuilder getCitiesOrBuilder(int index) {
         return this.cities_.get(index);
      }

      @Override
      public List<String> getAdditionalTagsList() {
         return this.additionalTags_;
      }

      @Override
      public int getAdditionalTagsCount() {
         return this.additionalTags_.size();
      }

      @Override
      public String getAdditionalTags(int index) {
         return this.additionalTags_.get(index);
      }

      @Override
      public ByteString getAdditionalTagsBytes(int index) {
         return this.additionalTags_.getByteString(index);
      }

      private void initFields() {
         this.size_ = 0L;
         this.offset_ = 0L;
         this.name_ = "";
         this.nameEn_ = "";
         this.indexNameOffset_ = 0;
         this.cities_ = Collections.emptyList();
         this.additionalTags_ = LazyStringArrayList.EMPTY;
      }

      @Override
      public final boolean isInitialized() {
         byte isInitialized = this.memoizedIsInitialized;
         if (isInitialized != -1) {
            return isInitialized == 1;
         } else if (!this.hasSize()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasOffset()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else {
            for(int i = 0; i < this.getCitiesCount(); ++i) {
               if (!this.getCities(i).isInitialized()) {
                  this.memoizedIsInitialized = 0;
                  return false;
               }
            }

            this.memoizedIsInitialized = 1;
            return true;
         }
      }

      @Override
      public void writeTo(CodedOutputStream output) throws IOException {
         this.getSerializedSize();
         if ((this.bitField0_ & 1) == 1) {
            output.writeInt64(1, this.size_);
         }

         if ((this.bitField0_ & 2) == 2) {
            output.writeInt64(2, this.offset_);
         }

         if ((this.bitField0_ & 4) == 4) {
            output.writeBytes(3, this.getNameBytes());
         }

         if ((this.bitField0_ & 8) == 8) {
            output.writeBytes(4, this.getNameEnBytes());
         }

         if ((this.bitField0_ & 16) == 16) {
            output.writeInt32(5, this.indexNameOffset_);
         }

         for(int i = 0; i < this.cities_.size(); ++i) {
            output.writeMessage(8, this.cities_.get(i));
         }

         for(int i = 0; i < this.additionalTags_.size(); ++i) {
            output.writeBytes(9, this.additionalTags_.getByteString(i));
         }
      }

      @Override
      public int getSerializedSize() {
         int size = this.memoizedSerializedSize;
         if (size != -1) {
            return size;
         } else {
            size = 0;
            if ((this.bitField0_ & 1) == 1) {
               size += CodedOutputStream.computeInt64Size(1, this.size_);
            }

            if ((this.bitField0_ & 2) == 2) {
               size += CodedOutputStream.computeInt64Size(2, this.offset_);
            }

            if ((this.bitField0_ & 4) == 4) {
               size += CodedOutputStream.computeBytesSize(3, this.getNameBytes());
            }

            if ((this.bitField0_ & 8) == 8) {
               size += CodedOutputStream.computeBytesSize(4, this.getNameEnBytes());
            }

            if ((this.bitField0_ & 16) == 16) {
               size += CodedOutputStream.computeInt32Size(5, this.indexNameOffset_);
            }

            for(int i = 0; i < this.cities_.size(); ++i) {
               size += CodedOutputStream.computeMessageSize(8, this.cities_.get(i));
            }

            int dataSize = 0;

            for(int i = 0; i < this.additionalTags_.size(); ++i) {
               dataSize += CodedOutputStream.computeBytesSizeNoTag(this.additionalTags_.getByteString(i));
            }

            size += dataSize;
            size += 1 * this.getAdditionalTagsList().size();
            this.memoizedSerializedSize = size;
            return size;
         }
      }

      @Override
      protected Object writeReplace() throws ObjectStreamException {
         return super.writeReplace();
      }

      public static OsmandIndex.AddressPart parseFrom(ByteString data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.AddressPart parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.AddressPart parseFrom(byte[] data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.AddressPart parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.AddressPart parseFrom(InputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.AddressPart parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.AddressPart parseDelimitedFrom(InputStream input) throws IOException {
         return PARSER.parseDelimitedFrom(input);
      }

      public static OsmandIndex.AddressPart parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseDelimitedFrom(input, extensionRegistry);
      }

      public static OsmandIndex.AddressPart parseFrom(CodedInputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.AddressPart parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.AddressPart.Builder newBuilder() {
         return OsmandIndex.AddressPart.Builder.create();
      }

      public OsmandIndex.AddressPart.Builder newBuilderForType() {
         return newBuilder();
      }

      public static OsmandIndex.AddressPart.Builder newBuilder(OsmandIndex.AddressPart prototype) {
         return newBuilder().mergeFrom(prototype);
      }

      public OsmandIndex.AddressPart.Builder toBuilder() {
         return newBuilder(this);
      }

      static {
         defaultInstance.initFields();
      }

      public static final class Builder
         extends GeneratedMessageLite.Builder<OsmandIndex.AddressPart, OsmandIndex.AddressPart.Builder>
         implements OsmandIndex.AddressPartOrBuilder {
         private int bitField0_;
         private long size_;
         private long offset_;
         private Object name_ = "";
         private Object nameEn_ = "";
         private int indexNameOffset_;
         private List<OsmandIndex.CityBlock> cities_ = Collections.emptyList();
         private LazyStringList additionalTags_ = LazyStringArrayList.EMPTY;

         private Builder() {
            this.maybeForceBuilderInitialization();
         }

         private void maybeForceBuilderInitialization() {
         }

         private static OsmandIndex.AddressPart.Builder create() {
            return new OsmandIndex.AddressPart.Builder();
         }

         public OsmandIndex.AddressPart.Builder clear() {
            super.clear();
            this.size_ = 0L;
            this.bitField0_ &= -2;
            this.offset_ = 0L;
            this.bitField0_ &= -3;
            this.name_ = "";
            this.bitField0_ &= -5;
            this.nameEn_ = "";
            this.bitField0_ &= -9;
            this.indexNameOffset_ = 0;
            this.bitField0_ &= -17;
            this.cities_ = Collections.emptyList();
            this.bitField0_ &= -33;
            this.additionalTags_ = LazyStringArrayList.EMPTY;
            this.bitField0_ &= -65;
            return this;
         }

         public OsmandIndex.AddressPart.Builder clone() {
            return create().mergeFrom(this.buildPartial());
         }

         public OsmandIndex.AddressPart getDefaultInstanceForType() {
            return OsmandIndex.AddressPart.getDefaultInstance();
         }

         public OsmandIndex.AddressPart build() {
            OsmandIndex.AddressPart result = this.buildPartial();
            if (!result.isInitialized()) {
               throw newUninitializedMessageException(result);
            } else {
               return result;
            }
         }

         public OsmandIndex.AddressPart buildPartial() {
            OsmandIndex.AddressPart result = new OsmandIndex.AddressPart(this);
            int from_bitField0_ = this.bitField0_;
            int to_bitField0_ = 0;
            if ((from_bitField0_ & 1) == 1) {
               to_bitField0_ |= 1;
            }

            result.size_ = this.size_;
            if ((from_bitField0_ & 2) == 2) {
               to_bitField0_ |= 2;
            }

            result.offset_ = this.offset_;
            if ((from_bitField0_ & 4) == 4) {
               to_bitField0_ |= 4;
            }

            result.name_ = this.name_;
            if ((from_bitField0_ & 8) == 8) {
               to_bitField0_ |= 8;
            }

            result.nameEn_ = this.nameEn_;
            if ((from_bitField0_ & 16) == 16) {
               to_bitField0_ |= 16;
            }

            result.indexNameOffset_ = this.indexNameOffset_;
            if ((this.bitField0_ & 32) == 32) {
               this.cities_ = Collections.unmodifiableList(this.cities_);
               this.bitField0_ &= -33;
            }

            result.cities_ = this.cities_;
            if ((this.bitField0_ & 64) == 64) {
               this.additionalTags_ = new UnmodifiableLazyStringList(this.additionalTags_);
               this.bitField0_ &= -65;
            }

            result.additionalTags_ = this.additionalTags_;
            result.bitField0_ = to_bitField0_;
            return result;
         }

         public OsmandIndex.AddressPart.Builder mergeFrom(OsmandIndex.AddressPart other) {
            if (other == OsmandIndex.AddressPart.getDefaultInstance()) {
               return this;
            } else {
               if (other.hasSize()) {
                  this.setSize(other.getSize());
               }

               if (other.hasOffset()) {
                  this.setOffset(other.getOffset());
               }

               if (other.hasName()) {
                  this.bitField0_ |= 4;
                  this.name_ = other.name_;
               }

               if (other.hasNameEn()) {
                  this.bitField0_ |= 8;
                  this.nameEn_ = other.nameEn_;
               }

               if (other.hasIndexNameOffset()) {
                  this.setIndexNameOffset(other.getIndexNameOffset());
               }

               if (!other.cities_.isEmpty()) {
                  if (this.cities_.isEmpty()) {
                     this.cities_ = other.cities_;
                     this.bitField0_ &= -33;
                  } else {
                     this.ensureCitiesIsMutable();
                     this.cities_.addAll(other.cities_);
                  }
               }

               if (!other.additionalTags_.isEmpty()) {
                  if (this.additionalTags_.isEmpty()) {
                     this.additionalTags_ = other.additionalTags_;
                     this.bitField0_ &= -65;
                  } else {
                     this.ensureAdditionalTagsIsMutable();
                     this.additionalTags_.addAll(other.additionalTags_);
                  }
               }

               return this;
            }
         }

         @Override
         public final boolean isInitialized() {
            if (!this.hasSize()) {
               return false;
            } else if (!this.hasOffset()) {
               return false;
            } else {
               for(int i = 0; i < this.getCitiesCount(); ++i) {
                  if (!this.getCities(i).isInitialized()) {
                     return false;
                  }
               }

               return true;
            }
         }

         public OsmandIndex.AddressPart.Builder mergeFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            OsmandIndex.AddressPart parsedMessage = null;

            try {
               parsedMessage = OsmandIndex.AddressPart.PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (InvalidProtocolBufferException var8) {
               parsedMessage = (OsmandIndex.AddressPart)var8.getUnfinishedMessage();
               throw var8;
            } finally {
               if (parsedMessage != null) {
                  this.mergeFrom(parsedMessage);
               }
            }

            return this;
         }

         @Override
         public boolean hasSize() {
            return (this.bitField0_ & 1) == 1;
         }

         @Override
         public long getSize() {
            return this.size_;
         }

         public OsmandIndex.AddressPart.Builder setSize(long value) {
            this.bitField0_ |= 1;
            this.size_ = value;
            return this;
         }

         public OsmandIndex.AddressPart.Builder clearSize() {
            this.bitField0_ &= -2;
            this.size_ = 0L;
            return this;
         }

         @Override
         public boolean hasOffset() {
            return (this.bitField0_ & 2) == 2;
         }

         @Override
         public long getOffset() {
            return this.offset_;
         }

         public OsmandIndex.AddressPart.Builder setOffset(long value) {
            this.bitField0_ |= 2;
            this.offset_ = value;
            return this;
         }

         public OsmandIndex.AddressPart.Builder clearOffset() {
            this.bitField0_ &= -3;
            this.offset_ = 0L;
            return this;
         }

         @Override
         public boolean hasName() {
            return (this.bitField0_ & 4) == 4;
         }

         @Override
         public String getName() {
            Object ref = this.name_;
            if (!(ref instanceof String)) {
               String s = ((ByteString)ref).toStringUtf8();
               this.name_ = s;
               return s;
            } else {
               return (String)ref;
            }
         }

         @Override
         public ByteString getNameBytes() {
            Object ref = this.name_;
            if (ref instanceof String) {
               ByteString b = ByteString.copyFromUtf8((String)ref);
               this.name_ = b;
               return b;
            } else {
               return (ByteString)ref;
            }
         }

         public OsmandIndex.AddressPart.Builder setName(String value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.bitField0_ |= 4;
               this.name_ = value;
               return this;
            }
         }

         public OsmandIndex.AddressPart.Builder clearName() {
            this.bitField0_ &= -5;
            this.name_ = OsmandIndex.AddressPart.getDefaultInstance().getName();
            return this;
         }

         public OsmandIndex.AddressPart.Builder setNameBytes(ByteString value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.bitField0_ |= 4;
               this.name_ = value;
               return this;
            }
         }

         @Override
         public boolean hasNameEn() {
            return (this.bitField0_ & 8) == 8;
         }

         @Override
         public String getNameEn() {
            Object ref = this.nameEn_;
            if (!(ref instanceof String)) {
               String s = ((ByteString)ref).toStringUtf8();
               this.nameEn_ = s;
               return s;
            } else {
               return (String)ref;
            }
         }

         @Override
         public ByteString getNameEnBytes() {
            Object ref = this.nameEn_;
            if (ref instanceof String) {
               ByteString b = ByteString.copyFromUtf8((String)ref);
               this.nameEn_ = b;
               return b;
            } else {
               return (ByteString)ref;
            }
         }

         public OsmandIndex.AddressPart.Builder setNameEn(String value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.bitField0_ |= 8;
               this.nameEn_ = value;
               return this;
            }
         }

         public OsmandIndex.AddressPart.Builder clearNameEn() {
            this.bitField0_ &= -9;
            this.nameEn_ = OsmandIndex.AddressPart.getDefaultInstance().getNameEn();
            return this;
         }

         public OsmandIndex.AddressPart.Builder setNameEnBytes(ByteString value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.bitField0_ |= 8;
               this.nameEn_ = value;
               return this;
            }
         }

         @Override
         public boolean hasIndexNameOffset() {
            return (this.bitField0_ & 16) == 16;
         }

         @Override
         public int getIndexNameOffset() {
            return this.indexNameOffset_;
         }

         public OsmandIndex.AddressPart.Builder setIndexNameOffset(int value) {
            this.bitField0_ |= 16;
            this.indexNameOffset_ = value;
            return this;
         }

         public OsmandIndex.AddressPart.Builder clearIndexNameOffset() {
            this.bitField0_ &= -17;
            this.indexNameOffset_ = 0;
            return this;
         }

         private void ensureCitiesIsMutable() {
            if ((this.bitField0_ & 32) != 32) {
               this.cities_ = new ArrayList<>(this.cities_);
               this.bitField0_ |= 32;
            }
         }

         @Override
         public List<OsmandIndex.CityBlock> getCitiesList() {
            return Collections.unmodifiableList(this.cities_);
         }

         @Override
         public int getCitiesCount() {
            return this.cities_.size();
         }

         @Override
         public OsmandIndex.CityBlock getCities(int index) {
            return this.cities_.get(index);
         }

         public OsmandIndex.AddressPart.Builder setCities(int index, OsmandIndex.CityBlock value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureCitiesIsMutable();
               this.cities_.set(index, value);
               return this;
            }
         }

         public OsmandIndex.AddressPart.Builder setCities(int index, OsmandIndex.CityBlock.Builder builderForValue) {
            this.ensureCitiesIsMutable();
            this.cities_.set(index, builderForValue.build());
            return this;
         }

         public OsmandIndex.AddressPart.Builder addCities(OsmandIndex.CityBlock value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureCitiesIsMutable();
               this.cities_.add(value);
               return this;
            }
         }

         public OsmandIndex.AddressPart.Builder addCities(int index, OsmandIndex.CityBlock value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureCitiesIsMutable();
               this.cities_.add(index, value);
               return this;
            }
         }

         public OsmandIndex.AddressPart.Builder addCities(OsmandIndex.CityBlock.Builder builderForValue) {
            this.ensureCitiesIsMutable();
            this.cities_.add(builderForValue.build());
            return this;
         }

         public OsmandIndex.AddressPart.Builder addCities(int index, OsmandIndex.CityBlock.Builder builderForValue) {
            this.ensureCitiesIsMutable();
            this.cities_.add(index, builderForValue.build());
            return this;
         }

         public OsmandIndex.AddressPart.Builder addAllCities(Iterable<? extends OsmandIndex.CityBlock> values) {
            this.ensureCitiesIsMutable();
            GeneratedMessageLite.Builder.addAll(values, this.cities_);
            return this;
         }

         public OsmandIndex.AddressPart.Builder clearCities() {
            this.cities_ = Collections.emptyList();
            this.bitField0_ &= -33;
            return this;
         }

         public OsmandIndex.AddressPart.Builder removeCities(int index) {
            this.ensureCitiesIsMutable();
            this.cities_.remove(index);
            return this;
         }

         private void ensureAdditionalTagsIsMutable() {
            if ((this.bitField0_ & 64) != 64) {
               this.additionalTags_ = new LazyStringArrayList(this.additionalTags_);
               this.bitField0_ |= 64;
            }
         }

         @Override
         public List<String> getAdditionalTagsList() {
            return Collections.unmodifiableList(this.additionalTags_);
         }

         @Override
         public int getAdditionalTagsCount() {
            return this.additionalTags_.size();
         }

         @Override
         public String getAdditionalTags(int index) {
            return this.additionalTags_.get(index);
         }

         @Override
         public ByteString getAdditionalTagsBytes(int index) {
            return this.additionalTags_.getByteString(index);
         }

         public OsmandIndex.AddressPart.Builder setAdditionalTags(int index, String value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureAdditionalTagsIsMutable();
               this.additionalTags_.set(index, value);
               return this;
            }
         }

         public OsmandIndex.AddressPart.Builder addAdditionalTags(String value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureAdditionalTagsIsMutable();
               this.additionalTags_.add(value);
               return this;
            }
         }

         public OsmandIndex.AddressPart.Builder addAllAdditionalTags(Iterable<String> values) {
            this.ensureAdditionalTagsIsMutable();
            GeneratedMessageLite.Builder.addAll(values, this.additionalTags_);
            return this;
         }

         public OsmandIndex.AddressPart.Builder clearAdditionalTags() {
            this.additionalTags_ = LazyStringArrayList.EMPTY;
            this.bitField0_ &= -65;
            return this;
         }

         public OsmandIndex.AddressPart.Builder addAdditionalTagsBytes(ByteString value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureAdditionalTagsIsMutable();
               this.additionalTags_.add(value);
               return this;
            }
         }
      }
   }

   public interface AddressPartOrBuilder extends MessageLiteOrBuilder {
      boolean hasSize();

      long getSize();

      boolean hasOffset();

      long getOffset();

      boolean hasName();

      String getName();

      ByteString getNameBytes();

      boolean hasNameEn();

      String getNameEn();

      ByteString getNameEnBytes();

      boolean hasIndexNameOffset();

      int getIndexNameOffset();

      List<OsmandIndex.CityBlock> getCitiesList();

      OsmandIndex.CityBlock getCities(int var1);

      int getCitiesCount();

      List<String> getAdditionalTagsList();

      int getAdditionalTagsCount();

      String getAdditionalTags(int var1);

      ByteString getAdditionalTagsBytes(int var1);
   }

   public static final class CityBlock extends GeneratedMessageLite implements OsmandIndex.CityBlockOrBuilder {
      private static final OsmandIndex.CityBlock defaultInstance = new OsmandIndex.CityBlock(true);
      public static Parser<OsmandIndex.CityBlock> PARSER = new AbstractParser<OsmandIndex.CityBlock>() {
         public OsmandIndex.CityBlock parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return new OsmandIndex.CityBlock(input, extensionRegistry);
         }
      };
      private int bitField0_;
      public static final int SIZE_FIELD_NUMBER = 1;
      private long size_;
      public static final int OFFSET_FIELD_NUMBER = 2;
      private long offset_;
      public static final int TYPE_FIELD_NUMBER = 3;
      private int type_;
      private byte memoizedIsInitialized = -1;
      private int memoizedSerializedSize = -1;
      private static final long serialVersionUID = 0L;

      private CityBlock(GeneratedMessageLite.Builder builder) {
         super(builder);
      }

      private CityBlock(boolean noInit) {
      }

      public static OsmandIndex.CityBlock getDefaultInstance() {
         return defaultInstance;
      }

      public OsmandIndex.CityBlock getDefaultInstanceForType() {
         return defaultInstance;
      }

      private CityBlock(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         this.initFields();
         int mutable_bitField0_ = 0;

         try {
            boolean done = false;

            while(!done) {
               int tag = input.readTag();
               switch(tag) {
                  case 0:
                     done = true;
                     break;
                  case 8:
                     this.bitField0_ |= 1;
                     this.size_ = input.readInt64();
                     break;
                  case 16:
                     this.bitField0_ |= 2;
                     this.offset_ = input.readInt64();
                     break;
                  case 24:
                     this.bitField0_ |= 4;
                     this.type_ = input.readInt32();
                     break;
                  default:
                     if (!this.parseUnknownField(input, extensionRegistry, tag)) {
                        done = true;
                     }
               }
            }
         } catch (InvalidProtocolBufferException var10) {
            throw var10.setUnfinishedMessage(this);
         } catch (IOException var11) {
            throw new InvalidProtocolBufferException(var11.getMessage()).setUnfinishedMessage(this);
         } finally {
            this.makeExtensionsImmutable();
         }
      }

      @Override
      public Parser<OsmandIndex.CityBlock> getParserForType() {
         return PARSER;
      }

      @Override
      public boolean hasSize() {
         return (this.bitField0_ & 1) == 1;
      }

      @Override
      public long getSize() {
         return this.size_;
      }

      @Override
      public boolean hasOffset() {
         return (this.bitField0_ & 2) == 2;
      }

      @Override
      public long getOffset() {
         return this.offset_;
      }

      @Override
      public boolean hasType() {
         return (this.bitField0_ & 4) == 4;
      }

      @Override
      public int getType() {
         return this.type_;
      }

      private void initFields() {
         this.size_ = 0L;
         this.offset_ = 0L;
         this.type_ = 0;
      }

      @Override
      public final boolean isInitialized() {
         byte isInitialized = this.memoizedIsInitialized;
         if (isInitialized != -1) {
            return isInitialized == 1;
         } else if (!this.hasSize()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasOffset()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasType()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else {
            this.memoizedIsInitialized = 1;
            return true;
         }
      }

      @Override
      public void writeTo(CodedOutputStream output) throws IOException {
         this.getSerializedSize();
         if ((this.bitField0_ & 1) == 1) {
            output.writeInt64(1, this.size_);
         }

         if ((this.bitField0_ & 2) == 2) {
            output.writeInt64(2, this.offset_);
         }

         if ((this.bitField0_ & 4) == 4) {
            output.writeInt32(3, this.type_);
         }
      }

      @Override
      public int getSerializedSize() {
         int size = this.memoizedSerializedSize;
         if (size != -1) {
            return size;
         } else {
            size = 0;
            if ((this.bitField0_ & 1) == 1) {
               size += CodedOutputStream.computeInt64Size(1, this.size_);
            }

            if ((this.bitField0_ & 2) == 2) {
               size += CodedOutputStream.computeInt64Size(2, this.offset_);
            }

            if ((this.bitField0_ & 4) == 4) {
               size += CodedOutputStream.computeInt32Size(3, this.type_);
            }

            this.memoizedSerializedSize = size;
            return size;
         }
      }

      @Override
      protected Object writeReplace() throws ObjectStreamException {
         return super.writeReplace();
      }

      public static OsmandIndex.CityBlock parseFrom(ByteString data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.CityBlock parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.CityBlock parseFrom(byte[] data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.CityBlock parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.CityBlock parseFrom(InputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.CityBlock parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.CityBlock parseDelimitedFrom(InputStream input) throws IOException {
         return PARSER.parseDelimitedFrom(input);
      }

      public static OsmandIndex.CityBlock parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseDelimitedFrom(input, extensionRegistry);
      }

      public static OsmandIndex.CityBlock parseFrom(CodedInputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.CityBlock parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.CityBlock.Builder newBuilder() {
         return OsmandIndex.CityBlock.Builder.create();
      }

      public OsmandIndex.CityBlock.Builder newBuilderForType() {
         return newBuilder();
      }

      public static OsmandIndex.CityBlock.Builder newBuilder(OsmandIndex.CityBlock prototype) {
         return newBuilder().mergeFrom(prototype);
      }

      public OsmandIndex.CityBlock.Builder toBuilder() {
         return newBuilder(this);
      }

      static {
         defaultInstance.initFields();
      }

      public static final class Builder
         extends GeneratedMessageLite.Builder<OsmandIndex.CityBlock, OsmandIndex.CityBlock.Builder>
         implements OsmandIndex.CityBlockOrBuilder {
         private int bitField0_;
         private long size_;
         private long offset_;
         private int type_;

         private Builder() {
            this.maybeForceBuilderInitialization();
         }

         private void maybeForceBuilderInitialization() {
         }

         private static OsmandIndex.CityBlock.Builder create() {
            return new OsmandIndex.CityBlock.Builder();
         }

         public OsmandIndex.CityBlock.Builder clear() {
            super.clear();
            this.size_ = 0L;
            this.bitField0_ &= -2;
            this.offset_ = 0L;
            this.bitField0_ &= -3;
            this.type_ = 0;
            this.bitField0_ &= -5;
            return this;
         }

         public OsmandIndex.CityBlock.Builder clone() {
            return create().mergeFrom(this.buildPartial());
         }

         public OsmandIndex.CityBlock getDefaultInstanceForType() {
            return OsmandIndex.CityBlock.getDefaultInstance();
         }

         public OsmandIndex.CityBlock build() {
            OsmandIndex.CityBlock result = this.buildPartial();
            if (!result.isInitialized()) {
               throw newUninitializedMessageException(result);
            } else {
               return result;
            }
         }

         public OsmandIndex.CityBlock buildPartial() {
            OsmandIndex.CityBlock result = new OsmandIndex.CityBlock(this);
            int from_bitField0_ = this.bitField0_;
            int to_bitField0_ = 0;
            if ((from_bitField0_ & 1) == 1) {
               to_bitField0_ |= 1;
            }

            result.size_ = this.size_;
            if ((from_bitField0_ & 2) == 2) {
               to_bitField0_ |= 2;
            }

            result.offset_ = this.offset_;
            if ((from_bitField0_ & 4) == 4) {
               to_bitField0_ |= 4;
            }

            result.type_ = this.type_;
            result.bitField0_ = to_bitField0_;
            return result;
         }

         public OsmandIndex.CityBlock.Builder mergeFrom(OsmandIndex.CityBlock other) {
            if (other == OsmandIndex.CityBlock.getDefaultInstance()) {
               return this;
            } else {
               if (other.hasSize()) {
                  this.setSize(other.getSize());
               }

               if (other.hasOffset()) {
                  this.setOffset(other.getOffset());
               }

               if (other.hasType()) {
                  this.setType(other.getType());
               }

               return this;
            }
         }

         @Override
         public final boolean isInitialized() {
            if (!this.hasSize()) {
               return false;
            } else if (!this.hasOffset()) {
               return false;
            } else {
               return this.hasType();
            }
         }

         public OsmandIndex.CityBlock.Builder mergeFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            OsmandIndex.CityBlock parsedMessage = null;

            try {
               parsedMessage = OsmandIndex.CityBlock.PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (InvalidProtocolBufferException var8) {
               parsedMessage = (OsmandIndex.CityBlock)var8.getUnfinishedMessage();
               throw var8;
            } finally {
               if (parsedMessage != null) {
                  this.mergeFrom(parsedMessage);
               }
            }

            return this;
         }

         @Override
         public boolean hasSize() {
            return (this.bitField0_ & 1) == 1;
         }

         @Override
         public long getSize() {
            return this.size_;
         }

         public OsmandIndex.CityBlock.Builder setSize(long value) {
            this.bitField0_ |= 1;
            this.size_ = value;
            return this;
         }

         public OsmandIndex.CityBlock.Builder clearSize() {
            this.bitField0_ &= -2;
            this.size_ = 0L;
            return this;
         }

         @Override
         public boolean hasOffset() {
            return (this.bitField0_ & 2) == 2;
         }

         @Override
         public long getOffset() {
            return this.offset_;
         }

         public OsmandIndex.CityBlock.Builder setOffset(long value) {
            this.bitField0_ |= 2;
            this.offset_ = value;
            return this;
         }

         public OsmandIndex.CityBlock.Builder clearOffset() {
            this.bitField0_ &= -3;
            this.offset_ = 0L;
            return this;
         }

         @Override
         public boolean hasType() {
            return (this.bitField0_ & 4) == 4;
         }

         @Override
         public int getType() {
            return this.type_;
         }

         public OsmandIndex.CityBlock.Builder setType(int value) {
            this.bitField0_ |= 4;
            this.type_ = value;
            return this;
         }

         public OsmandIndex.CityBlock.Builder clearType() {
            this.bitField0_ &= -5;
            this.type_ = 0;
            return this;
         }
      }
   }

   public interface CityBlockOrBuilder extends MessageLiteOrBuilder {
      boolean hasSize();

      long getSize();

      boolean hasOffset();

      long getOffset();

      boolean hasType();

      int getType();
   }

   public static final class FileIndex extends GeneratedMessageLite implements OsmandIndex.FileIndexOrBuilder {
      private static final OsmandIndex.FileIndex defaultInstance = new OsmandIndex.FileIndex(true);
      public static Parser<OsmandIndex.FileIndex> PARSER = new AbstractParser<OsmandIndex.FileIndex>() {
         public OsmandIndex.FileIndex parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return new OsmandIndex.FileIndex(input, extensionRegistry);
         }
      };
      private int bitField0_;
      public static final int SIZE_FIELD_NUMBER = 1;
      private long size_;
      public static final int DATEMODIFIED_FIELD_NUMBER = 2;
      private long dateModified_;
      public static final int FILENAME_FIELD_NUMBER = 3;
      private Object fileName_;
      public static final int VERSION_FIELD_NUMBER = 4;
      private int version_;
      public static final int ADDRESSINDEX_FIELD_NUMBER = 8;
      private List<OsmandIndex.AddressPart> addressIndex_;
      public static final int TRANSPORTINDEX_FIELD_NUMBER = 9;
      private List<OsmandIndex.TransportPart> transportIndex_;
      public static final int POIINDEX_FIELD_NUMBER = 10;
      private List<OsmandIndex.PoiPart> poiIndex_;
      public static final int MAPINDEX_FIELD_NUMBER = 11;
      private List<OsmandIndex.MapPart> mapIndex_;
      public static final int ROUTINGINDEX_FIELD_NUMBER = 12;
      private List<OsmandIndex.RoutingPart> routingIndex_;
      private byte memoizedIsInitialized = -1;
      private int memoizedSerializedSize = -1;
      private static final long serialVersionUID = 0L;

      private FileIndex(GeneratedMessageLite.Builder builder) {
         super(builder);
      }

      private FileIndex(boolean noInit) {
      }

      public static OsmandIndex.FileIndex getDefaultInstance() {
         return defaultInstance;
      }

      public OsmandIndex.FileIndex getDefaultInstanceForType() {
         return defaultInstance;
      }

      private FileIndex(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         this.initFields();
         int mutable_bitField0_ = 0;

         try {
            boolean done = false;

            while(!done) {
               int tag = input.readTag();
               switch(tag) {
                  case 0:
                     done = true;
                     break;
                  case 8:
                     this.bitField0_ |= 1;
                     this.size_ = input.readInt64();
                     break;
                  case 16:
                     this.bitField0_ |= 2;
                     this.dateModified_ = input.readInt64();
                     break;
                  case 26:
                     this.bitField0_ |= 4;
                     this.fileName_ = input.readBytes();
                     break;
                  case 32:
                     this.bitField0_ |= 8;
                     this.version_ = input.readInt32();
                     break;
                  case 66:
                     if ((mutable_bitField0_ & 16) != 16) {
                        this.addressIndex_ = new ArrayList<>();
                        mutable_bitField0_ |= 16;
                     }

                     this.addressIndex_.add(input.readMessage(OsmandIndex.AddressPart.PARSER, extensionRegistry));
                     break;
                  case 74:
                     if ((mutable_bitField0_ & 32) != 32) {
                        this.transportIndex_ = new ArrayList<>();
                        mutable_bitField0_ |= 32;
                     }

                     this.transportIndex_.add(input.readMessage(OsmandIndex.TransportPart.PARSER, extensionRegistry));
                     break;
                  case 82:
                     if ((mutable_bitField0_ & 64) != 64) {
                        this.poiIndex_ = new ArrayList<>();
                        mutable_bitField0_ |= 64;
                     }

                     this.poiIndex_.add(input.readMessage(OsmandIndex.PoiPart.PARSER, extensionRegistry));
                     break;
                  case 90:
                     if ((mutable_bitField0_ & 128) != 128) {
                        this.mapIndex_ = new ArrayList<>();
                        mutable_bitField0_ |= 128;
                     }

                     this.mapIndex_.add(input.readMessage(OsmandIndex.MapPart.PARSER, extensionRegistry));
                     break;
                  case 98:
                     if ((mutable_bitField0_ & 256) != 256) {
                        this.routingIndex_ = new ArrayList<>();
                        mutable_bitField0_ |= 256;
                     }

                     this.routingIndex_.add(input.readMessage(OsmandIndex.RoutingPart.PARSER, extensionRegistry));
                     break;
                  default:
                     if (!this.parseUnknownField(input, extensionRegistry, tag)) {
                        done = true;
                     }
               }
            }
         } catch (InvalidProtocolBufferException var10) {
            throw var10.setUnfinishedMessage(this);
         } catch (IOException var11) {
            throw new InvalidProtocolBufferException(var11.getMessage()).setUnfinishedMessage(this);
         } finally {
            if ((mutable_bitField0_ & 16) == 16) {
               this.addressIndex_ = Collections.unmodifiableList(this.addressIndex_);
            }

            if ((mutable_bitField0_ & 32) == 32) {
               this.transportIndex_ = Collections.unmodifiableList(this.transportIndex_);
            }

            if ((mutable_bitField0_ & 64) == 64) {
               this.poiIndex_ = Collections.unmodifiableList(this.poiIndex_);
            }

            if ((mutable_bitField0_ & 128) == 128) {
               this.mapIndex_ = Collections.unmodifiableList(this.mapIndex_);
            }

            if ((mutable_bitField0_ & 256) == 256) {
               this.routingIndex_ = Collections.unmodifiableList(this.routingIndex_);
            }

            this.makeExtensionsImmutable();
         }
      }

      @Override
      public Parser<OsmandIndex.FileIndex> getParserForType() {
         return PARSER;
      }

      @Override
      public boolean hasSize() {
         return (this.bitField0_ & 1) == 1;
      }

      @Override
      public long getSize() {
         return this.size_;
      }

      @Override
      public boolean hasDateModified() {
         return (this.bitField0_ & 2) == 2;
      }

      @Override
      public long getDateModified() {
         return this.dateModified_;
      }

      @Override
      public boolean hasFileName() {
         return (this.bitField0_ & 4) == 4;
      }

      @Override
      public String getFileName() {
         Object ref = this.fileName_;
         if (ref instanceof String) {
            return (String)ref;
         } else {
            ByteString bs = (ByteString)ref;
            String s = bs.toStringUtf8();
            if (bs.isValidUtf8()) {
               this.fileName_ = s;
            }

            return s;
         }
      }

      @Override
      public ByteString getFileNameBytes() {
         Object ref = this.fileName_;
         if (ref instanceof String) {
            ByteString b = ByteString.copyFromUtf8((String)ref);
            this.fileName_ = b;
            return b;
         } else {
            return (ByteString)ref;
         }
      }

      @Override
      public boolean hasVersion() {
         return (this.bitField0_ & 8) == 8;
      }

      @Override
      public int getVersion() {
         return this.version_;
      }

      @Override
      public List<OsmandIndex.AddressPart> getAddressIndexList() {
         return this.addressIndex_;
      }

      public List<? extends OsmandIndex.AddressPartOrBuilder> getAddressIndexOrBuilderList() {
         return this.addressIndex_;
      }

      @Override
      public int getAddressIndexCount() {
         return this.addressIndex_.size();
      }

      @Override
      public OsmandIndex.AddressPart getAddressIndex(int index) {
         return this.addressIndex_.get(index);
      }

      public OsmandIndex.AddressPartOrBuilder getAddressIndexOrBuilder(int index) {
         return this.addressIndex_.get(index);
      }

      @Override
      public List<OsmandIndex.TransportPart> getTransportIndexList() {
         return this.transportIndex_;
      }

      public List<? extends OsmandIndex.TransportPartOrBuilder> getTransportIndexOrBuilderList() {
         return this.transportIndex_;
      }

      @Override
      public int getTransportIndexCount() {
         return this.transportIndex_.size();
      }

      @Override
      public OsmandIndex.TransportPart getTransportIndex(int index) {
         return this.transportIndex_.get(index);
      }

      public OsmandIndex.TransportPartOrBuilder getTransportIndexOrBuilder(int index) {
         return this.transportIndex_.get(index);
      }

      @Override
      public List<OsmandIndex.PoiPart> getPoiIndexList() {
         return this.poiIndex_;
      }

      public List<? extends OsmandIndex.PoiPartOrBuilder> getPoiIndexOrBuilderList() {
         return this.poiIndex_;
      }

      @Override
      public int getPoiIndexCount() {
         return this.poiIndex_.size();
      }

      @Override
      public OsmandIndex.PoiPart getPoiIndex(int index) {
         return this.poiIndex_.get(index);
      }

      public OsmandIndex.PoiPartOrBuilder getPoiIndexOrBuilder(int index) {
         return this.poiIndex_.get(index);
      }

      @Override
      public List<OsmandIndex.MapPart> getMapIndexList() {
         return this.mapIndex_;
      }

      public List<? extends OsmandIndex.MapPartOrBuilder> getMapIndexOrBuilderList() {
         return this.mapIndex_;
      }

      @Override
      public int getMapIndexCount() {
         return this.mapIndex_.size();
      }

      @Override
      public OsmandIndex.MapPart getMapIndex(int index) {
         return this.mapIndex_.get(index);
      }

      public OsmandIndex.MapPartOrBuilder getMapIndexOrBuilder(int index) {
         return this.mapIndex_.get(index);
      }

      @Override
      public List<OsmandIndex.RoutingPart> getRoutingIndexList() {
         return this.routingIndex_;
      }

      public List<? extends OsmandIndex.RoutingPartOrBuilder> getRoutingIndexOrBuilderList() {
         return this.routingIndex_;
      }

      @Override
      public int getRoutingIndexCount() {
         return this.routingIndex_.size();
      }

      @Override
      public OsmandIndex.RoutingPart getRoutingIndex(int index) {
         return this.routingIndex_.get(index);
      }

      public OsmandIndex.RoutingPartOrBuilder getRoutingIndexOrBuilder(int index) {
         return this.routingIndex_.get(index);
      }

      private void initFields() {
         this.size_ = 0L;
         this.dateModified_ = 0L;
         this.fileName_ = "";
         this.version_ = 0;
         this.addressIndex_ = Collections.emptyList();
         this.transportIndex_ = Collections.emptyList();
         this.poiIndex_ = Collections.emptyList();
         this.mapIndex_ = Collections.emptyList();
         this.routingIndex_ = Collections.emptyList();
      }

      @Override
      public final boolean isInitialized() {
         byte isInitialized = this.memoizedIsInitialized;
         if (isInitialized != -1) {
            return isInitialized == 1;
         } else if (!this.hasSize()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasDateModified()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasFileName()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasVersion()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else {
            for(int i = 0; i < this.getAddressIndexCount(); ++i) {
               if (!this.getAddressIndex(i).isInitialized()) {
                  this.memoizedIsInitialized = 0;
                  return false;
               }
            }

            for(int i = 0; i < this.getTransportIndexCount(); ++i) {
               if (!this.getTransportIndex(i).isInitialized()) {
                  this.memoizedIsInitialized = 0;
                  return false;
               }
            }

            for(int i = 0; i < this.getPoiIndexCount(); ++i) {
               if (!this.getPoiIndex(i).isInitialized()) {
                  this.memoizedIsInitialized = 0;
                  return false;
               }
            }

            for(int i = 0; i < this.getMapIndexCount(); ++i) {
               if (!this.getMapIndex(i).isInitialized()) {
                  this.memoizedIsInitialized = 0;
                  return false;
               }
            }

            for(int i = 0; i < this.getRoutingIndexCount(); ++i) {
               if (!this.getRoutingIndex(i).isInitialized()) {
                  this.memoizedIsInitialized = 0;
                  return false;
               }
            }

            this.memoizedIsInitialized = 1;
            return true;
         }
      }

      @Override
      public void writeTo(CodedOutputStream output) throws IOException {
         this.getSerializedSize();
         if ((this.bitField0_ & 1) == 1) {
            output.writeInt64(1, this.size_);
         }

         if ((this.bitField0_ & 2) == 2) {
            output.writeInt64(2, this.dateModified_);
         }

         if ((this.bitField0_ & 4) == 4) {
            output.writeBytes(3, this.getFileNameBytes());
         }

         if ((this.bitField0_ & 8) == 8) {
            output.writeInt32(4, this.version_);
         }

         for(int i = 0; i < this.addressIndex_.size(); ++i) {
            output.writeMessage(8, this.addressIndex_.get(i));
         }

         for(int i = 0; i < this.transportIndex_.size(); ++i) {
            output.writeMessage(9, this.transportIndex_.get(i));
         }

         for(int i = 0; i < this.poiIndex_.size(); ++i) {
            output.writeMessage(10, this.poiIndex_.get(i));
         }

         for(int i = 0; i < this.mapIndex_.size(); ++i) {
            output.writeMessage(11, this.mapIndex_.get(i));
         }

         for(int i = 0; i < this.routingIndex_.size(); ++i) {
            output.writeMessage(12, this.routingIndex_.get(i));
         }
      }

      @Override
      public int getSerializedSize() {
         int size = this.memoizedSerializedSize;
         if (size != -1) {
            return size;
         } else {
            size = 0;
            if ((this.bitField0_ & 1) == 1) {
               size += CodedOutputStream.computeInt64Size(1, this.size_);
            }

            if ((this.bitField0_ & 2) == 2) {
               size += CodedOutputStream.computeInt64Size(2, this.dateModified_);
            }

            if ((this.bitField0_ & 4) == 4) {
               size += CodedOutputStream.computeBytesSize(3, this.getFileNameBytes());
            }

            if ((this.bitField0_ & 8) == 8) {
               size += CodedOutputStream.computeInt32Size(4, this.version_);
            }

            for(int i = 0; i < this.addressIndex_.size(); ++i) {
               size += CodedOutputStream.computeMessageSize(8, this.addressIndex_.get(i));
            }

            for(int i = 0; i < this.transportIndex_.size(); ++i) {
               size += CodedOutputStream.computeMessageSize(9, this.transportIndex_.get(i));
            }

            for(int i = 0; i < this.poiIndex_.size(); ++i) {
               size += CodedOutputStream.computeMessageSize(10, this.poiIndex_.get(i));
            }

            for(int i = 0; i < this.mapIndex_.size(); ++i) {
               size += CodedOutputStream.computeMessageSize(11, this.mapIndex_.get(i));
            }

            for(int i = 0; i < this.routingIndex_.size(); ++i) {
               size += CodedOutputStream.computeMessageSize(12, this.routingIndex_.get(i));
            }

            this.memoizedSerializedSize = size;
            return size;
         }
      }

      @Override
      protected Object writeReplace() throws ObjectStreamException {
         return super.writeReplace();
      }

      public static OsmandIndex.FileIndex parseFrom(ByteString data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.FileIndex parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.FileIndex parseFrom(byte[] data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.FileIndex parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.FileIndex parseFrom(InputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.FileIndex parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.FileIndex parseDelimitedFrom(InputStream input) throws IOException {
         return PARSER.parseDelimitedFrom(input);
      }

      public static OsmandIndex.FileIndex parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseDelimitedFrom(input, extensionRegistry);
      }

      public static OsmandIndex.FileIndex parseFrom(CodedInputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.FileIndex parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.FileIndex.Builder newBuilder() {
         return OsmandIndex.FileIndex.Builder.create();
      }

      public OsmandIndex.FileIndex.Builder newBuilderForType() {
         return newBuilder();
      }

      public static OsmandIndex.FileIndex.Builder newBuilder(OsmandIndex.FileIndex prototype) {
         return newBuilder().mergeFrom(prototype);
      }

      public OsmandIndex.FileIndex.Builder toBuilder() {
         return newBuilder(this);
      }

      static {
         defaultInstance.initFields();
      }

      public static final class Builder
         extends GeneratedMessageLite.Builder<OsmandIndex.FileIndex, OsmandIndex.FileIndex.Builder>
         implements OsmandIndex.FileIndexOrBuilder {
         private int bitField0_;
         private long size_;
         private long dateModified_;
         private Object fileName_ = "";
         private int version_;
         private List<OsmandIndex.AddressPart> addressIndex_ = Collections.emptyList();
         private List<OsmandIndex.TransportPart> transportIndex_ = Collections.emptyList();
         private List<OsmandIndex.PoiPart> poiIndex_ = Collections.emptyList();
         private List<OsmandIndex.MapPart> mapIndex_ = Collections.emptyList();
         private List<OsmandIndex.RoutingPart> routingIndex_ = Collections.emptyList();

         private Builder() {
            this.maybeForceBuilderInitialization();
         }

         private void maybeForceBuilderInitialization() {
         }

         private static OsmandIndex.FileIndex.Builder create() {
            return new OsmandIndex.FileIndex.Builder();
         }

         public OsmandIndex.FileIndex.Builder clear() {
            super.clear();
            this.size_ = 0L;
            this.bitField0_ &= -2;
            this.dateModified_ = 0L;
            this.bitField0_ &= -3;
            this.fileName_ = "";
            this.bitField0_ &= -5;
            this.version_ = 0;
            this.bitField0_ &= -9;
            this.addressIndex_ = Collections.emptyList();
            this.bitField0_ &= -17;
            this.transportIndex_ = Collections.emptyList();
            this.bitField0_ &= -33;
            this.poiIndex_ = Collections.emptyList();
            this.bitField0_ &= -65;
            this.mapIndex_ = Collections.emptyList();
            this.bitField0_ &= -129;
            this.routingIndex_ = Collections.emptyList();
            this.bitField0_ &= -257;
            return this;
         }

         public OsmandIndex.FileIndex.Builder clone() {
            return create().mergeFrom(this.buildPartial());
         }

         public OsmandIndex.FileIndex getDefaultInstanceForType() {
            return OsmandIndex.FileIndex.getDefaultInstance();
         }

         public OsmandIndex.FileIndex build() {
            OsmandIndex.FileIndex result = this.buildPartial();
            if (!result.isInitialized()) {
               throw newUninitializedMessageException(result);
            } else {
               return result;
            }
         }

         public OsmandIndex.FileIndex buildPartial() {
            OsmandIndex.FileIndex result = new OsmandIndex.FileIndex(this);
            int from_bitField0_ = this.bitField0_;
            int to_bitField0_ = 0;
            if ((from_bitField0_ & 1) == 1) {
               to_bitField0_ |= 1;
            }

            result.size_ = this.size_;
            if ((from_bitField0_ & 2) == 2) {
               to_bitField0_ |= 2;
            }

            result.dateModified_ = this.dateModified_;
            if ((from_bitField0_ & 4) == 4) {
               to_bitField0_ |= 4;
            }

            result.fileName_ = this.fileName_;
            if ((from_bitField0_ & 8) == 8) {
               to_bitField0_ |= 8;
            }

            result.version_ = this.version_;
            if ((this.bitField0_ & 16) == 16) {
               this.addressIndex_ = Collections.unmodifiableList(this.addressIndex_);
               this.bitField0_ &= -17;
            }

            result.addressIndex_ = this.addressIndex_;
            if ((this.bitField0_ & 32) == 32) {
               this.transportIndex_ = Collections.unmodifiableList(this.transportIndex_);
               this.bitField0_ &= -33;
            }

            result.transportIndex_ = this.transportIndex_;
            if ((this.bitField0_ & 64) == 64) {
               this.poiIndex_ = Collections.unmodifiableList(this.poiIndex_);
               this.bitField0_ &= -65;
            }

            result.poiIndex_ = this.poiIndex_;
            if ((this.bitField0_ & 128) == 128) {
               this.mapIndex_ = Collections.unmodifiableList(this.mapIndex_);
               this.bitField0_ &= -129;
            }

            result.mapIndex_ = this.mapIndex_;
            if ((this.bitField0_ & 256) == 256) {
               this.routingIndex_ = Collections.unmodifiableList(this.routingIndex_);
               this.bitField0_ &= -257;
            }

            result.routingIndex_ = this.routingIndex_;
            result.bitField0_ = to_bitField0_;
            return result;
         }

         public OsmandIndex.FileIndex.Builder mergeFrom(OsmandIndex.FileIndex other) {
            if (other == OsmandIndex.FileIndex.getDefaultInstance()) {
               return this;
            } else {
               if (other.hasSize()) {
                  this.setSize(other.getSize());
               }

               if (other.hasDateModified()) {
                  this.setDateModified(other.getDateModified());
               }

               if (other.hasFileName()) {
                  this.bitField0_ |= 4;
                  this.fileName_ = other.fileName_;
               }

               if (other.hasVersion()) {
                  this.setVersion(other.getVersion());
               }

               if (!other.addressIndex_.isEmpty()) {
                  if (this.addressIndex_.isEmpty()) {
                     this.addressIndex_ = other.addressIndex_;
                     this.bitField0_ &= -17;
                  } else {
                     this.ensureAddressIndexIsMutable();
                     this.addressIndex_.addAll(other.addressIndex_);
                  }
               }

               if (!other.transportIndex_.isEmpty()) {
                  if (this.transportIndex_.isEmpty()) {
                     this.transportIndex_ = other.transportIndex_;
                     this.bitField0_ &= -33;
                  } else {
                     this.ensureTransportIndexIsMutable();
                     this.transportIndex_.addAll(other.transportIndex_);
                  }
               }

               if (!other.poiIndex_.isEmpty()) {
                  if (this.poiIndex_.isEmpty()) {
                     this.poiIndex_ = other.poiIndex_;
                     this.bitField0_ &= -65;
                  } else {
                     this.ensurePoiIndexIsMutable();
                     this.poiIndex_.addAll(other.poiIndex_);
                  }
               }

               if (!other.mapIndex_.isEmpty()) {
                  if (this.mapIndex_.isEmpty()) {
                     this.mapIndex_ = other.mapIndex_;
                     this.bitField0_ &= -129;
                  } else {
                     this.ensureMapIndexIsMutable();
                     this.mapIndex_.addAll(other.mapIndex_);
                  }
               }

               if (!other.routingIndex_.isEmpty()) {
                  if (this.routingIndex_.isEmpty()) {
                     this.routingIndex_ = other.routingIndex_;
                     this.bitField0_ &= -257;
                  } else {
                     this.ensureRoutingIndexIsMutable();
                     this.routingIndex_.addAll(other.routingIndex_);
                  }
               }

               return this;
            }
         }

         @Override
         public final boolean isInitialized() {
            if (!this.hasSize()) {
               return false;
            } else if (!this.hasDateModified()) {
               return false;
            } else if (!this.hasFileName()) {
               return false;
            } else if (!this.hasVersion()) {
               return false;
            } else {
               for(int i = 0; i < this.getAddressIndexCount(); ++i) {
                  if (!this.getAddressIndex(i).isInitialized()) {
                     return false;
                  }
               }

               for(int i = 0; i < this.getTransportIndexCount(); ++i) {
                  if (!this.getTransportIndex(i).isInitialized()) {
                     return false;
                  }
               }

               for(int i = 0; i < this.getPoiIndexCount(); ++i) {
                  if (!this.getPoiIndex(i).isInitialized()) {
                     return false;
                  }
               }

               for(int i = 0; i < this.getMapIndexCount(); ++i) {
                  if (!this.getMapIndex(i).isInitialized()) {
                     return false;
                  }
               }

               for(int i = 0; i < this.getRoutingIndexCount(); ++i) {
                  if (!this.getRoutingIndex(i).isInitialized()) {
                     return false;
                  }
               }

               return true;
            }
         }

         public OsmandIndex.FileIndex.Builder mergeFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            OsmandIndex.FileIndex parsedMessage = null;

            try {
               parsedMessage = OsmandIndex.FileIndex.PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (InvalidProtocolBufferException var8) {
               parsedMessage = (OsmandIndex.FileIndex)var8.getUnfinishedMessage();
               throw var8;
            } finally {
               if (parsedMessage != null) {
                  this.mergeFrom(parsedMessage);
               }
            }

            return this;
         }

         @Override
         public boolean hasSize() {
            return (this.bitField0_ & 1) == 1;
         }

         @Override
         public long getSize() {
            return this.size_;
         }

         public OsmandIndex.FileIndex.Builder setSize(long value) {
            this.bitField0_ |= 1;
            this.size_ = value;
            return this;
         }

         public OsmandIndex.FileIndex.Builder clearSize() {
            this.bitField0_ &= -2;
            this.size_ = 0L;
            return this;
         }

         @Override
         public boolean hasDateModified() {
            return (this.bitField0_ & 2) == 2;
         }

         @Override
         public long getDateModified() {
            return this.dateModified_;
         }

         public OsmandIndex.FileIndex.Builder setDateModified(long value) {
            this.bitField0_ |= 2;
            this.dateModified_ = value;
            return this;
         }

         public OsmandIndex.FileIndex.Builder clearDateModified() {
            this.bitField0_ &= -3;
            this.dateModified_ = 0L;
            return this;
         }

         @Override
         public boolean hasFileName() {
            return (this.bitField0_ & 4) == 4;
         }

         @Override
         public String getFileName() {
            Object ref = this.fileName_;
            if (!(ref instanceof String)) {
               String s = ((ByteString)ref).toStringUtf8();
               this.fileName_ = s;
               return s;
            } else {
               return (String)ref;
            }
         }

         @Override
         public ByteString getFileNameBytes() {
            Object ref = this.fileName_;
            if (ref instanceof String) {
               ByteString b = ByteString.copyFromUtf8((String)ref);
               this.fileName_ = b;
               return b;
            } else {
               return (ByteString)ref;
            }
         }

         public OsmandIndex.FileIndex.Builder setFileName(String value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.bitField0_ |= 4;
               this.fileName_ = value;
               return this;
            }
         }

         public OsmandIndex.FileIndex.Builder clearFileName() {
            this.bitField0_ &= -5;
            this.fileName_ = OsmandIndex.FileIndex.getDefaultInstance().getFileName();
            return this;
         }

         public OsmandIndex.FileIndex.Builder setFileNameBytes(ByteString value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.bitField0_ |= 4;
               this.fileName_ = value;
               return this;
            }
         }

         @Override
         public boolean hasVersion() {
            return (this.bitField0_ & 8) == 8;
         }

         @Override
         public int getVersion() {
            return this.version_;
         }

         public OsmandIndex.FileIndex.Builder setVersion(int value) {
            this.bitField0_ |= 8;
            this.version_ = value;
            return this;
         }

         public OsmandIndex.FileIndex.Builder clearVersion() {
            this.bitField0_ &= -9;
            this.version_ = 0;
            return this;
         }

         private void ensureAddressIndexIsMutable() {
            if ((this.bitField0_ & 16) != 16) {
               this.addressIndex_ = new ArrayList<>(this.addressIndex_);
               this.bitField0_ |= 16;
            }
         }

         @Override
         public List<OsmandIndex.AddressPart> getAddressIndexList() {
            return Collections.unmodifiableList(this.addressIndex_);
         }

         @Override
         public int getAddressIndexCount() {
            return this.addressIndex_.size();
         }

         @Override
         public OsmandIndex.AddressPart getAddressIndex(int index) {
            return this.addressIndex_.get(index);
         }

         public OsmandIndex.FileIndex.Builder setAddressIndex(int index, OsmandIndex.AddressPart value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureAddressIndexIsMutable();
               this.addressIndex_.set(index, value);
               return this;
            }
         }

         public OsmandIndex.FileIndex.Builder setAddressIndex(int index, OsmandIndex.AddressPart.Builder builderForValue) {
            this.ensureAddressIndexIsMutable();
            this.addressIndex_.set(index, builderForValue.build());
            return this;
         }

         public OsmandIndex.FileIndex.Builder addAddressIndex(OsmandIndex.AddressPart value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureAddressIndexIsMutable();
               this.addressIndex_.add(value);
               return this;
            }
         }

         public OsmandIndex.FileIndex.Builder addAddressIndex(int index, OsmandIndex.AddressPart value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureAddressIndexIsMutable();
               this.addressIndex_.add(index, value);
               return this;
            }
         }

         public OsmandIndex.FileIndex.Builder addAddressIndex(OsmandIndex.AddressPart.Builder builderForValue) {
            this.ensureAddressIndexIsMutable();
            this.addressIndex_.add(builderForValue.build());
            return this;
         }

         public OsmandIndex.FileIndex.Builder addAddressIndex(int index, OsmandIndex.AddressPart.Builder builderForValue) {
            this.ensureAddressIndexIsMutable();
            this.addressIndex_.add(index, builderForValue.build());
            return this;
         }

         public OsmandIndex.FileIndex.Builder addAllAddressIndex(Iterable<? extends OsmandIndex.AddressPart> values) {
            this.ensureAddressIndexIsMutable();
            GeneratedMessageLite.Builder.addAll(values, this.addressIndex_);
            return this;
         }

         public OsmandIndex.FileIndex.Builder clearAddressIndex() {
            this.addressIndex_ = Collections.emptyList();
            this.bitField0_ &= -17;
            return this;
         }

         public OsmandIndex.FileIndex.Builder removeAddressIndex(int index) {
            this.ensureAddressIndexIsMutable();
            this.addressIndex_.remove(index);
            return this;
         }

         private void ensureTransportIndexIsMutable() {
            if ((this.bitField0_ & 32) != 32) {
               this.transportIndex_ = new ArrayList<>(this.transportIndex_);
               this.bitField0_ |= 32;
            }
         }

         @Override
         public List<OsmandIndex.TransportPart> getTransportIndexList() {
            return Collections.unmodifiableList(this.transportIndex_);
         }

         @Override
         public int getTransportIndexCount() {
            return this.transportIndex_.size();
         }

         @Override
         public OsmandIndex.TransportPart getTransportIndex(int index) {
            return this.transportIndex_.get(index);
         }

         public OsmandIndex.FileIndex.Builder setTransportIndex(int index, OsmandIndex.TransportPart value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureTransportIndexIsMutable();
               this.transportIndex_.set(index, value);
               return this;
            }
         }

         public OsmandIndex.FileIndex.Builder setTransportIndex(int index, OsmandIndex.TransportPart.Builder builderForValue) {
            this.ensureTransportIndexIsMutable();
            this.transportIndex_.set(index, builderForValue.build());
            return this;
         }

         public OsmandIndex.FileIndex.Builder addTransportIndex(OsmandIndex.TransportPart value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureTransportIndexIsMutable();
               this.transportIndex_.add(value);
               return this;
            }
         }

         public OsmandIndex.FileIndex.Builder addTransportIndex(int index, OsmandIndex.TransportPart value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureTransportIndexIsMutable();
               this.transportIndex_.add(index, value);
               return this;
            }
         }

         public OsmandIndex.FileIndex.Builder addTransportIndex(OsmandIndex.TransportPart.Builder builderForValue) {
            this.ensureTransportIndexIsMutable();
            this.transportIndex_.add(builderForValue.build());
            return this;
         }

         public OsmandIndex.FileIndex.Builder addTransportIndex(int index, OsmandIndex.TransportPart.Builder builderForValue) {
            this.ensureTransportIndexIsMutable();
            this.transportIndex_.add(index, builderForValue.build());
            return this;
         }

         public OsmandIndex.FileIndex.Builder addAllTransportIndex(Iterable<? extends OsmandIndex.TransportPart> values) {
            this.ensureTransportIndexIsMutable();
            GeneratedMessageLite.Builder.addAll(values, this.transportIndex_);
            return this;
         }

         public OsmandIndex.FileIndex.Builder clearTransportIndex() {
            this.transportIndex_ = Collections.emptyList();
            this.bitField0_ &= -33;
            return this;
         }

         public OsmandIndex.FileIndex.Builder removeTransportIndex(int index) {
            this.ensureTransportIndexIsMutable();
            this.transportIndex_.remove(index);
            return this;
         }

         private void ensurePoiIndexIsMutable() {
            if ((this.bitField0_ & 64) != 64) {
               this.poiIndex_ = new ArrayList<>(this.poiIndex_);
               this.bitField0_ |= 64;
            }
         }

         @Override
         public List<OsmandIndex.PoiPart> getPoiIndexList() {
            return Collections.unmodifiableList(this.poiIndex_);
         }

         @Override
         public int getPoiIndexCount() {
            return this.poiIndex_.size();
         }

         @Override
         public OsmandIndex.PoiPart getPoiIndex(int index) {
            return this.poiIndex_.get(index);
         }

         public OsmandIndex.FileIndex.Builder setPoiIndex(int index, OsmandIndex.PoiPart value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensurePoiIndexIsMutable();
               this.poiIndex_.set(index, value);
               return this;
            }
         }

         public OsmandIndex.FileIndex.Builder setPoiIndex(int index, OsmandIndex.PoiPart.Builder builderForValue) {
            this.ensurePoiIndexIsMutable();
            this.poiIndex_.set(index, builderForValue.build());
            return this;
         }

         public OsmandIndex.FileIndex.Builder addPoiIndex(OsmandIndex.PoiPart value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensurePoiIndexIsMutable();
               this.poiIndex_.add(value);
               return this;
            }
         }

         public OsmandIndex.FileIndex.Builder addPoiIndex(int index, OsmandIndex.PoiPart value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensurePoiIndexIsMutable();
               this.poiIndex_.add(index, value);
               return this;
            }
         }

         public OsmandIndex.FileIndex.Builder addPoiIndex(OsmandIndex.PoiPart.Builder builderForValue) {
            this.ensurePoiIndexIsMutable();
            this.poiIndex_.add(builderForValue.build());
            return this;
         }

         public OsmandIndex.FileIndex.Builder addPoiIndex(int index, OsmandIndex.PoiPart.Builder builderForValue) {
            this.ensurePoiIndexIsMutable();
            this.poiIndex_.add(index, builderForValue.build());
            return this;
         }

         public OsmandIndex.FileIndex.Builder addAllPoiIndex(Iterable<? extends OsmandIndex.PoiPart> values) {
            this.ensurePoiIndexIsMutable();
            GeneratedMessageLite.Builder.addAll(values, this.poiIndex_);
            return this;
         }

         public OsmandIndex.FileIndex.Builder clearPoiIndex() {
            this.poiIndex_ = Collections.emptyList();
            this.bitField0_ &= -65;
            return this;
         }

         public OsmandIndex.FileIndex.Builder removePoiIndex(int index) {
            this.ensurePoiIndexIsMutable();
            this.poiIndex_.remove(index);
            return this;
         }

         private void ensureMapIndexIsMutable() {
            if ((this.bitField0_ & 128) != 128) {
               this.mapIndex_ = new ArrayList<>(this.mapIndex_);
               this.bitField0_ |= 128;
            }
         }

         @Override
         public List<OsmandIndex.MapPart> getMapIndexList() {
            return Collections.unmodifiableList(this.mapIndex_);
         }

         @Override
         public int getMapIndexCount() {
            return this.mapIndex_.size();
         }

         @Override
         public OsmandIndex.MapPart getMapIndex(int index) {
            return this.mapIndex_.get(index);
         }

         public OsmandIndex.FileIndex.Builder setMapIndex(int index, OsmandIndex.MapPart value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureMapIndexIsMutable();
               this.mapIndex_.set(index, value);
               return this;
            }
         }

         public OsmandIndex.FileIndex.Builder setMapIndex(int index, OsmandIndex.MapPart.Builder builderForValue) {
            this.ensureMapIndexIsMutable();
            this.mapIndex_.set(index, builderForValue.build());
            return this;
         }

         public OsmandIndex.FileIndex.Builder addMapIndex(OsmandIndex.MapPart value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureMapIndexIsMutable();
               this.mapIndex_.add(value);
               return this;
            }
         }

         public OsmandIndex.FileIndex.Builder addMapIndex(int index, OsmandIndex.MapPart value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureMapIndexIsMutable();
               this.mapIndex_.add(index, value);
               return this;
            }
         }

         public OsmandIndex.FileIndex.Builder addMapIndex(OsmandIndex.MapPart.Builder builderForValue) {
            this.ensureMapIndexIsMutable();
            this.mapIndex_.add(builderForValue.build());
            return this;
         }

         public OsmandIndex.FileIndex.Builder addMapIndex(int index, OsmandIndex.MapPart.Builder builderForValue) {
            this.ensureMapIndexIsMutable();
            this.mapIndex_.add(index, builderForValue.build());
            return this;
         }

         public OsmandIndex.FileIndex.Builder addAllMapIndex(Iterable<? extends OsmandIndex.MapPart> values) {
            this.ensureMapIndexIsMutable();
            GeneratedMessageLite.Builder.addAll(values, this.mapIndex_);
            return this;
         }

         public OsmandIndex.FileIndex.Builder clearMapIndex() {
            this.mapIndex_ = Collections.emptyList();
            this.bitField0_ &= -129;
            return this;
         }

         public OsmandIndex.FileIndex.Builder removeMapIndex(int index) {
            this.ensureMapIndexIsMutable();
            this.mapIndex_.remove(index);
            return this;
         }

         private void ensureRoutingIndexIsMutable() {
            if ((this.bitField0_ & 256) != 256) {
               this.routingIndex_ = new ArrayList<>(this.routingIndex_);
               this.bitField0_ |= 256;
            }
         }

         @Override
         public List<OsmandIndex.RoutingPart> getRoutingIndexList() {
            return Collections.unmodifiableList(this.routingIndex_);
         }

         @Override
         public int getRoutingIndexCount() {
            return this.routingIndex_.size();
         }

         @Override
         public OsmandIndex.RoutingPart getRoutingIndex(int index) {
            return this.routingIndex_.get(index);
         }

         public OsmandIndex.FileIndex.Builder setRoutingIndex(int index, OsmandIndex.RoutingPart value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureRoutingIndexIsMutable();
               this.routingIndex_.set(index, value);
               return this;
            }
         }

         public OsmandIndex.FileIndex.Builder setRoutingIndex(int index, OsmandIndex.RoutingPart.Builder builderForValue) {
            this.ensureRoutingIndexIsMutable();
            this.routingIndex_.set(index, builderForValue.build());
            return this;
         }

         public OsmandIndex.FileIndex.Builder addRoutingIndex(OsmandIndex.RoutingPart value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureRoutingIndexIsMutable();
               this.routingIndex_.add(value);
               return this;
            }
         }

         public OsmandIndex.FileIndex.Builder addRoutingIndex(int index, OsmandIndex.RoutingPart value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureRoutingIndexIsMutable();
               this.routingIndex_.add(index, value);
               return this;
            }
         }

         public OsmandIndex.FileIndex.Builder addRoutingIndex(OsmandIndex.RoutingPart.Builder builderForValue) {
            this.ensureRoutingIndexIsMutable();
            this.routingIndex_.add(builderForValue.build());
            return this;
         }

         public OsmandIndex.FileIndex.Builder addRoutingIndex(int index, OsmandIndex.RoutingPart.Builder builderForValue) {
            this.ensureRoutingIndexIsMutable();
            this.routingIndex_.add(index, builderForValue.build());
            return this;
         }

         public OsmandIndex.FileIndex.Builder addAllRoutingIndex(Iterable<? extends OsmandIndex.RoutingPart> values) {
            this.ensureRoutingIndexIsMutable();
            GeneratedMessageLite.Builder.addAll(values, this.routingIndex_);
            return this;
         }

         public OsmandIndex.FileIndex.Builder clearRoutingIndex() {
            this.routingIndex_ = Collections.emptyList();
            this.bitField0_ &= -257;
            return this;
         }

         public OsmandIndex.FileIndex.Builder removeRoutingIndex(int index) {
            this.ensureRoutingIndexIsMutable();
            this.routingIndex_.remove(index);
            return this;
         }
      }
   }

   public interface FileIndexOrBuilder extends MessageLiteOrBuilder {
      boolean hasSize();

      long getSize();

      boolean hasDateModified();

      long getDateModified();

      boolean hasFileName();

      String getFileName();

      ByteString getFileNameBytes();

      boolean hasVersion();

      int getVersion();

      List<OsmandIndex.AddressPart> getAddressIndexList();

      OsmandIndex.AddressPart getAddressIndex(int var1);

      int getAddressIndexCount();

      List<OsmandIndex.TransportPart> getTransportIndexList();

      OsmandIndex.TransportPart getTransportIndex(int var1);

      int getTransportIndexCount();

      List<OsmandIndex.PoiPart> getPoiIndexList();

      OsmandIndex.PoiPart getPoiIndex(int var1);

      int getPoiIndexCount();

      List<OsmandIndex.MapPart> getMapIndexList();

      OsmandIndex.MapPart getMapIndex(int var1);

      int getMapIndexCount();

      List<OsmandIndex.RoutingPart> getRoutingIndexList();

      OsmandIndex.RoutingPart getRoutingIndex(int var1);

      int getRoutingIndexCount();
   }

   public static final class MapLevel extends GeneratedMessageLite implements OsmandIndex.MapLevelOrBuilder {
      private static final OsmandIndex.MapLevel defaultInstance = new OsmandIndex.MapLevel(true);
      public static Parser<OsmandIndex.MapLevel> PARSER = new AbstractParser<OsmandIndex.MapLevel>() {
         public OsmandIndex.MapLevel parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return new OsmandIndex.MapLevel(input, extensionRegistry);
         }
      };
      private int bitField0_;
      public static final int SIZE_FIELD_NUMBER = 1;
      private long size_;
      public static final int OFFSET_FIELD_NUMBER = 2;
      private long offset_;
      public static final int LEFT_FIELD_NUMBER = 4;
      private int left_;
      public static final int RIGHT_FIELD_NUMBER = 5;
      private int right_;
      public static final int TOP_FIELD_NUMBER = 6;
      private int top_;
      public static final int BOTTOM_FIELD_NUMBER = 7;
      private int bottom_;
      public static final int MINZOOM_FIELD_NUMBER = 8;
      private int minzoom_;
      public static final int MAXZOOM_FIELD_NUMBER = 9;
      private int maxzoom_;
      private byte memoizedIsInitialized = -1;
      private int memoizedSerializedSize = -1;
      private static final long serialVersionUID = 0L;

      private MapLevel(GeneratedMessageLite.Builder builder) {
         super(builder);
      }

      private MapLevel(boolean noInit) {
      }

      public static OsmandIndex.MapLevel getDefaultInstance() {
         return defaultInstance;
      }

      public OsmandIndex.MapLevel getDefaultInstanceForType() {
         return defaultInstance;
      }

      private MapLevel(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         this.initFields();
         int mutable_bitField0_ = 0;

         try {
            boolean done = false;

            while(!done) {
               int tag = input.readTag();
               switch(tag) {
                  case 0:
                     done = true;
                     break;
                  case 8:
                     this.bitField0_ |= 1;
                     this.size_ = input.readInt64();
                     break;
                  case 16:
                     this.bitField0_ |= 2;
                     this.offset_ = input.readInt64();
                     break;
                  case 32:
                     this.bitField0_ |= 4;
                     this.left_ = input.readInt32();
                     break;
                  case 40:
                     this.bitField0_ |= 8;
                     this.right_ = input.readInt32();
                     break;
                  case 48:
                     this.bitField0_ |= 16;
                     this.top_ = input.readInt32();
                     break;
                  case 56:
                     this.bitField0_ |= 32;
                     this.bottom_ = input.readInt32();
                     break;
                  case 64:
                     this.bitField0_ |= 64;
                     this.minzoom_ = input.readInt32();
                     break;
                  case 72:
                     this.bitField0_ |= 128;
                     this.maxzoom_ = input.readInt32();
                     break;
                  default:
                     if (!this.parseUnknownField(input, extensionRegistry, tag)) {
                        done = true;
                     }
               }
            }
         } catch (InvalidProtocolBufferException var10) {
            throw var10.setUnfinishedMessage(this);
         } catch (IOException var11) {
            throw new InvalidProtocolBufferException(var11.getMessage()).setUnfinishedMessage(this);
         } finally {
            this.makeExtensionsImmutable();
         }
      }

      @Override
      public Parser<OsmandIndex.MapLevel> getParserForType() {
         return PARSER;
      }

      @Override
      public boolean hasSize() {
         return (this.bitField0_ & 1) == 1;
      }

      @Override
      public long getSize() {
         return this.size_;
      }

      @Override
      public boolean hasOffset() {
         return (this.bitField0_ & 2) == 2;
      }

      @Override
      public long getOffset() {
         return this.offset_;
      }

      @Override
      public boolean hasLeft() {
         return (this.bitField0_ & 4) == 4;
      }

      @Override
      public int getLeft() {
         return this.left_;
      }

      @Override
      public boolean hasRight() {
         return (this.bitField0_ & 8) == 8;
      }

      @Override
      public int getRight() {
         return this.right_;
      }

      @Override
      public boolean hasTop() {
         return (this.bitField0_ & 16) == 16;
      }

      @Override
      public int getTop() {
         return this.top_;
      }

      @Override
      public boolean hasBottom() {
         return (this.bitField0_ & 32) == 32;
      }

      @Override
      public int getBottom() {
         return this.bottom_;
      }

      @Override
      public boolean hasMinzoom() {
         return (this.bitField0_ & 64) == 64;
      }

      @Override
      public int getMinzoom() {
         return this.minzoom_;
      }

      @Override
      public boolean hasMaxzoom() {
         return (this.bitField0_ & 128) == 128;
      }

      @Override
      public int getMaxzoom() {
         return this.maxzoom_;
      }

      private void initFields() {
         this.size_ = 0L;
         this.offset_ = 0L;
         this.left_ = 0;
         this.right_ = 0;
         this.top_ = 0;
         this.bottom_ = 0;
         this.minzoom_ = 0;
         this.maxzoom_ = 0;
      }

      @Override
      public final boolean isInitialized() {
         byte isInitialized = this.memoizedIsInitialized;
         if (isInitialized != -1) {
            return isInitialized == 1;
         } else if (!this.hasSize()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasOffset()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasLeft()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasRight()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasTop()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasBottom()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else {
            this.memoizedIsInitialized = 1;
            return true;
         }
      }

      @Override
      public void writeTo(CodedOutputStream output) throws IOException {
         this.getSerializedSize();
         if ((this.bitField0_ & 1) == 1) {
            output.writeInt64(1, this.size_);
         }

         if ((this.bitField0_ & 2) == 2) {
            output.writeInt64(2, this.offset_);
         }

         if ((this.bitField0_ & 4) == 4) {
            output.writeInt32(4, this.left_);
         }

         if ((this.bitField0_ & 8) == 8) {
            output.writeInt32(5, this.right_);
         }

         if ((this.bitField0_ & 16) == 16) {
            output.writeInt32(6, this.top_);
         }

         if ((this.bitField0_ & 32) == 32) {
            output.writeInt32(7, this.bottom_);
         }

         if ((this.bitField0_ & 64) == 64) {
            output.writeInt32(8, this.minzoom_);
         }

         if ((this.bitField0_ & 128) == 128) {
            output.writeInt32(9, this.maxzoom_);
         }
      }

      @Override
      public int getSerializedSize() {
         int size = this.memoizedSerializedSize;
         if (size != -1) {
            return size;
         } else {
            size = 0;
            if ((this.bitField0_ & 1) == 1) {
               size += CodedOutputStream.computeInt64Size(1, this.size_);
            }

            if ((this.bitField0_ & 2) == 2) {
               size += CodedOutputStream.computeInt64Size(2, this.offset_);
            }

            if ((this.bitField0_ & 4) == 4) {
               size += CodedOutputStream.computeInt32Size(4, this.left_);
            }

            if ((this.bitField0_ & 8) == 8) {
               size += CodedOutputStream.computeInt32Size(5, this.right_);
            }

            if ((this.bitField0_ & 16) == 16) {
               size += CodedOutputStream.computeInt32Size(6, this.top_);
            }

            if ((this.bitField0_ & 32) == 32) {
               size += CodedOutputStream.computeInt32Size(7, this.bottom_);
            }

            if ((this.bitField0_ & 64) == 64) {
               size += CodedOutputStream.computeInt32Size(8, this.minzoom_);
            }

            if ((this.bitField0_ & 128) == 128) {
               size += CodedOutputStream.computeInt32Size(9, this.maxzoom_);
            }

            this.memoizedSerializedSize = size;
            return size;
         }
      }

      @Override
      protected Object writeReplace() throws ObjectStreamException {
         return super.writeReplace();
      }

      public static OsmandIndex.MapLevel parseFrom(ByteString data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.MapLevel parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.MapLevel parseFrom(byte[] data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.MapLevel parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.MapLevel parseFrom(InputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.MapLevel parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.MapLevel parseDelimitedFrom(InputStream input) throws IOException {
         return PARSER.parseDelimitedFrom(input);
      }

      public static OsmandIndex.MapLevel parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseDelimitedFrom(input, extensionRegistry);
      }

      public static OsmandIndex.MapLevel parseFrom(CodedInputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.MapLevel parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.MapLevel.Builder newBuilder() {
         return OsmandIndex.MapLevel.Builder.create();
      }

      public OsmandIndex.MapLevel.Builder newBuilderForType() {
         return newBuilder();
      }

      public static OsmandIndex.MapLevel.Builder newBuilder(OsmandIndex.MapLevel prototype) {
         return newBuilder().mergeFrom(prototype);
      }

      public OsmandIndex.MapLevel.Builder toBuilder() {
         return newBuilder(this);
      }

      static {
         defaultInstance.initFields();
      }

      public static final class Builder
         extends GeneratedMessageLite.Builder<OsmandIndex.MapLevel, OsmandIndex.MapLevel.Builder>
         implements OsmandIndex.MapLevelOrBuilder {
         private int bitField0_;
         private long size_;
         private long offset_;
         private int left_;
         private int right_;
         private int top_;
         private int bottom_;
         private int minzoom_;
         private int maxzoom_;

         private Builder() {
            this.maybeForceBuilderInitialization();
         }

         private void maybeForceBuilderInitialization() {
         }

         private static OsmandIndex.MapLevel.Builder create() {
            return new OsmandIndex.MapLevel.Builder();
         }

         public OsmandIndex.MapLevel.Builder clear() {
            super.clear();
            this.size_ = 0L;
            this.bitField0_ &= -2;
            this.offset_ = 0L;
            this.bitField0_ &= -3;
            this.left_ = 0;
            this.bitField0_ &= -5;
            this.right_ = 0;
            this.bitField0_ &= -9;
            this.top_ = 0;
            this.bitField0_ &= -17;
            this.bottom_ = 0;
            this.bitField0_ &= -33;
            this.minzoom_ = 0;
            this.bitField0_ &= -65;
            this.maxzoom_ = 0;
            this.bitField0_ &= -129;
            return this;
         }

         public OsmandIndex.MapLevel.Builder clone() {
            return create().mergeFrom(this.buildPartial());
         }

         public OsmandIndex.MapLevel getDefaultInstanceForType() {
            return OsmandIndex.MapLevel.getDefaultInstance();
         }

         public OsmandIndex.MapLevel build() {
            OsmandIndex.MapLevel result = this.buildPartial();
            if (!result.isInitialized()) {
               throw newUninitializedMessageException(result);
            } else {
               return result;
            }
         }

         public OsmandIndex.MapLevel buildPartial() {
            OsmandIndex.MapLevel result = new OsmandIndex.MapLevel(this);
            int from_bitField0_ = this.bitField0_;
            int to_bitField0_ = 0;
            if ((from_bitField0_ & 1) == 1) {
               to_bitField0_ |= 1;
            }

            result.size_ = this.size_;
            if ((from_bitField0_ & 2) == 2) {
               to_bitField0_ |= 2;
            }

            result.offset_ = this.offset_;
            if ((from_bitField0_ & 4) == 4) {
               to_bitField0_ |= 4;
            }

            result.left_ = this.left_;
            if ((from_bitField0_ & 8) == 8) {
               to_bitField0_ |= 8;
            }

            result.right_ = this.right_;
            if ((from_bitField0_ & 16) == 16) {
               to_bitField0_ |= 16;
            }

            result.top_ = this.top_;
            if ((from_bitField0_ & 32) == 32) {
               to_bitField0_ |= 32;
            }

            result.bottom_ = this.bottom_;
            if ((from_bitField0_ & 64) == 64) {
               to_bitField0_ |= 64;
            }

            result.minzoom_ = this.minzoom_;
            if ((from_bitField0_ & 128) == 128) {
               to_bitField0_ |= 128;
            }

            result.maxzoom_ = this.maxzoom_;
            result.bitField0_ = to_bitField0_;
            return result;
         }

         public OsmandIndex.MapLevel.Builder mergeFrom(OsmandIndex.MapLevel other) {
            if (other == OsmandIndex.MapLevel.getDefaultInstance()) {
               return this;
            } else {
               if (other.hasSize()) {
                  this.setSize(other.getSize());
               }

               if (other.hasOffset()) {
                  this.setOffset(other.getOffset());
               }

               if (other.hasLeft()) {
                  this.setLeft(other.getLeft());
               }

               if (other.hasRight()) {
                  this.setRight(other.getRight());
               }

               if (other.hasTop()) {
                  this.setTop(other.getTop());
               }

               if (other.hasBottom()) {
                  this.setBottom(other.getBottom());
               }

               if (other.hasMinzoom()) {
                  this.setMinzoom(other.getMinzoom());
               }

               if (other.hasMaxzoom()) {
                  this.setMaxzoom(other.getMaxzoom());
               }

               return this;
            }
         }

         @Override
         public final boolean isInitialized() {
            if (!this.hasSize()) {
               return false;
            } else if (!this.hasOffset()) {
               return false;
            } else if (!this.hasLeft()) {
               return false;
            } else if (!this.hasRight()) {
               return false;
            } else if (!this.hasTop()) {
               return false;
            } else {
               return this.hasBottom();
            }
         }

         public OsmandIndex.MapLevel.Builder mergeFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            OsmandIndex.MapLevel parsedMessage = null;

            try {
               parsedMessage = OsmandIndex.MapLevel.PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (InvalidProtocolBufferException var8) {
               parsedMessage = (OsmandIndex.MapLevel)var8.getUnfinishedMessage();
               throw var8;
            } finally {
               if (parsedMessage != null) {
                  this.mergeFrom(parsedMessage);
               }
            }

            return this;
         }

         @Override
         public boolean hasSize() {
            return (this.bitField0_ & 1) == 1;
         }

         @Override
         public long getSize() {
            return this.size_;
         }

         public OsmandIndex.MapLevel.Builder setSize(long value) {
            this.bitField0_ |= 1;
            this.size_ = value;
            return this;
         }

         public OsmandIndex.MapLevel.Builder clearSize() {
            this.bitField0_ &= -2;
            this.size_ = 0L;
            return this;
         }

         @Override
         public boolean hasOffset() {
            return (this.bitField0_ & 2) == 2;
         }

         @Override
         public long getOffset() {
            return this.offset_;
         }

         public OsmandIndex.MapLevel.Builder setOffset(long value) {
            this.bitField0_ |= 2;
            this.offset_ = value;
            return this;
         }

         public OsmandIndex.MapLevel.Builder clearOffset() {
            this.bitField0_ &= -3;
            this.offset_ = 0L;
            return this;
         }

         @Override
         public boolean hasLeft() {
            return (this.bitField0_ & 4) == 4;
         }

         @Override
         public int getLeft() {
            return this.left_;
         }

         public OsmandIndex.MapLevel.Builder setLeft(int value) {
            this.bitField0_ |= 4;
            this.left_ = value;
            return this;
         }

         public OsmandIndex.MapLevel.Builder clearLeft() {
            this.bitField0_ &= -5;
            this.left_ = 0;
            return this;
         }

         @Override
         public boolean hasRight() {
            return (this.bitField0_ & 8) == 8;
         }

         @Override
         public int getRight() {
            return this.right_;
         }

         public OsmandIndex.MapLevel.Builder setRight(int value) {
            this.bitField0_ |= 8;
            this.right_ = value;
            return this;
         }

         public OsmandIndex.MapLevel.Builder clearRight() {
            this.bitField0_ &= -9;
            this.right_ = 0;
            return this;
         }

         @Override
         public boolean hasTop() {
            return (this.bitField0_ & 16) == 16;
         }

         @Override
         public int getTop() {
            return this.top_;
         }

         public OsmandIndex.MapLevel.Builder setTop(int value) {
            this.bitField0_ |= 16;
            this.top_ = value;
            return this;
         }

         public OsmandIndex.MapLevel.Builder clearTop() {
            this.bitField0_ &= -17;
            this.top_ = 0;
            return this;
         }

         @Override
         public boolean hasBottom() {
            return (this.bitField0_ & 32) == 32;
         }

         @Override
         public int getBottom() {
            return this.bottom_;
         }

         public OsmandIndex.MapLevel.Builder setBottom(int value) {
            this.bitField0_ |= 32;
            this.bottom_ = value;
            return this;
         }

         public OsmandIndex.MapLevel.Builder clearBottom() {
            this.bitField0_ &= -33;
            this.bottom_ = 0;
            return this;
         }

         @Override
         public boolean hasMinzoom() {
            return (this.bitField0_ & 64) == 64;
         }

         @Override
         public int getMinzoom() {
            return this.minzoom_;
         }

         public OsmandIndex.MapLevel.Builder setMinzoom(int value) {
            this.bitField0_ |= 64;
            this.minzoom_ = value;
            return this;
         }

         public OsmandIndex.MapLevel.Builder clearMinzoom() {
            this.bitField0_ &= -65;
            this.minzoom_ = 0;
            return this;
         }

         @Override
         public boolean hasMaxzoom() {
            return (this.bitField0_ & 128) == 128;
         }

         @Override
         public int getMaxzoom() {
            return this.maxzoom_;
         }

         public OsmandIndex.MapLevel.Builder setMaxzoom(int value) {
            this.bitField0_ |= 128;
            this.maxzoom_ = value;
            return this;
         }

         public OsmandIndex.MapLevel.Builder clearMaxzoom() {
            this.bitField0_ &= -129;
            this.maxzoom_ = 0;
            return this;
         }
      }
   }

   public interface MapLevelOrBuilder extends MessageLiteOrBuilder {
      boolean hasSize();

      long getSize();

      boolean hasOffset();

      long getOffset();

      boolean hasLeft();

      int getLeft();

      boolean hasRight();

      int getRight();

      boolean hasTop();

      int getTop();

      boolean hasBottom();

      int getBottom();

      boolean hasMinzoom();

      int getMinzoom();

      boolean hasMaxzoom();

      int getMaxzoom();
   }

   public static final class MapPart extends GeneratedMessageLite implements OsmandIndex.MapPartOrBuilder {
      private static final OsmandIndex.MapPart defaultInstance = new OsmandIndex.MapPart(true);
      public static Parser<OsmandIndex.MapPart> PARSER = new AbstractParser<OsmandIndex.MapPart>() {
         public OsmandIndex.MapPart parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return new OsmandIndex.MapPart(input, extensionRegistry);
         }
      };
      private int bitField0_;
      public static final int SIZE_FIELD_NUMBER = 1;
      private long size_;
      public static final int OFFSET_FIELD_NUMBER = 2;
      private long offset_;
      public static final int NAME_FIELD_NUMBER = 3;
      private Object name_;
      public static final int LEVELS_FIELD_NUMBER = 5;
      private List<OsmandIndex.MapLevel> levels_;
      private byte memoizedIsInitialized = -1;
      private int memoizedSerializedSize = -1;
      private static final long serialVersionUID = 0L;

      private MapPart(GeneratedMessageLite.Builder builder) {
         super(builder);
      }

      private MapPart(boolean noInit) {
      }

      public static OsmandIndex.MapPart getDefaultInstance() {
         return defaultInstance;
      }

      public OsmandIndex.MapPart getDefaultInstanceForType() {
         return defaultInstance;
      }

      private MapPart(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         this.initFields();
         int mutable_bitField0_ = 0;

         try {
            boolean done = false;

            while(!done) {
               int tag = input.readTag();
               switch(tag) {
                  case 0:
                     done = true;
                     break;
                  case 8:
                     this.bitField0_ |= 1;
                     this.size_ = input.readInt64();
                     break;
                  case 16:
                     this.bitField0_ |= 2;
                     this.offset_ = input.readInt64();
                     break;
                  case 26:
                     this.bitField0_ |= 4;
                     this.name_ = input.readBytes();
                     break;
                  case 42:
                     if ((mutable_bitField0_ & 8) != 8) {
                        this.levels_ = new ArrayList<>();
                        mutable_bitField0_ |= 8;
                     }

                     this.levels_.add(input.readMessage(OsmandIndex.MapLevel.PARSER, extensionRegistry));
                     break;
                  default:
                     if (!this.parseUnknownField(input, extensionRegistry, tag)) {
                        done = true;
                     }
               }
            }
         } catch (InvalidProtocolBufferException var10) {
            throw var10.setUnfinishedMessage(this);
         } catch (IOException var11) {
            throw new InvalidProtocolBufferException(var11.getMessage()).setUnfinishedMessage(this);
         } finally {
            if ((mutable_bitField0_ & 8) == 8) {
               this.levels_ = Collections.unmodifiableList(this.levels_);
            }

            this.makeExtensionsImmutable();
         }
      }

      @Override
      public Parser<OsmandIndex.MapPart> getParserForType() {
         return PARSER;
      }

      @Override
      public boolean hasSize() {
         return (this.bitField0_ & 1) == 1;
      }

      @Override
      public long getSize() {
         return this.size_;
      }

      @Override
      public boolean hasOffset() {
         return (this.bitField0_ & 2) == 2;
      }

      @Override
      public long getOffset() {
         return this.offset_;
      }

      @Override
      public boolean hasName() {
         return (this.bitField0_ & 4) == 4;
      }

      @Override
      public String getName() {
         Object ref = this.name_;
         if (ref instanceof String) {
            return (String)ref;
         } else {
            ByteString bs = (ByteString)ref;
            String s = bs.toStringUtf8();
            if (bs.isValidUtf8()) {
               this.name_ = s;
            }

            return s;
         }
      }

      @Override
      public ByteString getNameBytes() {
         Object ref = this.name_;
         if (ref instanceof String) {
            ByteString b = ByteString.copyFromUtf8((String)ref);
            this.name_ = b;
            return b;
         } else {
            return (ByteString)ref;
         }
      }

      @Override
      public List<OsmandIndex.MapLevel> getLevelsList() {
         return this.levels_;
      }

      public List<? extends OsmandIndex.MapLevelOrBuilder> getLevelsOrBuilderList() {
         return this.levels_;
      }

      @Override
      public int getLevelsCount() {
         return this.levels_.size();
      }

      @Override
      public OsmandIndex.MapLevel getLevels(int index) {
         return this.levels_.get(index);
      }

      public OsmandIndex.MapLevelOrBuilder getLevelsOrBuilder(int index) {
         return this.levels_.get(index);
      }

      private void initFields() {
         this.size_ = 0L;
         this.offset_ = 0L;
         this.name_ = "";
         this.levels_ = Collections.emptyList();
      }

      @Override
      public final boolean isInitialized() {
         byte isInitialized = this.memoizedIsInitialized;
         if (isInitialized != -1) {
            return isInitialized == 1;
         } else if (!this.hasSize()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasOffset()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else {
            for(int i = 0; i < this.getLevelsCount(); ++i) {
               if (!this.getLevels(i).isInitialized()) {
                  this.memoizedIsInitialized = 0;
                  return false;
               }
            }

            this.memoizedIsInitialized = 1;
            return true;
         }
      }

      @Override
      public void writeTo(CodedOutputStream output) throws IOException {
         this.getSerializedSize();
         if ((this.bitField0_ & 1) == 1) {
            output.writeInt64(1, this.size_);
         }

         if ((this.bitField0_ & 2) == 2) {
            output.writeInt64(2, this.offset_);
         }

         if ((this.bitField0_ & 4) == 4) {
            output.writeBytes(3, this.getNameBytes());
         }

         for(int i = 0; i < this.levels_.size(); ++i) {
            output.writeMessage(5, this.levels_.get(i));
         }
      }

      @Override
      public int getSerializedSize() {
         int size = this.memoizedSerializedSize;
         if (size != -1) {
            return size;
         } else {
            size = 0;
            if ((this.bitField0_ & 1) == 1) {
               size += CodedOutputStream.computeInt64Size(1, this.size_);
            }

            if ((this.bitField0_ & 2) == 2) {
               size += CodedOutputStream.computeInt64Size(2, this.offset_);
            }

            if ((this.bitField0_ & 4) == 4) {
               size += CodedOutputStream.computeBytesSize(3, this.getNameBytes());
            }

            for(int i = 0; i < this.levels_.size(); ++i) {
               size += CodedOutputStream.computeMessageSize(5, this.levels_.get(i));
            }

            this.memoizedSerializedSize = size;
            return size;
         }
      }

      @Override
      protected Object writeReplace() throws ObjectStreamException {
         return super.writeReplace();
      }

      public static OsmandIndex.MapPart parseFrom(ByteString data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.MapPart parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.MapPart parseFrom(byte[] data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.MapPart parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.MapPart parseFrom(InputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.MapPart parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.MapPart parseDelimitedFrom(InputStream input) throws IOException {
         return PARSER.parseDelimitedFrom(input);
      }

      public static OsmandIndex.MapPart parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseDelimitedFrom(input, extensionRegistry);
      }

      public static OsmandIndex.MapPart parseFrom(CodedInputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.MapPart parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.MapPart.Builder newBuilder() {
         return OsmandIndex.MapPart.Builder.create();
      }

      public OsmandIndex.MapPart.Builder newBuilderForType() {
         return newBuilder();
      }

      public static OsmandIndex.MapPart.Builder newBuilder(OsmandIndex.MapPart prototype) {
         return newBuilder().mergeFrom(prototype);
      }

      public OsmandIndex.MapPart.Builder toBuilder() {
         return newBuilder(this);
      }

      static {
         defaultInstance.initFields();
      }

      public static final class Builder
         extends GeneratedMessageLite.Builder<OsmandIndex.MapPart, OsmandIndex.MapPart.Builder>
         implements OsmandIndex.MapPartOrBuilder {
         private int bitField0_;
         private long size_;
         private long offset_;
         private Object name_ = "";
         private List<OsmandIndex.MapLevel> levels_ = Collections.emptyList();

         private Builder() {
            this.maybeForceBuilderInitialization();
         }

         private void maybeForceBuilderInitialization() {
         }

         private static OsmandIndex.MapPart.Builder create() {
            return new OsmandIndex.MapPart.Builder();
         }

         public OsmandIndex.MapPart.Builder clear() {
            super.clear();
            this.size_ = 0L;
            this.bitField0_ &= -2;
            this.offset_ = 0L;
            this.bitField0_ &= -3;
            this.name_ = "";
            this.bitField0_ &= -5;
            this.levels_ = Collections.emptyList();
            this.bitField0_ &= -9;
            return this;
         }

         public OsmandIndex.MapPart.Builder clone() {
            return create().mergeFrom(this.buildPartial());
         }

         public OsmandIndex.MapPart getDefaultInstanceForType() {
            return OsmandIndex.MapPart.getDefaultInstance();
         }

         public OsmandIndex.MapPart build() {
            OsmandIndex.MapPart result = this.buildPartial();
            if (!result.isInitialized()) {
               throw newUninitializedMessageException(result);
            } else {
               return result;
            }
         }

         public OsmandIndex.MapPart buildPartial() {
            OsmandIndex.MapPart result = new OsmandIndex.MapPart(this);
            int from_bitField0_ = this.bitField0_;
            int to_bitField0_ = 0;
            if ((from_bitField0_ & 1) == 1) {
               to_bitField0_ |= 1;
            }

            result.size_ = this.size_;
            if ((from_bitField0_ & 2) == 2) {
               to_bitField0_ |= 2;
            }

            result.offset_ = this.offset_;
            if ((from_bitField0_ & 4) == 4) {
               to_bitField0_ |= 4;
            }

            result.name_ = this.name_;
            if ((this.bitField0_ & 8) == 8) {
               this.levels_ = Collections.unmodifiableList(this.levels_);
               this.bitField0_ &= -9;
            }

            result.levels_ = this.levels_;
            result.bitField0_ = to_bitField0_;
            return result;
         }

         public OsmandIndex.MapPart.Builder mergeFrom(OsmandIndex.MapPart other) {
            if (other == OsmandIndex.MapPart.getDefaultInstance()) {
               return this;
            } else {
               if (other.hasSize()) {
                  this.setSize(other.getSize());
               }

               if (other.hasOffset()) {
                  this.setOffset(other.getOffset());
               }

               if (other.hasName()) {
                  this.bitField0_ |= 4;
                  this.name_ = other.name_;
               }

               if (!other.levels_.isEmpty()) {
                  if (this.levels_.isEmpty()) {
                     this.levels_ = other.levels_;
                     this.bitField0_ &= -9;
                  } else {
                     this.ensureLevelsIsMutable();
                     this.levels_.addAll(other.levels_);
                  }
               }

               return this;
            }
         }

         @Override
         public final boolean isInitialized() {
            if (!this.hasSize()) {
               return false;
            } else if (!this.hasOffset()) {
               return false;
            } else {
               for(int i = 0; i < this.getLevelsCount(); ++i) {
                  if (!this.getLevels(i).isInitialized()) {
                     return false;
                  }
               }

               return true;
            }
         }

         public OsmandIndex.MapPart.Builder mergeFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            OsmandIndex.MapPart parsedMessage = null;

            try {
               parsedMessage = OsmandIndex.MapPart.PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (InvalidProtocolBufferException var8) {
               parsedMessage = (OsmandIndex.MapPart)var8.getUnfinishedMessage();
               throw var8;
            } finally {
               if (parsedMessage != null) {
                  this.mergeFrom(parsedMessage);
               }
            }

            return this;
         }

         @Override
         public boolean hasSize() {
            return (this.bitField0_ & 1) == 1;
         }

         @Override
         public long getSize() {
            return this.size_;
         }

         public OsmandIndex.MapPart.Builder setSize(long value) {
            this.bitField0_ |= 1;
            this.size_ = value;
            return this;
         }

         public OsmandIndex.MapPart.Builder clearSize() {
            this.bitField0_ &= -2;
            this.size_ = 0L;
            return this;
         }

         @Override
         public boolean hasOffset() {
            return (this.bitField0_ & 2) == 2;
         }

         @Override
         public long getOffset() {
            return this.offset_;
         }

         public OsmandIndex.MapPart.Builder setOffset(long value) {
            this.bitField0_ |= 2;
            this.offset_ = value;
            return this;
         }

         public OsmandIndex.MapPart.Builder clearOffset() {
            this.bitField0_ &= -3;
            this.offset_ = 0L;
            return this;
         }

         @Override
         public boolean hasName() {
            return (this.bitField0_ & 4) == 4;
         }

         @Override
         public String getName() {
            Object ref = this.name_;
            if (!(ref instanceof String)) {
               String s = ((ByteString)ref).toStringUtf8();
               this.name_ = s;
               return s;
            } else {
               return (String)ref;
            }
         }

         @Override
         public ByteString getNameBytes() {
            Object ref = this.name_;
            if (ref instanceof String) {
               ByteString b = ByteString.copyFromUtf8((String)ref);
               this.name_ = b;
               return b;
            } else {
               return (ByteString)ref;
            }
         }

         public OsmandIndex.MapPart.Builder setName(String value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.bitField0_ |= 4;
               this.name_ = value;
               return this;
            }
         }

         public OsmandIndex.MapPart.Builder clearName() {
            this.bitField0_ &= -5;
            this.name_ = OsmandIndex.MapPart.getDefaultInstance().getName();
            return this;
         }

         public OsmandIndex.MapPart.Builder setNameBytes(ByteString value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.bitField0_ |= 4;
               this.name_ = value;
               return this;
            }
         }

         private void ensureLevelsIsMutable() {
            if ((this.bitField0_ & 8) != 8) {
               this.levels_ = new ArrayList<>(this.levels_);
               this.bitField0_ |= 8;
            }
         }

         @Override
         public List<OsmandIndex.MapLevel> getLevelsList() {
            return Collections.unmodifiableList(this.levels_);
         }

         @Override
         public int getLevelsCount() {
            return this.levels_.size();
         }

         @Override
         public OsmandIndex.MapLevel getLevels(int index) {
            return this.levels_.get(index);
         }

         public OsmandIndex.MapPart.Builder setLevels(int index, OsmandIndex.MapLevel value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureLevelsIsMutable();
               this.levels_.set(index, value);
               return this;
            }
         }

         public OsmandIndex.MapPart.Builder setLevels(int index, OsmandIndex.MapLevel.Builder builderForValue) {
            this.ensureLevelsIsMutable();
            this.levels_.set(index, builderForValue.build());
            return this;
         }

         public OsmandIndex.MapPart.Builder addLevels(OsmandIndex.MapLevel value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureLevelsIsMutable();
               this.levels_.add(value);
               return this;
            }
         }

         public OsmandIndex.MapPart.Builder addLevels(int index, OsmandIndex.MapLevel value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureLevelsIsMutable();
               this.levels_.add(index, value);
               return this;
            }
         }

         public OsmandIndex.MapPart.Builder addLevels(OsmandIndex.MapLevel.Builder builderForValue) {
            this.ensureLevelsIsMutable();
            this.levels_.add(builderForValue.build());
            return this;
         }

         public OsmandIndex.MapPart.Builder addLevels(int index, OsmandIndex.MapLevel.Builder builderForValue) {
            this.ensureLevelsIsMutable();
            this.levels_.add(index, builderForValue.build());
            return this;
         }

         public OsmandIndex.MapPart.Builder addAllLevels(Iterable<? extends OsmandIndex.MapLevel> values) {
            this.ensureLevelsIsMutable();
            GeneratedMessageLite.Builder.addAll(values, this.levels_);
            return this;
         }

         public OsmandIndex.MapPart.Builder clearLevels() {
            this.levels_ = Collections.emptyList();
            this.bitField0_ &= -9;
            return this;
         }

         public OsmandIndex.MapPart.Builder removeLevels(int index) {
            this.ensureLevelsIsMutable();
            this.levels_.remove(index);
            return this;
         }
      }
   }

   public interface MapPartOrBuilder extends MessageLiteOrBuilder {
      boolean hasSize();

      long getSize();

      boolean hasOffset();

      long getOffset();

      boolean hasName();

      String getName();

      ByteString getNameBytes();

      List<OsmandIndex.MapLevel> getLevelsList();

      OsmandIndex.MapLevel getLevels(int var1);

      int getLevelsCount();
   }

   public static final class OsmAndStoredIndex extends GeneratedMessageLite implements OsmandIndex.OsmAndStoredIndexOrBuilder {
      private static final OsmandIndex.OsmAndStoredIndex defaultInstance = new OsmandIndex.OsmAndStoredIndex(true);
      public static Parser<OsmandIndex.OsmAndStoredIndex> PARSER = new AbstractParser<OsmandIndex.OsmAndStoredIndex>() {
         public OsmandIndex.OsmAndStoredIndex parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return new OsmandIndex.OsmAndStoredIndex(input, extensionRegistry);
         }
      };
      private int bitField0_;
      public static final int VERSION_FIELD_NUMBER = 1;
      private int version_;
      public static final int DATECREATED_FIELD_NUMBER = 18;
      private long dateCreated_;
      public static final int FILEINDEX_FIELD_NUMBER = 7;
      private List<OsmandIndex.FileIndex> fileIndex_;
      private byte memoizedIsInitialized = -1;
      private int memoizedSerializedSize = -1;
      private static final long serialVersionUID = 0L;

      private OsmAndStoredIndex(GeneratedMessageLite.Builder builder) {
         super(builder);
      }

      private OsmAndStoredIndex(boolean noInit) {
      }

      public static OsmandIndex.OsmAndStoredIndex getDefaultInstance() {
         return defaultInstance;
      }

      public OsmandIndex.OsmAndStoredIndex getDefaultInstanceForType() {
         return defaultInstance;
      }

      private OsmAndStoredIndex(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         this.initFields();
         int mutable_bitField0_ = 0;

         try {
            boolean done = false;

            while(!done) {
               int tag = input.readTag();
               switch(tag) {
                  case 0:
                     done = true;
                     break;
                  case 8:
                     this.bitField0_ |= 1;
                     this.version_ = input.readUInt32();
                     break;
                  case 58:
                     if ((mutable_bitField0_ & 4) != 4) {
                        this.fileIndex_ = new ArrayList<>();
                        mutable_bitField0_ |= 4;
                     }

                     this.fileIndex_.add(input.readMessage(OsmandIndex.FileIndex.PARSER, extensionRegistry));
                     break;
                  case 144:
                     this.bitField0_ |= 2;
                     this.dateCreated_ = input.readInt64();
                     break;
                  default:
                     if (!this.parseUnknownField(input, extensionRegistry, tag)) {
                        done = true;
                     }
               }
            }
         } catch (InvalidProtocolBufferException var10) {
            throw var10.setUnfinishedMessage(this);
         } catch (IOException var11) {
            throw new InvalidProtocolBufferException(var11.getMessage()).setUnfinishedMessage(this);
         } finally {
            if ((mutable_bitField0_ & 4) == 4) {
               this.fileIndex_ = Collections.unmodifiableList(this.fileIndex_);
            }

            this.makeExtensionsImmutable();
         }
      }

      @Override
      public Parser<OsmandIndex.OsmAndStoredIndex> getParserForType() {
         return PARSER;
      }

      @Override
      public boolean hasVersion() {
         return (this.bitField0_ & 1) == 1;
      }

      @Override
      public int getVersion() {
         return this.version_;
      }

      @Override
      public boolean hasDateCreated() {
         return (this.bitField0_ & 2) == 2;
      }

      @Override
      public long getDateCreated() {
         return this.dateCreated_;
      }

      @Override
      public List<OsmandIndex.FileIndex> getFileIndexList() {
         return this.fileIndex_;
      }

      public List<? extends OsmandIndex.FileIndexOrBuilder> getFileIndexOrBuilderList() {
         return this.fileIndex_;
      }

      @Override
      public int getFileIndexCount() {
         return this.fileIndex_.size();
      }

      @Override
      public OsmandIndex.FileIndex getFileIndex(int index) {
         return this.fileIndex_.get(index);
      }

      public OsmandIndex.FileIndexOrBuilder getFileIndexOrBuilder(int index) {
         return this.fileIndex_.get(index);
      }

      private void initFields() {
         this.version_ = 0;
         this.dateCreated_ = 0L;
         this.fileIndex_ = Collections.emptyList();
      }

      @Override
      public final boolean isInitialized() {
         byte isInitialized = this.memoizedIsInitialized;
         if (isInitialized != -1) {
            return isInitialized == 1;
         } else if (!this.hasVersion()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasDateCreated()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else {
            for(int i = 0; i < this.getFileIndexCount(); ++i) {
               if (!this.getFileIndex(i).isInitialized()) {
                  this.memoizedIsInitialized = 0;
                  return false;
               }
            }

            this.memoizedIsInitialized = 1;
            return true;
         }
      }

      @Override
      public void writeTo(CodedOutputStream output) throws IOException {
         this.getSerializedSize();
         if ((this.bitField0_ & 1) == 1) {
            output.writeUInt32(1, this.version_);
         }

         for(int i = 0; i < this.fileIndex_.size(); ++i) {
            output.writeMessage(7, this.fileIndex_.get(i));
         }

         if ((this.bitField0_ & 2) == 2) {
            output.writeInt64(18, this.dateCreated_);
         }
      }

      @Override
      public int getSerializedSize() {
         int size = this.memoizedSerializedSize;
         if (size != -1) {
            return size;
         } else {
            size = 0;
            if ((this.bitField0_ & 1) == 1) {
               size += CodedOutputStream.computeUInt32Size(1, this.version_);
            }

            for(int i = 0; i < this.fileIndex_.size(); ++i) {
               size += CodedOutputStream.computeMessageSize(7, this.fileIndex_.get(i));
            }

            if ((this.bitField0_ & 2) == 2) {
               size += CodedOutputStream.computeInt64Size(18, this.dateCreated_);
            }

            this.memoizedSerializedSize = size;
            return size;
         }
      }

      @Override
      protected Object writeReplace() throws ObjectStreamException {
         return super.writeReplace();
      }

      public static OsmandIndex.OsmAndStoredIndex parseFrom(ByteString data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.OsmAndStoredIndex parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.OsmAndStoredIndex parseFrom(byte[] data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.OsmAndStoredIndex parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.OsmAndStoredIndex parseFrom(InputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.OsmAndStoredIndex parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.OsmAndStoredIndex parseDelimitedFrom(InputStream input) throws IOException {
         return PARSER.parseDelimitedFrom(input);
      }

      public static OsmandIndex.OsmAndStoredIndex parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseDelimitedFrom(input, extensionRegistry);
      }

      public static OsmandIndex.OsmAndStoredIndex parseFrom(CodedInputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.OsmAndStoredIndex parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.OsmAndStoredIndex.Builder newBuilder() {
         return OsmandIndex.OsmAndStoredIndex.Builder.create();
      }

      public OsmandIndex.OsmAndStoredIndex.Builder newBuilderForType() {
         return newBuilder();
      }

      public static OsmandIndex.OsmAndStoredIndex.Builder newBuilder(OsmandIndex.OsmAndStoredIndex prototype) {
         return newBuilder().mergeFrom(prototype);
      }

      public OsmandIndex.OsmAndStoredIndex.Builder toBuilder() {
         return newBuilder(this);
      }

      static {
         defaultInstance.initFields();
      }

      public static final class Builder
         extends GeneratedMessageLite.Builder<OsmandIndex.OsmAndStoredIndex, OsmandIndex.OsmAndStoredIndex.Builder>
         implements OsmandIndex.OsmAndStoredIndexOrBuilder {
         private int bitField0_;
         private int version_;
         private long dateCreated_;
         private List<OsmandIndex.FileIndex> fileIndex_ = Collections.emptyList();

         private Builder() {
            this.maybeForceBuilderInitialization();
         }

         private void maybeForceBuilderInitialization() {
         }

         private static OsmandIndex.OsmAndStoredIndex.Builder create() {
            return new OsmandIndex.OsmAndStoredIndex.Builder();
         }

         public OsmandIndex.OsmAndStoredIndex.Builder clear() {
            super.clear();
            this.version_ = 0;
            this.bitField0_ &= -2;
            this.dateCreated_ = 0L;
            this.bitField0_ &= -3;
            this.fileIndex_ = Collections.emptyList();
            this.bitField0_ &= -5;
            return this;
         }

         public OsmandIndex.OsmAndStoredIndex.Builder clone() {
            return create().mergeFrom(this.buildPartial());
         }

         public OsmandIndex.OsmAndStoredIndex getDefaultInstanceForType() {
            return OsmandIndex.OsmAndStoredIndex.getDefaultInstance();
         }

         public OsmandIndex.OsmAndStoredIndex build() {
            OsmandIndex.OsmAndStoredIndex result = this.buildPartial();
            if (!result.isInitialized()) {
               throw newUninitializedMessageException(result);
            } else {
               return result;
            }
         }

         public OsmandIndex.OsmAndStoredIndex buildPartial() {
            OsmandIndex.OsmAndStoredIndex result = new OsmandIndex.OsmAndStoredIndex(this);
            int from_bitField0_ = this.bitField0_;
            int to_bitField0_ = 0;
            if ((from_bitField0_ & 1) == 1) {
               to_bitField0_ |= 1;
            }

            result.version_ = this.version_;
            if ((from_bitField0_ & 2) == 2) {
               to_bitField0_ |= 2;
            }

            result.dateCreated_ = this.dateCreated_;
            if ((this.bitField0_ & 4) == 4) {
               this.fileIndex_ = Collections.unmodifiableList(this.fileIndex_);
               this.bitField0_ &= -5;
            }

            result.fileIndex_ = this.fileIndex_;
            result.bitField0_ = to_bitField0_;
            return result;
         }

         public OsmandIndex.OsmAndStoredIndex.Builder mergeFrom(OsmandIndex.OsmAndStoredIndex other) {
            if (other == OsmandIndex.OsmAndStoredIndex.getDefaultInstance()) {
               return this;
            } else {
               if (other.hasVersion()) {
                  this.setVersion(other.getVersion());
               }

               if (other.hasDateCreated()) {
                  this.setDateCreated(other.getDateCreated());
               }

               if (!other.fileIndex_.isEmpty()) {
                  if (this.fileIndex_.isEmpty()) {
                     this.fileIndex_ = other.fileIndex_;
                     this.bitField0_ &= -5;
                  } else {
                     this.ensureFileIndexIsMutable();
                     this.fileIndex_.addAll(other.fileIndex_);
                  }
               }

               return this;
            }
         }

         @Override
         public final boolean isInitialized() {
            if (!this.hasVersion()) {
               return false;
            } else if (!this.hasDateCreated()) {
               return false;
            } else {
               for(int i = 0; i < this.getFileIndexCount(); ++i) {
                  if (!this.getFileIndex(i).isInitialized()) {
                     return false;
                  }
               }

               return true;
            }
         }

         public OsmandIndex.OsmAndStoredIndex.Builder mergeFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            OsmandIndex.OsmAndStoredIndex parsedMessage = null;

            try {
               parsedMessage = OsmandIndex.OsmAndStoredIndex.PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (InvalidProtocolBufferException var8) {
               parsedMessage = (OsmandIndex.OsmAndStoredIndex)var8.getUnfinishedMessage();
               throw var8;
            } finally {
               if (parsedMessage != null) {
                  this.mergeFrom(parsedMessage);
               }
            }

            return this;
         }

         @Override
         public boolean hasVersion() {
            return (this.bitField0_ & 1) == 1;
         }

         @Override
         public int getVersion() {
            return this.version_;
         }

         public OsmandIndex.OsmAndStoredIndex.Builder setVersion(int value) {
            this.bitField0_ |= 1;
            this.version_ = value;
            return this;
         }

         public OsmandIndex.OsmAndStoredIndex.Builder clearVersion() {
            this.bitField0_ &= -2;
            this.version_ = 0;
            return this;
         }

         @Override
         public boolean hasDateCreated() {
            return (this.bitField0_ & 2) == 2;
         }

         @Override
         public long getDateCreated() {
            return this.dateCreated_;
         }

         public OsmandIndex.OsmAndStoredIndex.Builder setDateCreated(long value) {
            this.bitField0_ |= 2;
            this.dateCreated_ = value;
            return this;
         }

         public OsmandIndex.OsmAndStoredIndex.Builder clearDateCreated() {
            this.bitField0_ &= -3;
            this.dateCreated_ = 0L;
            return this;
         }

         private void ensureFileIndexIsMutable() {
            if ((this.bitField0_ & 4) != 4) {
               this.fileIndex_ = new ArrayList<>(this.fileIndex_);
               this.bitField0_ |= 4;
            }
         }

         @Override
         public List<OsmandIndex.FileIndex> getFileIndexList() {
            return Collections.unmodifiableList(this.fileIndex_);
         }

         @Override
         public int getFileIndexCount() {
            return this.fileIndex_.size();
         }

         @Override
         public OsmandIndex.FileIndex getFileIndex(int index) {
            return this.fileIndex_.get(index);
         }

         public OsmandIndex.OsmAndStoredIndex.Builder setFileIndex(int index, OsmandIndex.FileIndex value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureFileIndexIsMutable();
               this.fileIndex_.set(index, value);
               return this;
            }
         }

         public OsmandIndex.OsmAndStoredIndex.Builder setFileIndex(int index, OsmandIndex.FileIndex.Builder builderForValue) {
            this.ensureFileIndexIsMutable();
            this.fileIndex_.set(index, builderForValue.build());
            return this;
         }

         public OsmandIndex.OsmAndStoredIndex.Builder addFileIndex(OsmandIndex.FileIndex value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureFileIndexIsMutable();
               this.fileIndex_.add(value);
               return this;
            }
         }

         public OsmandIndex.OsmAndStoredIndex.Builder addFileIndex(int index, OsmandIndex.FileIndex value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureFileIndexIsMutable();
               this.fileIndex_.add(index, value);
               return this;
            }
         }

         public OsmandIndex.OsmAndStoredIndex.Builder addFileIndex(OsmandIndex.FileIndex.Builder builderForValue) {
            this.ensureFileIndexIsMutable();
            this.fileIndex_.add(builderForValue.build());
            return this;
         }

         public OsmandIndex.OsmAndStoredIndex.Builder addFileIndex(int index, OsmandIndex.FileIndex.Builder builderForValue) {
            this.ensureFileIndexIsMutable();
            this.fileIndex_.add(index, builderForValue.build());
            return this;
         }

         public OsmandIndex.OsmAndStoredIndex.Builder addAllFileIndex(Iterable<? extends OsmandIndex.FileIndex> values) {
            this.ensureFileIndexIsMutable();
            GeneratedMessageLite.Builder.addAll(values, this.fileIndex_);
            return this;
         }

         public OsmandIndex.OsmAndStoredIndex.Builder clearFileIndex() {
            this.fileIndex_ = Collections.emptyList();
            this.bitField0_ &= -5;
            return this;
         }

         public OsmandIndex.OsmAndStoredIndex.Builder removeFileIndex(int index) {
            this.ensureFileIndexIsMutable();
            this.fileIndex_.remove(index);
            return this;
         }
      }
   }

   public interface OsmAndStoredIndexOrBuilder extends MessageLiteOrBuilder {
      boolean hasVersion();

      int getVersion();

      boolean hasDateCreated();

      long getDateCreated();

      List<OsmandIndex.FileIndex> getFileIndexList();

      OsmandIndex.FileIndex getFileIndex(int var1);

      int getFileIndexCount();
   }

   public static final class PoiPart extends GeneratedMessageLite implements OsmandIndex.PoiPartOrBuilder {
      private static final OsmandIndex.PoiPart defaultInstance = new OsmandIndex.PoiPart(true);
      public static Parser<OsmandIndex.PoiPart> PARSER = new AbstractParser<OsmandIndex.PoiPart>() {
         public OsmandIndex.PoiPart parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return new OsmandIndex.PoiPart(input, extensionRegistry);
         }
      };
      private int bitField0_;
      public static final int SIZE_FIELD_NUMBER = 1;
      private long size_;
      public static final int OFFSET_FIELD_NUMBER = 2;
      private long offset_;
      public static final int NAME_FIELD_NUMBER = 3;
      private Object name_;
      public static final int LEFT_FIELD_NUMBER = 4;
      private int left_;
      public static final int RIGHT_FIELD_NUMBER = 5;
      private int right_;
      public static final int TOP_FIELD_NUMBER = 6;
      private int top_;
      public static final int BOTTOM_FIELD_NUMBER = 7;
      private int bottom_;
      private byte memoizedIsInitialized = -1;
      private int memoizedSerializedSize = -1;
      private static final long serialVersionUID = 0L;

      private PoiPart(GeneratedMessageLite.Builder builder) {
         super(builder);
      }

      private PoiPart(boolean noInit) {
      }

      public static OsmandIndex.PoiPart getDefaultInstance() {
         return defaultInstance;
      }

      public OsmandIndex.PoiPart getDefaultInstanceForType() {
         return defaultInstance;
      }

      private PoiPart(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         this.initFields();
         int mutable_bitField0_ = 0;

         try {
            boolean done = false;

            while(!done) {
               int tag = input.readTag();
               switch(tag) {
                  case 0:
                     done = true;
                     break;
                  case 8:
                     this.bitField0_ |= 1;
                     this.size_ = input.readInt64();
                     break;
                  case 16:
                     this.bitField0_ |= 2;
                     this.offset_ = input.readInt64();
                     break;
                  case 26:
                     this.bitField0_ |= 4;
                     this.name_ = input.readBytes();
                     break;
                  case 32:
                     this.bitField0_ |= 8;
                     this.left_ = input.readInt32();
                     break;
                  case 40:
                     this.bitField0_ |= 16;
                     this.right_ = input.readInt32();
                     break;
                  case 48:
                     this.bitField0_ |= 32;
                     this.top_ = input.readInt32();
                     break;
                  case 56:
                     this.bitField0_ |= 64;
                     this.bottom_ = input.readInt32();
                     break;
                  default:
                     if (!this.parseUnknownField(input, extensionRegistry, tag)) {
                        done = true;
                     }
               }
            }
         } catch (InvalidProtocolBufferException var10) {
            throw var10.setUnfinishedMessage(this);
         } catch (IOException var11) {
            throw new InvalidProtocolBufferException(var11.getMessage()).setUnfinishedMessage(this);
         } finally {
            this.makeExtensionsImmutable();
         }
      }

      @Override
      public Parser<OsmandIndex.PoiPart> getParserForType() {
         return PARSER;
      }

      @Override
      public boolean hasSize() {
         return (this.bitField0_ & 1) == 1;
      }

      @Override
      public long getSize() {
         return this.size_;
      }

      @Override
      public boolean hasOffset() {
         return (this.bitField0_ & 2) == 2;
      }

      @Override
      public long getOffset() {
         return this.offset_;
      }

      @Override
      public boolean hasName() {
         return (this.bitField0_ & 4) == 4;
      }

      @Override
      public String getName() {
         Object ref = this.name_;
         if (ref instanceof String) {
            return (String)ref;
         } else {
            ByteString bs = (ByteString)ref;
            String s = bs.toStringUtf8();
            if (bs.isValidUtf8()) {
               this.name_ = s;
            }

            return s;
         }
      }

      @Override
      public ByteString getNameBytes() {
         Object ref = this.name_;
         if (ref instanceof String) {
            ByteString b = ByteString.copyFromUtf8((String)ref);
            this.name_ = b;
            return b;
         } else {
            return (ByteString)ref;
         }
      }

      @Override
      public boolean hasLeft() {
         return (this.bitField0_ & 8) == 8;
      }

      @Override
      public int getLeft() {
         return this.left_;
      }

      @Override
      public boolean hasRight() {
         return (this.bitField0_ & 16) == 16;
      }

      @Override
      public int getRight() {
         return this.right_;
      }

      @Override
      public boolean hasTop() {
         return (this.bitField0_ & 32) == 32;
      }

      @Override
      public int getTop() {
         return this.top_;
      }

      @Override
      public boolean hasBottom() {
         return (this.bitField0_ & 64) == 64;
      }

      @Override
      public int getBottom() {
         return this.bottom_;
      }

      private void initFields() {
         this.size_ = 0L;
         this.offset_ = 0L;
         this.name_ = "";
         this.left_ = 0;
         this.right_ = 0;
         this.top_ = 0;
         this.bottom_ = 0;
      }

      @Override
      public final boolean isInitialized() {
         byte isInitialized = this.memoizedIsInitialized;
         if (isInitialized != -1) {
            return isInitialized == 1;
         } else if (!this.hasSize()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasOffset()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasLeft()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasRight()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasTop()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasBottom()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else {
            this.memoizedIsInitialized = 1;
            return true;
         }
      }

      @Override
      public void writeTo(CodedOutputStream output) throws IOException {
         this.getSerializedSize();
         if ((this.bitField0_ & 1) == 1) {
            output.writeInt64(1, this.size_);
         }

         if ((this.bitField0_ & 2) == 2) {
            output.writeInt64(2, this.offset_);
         }

         if ((this.bitField0_ & 4) == 4) {
            output.writeBytes(3, this.getNameBytes());
         }

         if ((this.bitField0_ & 8) == 8) {
            output.writeInt32(4, this.left_);
         }

         if ((this.bitField0_ & 16) == 16) {
            output.writeInt32(5, this.right_);
         }

         if ((this.bitField0_ & 32) == 32) {
            output.writeInt32(6, this.top_);
         }

         if ((this.bitField0_ & 64) == 64) {
            output.writeInt32(7, this.bottom_);
         }
      }

      @Override
      public int getSerializedSize() {
         int size = this.memoizedSerializedSize;
         if (size != -1) {
            return size;
         } else {
            size = 0;
            if ((this.bitField0_ & 1) == 1) {
               size += CodedOutputStream.computeInt64Size(1, this.size_);
            }

            if ((this.bitField0_ & 2) == 2) {
               size += CodedOutputStream.computeInt64Size(2, this.offset_);
            }

            if ((this.bitField0_ & 4) == 4) {
               size += CodedOutputStream.computeBytesSize(3, this.getNameBytes());
            }

            if ((this.bitField0_ & 8) == 8) {
               size += CodedOutputStream.computeInt32Size(4, this.left_);
            }

            if ((this.bitField0_ & 16) == 16) {
               size += CodedOutputStream.computeInt32Size(5, this.right_);
            }

            if ((this.bitField0_ & 32) == 32) {
               size += CodedOutputStream.computeInt32Size(6, this.top_);
            }

            if ((this.bitField0_ & 64) == 64) {
               size += CodedOutputStream.computeInt32Size(7, this.bottom_);
            }

            this.memoizedSerializedSize = size;
            return size;
         }
      }

      @Override
      protected Object writeReplace() throws ObjectStreamException {
         return super.writeReplace();
      }

      public static OsmandIndex.PoiPart parseFrom(ByteString data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.PoiPart parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.PoiPart parseFrom(byte[] data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.PoiPart parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.PoiPart parseFrom(InputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.PoiPart parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.PoiPart parseDelimitedFrom(InputStream input) throws IOException {
         return PARSER.parseDelimitedFrom(input);
      }

      public static OsmandIndex.PoiPart parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseDelimitedFrom(input, extensionRegistry);
      }

      public static OsmandIndex.PoiPart parseFrom(CodedInputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.PoiPart parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.PoiPart.Builder newBuilder() {
         return OsmandIndex.PoiPart.Builder.create();
      }

      public OsmandIndex.PoiPart.Builder newBuilderForType() {
         return newBuilder();
      }

      public static OsmandIndex.PoiPart.Builder newBuilder(OsmandIndex.PoiPart prototype) {
         return newBuilder().mergeFrom(prototype);
      }

      public OsmandIndex.PoiPart.Builder toBuilder() {
         return newBuilder(this);
      }

      static {
         defaultInstance.initFields();
      }

      public static final class Builder
         extends GeneratedMessageLite.Builder<OsmandIndex.PoiPart, OsmandIndex.PoiPart.Builder>
         implements OsmandIndex.PoiPartOrBuilder {
         private int bitField0_;
         private long size_;
         private long offset_;
         private Object name_ = "";
         private int left_;
         private int right_;
         private int top_;
         private int bottom_;

         private Builder() {
            this.maybeForceBuilderInitialization();
         }

         private void maybeForceBuilderInitialization() {
         }

         private static OsmandIndex.PoiPart.Builder create() {
            return new OsmandIndex.PoiPart.Builder();
         }

         public OsmandIndex.PoiPart.Builder clear() {
            super.clear();
            this.size_ = 0L;
            this.bitField0_ &= -2;
            this.offset_ = 0L;
            this.bitField0_ &= -3;
            this.name_ = "";
            this.bitField0_ &= -5;
            this.left_ = 0;
            this.bitField0_ &= -9;
            this.right_ = 0;
            this.bitField0_ &= -17;
            this.top_ = 0;
            this.bitField0_ &= -33;
            this.bottom_ = 0;
            this.bitField0_ &= -65;
            return this;
         }

         public OsmandIndex.PoiPart.Builder clone() {
            return create().mergeFrom(this.buildPartial());
         }

         public OsmandIndex.PoiPart getDefaultInstanceForType() {
            return OsmandIndex.PoiPart.getDefaultInstance();
         }

         public OsmandIndex.PoiPart build() {
            OsmandIndex.PoiPart result = this.buildPartial();
            if (!result.isInitialized()) {
               throw newUninitializedMessageException(result);
            } else {
               return result;
            }
         }

         public OsmandIndex.PoiPart buildPartial() {
            OsmandIndex.PoiPart result = new OsmandIndex.PoiPart(this);
            int from_bitField0_ = this.bitField0_;
            int to_bitField0_ = 0;
            if ((from_bitField0_ & 1) == 1) {
               to_bitField0_ |= 1;
            }

            result.size_ = this.size_;
            if ((from_bitField0_ & 2) == 2) {
               to_bitField0_ |= 2;
            }

            result.offset_ = this.offset_;
            if ((from_bitField0_ & 4) == 4) {
               to_bitField0_ |= 4;
            }

            result.name_ = this.name_;
            if ((from_bitField0_ & 8) == 8) {
               to_bitField0_ |= 8;
            }

            result.left_ = this.left_;
            if ((from_bitField0_ & 16) == 16) {
               to_bitField0_ |= 16;
            }

            result.right_ = this.right_;
            if ((from_bitField0_ & 32) == 32) {
               to_bitField0_ |= 32;
            }

            result.top_ = this.top_;
            if ((from_bitField0_ & 64) == 64) {
               to_bitField0_ |= 64;
            }

            result.bottom_ = this.bottom_;
            result.bitField0_ = to_bitField0_;
            return result;
         }

         public OsmandIndex.PoiPart.Builder mergeFrom(OsmandIndex.PoiPart other) {
            if (other == OsmandIndex.PoiPart.getDefaultInstance()) {
               return this;
            } else {
               if (other.hasSize()) {
                  this.setSize(other.getSize());
               }

               if (other.hasOffset()) {
                  this.setOffset(other.getOffset());
               }

               if (other.hasName()) {
                  this.bitField0_ |= 4;
                  this.name_ = other.name_;
               }

               if (other.hasLeft()) {
                  this.setLeft(other.getLeft());
               }

               if (other.hasRight()) {
                  this.setRight(other.getRight());
               }

               if (other.hasTop()) {
                  this.setTop(other.getTop());
               }

               if (other.hasBottom()) {
                  this.setBottom(other.getBottom());
               }

               return this;
            }
         }

         @Override
         public final boolean isInitialized() {
            if (!this.hasSize()) {
               return false;
            } else if (!this.hasOffset()) {
               return false;
            } else if (!this.hasLeft()) {
               return false;
            } else if (!this.hasRight()) {
               return false;
            } else if (!this.hasTop()) {
               return false;
            } else {
               return this.hasBottom();
            }
         }

         public OsmandIndex.PoiPart.Builder mergeFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            OsmandIndex.PoiPart parsedMessage = null;

            try {
               parsedMessage = OsmandIndex.PoiPart.PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (InvalidProtocolBufferException var8) {
               parsedMessage = (OsmandIndex.PoiPart)var8.getUnfinishedMessage();
               throw var8;
            } finally {
               if (parsedMessage != null) {
                  this.mergeFrom(parsedMessage);
               }
            }

            return this;
         }

         @Override
         public boolean hasSize() {
            return (this.bitField0_ & 1) == 1;
         }

         @Override
         public long getSize() {
            return this.size_;
         }

         public OsmandIndex.PoiPart.Builder setSize(long value) {
            this.bitField0_ |= 1;
            this.size_ = value;
            return this;
         }

         public OsmandIndex.PoiPart.Builder clearSize() {
            this.bitField0_ &= -2;
            this.size_ = 0L;
            return this;
         }

         @Override
         public boolean hasOffset() {
            return (this.bitField0_ & 2) == 2;
         }

         @Override
         public long getOffset() {
            return this.offset_;
         }

         public OsmandIndex.PoiPart.Builder setOffset(long value) {
            this.bitField0_ |= 2;
            this.offset_ = value;
            return this;
         }

         public OsmandIndex.PoiPart.Builder clearOffset() {
            this.bitField0_ &= -3;
            this.offset_ = 0L;
            return this;
         }

         @Override
         public boolean hasName() {
            return (this.bitField0_ & 4) == 4;
         }

         @Override
         public String getName() {
            Object ref = this.name_;
            if (!(ref instanceof String)) {
               String s = ((ByteString)ref).toStringUtf8();
               this.name_ = s;
               return s;
            } else {
               return (String)ref;
            }
         }

         @Override
         public ByteString getNameBytes() {
            Object ref = this.name_;
            if (ref instanceof String) {
               ByteString b = ByteString.copyFromUtf8((String)ref);
               this.name_ = b;
               return b;
            } else {
               return (ByteString)ref;
            }
         }

         public OsmandIndex.PoiPart.Builder setName(String value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.bitField0_ |= 4;
               this.name_ = value;
               return this;
            }
         }

         public OsmandIndex.PoiPart.Builder clearName() {
            this.bitField0_ &= -5;
            this.name_ = OsmandIndex.PoiPart.getDefaultInstance().getName();
            return this;
         }

         public OsmandIndex.PoiPart.Builder setNameBytes(ByteString value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.bitField0_ |= 4;
               this.name_ = value;
               return this;
            }
         }

         @Override
         public boolean hasLeft() {
            return (this.bitField0_ & 8) == 8;
         }

         @Override
         public int getLeft() {
            return this.left_;
         }

         public OsmandIndex.PoiPart.Builder setLeft(int value) {
            this.bitField0_ |= 8;
            this.left_ = value;
            return this;
         }

         public OsmandIndex.PoiPart.Builder clearLeft() {
            this.bitField0_ &= -9;
            this.left_ = 0;
            return this;
         }

         @Override
         public boolean hasRight() {
            return (this.bitField0_ & 16) == 16;
         }

         @Override
         public int getRight() {
            return this.right_;
         }

         public OsmandIndex.PoiPart.Builder setRight(int value) {
            this.bitField0_ |= 16;
            this.right_ = value;
            return this;
         }

         public OsmandIndex.PoiPart.Builder clearRight() {
            this.bitField0_ &= -17;
            this.right_ = 0;
            return this;
         }

         @Override
         public boolean hasTop() {
            return (this.bitField0_ & 32) == 32;
         }

         @Override
         public int getTop() {
            return this.top_;
         }

         public OsmandIndex.PoiPart.Builder setTop(int value) {
            this.bitField0_ |= 32;
            this.top_ = value;
            return this;
         }

         public OsmandIndex.PoiPart.Builder clearTop() {
            this.bitField0_ &= -33;
            this.top_ = 0;
            return this;
         }

         @Override
         public boolean hasBottom() {
            return (this.bitField0_ & 64) == 64;
         }

         @Override
         public int getBottom() {
            return this.bottom_;
         }

         public OsmandIndex.PoiPart.Builder setBottom(int value) {
            this.bitField0_ |= 64;
            this.bottom_ = value;
            return this;
         }

         public OsmandIndex.PoiPart.Builder clearBottom() {
            this.bitField0_ &= -65;
            this.bottom_ = 0;
            return this;
         }
      }
   }

   public interface PoiPartOrBuilder extends MessageLiteOrBuilder {
      boolean hasSize();

      long getSize();

      boolean hasOffset();

      long getOffset();

      boolean hasName();

      String getName();

      ByteString getNameBytes();

      boolean hasLeft();

      int getLeft();

      boolean hasRight();

      int getRight();

      boolean hasTop();

      int getTop();

      boolean hasBottom();

      int getBottom();
   }

   public static final class RoutingPart extends GeneratedMessageLite implements OsmandIndex.RoutingPartOrBuilder {
      private static final OsmandIndex.RoutingPart defaultInstance = new OsmandIndex.RoutingPart(true);
      public static Parser<OsmandIndex.RoutingPart> PARSER = new AbstractParser<OsmandIndex.RoutingPart>() {
         public OsmandIndex.RoutingPart parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return new OsmandIndex.RoutingPart(input, extensionRegistry);
         }
      };
      private int bitField0_;
      public static final int SIZE_FIELD_NUMBER = 1;
      private long size_;
      public static final int OFFSET_FIELD_NUMBER = 2;
      private long offset_;
      public static final int NAME_FIELD_NUMBER = 3;
      private Object name_;
      public static final int SUBREGIONS_FIELD_NUMBER = 5;
      private List<OsmandIndex.RoutingSubregion> subregions_;
      private byte memoizedIsInitialized = -1;
      private int memoizedSerializedSize = -1;
      private static final long serialVersionUID = 0L;

      private RoutingPart(GeneratedMessageLite.Builder builder) {
         super(builder);
      }

      private RoutingPart(boolean noInit) {
      }

      public static OsmandIndex.RoutingPart getDefaultInstance() {
         return defaultInstance;
      }

      public OsmandIndex.RoutingPart getDefaultInstanceForType() {
         return defaultInstance;
      }

      private RoutingPart(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         this.initFields();
         int mutable_bitField0_ = 0;

         try {
            boolean done = false;

            while(!done) {
               int tag = input.readTag();
               switch(tag) {
                  case 0:
                     done = true;
                     break;
                  case 8:
                     this.bitField0_ |= 1;
                     this.size_ = input.readInt64();
                     break;
                  case 16:
                     this.bitField0_ |= 2;
                     this.offset_ = input.readInt64();
                     break;
                  case 26:
                     this.bitField0_ |= 4;
                     this.name_ = input.readBytes();
                     break;
                  case 42:
                     if ((mutable_bitField0_ & 8) != 8) {
                        this.subregions_ = new ArrayList<>();
                        mutable_bitField0_ |= 8;
                     }

                     this.subregions_.add(input.readMessage(OsmandIndex.RoutingSubregion.PARSER, extensionRegistry));
                     break;
                  default:
                     if (!this.parseUnknownField(input, extensionRegistry, tag)) {
                        done = true;
                     }
               }
            }
         } catch (InvalidProtocolBufferException var10) {
            throw var10.setUnfinishedMessage(this);
         } catch (IOException var11) {
            throw new InvalidProtocolBufferException(var11.getMessage()).setUnfinishedMessage(this);
         } finally {
            if ((mutable_bitField0_ & 8) == 8) {
               this.subregions_ = Collections.unmodifiableList(this.subregions_);
            }

            this.makeExtensionsImmutable();
         }
      }

      @Override
      public Parser<OsmandIndex.RoutingPart> getParserForType() {
         return PARSER;
      }

      @Override
      public boolean hasSize() {
         return (this.bitField0_ & 1) == 1;
      }

      @Override
      public long getSize() {
         return this.size_;
      }

      @Override
      public boolean hasOffset() {
         return (this.bitField0_ & 2) == 2;
      }

      @Override
      public long getOffset() {
         return this.offset_;
      }

      @Override
      public boolean hasName() {
         return (this.bitField0_ & 4) == 4;
      }

      @Override
      public String getName() {
         Object ref = this.name_;
         if (ref instanceof String) {
            return (String)ref;
         } else {
            ByteString bs = (ByteString)ref;
            String s = bs.toStringUtf8();
            if (bs.isValidUtf8()) {
               this.name_ = s;
            }

            return s;
         }
      }

      @Override
      public ByteString getNameBytes() {
         Object ref = this.name_;
         if (ref instanceof String) {
            ByteString b = ByteString.copyFromUtf8((String)ref);
            this.name_ = b;
            return b;
         } else {
            return (ByteString)ref;
         }
      }

      @Override
      public List<OsmandIndex.RoutingSubregion> getSubregionsList() {
         return this.subregions_;
      }

      public List<? extends OsmandIndex.RoutingSubregionOrBuilder> getSubregionsOrBuilderList() {
         return this.subregions_;
      }

      @Override
      public int getSubregionsCount() {
         return this.subregions_.size();
      }

      @Override
      public OsmandIndex.RoutingSubregion getSubregions(int index) {
         return this.subregions_.get(index);
      }

      public OsmandIndex.RoutingSubregionOrBuilder getSubregionsOrBuilder(int index) {
         return this.subregions_.get(index);
      }

      private void initFields() {
         this.size_ = 0L;
         this.offset_ = 0L;
         this.name_ = "";
         this.subregions_ = Collections.emptyList();
      }

      @Override
      public final boolean isInitialized() {
         byte isInitialized = this.memoizedIsInitialized;
         if (isInitialized != -1) {
            return isInitialized == 1;
         } else if (!this.hasSize()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasOffset()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else {
            for(int i = 0; i < this.getSubregionsCount(); ++i) {
               if (!this.getSubregions(i).isInitialized()) {
                  this.memoizedIsInitialized = 0;
                  return false;
               }
            }

            this.memoizedIsInitialized = 1;
            return true;
         }
      }

      @Override
      public void writeTo(CodedOutputStream output) throws IOException {
         this.getSerializedSize();
         if ((this.bitField0_ & 1) == 1) {
            output.writeInt64(1, this.size_);
         }

         if ((this.bitField0_ & 2) == 2) {
            output.writeInt64(2, this.offset_);
         }

         if ((this.bitField0_ & 4) == 4) {
            output.writeBytes(3, this.getNameBytes());
         }

         for(int i = 0; i < this.subregions_.size(); ++i) {
            output.writeMessage(5, this.subregions_.get(i));
         }
      }

      @Override
      public int getSerializedSize() {
         int size = this.memoizedSerializedSize;
         if (size != -1) {
            return size;
         } else {
            size = 0;
            if ((this.bitField0_ & 1) == 1) {
               size += CodedOutputStream.computeInt64Size(1, this.size_);
            }

            if ((this.bitField0_ & 2) == 2) {
               size += CodedOutputStream.computeInt64Size(2, this.offset_);
            }

            if ((this.bitField0_ & 4) == 4) {
               size += CodedOutputStream.computeBytesSize(3, this.getNameBytes());
            }

            for(int i = 0; i < this.subregions_.size(); ++i) {
               size += CodedOutputStream.computeMessageSize(5, this.subregions_.get(i));
            }

            this.memoizedSerializedSize = size;
            return size;
         }
      }

      @Override
      protected Object writeReplace() throws ObjectStreamException {
         return super.writeReplace();
      }

      public static OsmandIndex.RoutingPart parseFrom(ByteString data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.RoutingPart parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.RoutingPart parseFrom(byte[] data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.RoutingPart parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.RoutingPart parseFrom(InputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.RoutingPart parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.RoutingPart parseDelimitedFrom(InputStream input) throws IOException {
         return PARSER.parseDelimitedFrom(input);
      }

      public static OsmandIndex.RoutingPart parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseDelimitedFrom(input, extensionRegistry);
      }

      public static OsmandIndex.RoutingPart parseFrom(CodedInputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.RoutingPart parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.RoutingPart.Builder newBuilder() {
         return OsmandIndex.RoutingPart.Builder.create();
      }

      public OsmandIndex.RoutingPart.Builder newBuilderForType() {
         return newBuilder();
      }

      public static OsmandIndex.RoutingPart.Builder newBuilder(OsmandIndex.RoutingPart prototype) {
         return newBuilder().mergeFrom(prototype);
      }

      public OsmandIndex.RoutingPart.Builder toBuilder() {
         return newBuilder(this);
      }

      static {
         defaultInstance.initFields();
      }

      public static final class Builder
         extends GeneratedMessageLite.Builder<OsmandIndex.RoutingPart, OsmandIndex.RoutingPart.Builder>
         implements OsmandIndex.RoutingPartOrBuilder {
         private int bitField0_;
         private long size_;
         private long offset_;
         private Object name_ = "";
         private List<OsmandIndex.RoutingSubregion> subregions_ = Collections.emptyList();

         private Builder() {
            this.maybeForceBuilderInitialization();
         }

         private void maybeForceBuilderInitialization() {
         }

         private static OsmandIndex.RoutingPart.Builder create() {
            return new OsmandIndex.RoutingPart.Builder();
         }

         public OsmandIndex.RoutingPart.Builder clear() {
            super.clear();
            this.size_ = 0L;
            this.bitField0_ &= -2;
            this.offset_ = 0L;
            this.bitField0_ &= -3;
            this.name_ = "";
            this.bitField0_ &= -5;
            this.subregions_ = Collections.emptyList();
            this.bitField0_ &= -9;
            return this;
         }

         public OsmandIndex.RoutingPart.Builder clone() {
            return create().mergeFrom(this.buildPartial());
         }

         public OsmandIndex.RoutingPart getDefaultInstanceForType() {
            return OsmandIndex.RoutingPart.getDefaultInstance();
         }

         public OsmandIndex.RoutingPart build() {
            OsmandIndex.RoutingPart result = this.buildPartial();
            if (!result.isInitialized()) {
               throw newUninitializedMessageException(result);
            } else {
               return result;
            }
         }

         public OsmandIndex.RoutingPart buildPartial() {
            OsmandIndex.RoutingPart result = new OsmandIndex.RoutingPart(this);
            int from_bitField0_ = this.bitField0_;
            int to_bitField0_ = 0;
            if ((from_bitField0_ & 1) == 1) {
               to_bitField0_ |= 1;
            }

            result.size_ = this.size_;
            if ((from_bitField0_ & 2) == 2) {
               to_bitField0_ |= 2;
            }

            result.offset_ = this.offset_;
            if ((from_bitField0_ & 4) == 4) {
               to_bitField0_ |= 4;
            }

            result.name_ = this.name_;
            if ((this.bitField0_ & 8) == 8) {
               this.subregions_ = Collections.unmodifiableList(this.subregions_);
               this.bitField0_ &= -9;
            }

            result.subregions_ = this.subregions_;
            result.bitField0_ = to_bitField0_;
            return result;
         }

         public OsmandIndex.RoutingPart.Builder mergeFrom(OsmandIndex.RoutingPart other) {
            if (other == OsmandIndex.RoutingPart.getDefaultInstance()) {
               return this;
            } else {
               if (other.hasSize()) {
                  this.setSize(other.getSize());
               }

               if (other.hasOffset()) {
                  this.setOffset(other.getOffset());
               }

               if (other.hasName()) {
                  this.bitField0_ |= 4;
                  this.name_ = other.name_;
               }

               if (!other.subregions_.isEmpty()) {
                  if (this.subregions_.isEmpty()) {
                     this.subregions_ = other.subregions_;
                     this.bitField0_ &= -9;
                  } else {
                     this.ensureSubregionsIsMutable();
                     this.subregions_.addAll(other.subregions_);
                  }
               }

               return this;
            }
         }

         @Override
         public final boolean isInitialized() {
            if (!this.hasSize()) {
               return false;
            } else if (!this.hasOffset()) {
               return false;
            } else {
               for(int i = 0; i < this.getSubregionsCount(); ++i) {
                  if (!this.getSubregions(i).isInitialized()) {
                     return false;
                  }
               }

               return true;
            }
         }

         public OsmandIndex.RoutingPart.Builder mergeFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            OsmandIndex.RoutingPart parsedMessage = null;

            try {
               parsedMessage = OsmandIndex.RoutingPart.PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (InvalidProtocolBufferException var8) {
               parsedMessage = (OsmandIndex.RoutingPart)var8.getUnfinishedMessage();
               throw var8;
            } finally {
               if (parsedMessage != null) {
                  this.mergeFrom(parsedMessage);
               }
            }

            return this;
         }

         @Override
         public boolean hasSize() {
            return (this.bitField0_ & 1) == 1;
         }

         @Override
         public long getSize() {
            return this.size_;
         }

         public OsmandIndex.RoutingPart.Builder setSize(long value) {
            this.bitField0_ |= 1;
            this.size_ = value;
            return this;
         }

         public OsmandIndex.RoutingPart.Builder clearSize() {
            this.bitField0_ &= -2;
            this.size_ = 0L;
            return this;
         }

         @Override
         public boolean hasOffset() {
            return (this.bitField0_ & 2) == 2;
         }

         @Override
         public long getOffset() {
            return this.offset_;
         }

         public OsmandIndex.RoutingPart.Builder setOffset(long value) {
            this.bitField0_ |= 2;
            this.offset_ = value;
            return this;
         }

         public OsmandIndex.RoutingPart.Builder clearOffset() {
            this.bitField0_ &= -3;
            this.offset_ = 0L;
            return this;
         }

         @Override
         public boolean hasName() {
            return (this.bitField0_ & 4) == 4;
         }

         @Override
         public String getName() {
            Object ref = this.name_;
            if (!(ref instanceof String)) {
               String s = ((ByteString)ref).toStringUtf8();
               this.name_ = s;
               return s;
            } else {
               return (String)ref;
            }
         }

         @Override
         public ByteString getNameBytes() {
            Object ref = this.name_;
            if (ref instanceof String) {
               ByteString b = ByteString.copyFromUtf8((String)ref);
               this.name_ = b;
               return b;
            } else {
               return (ByteString)ref;
            }
         }

         public OsmandIndex.RoutingPart.Builder setName(String value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.bitField0_ |= 4;
               this.name_ = value;
               return this;
            }
         }

         public OsmandIndex.RoutingPart.Builder clearName() {
            this.bitField0_ &= -5;
            this.name_ = OsmandIndex.RoutingPart.getDefaultInstance().getName();
            return this;
         }

         public OsmandIndex.RoutingPart.Builder setNameBytes(ByteString value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.bitField0_ |= 4;
               this.name_ = value;
               return this;
            }
         }

         private void ensureSubregionsIsMutable() {
            if ((this.bitField0_ & 8) != 8) {
               this.subregions_ = new ArrayList<>(this.subregions_);
               this.bitField0_ |= 8;
            }
         }

         @Override
         public List<OsmandIndex.RoutingSubregion> getSubregionsList() {
            return Collections.unmodifiableList(this.subregions_);
         }

         @Override
         public int getSubregionsCount() {
            return this.subregions_.size();
         }

         @Override
         public OsmandIndex.RoutingSubregion getSubregions(int index) {
            return this.subregions_.get(index);
         }

         public OsmandIndex.RoutingPart.Builder setSubregions(int index, OsmandIndex.RoutingSubregion value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureSubregionsIsMutable();
               this.subregions_.set(index, value);
               return this;
            }
         }

         public OsmandIndex.RoutingPart.Builder setSubregions(int index, OsmandIndex.RoutingSubregion.Builder builderForValue) {
            this.ensureSubregionsIsMutable();
            this.subregions_.set(index, builderForValue.build());
            return this;
         }

         public OsmandIndex.RoutingPart.Builder addSubregions(OsmandIndex.RoutingSubregion value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureSubregionsIsMutable();
               this.subregions_.add(value);
               return this;
            }
         }

         public OsmandIndex.RoutingPart.Builder addSubregions(int index, OsmandIndex.RoutingSubregion value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.ensureSubregionsIsMutable();
               this.subregions_.add(index, value);
               return this;
            }
         }

         public OsmandIndex.RoutingPart.Builder addSubregions(OsmandIndex.RoutingSubregion.Builder builderForValue) {
            this.ensureSubregionsIsMutable();
            this.subregions_.add(builderForValue.build());
            return this;
         }

         public OsmandIndex.RoutingPart.Builder addSubregions(int index, OsmandIndex.RoutingSubregion.Builder builderForValue) {
            this.ensureSubregionsIsMutable();
            this.subregions_.add(index, builderForValue.build());
            return this;
         }

         public OsmandIndex.RoutingPart.Builder addAllSubregions(Iterable<? extends OsmandIndex.RoutingSubregion> values) {
            this.ensureSubregionsIsMutable();
            GeneratedMessageLite.Builder.addAll(values, this.subregions_);
            return this;
         }

         public OsmandIndex.RoutingPart.Builder clearSubregions() {
            this.subregions_ = Collections.emptyList();
            this.bitField0_ &= -9;
            return this;
         }

         public OsmandIndex.RoutingPart.Builder removeSubregions(int index) {
            this.ensureSubregionsIsMutable();
            this.subregions_.remove(index);
            return this;
         }
      }
   }

   public interface RoutingPartOrBuilder extends MessageLiteOrBuilder {
      boolean hasSize();

      long getSize();

      boolean hasOffset();

      long getOffset();

      boolean hasName();

      String getName();

      ByteString getNameBytes();

      List<OsmandIndex.RoutingSubregion> getSubregionsList();

      OsmandIndex.RoutingSubregion getSubregions(int var1);

      int getSubregionsCount();
   }

   public static final class RoutingSubregion extends GeneratedMessageLite implements OsmandIndex.RoutingSubregionOrBuilder {
      private static final OsmandIndex.RoutingSubregion defaultInstance = new OsmandIndex.RoutingSubregion(true);
      public static Parser<OsmandIndex.RoutingSubregion> PARSER = new AbstractParser<OsmandIndex.RoutingSubregion>() {
         public OsmandIndex.RoutingSubregion parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return new OsmandIndex.RoutingSubregion(input, extensionRegistry);
         }
      };
      private int bitField0_;
      public static final int SIZE_FIELD_NUMBER = 1;
      private long size_;
      public static final int OFFSET_FIELD_NUMBER = 2;
      private long offset_;
      public static final int BASEMAP_FIELD_NUMBER = 3;
      private boolean basemap_;
      public static final int LEFT_FIELD_NUMBER = 4;
      private int left_;
      public static final int RIGHT_FIELD_NUMBER = 5;
      private int right_;
      public static final int TOP_FIELD_NUMBER = 6;
      private int top_;
      public static final int BOTTOM_FIELD_NUMBER = 7;
      private int bottom_;
      public static final int SHIFTODATA_FIELD_NUMBER = 8;
      private int shifToData_;
      private byte memoizedIsInitialized = -1;
      private int memoizedSerializedSize = -1;
      private static final long serialVersionUID = 0L;

      private RoutingSubregion(GeneratedMessageLite.Builder builder) {
         super(builder);
      }

      private RoutingSubregion(boolean noInit) {
      }

      public static OsmandIndex.RoutingSubregion getDefaultInstance() {
         return defaultInstance;
      }

      public OsmandIndex.RoutingSubregion getDefaultInstanceForType() {
         return defaultInstance;
      }

      private RoutingSubregion(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         this.initFields();
         int mutable_bitField0_ = 0;

         try {
            boolean done = false;

            while(!done) {
               int tag = input.readTag();
               switch(tag) {
                  case 0:
                     done = true;
                     break;
                  case 8:
                     this.bitField0_ |= 1;
                     this.size_ = input.readInt64();
                     break;
                  case 16:
                     this.bitField0_ |= 2;
                     this.offset_ = input.readInt64();
                     break;
                  case 24:
                     this.bitField0_ |= 4;
                     this.basemap_ = input.readBool();
                     break;
                  case 32:
                     this.bitField0_ |= 8;
                     this.left_ = input.readInt32();
                     break;
                  case 40:
                     this.bitField0_ |= 16;
                     this.right_ = input.readInt32();
                     break;
                  case 48:
                     this.bitField0_ |= 32;
                     this.top_ = input.readInt32();
                     break;
                  case 56:
                     this.bitField0_ |= 64;
                     this.bottom_ = input.readInt32();
                     break;
                  case 64:
                     this.bitField0_ |= 128;
                     this.shifToData_ = input.readUInt32();
                     break;
                  default:
                     if (!this.parseUnknownField(input, extensionRegistry, tag)) {
                        done = true;
                     }
               }
            }
         } catch (InvalidProtocolBufferException var10) {
            throw var10.setUnfinishedMessage(this);
         } catch (IOException var11) {
            throw new InvalidProtocolBufferException(var11.getMessage()).setUnfinishedMessage(this);
         } finally {
            this.makeExtensionsImmutable();
         }
      }

      @Override
      public Parser<OsmandIndex.RoutingSubregion> getParserForType() {
         return PARSER;
      }

      @Override
      public boolean hasSize() {
         return (this.bitField0_ & 1) == 1;
      }

      @Override
      public long getSize() {
         return this.size_;
      }

      @Override
      public boolean hasOffset() {
         return (this.bitField0_ & 2) == 2;
      }

      @Override
      public long getOffset() {
         return this.offset_;
      }

      @Override
      public boolean hasBasemap() {
         return (this.bitField0_ & 4) == 4;
      }

      @Override
      public boolean getBasemap() {
         return this.basemap_;
      }

      @Override
      public boolean hasLeft() {
         return (this.bitField0_ & 8) == 8;
      }

      @Override
      public int getLeft() {
         return this.left_;
      }

      @Override
      public boolean hasRight() {
         return (this.bitField0_ & 16) == 16;
      }

      @Override
      public int getRight() {
         return this.right_;
      }

      @Override
      public boolean hasTop() {
         return (this.bitField0_ & 32) == 32;
      }

      @Override
      public int getTop() {
         return this.top_;
      }

      @Override
      public boolean hasBottom() {
         return (this.bitField0_ & 64) == 64;
      }

      @Override
      public int getBottom() {
         return this.bottom_;
      }

      @Override
      public boolean hasShifToData() {
         return (this.bitField0_ & 128) == 128;
      }

      @Override
      public int getShifToData() {
         return this.shifToData_;
      }

      private void initFields() {
         this.size_ = 0L;
         this.offset_ = 0L;
         this.basemap_ = false;
         this.left_ = 0;
         this.right_ = 0;
         this.top_ = 0;
         this.bottom_ = 0;
         this.shifToData_ = 0;
      }

      @Override
      public final boolean isInitialized() {
         byte isInitialized = this.memoizedIsInitialized;
         if (isInitialized != -1) {
            return isInitialized == 1;
         } else if (!this.hasSize()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasOffset()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasLeft()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasRight()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasTop()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasBottom()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasShifToData()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else {
            this.memoizedIsInitialized = 1;
            return true;
         }
      }

      @Override
      public void writeTo(CodedOutputStream output) throws IOException {
         this.getSerializedSize();
         if ((this.bitField0_ & 1) == 1) {
            output.writeInt64(1, this.size_);
         }

         if ((this.bitField0_ & 2) == 2) {
            output.writeInt64(2, this.offset_);
         }

         if ((this.bitField0_ & 4) == 4) {
            output.writeBool(3, this.basemap_);
         }

         if ((this.bitField0_ & 8) == 8) {
            output.writeInt32(4, this.left_);
         }

         if ((this.bitField0_ & 16) == 16) {
            output.writeInt32(5, this.right_);
         }

         if ((this.bitField0_ & 32) == 32) {
            output.writeInt32(6, this.top_);
         }

         if ((this.bitField0_ & 64) == 64) {
            output.writeInt32(7, this.bottom_);
         }

         if ((this.bitField0_ & 128) == 128) {
            output.writeUInt32(8, this.shifToData_);
         }
      }

      @Override
      public int getSerializedSize() {
         int size = this.memoizedSerializedSize;
         if (size != -1) {
            return size;
         } else {
            size = 0;
            if ((this.bitField0_ & 1) == 1) {
               size += CodedOutputStream.computeInt64Size(1, this.size_);
            }

            if ((this.bitField0_ & 2) == 2) {
               size += CodedOutputStream.computeInt64Size(2, this.offset_);
            }

            if ((this.bitField0_ & 4) == 4) {
               size += CodedOutputStream.computeBoolSize(3, this.basemap_);
            }

            if ((this.bitField0_ & 8) == 8) {
               size += CodedOutputStream.computeInt32Size(4, this.left_);
            }

            if ((this.bitField0_ & 16) == 16) {
               size += CodedOutputStream.computeInt32Size(5, this.right_);
            }

            if ((this.bitField0_ & 32) == 32) {
               size += CodedOutputStream.computeInt32Size(6, this.top_);
            }

            if ((this.bitField0_ & 64) == 64) {
               size += CodedOutputStream.computeInt32Size(7, this.bottom_);
            }

            if ((this.bitField0_ & 128) == 128) {
               size += CodedOutputStream.computeUInt32Size(8, this.shifToData_);
            }

            this.memoizedSerializedSize = size;
            return size;
         }
      }

      @Override
      protected Object writeReplace() throws ObjectStreamException {
         return super.writeReplace();
      }

      public static OsmandIndex.RoutingSubregion parseFrom(ByteString data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.RoutingSubregion parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.RoutingSubregion parseFrom(byte[] data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.RoutingSubregion parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.RoutingSubregion parseFrom(InputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.RoutingSubregion parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.RoutingSubregion parseDelimitedFrom(InputStream input) throws IOException {
         return PARSER.parseDelimitedFrom(input);
      }

      public static OsmandIndex.RoutingSubregion parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseDelimitedFrom(input, extensionRegistry);
      }

      public static OsmandIndex.RoutingSubregion parseFrom(CodedInputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.RoutingSubregion parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.RoutingSubregion.Builder newBuilder() {
         return OsmandIndex.RoutingSubregion.Builder.create();
      }

      public OsmandIndex.RoutingSubregion.Builder newBuilderForType() {
         return newBuilder();
      }

      public static OsmandIndex.RoutingSubregion.Builder newBuilder(OsmandIndex.RoutingSubregion prototype) {
         return newBuilder().mergeFrom(prototype);
      }

      public OsmandIndex.RoutingSubregion.Builder toBuilder() {
         return newBuilder(this);
      }

      static {
         defaultInstance.initFields();
      }

      public static final class Builder
         extends GeneratedMessageLite.Builder<OsmandIndex.RoutingSubregion, OsmandIndex.RoutingSubregion.Builder>
         implements OsmandIndex.RoutingSubregionOrBuilder {
         private int bitField0_;
         private long size_;
         private long offset_;
         private boolean basemap_;
         private int left_;
         private int right_;
         private int top_;
         private int bottom_;
         private int shifToData_;

         private Builder() {
            this.maybeForceBuilderInitialization();
         }

         private void maybeForceBuilderInitialization() {
         }

         private static OsmandIndex.RoutingSubregion.Builder create() {
            return new OsmandIndex.RoutingSubregion.Builder();
         }

         public OsmandIndex.RoutingSubregion.Builder clear() {
            super.clear();
            this.size_ = 0L;
            this.bitField0_ &= -2;
            this.offset_ = 0L;
            this.bitField0_ &= -3;
            this.basemap_ = false;
            this.bitField0_ &= -5;
            this.left_ = 0;
            this.bitField0_ &= -9;
            this.right_ = 0;
            this.bitField0_ &= -17;
            this.top_ = 0;
            this.bitField0_ &= -33;
            this.bottom_ = 0;
            this.bitField0_ &= -65;
            this.shifToData_ = 0;
            this.bitField0_ &= -129;
            return this;
         }

         public OsmandIndex.RoutingSubregion.Builder clone() {
            return create().mergeFrom(this.buildPartial());
         }

         public OsmandIndex.RoutingSubregion getDefaultInstanceForType() {
            return OsmandIndex.RoutingSubregion.getDefaultInstance();
         }

         public OsmandIndex.RoutingSubregion build() {
            OsmandIndex.RoutingSubregion result = this.buildPartial();
            if (!result.isInitialized()) {
               throw newUninitializedMessageException(result);
            } else {
               return result;
            }
         }

         public OsmandIndex.RoutingSubregion buildPartial() {
            OsmandIndex.RoutingSubregion result = new OsmandIndex.RoutingSubregion(this);
            int from_bitField0_ = this.bitField0_;
            int to_bitField0_ = 0;
            if ((from_bitField0_ & 1) == 1) {
               to_bitField0_ |= 1;
            }

            result.size_ = this.size_;
            if ((from_bitField0_ & 2) == 2) {
               to_bitField0_ |= 2;
            }

            result.offset_ = this.offset_;
            if ((from_bitField0_ & 4) == 4) {
               to_bitField0_ |= 4;
            }

            result.basemap_ = this.basemap_;
            if ((from_bitField0_ & 8) == 8) {
               to_bitField0_ |= 8;
            }

            result.left_ = this.left_;
            if ((from_bitField0_ & 16) == 16) {
               to_bitField0_ |= 16;
            }

            result.right_ = this.right_;
            if ((from_bitField0_ & 32) == 32) {
               to_bitField0_ |= 32;
            }

            result.top_ = this.top_;
            if ((from_bitField0_ & 64) == 64) {
               to_bitField0_ |= 64;
            }

            result.bottom_ = this.bottom_;
            if ((from_bitField0_ & 128) == 128) {
               to_bitField0_ |= 128;
            }

            result.shifToData_ = this.shifToData_;
            result.bitField0_ = to_bitField0_;
            return result;
         }

         public OsmandIndex.RoutingSubregion.Builder mergeFrom(OsmandIndex.RoutingSubregion other) {
            if (other == OsmandIndex.RoutingSubregion.getDefaultInstance()) {
               return this;
            } else {
               if (other.hasSize()) {
                  this.setSize(other.getSize());
               }

               if (other.hasOffset()) {
                  this.setOffset(other.getOffset());
               }

               if (other.hasBasemap()) {
                  this.setBasemap(other.getBasemap());
               }

               if (other.hasLeft()) {
                  this.setLeft(other.getLeft());
               }

               if (other.hasRight()) {
                  this.setRight(other.getRight());
               }

               if (other.hasTop()) {
                  this.setTop(other.getTop());
               }

               if (other.hasBottom()) {
                  this.setBottom(other.getBottom());
               }

               if (other.hasShifToData()) {
                  this.setShifToData(other.getShifToData());
               }

               return this;
            }
         }

         @Override
         public final boolean isInitialized() {
            if (!this.hasSize()) {
               return false;
            } else if (!this.hasOffset()) {
               return false;
            } else if (!this.hasLeft()) {
               return false;
            } else if (!this.hasRight()) {
               return false;
            } else if (!this.hasTop()) {
               return false;
            } else if (!this.hasBottom()) {
               return false;
            } else {
               return this.hasShifToData();
            }
         }

         public OsmandIndex.RoutingSubregion.Builder mergeFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            OsmandIndex.RoutingSubregion parsedMessage = null;

            try {
               parsedMessage = OsmandIndex.RoutingSubregion.PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (InvalidProtocolBufferException var8) {
               parsedMessage = (OsmandIndex.RoutingSubregion)var8.getUnfinishedMessage();
               throw var8;
            } finally {
               if (parsedMessage != null) {
                  this.mergeFrom(parsedMessage);
               }
            }

            return this;
         }

         @Override
         public boolean hasSize() {
            return (this.bitField0_ & 1) == 1;
         }

         @Override
         public long getSize() {
            return this.size_;
         }

         public OsmandIndex.RoutingSubregion.Builder setSize(long value) {
            this.bitField0_ |= 1;
            this.size_ = value;
            return this;
         }

         public OsmandIndex.RoutingSubregion.Builder clearSize() {
            this.bitField0_ &= -2;
            this.size_ = 0L;
            return this;
         }

         @Override
         public boolean hasOffset() {
            return (this.bitField0_ & 2) == 2;
         }

         @Override
         public long getOffset() {
            return this.offset_;
         }

         public OsmandIndex.RoutingSubregion.Builder setOffset(long value) {
            this.bitField0_ |= 2;
            this.offset_ = value;
            return this;
         }

         public OsmandIndex.RoutingSubregion.Builder clearOffset() {
            this.bitField0_ &= -3;
            this.offset_ = 0L;
            return this;
         }

         @Override
         public boolean hasBasemap() {
            return (this.bitField0_ & 4) == 4;
         }

         @Override
         public boolean getBasemap() {
            return this.basemap_;
         }

         public OsmandIndex.RoutingSubregion.Builder setBasemap(boolean value) {
            this.bitField0_ |= 4;
            this.basemap_ = value;
            return this;
         }

         public OsmandIndex.RoutingSubregion.Builder clearBasemap() {
            this.bitField0_ &= -5;
            this.basemap_ = false;
            return this;
         }

         @Override
         public boolean hasLeft() {
            return (this.bitField0_ & 8) == 8;
         }

         @Override
         public int getLeft() {
            return this.left_;
         }

         public OsmandIndex.RoutingSubregion.Builder setLeft(int value) {
            this.bitField0_ |= 8;
            this.left_ = value;
            return this;
         }

         public OsmandIndex.RoutingSubregion.Builder clearLeft() {
            this.bitField0_ &= -9;
            this.left_ = 0;
            return this;
         }

         @Override
         public boolean hasRight() {
            return (this.bitField0_ & 16) == 16;
         }

         @Override
         public int getRight() {
            return this.right_;
         }

         public OsmandIndex.RoutingSubregion.Builder setRight(int value) {
            this.bitField0_ |= 16;
            this.right_ = value;
            return this;
         }

         public OsmandIndex.RoutingSubregion.Builder clearRight() {
            this.bitField0_ &= -17;
            this.right_ = 0;
            return this;
         }

         @Override
         public boolean hasTop() {
            return (this.bitField0_ & 32) == 32;
         }

         @Override
         public int getTop() {
            return this.top_;
         }

         public OsmandIndex.RoutingSubregion.Builder setTop(int value) {
            this.bitField0_ |= 32;
            this.top_ = value;
            return this;
         }

         public OsmandIndex.RoutingSubregion.Builder clearTop() {
            this.bitField0_ &= -33;
            this.top_ = 0;
            return this;
         }

         @Override
         public boolean hasBottom() {
            return (this.bitField0_ & 64) == 64;
         }

         @Override
         public int getBottom() {
            return this.bottom_;
         }

         public OsmandIndex.RoutingSubregion.Builder setBottom(int value) {
            this.bitField0_ |= 64;
            this.bottom_ = value;
            return this;
         }

         public OsmandIndex.RoutingSubregion.Builder clearBottom() {
            this.bitField0_ &= -65;
            this.bottom_ = 0;
            return this;
         }

         @Override
         public boolean hasShifToData() {
            return (this.bitField0_ & 128) == 128;
         }

         @Override
         public int getShifToData() {
            return this.shifToData_;
         }

         public OsmandIndex.RoutingSubregion.Builder setShifToData(int value) {
            this.bitField0_ |= 128;
            this.shifToData_ = value;
            return this;
         }

         public OsmandIndex.RoutingSubregion.Builder clearShifToData() {
            this.bitField0_ &= -129;
            this.shifToData_ = 0;
            return this;
         }
      }
   }

   public interface RoutingSubregionOrBuilder extends MessageLiteOrBuilder {
      boolean hasSize();

      long getSize();

      boolean hasOffset();

      long getOffset();

      boolean hasBasemap();

      boolean getBasemap();

      boolean hasLeft();

      int getLeft();

      boolean hasRight();

      int getRight();

      boolean hasTop();

      int getTop();

      boolean hasBottom();

      int getBottom();

      boolean hasShifToData();

      int getShifToData();
   }

   public static final class TransportPart extends GeneratedMessageLite implements OsmandIndex.TransportPartOrBuilder {
      private static final OsmandIndex.TransportPart defaultInstance = new OsmandIndex.TransportPart(true);
      public static Parser<OsmandIndex.TransportPart> PARSER = new AbstractParser<OsmandIndex.TransportPart>() {
         public OsmandIndex.TransportPart parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return new OsmandIndex.TransportPart(input, extensionRegistry);
         }
      };
      private int bitField0_;
      public static final int SIZE_FIELD_NUMBER = 1;
      private long size_;
      public static final int OFFSET_FIELD_NUMBER = 2;
      private long offset_;
      public static final int NAME_FIELD_NUMBER = 3;
      private Object name_;
      public static final int LEFT_FIELD_NUMBER = 4;
      private int left_;
      public static final int RIGHT_FIELD_NUMBER = 5;
      private int right_;
      public static final int TOP_FIELD_NUMBER = 6;
      private int top_;
      public static final int BOTTOM_FIELD_NUMBER = 7;
      private int bottom_;
      public static final int STRINGTABLEOFFSET_FIELD_NUMBER = 8;
      private int stringTableOffset_;
      public static final int STRINGTABLELENGTH_FIELD_NUMBER = 9;
      private int stringTableLength_;
      public static final int STOPSTABLEOFFSET_FIELD_NUMBER = 10;
      private int stopsTableOffset_;
      public static final int STOPSTABLELENGTH_FIELD_NUMBER = 11;
      private int stopsTableLength_;
      public static final int INCOMPLETEROUTESOFFSET_FIELD_NUMBER = 12;
      private int incompleteRoutesOffset_;
      public static final int INCOMPLETEROUTESLENGTH_FIELD_NUMBER = 13;
      private int incompleteRoutesLength_;
      private byte memoizedIsInitialized = -1;
      private int memoizedSerializedSize = -1;
      private static final long serialVersionUID = 0L;

      private TransportPart(GeneratedMessageLite.Builder builder) {
         super(builder);
      }

      private TransportPart(boolean noInit) {
      }

      public static OsmandIndex.TransportPart getDefaultInstance() {
         return defaultInstance;
      }

      public OsmandIndex.TransportPart getDefaultInstanceForType() {
         return defaultInstance;
      }

      private TransportPart(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         this.initFields();
         int mutable_bitField0_ = 0;

         try {
            boolean done = false;

            while(!done) {
               int tag = input.readTag();
               switch(tag) {
                  case 0:
                     done = true;
                     break;
                  case 8:
                     this.bitField0_ |= 1;
                     this.size_ = input.readInt64();
                     break;
                  case 16:
                     this.bitField0_ |= 2;
                     this.offset_ = input.readInt64();
                     break;
                  case 26:
                     this.bitField0_ |= 4;
                     this.name_ = input.readBytes();
                     break;
                  case 32:
                     this.bitField0_ |= 8;
                     this.left_ = input.readInt32();
                     break;
                  case 40:
                     this.bitField0_ |= 16;
                     this.right_ = input.readInt32();
                     break;
                  case 48:
                     this.bitField0_ |= 32;
                     this.top_ = input.readInt32();
                     break;
                  case 56:
                     this.bitField0_ |= 64;
                     this.bottom_ = input.readInt32();
                     break;
                  case 64:
                     this.bitField0_ |= 128;
                     this.stringTableOffset_ = input.readUInt32();
                     break;
                  case 72:
                     this.bitField0_ |= 256;
                     this.stringTableLength_ = input.readUInt32();
                     break;
                  case 80:
                     this.bitField0_ |= 512;
                     this.stopsTableOffset_ = input.readUInt32();
                     break;
                  case 88:
                     this.bitField0_ |= 1024;
                     this.stopsTableLength_ = input.readUInt32();
                     break;
                  case 96:
                     this.bitField0_ |= 2048;
                     this.incompleteRoutesOffset_ = input.readUInt32();
                     break;
                  case 104:
                     this.bitField0_ |= 4096;
                     this.incompleteRoutesLength_ = input.readUInt32();
                     break;
                  default:
                     if (!this.parseUnknownField(input, extensionRegistry, tag)) {
                        done = true;
                     }
               }
            }
         } catch (InvalidProtocolBufferException var10) {
            throw var10.setUnfinishedMessage(this);
         } catch (IOException var11) {
            throw new InvalidProtocolBufferException(var11.getMessage()).setUnfinishedMessage(this);
         } finally {
            this.makeExtensionsImmutable();
         }
      }

      @Override
      public Parser<OsmandIndex.TransportPart> getParserForType() {
         return PARSER;
      }

      @Override
      public boolean hasSize() {
         return (this.bitField0_ & 1) == 1;
      }

      @Override
      public long getSize() {
         return this.size_;
      }

      @Override
      public boolean hasOffset() {
         return (this.bitField0_ & 2) == 2;
      }

      @Override
      public long getOffset() {
         return this.offset_;
      }

      @Override
      public boolean hasName() {
         return (this.bitField0_ & 4) == 4;
      }

      @Override
      public String getName() {
         Object ref = this.name_;
         if (ref instanceof String) {
            return (String)ref;
         } else {
            ByteString bs = (ByteString)ref;
            String s = bs.toStringUtf8();
            if (bs.isValidUtf8()) {
               this.name_ = s;
            }

            return s;
         }
      }

      @Override
      public ByteString getNameBytes() {
         Object ref = this.name_;
         if (ref instanceof String) {
            ByteString b = ByteString.copyFromUtf8((String)ref);
            this.name_ = b;
            return b;
         } else {
            return (ByteString)ref;
         }
      }

      @Override
      public boolean hasLeft() {
         return (this.bitField0_ & 8) == 8;
      }

      @Override
      public int getLeft() {
         return this.left_;
      }

      @Override
      public boolean hasRight() {
         return (this.bitField0_ & 16) == 16;
      }

      @Override
      public int getRight() {
         return this.right_;
      }

      @Override
      public boolean hasTop() {
         return (this.bitField0_ & 32) == 32;
      }

      @Override
      public int getTop() {
         return this.top_;
      }

      @Override
      public boolean hasBottom() {
         return (this.bitField0_ & 64) == 64;
      }

      @Override
      public int getBottom() {
         return this.bottom_;
      }

      @Override
      public boolean hasStringTableOffset() {
         return (this.bitField0_ & 128) == 128;
      }

      @Override
      public int getStringTableOffset() {
         return this.stringTableOffset_;
      }

      @Override
      public boolean hasStringTableLength() {
         return (this.bitField0_ & 256) == 256;
      }

      @Override
      public int getStringTableLength() {
         return this.stringTableLength_;
      }

      @Override
      public boolean hasStopsTableOffset() {
         return (this.bitField0_ & 512) == 512;
      }

      @Override
      public int getStopsTableOffset() {
         return this.stopsTableOffset_;
      }

      @Override
      public boolean hasStopsTableLength() {
         return (this.bitField0_ & 1024) == 1024;
      }

      @Override
      public int getStopsTableLength() {
         return this.stopsTableLength_;
      }

      @Override
      public boolean hasIncompleteRoutesOffset() {
         return (this.bitField0_ & 2048) == 2048;
      }

      @Override
      public int getIncompleteRoutesOffset() {
         return this.incompleteRoutesOffset_;
      }

      @Override
      public boolean hasIncompleteRoutesLength() {
         return (this.bitField0_ & 4096) == 4096;
      }

      @Override
      public int getIncompleteRoutesLength() {
         return this.incompleteRoutesLength_;
      }

      private void initFields() {
         this.size_ = 0L;
         this.offset_ = 0L;
         this.name_ = "";
         this.left_ = 0;
         this.right_ = 0;
         this.top_ = 0;
         this.bottom_ = 0;
         this.stringTableOffset_ = 0;
         this.stringTableLength_ = 0;
         this.stopsTableOffset_ = 0;
         this.stopsTableLength_ = 0;
         this.incompleteRoutesOffset_ = 0;
         this.incompleteRoutesLength_ = 0;
      }

      @Override
      public final boolean isInitialized() {
         byte isInitialized = this.memoizedIsInitialized;
         if (isInitialized != -1) {
            return isInitialized == 1;
         } else if (!this.hasSize()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else if (!this.hasOffset()) {
            this.memoizedIsInitialized = 0;
            return false;
         } else {
            this.memoizedIsInitialized = 1;
            return true;
         }
      }

      @Override
      public void writeTo(CodedOutputStream output) throws IOException {
         this.getSerializedSize();
         if ((this.bitField0_ & 1) == 1) {
            output.writeInt64(1, this.size_);
         }

         if ((this.bitField0_ & 2) == 2) {
            output.writeInt64(2, this.offset_);
         }

         if ((this.bitField0_ & 4) == 4) {
            output.writeBytes(3, this.getNameBytes());
         }

         if ((this.bitField0_ & 8) == 8) {
            output.writeInt32(4, this.left_);
         }

         if ((this.bitField0_ & 16) == 16) {
            output.writeInt32(5, this.right_);
         }

         if ((this.bitField0_ & 32) == 32) {
            output.writeInt32(6, this.top_);
         }

         if ((this.bitField0_ & 64) == 64) {
            output.writeInt32(7, this.bottom_);
         }

         if ((this.bitField0_ & 128) == 128) {
            output.writeUInt32(8, this.stringTableOffset_);
         }

         if ((this.bitField0_ & 256) == 256) {
            output.writeUInt32(9, this.stringTableLength_);
         }

         if ((this.bitField0_ & 512) == 512) {
            output.writeUInt32(10, this.stopsTableOffset_);
         }

         if ((this.bitField0_ & 1024) == 1024) {
            output.writeUInt32(11, this.stopsTableLength_);
         }

         if ((this.bitField0_ & 2048) == 2048) {
            output.writeUInt32(12, this.incompleteRoutesOffset_);
         }

         if ((this.bitField0_ & 4096) == 4096) {
            output.writeUInt32(13, this.incompleteRoutesLength_);
         }
      }

      @Override
      public int getSerializedSize() {
         int size = this.memoizedSerializedSize;
         if (size != -1) {
            return size;
         } else {
            size = 0;
            if ((this.bitField0_ & 1) == 1) {
               size += CodedOutputStream.computeInt64Size(1, this.size_);
            }

            if ((this.bitField0_ & 2) == 2) {
               size += CodedOutputStream.computeInt64Size(2, this.offset_);
            }

            if ((this.bitField0_ & 4) == 4) {
               size += CodedOutputStream.computeBytesSize(3, this.getNameBytes());
            }

            if ((this.bitField0_ & 8) == 8) {
               size += CodedOutputStream.computeInt32Size(4, this.left_);
            }

            if ((this.bitField0_ & 16) == 16) {
               size += CodedOutputStream.computeInt32Size(5, this.right_);
            }

            if ((this.bitField0_ & 32) == 32) {
               size += CodedOutputStream.computeInt32Size(6, this.top_);
            }

            if ((this.bitField0_ & 64) == 64) {
               size += CodedOutputStream.computeInt32Size(7, this.bottom_);
            }

            if ((this.bitField0_ & 128) == 128) {
               size += CodedOutputStream.computeUInt32Size(8, this.stringTableOffset_);
            }

            if ((this.bitField0_ & 256) == 256) {
               size += CodedOutputStream.computeUInt32Size(9, this.stringTableLength_);
            }

            if ((this.bitField0_ & 512) == 512) {
               size += CodedOutputStream.computeUInt32Size(10, this.stopsTableOffset_);
            }

            if ((this.bitField0_ & 1024) == 1024) {
               size += CodedOutputStream.computeUInt32Size(11, this.stopsTableLength_);
            }

            if ((this.bitField0_ & 2048) == 2048) {
               size += CodedOutputStream.computeUInt32Size(12, this.incompleteRoutesOffset_);
            }

            if ((this.bitField0_ & 4096) == 4096) {
               size += CodedOutputStream.computeUInt32Size(13, this.incompleteRoutesLength_);
            }

            this.memoizedSerializedSize = size;
            return size;
         }
      }

      @Override
      protected Object writeReplace() throws ObjectStreamException {
         return super.writeReplace();
      }

      public static OsmandIndex.TransportPart parseFrom(ByteString data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.TransportPart parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.TransportPart parseFrom(byte[] data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static OsmandIndex.TransportPart parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static OsmandIndex.TransportPart parseFrom(InputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.TransportPart parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.TransportPart parseDelimitedFrom(InputStream input) throws IOException {
         return PARSER.parseDelimitedFrom(input);
      }

      public static OsmandIndex.TransportPart parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseDelimitedFrom(input, extensionRegistry);
      }

      public static OsmandIndex.TransportPart parseFrom(CodedInputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static OsmandIndex.TransportPart parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static OsmandIndex.TransportPart.Builder newBuilder() {
         return OsmandIndex.TransportPart.Builder.create();
      }

      public OsmandIndex.TransportPart.Builder newBuilderForType() {
         return newBuilder();
      }

      public static OsmandIndex.TransportPart.Builder newBuilder(OsmandIndex.TransportPart prototype) {
         return newBuilder().mergeFrom(prototype);
      }

      public OsmandIndex.TransportPart.Builder toBuilder() {
         return newBuilder(this);
      }

      static {
         defaultInstance.initFields();
      }

      public static final class Builder
         extends GeneratedMessageLite.Builder<OsmandIndex.TransportPart, OsmandIndex.TransportPart.Builder>
         implements OsmandIndex.TransportPartOrBuilder {
         private int bitField0_;
         private long size_;
         private long offset_;
         private Object name_ = "";
         private int left_;
         private int right_;
         private int top_;
         private int bottom_;
         private int stringTableOffset_;
         private int stringTableLength_;
         private int stopsTableOffset_;
         private int stopsTableLength_;
         private int incompleteRoutesOffset_;
         private int incompleteRoutesLength_;

         private Builder() {
            this.maybeForceBuilderInitialization();
         }

         private void maybeForceBuilderInitialization() {
         }

         private static OsmandIndex.TransportPart.Builder create() {
            return new OsmandIndex.TransportPart.Builder();
         }

         public OsmandIndex.TransportPart.Builder clear() {
            super.clear();
            this.size_ = 0L;
            this.bitField0_ &= -2;
            this.offset_ = 0L;
            this.bitField0_ &= -3;
            this.name_ = "";
            this.bitField0_ &= -5;
            this.left_ = 0;
            this.bitField0_ &= -9;
            this.right_ = 0;
            this.bitField0_ &= -17;
            this.top_ = 0;
            this.bitField0_ &= -33;
            this.bottom_ = 0;
            this.bitField0_ &= -65;
            this.stringTableOffset_ = 0;
            this.bitField0_ &= -129;
            this.stringTableLength_ = 0;
            this.bitField0_ &= -257;
            this.stopsTableOffset_ = 0;
            this.bitField0_ &= -513;
            this.stopsTableLength_ = 0;
            this.bitField0_ &= -1025;
            this.incompleteRoutesOffset_ = 0;
            this.bitField0_ &= -2049;
            this.incompleteRoutesLength_ = 0;
            this.bitField0_ &= -4097;
            return this;
         }

         public OsmandIndex.TransportPart.Builder clone() {
            return create().mergeFrom(this.buildPartial());
         }

         public OsmandIndex.TransportPart getDefaultInstanceForType() {
            return OsmandIndex.TransportPart.getDefaultInstance();
         }

         public OsmandIndex.TransportPart build() {
            OsmandIndex.TransportPart result = this.buildPartial();
            if (!result.isInitialized()) {
               throw newUninitializedMessageException(result);
            } else {
               return result;
            }
         }

         public OsmandIndex.TransportPart buildPartial() {
            OsmandIndex.TransportPart result = new OsmandIndex.TransportPart(this);
            int from_bitField0_ = this.bitField0_;
            int to_bitField0_ = 0;
            if ((from_bitField0_ & 1) == 1) {
               to_bitField0_ |= 1;
            }

            result.size_ = this.size_;
            if ((from_bitField0_ & 2) == 2) {
               to_bitField0_ |= 2;
            }

            result.offset_ = this.offset_;
            if ((from_bitField0_ & 4) == 4) {
               to_bitField0_ |= 4;
            }

            result.name_ = this.name_;
            if ((from_bitField0_ & 8) == 8) {
               to_bitField0_ |= 8;
            }

            result.left_ = this.left_;
            if ((from_bitField0_ & 16) == 16) {
               to_bitField0_ |= 16;
            }

            result.right_ = this.right_;
            if ((from_bitField0_ & 32) == 32) {
               to_bitField0_ |= 32;
            }

            result.top_ = this.top_;
            if ((from_bitField0_ & 64) == 64) {
               to_bitField0_ |= 64;
            }

            result.bottom_ = this.bottom_;
            if ((from_bitField0_ & 128) == 128) {
               to_bitField0_ |= 128;
            }

            result.stringTableOffset_ = this.stringTableOffset_;
            if ((from_bitField0_ & 256) == 256) {
               to_bitField0_ |= 256;
            }

            result.stringTableLength_ = this.stringTableLength_;
            if ((from_bitField0_ & 512) == 512) {
               to_bitField0_ |= 512;
            }

            result.stopsTableOffset_ = this.stopsTableOffset_;
            if ((from_bitField0_ & 1024) == 1024) {
               to_bitField0_ |= 1024;
            }

            result.stopsTableLength_ = this.stopsTableLength_;
            if ((from_bitField0_ & 2048) == 2048) {
               to_bitField0_ |= 2048;
            }

            result.incompleteRoutesOffset_ = this.incompleteRoutesOffset_;
            if ((from_bitField0_ & 4096) == 4096) {
               to_bitField0_ |= 4096;
            }

            result.incompleteRoutesLength_ = this.incompleteRoutesLength_;
            result.bitField0_ = to_bitField0_;
            return result;
         }

         public OsmandIndex.TransportPart.Builder mergeFrom(OsmandIndex.TransportPart other) {
            if (other == OsmandIndex.TransportPart.getDefaultInstance()) {
               return this;
            } else {
               if (other.hasSize()) {
                  this.setSize(other.getSize());
               }

               if (other.hasOffset()) {
                  this.setOffset(other.getOffset());
               }

               if (other.hasName()) {
                  this.bitField0_ |= 4;
                  this.name_ = other.name_;
               }

               if (other.hasLeft()) {
                  this.setLeft(other.getLeft());
               }

               if (other.hasRight()) {
                  this.setRight(other.getRight());
               }

               if (other.hasTop()) {
                  this.setTop(other.getTop());
               }

               if (other.hasBottom()) {
                  this.setBottom(other.getBottom());
               }

               if (other.hasStringTableOffset()) {
                  this.setStringTableOffset(other.getStringTableOffset());
               }

               if (other.hasStringTableLength()) {
                  this.setStringTableLength(other.getStringTableLength());
               }

               if (other.hasStopsTableOffset()) {
                  this.setStopsTableOffset(other.getStopsTableOffset());
               }

               if (other.hasStopsTableLength()) {
                  this.setStopsTableLength(other.getStopsTableLength());
               }

               if (other.hasIncompleteRoutesOffset()) {
                  this.setIncompleteRoutesOffset(other.getIncompleteRoutesOffset());
               }

               if (other.hasIncompleteRoutesLength()) {
                  this.setIncompleteRoutesLength(other.getIncompleteRoutesLength());
               }

               return this;
            }
         }

         @Override
         public final boolean isInitialized() {
            if (!this.hasSize()) {
               return false;
            } else {
               return this.hasOffset();
            }
         }

         public OsmandIndex.TransportPart.Builder mergeFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            OsmandIndex.TransportPart parsedMessage = null;

            try {
               parsedMessage = OsmandIndex.TransportPart.PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (InvalidProtocolBufferException var8) {
               parsedMessage = (OsmandIndex.TransportPart)var8.getUnfinishedMessage();
               throw var8;
            } finally {
               if (parsedMessage != null) {
                  this.mergeFrom(parsedMessage);
               }
            }

            return this;
         }

         @Override
         public boolean hasSize() {
            return (this.bitField0_ & 1) == 1;
         }

         @Override
         public long getSize() {
            return this.size_;
         }

         public OsmandIndex.TransportPart.Builder setSize(long value) {
            this.bitField0_ |= 1;
            this.size_ = value;
            return this;
         }

         public OsmandIndex.TransportPart.Builder clearSize() {
            this.bitField0_ &= -2;
            this.size_ = 0L;
            return this;
         }

         @Override
         public boolean hasOffset() {
            return (this.bitField0_ & 2) == 2;
         }

         @Override
         public long getOffset() {
            return this.offset_;
         }

         public OsmandIndex.TransportPart.Builder setOffset(long value) {
            this.bitField0_ |= 2;
            this.offset_ = value;
            return this;
         }

         public OsmandIndex.TransportPart.Builder clearOffset() {
            this.bitField0_ &= -3;
            this.offset_ = 0L;
            return this;
         }

         @Override
         public boolean hasName() {
            return (this.bitField0_ & 4) == 4;
         }

         @Override
         public String getName() {
            Object ref = this.name_;
            if (!(ref instanceof String)) {
               String s = ((ByteString)ref).toStringUtf8();
               this.name_ = s;
               return s;
            } else {
               return (String)ref;
            }
         }

         @Override
         public ByteString getNameBytes() {
            Object ref = this.name_;
            if (ref instanceof String) {
               ByteString b = ByteString.copyFromUtf8((String)ref);
               this.name_ = b;
               return b;
            } else {
               return (ByteString)ref;
            }
         }

         public OsmandIndex.TransportPart.Builder setName(String value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.bitField0_ |= 4;
               this.name_ = value;
               return this;
            }
         }

         public OsmandIndex.TransportPart.Builder clearName() {
            this.bitField0_ &= -5;
            this.name_ = OsmandIndex.TransportPart.getDefaultInstance().getName();
            return this;
         }

         public OsmandIndex.TransportPart.Builder setNameBytes(ByteString value) {
            if (value == null) {
               throw new NullPointerException();
            } else {
               this.bitField0_ |= 4;
               this.name_ = value;
               return this;
            }
         }

         @Override
         public boolean hasLeft() {
            return (this.bitField0_ & 8) == 8;
         }

         @Override
         public int getLeft() {
            return this.left_;
         }

         public OsmandIndex.TransportPart.Builder setLeft(int value) {
            this.bitField0_ |= 8;
            this.left_ = value;
            return this;
         }

         public OsmandIndex.TransportPart.Builder clearLeft() {
            this.bitField0_ &= -9;
            this.left_ = 0;
            return this;
         }

         @Override
         public boolean hasRight() {
            return (this.bitField0_ & 16) == 16;
         }

         @Override
         public int getRight() {
            return this.right_;
         }

         public OsmandIndex.TransportPart.Builder setRight(int value) {
            this.bitField0_ |= 16;
            this.right_ = value;
            return this;
         }

         public OsmandIndex.TransportPart.Builder clearRight() {
            this.bitField0_ &= -17;
            this.right_ = 0;
            return this;
         }

         @Override
         public boolean hasTop() {
            return (this.bitField0_ & 32) == 32;
         }

         @Override
         public int getTop() {
            return this.top_;
         }

         public OsmandIndex.TransportPart.Builder setTop(int value) {
            this.bitField0_ |= 32;
            this.top_ = value;
            return this;
         }

         public OsmandIndex.TransportPart.Builder clearTop() {
            this.bitField0_ &= -33;
            this.top_ = 0;
            return this;
         }

         @Override
         public boolean hasBottom() {
            return (this.bitField0_ & 64) == 64;
         }

         @Override
         public int getBottom() {
            return this.bottom_;
         }

         public OsmandIndex.TransportPart.Builder setBottom(int value) {
            this.bitField0_ |= 64;
            this.bottom_ = value;
            return this;
         }

         public OsmandIndex.TransportPart.Builder clearBottom() {
            this.bitField0_ &= -65;
            this.bottom_ = 0;
            return this;
         }

         @Override
         public boolean hasStringTableOffset() {
            return (this.bitField0_ & 128) == 128;
         }

         @Override
         public int getStringTableOffset() {
            return this.stringTableOffset_;
         }

         public OsmandIndex.TransportPart.Builder setStringTableOffset(int value) {
            this.bitField0_ |= 128;
            this.stringTableOffset_ = value;
            return this;
         }

         public OsmandIndex.TransportPart.Builder clearStringTableOffset() {
            this.bitField0_ &= -129;
            this.stringTableOffset_ = 0;
            return this;
         }

         @Override
         public boolean hasStringTableLength() {
            return (this.bitField0_ & 256) == 256;
         }

         @Override
         public int getStringTableLength() {
            return this.stringTableLength_;
         }

         public OsmandIndex.TransportPart.Builder setStringTableLength(int value) {
            this.bitField0_ |= 256;
            this.stringTableLength_ = value;
            return this;
         }

         public OsmandIndex.TransportPart.Builder clearStringTableLength() {
            this.bitField0_ &= -257;
            this.stringTableLength_ = 0;
            return this;
         }

         @Override
         public boolean hasStopsTableOffset() {
            return (this.bitField0_ & 512) == 512;
         }

         @Override
         public int getStopsTableOffset() {
            return this.stopsTableOffset_;
         }

         public OsmandIndex.TransportPart.Builder setStopsTableOffset(int value) {
            this.bitField0_ |= 512;
            this.stopsTableOffset_ = value;
            return this;
         }

         public OsmandIndex.TransportPart.Builder clearStopsTableOffset() {
            this.bitField0_ &= -513;
            this.stopsTableOffset_ = 0;
            return this;
         }

         @Override
         public boolean hasStopsTableLength() {
            return (this.bitField0_ & 1024) == 1024;
         }

         @Override
         public int getStopsTableLength() {
            return this.stopsTableLength_;
         }

         public OsmandIndex.TransportPart.Builder setStopsTableLength(int value) {
            this.bitField0_ |= 1024;
            this.stopsTableLength_ = value;
            return this;
         }

         public OsmandIndex.TransportPart.Builder clearStopsTableLength() {
            this.bitField0_ &= -1025;
            this.stopsTableLength_ = 0;
            return this;
         }

         @Override
         public boolean hasIncompleteRoutesOffset() {
            return (this.bitField0_ & 2048) == 2048;
         }

         @Override
         public int getIncompleteRoutesOffset() {
            return this.incompleteRoutesOffset_;
         }

         public OsmandIndex.TransportPart.Builder setIncompleteRoutesOffset(int value) {
            this.bitField0_ |= 2048;
            this.incompleteRoutesOffset_ = value;
            return this;
         }

         public OsmandIndex.TransportPart.Builder clearIncompleteRoutesOffset() {
            this.bitField0_ &= -2049;
            this.incompleteRoutesOffset_ = 0;
            return this;
         }

         @Override
         public boolean hasIncompleteRoutesLength() {
            return (this.bitField0_ & 4096) == 4096;
         }

         @Override
         public int getIncompleteRoutesLength() {
            return this.incompleteRoutesLength_;
         }

         public OsmandIndex.TransportPart.Builder setIncompleteRoutesLength(int value) {
            this.bitField0_ |= 4096;
            this.incompleteRoutesLength_ = value;
            return this;
         }

         public OsmandIndex.TransportPart.Builder clearIncompleteRoutesLength() {
            this.bitField0_ &= -4097;
            this.incompleteRoutesLength_ = 0;
            return this;
         }
      }
   }

   public interface TransportPartOrBuilder extends MessageLiteOrBuilder {
      boolean hasSize();

      long getSize();

      boolean hasOffset();

      long getOffset();

      boolean hasName();

      String getName();

      ByteString getNameBytes();

      boolean hasLeft();

      int getLeft();

      boolean hasRight();

      int getRight();

      boolean hasTop();

      int getTop();

      boolean hasBottom();

      int getBottom();

      boolean hasStringTableOffset();

      int getStringTableOffset();

      boolean hasStringTableLength();

      int getStringTableLength();

      boolean hasStopsTableOffset();

      int getStopsTableOffset();

      boolean hasStopsTableLength();

      int getStopsTableLength();

      boolean hasIncompleteRoutesOffset();

      int getIncompleteRoutesOffset();

      boolean hasIncompleteRoutesLength();

      int getIncompleteRoutesLength();
   }
}
