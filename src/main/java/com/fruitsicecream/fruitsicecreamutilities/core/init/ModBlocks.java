package com.fruitsicecream.fruitsicecreamutilities.core.init;

import com.fruitsicecream.fruitsicecreamutilities.FruitsIceCreamUtilities;
import com.fruitsicecream.fruitsicecreamutilities.core.config.ModConfig;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.DiamondPipeBlock;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.ExperienceCollectorBlock;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.ExperienceCoreBlock;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.GoldPipeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, FruitsIceCreamUtilities.MOD_ID);

    // Experience Core Blocks - Ahora usan configuración
    public static final RegistryObject<Block> EXPERIENCE_CORE_MK1 = BLOCKS.register("exp_core_mk1",
            () -> new ExperienceCoreBlock(
                    BlockBehaviour.Properties.of()
                            .strength(3.0f, 6.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops(),
                    1  // tier - Los valores se obtienen de la config
            ));

    public static final RegistryObject<Block> EXPERIENCE_CORE_MK2 = BLOCKS.register("exp_core_mk2",
            () -> new ExperienceCoreBlock(
                    BlockBehaviour.Properties.of()
                            .strength(3.5f, 8.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops(),
                    2
            ));

    public static final RegistryObject<Block> EXPERIENCE_CORE_MK3 = BLOCKS.register("exp_core_mk3",
            () -> new ExperienceCoreBlock(
                    BlockBehaviour.Properties.of()
                            .strength(4.0f, 10.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops(),
                    3
            ));

    public static final RegistryObject<Block> EXPERIENCE_CORE_MK4 = BLOCKS.register("exp_core_mk4",
            () -> new ExperienceCoreBlock(
                    BlockBehaviour.Properties.of()
                            .strength(4.5f, 12.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops(),
                    4
            ));

    public static final RegistryObject<Block> EXPERIENCE_CORE_MK5 = BLOCKS.register("exp_core_mk5",
            () -> new ExperienceCoreBlock(
                    BlockBehaviour.Properties.of()
                            .strength(5.0f, 15.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops(),
                    5
            ));

    // Experience Collector Blocks - Ahora usan configuración
    public static final RegistryObject<Block> BASIC_EXPERIENCE_COLLECTOR = BLOCKS.register("basic_exp_collector",
            () -> new ExperienceCollectorBlock(
                    BlockBehaviour.Properties.of()
                            .strength(3.0f, 6.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops(),
                    1  // tier - Los valores se obtienen de la config
            ));

    public static final RegistryObject<Block> ADVANCED_EXPERIENCE_COLLECTOR = BLOCKS.register("advanced_exp_collector",
            () -> new ExperienceCollectorBlock(
                    BlockBehaviour.Properties.of()
                            .strength(4.5f, 12.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops(),
                    2
            ));

    public static final RegistryObject<Block> GOLD_PIPE = BLOCKS.register("gold_pipe",
            () -> new GoldPipeBlock());

    public static final RegistryObject<Block> DIAMOND_PIPE = BLOCKS.register("diamond_pipe",
            () -> new DiamondPipeBlock());

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}