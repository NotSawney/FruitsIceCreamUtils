package com.fruitsicecream.fruitsicecreamutilities.core.init;

import com.fruitsicecream.fruitsicecreamutilities.FruitsIceCreamUtilities;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.ExperienceCoreBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.Tags;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, FruitsIceCreamUtilities.MOD_ID);

    // Experience Core Blocks - Tier 1-3: Iron pickaxe required
    public static final RegistryObject<Block> EXPERIENCE_CORE_MK1 = BLOCKS.register("exp_core_mk1",
            () -> new ExperienceCoreBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 15)
                    .requiresCorrectToolForDrops(), // Requiere herramienta correcta
                    1, 360));

    public static final RegistryObject<Block> EXPERIENCE_CORE_MK2 = BLOCKS.register("exp_core_mk2",
            () -> new ExperienceCoreBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f, 8.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 15)
                    .requiresCorrectToolForDrops(),
                    2, 720));

    public static final RegistryObject<Block> EXPERIENCE_CORE_MK3 = BLOCKS.register("exp_core_mk3",
            () -> new ExperienceCoreBlock(BlockBehaviour.Properties.of()
                    .strength(4.0f, 10.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 15)
                    .requiresCorrectToolForDrops(),
                    3, 1440));

    // Experience Core Blocks - Tier 4-5: Diamond pickaxe required
    public static final RegistryObject<Block> EXPERIENCE_CORE_MK4 = BLOCKS.register("exp_core_mk4",
            () -> new ExperienceCoreBlock(BlockBehaviour.Properties.of()
                    .strength(4.5f, 12.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 15)
                    .requiresCorrectToolForDrops(),
                    4, 2880));

    public static final RegistryObject<Block> EXPERIENCE_CORE_MK5 = BLOCKS.register("exp_core_mk5",
            () -> new ExperienceCoreBlock(BlockBehaviour.Properties.of()
                    .strength(5.0f, 15.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 15)
                    .requiresCorrectToolForDrops(),
                    5, 5760));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}