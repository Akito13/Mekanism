package mekanism.api;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder.Implementation;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import mekanism.api.annotations.NothingNullByDefault;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

//TODO - 1.21: Update the wiki docs to fix the syntax
@NothingNullByDefault
public class SerializerHelper {

    private SerializerHelper() {
    }

    /**
     * Long Codec which accepts a number >= 0
     */
    public static final Codec<Long> POSITIVE_LONG_CODEC = Util.make(() -> {
        final Function<Long, DataResult<Long>> checker = Codec.checkRange(0L, Long.MAX_VALUE);
        return Codec.LONG.flatXmap(checker, checker);
    });

    /**
     * Long Codec which accepts a number > 0
     */
    public static final Codec<Long> POSITIVE_NONZERO_LONG_CODEC = Util.make(() -> {
        final Function<Long, DataResult<Long>> checker = Codec.checkRange(1L, Long.MAX_VALUE);
        return Codec.LONG.flatXmap(checker, checker);
    });

    @Deprecated(since = "10.6.6", forRemoval = true)//TODO - 1.22: Remove
    private static final Codec<Long> LEGACY_CODEC_FLOATING_LONG = new PrimitiveCodec<>() {
        @Override
        public <T> DataResult<Long> read(DynamicOps<T> ops, T input) {
            return ops.getStringValue(input).flatMap(number -> {
                try {
                    long value;
                    int index = number.indexOf('.');
                    if (index == -1) {
                        value = Long.parseUnsignedLong(number);
                    } else {
                        value = Long.parseUnsignedLong(number.substring(0, index));
                    }
                    if (value < 0) {
                        //Clamp unsigned to positive.
                        value = Long.MAX_VALUE;
                    }
                    if (value == 0) {
                        //If we are at zero, see if we should ceil the decimal
                        if (index == -1) {
                            return DataResult.success(0L);
                        }
                        String decimalAsString = number.substring(index + 1);
                        int numberDigits = decimalAsString.length();
                        if (numberDigits < 4) {
                            //We need to pad it on the right with zeros
                            decimalAsString += "0".repeat(Math.max(0, 4 - numberDigits));
                        } else if (numberDigits > 4) {
                            //We need to trim it to make sure it will be in range of a short
                            decimalAsString = decimalAsString.substring(0, 4);
                        }
                        if (Short.parseShort(decimalAsString) > 0) {
                            return DataResult.success(1L);
                        }
                    }
                    return DataResult.success(value);
                } catch (NumberFormatException e) {
                    return DataResult.error(e::getMessage);
                }
            });
        }

        @Override
        public <T> T write(DynamicOps<T> ops, Long value) {
            return ops.createString(value.toString());
        }

        @Override
        public String toString() {
            return "LegacyFloatingLong";
        }
    };

    /**
     * Long Codec which accepts a number >= 0
     *
     * @since 10.6.6
     * @deprecated Prefer {@link #POSITIVE_LONG_CODEC}. This field just exists for people who want to be able to load legacy data that was stored as a FloatingLong/
     */
    @Deprecated(since = "10.6.6", forRemoval = true)//TODO - 1.22: Remove
    //Note: We use vanilla's withAlternative instead of Neo's as we always want to encode with the non legacy codec
    public static final Codec<Long> POSITIVE_LONG_CODEC_LEGACY = Codec.withAlternative(POSITIVE_LONG_CODEC, LEGACY_CODEC_FLOATING_LONG);

    /**
     * Long Codec which accepts a number > 0
     *
     * @since 10.6.6
     * @deprecated Prefer {@link #POSITIVE_LONG_CODEC}. This field just exists for people who want to be able to load legacy data that was stored as a FloatingLong/
     */
    @Deprecated(since = "10.6.6", forRemoval = true)//TODO - 1.22: Remove
    //Note: We use vanilla's withAlternative instead of Neo's as we always want to encode with the non legacy codec
    public static final Codec<Long> POSITIVE_NONZERO_LONG_CODEC_LEGACY = Codec.withAlternative(POSITIVE_NONZERO_LONG_CODEC,
          LEGACY_CODEC_FLOATING_LONG.validate(val -> val == 0 ? DataResult.error(() -> "Value must be greater than zero") : DataResult.success(val))
    );

