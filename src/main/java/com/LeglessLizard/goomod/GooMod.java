package com.LeglessLizard.goomod;

import com.LeglessLizard.goomod.block.GooBlock;
import com.LeglessLizard.goomod.block.PhageDisruptorBlock;
import com.LeglessLizard.goomod.config.ModConfig;
import com.LeglessLizard.goomod.registry.ModBlockEntities;
import com.LeglessLizard.goomod.registry.ModBlocks;
import com.LeglessLizard.goomod.registry.ModCreativeTabs;
import com.LeglessLizard.goomod.registry.ModEntities;
import com.LeglessLizard.goomod.client.PrimedGooEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(GooMod.MOD_ID)
public class GooMod {
    public static final String MOD_ID = "goomod";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static boolean SPREADING_ENABLED = true;

    public GooMod(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);

        modContainer.registerConfig(Type.COMMON, ModConfig.SPEC);

        NeoForge.EVENT_BUS.addListener((ServerStartingEvent event) -> GooBlock.CONVERTED_COUNT.set(0));

        // Unified command registration
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> {
            // /goospread command
            event.getDispatcher().register(
                Commands.literal("goospread")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        SPREADING_ENABLED = !SPREADING_ENABLED;
                        ctx.getSource().sendSuccess(() -> Component.literal("Goo spreading: " + SPREADING_ENABLED), true);
                        return 1;
                    })
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> {
                            SPREADING_ENABLED = BoolArgumentType.getBool(ctx, "enabled");
                            ctx.getSource().sendSuccess(() -> Component.literal("Goo spreading: " + SPREADING_ENABLED), true);
                            return 1;
                        })
                    )
            );

            // /disrupt command with subcommands
            event.getDispatcher().register(
                Commands.literal("disrupt")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("toggle")
                        .executes(ctx -> {
                            PhageDisruptorBlock.toggleCommandDisrupt();
                            ctx.getSource().sendSuccess(() ->
                                Component.literal("Global disrupt: " + (PhageDisruptorBlock.GLOBAL_COMMAND_DISRUPT ? "ON" : "OFF")), true);
                            return 1;
                        })
                    )
                    .then(Commands.literal("once")
                        .executes(ctx -> {
                            PhageDisruptorBlock.disruptOnce(ctx.getSource().getLevel());
                            ctx.getSource().sendSuccess(() -> Component.literal("Phages purged once."), true);
                            return 1;
                        })
                    )
                    .then(Commands.literal("status")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() ->
                                Component.literal("Global disrupt: " + (PhageDisruptorBlock.GLOBAL_COMMAND_DISRUPT ? "ON" : "OFF")), true);
                            return 1;
                        })
                    )
                    .then(Commands.literal("count")
                        .executes(ctx -> {
                            int count = PhageDisruptorBlock.countDisruptors(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() ->
                                Component.literal("Disruptors in world: " + count), true);
                            return 1;
                        })
                    )
                    .then(Commands.literal("converge")
                        .executes(ctx -> {
                            int count = 0;
                            for (var lvl : ctx.getSource().getServer().getAllLevels()) {
                                if (!(lvl instanceof net.minecraft.server.level.ServerLevel serverLevel)) continue;
                                for (var corePos : com.LeglessLizard.goomod.block.UltimatePhageCoreBlock.ACTIVE_CORES) {
                                    if (!serverLevel.isLoaded(corePos)) continue;
                                    var be = serverLevel.getBlockEntity(corePos);
                                    if (be instanceof com.LeglessLizard.goomod.block.UltimatePhageCoreBlockEntity core) {
                                        if (!core.isReturning) {
                                            com.LeglessLizard.goomod.block.UltimatePhageCoreBlock
                                                .beginConvergence(serverLevel, corePos, core);
                                        }
                                        count++;
                                    }
                                }
                            }
                            final int c = count;
                            ctx.getSource().sendSuccess(() -> Component.literal("Slurping " + c + " core(s)."), true);
                            return 1;
                        })
                    )
                    .then(Commands.literal("locate")
                        .executes(ctx -> {
                            var positions = PhageDisruptorBlock.DISRUPTOR_POSITIONS;
                            if (positions.isEmpty()) {
                                ctx.getSource().sendSuccess(() -> Component.literal("No disruptors found."), false);
                            } else {
                                for (BlockPos pos : positions) {
                                    Component msg = Component.literal("Disruptor at ")
                                        .append(Component.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ())
                                        .withStyle(style -> style.withClickEvent(
                                            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + pos.getX() + " " + pos.getY() + " " + pos.getZ())
                                        )));
                                    ctx.getSource().sendSuccess(() -> msg, false);
                                }
                            }
                            return 1;
                        })
                    )
            );
        });

        // Disruptor tick handler
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre event) -> {
            for (ServerLevel level : event.getServer().getAllLevels()) {
                PhageDisruptorBlock.tickDisruptor(level);
            }
        });

        modEventBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(ModEntities.PRIMED_GOO_EFFECT.get(), PrimedGooEffectRenderer::new);
        });
    }
}