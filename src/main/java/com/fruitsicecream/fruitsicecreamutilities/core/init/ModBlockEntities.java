package com.fruitsicecream.fruitsicecreamutilities.core.init;

import com.fruitsicecream.fruitsicecreamutilities.FruitsIceCreamUtilities;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.ExperienceCollectorBlockEntity;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.ExperienceCoreBlockEntity;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.PipeBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, FruitsIceCreamUtilities.MOD_ID);

    public static final RegistryObject<BlockEntityType<ExperienceCoreBlockEntity>> EXPERIENCE_CORE_BE =
            BLOCK_ENTITIES.register("experience_core_be", () ->
                    BlockEntityType.Builder.of(ExperienceCoreBlockEntity::new,
                            ModBlocks.EXPERIENCE_CORE_MK1.get(),
                            ModBlocks.EXPERIENCE_CORE_MK2.get(),
                            ModBlocks.EXPERIENCE_CORE_MK3.get(),
                            ModBlocks.EXPERIENCE_CORE_MK4.get(),
                            ModBlocks.EXPERIENCE_CORE_MK5.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<ExperienceCollectorBlockEntity>> EXPERIENCE_COLLECTOR_BE =
            BLOCK_ENTITIES.register("experience_collector_be", () ->
                    BlockEntityType.Builder.of(ExperienceCollectorBlockEntity::new,
                            ModBlocks.BASIC_EXPERIENCE_COLLECTOR.get(),
                            ModBlocks.ADVANCED_EXPERIENCE_COLLECTOR.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<PipeBlockEntity>> PIPE_BE =
            BLOCK_ENTITIES.register("pipe_be", () ->
                    BlockEntityType.Builder.of(PipeBlockEntity::new,
                            ModBlocks.GOLD_PIPE.get(),
                            ModBlocks.DIAMOND_PIPE.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}