package com.nxst.tomeofbinding;

import com.mojang.logging.LogUtils;
import com.nxst.tomeofbinding.config.ModConfig;
import com.nxst.tomeofbinding.item.ScrollOfVisionItem;
import com.nxst.tomeofbinding.item.TomeItem;
import com.nxst.tomeofbinding.network.PlayerSelectPacket;
import com.nxst.tomeofbinding.network.TeleportToLastLocationPacket;
import com.nxst.tomeofbinding.network.data.PlayerListRequestMessage;
import com.nxst.tomeofbinding.network.data.PlayerListResponseMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(Tome.MODID)
public class Tome {
    public static final String MODID = "tomeofbinding";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(MODID, "main_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<Item> TOME = ITEMS.register("tome",
            () -> new TomeItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SCROLL_OF_VISION = ITEMS.register("scroll_of_vision",
            () -> new ScrollOfVisionItem(new Item.Properties().stacksTo(16)));

    public Tome() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModConfig.loadConfig();
        ITEMS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private static int packetId = 0;

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            INSTANCE.registerMessage(packetId++, PlayerListRequestMessage.class, PlayerListRequestMessage::encode, PlayerListRequestMessage::decode, PlayerListRequestMessage::handle);
            INSTANCE.registerMessage(packetId++, PlayerListResponseMessage.class, PlayerListResponseMessage::encode, PlayerListResponseMessage::decode, PlayerListResponseMessage::handle);
            INSTANCE.registerMessage(packetId++, PlayerSelectPacket.class, PlayerSelectPacket::encode, PlayerSelectPacket::decode, PlayerSelectPacket::handle);
            INSTANCE.registerMessage(packetId++, TeleportToLastLocationPacket.class, TeleportToLastLocationPacket::encode, TeleportToLastLocationPacket::decode, TeleportToLastLocationPacket::handle);
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void addCreative(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                event.accept(TOME.get());
                event.accept(SCROLL_OF_VISION.get());
            }
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientEvents {
        private static int tickCounter = 0;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null && mc.player != null) {
                    tickCounter++;
                    if (tickCounter >= 20) {
                        tickCounter = 0;
                        boolean historyUpdated = false;
                        for (Player player : mc.level.players()) {
                            if (!player.getUUID().equals(mc.player.getUUID())) {
                                ModConfig.addPlayerToHistory(player.getUUID().toString(), player.getName().getString(), player.level().dimension().location().toString(), player.getX(), player.getY(), player.getZ());
                                historyUpdated = true;
                            }
                        }
                        if (historyUpdated) {
                            ModConfig.saveConfig();
                        }
                    }
                }
            }
        }
    }
}