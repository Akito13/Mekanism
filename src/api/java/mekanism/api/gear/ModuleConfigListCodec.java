package mekanism.api.gear;

import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import mekanism.api.gear.config.ModuleConfig;

//TODO - 1.20.5: Docs?
//Based off of ListCodec
class ModuleConfigListCodec implements Codec<List<ModuleConfig<?>>> {

    private final List<Codec<ModuleConfig<?>>> codecs;

    ModuleConfigListCodec(List<Codec<ModuleConfig<?>>> codecs) {
        this.codecs = codecs;
    }

    @Override
    public <T> DataResult<T> encode(List<ModuleConfig<?>> input, DynamicOps<T> ops, T prefix) {
        int size = input.size();
        if (codecs.size() != size) {
            return DataResult.error(() -> "List length: " + size + ", does not match the number of codecs we have: " + codecs.size());
        }
        ListBuilder<T> builder = ops.listBuilder();
        for (int i = 0; i < size; i++) {
            builder.add(codecs.get(i).encodeStart(ops, input.get(i)));
        }
        return builder.build(prefix);
    }

    @Override
    public <T> DataResult<Pair<List<ModuleConfig<?>>, T>> decode(DynamicOps<T> ops, T input) {
        return ops.getList(input).setLifecycle(Lifecycle.stable()).flatMap(stream -> {
            final DecoderState<T> decoder = new DecoderState<>(ops);
            stream.accept(decoder::accept);
            return decoder.build();
        });
    }

    @Override
    public String toString() {
        return "ModuleConfigListCodec[" + codecs.stream().map(Object::toString).collect(Collectors.joining(", ")) + "]";
    }

    private class DecoderState<T> {

        private static final DataResult<Unit> INITIAL_RESULT = DataResult.success(Unit.INSTANCE, Lifecycle.stable());

        private final DynamicOps<T> ops;
        private final List<ModuleConfig<?>> elements = new ArrayList<>();
        private final Stream.Builder<T> failed = Stream.builder();
        private DataResult<Unit> result = INITIAL_RESULT;
        private int index;

        private DecoderState(final DynamicOps<T> ops) {
            this.ops = ops;
        }

        public void accept(final T value) {
            if (index >= codecs.size()) {
                failed.add(value);
                return;
            }
            Codec<ModuleConfig<?>> elementCodec = codecs.get(index++);
            final DataResult<Pair<ModuleConfig<?>, T>> elementResult = elementCodec.decode(ops, value);
            elementResult.error().ifPresent(error -> failed.add(value));
            elementResult.resultOrPartial().ifPresent(pair -> elements.add(pair.getFirst()));
            result = result.apply2stable((result, element) -> result, elementResult);
        }

        public DataResult<Pair<List<ModuleConfig<?>>, T>> build() {
            final T errors = ops.createList(failed.build());
            final Pair<List<ModuleConfig<?>>, T> pair = Pair.of(List.copyOf(elements), errors);
            return result.map(ignored -> pair).setPartial(pair);
        }
    }
}