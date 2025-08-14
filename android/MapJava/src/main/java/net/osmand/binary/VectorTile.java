package net.osmand.binary;

import com.google.protobuf.AbstractParser;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Internal;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.LazyStringArrayList;
import com.google.protobuf.LazyStringList;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Parser;
import com.google.protobuf.ProtocolMessageEnum;
import com.google.protobuf.RepeatedFieldBuilder;
import com.google.protobuf.UnknownFieldSet;
import com.google.protobuf.UnmodifiableLazyStringList;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VectorTile {
   private static Descriptors.Descriptor internal_static_OsmAnd_VectorTile_Tile_descriptor;
   private static GeneratedMessage.FieldAccessorTable internal_static_OsmAnd_VectorTile_Tile_fieldAccessorTable;
   private static Descriptors.Descriptor internal_static_OsmAnd_VectorTile_Tile_Value_descriptor;
   private static GeneratedMessage.FieldAccessorTable internal_static_OsmAnd_VectorTile_Tile_Value_fieldAccessorTable;
   private static Descriptors.Descriptor internal_static_OsmAnd_VectorTile_Tile_Feature_descriptor;
   private static GeneratedMessage.FieldAccessorTable internal_static_OsmAnd_VectorTile_Tile_Feature_fieldAccessorTable;
   private static Descriptors.Descriptor internal_static_OsmAnd_VectorTile_Tile_Layer_descriptor;
   private static GeneratedMessage.FieldAccessorTable internal_static_OsmAnd_VectorTile_Tile_Layer_fieldAccessorTable;
   private static Descriptors.FileDescriptor descriptor;

   private VectorTile() {
   }

   public static void registerAllExtensions(ExtensionRegistry registry) {
   }

   public static Descriptors.FileDescriptor getDescriptor() {
      return descriptor;
   }

   static {
      String[] descriptorData = new String[]{
         "\n\u0011vector_tile.proto\u0012\u0011OsmAnd.VectorTile\"Ø\u0004\n\u0004Tile\u0012-\n\u0006layers\u0018\u0003 \u0003(\u000b2\u001d.OsmAnd.VectorTile.Tile.Layer\u001a¡\u0001\n\u0005Value\u0012\u0014\n\fstring_value\u0018\u0001 \u0001(\t\u0012\u0013\n\u000bfloat_value\u0018\u0002 \u0001(\u0002\u0012\u0014\n\fdouble_value\u0018\u0003 \u0001(\u0001\u0012\u0011\n\tint_value\u0018\u0004 \u0001(\u0003\u0012\u0012\n\nuint_value\u0018\u0005 \u0001(\u0004\u0012\u0012\n\nsint_value\u0018\u0006 \u0001(\u0012\u0012\u0012\n\nbool_value\u0018\u0007 \u0001(\b*\b\b\b\u0010\u0080\u0080\u0080\u0080\u0002\u001ay\n\u0007Feature\u0012\r\n\u0002id\u0018\u0001 \u0001(\u0004:\u00010\u0012\u0010\n\u0004tags\u0018\u0002 \u0003(\rB\u0002\u0010\u0001\u00127\n\u0004type\u0018\u0003 \u0001(\u000e2 .OsmAnd.VectorTile.Tile.GeomType:\u0007UNKNOWN\u0012\u0014\n\bgeometry\u0018\u0004 \u0003(\rB\u0002\u0010\u0001\u001a¹\u0001\n\u0005Layer\u0012\u0012\n\u0007versi",
         "on\u0018\u000f \u0002(\r:\u00011\u0012\f\n\u0004name\u0018\u0001 \u0002(\t\u00121\n\bfeatures\u0018\u0002 \u0003(\u000b2\u001f.OsmAnd.VectorTile.Tile.Feature\u0012\f\n\u0004keys\u0018\u0003 \u0003(\t\u0012-\n\u0006values\u0018\u0004 \u0003(\u000b2\u001d.OsmAnd.VectorTile.Tile.Value\u0012\u0014\n\u0006extent\u0018\u0005 \u0001(\r:\u00044096*\b\b\u0010\u0010\u0080\u0080\u0080\u0080\u0002\"?\n\bGeomType\u0012\u000b\n\u0007UNKNOWN\u0010\u0000\u0012\t\n\u0005POINT\u0010\u0001\u0012\u000e\n\nLINESTRING\u0010\u0002\u0012\u000b\n\u0007POLYGON\u0010\u0003*\u0005\b\u0010\u0010\u0080@B\u001f\n\u0011net.osmand.binaryB\nVectorTile"
      };
      Descriptors.FileDescriptor.InternalDescriptorAssigner assigner = new Descriptors.FileDescriptor.InternalDescriptorAssigner() {
         @Override
         public ExtensionRegistry assignDescriptors(Descriptors.FileDescriptor root) {
            VectorTile.descriptor = root;
            VectorTile.internal_static_OsmAnd_VectorTile_Tile_descriptor = VectorTile.getDescriptor().getMessageTypes().get(0);
            VectorTile.internal_static_OsmAnd_VectorTile_Tile_fieldAccessorTable = new GeneratedMessage.FieldAccessorTable(
               VectorTile.internal_static_OsmAnd_VectorTile_Tile_descriptor, new String[]{"Layers"}
            );
            VectorTile.internal_static_OsmAnd_VectorTile_Tile_Value_descriptor = VectorTile.internal_static_OsmAnd_VectorTile_Tile_descriptor
               .getNestedTypes()
               .get(0);
            VectorTile.internal_static_OsmAnd_VectorTile_Tile_Value_fieldAccessorTable = new GeneratedMessage.FieldAccessorTable(
               VectorTile.internal_static_OsmAnd_VectorTile_Tile_Value_descriptor,
               new String[]{"StringValue", "FloatValue", "DoubleValue", "IntValue", "UintValue", "SintValue", "BoolValue"}
            );
            VectorTile.internal_static_OsmAnd_VectorTile_Tile_Feature_descriptor = VectorTile.internal_static_OsmAnd_VectorTile_Tile_descriptor
               .getNestedTypes()
               .get(1);
            VectorTile.internal_static_OsmAnd_VectorTile_Tile_Feature_fieldAccessorTable = new GeneratedMessage.FieldAccessorTable(
               VectorTile.internal_static_OsmAnd_VectorTile_Tile_Feature_descriptor, new String[]{"Id", "Tags", "Type", "Geometry"}
            );
            VectorTile.internal_static_OsmAnd_VectorTile_Tile_Layer_descriptor = VectorTile.internal_static_OsmAnd_VectorTile_Tile_descriptor
               .getNestedTypes()
               .get(2);
            VectorTile.internal_static_OsmAnd_VectorTile_Tile_Layer_fieldAccessorTable = new GeneratedMessage.FieldAccessorTable(
               VectorTile.internal_static_OsmAnd_VectorTile_Tile_Layer_descriptor, new String[]{"Version", "Name", "Features", "Keys", "Values", "Extent"}
            );
            return null;
         }
      };
      Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(descriptorData, new Descriptors.FileDescriptor[0], assigner);
   }

   public static final class Tile extends GeneratedMessage.ExtendableMessage<VectorTile.Tile> implements VectorTile.TileOrBuilder {
      private static final VectorTile.Tile defaultInstance = new VectorTile.Tile(true);
      private final UnknownFieldSet unknownFields;
      public static Parser<VectorTile.Tile> PARSER = new AbstractParser<VectorTile.Tile>() {
         public VectorTile.Tile parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return new VectorTile.Tile(input, extensionRegistry);
         }
      };
      public static final int LAYERS_FIELD_NUMBER = 3;
      private List<VectorTile.Tile.Layer> layers_;
      private byte memoizedIsInitialized = -1;
      private int memoizedSerializedSize = -1;
      private static final long serialVersionUID = 0L;

      private Tile(GeneratedMessage.ExtendableBuilder<VectorTile.Tile, ?> builder) {
         super(builder);
         this.unknownFields = builder.getUnknownFields();
      }

      private Tile(boolean noInit) {
         this.unknownFields = UnknownFieldSet.getDefaultInstance();
      }

      public static VectorTile.Tile getDefaultInstance() {
         return defaultInstance;
      }

      public VectorTile.Tile getDefaultInstanceForType() {
         return defaultInstance;
      }

      @Override
      public final UnknownFieldSet getUnknownFields() {
         return this.unknownFields;
      }

      private Tile(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         this.initFields();
         int mutable_bitField0_ = 0;
         UnknownFieldSet.Builder unknownFields = UnknownFieldSet.newBuilder();

         try {
            boolean done = false;

            while(!done) {
               int tag = input.readTag();
               switch(tag) {
                  case 0:
                     done = true;
                     break;
                  case 26:
                     if ((mutable_bitField0_ & 1) != 1) {
                        this.layers_ = new ArrayList<>();
                        mutable_bitField0_ |= 1;
                     }

                     this.layers_.add(input.readMessage(VectorTile.Tile.Layer.PARSER, extensionRegistry));
                     break;
                  default:
                     if (!this.parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                        done = true;
                     }
               }
            }
         } catch (InvalidProtocolBufferException var11) {
            throw var11.setUnfinishedMessage(this);
         } catch (IOException var12) {
            throw new InvalidProtocolBufferException(var12.getMessage()).setUnfinishedMessage(this);
         } finally {
            if ((mutable_bitField0_ & 1) == 1) {
               this.layers_ = Collections.unmodifiableList(this.layers_);
            }

            this.unknownFields = unknownFields.build();
            this.makeExtensionsImmutable();
         }
      }

      public static final Descriptors.Descriptor getDescriptor() {
         return VectorTile.internal_static_OsmAnd_VectorTile_Tile_descriptor;
      }

      @Override
      protected GeneratedMessage.FieldAccessorTable internalGetFieldAccessorTable() {
         return VectorTile.internal_static_OsmAnd_VectorTile_Tile_fieldAccessorTable
            .ensureFieldAccessorsInitialized(VectorTile.Tile.class, VectorTile.Tile.Builder.class);
      }

      @Override
      public Parser<VectorTile.Tile> getParserForType() {
         return PARSER;
      }

      @Override
      public List<VectorTile.Tile.Layer> getLayersList() {
         return this.layers_;
      }

      @Override
      public List<? extends VectorTile.Tile.LayerOrBuilder> getLayersOrBuilderList() {
         return this.layers_;
      }

      @Override
      public int getLayersCount() {
         return this.layers_.size();
      }

      @Override
      public VectorTile.Tile.Layer getLayers(int index) {
         return this.layers_.get(index);
      }

      @Override
      public VectorTile.Tile.LayerOrBuilder getLayersOrBuilder(int index) {
         return this.layers_.get(index);
      }

      private void initFields() {
         this.layers_ = Collections.emptyList();
      }

      @Override
      public final boolean isInitialized() {
         byte isInitialized = this.memoizedIsInitialized;
         if (isInitialized != -1) {
            return isInitialized == 1;
         } else {
            for(int i = 0; i < this.getLayersCount(); ++i) {
               if (!this.getLayers(i).isInitialized()) {
                  this.memoizedIsInitialized = 0;
                  return false;
               }
            }

            if (!this.extensionsAreInitialized()) {
               this.memoizedIsInitialized = 0;
               return false;
            } else {
               this.memoizedIsInitialized = 1;
               return true;
            }
         }
      }

      @Override
      public void writeTo(CodedOutputStream output) throws IOException {
         this.getSerializedSize();
         GeneratedMessage.ExtendableMessage<VectorTile.Tile>.ExtensionWriter extensionWriter = this.newExtensionWriter();

         for(int i = 0; i < this.layers_.size(); ++i) {
            output.writeMessage(3, this.layers_.get(i));
         }

         extensionWriter.writeUntil(8192, output);
         this.getUnknownFields().writeTo(output);
      }

      @Override
      public int getSerializedSize() {
         int size = this.memoizedSerializedSize;
         if (size != -1) {
            return size;
         } else {
            size = 0;

            for(int i = 0; i < this.layers_.size(); ++i) {
               size += CodedOutputStream.computeMessageSize(3, this.layers_.get(i));
            }

            size += this.extensionsSerializedSize();
            size += this.getUnknownFields().getSerializedSize();
            this.memoizedSerializedSize = size;
            return size;
         }
      }

      @Override
      protected Object writeReplace() throws ObjectStreamException {
         return super.writeReplace();
      }

      public static VectorTile.Tile parseFrom(ByteString data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static VectorTile.Tile parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static VectorTile.Tile parseFrom(byte[] data) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data);
      }

      public static VectorTile.Tile parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
         return PARSER.parseFrom(data, extensionRegistry);
      }

      public static VectorTile.Tile parseFrom(InputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static VectorTile.Tile parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static VectorTile.Tile parseDelimitedFrom(InputStream input) throws IOException {
         return PARSER.parseDelimitedFrom(input);
      }

      public static VectorTile.Tile parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseDelimitedFrom(input, extensionRegistry);
      }

      public static VectorTile.Tile parseFrom(CodedInputStream input) throws IOException {
         return PARSER.parseFrom(input);
      }

      public static VectorTile.Tile parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
         return PARSER.parseFrom(input, extensionRegistry);
      }

      public static VectorTile.Tile.Builder newBuilder() {
         return VectorTile.Tile.Builder.create();
      }

      public VectorTile.Tile.Builder newBuilderForType() {
         return newBuilder();
      }

      public static VectorTile.Tile.Builder newBuilder(VectorTile.Tile prototype) {
         return newBuilder().mergeFrom(prototype);
      }

      public VectorTile.Tile.Builder toBuilder() {
         return newBuilder(this);
      }

      protected VectorTile.Tile.Builder newBuilderForType(GeneratedMessage.BuilderParent parent) {
         return new VectorTile.Tile.Builder(parent);
      }

      static {
         defaultInstance.initFields();
      }

      public static final class Builder
         extends GeneratedMessage.ExtendableBuilder<VectorTile.Tile, VectorTile.Tile.Builder>
         implements VectorTile.TileOrBuilder {
         private int bitField0_;
         private List<VectorTile.Tile.Layer> layers_ = Collections.emptyList();
         private RepeatedFieldBuilder<VectorTile.Tile.Layer, VectorTile.Tile.Layer.Builder, VectorTile.Tile.LayerOrBuilder> layersBuilder_;

         public static final Descriptors.Descriptor getDescriptor() {
            return VectorTile.internal_static_OsmAnd_VectorTile_Tile_descriptor;
         }

         @Override
         protected GeneratedMessage.FieldAccessorTable internalGetFieldAccessorTable() {
            return VectorTile.internal_static_OsmAnd_VectorTile_Tile_fieldAccessorTable
               .ensureFieldAccessorsInitialized(VectorTile.Tile.class, VectorTile.Tile.Builder.class);
         }

         private Builder() {
            this.maybeForceBuilderInitialization();
         }

         private Builder(GeneratedMessage.BuilderParent parent) {
            super(parent);
            this.maybeForceBuilderInitialization();
         }

         private void maybeForceBuilderInitialization() {
            if (VectorTile.Tile.alwaysUseFieldBuilders) {
               this.getLayersFieldBuilder();
            }
         }

         private static VectorTile.Tile.Builder create() {
            return new VectorTile.Tile.Builder();
         }

         public VectorTile.Tile.Builder clear() {
            super.clear();
            if (this.layersBuilder_ == null) {
               this.layers_ = Collections.emptyList();
               this.bitField0_ &= -2;
            } else {
               this.layersBuilder_.clear();
            }

            return this;
         }

         public VectorTile.Tile.Builder clone() {
            return create().mergeFrom(this.buildPartial());
         }

         @Override
         public Descriptors.Descriptor getDescriptorForType() {
            return VectorTile.internal_static_OsmAnd_VectorTile_Tile_descriptor;
         }

         public VectorTile.Tile getDefaultInstanceForType() {
            return VectorTile.Tile.getDefaultInstance();
         }

         public VectorTile.Tile build() {
            VectorTile.Tile result = this.buildPartial();
            if (!result.isInitialized()) {
               throw newUninitializedMessageException(result);
            } else {
               return result;
            }
         }

         public VectorTile.Tile buildPartial() {
            VectorTile.Tile result = new VectorTile.Tile(this);
            int from_bitField0_ = this.bitField0_;
            if (this.layersBuilder_ == null) {
               if ((this.bitField0_ & 1) == 1) {
                  this.layers_ = Collections.unmodifiableList(this.layers_);
                  this.bitField0_ &= -2;
               }

               result.layers_ = this.layers_;
            } else {
               result.layers_ = this.layersBuilder_.build();
            }

            this.onBuilt();
            return result;
         }

         public VectorTile.Tile.Builder mergeFrom(Message other) {
            if (other instanceof VectorTile.Tile) {
               return this.mergeFrom((VectorTile.Tile)other);
            } else {
               super.mergeFrom(other);
               return this;
            }
         }

         public VectorTile.Tile.Builder mergeFrom(VectorTile.Tile other) {
            if (other == VectorTile.Tile.getDefaultInstance()) {
               return this;
            } else {
               if (this.layersBuilder_ == null) {
                  if (!other.layers_.isEmpty()) {
                     if (this.layers_.isEmpty()) {
                        this.layers_ = other.layers_;
                        this.bitField0_ &= -2;
                     } else {
                        this.ensureLayersIsMutable();
                        this.layers_.addAll(other.layers_);
                     }

                     this.onChanged();
                  }
               } else if (!other.layers_.isEmpty()) {
                  if (this.layersBuilder_.isEmpty()) {
                     this.layersBuilder_.dispose();
                     this.layersBuilder_ = null;
                     this.layers_ = other.layers_;
                     this.bitField0_ &= -2;
                     this.layersBuilder_ = VectorTile.Tile.alwaysUseFieldBuilders ? this.getLayersFieldBuilder() : null;
                  } else {
                     this.layersBuilder_.addAllMessages(other.layers_);
                  }
               }

               this.mergeExtensionFields(other);
               this.mergeUnknownFields(other.getUnknownFields());
               return this;
            }
         }

         @Override
         public final boolean isInitialized() {
            for(int i = 0; i < this.getLayersCount(); ++i) {
               if (!this.getLayers(i).isInitialized()) {
                  return false;
               }
            }

            return this.extensionsAreInitialized();
         }

         public VectorTile.Tile.Builder mergeFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            VectorTile.Tile parsedMessage = null;

            try {
               parsedMessage = VectorTile.Tile.PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (InvalidProtocolBufferException var8) {
               parsedMessage = (VectorTile.Tile)var8.getUnfinishedMessage();
               throw var8;
            } finally {
               if (parsedMessage != null) {
                  this.mergeFrom(parsedMessage);
               }
            }

            return this;
         }

         private void ensureLayersIsMutable() {
            if ((this.bitField0_ & 1) != 1) {
               this.layers_ = new ArrayList<>(this.layers_);
               this.bitField0_ |= 1;
            }
         }

         @Override
         public List<VectorTile.Tile.Layer> getLayersList() {
            return this.layersBuilder_ == null ? Collections.unmodifiableList(this.layers_) : this.layersBuilder_.getMessageList();
         }

         @Override
         public int getLayersCount() {
            return this.layersBuilder_ == null ? this.layers_.size() : this.layersBuilder_.getCount();
         }

         @Override
         public VectorTile.Tile.Layer getLayers(int index) {
            return this.layersBuilder_ == null ? this.layers_.get(index) : this.layersBuilder_.getMessage(index);
         }

         public VectorTile.Tile.Builder setLayers(int index, VectorTile.Tile.Layer value) {
            if (this.layersBuilder_ == null) {
               if (value == null) {
                  throw new NullPointerException();
               }

               this.ensureLayersIsMutable();
               this.layers_.set(index, value);
               this.onChanged();
            } else {
               this.layersBuilder_.setMessage(index, value);
            }

            return this;
         }

         public VectorTile.Tile.Builder setLayers(int index, VectorTile.Tile.Layer.Builder builderForValue) {
            if (this.layersBuilder_ == null) {
               this.ensureLayersIsMutable();
               this.layers_.set(index, builderForValue.build());
               this.onChanged();
            } else {
               this.layersBuilder_.setMessage(index, builderForValue.build());
            }

            return this;
         }

         public VectorTile.Tile.Builder addLayers(VectorTile.Tile.Layer value) {
            if (this.layersBuilder_ == null) {
               if (value == null) {
                  throw new NullPointerException();
               }

               this.ensureLayersIsMutable();
               this.layers_.add(value);
               this.onChanged();
            } else {
               this.layersBuilder_.addMessage(value);
            }

            return this;
         }

         public VectorTile.Tile.Builder addLayers(int index, VectorTile.Tile.Layer value) {
            if (this.layersBuilder_ == null) {
               if (value == null) {
                  throw new NullPointerException();
               }

               this.ensureLayersIsMutable();
               this.layers_.add(index, value);
               this.onChanged();
            } else {
               this.layersBuilder_.addMessage(index, value);
            }

            return this;
         }

         public VectorTile.Tile.Builder addLayers(VectorTile.Tile.Layer.Builder builderForValue) {
            if (this.layersBuilder_ == null) {
               this.ensureLayersIsMutable();
               this.layers_.add(builderForValue.build());
               this.onChanged();
            } else {
               this.layersBuilder_.addMessage(builderForValue.build());
            }

            return this;
         }

         public VectorTile.Tile.Builder addLayers(int index, VectorTile.Tile.Layer.Builder builderForValue) {
            if (this.layersBuilder_ == null) {
               this.ensureLayersIsMutable();
               this.layers_.add(index, builderForValue.build());
               this.onChanged();
            } else {
               this.layersBuilder_.addMessage(index, builderForValue.build());
            }

            return this;
         }

         public VectorTile.Tile.Builder addAllLayers(Iterable<? extends VectorTile.Tile.Layer> values) {
            if (this.layersBuilder_ == null) {
               this.ensureLayersIsMutable();
               GeneratedMessage.ExtendableBuilder.addAll(values, this.layers_);
               this.onChanged();
            } else {
               this.layersBuilder_.addAllMessages(values);
            }

            return this;
         }

         public VectorTile.Tile.Builder clearLayers() {
            if (this.layersBuilder_ == null) {
               this.layers_ = Collections.emptyList();
               this.bitField0_ &= -2;
               this.onChanged();
            } else {
               this.layersBuilder_.clear();
            }

            return this;
         }

         public VectorTile.Tile.Builder removeLayers(int index) {
            if (this.layersBuilder_ == null) {
               this.ensureLayersIsMutable();
               this.layers_.remove(index);
               this.onChanged();
            } else {
               this.layersBuilder_.remove(index);
            }

            return this;
         }

         public VectorTile.Tile.Layer.Builder getLayersBuilder(int index) {
            return this.getLayersFieldBuilder().getBuilder(index);
         }

         @Override
         public VectorTile.Tile.LayerOrBuilder getLayersOrBuilder(int index) {
            return this.layersBuilder_ == null ? this.layers_.get(index) : this.layersBuilder_.getMessageOrBuilder(index);
         }

         @Override
         public List<? extends VectorTile.Tile.LayerOrBuilder> getLayersOrBuilderList() {
            return this.layersBuilder_ != null ? this.layersBuilder_.getMessageOrBuilderList() : Collections.unmodifiableList(this.layers_);
         }

         public VectorTile.Tile.Layer.Builder addLayersBuilder() {
            return this.getLayersFieldBuilder().addBuilder(VectorTile.Tile.Layer.getDefaultInstance());
         }

         public VectorTile.Tile.Layer.Builder addLayersBuilder(int index) {
            return this.getLayersFieldBuilder().addBuilder(index, VectorTile.Tile.Layer.getDefaultInstance());
         }

         public List<VectorTile.Tile.Layer.Builder> getLayersBuilderList() {
            return this.getLayersFieldBuilder().getBuilderList();
         }

         private RepeatedFieldBuilder<VectorTile.Tile.Layer, VectorTile.Tile.Layer.Builder, VectorTile.Tile.LayerOrBuilder> getLayersFieldBuilder() {
            if (this.layersBuilder_ == null) {
               this.layersBuilder_ = new RepeatedFieldBuilder<>(this.layers_, (this.bitField0_ & 1) == 1, this.getParentForChildren(), this.isClean());
               this.layers_ = null;
            }

            return this.layersBuilder_;
         }
      }

      public static final class Feature extends GeneratedMessage implements VectorTile.Tile.FeatureOrBuilder {
         private static final VectorTile.Tile.Feature defaultInstance = new VectorTile.Tile.Feature(true);
         private final UnknownFieldSet unknownFields;
         public static Parser<VectorTile.Tile.Feature> PARSER = new AbstractParser<VectorTile.Tile.Feature>() {
            public VectorTile.Tile.Feature parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
               return new VectorTile.Tile.Feature(input, extensionRegistry);
            }
         };
         private int bitField0_;
         public static final int ID_FIELD_NUMBER = 1;
         private long id_;
         public static final int TAGS_FIELD_NUMBER = 2;
         private List<Integer> tags_;
         private int tagsMemoizedSerializedSize = -1;
         public static final int TYPE_FIELD_NUMBER = 3;
         private VectorTile.Tile.GeomType type_;
         public static final int GEOMETRY_FIELD_NUMBER = 4;
         private List<Integer> geometry_;
         private int geometryMemoizedSerializedSize = -1;
         private byte memoizedIsInitialized = -1;
         private int memoizedSerializedSize = -1;
         private static final long serialVersionUID = 0L;

         private Feature(GeneratedMessage.Builder<?> builder) {
            super(builder);
            this.unknownFields = builder.getUnknownFields();
         }

         private Feature(boolean noInit) {
            this.unknownFields = UnknownFieldSet.getDefaultInstance();
         }

         public static VectorTile.Tile.Feature getDefaultInstance() {
            return defaultInstance;
         }

         public VectorTile.Tile.Feature getDefaultInstanceForType() {
            return defaultInstance;
         }

         @Override
         public final UnknownFieldSet getUnknownFields() {
            return this.unknownFields;
         }

         private Feature(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            this.initFields();
            int mutable_bitField0_ = 0;
            UnknownFieldSet.Builder unknownFields = UnknownFieldSet.newBuilder();

            try {
               boolean done = false;

               while(!done) {
                  int tag = input.readTag();
                  int limit;
                  switch(tag) {
                     case 0:
                        done = true;
                        continue;
                     case 8:
                        this.bitField0_ |= 1;
                        this.id_ = input.readUInt64();
                        continue;
                     case 16:
                        if ((mutable_bitField0_ & 2) != 2) {
                           this.tags_ = new ArrayList<>();
                           mutable_bitField0_ |= 2;
                        }

                        this.tags_.add(input.readUInt32());
                        continue;
                     case 18:
                        int length = input.readRawVarint32();
                        limit = input.pushLimit(length);
                        if ((mutable_bitField0_ & 2) != 2 && input.getBytesUntilLimit() > 0) {
                           this.tags_ = new ArrayList<>();
                           mutable_bitField0_ |= 2;
                        }
                        break;
                     case 24:
                        int rawValue = input.readEnum();
                        VectorTile.Tile.GeomType value = VectorTile.Tile.GeomType.valueOf(rawValue);
                        if (value == null) {
                           unknownFields.mergeVarintField(3, rawValue);
                        } else {
                           this.bitField0_ |= 2;
                           this.type_ = value;
                        }
                        continue;
                     case 32:
                        if ((mutable_bitField0_ & 8) != 8) {
                           this.geometry_ = new ArrayList<>();
                           mutable_bitField0_ |= 8;
                        }

                        this.geometry_.add(input.readUInt32());
                        continue;
                     case 34:
                        int bytesLength = input.readRawVarint32();
                        limit = input.pushLimit(bytesLength);
                        if ((mutable_bitField0_ & 8) != 8 && input.getBytesUntilLimit() > 0) {
                           this.geometry_ = new ArrayList<>();
                           mutable_bitField0_ |= 8;
                        }

                        while(input.getBytesUntilLimit() > 0) {
                           this.geometry_.add(input.readUInt32());
                        }

                        input.popLimit(limit);
                        continue;
                     default:
                        if (!this.parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                           done = true;
                        }
                        continue;
                  }

                  while(input.getBytesUntilLimit() > 0) {
                     this.tags_.add(input.readUInt32());
                  }

                  input.popLimit(limit);
               }
            } catch (InvalidProtocolBufferException var13) {
               throw var13.setUnfinishedMessage(this);
            } catch (IOException var14) {
               throw new InvalidProtocolBufferException(var14.getMessage()).setUnfinishedMessage(this);
            } finally {
               if ((mutable_bitField0_ & 2) == 2) {
                  this.tags_ = Collections.unmodifiableList(this.tags_);
               }

               if ((mutable_bitField0_ & 8) == 8) {
                  this.geometry_ = Collections.unmodifiableList(this.geometry_);
               }

               this.unknownFields = unknownFields.build();
               this.makeExtensionsImmutable();
            }
         }

         public static final Descriptors.Descriptor getDescriptor() {
            return VectorTile.internal_static_OsmAnd_VectorTile_Tile_Feature_descriptor;
         }

         @Override
         protected GeneratedMessage.FieldAccessorTable internalGetFieldAccessorTable() {
            return VectorTile.internal_static_OsmAnd_VectorTile_Tile_Feature_fieldAccessorTable
               .ensureFieldAccessorsInitialized(VectorTile.Tile.Feature.class, VectorTile.Tile.Feature.Builder.class);
         }

         @Override
         public Parser<VectorTile.Tile.Feature> getParserForType() {
            return PARSER;
         }

         @Override
         public boolean hasId() {
            return (this.bitField0_ & 1) == 1;
         }

         @Override
         public long getId() {
            return this.id_;
         }

         @Override
         public List<Integer> getTagsList() {
            return this.tags_;
         }

         @Override
         public int getTagsCount() {
            return this.tags_.size();
         }

         @Override
         public int getTags(int index) {
            return this.tags_.get(index);
         }

         @Override
         public boolean hasType() {
            return (this.bitField0_ & 2) == 2;
         }

         @Override
         public VectorTile.Tile.GeomType getType() {
            return this.type_;
         }

         @Override
         public List<Integer> getGeometryList() {
            return this.geometry_;
         }

         @Override
         public int getGeometryCount() {
            return this.geometry_.size();
         }

         @Override
         public int getGeometry(int index) {
            return this.geometry_.get(index);
         }

         private void initFields() {
            this.id_ = 0L;
            this.tags_ = Collections.emptyList();
            this.type_ = VectorTile.Tile.GeomType.UNKNOWN;
            this.geometry_ = Collections.emptyList();
         }

         @Override
         public final boolean isInitialized() {
            byte isInitialized = this.memoizedIsInitialized;
            if (isInitialized != -1) {
               return isInitialized == 1;
            } else {
               this.memoizedIsInitialized = 1;
               return true;
            }
         }

         @Override
         public void writeTo(CodedOutputStream output) throws IOException {
            this.getSerializedSize();
            if ((this.bitField0_ & 1) == 1) {
               output.writeUInt64(1, this.id_);
            }

            if (this.getTagsList().size() > 0) {
               output.writeRawVarint32(18);
               output.writeRawVarint32(this.tagsMemoizedSerializedSize);
            }

            for(int i = 0; i < this.tags_.size(); ++i) {
               output.writeUInt32NoTag(this.tags_.get(i));
            }

            if ((this.bitField0_ & 2) == 2) {
               output.writeEnum(3, this.type_.getNumber());
            }

            if (this.getGeometryList().size() > 0) {
               output.writeRawVarint32(34);
               output.writeRawVarint32(this.geometryMemoizedSerializedSize);
            }

            for(int i = 0; i < this.geometry_.size(); ++i) {
               output.writeUInt32NoTag(this.geometry_.get(i));
            }

            this.getUnknownFields().writeTo(output);
         }

         @Override
         public int getSerializedSize() {
            int size = this.memoizedSerializedSize;
            if (size != -1) {
               return size;
            } else {
               size = 0;
               if ((this.bitField0_ & 1) == 1) {
                  size += CodedOutputStream.computeUInt64Size(1, this.id_);
               }

               int dataSize = 0;

               for(int i = 0; i < this.tags_.size(); ++i) {
                  dataSize += CodedOutputStream.computeUInt32SizeNoTag(this.tags_.get(i));
               }

               size += dataSize;
               if (!this.getTagsList().isEmpty()) {
                  size = ++size + CodedOutputStream.computeInt32SizeNoTag(dataSize);
               }

               this.tagsMemoizedSerializedSize = dataSize;
               if ((this.bitField0_ & 2) == 2) {
                  size += CodedOutputStream.computeEnumSize(3, this.type_.getNumber());
               }

               dataSize = 0;

               for(int i = 0; i < this.geometry_.size(); ++i) {
                  dataSize += CodedOutputStream.computeUInt32SizeNoTag(this.geometry_.get(i));
               }

               size += dataSize;
               if (!this.getGeometryList().isEmpty()) {
                  size = ++size + CodedOutputStream.computeInt32SizeNoTag(dataSize);
               }

               this.geometryMemoizedSerializedSize = dataSize;
               size += this.getUnknownFields().getSerializedSize();
               this.memoizedSerializedSize = size;
               return size;
            }
         }

         @Override
         protected Object writeReplace() throws ObjectStreamException {
            return super.writeReplace();
         }

         public static VectorTile.Tile.Feature parseFrom(ByteString data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
         }

         public static VectorTile.Tile.Feature parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
         }

         public static VectorTile.Tile.Feature parseFrom(byte[] data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
         }

         public static VectorTile.Tile.Feature parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
         }

         public static VectorTile.Tile.Feature parseFrom(InputStream input) throws IOException {
            return PARSER.parseFrom(input);
         }

         public static VectorTile.Tile.Feature parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return PARSER.parseFrom(input, extensionRegistry);
         }

         public static VectorTile.Tile.Feature parseDelimitedFrom(InputStream input) throws IOException {
            return PARSER.parseDelimitedFrom(input);
         }

         public static VectorTile.Tile.Feature parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return PARSER.parseDelimitedFrom(input, extensionRegistry);
         }

         public static VectorTile.Tile.Feature parseFrom(CodedInputStream input) throws IOException {
            return PARSER.parseFrom(input);
         }

         public static VectorTile.Tile.Feature parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return PARSER.parseFrom(input, extensionRegistry);
         }

         public static VectorTile.Tile.Feature.Builder newBuilder() {
            return VectorTile.Tile.Feature.Builder.create();
         }

         public VectorTile.Tile.Feature.Builder newBuilderForType() {
            return newBuilder();
         }

         public static VectorTile.Tile.Feature.Builder newBuilder(VectorTile.Tile.Feature prototype) {
            return newBuilder().mergeFrom(prototype);
         }

         public VectorTile.Tile.Feature.Builder toBuilder() {
            return newBuilder(this);
         }

         protected VectorTile.Tile.Feature.Builder newBuilderForType(GeneratedMessage.BuilderParent parent) {
            return new VectorTile.Tile.Feature.Builder(parent);
         }

         static {
            defaultInstance.initFields();
         }

         public static final class Builder extends GeneratedMessage.Builder<VectorTile.Tile.Feature.Builder> implements VectorTile.Tile.FeatureOrBuilder {
            private int bitField0_;
            private long id_;
            private List<Integer> tags_ = Collections.emptyList();
            private VectorTile.Tile.GeomType type_ = VectorTile.Tile.GeomType.UNKNOWN;
            private List<Integer> geometry_ = Collections.emptyList();

            public static final Descriptors.Descriptor getDescriptor() {
               return VectorTile.internal_static_OsmAnd_VectorTile_Tile_Feature_descriptor;
            }

            @Override
            protected GeneratedMessage.FieldAccessorTable internalGetFieldAccessorTable() {
               return VectorTile.internal_static_OsmAnd_VectorTile_Tile_Feature_fieldAccessorTable
                  .ensureFieldAccessorsInitialized(VectorTile.Tile.Feature.class, VectorTile.Tile.Feature.Builder.class);
            }

            private Builder() {
               this.maybeForceBuilderInitialization();
            }

            private Builder(GeneratedMessage.BuilderParent parent) {
               super(parent);
               this.maybeForceBuilderInitialization();
            }

            private void maybeForceBuilderInitialization() {
               if (VectorTile.Tile.Feature.alwaysUseFieldBuilders) {
               }
            }

            private static VectorTile.Tile.Feature.Builder create() {
               return new VectorTile.Tile.Feature.Builder();
            }

            public VectorTile.Tile.Feature.Builder clear() {
               super.clear();
               this.id_ = 0L;
               this.bitField0_ &= -2;
               this.tags_ = Collections.emptyList();
               this.bitField0_ &= -3;
               this.type_ = VectorTile.Tile.GeomType.UNKNOWN;
               this.bitField0_ &= -5;
               this.geometry_ = Collections.emptyList();
               this.bitField0_ &= -9;
               return this;
            }

            public VectorTile.Tile.Feature.Builder clone() {
               return create().mergeFrom(this.buildPartial());
            }

            @Override
            public Descriptors.Descriptor getDescriptorForType() {
               return VectorTile.internal_static_OsmAnd_VectorTile_Tile_Feature_descriptor;
            }

            public VectorTile.Tile.Feature getDefaultInstanceForType() {
               return VectorTile.Tile.Feature.getDefaultInstance();
            }

            public VectorTile.Tile.Feature build() {
               VectorTile.Tile.Feature result = this.buildPartial();
               if (!result.isInitialized()) {
                  throw newUninitializedMessageException(result);
               } else {
                  return result;
               }
            }

            public VectorTile.Tile.Feature buildPartial() {
               VectorTile.Tile.Feature result = new VectorTile.Tile.Feature(this);
               int from_bitField0_ = this.bitField0_;
               int to_bitField0_ = 0;
               if ((from_bitField0_ & 1) == 1) {
                  to_bitField0_ |= 1;
               }

               result.id_ = this.id_;
               if ((this.bitField0_ & 2) == 2) {
                  this.tags_ = Collections.unmodifiableList(this.tags_);
                  this.bitField0_ &= -3;
               }

               result.tags_ = this.tags_;
               if ((from_bitField0_ & 4) == 4) {
                  to_bitField0_ |= 2;
               }

               result.type_ = this.type_;
               if ((this.bitField0_ & 8) == 8) {
                  this.geometry_ = Collections.unmodifiableList(this.geometry_);
                  this.bitField0_ &= -9;
               }

               result.geometry_ = this.geometry_;
               result.bitField0_ = to_bitField0_;
               this.onBuilt();
               return result;
            }

            public VectorTile.Tile.Feature.Builder mergeFrom(Message other) {
               if (other instanceof VectorTile.Tile.Feature) {
                  return this.mergeFrom((VectorTile.Tile.Feature)other);
               } else {
                  super.mergeFrom(other);
                  return this;
               }
            }

            public VectorTile.Tile.Feature.Builder mergeFrom(VectorTile.Tile.Feature other) {
               if (other == VectorTile.Tile.Feature.getDefaultInstance()) {
                  return this;
               } else {
                  if (other.hasId()) {
                     this.setId(other.getId());
                  }

                  if (!other.tags_.isEmpty()) {
                     if (this.tags_.isEmpty()) {
                        this.tags_ = other.tags_;
                        this.bitField0_ &= -3;
                     } else {
                        this.ensureTagsIsMutable();
                        this.tags_.addAll(other.tags_);
                     }

                     this.onChanged();
                  }

                  if (other.hasType()) {
                     this.setType(other.getType());
                  }

                  if (!other.geometry_.isEmpty()) {
                     if (this.geometry_.isEmpty()) {
                        this.geometry_ = other.geometry_;
                        this.bitField0_ &= -9;
                     } else {
                        this.ensureGeometryIsMutable();
                        this.geometry_.addAll(other.geometry_);
                     }

                     this.onChanged();
                  }

                  this.mergeUnknownFields(other.getUnknownFields());
                  return this;
               }
            }

            @Override
            public final boolean isInitialized() {
               return true;
            }

            public VectorTile.Tile.Feature.Builder mergeFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
               VectorTile.Tile.Feature parsedMessage = null;

               try {
                  parsedMessage = VectorTile.Tile.Feature.PARSER.parsePartialFrom(input, extensionRegistry);
               } catch (InvalidProtocolBufferException var8) {
                  parsedMessage = (VectorTile.Tile.Feature)var8.getUnfinishedMessage();
                  throw var8;
               } finally {
                  if (parsedMessage != null) {
                     this.mergeFrom(parsedMessage);
                  }
               }

               return this;
            }

            @Override
            public boolean hasId() {
               return (this.bitField0_ & 1) == 1;
            }

            @Override
            public long getId() {
               return this.id_;
            }

            public VectorTile.Tile.Feature.Builder setId(long value) {
               this.bitField0_ |= 1;
               this.id_ = value;
               this.onChanged();
               return this;
            }

            public VectorTile.Tile.Feature.Builder clearId() {
               this.bitField0_ &= -2;
               this.id_ = 0L;
               this.onChanged();
               return this;
            }

            private void ensureTagsIsMutable() {
               if ((this.bitField0_ & 2) != 2) {
                  this.tags_ = new ArrayList<>(this.tags_);
                  this.bitField0_ |= 2;
               }
            }

            @Override
            public List<Integer> getTagsList() {
               return Collections.unmodifiableList(this.tags_);
            }

            @Override
            public int getTagsCount() {
               return this.tags_.size();
            }

            @Override
            public int getTags(int index) {
               return this.tags_.get(index);
            }

            public VectorTile.Tile.Feature.Builder setTags(int index, int value) {
               this.ensureTagsIsMutable();
               this.tags_.set(index, value);
               this.onChanged();
               return this;
            }

            public VectorTile.Tile.Feature.Builder addTags(int value) {
               this.ensureTagsIsMutable();
               this.tags_.add(value);
               this.onChanged();
               return this;
            }

            public VectorTile.Tile.Feature.Builder addAllTags(Iterable<? extends Integer> values) {
               this.ensureTagsIsMutable();
               GeneratedMessage.Builder.addAll(values, this.tags_);
               this.onChanged();
               return this;
            }

            public VectorTile.Tile.Feature.Builder clearTags() {
               this.tags_ = Collections.emptyList();
               this.bitField0_ &= -3;
               this.onChanged();
               return this;
            }

            @Override
            public boolean hasType() {
               return (this.bitField0_ & 4) == 4;
            }

            @Override
            public VectorTile.Tile.GeomType getType() {
               return this.type_;
            }

            public VectorTile.Tile.Feature.Builder setType(VectorTile.Tile.GeomType value) {
               if (value == null) {
                  throw new NullPointerException();
               } else {
                  this.bitField0_ |= 4;
                  this.type_ = value;
                  this.onChanged();
                  return this;
               }
            }

            public VectorTile.Tile.Feature.Builder clearType() {
               this.bitField0_ &= -5;
               this.type_ = VectorTile.Tile.GeomType.UNKNOWN;
               this.onChanged();
               return this;
            }

            private void ensureGeometryIsMutable() {
               if ((this.bitField0_ & 8) != 8) {
                  this.geometry_ = new ArrayList<>(this.geometry_);
                  this.bitField0_ |= 8;
               }
            }

            @Override
            public List<Integer> getGeometryList() {
               return Collections.unmodifiableList(this.geometry_);
            }

            @Override
            public int getGeometryCount() {
               return this.geometry_.size();
            }

            @Override
            public int getGeometry(int index) {
               return this.geometry_.get(index);
            }

            public VectorTile.Tile.Feature.Builder setGeometry(int index, int value) {
               this.ensureGeometryIsMutable();
               this.geometry_.set(index, value);
               this.onChanged();
               return this;
            }

            public VectorTile.Tile.Feature.Builder addGeometry(int value) {
               this.ensureGeometryIsMutable();
               this.geometry_.add(value);
               this.onChanged();
               return this;
            }

            public VectorTile.Tile.Feature.Builder addAllGeometry(Iterable<? extends Integer> values) {
               this.ensureGeometryIsMutable();
               GeneratedMessage.Builder.addAll(values, this.geometry_);
               this.onChanged();
               return this;
            }

            public VectorTile.Tile.Feature.Builder clearGeometry() {
               this.geometry_ = Collections.emptyList();
               this.bitField0_ &= -9;
               this.onChanged();
               return this;
            }
         }
      }

      public interface FeatureOrBuilder extends MessageOrBuilder {
         boolean hasId();

         long getId();

         List<Integer> getTagsList();

         int getTagsCount();

         int getTags(int var1);

         boolean hasType();

         VectorTile.Tile.GeomType getType();

         List<Integer> getGeometryList();

         int getGeometryCount();

         int getGeometry(int var1);
      }

      public static enum GeomType implements ProtocolMessageEnum {
         UNKNOWN(0, 0),
         POINT(1, 1),
         LINESTRING(2, 2),
         POLYGON(3, 3);

         public static final int UNKNOWN_VALUE = 0;
         public static final int POINT_VALUE = 1;
         public static final int LINESTRING_VALUE = 2;
         public static final int POLYGON_VALUE = 3;
         private static Internal.EnumLiteMap<VectorTile.Tile.GeomType> internalValueMap = new Internal.EnumLiteMap<VectorTile.Tile.GeomType>() {
            public VectorTile.Tile.GeomType findValueByNumber(int number) {
               return VectorTile.Tile.GeomType.valueOf(number);
            }
         };
         private static final VectorTile.Tile.GeomType[] VALUES = values();
         private final int index;
         private final int value;

         @Override
         public final int getNumber() {
            return this.value;
         }

         public static VectorTile.Tile.GeomType valueOf(int value) {
            switch(value) {
               case 0:
                  return UNKNOWN;
               case 1:
                  return POINT;
               case 2:
                  return LINESTRING;
               case 3:
                  return POLYGON;
               default:
                  return null;
            }
         }

         public static Internal.EnumLiteMap<VectorTile.Tile.GeomType> internalGetValueMap() {
            return internalValueMap;
         }

         @Override
         public final Descriptors.EnumValueDescriptor getValueDescriptor() {
            return getDescriptor().getValues().get(this.index);
         }

         @Override
         public final Descriptors.EnumDescriptor getDescriptorForType() {
            return getDescriptor();
         }

         public static final Descriptors.EnumDescriptor getDescriptor() {
            return VectorTile.Tile.getDescriptor().getEnumTypes().get(0);
         }

         public static VectorTile.Tile.GeomType valueOf(Descriptors.EnumValueDescriptor desc) {
            if (desc.getType() != getDescriptor()) {
               throw new IllegalArgumentException("EnumValueDescriptor is not for this type.");
            } else {
               return VALUES[desc.getIndex()];
            }
         }

         private GeomType(int index, int value) {
            this.index = index;
            this.value = value;
         }
      }

      public static final class Layer extends GeneratedMessage.ExtendableMessage<VectorTile.Tile.Layer> implements VectorTile.Tile.LayerOrBuilder {
         private static final VectorTile.Tile.Layer defaultInstance = new VectorTile.Tile.Layer(true);
         private final UnknownFieldSet unknownFields;
         public static Parser<VectorTile.Tile.Layer> PARSER = new AbstractParser<VectorTile.Tile.Layer>() {
            public VectorTile.Tile.Layer parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
               return new VectorTile.Tile.Layer(input, extensionRegistry);
            }
         };
         private int bitField0_;
         public static final int VERSION_FIELD_NUMBER = 15;
         private int version_;
         public static final int NAME_FIELD_NUMBER = 1;
         private Object name_;
         public static final int FEATURES_FIELD_NUMBER = 2;
         private List<VectorTile.Tile.Feature> features_;
         public static final int KEYS_FIELD_NUMBER = 3;
         private LazyStringList keys_;
         public static final int VALUES_FIELD_NUMBER = 4;
         private List<VectorTile.Tile.Value> values_;
         public static final int EXTENT_FIELD_NUMBER = 5;
         private int extent_;
         private byte memoizedIsInitialized = -1;
         private int memoizedSerializedSize = -1;
         private static final long serialVersionUID = 0L;

         private Layer(GeneratedMessage.ExtendableBuilder<VectorTile.Tile.Layer, ?> builder) {
            super(builder);
            this.unknownFields = builder.getUnknownFields();
         }

         private Layer(boolean noInit) {
            this.unknownFields = UnknownFieldSet.getDefaultInstance();
         }

         public static VectorTile.Tile.Layer getDefaultInstance() {
            return defaultInstance;
         }

         public VectorTile.Tile.Layer getDefaultInstanceForType() {
            return defaultInstance;
         }

         @Override
         public final UnknownFieldSet getUnknownFields() {
            return this.unknownFields;
         }

         private Layer(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            this.initFields();
            int mutable_bitField0_ = 0;
            UnknownFieldSet.Builder unknownFields = UnknownFieldSet.newBuilder();

            try {
               boolean done = false;

               while(!done) {
                  int tag = input.readTag();
                  switch(tag) {
                     case 0:
                        done = true;
                        break;
                     case 10:
                        this.bitField0_ |= 2;
                        this.name_ = input.readBytes();
                        break;
                     case 18:
                        if ((mutable_bitField0_ & 4) != 4) {
                           this.features_ = new ArrayList<>();
                           mutable_bitField0_ |= 4;
                        }

                        this.features_.add(input.readMessage(VectorTile.Tile.Feature.PARSER, extensionRegistry));
                        break;
                     case 26:
                        if ((mutable_bitField0_ & 8) != 8) {
                           this.keys_ = new LazyStringArrayList();
                           mutable_bitField0_ |= 8;
                        }

                        this.keys_.add(input.readBytes());
                        break;
                     case 34:
                        if ((mutable_bitField0_ & 16) != 16) {
                           this.values_ = new ArrayList<>();
                           mutable_bitField0_ |= 16;
                        }

                        this.values_.add(input.readMessage(VectorTile.Tile.Value.PARSER, extensionRegistry));
                        break;
                     case 40:
                        this.bitField0_ |= 4;
                        this.extent_ = input.readUInt32();
                        break;
                     case 120:
                        this.bitField0_ |= 1;
                        this.version_ = input.readUInt32();
                        break;
                     default:
                        if (!this.parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                           done = true;
                        }
                  }
               }
            } catch (InvalidProtocolBufferException var11) {
               throw var11.setUnfinishedMessage(this);
            } catch (IOException var12) {
               throw new InvalidProtocolBufferException(var12.getMessage()).setUnfinishedMessage(this);
            } finally {
               if ((mutable_bitField0_ & 4) == 4) {
                  this.features_ = Collections.unmodifiableList(this.features_);
               }

               if ((mutable_bitField0_ & 8) == 8) {
                  this.keys_ = new UnmodifiableLazyStringList(this.keys_);
               }

               if ((mutable_bitField0_ & 16) == 16) {
                  this.values_ = Collections.unmodifiableList(this.values_);
               }

               this.unknownFields = unknownFields.build();
               this.makeExtensionsImmutable();
            }
         }

         public static final Descriptors.Descriptor getDescriptor() {
            return VectorTile.internal_static_OsmAnd_VectorTile_Tile_Layer_descriptor;
         }

         @Override
         protected GeneratedMessage.FieldAccessorTable internalGetFieldAccessorTable() {
            return VectorTile.internal_static_OsmAnd_VectorTile_Tile_Layer_fieldAccessorTable
               .ensureFieldAccessorsInitialized(VectorTile.Tile.Layer.class, VectorTile.Tile.Layer.Builder.class);
         }

         @Override
         public Parser<VectorTile.Tile.Layer> getParserForType() {
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
         public boolean hasName() {
            return (this.bitField0_ & 2) == 2;
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
         public List<VectorTile.Tile.Feature> getFeaturesList() {
            return this.features_;
         }

         @Override
         public List<? extends VectorTile.Tile.FeatureOrBuilder> getFeaturesOrBuilderList() {
            return this.features_;
         }

         @Override
         public int getFeaturesCount() {
            return this.features_.size();
         }

         @Override
         public VectorTile.Tile.Feature getFeatures(int index) {
            return this.features_.get(index);
         }

         @Override
         public VectorTile.Tile.FeatureOrBuilder getFeaturesOrBuilder(int index) {
            return this.features_.get(index);
         }

         @Override
         public List<String> getKeysList() {
            return this.keys_;
         }

         @Override
         public int getKeysCount() {
            return this.keys_.size();
         }

         @Override
         public String getKeys(int index) {
            return this.keys_.get(index);
         }

         @Override
         public ByteString getKeysBytes(int index) {
            return this.keys_.getByteString(index);
         }

         @Override
         public List<VectorTile.Tile.Value> getValuesList() {
            return this.values_;
         }

         @Override
         public List<? extends VectorTile.Tile.ValueOrBuilder> getValuesOrBuilderList() {
            return this.values_;
         }

         @Override
         public int getValuesCount() {
            return this.values_.size();
         }

         @Override
         public VectorTile.Tile.Value getValues(int index) {
            return this.values_.get(index);
         }

         @Override
         public VectorTile.Tile.ValueOrBuilder getValuesOrBuilder(int index) {
            return this.values_.get(index);
         }

         @Override
         public boolean hasExtent() {
            return (this.bitField0_ & 4) == 4;
         }

         @Override
         public int getExtent() {
            return this.extent_;
         }

         private void initFields() {
            this.version_ = 1;
            this.name_ = "";
            this.features_ = Collections.emptyList();
            this.keys_ = LazyStringArrayList.EMPTY;
            this.values_ = Collections.emptyList();
            this.extent_ = 4096;
         }

         @Override
         public final boolean isInitialized() {
            byte isInitialized = this.memoizedIsInitialized;
            if (isInitialized != -1) {
               return isInitialized == 1;
            } else if (!this.hasVersion()) {
               this.memoizedIsInitialized = 0;
               return false;
            } else if (!this.hasName()) {
               this.memoizedIsInitialized = 0;
               return false;
            } else {
               for(int i = 0; i < this.getValuesCount(); ++i) {
                  if (!this.getValues(i).isInitialized()) {
                     this.memoizedIsInitialized = 0;
                     return false;
                  }
               }

               if (!this.extensionsAreInitialized()) {
                  this.memoizedIsInitialized = 0;
                  return false;
               } else {
                  this.memoizedIsInitialized = 1;
                  return true;
               }
            }
         }

         @Override
         public void writeTo(CodedOutputStream output) throws IOException {
            this.getSerializedSize();
            GeneratedMessage.ExtendableMessage<VectorTile.Tile.Layer>.ExtensionWriter extensionWriter = this.newExtensionWriter();
            if ((this.bitField0_ & 2) == 2) {
               output.writeBytes(1, this.getNameBytes());
            }

            for(int i = 0; i < this.features_.size(); ++i) {
               output.writeMessage(2, this.features_.get(i));
            }

            for(int i = 0; i < this.keys_.size(); ++i) {
               output.writeBytes(3, this.keys_.getByteString(i));
            }

            for(int i = 0; i < this.values_.size(); ++i) {
               output.writeMessage(4, this.values_.get(i));
            }

            if ((this.bitField0_ & 4) == 4) {
               output.writeUInt32(5, this.extent_);
            }

            if ((this.bitField0_ & 1) == 1) {
               output.writeUInt32(15, this.version_);
            }

            extensionWriter.writeUntil(536870912, output);
            this.getUnknownFields().writeTo(output);
         }

         @Override
         public int getSerializedSize() {
            int size = this.memoizedSerializedSize;
            if (size != -1) {
               return size;
            } else {
               size = 0;
               if ((this.bitField0_ & 2) == 2) {
                  size += CodedOutputStream.computeBytesSize(1, this.getNameBytes());
               }

               for(int i = 0; i < this.features_.size(); ++i) {
                  size += CodedOutputStream.computeMessageSize(2, this.features_.get(i));
               }

               int dataSize = 0;

               for(int i = 0; i < this.keys_.size(); ++i) {
                  dataSize += CodedOutputStream.computeBytesSizeNoTag(this.keys_.getByteString(i));
               }

               size += dataSize;
               size += 1 * this.getKeysList().size();

               for(int i = 0; i < this.values_.size(); ++i) {
                  size += CodedOutputStream.computeMessageSize(4, this.values_.get(i));
               }

               if ((this.bitField0_ & 4) == 4) {
                  size += CodedOutputStream.computeUInt32Size(5, this.extent_);
               }

               if ((this.bitField0_ & 1) == 1) {
                  size += CodedOutputStream.computeUInt32Size(15, this.version_);
               }

               size += this.extensionsSerializedSize();
               size += this.getUnknownFields().getSerializedSize();
               this.memoizedSerializedSize = size;
               return size;
            }
         }

         @Override
         protected Object writeReplace() throws ObjectStreamException {
            return super.writeReplace();
         }

         public static VectorTile.Tile.Layer parseFrom(ByteString data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
         }

         public static VectorTile.Tile.Layer parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
         }

         public static VectorTile.Tile.Layer parseFrom(byte[] data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
         }

         public static VectorTile.Tile.Layer parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
         }

         public static VectorTile.Tile.Layer parseFrom(InputStream input) throws IOException {
            return PARSER.parseFrom(input);
         }

         public static VectorTile.Tile.Layer parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return PARSER.parseFrom(input, extensionRegistry);
         }

         public static VectorTile.Tile.Layer parseDelimitedFrom(InputStream input) throws IOException {
            return PARSER.parseDelimitedFrom(input);
         }

         public static VectorTile.Tile.Layer parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return PARSER.parseDelimitedFrom(input, extensionRegistry);
         }

         public static VectorTile.Tile.Layer parseFrom(CodedInputStream input) throws IOException {
            return PARSER.parseFrom(input);
         }

         public static VectorTile.Tile.Layer parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return PARSER.parseFrom(input, extensionRegistry);
         }

         public static VectorTile.Tile.Layer.Builder newBuilder() {
            return VectorTile.Tile.Layer.Builder.create();
         }

         public VectorTile.Tile.Layer.Builder newBuilderForType() {
            return newBuilder();
         }

         public static VectorTile.Tile.Layer.Builder newBuilder(VectorTile.Tile.Layer prototype) {
            return newBuilder().mergeFrom(prototype);
         }

         public VectorTile.Tile.Layer.Builder toBuilder() {
            return newBuilder(this);
         }

         protected VectorTile.Tile.Layer.Builder newBuilderForType(GeneratedMessage.BuilderParent parent) {
            return new VectorTile.Tile.Layer.Builder(parent);
         }

         static {
            defaultInstance.initFields();
         }

         public static final class Builder
            extends GeneratedMessage.ExtendableBuilder<VectorTile.Tile.Layer, VectorTile.Tile.Layer.Builder>
            implements VectorTile.Tile.LayerOrBuilder {
            private int bitField0_;
            private int version_ = 1;
            private Object name_ = "";
            private List<VectorTile.Tile.Feature> features_ = Collections.emptyList();
            private RepeatedFieldBuilder<VectorTile.Tile.Feature, VectorTile.Tile.Feature.Builder, VectorTile.Tile.FeatureOrBuilder> featuresBuilder_;
            private LazyStringList keys_ = LazyStringArrayList.EMPTY;
            private List<VectorTile.Tile.Value> values_ = Collections.emptyList();
            private RepeatedFieldBuilder<VectorTile.Tile.Value, VectorTile.Tile.Value.Builder, VectorTile.Tile.ValueOrBuilder> valuesBuilder_;
            private int extent_ = 4096;

            public static final Descriptors.Descriptor getDescriptor() {
               return VectorTile.internal_static_OsmAnd_VectorTile_Tile_Layer_descriptor;
            }

            @Override
            protected GeneratedMessage.FieldAccessorTable internalGetFieldAccessorTable() {
               return VectorTile.internal_static_OsmAnd_VectorTile_Tile_Layer_fieldAccessorTable
                  .ensureFieldAccessorsInitialized(VectorTile.Tile.Layer.class, VectorTile.Tile.Layer.Builder.class);
            }

            private Builder() {
               this.maybeForceBuilderInitialization();
            }

            private Builder(GeneratedMessage.BuilderParent parent) {
               super(parent);
               this.maybeForceBuilderInitialization();
            }

            private void maybeForceBuilderInitialization() {
               if (VectorTile.Tile.Layer.alwaysUseFieldBuilders) {
                  this.getFeaturesFieldBuilder();
                  this.getValuesFieldBuilder();
               }
            }

            private static VectorTile.Tile.Layer.Builder create() {
               return new VectorTile.Tile.Layer.Builder();
            }

            public VectorTile.Tile.Layer.Builder clear() {
               super.clear();
               this.version_ = 1;
               this.bitField0_ &= -2;
               this.name_ = "";
               this.bitField0_ &= -3;
               if (this.featuresBuilder_ == null) {
                  this.features_ = Collections.emptyList();
                  this.bitField0_ &= -5;
               } else {
                  this.featuresBuilder_.clear();
               }

               this.keys_ = LazyStringArrayList.EMPTY;
               this.bitField0_ &= -9;
               if (this.valuesBuilder_ == null) {
                  this.values_ = Collections.emptyList();
                  this.bitField0_ &= -17;
               } else {
                  this.valuesBuilder_.clear();
               }

               this.extent_ = 4096;
               this.bitField0_ &= -33;
               return this;
            }

            public VectorTile.Tile.Layer.Builder clone() {
               return create().mergeFrom(this.buildPartial());
            }

            @Override
            public Descriptors.Descriptor getDescriptorForType() {
               return VectorTile.internal_static_OsmAnd_VectorTile_Tile_Layer_descriptor;
            }

            public VectorTile.Tile.Layer getDefaultInstanceForType() {
               return VectorTile.Tile.Layer.getDefaultInstance();
            }

            public VectorTile.Tile.Layer build() {
               VectorTile.Tile.Layer result = this.buildPartial();
               if (!result.isInitialized()) {
                  throw newUninitializedMessageException(result);
               } else {
                  return result;
               }
            }

            public VectorTile.Tile.Layer buildPartial() {
               VectorTile.Tile.Layer result = new VectorTile.Tile.Layer(this);
               int from_bitField0_ = this.bitField0_;
               int to_bitField0_ = 0;
               if ((from_bitField0_ & 1) == 1) {
                  to_bitField0_ |= 1;
               }

               result.version_ = this.version_;
               if ((from_bitField0_ & 2) == 2) {
                  to_bitField0_ |= 2;
               }

               result.name_ = this.name_;
               if (this.featuresBuilder_ == null) {
                  if ((this.bitField0_ & 4) == 4) {
                     this.features_ = Collections.unmodifiableList(this.features_);
                     this.bitField0_ &= -5;
                  }

                  result.features_ = this.features_;
               } else {
                  result.features_ = this.featuresBuilder_.build();
               }

               if ((this.bitField0_ & 8) == 8) {
                  this.keys_ = new UnmodifiableLazyStringList(this.keys_);
                  this.bitField0_ &= -9;
               }

               result.keys_ = this.keys_;
               if (this.valuesBuilder_ == null) {
                  if ((this.bitField0_ & 16) == 16) {
                     this.values_ = Collections.unmodifiableList(this.values_);
                     this.bitField0_ &= -17;
                  }

                  result.values_ = this.values_;
               } else {
                  result.values_ = this.valuesBuilder_.build();
               }

               if ((from_bitField0_ & 32) == 32) {
                  to_bitField0_ |= 4;
               }

               result.extent_ = this.extent_;
               result.bitField0_ = to_bitField0_;
               this.onBuilt();
               return result;
            }

            public VectorTile.Tile.Layer.Builder mergeFrom(Message other) {
               if (other instanceof VectorTile.Tile.Layer) {
                  return this.mergeFrom((VectorTile.Tile.Layer)other);
               } else {
                  super.mergeFrom(other);
                  return this;
               }
            }

            public VectorTile.Tile.Layer.Builder mergeFrom(VectorTile.Tile.Layer other) {
               if (other == VectorTile.Tile.Layer.getDefaultInstance()) {
                  return this;
               } else {
                  if (other.hasVersion()) {
                     this.setVersion(other.getVersion());
                  }

                  if (other.hasName()) {
                     this.bitField0_ |= 2;
                     this.name_ = other.name_;
                     this.onChanged();
                  }

                  if (this.featuresBuilder_ == null) {
                     if (!other.features_.isEmpty()) {
                        if (this.features_.isEmpty()) {
                           this.features_ = other.features_;
                           this.bitField0_ &= -5;
                        } else {
                           this.ensureFeaturesIsMutable();
                           this.features_.addAll(other.features_);
                        }

                        this.onChanged();
                     }
                  } else if (!other.features_.isEmpty()) {
                     if (this.featuresBuilder_.isEmpty()) {
                        this.featuresBuilder_.dispose();
                        this.featuresBuilder_ = null;
                        this.features_ = other.features_;
                        this.bitField0_ &= -5;
                        this.featuresBuilder_ = VectorTile.Tile.Layer.alwaysUseFieldBuilders ? this.getFeaturesFieldBuilder() : null;
                     } else {
                        this.featuresBuilder_.addAllMessages(other.features_);
                     }
                  }

                  if (!other.keys_.isEmpty()) {
                     if (this.keys_.isEmpty()) {
                        this.keys_ = other.keys_;
                        this.bitField0_ &= -9;
                     } else {
                        this.ensureKeysIsMutable();
                        this.keys_.addAll(other.keys_);
                     }

                     this.onChanged();
                  }

                  if (this.valuesBuilder_ == null) {
                     if (!other.values_.isEmpty()) {
                        if (this.values_.isEmpty()) {
                           this.values_ = other.values_;
                           this.bitField0_ &= -17;
                        } else {
                           this.ensureValuesIsMutable();
                           this.values_.addAll(other.values_);
                        }

                        this.onChanged();
                     }
                  } else if (!other.values_.isEmpty()) {
                     if (this.valuesBuilder_.isEmpty()) {
                        this.valuesBuilder_.dispose();
                        this.valuesBuilder_ = null;
                        this.values_ = other.values_;
                        this.bitField0_ &= -17;
                        this.valuesBuilder_ = VectorTile.Tile.Layer.alwaysUseFieldBuilders ? this.getValuesFieldBuilder() : null;
                     } else {
                        this.valuesBuilder_.addAllMessages(other.values_);
                     }
                  }

                  if (other.hasExtent()) {
                     this.setExtent(other.getExtent());
                  }

                  this.mergeExtensionFields(other);
                  this.mergeUnknownFields(other.getUnknownFields());
                  return this;
               }
            }

            @Override
            public final boolean isInitialized() {
               if (!this.hasVersion()) {
                  return false;
               } else if (!this.hasName()) {
                  return false;
               } else {
                  for(int i = 0; i < this.getValuesCount(); ++i) {
                     if (!this.getValues(i).isInitialized()) {
                        return false;
                     }
                  }

                  return this.extensionsAreInitialized();
               }
            }

            public VectorTile.Tile.Layer.Builder mergeFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
               VectorTile.Tile.Layer parsedMessage = null;

               try {
                  parsedMessage = VectorTile.Tile.Layer.PARSER.parsePartialFrom(input, extensionRegistry);
               } catch (InvalidProtocolBufferException var8) {
                  parsedMessage = (VectorTile.Tile.Layer)var8.getUnfinishedMessage();
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

            public VectorTile.Tile.Layer.Builder setVersion(int value) {
               this.bitField0_ |= 1;
               this.version_ = value;
               this.onChanged();
               return this;
            }

            public VectorTile.Tile.Layer.Builder clearVersion() {
               this.bitField0_ &= -2;
               this.version_ = 1;
               this.onChanged();
               return this;
            }

            @Override
            public boolean hasName() {
               return (this.bitField0_ & 2) == 2;
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

            public VectorTile.Tile.Layer.Builder setName(String value) {
               if (value == null) {
                  throw new NullPointerException();
               } else {
                  this.bitField0_ |= 2;
                  this.name_ = value;
                  this.onChanged();
                  return this;
               }
            }

            public VectorTile.Tile.Layer.Builder clearName() {
               this.bitField0_ &= -3;
               this.name_ = VectorTile.Tile.Layer.getDefaultInstance().getName();
               this.onChanged();
               return this;
            }

            public VectorTile.Tile.Layer.Builder setNameBytes(ByteString value) {
               if (value == null) {
                  throw new NullPointerException();
               } else {
                  this.bitField0_ |= 2;
                  this.name_ = value;
                  this.onChanged();
                  return this;
               }
            }

            private void ensureFeaturesIsMutable() {
               if ((this.bitField0_ & 4) != 4) {
                  this.features_ = new ArrayList<>(this.features_);
                  this.bitField0_ |= 4;
               }
            }

            @Override
            public List<VectorTile.Tile.Feature> getFeaturesList() {
               return this.featuresBuilder_ == null ? Collections.unmodifiableList(this.features_) : this.featuresBuilder_.getMessageList();
            }

            @Override
            public int getFeaturesCount() {
               return this.featuresBuilder_ == null ? this.features_.size() : this.featuresBuilder_.getCount();
            }

            @Override
            public VectorTile.Tile.Feature getFeatures(int index) {
               return this.featuresBuilder_ == null ? this.features_.get(index) : this.featuresBuilder_.getMessage(index);
            }

            public VectorTile.Tile.Layer.Builder setFeatures(int index, VectorTile.Tile.Feature value) {
               if (this.featuresBuilder_ == null) {
                  if (value == null) {
                     throw new NullPointerException();
                  }

                  this.ensureFeaturesIsMutable();
                  this.features_.set(index, value);
                  this.onChanged();
               } else {
                  this.featuresBuilder_.setMessage(index, value);
               }

               return this;
            }

            public VectorTile.Tile.Layer.Builder setFeatures(int index, VectorTile.Tile.Feature.Builder builderForValue) {
               if (this.featuresBuilder_ == null) {
                  this.ensureFeaturesIsMutable();
                  this.features_.set(index, builderForValue.build());
                  this.onChanged();
               } else {
                  this.featuresBuilder_.setMessage(index, builderForValue.build());
               }

               return this;
            }

            public VectorTile.Tile.Layer.Builder addFeatures(VectorTile.Tile.Feature value) {
               if (this.featuresBuilder_ == null) {
                  if (value == null) {
                     throw new NullPointerException();
                  }

                  this.ensureFeaturesIsMutable();
                  this.features_.add(value);
                  this.onChanged();
               } else {
                  this.featuresBuilder_.addMessage(value);
               }

               return this;
            }

            public VectorTile.Tile.Layer.Builder addFeatures(int index, VectorTile.Tile.Feature value) {
               if (this.featuresBuilder_ == null) {
                  if (value == null) {
                     throw new NullPointerException();
                  }

                  this.ensureFeaturesIsMutable();
                  this.features_.add(index, value);
                  this.onChanged();
               } else {
                  this.featuresBuilder_.addMessage(index, value);
               }

               return this;
            }

            public VectorTile.Tile.Layer.Builder addFeatures(VectorTile.Tile.Feature.Builder builderForValue) {
               if (this.featuresBuilder_ == null) {
                  this.ensureFeaturesIsMutable();
                  this.features_.add(builderForValue.build());
                  this.onChanged();
               } else {
                  this.featuresBuilder_.addMessage(builderForValue.build());
               }

               return this;
            }

            public VectorTile.Tile.Layer.Builder addFeatures(int index, VectorTile.Tile.Feature.Builder builderForValue) {
               if (this.featuresBuilder_ == null) {
                  this.ensureFeaturesIsMutable();
                  this.features_.add(index, builderForValue.build());
                  this.onChanged();
               } else {
                  this.featuresBuilder_.addMessage(index, builderForValue.build());
               }

               return this;
            }

            public VectorTile.Tile.Layer.Builder addAllFeatures(Iterable<? extends VectorTile.Tile.Feature> values) {
               if (this.featuresBuilder_ == null) {
                  this.ensureFeaturesIsMutable();
                  GeneratedMessage.ExtendableBuilder.addAll(values, this.features_);
                  this.onChanged();
               } else {
                  this.featuresBuilder_.addAllMessages(values);
               }

               return this;
            }

            public VectorTile.Tile.Layer.Builder clearFeatures() {
               if (this.featuresBuilder_ == null) {
                  this.features_ = Collections.emptyList();
                  this.bitField0_ &= -5;
                  this.onChanged();
               } else {
                  this.featuresBuilder_.clear();
               }

               return this;
            }

            public VectorTile.Tile.Layer.Builder removeFeatures(int index) {
               if (this.featuresBuilder_ == null) {
                  this.ensureFeaturesIsMutable();
                  this.features_.remove(index);
                  this.onChanged();
               } else {
                  this.featuresBuilder_.remove(index);
               }

               return this;
            }

            public VectorTile.Tile.Feature.Builder getFeaturesBuilder(int index) {
               return this.getFeaturesFieldBuilder().getBuilder(index);
            }

            @Override
            public VectorTile.Tile.FeatureOrBuilder getFeaturesOrBuilder(int index) {
               return this.featuresBuilder_ == null ? this.features_.get(index) : this.featuresBuilder_.getMessageOrBuilder(index);
            }

            @Override
            public List<? extends VectorTile.Tile.FeatureOrBuilder> getFeaturesOrBuilderList() {
               return this.featuresBuilder_ != null ? this.featuresBuilder_.getMessageOrBuilderList() : Collections.unmodifiableList(this.features_);
            }

            public VectorTile.Tile.Feature.Builder addFeaturesBuilder() {
               return this.getFeaturesFieldBuilder().addBuilder(VectorTile.Tile.Feature.getDefaultInstance());
            }

            public VectorTile.Tile.Feature.Builder addFeaturesBuilder(int index) {
               return this.getFeaturesFieldBuilder().addBuilder(index, VectorTile.Tile.Feature.getDefaultInstance());
            }

            public List<VectorTile.Tile.Feature.Builder> getFeaturesBuilderList() {
               return this.getFeaturesFieldBuilder().getBuilderList();
            }

            private RepeatedFieldBuilder<VectorTile.Tile.Feature, VectorTile.Tile.Feature.Builder, VectorTile.Tile.FeatureOrBuilder> getFeaturesFieldBuilder() {
               if (this.featuresBuilder_ == null) {
                  this.featuresBuilder_ = new RepeatedFieldBuilder<>(this.features_, (this.bitField0_ & 4) == 4, this.getParentForChildren(), this.isClean());
                  this.features_ = null;
               }

               return this.featuresBuilder_;
            }

            private void ensureKeysIsMutable() {
               if ((this.bitField0_ & 8) != 8) {
                  this.keys_ = new LazyStringArrayList(this.keys_);
                  this.bitField0_ |= 8;
               }
            }

            @Override
            public List<String> getKeysList() {
               return Collections.unmodifiableList(this.keys_);
            }

            @Override
            public int getKeysCount() {
               return this.keys_.size();
            }

            @Override
            public String getKeys(int index) {
               return this.keys_.get(index);
            }

            @Override
            public ByteString getKeysBytes(int index) {
               return this.keys_.getByteString(index);
            }

            public VectorTile.Tile.Layer.Builder setKeys(int index, String value) {
               if (value == null) {
                  throw new NullPointerException();
               } else {
                  this.ensureKeysIsMutable();
                  this.keys_.set(index, value);
                  this.onChanged();
                  return this;
               }
            }

            public VectorTile.Tile.Layer.Builder addKeys(String value) {
               if (value == null) {
                  throw new NullPointerException();
               } else {
                  this.ensureKeysIsMutable();
                  this.keys_.add(value);
                  this.onChanged();
                  return this;
               }
            }

            public VectorTile.Tile.Layer.Builder addAllKeys(Iterable<String> values) {
               this.ensureKeysIsMutable();
               GeneratedMessage.ExtendableBuilder.addAll(values, this.keys_);
               this.onChanged();
               return this;
            }

            public VectorTile.Tile.Layer.Builder clearKeys() {
               this.keys_ = LazyStringArrayList.EMPTY;
               this.bitField0_ &= -9;
               this.onChanged();
               return this;
            }

            public VectorTile.Tile.Layer.Builder addKeysBytes(ByteString value) {
               if (value == null) {
                  throw new NullPointerException();
               } else {
                  this.ensureKeysIsMutable();
                  this.keys_.add(value);
                  this.onChanged();
                  return this;
               }
            }

            private void ensureValuesIsMutable() {
               if ((this.bitField0_ & 16) != 16) {
                  this.values_ = new ArrayList<>(this.values_);
                  this.bitField0_ |= 16;
               }
            }

            @Override
            public List<VectorTile.Tile.Value> getValuesList() {
               return this.valuesBuilder_ == null ? Collections.unmodifiableList(this.values_) : this.valuesBuilder_.getMessageList();
            }

            @Override
            public int getValuesCount() {
               return this.valuesBuilder_ == null ? this.values_.size() : this.valuesBuilder_.getCount();
            }

            @Override
            public VectorTile.Tile.Value getValues(int index) {
               return this.valuesBuilder_ == null ? this.values_.get(index) : this.valuesBuilder_.getMessage(index);
            }

            public VectorTile.Tile.Layer.Builder setValues(int index, VectorTile.Tile.Value value) {
               if (this.valuesBuilder_ == null) {
                  if (value == null) {
                     throw new NullPointerException();
                  }

                  this.ensureValuesIsMutable();
                  this.values_.set(index, value);
                  this.onChanged();
               } else {
                  this.valuesBuilder_.setMessage(index, value);
               }

               return this;
            }

            public VectorTile.Tile.Layer.Builder setValues(int index, VectorTile.Tile.Value.Builder builderForValue) {
               if (this.valuesBuilder_ == null) {
                  this.ensureValuesIsMutable();
                  this.values_.set(index, builderForValue.build());
                  this.onChanged();
               } else {
                  this.valuesBuilder_.setMessage(index, builderForValue.build());
               }

               return this;
            }

            public VectorTile.Tile.Layer.Builder addValues(VectorTile.Tile.Value value) {
               if (this.valuesBuilder_ == null) {
                  if (value == null) {
                     throw new NullPointerException();
                  }

                  this.ensureValuesIsMutable();
                  this.values_.add(value);
                  this.onChanged();
               } else {
                  this.valuesBuilder_.addMessage(value);
               }

               return this;
            }

            public VectorTile.Tile.Layer.Builder addValues(int index, VectorTile.Tile.Value value) {
               if (this.valuesBuilder_ == null) {
                  if (value == null) {
                     throw new NullPointerException();
                  }

                  this.ensureValuesIsMutable();
                  this.values_.add(index, value);
                  this.onChanged();
               } else {
                  this.valuesBuilder_.addMessage(index, value);
               }

               return this;
            }

            public VectorTile.Tile.Layer.Builder addValues(VectorTile.Tile.Value.Builder builderForValue) {
               if (this.valuesBuilder_ == null) {
                  this.ensureValuesIsMutable();
                  this.values_.add(builderForValue.build());
                  this.onChanged();
               } else {
                  this.valuesBuilder_.addMessage(builderForValue.build());
               }

               return this;
            }

            public VectorTile.Tile.Layer.Builder addValues(int index, VectorTile.Tile.Value.Builder builderForValue) {
               if (this.valuesBuilder_ == null) {
                  this.ensureValuesIsMutable();
                  this.values_.add(index, builderForValue.build());
                  this.onChanged();
               } else {
                  this.valuesBuilder_.addMessage(index, builderForValue.build());
               }

               return this;
            }

            public VectorTile.Tile.Layer.Builder addAllValues(Iterable<? extends VectorTile.Tile.Value> values) {
               if (this.valuesBuilder_ == null) {
                  this.ensureValuesIsMutable();
                  GeneratedMessage.ExtendableBuilder.addAll(values, this.values_);
                  this.onChanged();
               } else {
                  this.valuesBuilder_.addAllMessages(values);
               }

               return this;
            }

            public VectorTile.Tile.Layer.Builder clearValues() {
               if (this.valuesBuilder_ == null) {
                  this.values_ = Collections.emptyList();
                  this.bitField0_ &= -17;
                  this.onChanged();
               } else {
                  this.valuesBuilder_.clear();
               }

               return this;
            }

            public VectorTile.Tile.Layer.Builder removeValues(int index) {
               if (this.valuesBuilder_ == null) {
                  this.ensureValuesIsMutable();
                  this.values_.remove(index);
                  this.onChanged();
               } else {
                  this.valuesBuilder_.remove(index);
               }

               return this;
            }

            public VectorTile.Tile.Value.Builder getValuesBuilder(int index) {
               return this.getValuesFieldBuilder().getBuilder(index);
            }

            @Override
            public VectorTile.Tile.ValueOrBuilder getValuesOrBuilder(int index) {
               return this.valuesBuilder_ == null ? this.values_.get(index) : this.valuesBuilder_.getMessageOrBuilder(index);
            }

            @Override
            public List<? extends VectorTile.Tile.ValueOrBuilder> getValuesOrBuilderList() {
               return this.valuesBuilder_ != null ? this.valuesBuilder_.getMessageOrBuilderList() : Collections.unmodifiableList(this.values_);
            }

            public VectorTile.Tile.Value.Builder addValuesBuilder() {
               return this.getValuesFieldBuilder().addBuilder(VectorTile.Tile.Value.getDefaultInstance());
            }

            public VectorTile.Tile.Value.Builder addValuesBuilder(int index) {
               return this.getValuesFieldBuilder().addBuilder(index, VectorTile.Tile.Value.getDefaultInstance());
            }

            public List<VectorTile.Tile.Value.Builder> getValuesBuilderList() {
               return this.getValuesFieldBuilder().getBuilderList();
            }

            private RepeatedFieldBuilder<VectorTile.Tile.Value, VectorTile.Tile.Value.Builder, VectorTile.Tile.ValueOrBuilder> getValuesFieldBuilder() {
               if (this.valuesBuilder_ == null) {
                  this.valuesBuilder_ = new RepeatedFieldBuilder<>(this.values_, (this.bitField0_ & 16) == 16, this.getParentForChildren(), this.isClean());
                  this.values_ = null;
               }

               return this.valuesBuilder_;
            }

            @Override
            public boolean hasExtent() {
               return (this.bitField0_ & 32) == 32;
            }

            @Override
            public int getExtent() {
               return this.extent_;
            }

            public VectorTile.Tile.Layer.Builder setExtent(int value) {
               this.bitField0_ |= 32;
               this.extent_ = value;
               this.onChanged();
               return this;
            }

            public VectorTile.Tile.Layer.Builder clearExtent() {
               this.bitField0_ &= -33;
               this.extent_ = 4096;
               this.onChanged();
               return this;
            }
         }
      }

      public interface LayerOrBuilder extends GeneratedMessage.ExtendableMessageOrBuilder<VectorTile.Tile.Layer> {
         boolean hasVersion();

         int getVersion();

         boolean hasName();

         String getName();

         ByteString getNameBytes();

         List<VectorTile.Tile.Feature> getFeaturesList();

         VectorTile.Tile.Feature getFeatures(int var1);

         int getFeaturesCount();

         List<? extends VectorTile.Tile.FeatureOrBuilder> getFeaturesOrBuilderList();

         VectorTile.Tile.FeatureOrBuilder getFeaturesOrBuilder(int var1);

         List<String> getKeysList();

         int getKeysCount();

         String getKeys(int var1);

         ByteString getKeysBytes(int var1);

         List<VectorTile.Tile.Value> getValuesList();

         VectorTile.Tile.Value getValues(int var1);

         int getValuesCount();

         List<? extends VectorTile.Tile.ValueOrBuilder> getValuesOrBuilderList();

         VectorTile.Tile.ValueOrBuilder getValuesOrBuilder(int var1);

         boolean hasExtent();

         int getExtent();
      }

      public static final class Value extends GeneratedMessage.ExtendableMessage<VectorTile.Tile.Value> implements VectorTile.Tile.ValueOrBuilder {
         private static final VectorTile.Tile.Value defaultInstance = new VectorTile.Tile.Value(true);
         private final UnknownFieldSet unknownFields;
         public static Parser<VectorTile.Tile.Value> PARSER = new AbstractParser<VectorTile.Tile.Value>() {
            public VectorTile.Tile.Value parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
               return new VectorTile.Tile.Value(input, extensionRegistry);
            }
         };
         private int bitField0_;
         public static final int STRING_VALUE_FIELD_NUMBER = 1;
         private Object stringValue_;
         public static final int FLOAT_VALUE_FIELD_NUMBER = 2;
         private float floatValue_;
         public static final int DOUBLE_VALUE_FIELD_NUMBER = 3;
         private double doubleValue_;
         public static final int INT_VALUE_FIELD_NUMBER = 4;
         private long intValue_;
         public static final int UINT_VALUE_FIELD_NUMBER = 5;
         private long uintValue_;
         public static final int SINT_VALUE_FIELD_NUMBER = 6;
         private long sintValue_;
         public static final int BOOL_VALUE_FIELD_NUMBER = 7;
         private boolean boolValue_;
         private byte memoizedIsInitialized = -1;
         private int memoizedSerializedSize = -1;
         private static final long serialVersionUID = 0L;

         private Value(GeneratedMessage.ExtendableBuilder<VectorTile.Tile.Value, ?> builder) {
            super(builder);
            this.unknownFields = builder.getUnknownFields();
         }

         private Value(boolean noInit) {
            this.unknownFields = UnknownFieldSet.getDefaultInstance();
         }

         public static VectorTile.Tile.Value getDefaultInstance() {
            return defaultInstance;
         }

         public VectorTile.Tile.Value getDefaultInstanceForType() {
            return defaultInstance;
         }

         @Override
         public final UnknownFieldSet getUnknownFields() {
            return this.unknownFields;
         }

         private Value(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            this.initFields();
            int mutable_bitField0_ = 0;
            UnknownFieldSet.Builder unknownFields = UnknownFieldSet.newBuilder();

            try {
               boolean done = false;

               while(!done) {
                  int tag = input.readTag();
                  switch(tag) {
                     case 0:
                        done = true;
                        break;
                     case 10:
                        this.bitField0_ |= 1;
                        this.stringValue_ = input.readBytes();
                        break;
                     case 21:
                        this.bitField0_ |= 2;
                        this.floatValue_ = input.readFloat();
                        break;
                     case 25:
                        this.bitField0_ |= 4;
                        this.doubleValue_ = input.readDouble();
                        break;
                     case 32:
                        this.bitField0_ |= 8;
                        this.intValue_ = input.readInt64();
                        break;
                     case 40:
                        this.bitField0_ |= 16;
                        this.uintValue_ = input.readUInt64();
                        break;
                     case 48:
                        this.bitField0_ |= 32;
                        this.sintValue_ = input.readSInt64();
                        break;
                     case 56:
                        this.bitField0_ |= 64;
                        this.boolValue_ = input.readBool();
                        break;
                     default:
                        if (!this.parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                           done = true;
                        }
                  }
               }
            } catch (InvalidProtocolBufferException var11) {
               throw var11.setUnfinishedMessage(this);
            } catch (IOException var12) {
               throw new InvalidProtocolBufferException(var12.getMessage()).setUnfinishedMessage(this);
            } finally {
               this.unknownFields = unknownFields.build();
               this.makeExtensionsImmutable();
            }
         }

         public static final Descriptors.Descriptor getDescriptor() {
            return VectorTile.internal_static_OsmAnd_VectorTile_Tile_Value_descriptor;
         }

         @Override
         protected GeneratedMessage.FieldAccessorTable internalGetFieldAccessorTable() {
            return VectorTile.internal_static_OsmAnd_VectorTile_Tile_Value_fieldAccessorTable
               .ensureFieldAccessorsInitialized(VectorTile.Tile.Value.class, VectorTile.Tile.Value.Builder.class);
         }

         @Override
         public Parser<VectorTile.Tile.Value> getParserForType() {
            return PARSER;
         }

         @Override
         public boolean hasStringValue() {
            return (this.bitField0_ & 1) == 1;
         }

         @Override
         public String getStringValue() {
            Object ref = this.stringValue_;
            if (ref instanceof String) {
               return (String)ref;
            } else {
               ByteString bs = (ByteString)ref;
               String s = bs.toStringUtf8();
               if (bs.isValidUtf8()) {
                  this.stringValue_ = s;
               }

               return s;
            }
         }

         @Override
         public ByteString getStringValueBytes() {
            Object ref = this.stringValue_;
            if (ref instanceof String) {
               ByteString b = ByteString.copyFromUtf8((String)ref);
               this.stringValue_ = b;
               return b;
            } else {
               return (ByteString)ref;
            }
         }

         @Override
         public boolean hasFloatValue() {
            return (this.bitField0_ & 2) == 2;
         }

         @Override
         public float getFloatValue() {
            return this.floatValue_;
         }

         @Override
         public boolean hasDoubleValue() {
            return (this.bitField0_ & 4) == 4;
         }

         @Override
         public double getDoubleValue() {
            return this.doubleValue_;
         }

         @Override
         public boolean hasIntValue() {
            return (this.bitField0_ & 8) == 8;
         }

         @Override
         public long getIntValue() {
            return this.intValue_;
         }

         @Override
         public boolean hasUintValue() {
            return (this.bitField0_ & 16) == 16;
         }

         @Override
         public long getUintValue() {
            return this.uintValue_;
         }

         @Override
         public boolean hasSintValue() {
            return (this.bitField0_ & 32) == 32;
         }

         @Override
         public long getSintValue() {
            return this.sintValue_;
         }

         @Override
         public boolean hasBoolValue() {
            return (this.bitField0_ & 64) == 64;
         }

         @Override
         public boolean getBoolValue() {
            return this.boolValue_;
         }

         private void initFields() {
            this.stringValue_ = "";
            this.floatValue_ = 0.0F;
            this.doubleValue_ = 0.0;
            this.intValue_ = 0L;
            this.uintValue_ = 0L;
            this.sintValue_ = 0L;
            this.boolValue_ = false;
         }

         @Override
         public final boolean isInitialized() {
            byte isInitialized = this.memoizedIsInitialized;
            if (isInitialized != -1) {
               return isInitialized == 1;
            } else if (!this.extensionsAreInitialized()) {
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
            GeneratedMessage.ExtendableMessage<VectorTile.Tile.Value>.ExtensionWriter extensionWriter = this.newExtensionWriter();
            if ((this.bitField0_ & 1) == 1) {
               output.writeBytes(1, this.getStringValueBytes());
            }

            if ((this.bitField0_ & 2) == 2) {
               output.writeFloat(2, this.floatValue_);
            }

            if ((this.bitField0_ & 4) == 4) {
               output.writeDouble(3, this.doubleValue_);
            }

            if ((this.bitField0_ & 8) == 8) {
               output.writeInt64(4, this.intValue_);
            }

            if ((this.bitField0_ & 16) == 16) {
               output.writeUInt64(5, this.uintValue_);
            }

            if ((this.bitField0_ & 32) == 32) {
               output.writeSInt64(6, this.sintValue_);
            }

            if ((this.bitField0_ & 64) == 64) {
               output.writeBool(7, this.boolValue_);
            }

            extensionWriter.writeUntil(536870912, output);
            this.getUnknownFields().writeTo(output);
         }

         @Override
         public int getSerializedSize() {
            int size = this.memoizedSerializedSize;
            if (size != -1) {
               return size;
            } else {
               size = 0;
               if ((this.bitField0_ & 1) == 1) {
                  size += CodedOutputStream.computeBytesSize(1, this.getStringValueBytes());
               }

               if ((this.bitField0_ & 2) == 2) {
                  size += CodedOutputStream.computeFloatSize(2, this.floatValue_);
               }

               if ((this.bitField0_ & 4) == 4) {
                  size += CodedOutputStream.computeDoubleSize(3, this.doubleValue_);
               }

               if ((this.bitField0_ & 8) == 8) {
                  size += CodedOutputStream.computeInt64Size(4, this.intValue_);
               }

               if ((this.bitField0_ & 16) == 16) {
                  size += CodedOutputStream.computeUInt64Size(5, this.uintValue_);
               }

               if ((this.bitField0_ & 32) == 32) {
                  size += CodedOutputStream.computeSInt64Size(6, this.sintValue_);
               }

               if ((this.bitField0_ & 64) == 64) {
                  size += CodedOutputStream.computeBoolSize(7, this.boolValue_);
               }

               size += this.extensionsSerializedSize();
               size += this.getUnknownFields().getSerializedSize();
               this.memoizedSerializedSize = size;
               return size;
            }
         }

         @Override
         protected Object writeReplace() throws ObjectStreamException {
            return super.writeReplace();
         }

         public static VectorTile.Tile.Value parseFrom(ByteString data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
         }

         public static VectorTile.Tile.Value parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
         }

         public static VectorTile.Tile.Value parseFrom(byte[] data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
         }

         public static VectorTile.Tile.Value parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
         }

         public static VectorTile.Tile.Value parseFrom(InputStream input) throws IOException {
            return PARSER.parseFrom(input);
         }

         public static VectorTile.Tile.Value parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return PARSER.parseFrom(input, extensionRegistry);
         }

         public static VectorTile.Tile.Value parseDelimitedFrom(InputStream input) throws IOException {
            return PARSER.parseDelimitedFrom(input);
         }

         public static VectorTile.Tile.Value parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return PARSER.parseDelimitedFrom(input, extensionRegistry);
         }

         public static VectorTile.Tile.Value parseFrom(CodedInputStream input) throws IOException {
            return PARSER.parseFrom(input);
         }

         public static VectorTile.Tile.Value parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return PARSER.parseFrom(input, extensionRegistry);
         }

         public static VectorTile.Tile.Value.Builder newBuilder() {
            return VectorTile.Tile.Value.Builder.create();
         }

         public VectorTile.Tile.Value.Builder newBuilderForType() {
            return newBuilder();
         }

         public static VectorTile.Tile.Value.Builder newBuilder(VectorTile.Tile.Value prototype) {
            return newBuilder().mergeFrom(prototype);
         }

         public VectorTile.Tile.Value.Builder toBuilder() {
            return newBuilder(this);
         }

         protected VectorTile.Tile.Value.Builder newBuilderForType(GeneratedMessage.BuilderParent parent) {
            return new VectorTile.Tile.Value.Builder(parent);
         }

         static {
            defaultInstance.initFields();
         }

         public static final class Builder
            extends GeneratedMessage.ExtendableBuilder<VectorTile.Tile.Value, VectorTile.Tile.Value.Builder>
            implements VectorTile.Tile.ValueOrBuilder {
            private int bitField0_;
            private Object stringValue_ = "";
            private float floatValue_;
            private double doubleValue_;
            private long intValue_;
            private long uintValue_;
            private long sintValue_;
            private boolean boolValue_;

            public static final Descriptors.Descriptor getDescriptor() {
               return VectorTile.internal_static_OsmAnd_VectorTile_Tile_Value_descriptor;
            }

            @Override
            protected GeneratedMessage.FieldAccessorTable internalGetFieldAccessorTable() {
               return VectorTile.internal_static_OsmAnd_VectorTile_Tile_Value_fieldAccessorTable
                  .ensureFieldAccessorsInitialized(VectorTile.Tile.Value.class, VectorTile.Tile.Value.Builder.class);
            }

            private Builder() {
               this.maybeForceBuilderInitialization();
            }

            private Builder(GeneratedMessage.BuilderParent parent) {
               super(parent);
               this.maybeForceBuilderInitialization();
            }

            private void maybeForceBuilderInitialization() {
               if (VectorTile.Tile.Value.alwaysUseFieldBuilders) {
               }
            }

            private static VectorTile.Tile.Value.Builder create() {
               return new VectorTile.Tile.Value.Builder();
            }

            public VectorTile.Tile.Value.Builder clear() {
               super.clear();
               this.stringValue_ = "";
               this.bitField0_ &= -2;
               this.floatValue_ = 0.0F;
               this.bitField0_ &= -3;
               this.doubleValue_ = 0.0;
               this.bitField0_ &= -5;
               this.intValue_ = 0L;
               this.bitField0_ &= -9;
               this.uintValue_ = 0L;
               this.bitField0_ &= -17;
               this.sintValue_ = 0L;
               this.bitField0_ &= -33;
               this.boolValue_ = false;
               this.bitField0_ &= -65;
               return this;
            }

            public VectorTile.Tile.Value.Builder clone() {
               return create().mergeFrom(this.buildPartial());
            }

            @Override
            public Descriptors.Descriptor getDescriptorForType() {
               return VectorTile.internal_static_OsmAnd_VectorTile_Tile_Value_descriptor;
            }

            public VectorTile.Tile.Value getDefaultInstanceForType() {
               return VectorTile.Tile.Value.getDefaultInstance();
            }

            public VectorTile.Tile.Value build() {
               VectorTile.Tile.Value result = this.buildPartial();
               if (!result.isInitialized()) {
                  throw newUninitializedMessageException(result);
               } else {
                  return result;
               }
            }

            public VectorTile.Tile.Value buildPartial() {
               VectorTile.Tile.Value result = new VectorTile.Tile.Value(this);
               int from_bitField0_ = this.bitField0_;
               int to_bitField0_ = 0;
               if ((from_bitField0_ & 1) == 1) {
                  to_bitField0_ |= 1;
               }

               result.stringValue_ = this.stringValue_;
               if ((from_bitField0_ & 2) == 2) {
                  to_bitField0_ |= 2;
               }

               result.floatValue_ = this.floatValue_;
               if ((from_bitField0_ & 4) == 4) {
                  to_bitField0_ |= 4;
               }

               result.doubleValue_ = this.doubleValue_;
               if ((from_bitField0_ & 8) == 8) {
                  to_bitField0_ |= 8;
               }

               result.intValue_ = this.intValue_;
               if ((from_bitField0_ & 16) == 16) {
                  to_bitField0_ |= 16;
               }

               result.uintValue_ = this.uintValue_;
               if ((from_bitField0_ & 32) == 32) {
                  to_bitField0_ |= 32;
               }

               result.sintValue_ = this.sintValue_;
               if ((from_bitField0_ & 64) == 64) {
                  to_bitField0_ |= 64;
               }

               result.boolValue_ = this.boolValue_;
               result.bitField0_ = to_bitField0_;
               this.onBuilt();
               return result;
            }

            public VectorTile.Tile.Value.Builder mergeFrom(Message other) {
               if (other instanceof VectorTile.Tile.Value) {
                  return this.mergeFrom((VectorTile.Tile.Value)other);
               } else {
                  super.mergeFrom(other);
                  return this;
               }
            }

            public VectorTile.Tile.Value.Builder mergeFrom(VectorTile.Tile.Value other) {
               if (other == VectorTile.Tile.Value.getDefaultInstance()) {
                  return this;
               } else {
                  if (other.hasStringValue()) {
                     this.bitField0_ |= 1;
                     this.stringValue_ = other.stringValue_;
                     this.onChanged();
                  }

                  if (other.hasFloatValue()) {
                     this.setFloatValue(other.getFloatValue());
                  }

                  if (other.hasDoubleValue()) {
                     this.setDoubleValue(other.getDoubleValue());
                  }

                  if (other.hasIntValue()) {
                     this.setIntValue(other.getIntValue());
                  }

                  if (other.hasUintValue()) {
                     this.setUintValue(other.getUintValue());
                  }

                  if (other.hasSintValue()) {
                     this.setSintValue(other.getSintValue());
                  }

                  if (other.hasBoolValue()) {
                     this.setBoolValue(other.getBoolValue());
                  }

                  this.mergeExtensionFields(other);
                  this.mergeUnknownFields(other.getUnknownFields());
                  return this;
               }
            }

            @Override
            public final boolean isInitialized() {
               return this.extensionsAreInitialized();
            }

            public VectorTile.Tile.Value.Builder mergeFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
               VectorTile.Tile.Value parsedMessage = null;

               try {
                  parsedMessage = VectorTile.Tile.Value.PARSER.parsePartialFrom(input, extensionRegistry);
               } catch (InvalidProtocolBufferException var8) {
                  parsedMessage = (VectorTile.Tile.Value)var8.getUnfinishedMessage();
                  throw var8;
               } finally {
                  if (parsedMessage != null) {
                     this.mergeFrom(parsedMessage);
                  }
               }

               return this;
            }

            @Override
            public boolean hasStringValue() {
               return (this.bitField0_ & 1) == 1;
            }

            @Override
            public String getStringValue() {
               Object ref = this.stringValue_;
               if (!(ref instanceof String)) {
                  String s = ((ByteString)ref).toStringUtf8();
                  this.stringValue_ = s;
                  return s;
               } else {
                  return (String)ref;
               }
            }

            @Override
            public ByteString getStringValueBytes() {
               Object ref = this.stringValue_;
               if (ref instanceof String) {
                  ByteString b = ByteString.copyFromUtf8((String)ref);
                  this.stringValue_ = b;
                  return b;
               } else {
                  return (ByteString)ref;
               }
            }

            public VectorTile.Tile.Value.Builder setStringValue(String value) {
               if (value == null) {
                  throw new NullPointerException();
               } else {
                  this.bitField0_ |= 1;
                  this.stringValue_ = value;
                  this.onChanged();
                  return this;
               }
            }

            public VectorTile.Tile.Value.Builder clearStringValue() {
               this.bitField0_ &= -2;
               this.stringValue_ = VectorTile.Tile.Value.getDefaultInstance().getStringValue();
               this.onChanged();
               return this;
            }

            public VectorTile.Tile.Value.Builder setStringValueBytes(ByteString value) {
               if (value == null) {
                  throw new NullPointerException();
               } else {
                  this.bitField0_ |= 1;
                  this.stringValue_ = value;
                  this.onChanged();
                  return this;
               }
            }

            @Override
            public boolean hasFloatValue() {
               return (this.bitField0_ & 2) == 2;
            }

            @Override
            public float getFloatValue() {
               return this.floatValue_;
            }

            public VectorTile.Tile.Value.Builder setFloatValue(float value) {
               this.bitField0_ |= 2;
               this.floatValue_ = value;
               this.onChanged();
               return this;
            }

            public VectorTile.Tile.Value.Builder clearFloatValue() {
               this.bitField0_ &= -3;
               this.floatValue_ = 0.0F;
               this.onChanged();
               return this;
            }

            @Override
            public boolean hasDoubleValue() {
               return (this.bitField0_ & 4) == 4;
            }

            @Override
            public double getDoubleValue() {
               return this.doubleValue_;
            }

            public VectorTile.Tile.Value.Builder setDoubleValue(double value) {
               this.bitField0_ |= 4;
               this.doubleValue_ = value;
               this.onChanged();
               return this;
            }

            public VectorTile.Tile.Value.Builder clearDoubleValue() {
               this.bitField0_ &= -5;
               this.doubleValue_ = 0.0;
               this.onChanged();
               return this;
            }

            @Override
            public boolean hasIntValue() {
               return (this.bitField0_ & 8) == 8;
            }

            @Override
            public long getIntValue() {
               return this.intValue_;
            }

            public VectorTile.Tile.Value.Builder setIntValue(long value) {
               this.bitField0_ |= 8;
               this.intValue_ = value;
               this.onChanged();
               return this;
            }

            public VectorTile.Tile.Value.Builder clearIntValue() {
               this.bitField0_ &= -9;
               this.intValue_ = 0L;
               this.onChanged();
               return this;
            }

            @Override
            public boolean hasUintValue() {
               return (this.bitField0_ & 16) == 16;
            }

            @Override
            public long getUintValue() {
               return this.uintValue_;
            }

            public VectorTile.Tile.Value.Builder setUintValue(long value) {
               this.bitField0_ |= 16;
               this.uintValue_ = value;
               this.onChanged();
               return this;
            }

            public VectorTile.Tile.Value.Builder clearUintValue() {
               this.bitField0_ &= -17;
               this.uintValue_ = 0L;
               this.onChanged();
               return this;
            }

            @Override
            public boolean hasSintValue() {
               return (this.bitField0_ & 32) == 32;
            }

            @Override
            public long getSintValue() {
               return this.sintValue_;
            }

            public VectorTile.Tile.Value.Builder setSintValue(long value) {
               this.bitField0_ |= 32;
               this.sintValue_ = value;
               this.onChanged();
               return this;
            }

            public VectorTile.Tile.Value.Builder clearSintValue() {
               this.bitField0_ &= -33;
               this.sintValue_ = 0L;
               this.onChanged();
               return this;
            }

            @Override
            public boolean hasBoolValue() {
               return (this.bitField0_ & 64) == 64;
            }

            @Override
            public boolean getBoolValue() {
               return this.boolValue_;
            }

            public VectorTile.Tile.Value.Builder setBoolValue(boolean value) {
               this.bitField0_ |= 64;
               this.boolValue_ = value;
               this.onChanged();
               return this;
            }

            public VectorTile.Tile.Value.Builder clearBoolValue() {
               this.bitField0_ &= -65;
               this.boolValue_ = false;
               this.onChanged();
               return this;
            }
         }
      }

      public interface ValueOrBuilder extends GeneratedMessage.ExtendableMessageOrBuilder<VectorTile.Tile.Value> {
         boolean hasStringValue();

         String getStringValue();

         ByteString getStringValueBytes();

         boolean hasFloatValue();

         float getFloatValue();

         boolean hasDoubleValue();

         double getDoubleValue();

         boolean hasIntValue();

         long getIntValue();

         boolean hasUintValue();

         long getUintValue();

         boolean hasSintValue();

         long getSintValue();

         boolean hasBoolValue();

         boolean getBoolValue();
      }
   }

   public interface TileOrBuilder extends GeneratedMessage.ExtendableMessageOrBuilder<VectorTile.Tile> {
      List<VectorTile.Tile.Layer> getLayersList();

      VectorTile.Tile.Layer getLayers(int var1);

      int getLayersCount();

      List<? extends VectorTile.Tile.LayerOrBuilder> getLayersOrBuilderList();

      VectorTile.Tile.LayerOrBuilder getLayersOrBuilder(int var1);
   }
}