    /**
     * Custom codec to allow serializing an item stack without the upper bounds.
     *
     * @since 10.6.1
     */
    public static final Codec<ItemStack> OVERSIZED_ITEM_CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance -> instance.group(
          ItemStack.ITEM_NON_AIR_CODEC.fieldOf(SerializationConstants.ID).forGetter(ItemStack::getItemHolder),
          ExtraCodecs.POSITIVE_INT.fieldOf(SerializationConstants.COUNT).orElse(1).forGetter(ItemStack::getCount),
          DataComponentPatch.CODEC.optionalFieldOf(SerializationConstants.COMPONENTS, DataComponentPatch.EMPTY).forGetter(ItemStack::getComponentsPatch)
    ).apply(instance, ItemStack::new)));

    /**
     * Custom codec to allow serializing an item stack without the upper bounds. Allows empty items
     *
     * @since 10.6.4
     */
    public static final Codec<ItemStack> OVERSIZED_ITEM_OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(OVERSIZED_ITEM_CODEC)
          .xmap(optional -> optional.orElse(ItemStack.EMPTY), stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack));

    /**
     * Helper similar to {@link ItemStack#save(Provider)} but with support for oversized stacks.
     *
     * @since 10.6.1
     */
    public static Tag saveOversized(HolderLookup.Provider registryAccess, ItemStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Cannot encode empty ItemStack");
        }
        return OVERSIZED_ITEM_CODEC.encodeStart(registryAccess.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
    }

    /**
     * Helper similar to {@link ItemStack#parse(Provider, Tag)} but with support for oversized stacks.
     *
     * @since 10.6.1
     */
    public static Optional<ItemStack> parseOversized(HolderLookup.Provider pLookupProvider, Tag pTag) {
        return OVERSIZED_ITEM_CODEC.parse(pLookupProvider.createSerializationContext(NbtOps.INSTANCE), pTag)
              .resultOrPartial(p_330102_ -> MekanismAPI.logger.error("Tried to load invalid item: '{}'", p_330102_));
    }

    /**
     * Helper similar to {@link ItemStack#parseOptional(Provider, CompoundTag)} but with support for oversized stacks.
     *
     * @since 10.6.1
     */
    public static ItemStack parseOversizedOptional(HolderLookup.Provider pLookupProvider, CompoundTag tag) {
        return tag.isEmpty() ? ItemStack.EMPTY : parseOversized(pLookupProvider, tag).orElse(ItemStack.EMPTY);
    }

    /**
     * Generate a RecordCodecBuilder which is required only if the 'primary' is present. If this field is present, it will be returned regardless. Does not eat errors
     *
     * @param primaryField    the field which determines the required-ness. MUST be an Optional
     * @param dependentCodec  the codec for <strong>this</strong> field
     * @param dependentGetter the getter for this field (what you'd use on {@link MapCodec#forGetter(Function)})
     * @param <SOURCE>        the resulting type that both fields exist on
     * @param <THIS_TYPE>     the value type of this dependent field
     *
     * @return a RecordCodecBuilder which contains the resulting logic - use in side a `group()`
     */
    @NotNull
    public static <SOURCE, THIS_TYPE> RecordCodecBuilder<SOURCE, Optional<THIS_TYPE>> dependentOptionality(RecordCodecBuilder<SOURCE, ? extends Optional<?>> primaryField,
          MapCodec<Optional<THIS_TYPE>> dependentCodec, Function<SOURCE, Optional<THIS_TYPE>> dependentGetter) {
        Implementation<Optional<THIS_TYPE>> dependentRequired = new Implementation<>() {
            @Override
            public <T> DataResult<Optional<THIS_TYPE>> decode(DynamicOps<T> ops, MapLike<T> input) {
                DataResult<Optional<THIS_TYPE>> thisField = dependentCodec.decode(ops, input);

                //if the unboxed optional has a value, return this field's value.
                //if it had an error, return that
                if (thisField.error().isPresent() || thisField.result().orElse(Optional.empty()).isPresent()) {
                    return thisField;
                }

                //thisField must not be empty
                return DataResult.error(() -> "Missing value");
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return dependentCodec.keys(ops);
            }
        };
        return primaryField.dependent(dependentGetter, dependentCodec, primaryValue -> primaryValue.isEmpty() ? dependentCodec : dependentRequired);
    }

    /**
     * Generate a RecordCodecBuilder which is REQUIRED only if the 'other' is NOT present. When the other field is present, this one is OPTIONAL. Does not eat errors.
     *
     * @param otherField      the field which determines the required-ness. MUST be an Optional
     * @param dependentCodec  the codec for <strong>this</strong> field
     * @param dependentGetter the getter for this field (what you'd use on {@link MapCodec#forGetter(Function)})
     * @param <SOURCE>        the resulting type that both fields exist on
     * @param <THIS_TYPE>     the value type of this dependent field
     *
     * @return a RecordCodecBuilder which contains the resulting logic - use in side a `group()`
     */
    @NotNull
    public static <SOURCE, THIS_TYPE> RecordCodecBuilder<SOURCE, Optional<THIS_TYPE>> oneRequired(RecordCodecBuilder<SOURCE, ? extends Optional<?>> otherField,
          MapCodec<Optional<THIS_TYPE>> dependentCodec, Function<SOURCE, Optional<THIS_TYPE>> dependentGetter) {
        Implementation<Optional<THIS_TYPE>> dependentRequired = new Implementation<>() {
            @Override
            public <T> DataResult<Optional<THIS_TYPE>> decode(DynamicOps<T> ops, MapLike<T> input) {
                DataResult<Optional<THIS_TYPE>> thisField = dependentCodec.decode(ops, input);

                //if the unboxed optional has a value, return this field's value.
                //if it had an error, return that
                if (thisField.error().isPresent() || thisField.result().orElse(Optional.empty()).isPresent()) {
                    return thisField;
                }

                //the primary is empty, and this is also empty
                return DataResult.error(() -> getFieldNames(dependentCodec) + " is required");
            }

            private static <THIS_TYPE> String getFieldNames(MapCodec<Optional<THIS_TYPE>> codec) {
                return codec.keys(JsonOps.INSTANCE).map(JsonElement::getAsString).collect(Collectors.joining());
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return dependentCodec.keys(ops);
            }
        };
        return otherField.dependent(dependentGetter, dependentCodec, primaryValue -> primaryValue.isPresent() ? dependentCodec : dependentRequired);
    }
}